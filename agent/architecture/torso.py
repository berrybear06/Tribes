import math

import torch
import torch.nn as nn
import torch.nn.functional as F

from agent.architecture.common import Projection1d, Projection2d
from agent.architecture.entity import EntityTransformer
from agent.architecture.spatial import EntityScatter, TerritoryScatter, SpatialResNet, Downscale


class Torso(nn.Module):
	def __init__(
			self, max_entities, entity_input_size, entity_hidden_size, entity_output_size, entity_scatter_size,
			city_scatter_size, player_scatter_size, map_size, map_input_channels, spatial_channels,
			downscale_factor, downscale_kernel, resnet_blocks, use_layer_norm=False):
		super(Torso, self).__init__()

		self.entity_proj1 = Projection1d(max_entities, entity_input_size, entity_hidden_size)

		self.transformer = EntityTransformer(entity_hidden_size, 1, 16, 2)

		self.entity_proj2 = Projection1d(max_entities, entity_hidden_size, entity_output_size)

		# EntityScatter module
		self.entity_scatter = EntityScatter(
			input_size=entity_output_size,
			output_size=entity_scatter_size,
			max_entities=max_entities,
			map_size=map_size,
			use_layer_norm=use_layer_norm)

		# TerritoryScatter module for processing owning_city and owning_player
		self.territory_scatter = TerritoryScatter(
			size=map_size,
			entity_embedding_size=entity_output_size,
			city_output_size=city_scatter_size,
			player_output_size=player_scatter_size,
			max_entities=max_entities,
			use_layer_norm=use_layer_norm)

		# Final convolution after concatenation
		post_concat_size = map_input_channels + entity_scatter_size + city_scatter_size + player_scatter_size
		print(map_input_channels, entity_scatter_size, city_scatter_size, player_scatter_size)

		self.final_proj = Projection2d(map_size, post_concat_size, spatial_channels)

		self.spatial_resnet = SpatialResNet(
			map_size,
			spatial_channels,
			num_resblocks=resnet_blocks,
			hidden_size=spatial_channels,
			kernel_size=3,
			use_layer_norm=use_layer_norm)

		self.downscale1 = Downscale(map_size, spatial_channels, spatial_channels, downscale_factor, downscale_kernel)
		map_size_inter = math.ceil(map_size / downscale_factor)
		self.downscale2 = Downscale(map_size_inter, spatial_channels, spatial_channels, downscale_factor, downscale_kernel)

	def forward(self, parsed_state):
		# Process agent's perspective
		agent_data = parsed_state['agent']

		# Preprocess the entity list through 1D convolutions
		entity_list = agent_data['entity_list']  # Tensor of shape [units, channels]
		x = entity_list.permute(0, 2, 1)  # Rearrange to [batch_size, channels, units]
		print("after permute:", x.size())
		x = self.entity_proj1(x)
		print("before transform:", x.size())
		x = self.transformer(x, agent_data["non_null_mask"])
		print("after transform:", x)
		x = self.entity_proj2(x)
		print("after conv2:", x)

		# Scatter processed entities onto the 2D map
		x = x.permute(0, 2, 1)  # Batches, entities, channels
		entity_map = self.entity_scatter(agent_data["entity_x"], agent_data["entity_y"], x)

		# Process and scatter city embeddings and player ownership
		city_map, player_map = self.territory_scatter(
			x,
			agent_data['city_indices'],
			agent_data['owning_city'],
			agent_data['owning_player'])

		# Concatenate the entity map, city map, and player map
		print(agent_data["map"].size(), entity_map.size(), city_map.size(), player_map.size())
		concat_map = torch.cat((agent_data["map"], entity_map, city_map, player_map), dim=3)
		concat_map = concat_map.permute(0, 3, 1, 2)  # Channels first

		processed_map = self.final_proj(concat_map)

		resnet_output = self.spatial_resnet(processed_map)

		intermediate_map = self.downscale1(resnet_output)
		downscaled_map = self.downscale2(intermediate_map)

		# Return 1d game embedding, map skip connections for deconvolution, scalar context for action type gating
		return downscaled_map
