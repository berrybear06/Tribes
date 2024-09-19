import torch
import torch.nn as nn
import torch.nn.functional as F

from agent.architecture.common import SpatialResblock, Projection2d, Projection1d


class Downscale(nn.Module):
	"""Downscale the visual stream."""

	def __init__(
			self,
			input_spatial_size,
			input_features_size,
			output_features_size,
			downscale_factor,
			kernel_size,
			use_layer_norm=True):
		"""
		Initializes Downscale module.

		:param input_spatial_size: The spatial size of the input.
		:param input_features_size: The number of feature planes of the input.
		:param output_features_size: The number of feature planes of the output.
		:param downscale_factor: The downscale factor to apply to the input.
		:param kernel_size: The size of the convolution kernel to use.
		:param use_layer_norm: Whether to use layer normalization.
		"""

		super(Downscale, self).__init__()

		# Ensure the input spatial size is divisible by the downscale factor
		# if input_spatial_size % downscale_factor != 0:
		# 	raise ValueError(
		# 		f'input_spatial_size ({input_spatial_size}) must be a multiple of downscale_factor ({downscale_factor}).')

		self.input_spatial_size = input_spatial_size
		self.input_features_size = input_features_size
		self.output_features_size = output_features_size
		self.downscale_factor = downscale_factor
		self.kernel_size = kernel_size
		self.use_layer_norm = use_layer_norm

		# Layer normalization (if needed)
		if use_layer_norm:
			self.layer_norm = nn.LayerNorm([input_features_size, input_spatial_size, input_spatial_size])

		# Convolutional layer for downscaling (with stride for downsampling)
		self.conv = nn.Conv2d(
			in_channels=input_features_size,
			out_channels=output_features_size,
			kernel_size=kernel_size,
			stride=downscale_factor,
			padding=kernel_size // 2)  # 'same' padding equivalent

	def forward(self, x):
		if self.use_layer_norm:
			x = self.layer_norm(x)
		x = self.conv(F.relu(x))

		return x


class SpatialResNet(nn.Module):
	"""Spatial ResNet module with a stack of SpatialResblocks."""

	def __init__(
			self,
			size,
			input_channels,
			num_resblocks=3,
			hidden_size=None,
			kernel_size=3,
			use_layer_norm=True):
		"""
		Initializes SpatialResNet module.

		Args:
			size: Size of square feature channels.
			input_channels: Number of input feature channels.
			num_resblocks: Number of residual blocks.
			hidden_size: Size of the hidden layers in the residual blocks.
			kernel_size: Size of the convolution kernel.
			use_layer_norm: Whether to use layer normalization in the blocks.
		"""
		super(SpatialResNet, self).__init__()

		# Stack of SpatialResblocks
		self.resblocks = nn.ModuleList([
			SpatialResblock(
				size,
				channels=input_channels,
				kernel_size=kernel_size,
				num_layers=2,
				hidden_size=hidden_size or input_channels,
				use_layer_norm=use_layer_norm)
			for _ in range(num_resblocks)
		])

	def forward(self, x):
		for resblock in self.resblocks:
			x = resblock(x)

		return x


class EntityScatter(nn.Module):
	def __init__(self, input_size, output_size, max_entities, map_size, use_layer_norm=False):
		super(EntityScatter, self).__init__()
		self.MAP_SIZE = map_size
		self.use_layer_norm = use_layer_norm
		self.output_features_size = output_size

		self.proj = Projection1d(max_entities, input_size, output_size, channels_first=False)

	def forward(self, entity_x, entity_y, embeddings_list):
		"""
		Args:
			entity_x: A tensor of shape [batch_size, num_units] containing units' x-coordinates.
			entity_y: A tensor of shape [batch_size, num_units] containing units' y-coordinates.
			embeddings_list: A tensor of shape [batch_size, num_units, embedding_dim]
				containing the embeddings of units.

		Returns:
			A tensor of shape [batch_size, output_features_size, MAP_SIZE, MAP_SIZE].
		"""
		# Initialize an empty feature map with dimensions [batch_size, MAP_SIZE, MAP_SIZE, last_hidden_size]
		batch_size = embeddings_list.size(0)
		z = torch.zeros(batch_size, self.MAP_SIZE, self.MAP_SIZE, self.output_features_size)
		device = next(self.parameters()).device
		z = z.to(device)
		print("embed:", embeddings_list.size())
		embeddings_list = self.proj(embeddings_list)
		# Process each batch independently
		for batch_idx, (batch_x, batch_y, embeddings) in enumerate(zip(entity_x, entity_y, embeddings_list)):
			# Process each unit in the batch
			print(len(batch_x) , len(batch_y) , len(embeddings))
			print(batch_x.size(), batch_y.size(), embeddings.size())

			for x, y, embedding in zip(batch_x, batch_y, embeddings):


				# Place the embedding in the visual stream at position (x, y) for this batch
				z[batch_idx, x, y, :] = embedding

		return z


class TerritoryScatter(nn.Module):
	def __init__(self, size, entity_embedding_size, city_output_size, player_output_size, max_entities, use_layer_norm=False):
		super(TerritoryScatter, self).__init__()

		# Size of the map
		self.size = size

		self.use_layer_norm = use_layer_norm
		if use_layer_norm:
			self.layer_norm = nn.LayerNorm(entity_embedding_size)

		self.city_proj = Projection1d(max_entities, entity_embedding_size, city_output_size, channels_first=False)
		self.player_proj = Projection2d(size, 3, player_output_size, channels_first=False)

	def forward(self, entity_embeddings, city_indices, owning_city, owning_player):
		batch_size = entity_embeddings.size(0)  # Get the batch size

		if self.use_layer_norm:
			# Apply LayerNorm independently per entity embedding
			entity_embeddings = self.layer_norm(entity_embeddings)

		# Process city embeddings before scattering
		city_embeddings = self.city_proj(entity_embeddings)

		# Create a map with space for the city embeddings per batch
		city_map = torch.zeros((batch_size, self.size, self.size, city_embeddings.size(-1)))
		device = next(self.parameters()).device
		city_map = city_map.to(device)

		# Scatter city embeddings for each batch based on city_indices and owning_city
		for i in range(batch_size):
			for city_id, city_idx in city_indices.items():
				city_mask = (owning_city[i] == city_id)  # Get mask for where this city owns tiles in batch i
				city_map[i][city_mask] = city_embeddings[i, city_idx]  # Scatter city embedding into those tiles

		# Apply transformation to owning_player tensor (batch-wise)
		player_output = self.player_proj(owning_player)

		return city_map, player_output
