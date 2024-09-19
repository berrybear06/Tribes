import torch
import torch.nn as nn
import torch.nn.functional as F
from agent.architecture.common import ElementWiseResidualBlock


class EntityTransformer(nn.Module):
	def __init__(
			self, input_size, num_heads, hidden_size, num_layers,
			use_layer_norm=True, resblocks_before=0, resblocks_after=2, resblock_layers=2):
		super(EntityTransformer, self).__init__()
		self.use_layer_norm = use_layer_norm
		self.resblocks_before = resblocks_before
		self.resblocks_after = resblocks_after

		self.pre_resblocks = nn.ModuleList([
			ElementWiseResidualBlock(input_size, hidden_size, use_layer_norm)
			for _ in range(resblocks_before)
		])

		self.attention_layers = nn.ModuleList([
			nn.MultiheadAttention(embed_dim=input_size, num_heads=num_heads, batch_first=True)
			for _ in range(num_layers)
		])

		if self.use_layer_norm:
			self.layer_norms = nn.ModuleList([
				nn.LayerNorm(input_size) for _ in range(num_layers)
			])

		self.post_resblocks = nn.ModuleList([
			ElementWiseResidualBlock(input_size, resblock_layers, hidden_size, use_layer_norm)
			for _ in range(resblocks_after)
		])

	def forward(self, x, mask):
		print("called fwd:", x.size())
		# Apply pre-residual blocks
		x = x.permute(0, 2, 1)  # Batches, units, channels

		for resblock in self.pre_resblocks:
			x = resblock(x)

		# Apply transformer layers
		for count, attn_layer in enumerate(self.attention_layers):
			residual = x
			if self.use_layer_norm:
				x = self.layer_norms[count](x)
			print("before attn:", x.size(), mask.size())
			x, _ = attn_layer(x, x, x, key_padding_mask=mask)
			x += residual  # Residual connection

		# Apply post-residual blocks
		for resblock in self.post_resblocks:
			x = resblock(x)
		x = x.permute(0, 2, 1)  # Batches, channels, units
		return x
