package core.actions.cityactions.factory;

import core.Types;
import core.actions.Action;
import core.actions.ActionFactory;
import core.actions.cityactions.Build;
import core.actors.Actor;
import core.actors.City;
import core.actors.Tribe;
import core.game.Board;
import core.game.GameState;
import utils.Vector2d;

import java.util.LinkedList;

public class BuildFactory implements ActionFactory {

    @Override
    public LinkedList<Action> computeActionVariants(final Actor actor, final GameState gs) {

        City city = (City) actor;
        LinkedList<Action> actions = new LinkedList<>();
        Board board = gs.getBoard();
        LinkedList<Vector2d> tiles = board.getCityTiles(city.getActorId());

        for (Types.BUILDING building: Types.BUILDING.values()){
            if (canBuild(gs, building, city, tiles)) {
                for (Vector2d tile: tiles) {
                    //check if tile is empty
                    if (board.getBuildingAt(tile.x, tile.y) == null) {
                        Build action = new Build(city.getActorId());
                        action.setBuildingType(building);
                        action.setTargetPos(tile.copy());
                        if (action.isFeasible(gs)) {
                            actions.add(action);
                        }
                    }
                }
            }
        }
        return actions;
    }

    private static boolean canBuild(GameState gs, Types.BUILDING building, City city, LinkedList<Vector2d> tiles) {
        Tribe tribe = gs.getTribe(city.getTribeId());
        if (tribe.getStars() < building.getCost()) return false; // Cost constraint

        if (building.getTechnologyRequirement() != null &&
                !tribe.getTechTree().isResearched(building.getTechnologyRequirement())
        )
            return false; // Technology constraint

        Board board = gs.getBoard();
        if (building.isCityUnique()) {
            for (Vector2d tile: tiles) {
                if (board.getBuildingAt(tile.x, tile.y) == building) {
                    return false; // Uniqueness constraint
                }
            }
        }
        return true;
    }
}
