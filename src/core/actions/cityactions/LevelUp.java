package core.actions.cityactions;

import core.Types;
import core.actions.Action;
import core.actors.City;
import core.game.GameState;
import core.Types.CITY_LEVEL_UP;


public class LevelUp extends CityAction {

    private CITY_LEVEL_UP bonus;

    public LevelUp(int cityId)
    {
        super(Types.ACTION.LEVEL_UP);
        super.cityId = cityId;
    }
    public CITY_LEVEL_UP getBonus() {
        return bonus;
    }
    public void setBonus(CITY_LEVEL_UP bonus) {
        this.bonus = bonus;
    }

    @Override
    public boolean isFeasible(GameState gs) {
        City city = (City) gs.getActor(this.cityId);
        return city.canLevelUp() && bonus.validType(city.getLevel());
    }


    @Override
    public Action copy() {
        LevelUp lUp = new LevelUp(this.cityId);
        lUp.setBonus(this.bonus);
        lUp.setTargetPos(this.targetPos);
        return lUp;
    }

    @Override
    public String toString()
    {
        return "LEVEL_UP by city " + this.cityId+ " with bonus " + bonus.toString();
    }

    public boolean equals(Object o) {
        if(!(o instanceof LevelUp))
            return false;

        LevelUp other = (LevelUp) o;

        return super.equals(other) && bonus == other.bonus;
    }
}
