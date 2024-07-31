package players.oep;

import core.Types;
import core.actions.Action;
import core.actions.cityactions.CityAction;
import core.actions.unitactions.UnitAction;
import core.actors.Actor;
import core.actors.City;
import core.actors.units.Unit;
import core.game.GameState;
import players.Agent;
import players.heuristics.StateHeuristic;
import utils.ElapsedCpuTimer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;


public class OEPAgent extends Agent {

    private Random m_rnd;
    private StateHeuristic heuristic;
    private OEPParams params;

    private Individual bestIndividual;
    private int fmCallsCount;
    private int fmCallsRun;
    private GameState root;


    public OEPAgent(long seed, OEPParams params) {
        super(seed);
        m_rnd = new Random(seed);
        this.params = params;
    }

    @Override
    public Action act(GameState gs, ElapsedCpuTimer ect) {
        double avgTimeTaken;
        double acumTimeTaken = 0;
        long remaining;
        int numIters = 0;
        fmCallsCount = 0;

        int remainingLimit = 5;
        boolean stop = false;


        //create a population of individuals defined in param
        ArrayList<Individual> population = new ArrayList<>();
        for(int i = 0; i < params.POP_SIZE; i++){
            population.add(randomActions(gs.copy()));
        }

        this.heuristic = params.getStateHeuristic(playerID, allPlayerIDs);
        fmCallsRun = 0;

        this.bestIndividual = null;
        this.root = gs.copy();

        //keep going until time limit gone
        while(!stop){

            numIters ++;
            ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();
            fmCallsRun = 0;

            // rate each individual and sort them
            for(Individual individual : population){
                individual.setValue(eval( individual));
            }
            Collections.sort(population, Collections.reverseOrder());
            this.bestIndividual = population.get(population.size() - 1);

            //Kill the amount of the population that needs to die
            int amount =  (int)(population.size() * params.KILL_RATE);
            for(int i = 0; i < amount; i++){
                population.remove(0);
            }

            if(((fmCallsCount > params.num_fmcalls) || ((numIters == 1) && (fmCallsCount >= (0.9 * params.num_fmcalls))))&& params.stop_type == params.STOP_FMCALLS){
                //over limit and needs to chose individual and return
                break;
            }

            //perform uniform crossover
            boolean even = false;
            if(population.size() % 2 == 0){
                even = true;
            }
            Individual person1 = null;
            if(!even){
                person1 = population.get(m_rnd.nextInt(population.size()));
                population.remove(person1);
            }

            population = procreate(gs.copy(), population);

            if(!even){
                population.add(crossover(gs.copy(), person1, population.get(m_rnd.nextInt(population.size()))));
            }

            if(((fmCallsCount > params.num_fmcalls)  || ((numIters == 1) && (fmCallsCount >= (0.9 * params.num_fmcalls)))) && params.stop_type == params.STOP_FMCALLS){
                //over limit and needs to chose individual and return
                break;
            }

            population = shiftPop(gs.copy(),population);

            if(params.stop_type == params.STOP_TIME) {
                acumTimeTaken += (elapsedTimerIteration.elapsedMillis()) ;
                avgTimeTaken  = acumTimeTaken/numIters;
                remaining = ect.remainingTimeMillis();
                stop = remaining <= 2 * avgTimeTaken || remaining <= remainingLimit;
            }else if(params.stop_type == params.STOP_ITERATIONS) {
                stop = numIters >= params.num_iterations;
            }else if(params.stop_type == params.STOP_FMCALLS){
                stop = (fmCallsCount > params.num_fmcalls) || (fmCallsRun > (params.num_fmcalls - fmCallsCount));
            }

        }
        //System.out.println(numIters);
        return  bestIndividual.returnNext();


    }

    @Override
    public Agent copy() {
        return null;
    }

    private Individual randomActions(GameState gs){
        ArrayList<Action> individual = new ArrayList<>();
        while ((!gs.isGameOver() && (gs.getActiveTribeID() == getPlayerID())) && (individual.size() < (params.NODE_SIZE-1))){
            ArrayList<Action> allAvailableActions = this.allGoodActions(gs, m_rnd);
            Action a = allAvailableActions.get(m_rnd.nextInt(allAvailableActions.size()));
            if(!(a.getActionType() == Types.ACTION.END_TURN)){
                advance(gs, a);
                individual.add(a);
            }else if(allAvailableActions.size() == 1){
                advance(gs, a);
                individual.add(a);
            }
        }
        //if one less then max node size
        if(individual.size() == (params.NODE_SIZE-1) && (individual.get((params.NODE_SIZE-2)).getActionType() != Types.ACTION.END_TURN)){
            ArrayList<Action> allAvailableActions = this.allGoodActions(gs, m_rnd);
            Action end = null;
            for(Action a : allAvailableActions){
                if(a.getActionType() == Types.ACTION.END_TURN){
                    end = a;
                    break;
                }
            }
            individual.add(end);
        }

        Individual in = new Individual(individual);
        in.setGs(gs.copy());
        return in;
    }

