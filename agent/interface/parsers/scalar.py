from config import MAP_SIZE
from interface.utils import one_hot
from math import sqrt, log


class ScalarParser:
	"""
	Parses scalar features into the following agent-readable format.

	tribe: one-hot of four known, one unknown (5 -> 8)
	opponent_tribe: same as tribe (5 -> 8)
	stars_per_turn: one-hot of floor of sqrt(min(SPT, 64)) (9 -> 8)
	score: one-hot of floor of 4*log(min(score+1, 10000) (17 -> 8)
	opponent_score: same as score (17 -> 8)
	technology (24 -> 64)
	time: positional encoding (32)
	kills: one-hot of min(kills, 10) (11 -> 8)
	pacifist_count: one-hot of min(pacifist_count, 5) (6 -> 8)
	connected_cities: one-hot of min(connected_cities, 5) (6 -> 8)
	tiles_visible: one-hot of floor of 10 * (visible tiles) / (total tiles) (11 -> 8)

	Added to scalar_context as well:
	stars: one-hot of floor of sqrt(min(stars, 256)) (17 -> 16)
	previous_action: one-hot of previous action type, counting each non-road building (20 other + 19 build -> 64)
	available_actions: same as previous_action (39 -> 64)
	unit_counts_agent: sqrt of unit counts (12 -> 32)
	unit_counts_opponent: same as unit_counts_agent (12 -> 32)
	units_fresh: one_hot of floor of 10 * (fresh units) / (total units) (11 -> 8)
	units_can_move: same as units_fresh (11 -> 8)
	units_can_attack: same as units_fresh (11 -> 8)
	"""

	def __init__(self, total_unit_types=12):
		self.TOTAL_UNIT_TYPES = total_unit_types

	def parse(self, game_state, tribe_id, spt, visible):  # TODO add up spt, count tiles in map parser
		tribes = game_state["tribes"]
		units = game_state["unit"]

		tribe_data = tribes[str(tribe_id)]
		opponent_tribe_id = 1 - tribe_id
		opponent_data = tribes[str(opponent_tribe_id)]

		tribe_type = tribe_data["type"]
		tribe = one_hot(tribe_type, 5)

		tribes_met = tribe_data["tribesMet"]
		if opponent_tribe_id in tribes_met:
			opponent_tribe_type = opponent_data["type"]
		else:
			opponent_tribe_type = 4  # unknown
		opponent_tribe = one_hot(opponent_tribe_type, 5)

		spt_embed = int(sqrt(min(spt, 64)))
		stars_per_turn = one_hot(spt_embed, 9)

		score_embed = int(4*log(min(tribe_data["score"] + 1, 10000), 10))
		score = one_hot(score_embed, 17)

		opponent_score_embed = int(4*log(min(opponent_data["score"] + 1, 10000), 10))
		opponent_score = one_hot(opponent_score_embed, 17)

		technology = [int(status) for status in tribe_data["technology"]["researched"]]

		time = []  # TODO

		kills = one_hot(min(tribe_data["nKills"], 10), 11)

		pacifist_count = one_hot(min(tribe_data["nPacifistCount"], 5), 6)

		connected_cities = one_hot(min(len(tribe_data["connectedCities"]), 5), 6)

		tiles_visible = one_hot(int(10 * visible / (MAP_SIZE * MAP_SIZE)), 11)

		stars_embed = int(sqrt(min(tribe_data["star"], 256)))
		stars = one_hot(stars_embed, 17)

		# TODO
		previous_action = []
		available_actions = []

		unit_counts_agent = [0] * self.TOTAL_UNIT_TYPES
		unit_counts_opponent = [0] * self.TOTAL_UNIT_TYPES
		for unit in units.values():
			if unit["tribeId"] == tribe_id:  # agent's unit
				unit_counts_agent[unit["type"]] += 1
			else:  # opponent's unit
				unit_x, unit_y = unit["x"], unit["y"]
				if tribe_data["obsGrid"][unit_x][unit_y]:  # unit is visible
					unit_counts_opponent[unit["type"]] += 1

		# TODO
		units_fresh = []
		units_can_move = []
		units_can_attack = []

		return {
			"tribe": tribe,
			"opponent_tribe": opponent_tribe,
			"stars_per_turn": stars_per_turn,
			"score": score,
			"opponent_score": opponent_score,
			"technology": technology,
			"time": time,
			"kills": kills,
			"pacifist_count": pacifist_count,
			"connected_cities": connected_cities,
			"tiles_visible": tiles_visible,
			"stars": stars,
			"previous_action": previous_action,
			"available_actions": available_actions,
			"unit_counts_agent": unit_counts_agent,
			"unit_counts_opponent": unit_counts_opponent,
			"units_fresh": units_fresh,
			"units_can_move": units_can_move,
			"units_can_attack": units_can_attack
		}
