import torch

from agent.config import MAP_SIZE
from agent.interface import parsers


class Parser:
	"""
	Parses game states in json dictionary format produced by the GameSaver class.
	"""

	def __init__(self, map_size=MAP_SIZE):
		self.MAP_SIZE = map_size
		self.entity_parser = parsers.EntityParser()
		self.map_parser = parsers.MapParser()
		self.scalar_parser = parsers.ScalarParser()
		self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

	def parse(self, game_state):
		"""
		:param game_state: the game state in dictionary form

		:return: a dictionary with entries "agent" and "opponent".
			Each entry is a dictionary containing entity_list, city_indices, map,
			and scalar_features as seen from the respective player.
		"""

		# Assume that the player to move is the agent
		agent_tribe_id = game_state["activeTribeID"]
		opponent_tribe_id = 1 - agent_tribe_id

		return {
			"agent": self._parse_as_tribe(game_state, agent_tribe_id),
			"opponent": self._parse_as_tribe(game_state, opponent_tribe_id)
		}

	def _parse_as_tribe(self, game_state, tribe_id):
		entity_data = self.entity_parser.parse_entities(game_state["unit"], game_state["city"], tribe_id)
		parsed_map, parsed_owners = self.map_parser.parse_map(game_state, tribe_id)
		scalar_features = self.scalar_parser.parse(game_state, tribe_id, 0, 0)  # TODO

		entity_list = torch.tensor(entity_data["entity_list"], dtype=torch.float32, device=self.device)
		entity_x = torch.tensor(entity_data["entity_x"], dtype=torch.int, device=self.device)
		entity_y = torch.tensor(entity_data["entity_y"], dtype=torch.int, device=self.device)
		city_indices = entity_data["city_indices"]
		non_null_mask = torch.tensor(entity_data["non_null_mask"], dtype=torch.float32, device=self.device)
		parsed_map = torch.tensor(parsed_map, dtype=torch.float32, device=self.device)
		parsed_owners = torch.tensor(parsed_owners, dtype=torch.float32, device=self.device)

		return {
			"entity_list": entity_list,
			"entity_x": entity_x,
			"entity_y": entity_y,
			"city_indices": city_indices,
			"non_null_mask": non_null_mask,
			"map": parsed_map,
			"owning_city": game_state["board"]["cityID"],
			"owning_player": parsed_owners,
			"scalar_features": scalar_features
		}