    // must be given a even no. of population
    private ArrayList<Individual> procreate (GameState gs, ArrayList<Individual> population){
        ArrayList<Individual> group1 = new ArrayList<>();
        ArrayList<Individual> group2 = new ArrayList<>();

        while(!population.isEmpty()){
            Individual[] winners = tournamentSelection(population);

            group1.add(winners[0]);
            group2.add(winners[1]);

            population.remove(winners[0]);
            population.remove(winners[1]);

        }


        for(int i = 0; i < group1.size(); i++){
            population.add(crossover(gs.copy(), group1.get(i), group2.get(i)));
        }
        return population;
    }

    private Individual[] tournamentSelection(ArrayList<Individual> pop){
        if(pop.size() < params.TOURNAMENT_SIZE){
            if(pop.size() == 1){return new Individual[]{pop.get(0), pop.get(0)};}
            Collections.sort(pop);
            return new Individual[]{pop.get(0), pop.get(1)};
        }
        ArrayList<Individual> tournament1  =  new ArrayList<>();
        while(tournament1.size() < params.TOURNAMENT_SIZE){
            Individual temp = pop.get(m_rnd.nextInt(pop.size()));
            if(!(tournament1.contains(temp))){tournament1.add(temp);}
        }
        Collections.sort(tournament1);

        ArrayList<Individual> tournament2  =  new ArrayList<>();
        while(tournament2.size() < params.TOURNAMENT_SIZE){
            Individual temp = pop.get(m_rnd.nextInt(pop.size()));
            if(!(tournament2.contains(temp))){tournament2.add(temp);}
        }
        Collections.sort(tournament2);

        return new Individual[]{tournament1.get(0), tournament2.get(0)};
    }

    //method to perform uniform crossover on two individuals
    private Individual crossover(GameState clone, Individual individual1, Individual individual2){
        ArrayList<Action> in1 = individual1.getActions();
        ArrayList<Action> in2 = individual2.getActions();

        ArrayList<Action> child = new ArrayList<>();
        //if both individuals are of the same size

        boolean ind1 = true;
        boolean sameSize = false;
        int smallSize = in1.size();
        if(smallSize > in2.size()){
            ArrayList<Action> temp = in1;
            in1 = in2;
            in2 = temp;
            smallSize = in1.size();
        }
        else if(smallSize == in2.size()){
            sameSize = true;
        }
        smallSize --;
        if(!sameSize && !in1.isEmpty()){in1.remove(in1.size() - 1);}
        int in1amount =(int)(in1.size() / 2);
        int in2amount = in1.size() - in1amount;
        for(int i = 0; i < in2.size(); i++){
            if(i >= smallSize){
                child.add(in2.get(i));
            }else{
                if(in1amount == 0){
                    child.add(in2.get(i));
                }else if(in2amount == 0){
                    child.add(in1.get(i));
                }else{
                    int temp = m_rnd.nextInt(100);
                    if(temp < 50){
                        child.add(in1.get(i));
                        in1amount--;
                    }else{
                        child.add(in2.get(i));
                        in2amount--;
                    }
                }
            }
        }
        Individual in = repair(clone, child);
        return in;
    }
    //repair an individual if actions can't be performed with a random action
    private Individual repair(GameState gs, ArrayList<Action> child){
        ArrayList<Action> repairedChild = new ArrayList<>();
        boolean mutated = false;
        for(int a = 0 ;a < child.size(); a ++) {
            if(!(gs.getActiveTribeID() == getPlayerID())){
                Individual in = new Individual(repairedChild);
                in.setGs(gs.copy());
                return in;
            }
            int chance = m_rnd.nextInt((int)(params.MUTATION_RATE * 100));
            if((m_rnd.nextInt(100) < chance) && !mutated){
                Action ac = mutation(gs.copy());
                advance(gs, ac);
                repairedChild.add(ac);
                mutated = true;
            }else{
                GameState copy = gs.copy();
                boolean added = false;
                try {
                    boolean done = checkActionFeasibility(child.get(a), gs.copy());
                    if (!done) {
                        ArrayList<Action> allAvailableActions = this.allGoodActions(gs.copy(), m_rnd);
                        Action ac = allAvailableActions.get(m_rnd.nextInt(allAvailableActions.size()));
                        advance(gs,ac);
                        repairedChild.add(ac);
                        added = true;
                    } else {
                        repairedChild.add(child.get(a));
                        added = true;
                        advance(gs, child.get(a));
                    }
                } catch (Exception e) {
                    if(added){repairedChild.remove(repairedChild.size()-1);}
                    gs = copy;

                }
            }
        }

        if(!(gs.getActiveTribeID() == getPlayerID())){
            Individual in = new Individual(repairedChild);
            in.setGs(gs.copy());
            return in;
        }

        ArrayList<Action> allAvailableActions = this.allGoodActions(gs, m_rnd);
        Action end = null;
        for(Action a : allAvailableActions){
            if(a.getActionType() == Types.ACTION.END_TURN){
                end = a;
                break;
            }
        }
        repairedChild.add(end);

        Individual in = new Individual(repairedChild);
        in.setGs(gs);
        return in;
    }

