package players.mcts;

import core.actions.Action;
import core.actions.tribeactions.EndTurn;
import core.game.GameState;
import players.heuristics.StateHeuristic;
import utils.ElapsedCpuTimer;

import java.util.ArrayList;
import java.util.Random;
import static core.Types.ACTION.*;

class SingleTreeNode
{
    private MCTSParams params;

    private SingleTreeNode root;
    private SingleTreeNode parent;
    private SingleTreeNode[] children;
    private double totValue;
    private int nVisits;
    private Random m_rnd;
    private int m_depth;
    private double[] bounds = new double[]{Double.MAX_VALUE, -Double.MAX_VALUE};
    private int fmCallsCount;
    private int playerID;

    private ArrayList<Action> actions;
    private GameState state;

    private GameState rootState;
    private StateHeuristic rootStateHeuristic;

    //From MCTSPlayer
    SingleTreeNode(MCTSParams p, Random rnd, int num_actions, ArrayList<Action> actions, int playerID) {
        this(p, null, rnd, num_actions, actions, null, playerID, null, null);
    }

    private SingleTreeNode(MCTSParams p, SingleTreeNode parent, Random rnd, int num_actions,
                           ArrayList<Action> actions, StateHeuristic sh, int playerID, SingleTreeNode root, GameState state) {
        this.params = p;
        this.fmCallsCount = 0;
        this.parent = parent;
        this.m_rnd = rnd;
        this.actions = actions;
        this.root = root;
        children = new SingleTreeNode[num_actions];
        totValue = 0.0;
        this.playerID = playerID;
        this.state = state;
        if(parent != null) {
            m_depth = parent.m_depth + 1;
            this.rootStateHeuristic = sh;
        }
        else {
            m_depth = 0;
        }

    }

    void setRootGameState(SingleTreeNode root, GameState gs, ArrayList<Integer> allIDs)
    {
        this.state = gs;
        this.root = root;
        this.rootState = gs;
        this.rootStateHeuristic = params.getStateHeuristic(playerID, allIDs);
    }


    void mctsSearch(ElapsedCpuTimer elapsedTimer) {

        double avgTimeTaken;
        double acumTimeTaken = 0;
        long remaining;
        int numIters = 0;

        int remainingLimit = 5;
        boolean stop = false;

        while(!stop){
//            System.out.println("------- " + root.actions.size() + " -------");
            ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();
            SingleTreeNode selected = treePolicy();
            double delta = selected.rollOut();
            backUp(selected, delta);
            numIters++;

            //Stopping condition
            if(params.stop_type == params.STOP_TIME) {
                acumTimeTaken += (elapsedTimerIteration.elapsedMillis()) ;
                avgTimeTaken  = acumTimeTaken/numIters;
                remaining = elapsedTimer.remainingTimeMillis();
                stop = remaining <= 2 * avgTimeTaken || remaining <= remainingLimit;
            }else if(params.stop_type == params.STOP_ITERATIONS) {
                stop = numIters >= params.num_iterations;
            }else if(params.stop_type == params.STOP_FMCALLS)
            {
                stop = fmCallsCount > params.num_fmcalls;
            }
        }
    }

    private SingleTreeNode treePolicy() {

        SingleTreeNode cur = this;

        while (!cur.state.isGameOver() /*&& state.getAllAvailableActions().size() > 1 */ && cur.m_depth < params.ROLLOUT_LENGTH)
        {
            if (cur.notFullyExpanded()) {
                return cur.expand();

            } else {
                cur = cur.uct();
            }
        }

        return cur;
    }

    private int tryForceEnd(GameState state, EndTurn endTurn, int depth)
    {
        boolean willForceEnd = (depth > 0 && (depth % params.FORCE_TURN_END) == 0) && endTurn.isFeasible(state);
        if(!willForceEnd)
            return -1; //Not the time, or not available.

        ArrayList<Action> availableActions = state.getAllAvailableActions();
        int actionIdx = 0;
        while(actionIdx < availableActions.size())
        {
            Action act = availableActions.get(actionIdx);
            if(act.getActionType() == END_TURN)
            {
                //Here's the end turn, return it's index.
                return actionIdx;
            }else actionIdx++;
        }

        //This should not happen, but EndTurn is not available here.
        return -1;
    }

    private SingleTreeNode expand() {

        int bestAction = tryForceEnd(state, new EndTurn(state.getActiveTribeID()), this.m_depth);
        if(bestAction == -1)
        {
            //No turn end, expand
            double bestValue = -1;

            for (int i = 0; i < children.length; i++) {
                double x = m_rnd.nextDouble();
                if (x > bestValue && children[i] == null) {
                    bestAction = i;
                    bestValue = x;
                }
            }
        }

        //Roll the state, create a new node and assign it.
        GameState nextState = state.copy();
        ArrayList<Action> availableActions = m_depth == 0 && params.PRIORITIZE_ROOT ? actions : nextState.getAllAvailableActions();
        ArrayList<Action> nextActions = advance(nextState, availableActions.get(bestAction), true);
        SingleTreeNode tn = new SingleTreeNode(params, this, this.m_rnd, nextActions.size(),
                null, rootStateHeuristic, this.playerID, this.m_depth == 0 ? this : this.root, nextState);
        children[bestAction] = tn;
        return tn;
    }



    private ArrayList<Action> advance(GameState gs, Action act, boolean computeActions)
    {
        gs.advance(act, computeActions);
        root.fmCallsCount++;
        return gs.getAllAvailableActions();
    }


