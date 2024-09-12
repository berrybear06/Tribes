from agent.config import MAP_SIZE
from agent.interface.utils import one_hot


class MapParser:
	"""
	Parses the map into the following agent-readable format.

	visible (2)
	entity_owner (2)
	terrain (5: plains, mountain, forest, water, ocean)
	resource_or_ruin (7: fruit, game, fish, metal, crop, whale, ruin)
	building (8+4+7: lumber hut, farm, mine, sawmill, windmill, forge, customs house, port;
		plains, mountain, forest, ocean temples;
		7 monuments)
	village_or_city (2 booleans)
	can_build: one_hot no ruin, building, temple, monument, village, or city (2)
	networked (2)

	To be concatenated before the spatial encoding process:
	scattered_entities (16)
	scattered_owning_city (8)
	"""

	def __init__(
			self,
			terrain_types=5,
			terrain_mapping=None,
			resource_types=7,
			resource_mapping=None,
			building_types=8+4+7,
			map_size=MAP_SIZE
	):
		self.TERRAIN_TYPES = terrain_types
		self.RESOURCE_TYPES = resource_types
		self.BUILDING_TYPES = building_types
		self.MAP_SIZE = map_size
		# Plain, shallow water, deep water, mountain, village, city, forest, fog
		self.TERRAIN_MAPPING = terrain_mapping or [0, 1, 2, 3, -1, -1, 4, -1]
		# Fish, fruit, game, whale, NONE, ore, crop, ruin
		self.RESOURCE_MAPPING = resource_mapping or [0, 1, 2, 3, -1, 4, 5, 6]

	def parse_map(self, game_state, tribe_id):
		board = game_state["board"]
		units = game_state["unit"]
		tribes = game_state["tribes"]
		parsed_map = [[None] * self.MAP_SIZE for _ in range(self.MAP_SIZE)]
		for x in range(self.MAP_SIZE):
			for y in range(self.MAP_SIZE):
				unit_id = board["unitID"][x][y]
				unit = None
				if unit_id != 0:
					unit = units[str(unit_id)]
				tribe = tribes[str(tribe_id)]
				parsed_map[x][y] = self.parse_tile(x, y, board, unit, tribe, tribe_id)
		return parsed_map

	def parse_tile(self, x, y, board, unit, tribe, tribe_id):
		is_visible = tribe["obsGrid"][x][y]
		visible = one_hot(int(is_visible), 2)

		if is_visible:
			unit_belongs = -1 if unit is None else unit["tribeId"] == tribe_id
			entity_owner = one_hot(int(unit_belongs), 2)

			terrain_mapping = self.TERRAIN_MAPPING
			tile_terrain = board["terrain"][x][y]
			terrain = one_hot(terrain_mapping[tile_terrain], self.TERRAIN_TYPES)

			resource_mapping = self.RESOURCE_MAPPING
			tile_resource = board["resource"][x][y]
			resource_or_ruin = one_hot(resource_mapping[tile_resource], self.RESOURCE_TYPES)

			tile_building = board["building"][x][y]
			building = one_hot(tile_building, self.BUILDING_TYPES)

			has_building = tile_building != -1
			has_ruin = tile_resource == 7
			has_village = tile_building == 4
			has_city = tile_building == 5
			village_or_city = [int(has_village), int(has_city)]

			can_build = one_hot(int(not has_building and not has_ruin and not has_village and not has_city), 2)

			has_network = board["network"][x][y]
			networked = one_hot(int(has_network), 2)

			result = visible
			result.extend(entity_owner)
			result.extend(terrain)
			result.extend(resource_or_ruin)
			result.extend(building)
			result.extend(village_or_city)
			result.extend(can_build)
			result.extend(networked)
			return result

		else:
			result = visible
			result.extend([0] * (2 + self.TERRAIN_TYPES + self.RESOURCE_TYPES + self.BUILDING_TYPES + 2 + 2 + 2))
			return result