    //give a random possible move as a mutation
    private Action mutation(GameState gs){
        ArrayList<Action> allAvailableActions = this.allGoodActions(gs, m_rnd);
        return  allAvailableActions.get(m_rnd.nextInt(allAvailableActions.size()));
    }

    private ArrayList<Individual> shiftPop(GameState gs, ArrayList<Individual> population){
        ArrayList<Individual> newPop = new ArrayList<>();

        shift(gs.copy(), population.get(population.size()-1));
        newPop.add(population.get(population.size()-1));

        for(int i = 1; i < (params.POP_SIZE/2); i++){
            Individual ind = mutateInd(population.get(population.size()-1), gs.copy());
            newPop.add(ind);
        }

        for(int i = newPop.size(); i < params.POP_SIZE; i++){
            newPop.add(randomActions(gs.copy()));
        }

        return newPop;
    }

    private void shift(GameState gs, Individual individual){
        GameState clone = gs.copy();
        individual.shift();

        boolean feasible = true;
        int j = 0;
        while(feasible && j < individual.getActions().size())
        {
            Action act = individual.getActions().get(j);
            feasible = checkActionFeasibility(act, gs);
            if(feasible)
            {
                advance(gs, act);
                j++;
            }
        }

        int i = j;
        while((!gs.isGameOver() && (gs.getActiveTribeID() == getPlayerID())) && i < params.NODE_SIZE)
        {
            ArrayList<Action> allAvailableActions = this.allGoodActions(gs.copy(), m_rnd);
            Action ac = allAvailableActions.get(m_rnd.nextInt(allAvailableActions.size()));
            individual.getActions().add(ac);
            advance(gs, ac);
            i++;
        }

        //Eval individual
        double score = heuristic.evaluateState(clone,gs);
        individual.setValue(score);
        individual.setGs(gs.copy());
    }

    private Individual mutateInd(Individual individual, GameState gs){
        ArrayList<Action> child = new ArrayList<>();

        for(int a = 0 ;a < individual.getActions().size(); a ++) {
            if (!(gs.getActiveTribeID() == getPlayerID())) {
                Individual in = new Individual(child);
                in.setGs(gs.copy());
                return in;
            }
            int chance = m_rnd.nextInt((int) (params.MUTATION_RATE * 100));
            if ((m_rnd.nextInt(100) < chance) ) {
                Action ac = mutation(gs.copy());
                advance(gs, ac);
                child.add(ac);
            }else{
                if(checkActionFeasibility(individual.getActions().get(a), gs.copy())){
                    advance(gs,individual.getActions().get(a));
                    child.add(individual.getActions().get(a));
                }
                else{
                    ArrayList<Action> allAvailableActions = this.allGoodActions(gs.copy(), m_rnd);
                    Action ac = allAvailableActions.get(m_rnd.nextInt(allAvailableActions.size()));
                    advance(gs,ac);
                    child.add(ac);
                }
            }
        }

        Individual in = new Individual(child);
        in.setGs(gs.copy());
        return in;
    }

    public double eval( Individual actionSet){
        GameState gs = actionSet.getGs();
        return heuristic.evaluateState(this.root.copy(),gs.copy());
        //return heuristic.evaluateState(gs.copy());
    }

    private boolean checkActionFeasibility(Action a, GameState gs)
    {
        if(gs.isGameOver())
            return false;

        if(a instanceof UnitAction)
        {
            UnitAction ua = (UnitAction)a;
            int unitId = ua.getUnitId();
            Actor act = gs.getActor(unitId);
            if(!(act instanceof Unit) || act.getTribeId() != gs.getActiveTribeID())
                return false;
        }else if (a instanceof CityAction)
        {
            CityAction ca = (CityAction)a;
            int cityId = ca.getCityId();
            Actor act = gs.getActor(cityId);
            if(!(act instanceof City) || act.getTribeId() != gs.getActiveTribeID())
                return false;
        }

        boolean feasible = false;
        try{
            feasible = a.isFeasible(gs);
        }catch (Exception e) { }

        return feasible;
    }

    private void advance(GameState gs, Action move){
        this.fmCallsCount++;
        this.fmCallsRun++;
        gs.advance(move,true);
    }
}