    private SingleTreeNode uct() {

        SingleTreeNode selected;
        boolean IamMoving = (state.getActiveTribeID() == this.playerID);
        int bestAction = tryForceEnd(state, new EndTurn(state.getActiveTribeID()), this.m_depth);
        if(bestAction == -1)
        {
            //No end turn, use uct.
            double[] vals = new double[this.children.length];
            for(int i = 0; i < this.children.length; ++i)
            {
                SingleTreeNode child = children[i];

                double hvVal = child.totValue;
                double childValue =  hvVal / (child.nVisits + params.epsilon);
                childValue = normalise(childValue, bounds[0], bounds[1]);

                double uctValue = childValue +
                        params.K * Math.sqrt(Math.log(this.nVisits + 1) / (child.nVisits + params.epsilon));

                uctValue = noise(uctValue, params.epsilon, this.m_rnd.nextDouble());     //break ties randomly
                vals[i] = uctValue;
            }

            int which = -1;
            double bestValue = IamMoving ? -Double.MAX_VALUE : Double.MAX_VALUE;
            for(int i = 0; i < vals.length; ++i) {
                if ((IamMoving && vals[i] > bestValue) || (!IamMoving && vals[i] < bestValue)){
                    which = i;
                    bestValue = vals[i];
                }
            }

            if (which == -1)
            {
                //if(this.children.length == 0)
                System.out.println("Warning! couldn't find the best UCT value " + which + " : " + this.children.length + " " +
                //throw new RuntimeException("Warning! couldn't find the best UCT value " + which + " : " + this.children.length + " " +
                        bounds[0] + " " + bounds[1]);
                System.out.print(this.m_depth + ", AmIMoving? " + IamMoving + ";");
                for(int i = 0; i < this.children.length; ++i)
                    System.out.printf(" %f2", vals[i]);
                System.out.println("; selected: " + which);

                which = m_rnd.nextInt(children.length);
            }

            selected = children[which];

//            System.out.print(this.m_depth + ", AmIMoving? " + IamMoving + ";");
//            for(int i = 0; i < this.children.length; ++i)
//                System.out.printf(" %f2", vals[i]);
//            System.out.println("; selected: " + which);

        }else
        {
            selected = children[bestAction];
        }

        //Roll the state. This is closed loop, we don't advance the state. We can't do open loop here because the
        // number of actions available on a state depend on the state itself, and random events triggered by multiple
        // runs over the same tree node would have different outcomes (i.e Examine ruins).
        //advance(state, actions.get(selected.childIdx), true);

        root.fmCallsCount++;

        return selected;
    }

    private double rollOut()
    {
        if(params.ROLOUTS_ENABLED) {
            GameState rolloutState = state.copy();
            int thisDepth = this.m_depth;
            while (!finishRollout(rolloutState, thisDepth)) {
                EndTurn endTurn = new EndTurn(rolloutState.getActiveTribeID());
                int bestAction = tryForceEnd(rolloutState, endTurn, thisDepth);
                Action next = (bestAction != -1) ? endTurn : rolloutState.getAllAvailableActions().get(m_rnd.nextInt(rolloutState.getAllAvailableActions().size()));
                advance(rolloutState, next, true);
                thisDepth++;
            }
            return normalise(this.rootStateHeuristic.evaluateState(root.rootState, rolloutState), 0, 1);
        }

        return normalise(this.rootStateHeuristic.evaluateState(root.rootState, this.state), 0, 1);
    }

    private boolean finishRollout(GameState rollerState, int depth)
    {
        if (depth >= params.ROLLOUT_LENGTH)      //rollout end condition.
            return true;

        //end of game
        return rollerState.isGameOver();
    }


    private void backUp(SingleTreeNode node, double result)
    {
        SingleTreeNode n = node;
        while(n != null)
        {
            n.nVisits++;
            n.totValue += result;
            if (result < n.bounds[0]) {
                n.bounds[0] = result;
            }
            if (result > n.bounds[1]) {
                n.bounds[1] = result;
            }
            n = n.parent;
        }
    }


    int mostVisitedAction() {
        int selected = -1;
        double bestValue = -Double.MAX_VALUE;
        boolean allEqual = true;
        double first = -1;

        for (int i=0; i<children.length; i++) {

            if(children[i] != null)
            {
                if(first == -1)
                    first = children[i].nVisits;
                else if(first != children[i].nVisits)
                {
                    allEqual = false;
                }

                double childValue = children[i].nVisits;
                childValue = noise(childValue, params.epsilon, this.m_rnd.nextDouble());     //break ties randomly
                if (childValue > bestValue) {
                    bestValue = childValue;
                    selected = i;
                }
            }
        }

        if (selected == -1)
        {
            selected = 0;
        }else if(allEqual)
        {
            //If all are equal, we opt to choose for the one with the best Q.
            selected = bestAction();
        }

        return selected;
    }

    private int bestAction()
    {
        int selected = -1;
        double bestValue = -Double.MAX_VALUE;

        for (int i=0; i<children.length; i++) {

            if(children[i] != null) {
                double childValue = children[i].totValue / (children[i].nVisits + params.epsilon);
                childValue = noise(childValue, params.epsilon, this.m_rnd.nextDouble());     //break ties randomly
                if (childValue > bestValue) {
                    bestValue = childValue;
                    selected = i;
                }
            }
        }

        if (selected == -1)
        {
            System.out.println("Unexpected selection!");
            selected = 0;
        }

        return selected;
    }


    private boolean notFullyExpanded() {
        for (SingleTreeNode tn : children) {
            if (tn == null) {
                return true;
            }
        }

        return false;
    }

    private double normalise(double a_value, double a_min, double a_max)
    {
        if(a_min < a_max)
            return (a_value - a_min)/(a_max - a_min);
        else    // if bounds are invalid, then return same value
            return a_value;
    }

    private double noise(double input, double epsilon, double random)
    {
        return (input + epsilon) * (1.0 + epsilon * (random - 0.5));
    }

}
