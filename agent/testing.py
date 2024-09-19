import json

from agent.config import MAX_ENTITIES
from agent.interface import Parser
from agent.architecture.torso import Torso
import torch
from torch.utils.data import Dataset, DataLoader

# Custom Dataset that returns the same parsed data
class GameStateDataset(Dataset):
	def __init__(self, parsed_data):
		self.parsed_data = parsed_data

	def __len__(self):
		# Return a large number so that DataLoader can generate multiple batches if needed
		return 100

	def __getitem__(self, idx):
		# Return the same parsed data for each batch (since we want both entries to be the same)
		return self.parsed_data


# Load and parse the game state from the JSON file
path = r"C:\Users\jerry\Downloads\Tribes\save\1722469115074\0_1\game.json"
with open(path) as f:
	game_state = json.load(f)
	p = Parser()
	parsed_state = p.parse(game_state)

# Create the model
model = Torso(
	max_entities=MAX_ENTITIES,
	entity_input_size=88,  # Example size
	entity_hidden_size=4,
	entity_embed_size=5,
	entity_embed_all_size=10,
	entity_scatter_size=6,
	city_scatter_size=7,
	player_scatter_size=8,
	map_size=11,  # Example map size
	map_input_channels=41,
	spatial_channels=9,
	downscale_factor=2,
	downscale_kernel=3,
	resnet_blocks=1,
	use_layer_norm=True
)

# Create the dataset and dataloader
game_state_dataset = GameStateDataset(parsed_data=parsed_state)
data_loader = DataLoader(game_state_dataset, batch_size=2, shuffle=False)

# Iterate over the DataLoader and run the model on the batches
for batch_idx, batch in enumerate(data_loader):
	output = model(batch)
	print(f"Batch {batch_idx + 1} output shape: {output[0].shape}, {output[1].shape}")

	# For testing, just run one batch
	break
