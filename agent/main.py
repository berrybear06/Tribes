import json
from interface import Parser

# path = input("Path to game.json: ")
path = r"C:\Users\jerry\Downloads\Tribes\save\1722469115074\0_1\game.json"
with open(path) as f:
	game_state = json.load(f)
	p = Parser()
	parsed_state = p.parse(game_state)
	print(parsed_state)
