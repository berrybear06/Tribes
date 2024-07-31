package players.portfolio.scripts.utils;

import core.actors.Actor;
import core.game.GameState;

public interface InterestPoint
{
    boolean ofInterest(GameState gs, Actor ac, int posX, int posY);
}
