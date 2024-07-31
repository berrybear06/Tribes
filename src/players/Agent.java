package players;

import core.Types;
import core.actions.Action;
import core.game.GameState;
import utils.ElapsedCpuTimer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public abstract class Agent {

    protected ArrayList<Integer> allPlayerIDs;
    protected int playerID;
    protected long seed;

    /**
     * Default constructor, to be called in subclasses (initializes player ID and random seed for this agent.
     * @param seed - random seed for this player.
     */
    protected Agent(long seed) {
        reset(seed);
    }

    /**
     * Function requests an action from the agent, given current game state observation.
     * @param gs - current game state.
     * @param ect - a timer that indicates when the turn time is due to finish.
     * @return - action to play in this game state.
     */
    public abstract Action act(GameState gs, ElapsedCpuTimer ect);

    /**
     * Function called at the end of the game. May be used by agents for final analysis.
     * @param reward - final reward for this agent.
     */
    public void result(GameState gs, double reward) {}

    /**
     * Getter for player ID field.
     * @return - this player's ID.
     */
    public final int getPlayerID() {
        return playerID;
    }

    /**
     * Setter for the player ID field
     * @param playerID the player ID of this agent
     * @param allIds all IDs in this game.
     */
    public void setPlayerIDs(int playerID, ArrayList<Integer> allIds) {
        this.playerID = playerID;
        this.allPlayerIDs = allIds;
    }

    /**
     * Getter for seed field.
     * @return - this player's random seed.
     */
    public final long getSeed() {
        return seed;
    }

    public abstract Agent copy();

    public void reset(long seed) {
        this.seed = seed;
    }



    enum ACTION_TYPE
    {
        CITY,
        TRIBE,
        UNIT
    }

    /**
     * Determines the action group (City actions, Unit actions or Tribe actions) at random
     * @param gs current game state
     * @param rnd random number generator
     * @return the list of available actions of a given type (City, Unit or Tribe), at random.
     */
    protected ArrayList<Action> determineActionGroup(GameState gs, Random rnd)
    {
        ArrayList<ACTION_TYPE> availableTypes = new ArrayList<>();

        ArrayList<Action> cityActions = gs.getAllCityActions();
        ArrayList<Action> cityGoodActions = new ArrayList<>();
        for(Action act : cityActions)
            if(!(act.getActionType() == Types.ACTION.DESTROY))
                cityGoodActions.add(act);
        if(!cityGoodActions.isEmpty()) availableTypes.add(ACTION_TYPE.CITY);

        ArrayList<Action> unitActions = gs.getAllUnitActions();
        ArrayList<Action> unitGoodActions = new ArrayList<>();
        for(Action act : unitActions)
            if(!(act.getActionType() == Types.ACTION.DISBAND))
                unitGoodActions.add(act);
        if(!unitActions.isEmpty()) availableTypes.add(ACTION_TYPE.UNIT);

        ArrayList<Action> tribeActions = gs.getTribeActions();
        if(tribeActions.size() > 1) availableTypes.add(ACTION_TYPE.TRIBE); //>1, we need to have something else than EndTurn only.

        if(availableTypes.isEmpty())
        {
            return null;
        }

        int rndIdx = rnd.nextInt(availableTypes.size());
        ACTION_TYPE rootAction = availableTypes.get(rndIdx);
        if(rootAction == ACTION_TYPE.CITY)
        {
            return cityGoodActions;
        }
        if(rootAction == ACTION_TYPE.UNIT)
        {
            return unitGoodActions;
        }

        return tribeActions;
    }


    /**
     * Returns all available actions filtering out Destroy and Disband
     * @param gs current game state
     * @param rnd random number generator
     * @return the list of available actions of a given type (City, Unit or Tribe), at random.
     */
    protected ArrayList<Action> allGoodActions(GameState gs, Random rnd)
    {
        ArrayList<Action> allActions = new ArrayList<>();
        ArrayList<Action> cityActions = gs.getAllCityActions();
        for(Action act : cityActions)
            if(!(act.getActionType() == Types.ACTION.DESTROY))
                allActions.add(act);

        ArrayList<Action> unitActions = gs.getAllUnitActions();
        for(Action act : unitActions)
            if(!(act.getActionType() == Types.ACTION.DISBAND))
                allActions.add(act);

        ArrayList<Action> tribeActions = gs.getTribeActions();
        allActions.addAll(tribeActions);
        return allActions;
    }


    /**
     * Returns the number of actions available for each of the actors, from the perspective of this agent.
     * By default, it's the same as the game state says - but overriding this function allows for pruning analysis.
     */
    public ArrayList<Integer> actionsPerUnit(GameState gs)
    {
        ArrayList<Integer> actionCounts = new ArrayList<>();

        HashMap<Integer, ArrayList<Action>> cityActions = gs.getCityActions();
        for (Integer id : cityActions.keySet()) {
            actionCounts.add(cityActions.get(id).size());
        }

        HashMap<Integer, ArrayList<Action>> unitAcions = gs.getUnitActions();
        for (Integer id : unitAcions.keySet()) {
            actionCounts.add(unitAcions.get(id).size());
        }

        actionCounts.add(gs.getTribeActions().size());
        return actionCounts;
    }

    /**
     * Returns the total number of actions available in a game state, from the perspective of this agent.
     * By default, it's the same as the game state says - but overriding this function allows for pruning analysis.
     */
    public int actionsPerGameState(GameState gs)
    {
        return gs.getAllAvailableActions().size();
    }



}