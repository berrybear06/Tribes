package core.actions.unitactions;

import core.TribesConfig;
import core.actions.Action;
import core.actors.units.Battleship;
import core.actors.units.Boat;
import core.actors.units.Ship;
import core.game.GameState;
import core.actors.units.Unit;
import core.Types;

import java.util.LinkedList;

public class MakeVeteran extends UnitAction
{
    public MakeVeteran(int unitId)
    {
        super(Types.ACTION.MAKE_VETERAN);
        super.unitId = unitId;
    }


    @Override
    public boolean isFeasible(final GameState gs) {
        Unit unit = (Unit) gs.getActor(this.unitId);
        if(unit.getType() == Types.UNIT.SUPERUNIT)
            return false;
        if (unit instanceof Boat && ((Boat) unit).getBaseLandUnit() == Types.UNIT.SUPERUNIT)
            return false;
        if (unit instanceof Ship && ((Ship) unit).getBaseLandUnit() == Types.UNIT.SUPERUNIT)
            return false;
        if (unit instanceof Battleship && ((Battleship) unit).getBaseLandUnit() == Types.UNIT.SUPERUNIT)
            return false;
        return unit.getKills() >= TribesConfig.VETERAN_KILLS && !unit.isVeteran();
    }


    @Override
    public Action copy() {
        return new MakeVeteran(this.unitId);
    }

    public String toString() {
        return "MAKE_VETERAN by unit " + this.unitId;
    }
}
