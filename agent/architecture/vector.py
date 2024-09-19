from torch import nn

from agent.architecture.common import ResidualBlock


class ResNetModule(nn.Module):
	"""ResNet module with a stack of ResidualBlocks."""

	def __init__(self, input_size, num_resblocks=3, hidden_size=None, use_layer_norm=True):
		"""
		Initializes ResNet module.

		:param input_size: Size of the input vector.
		:param num_resblocks: Number of residual blocks.
		:param hidden_size: Size of the hidden layer in the residual blocks.
		:param use_layer_norm: Whether to use layer normalization in the blocks.
		"""
		super(ResNetModule, self).__init__()
		self.num_resblocks = num_resblocks

		# Stack of ResidualBlocks
		self.resblocks = nn.ModuleList([
			ResidualBlock(input_size, num_layers=2, hidden_size=hidden_size, use_layer_norm=use_layer_norm)
			for _ in range(num_resblocks)
		])

	def forward(self, x):
		for resblock in self.resblocks:
			x = resblock(x)

		return x

# class ElementWiseEmbedder:
