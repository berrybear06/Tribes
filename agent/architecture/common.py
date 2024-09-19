import torch
import torch.nn as nn
import torch.nn.functional as F


class ResidualBlock(nn.Module):
	"""Fully connected residual block."""

	def __init__(self, input_size, num_layers=2, hidden_size=None, use_layer_norm=True):
		"""
		Initializes the ResidualBlock module.

		Args:
		  input_size: Size of one-dimensional input.
		  num_layers: Number of layers in the residual block.
		  hidden_size: Size of hidden layers in the residual block.
		  use_layer_norm: Whether to use layer normalization.
		"""
		super(ResidualBlock, self).__init__()
		self.input_size = input_size
		self.num_layers = num_layers
		self.hidden_size = hidden_size or input_size
		self.use_layer_norm = use_layer_norm

		self.layers = nn.ModuleList()
		for i in range(num_layers):
			channels_in = input_size if i == 0 else hidden_size
			channels_out = input_size if i == num_layers - 1 else hidden_size
			linear = nn.Linear(channels_in, channels_out)
			self.layers.append(linear)
			if i < num_layers - 1:
				nn.init.normal_(linear.weight, mean=0.0, std=0.005)
				nn.init.constant_(linear.bias, 0.0)

		if use_layer_norm:
			self.layer_norms = nn.ModuleList()
			for i in range(num_layers):
				channels_in = input_size if i == 0 else hidden_size
				self.layer_norms.append(nn.LayerNorm(channels_in))

	def forward(self, x):
		shortcut = x
		for i, layer in enumerate(self.layers):
			if self.use_layer_norm:
				x = self.layer_norms[i](x)
			x = layer(F.relu(x))

		return x + shortcut


class ElementWiseResidualBlock(ResidualBlock):
	def forward(self, x):
		# Assuming x has shape [batch_size, units, channels]
		# Apply the residual block to each element along the axis (unit-wise)
		batch_size, num_units, feature_dim = x.size()
		print("during elemwiseres:", x[:, 1, :].size())
		output = torch.stack([super(ElementWiseResidualBlock, self).forward(x[:, i, :]) for i in range(num_units)],
							 dim=1)
		return output


class SpatialResblock(nn.Module):
	"""2D Convolutional Residual Block."""

	def __init__(self, size, channels, kernel_size=3, num_layers=2, hidden_size=None, use_layer_norm=True):
		"""
		Initializes SpatialResblock module.

		Args:
			size: The size of the square feature planes.
			channels: The number of feature planes.
			kernel_size: The size of the convolution kernel.
			num_layers: Number of layers in the residual block.
			hidden_size: Size of the activation vector in the residual block.
			use_layer_norm: Whether to use layer normalization.
		"""
		super(SpatialResblock, self).__init__()

		self.num_layers = num_layers
		self.hidden_size = hidden_size
		self.use_layer_norm = use_layer_norm
		self.kernel_size = kernel_size

		# Build the convolutional layers
		self.convs = nn.ModuleList()
		for i in range(self.num_layers):
			in_channels = channels if i > 0 else hidden_size
			out_channels = hidden_size if i < self.num_layers - 1 else channels
			conv = nn.Conv2d(in_channels, out_channels, kernel_size, padding=kernel_size // 2)
			self.convs.append(conv)
			if i < num_layers - 1:
				nn.init.normal_(conv.weight, mean=0.0, std=0.005)
				nn.init.constant_(conv.bias, 0.0)

		if self.use_layer_norm:
			self.layer_norms = nn.ModuleList()
			for i in range(self.num_layers):
				in_channels = channels if i > 0 else hidden_size
				self.layer_norms.append(nn.LayerNorm([in_channels, size, size]))

	def forward(self, x):
		shortcut = x
		for i, layer in enumerate(self.convs):
			if self.use_layer_norm:
				x = self.layer_norms[i](x)
			x = layer(F.relu(x))

		return x + shortcut


class Projection1d(nn.Module):
	"""One-dimensional projection with optional layer norm."""

	def __init__(self, input_dim, in_channels, out_channels, use_layer_norm=True, channels_first=True):
		super(Projection1d, self).__init__()
		self.use_layer_norm = use_layer_norm
		self.channels_first = channels_first
		if use_layer_norm:
			self.layer_norm = nn.LayerNorm(input_dim)
		self.conv = nn.Conv1d(in_channels, out_channels, kernel_size=1)

	def forward(self, x):
		if not self.channels_first:
			x = x.permute(0, 2, 1)

		if self.use_layer_norm:
			x = self.layer_norm(x)
		x = self.conv(F.relu(x))

		if not self.channels_first:
			x = x.permute(0, 2, 1)

		return x


class Projection2d(nn.Module):
	"""Two-dimensional projection with optional layer norm."""

	def __init__(self, input_dim, in_channels, out_channels, use_layer_norm=True, channels_first=True):
		super(Projection2d, self).__init__()
		self.use_layer_norm = use_layer_norm
		self.channels_first = channels_first
		if use_layer_norm:
			self.layer_norm = nn.LayerNorm([input_dim, input_dim])
		self.conv = nn.Conv2d(in_channels, out_channels, kernel_size=1)

	def forward(self, x):
		if not self.channels_first:
			x = x.permute(0, 3, 1, 2)

		if self.use_layer_norm:
			x = self.layer_norm(x)
		x = self.conv(F.relu(x))

		if not self.channels_first:
			x = x.permute(0, 2, 3, 1)

		return x