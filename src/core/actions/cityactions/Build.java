package core.actions.cityactions;

import core.Types;
import core.actions.Action;
import core.game.Board;
import core.game.GameState;
import utils.Vector2d;

public class Build extends CityAction
{
    private Types.BUILDING buildingType;

    public Build(int cityId)
    {
        super(Types.ACTION.BUILD);
        super.cityId = cityId;
    }

    public void setBuildingType(Types.BUILDING buildingType) {this.buildingType = buildingType;}

    public Types.BUILDING getBuildingType() {
        return buildingType;
    }

    @Override
    public boolean isFeasible(final GameState gs) {
        // Buildings that must be unique in a tribe (i.e. monuments)
        if (buildingType.isMonument()) {
            boolean monumentNotBuilt = gs.getTribe(gs.getActor(this.cityId).getTribeId()).isMonumentBuildable(buildingType);
            return monumentNotBuilt && isBuildable(gs);
        }
        return isBuildable(gs);
    }

    @Override
    public Action copy() {
        Build build = new Build(this.cityId);
        build.setBuildingType(this.buildingType);
        build.setTargetPos(this.targetPos.copy());
        return build;
    }

    private boolean isBuildable(final GameState gs) {
        // Cost and tech constraints moved to build factory

        Board board = gs.getBoard();
        //Resource constraint
        Types.RESOURCE resNeeded = buildingType.getResourceConstraint();
        if (resNeeded != null && resNeeded != board.getResourceAt(targetPos.x, targetPos.y)) // if there's a constraint, resource at location must be what's needed
            return false;

        //Terrain constraint
        if (!(buildingType.getTerrainRequirements().contains(board.getTerrainAt(targetPos.x, targetPos.y)))) return false;

        //Adjacency constraint
        Types.BUILDING buildingNeeded = buildingType.getAdjacencyConstraint();
        if(buildingNeeded != null)
        {
            boolean adjFound = false;
            for(Vector2d adjPos : targetPos.neighborhood(1,0,board.getSize()))
            {
                if(board.getBuildingAt(adjPos.x, adjPos.y) == buildingNeeded)
                {
                    adjFound = true;
                    break;
                }
            }

            return adjFound;
        }
        // Uniqueness constraint moved to build factory
        return true;
    }

    public String toString()
    {
        return "BUILD by city " + this.cityId+ " at " + targetPos + " : " + buildingType.toString();
    }

    public boolean equals(Object o) {
        if(!(o instanceof Build))
            return false;

        Build other = (Build) o;

        return super.equals(other) && buildingType == other.buildingType;
    }
}
