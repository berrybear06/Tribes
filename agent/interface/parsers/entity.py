import random
from math import ceil, log, sqrt

from config import MAP_SIZE, MAX_ENTITIES
from interface.utils import one_hot


class EntityParser:
	"""
	Parses units into the following agent-readable format.

	unit_type: one-hot (9: warrior, rider, defender, swordsman, archer, catapult, knight, mind bender, super unit)
	boat_type: one-hot (4: land unit, boat, ship, battleship)
	current_health: one-hot of floor of sqrt(min(current_health, 40)) (6 to exclude 0 health)
	x_position: binary encoding of x coordinate in tiles (ceil of log_2 (SIZE+1))
	y_position: binary encoding of y coordinate in tiles (ceil of log_2 (SIZE+1))
	has_home_city: one-hot (2)
	home_city_x: binary encoding of x coordinate of home city in tiles (ceil of log_2 (SIZE+1))
	home_city_y: binary encoding of y coordinate of home city in tiles (ceil of log_2 (SIZE+1))
	player: one-hot of unit/city belonging to self or enemy (2)
	is_veteran: one-hot (2)
	kills: one-hot (4: 0, 1, 2, 3+)
	is_capital: one-hot (2)
	has_walls: one-hot (2)
	level: one-hot (10: 1...10, considering level 10+ cities as level 10)
	production: float of stars per turn divided by level
	occupancy: one-hot of occupancy divided by (level+1) rounded down to the tenth (10: 0.0 ... 0.9)
	population: one-hot of population divided by (level+1) rounded down to the tenth (10: 0.0 ... 0.9)
	"""

	def __init__(
			self,
			unit_types=9,
			boat_types=4,
			boat_type_offset=7,
			max_health=40,
			map_size=MAP_SIZE,
			max_entities=MAX_ENTITIES
	):
		self.UNIT_TYPES = unit_types
		self.BOAT_TYPES = boat_types
		self.BOAT_TYPE_OFFSET = boat_type_offset
		self.MAX_HEALTH = max_health
		self.MAP_SIZE = map_size
		self.MAX_ENTITIES = max_entities
		self.BIN_SIZE = ceil(log(self.MAP_SIZE + 1, 2))
		self.HEALTH_SIZE = int(sqrt(self.MAX_HEALTH))

	def parse_entities(self, units, cities, tribe_id):
		parsed_entities = []
		city_indices = {}
		units_to_parse = list(units.values())
		if len(units) + len(cities) > self.MAX_ENTITIES:  # remove units at random
			for _ in range(len(units) + len(cities) - self.MAX_ENTITIES):
				units_to_parse.pop(random.randint(0, len(units_to_parse) - 1))
		for unit in units_to_parse:
			parsed_entities.append(self.parse_unit(unit, cities, tribe_id))
		for city_id, city in cities.items():
			parsed_entities.append(self.parse_city(city, tribe_id))
			city_indices[city_id] = len(parsed_entities) - 1
		return {"entity_list": parsed_entities, "city_indices": city_indices}

	def parse_unit(self, unit, cities, tribe_id):
		_utype = unit["type"]
		if _utype == 11:
			_utype = 8 # Change super unit's id
		if "baseLandType" in unit:
			_bltype = unit["baseLandType"]
			if _bltype == 11:
				_bltype = 8 # Change super unit's id
			boat_type = one_hot(_utype - self.BOAT_TYPE_OFFSET, self.BOAT_TYPES)
			unit_type = one_hot(_bltype, self.UNIT_TYPES)
		else:
			unit_type = one_hot(_utype, self.UNIT_TYPES)
			boat_type = one_hot(0, self.BOAT_TYPES)

		current_health = one_hot(int(sqrt(unit["currentHP"])) - 1, self.HEALTH_SIZE)

		x_position = encode_binary(unit["x"], self.BIN_SIZE)
		y_position = encode_binary(unit["y"], self.BIN_SIZE)

		_city_id = unit["cityID"]
		has_home_city = one_hot(int(_city_id == -1), 2)
		if _city_id == -1:
			home_city_x = [0] * self.BIN_SIZE
			home_city_y = [0] * self.BIN_SIZE
		else:
			_city = cities[str(_city_id)]
			home_city_x = encode_binary(_city["x"], self.BIN_SIZE)
			home_city_y = encode_binary(_city["y"], self.BIN_SIZE)

		player = one_hot(int(unit["tribeId"] == tribe_id), 2)

		is_veteran = one_hot(int(unit["isVeteran"]), 2)

		kills = one_hot(min(unit["kill"], 3), 4)

		result = unit_type
		result.extend(boat_type)
		result.extend(current_health)
		result.extend(x_position)
		result.extend(y_position)
		result.extend(has_home_city)
		result.extend(home_city_x)
		result.extend(home_city_y)
		result.extend(player)
		result.extend(is_veteran)
		result.extend(kills)
		# is_capital has_walls level production occupancy population limited_buildings
		result.extend([0]*(2 + 2 + 10 + 1 + 10 + 10 + 6))
		return result

	def parse_city(self, city, tribe_id):
		x_position = encode_binary(city["x"], self.BIN_SIZE)
		y_position = encode_binary(city["y"], self.BIN_SIZE)

		player = one_hot(int(city["tribeID"] == tribe_id), 2)

		is_capital = one_hot(int(city["isCapital"]), 2)

		has_walls = one_hot(int(city["hasWalls"]), 2)

		_lvl = city["level"]
		level = one_hot(min(_lvl, 10) - 1, 10)

		production = [sqrt(city["production"])]

		occupancy = one_hot(len(city["units"]) * 10 // (_lvl + 1), 10)

		population = one_hot(city["population"] * 10 // (_lvl + 1), 10)

		has_sawmill, has_windmill, has_forge = False, False, False
		for building in city["buildings"]:
			_btype = building["type"]
			if _btype == 7:
				has_sawmill = True
			elif _btype == 4:
				has_windmill = True
			elif _btype == 2:
				has_forge = True
		limited_buildings = one_hot(int(has_sawmill), 2)
		limited_buildings.extend(one_hot(int(has_windmill), 2))
		limited_buildings.extend(one_hot(int(has_forge), 2))

		results = [0]*(self.UNIT_TYPES + self.BOAT_TYPES + self.HEALTH_SIZE) # unit_type boat_type current_health
		results.extend(x_position)
		results.extend(y_position)
		results.extend([0]*(2 + self.BIN_SIZE + self.BIN_SIZE)) # has_home_city home_city_x home_city_y
		results.extend(player)
		results.extend([0]*(2 + 4)) # is_veteran kills
		results.extend(is_capital)
		results.extend(has_walls)
		results.extend(level)
		results.extend(production)
		results.extend(occupancy)
		results.extend(population)
		results.extend(limited_buildings)
		return results


def encode_binary(x, digits):
	binstr = format(x, "08b")[-digits:]
	return list(map(int, binstr))
