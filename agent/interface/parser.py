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

	def parse(self, game_state):
		"""
		:param game_state: the game state in dictionary form

		:return: a dictionary with entries "agent" and "opponent".
			Each entry is a dictionary containing unit_list, city_indices, map,
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
		parsed_map = self.map_parser.parse_map(game_state, tribe_id)
		scalar_features = self.scalar_parser.parse(game_state, tribe_id, 0, 0)  # TODO
		return {
			"entity_list": entity_data["entity_list"],
			"city_indices": entity_data["city_indices"],
			"map": parsed_map,
			"scalar_features": scalar_features
		}

	def _parse_scalar_features(self):
		pass
