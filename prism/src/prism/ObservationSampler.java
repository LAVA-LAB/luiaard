package prism;


import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Vector;
import java.util.List;
import java.util.BitSet;
import java.util.Map;
import java.util.Iterator;
import java.util.Map.Entry;
import common.Interval;
import explicit.Distribution;
import explicit.IMDP;
import explicit.IMDPModelChecker;
import explicit.IMDPSimple;
import explicit.MDP;
import explicit.MDPFromMDPAndMDStrategy;
import explicit.ModelModelGenerator;
import explicit.MinMax;
import explicit.rewards.MDPRewardsSimple;
import explicit.ModelCheckerResult;
import explicit.MDPSimple;
import parser.ast.ModulesFile;
import parser.ast.Property;
import parser.ast.PropertiesFile;
import parser.ast.Expression;
import parser.State;
import simulator.ModulesFileModelGenerator;
import simulator.PathFull;
import simulator.PathOnTheFly;
import simulator.RandomNumberGenerator;
import simulator.SimulatorEngine;
import simulator.method.CIwidth;
import simulator.method.SimulationMethod;
import strat.MDStrategy;
import strat.MDStrategyArray;
import strat.MRStrategy;
import strat.Strategy;
import explicit.MDPModelChecker;
import prism.Result;

public class ObservationSampler {

    private Prism prism;
	private MDP<Double> sul;

	private HashMap<TransitionTriple, Integer> samplesMap;
	private HashMap<StateActionPair, Integer> sampleSizeMap;
	private HashMap<StateActionPair, Integer> accumulatedSamples;

	private HashSet<Integer> terminatingStates;
	private SimulatorEngine sim;

	private HashSet<TransitionTriple> transitionsOfInterest;

	private boolean DEBUG = false;

	private ModulesFile modulesFileIMDP;
    private ModulesFile modulesFileMDP;


    public ObservationSampler(Prism prism, MDP<Double> sul, HashSet<Integer> terminatingStates) throws PrismException{
		this.sampleSizeMap = new HashMap<>();
		this.samplesMap = new HashMap<>();
		this.accumulatedSamples = new HashMap<>();
		this.transitionsOfInterest = new HashSet<>();
		this.terminatingStates = terminatingStates;

		this.prism = prism;
		this.sul = sul;

		//load model into simulator
		this.prism.loadModelIntoSimulator();
		this.sim = this.prism.getSimulator();
		sim.loadReachableStates(this.sul.getStatesList());
    }

	public void setModulesFiles( ModulesFile modulesFileMDP, ModulesFile modulesFileIMDP) {
		this.modulesFileIMDP = modulesFileIMDP;
		this.modulesFileMDP = modulesFileMDP;
	}

	public void setTerminatingStates(HashSet<Integer> set) {
		this.terminatingStates = set;
	}

	public void setTransitionsOfInterest(HashSet<TransitionTriple> set) {
		this.transitionsOfInterest = set;
	}
	
	public int getIndexFromState(State s) {
		return this.prism.getSimulator().getReachableStateIndex(s);
	}

	public String getActionString(MDP<Double> mdp, int s, int i) {
		String action = (String) mdp.getAction(s,i);
		if (action == null) {
			action = "_empty";
		}
		return action;
	}

	public HashMap<TransitionTriple, Integer> getSamplesMap() {
		return this.samplesMap;
	}

	public HashMap<StateActionPair, Integer> getSampleSizeMap() {
		return this.sampleSizeMap;
	}


	public void sampleAll(int k) throws PrismException {
		resetObservationSequence();
		List<State> states = sul.getStatesList();
		for (State s : states) {
			if (!this.terminatingStates.contains(getIndexFromState(s))) {
				int numChoices = sul.getNumChoices(getIndexFromState(s));
				for (int a = 0; a < numChoices; a++) {
					sim.createNewOnTheFlyPath();
					sim.initialisePath(s);
					for (int i = 0; i < k; i++) {
						boolean step = sim.automaticTransitionByChoice(i,this.terminatingStates);
						if (step) {
							PathOnTheFly path = (PathOnTheFly) sim.getPath();
							parseLastStep(path);
							sim.backtrackTo(0);
						}
					}
				}
			}
		}
	}



	public int simulate(long size, Strategy strat) throws PrismException {
		int samples = 0;
		resetObservationSequence();
		sim.createNewOnTheFlyPath();
		sim.loadStrategy(strat);
		sim.initialisePath(null);
		while (samples <= size) {
			boolean step = sim.automaticTransition(this.terminatingStates);
			if (step) {
				PathOnTheFly path = (PathOnTheFly) sim.getPath();
				parseLastStep(path);
				samples += 1;
			}
			else {
				sim.createNewOnTheFlyPath();
				sim.loadStrategy(strat);
				sim.initialisePath(null);
			}

		}
		return samples;
	}


	public int simulateEpisode(int horizon, Strategy strat) throws PrismException {
		int number_of_samples = 0;
		sim.createNewOnTheFlyPath();
		sim.loadStrategy(strat);
		sim.initialisePath(null);
		while (number_of_samples <= horizon) {
			boolean step = sim.automaticTransition(this.terminatingStates);
			if (! step) {
				// could not execute a new action
				break;
			}
			PathOnTheFly path = (PathOnTheFly) sim.getPath();
			parseLastStep(path);
			number_of_samples += 1;
		}
		return number_of_samples;
	}

	private void parseLastStep(PathOnTheFly path) {
		State s = path.getPreviousState();
		String a = path.getPreviousActionString();
		State successor = path.getCurrentState();
		parseStep(s, a, successor);
	}

	public boolean collectedEnoughSamples() {
		return this.collectedEnoughSamples(1);
	}

	public boolean collectedEnoughSamples(float ratio) {
		for (Map.Entry<StateActionPair, Integer> entry: this.sampleSizeMap.entrySet()){
//			if (this.accumulatedSamples.containsKey(entry.getKey())) {
			if (entry.getValue() >=  ratio * this.accumulatedSamples.getOrDefault(entry.getKey(), 1) ) {
				return true;
			}
		}
		return false;
	}
	public int getTotalSamples() {
		int total = 0;
		for (Map.Entry<StateActionPair, Integer> entry: this.accumulatedSamples.entrySet()){
			total += entry.getValue();
		}
		return total;
	}

	// TODO: sample with maxmax or minmin strategy (i.e. let adversary help)
	public int simulateWithOptimisticRobustStrategy(long size, String propertyString, IMDP<Double> estimate, String robustSpec) throws PrismException {
		resetObservationSequence();
		int samples = 0;

		MinMax minMax = MinMax.max().setMinUnc(false);
		IMDPModelChecker mc = new IMDPModelChecker(this.prism);
		mc.setGenStrat(true);
		mc.setErrorOnNonConverge(false);
		PropertiesFile pf = prism.parsePropertiesString(robustSpec);
		ModulesFileModelGenerator<?> modelGen = ModulesFileModelGenerator.create(modulesFileIMDP, this.prism);
		mc.setModelCheckingInfo(modelGen, pf, modelGen);
		Expression exprTarget = this.prism.parsePropertiesString(propertyString).getProperty(0);
		//BitSet target = mc.check(estimate, exprTarget).getBitSet();
		//ModelCheckerResult result = mc.computeReachRewards(estimate, rewards, target, minMax);
		//ModelCheckerResult result = mc.computeReachProbs(estimate, target, minMax);
		Result result = mc.check(estimate, exprTarget);
		MDStrategy strat = (MDStrategy) result.getStrategy();

		sim.loadStrategy(strat);
		long i = size;
		sim.createNewOnTheFlyPath();
		sim.initialisePath(null);
		while (i > 0) {
			boolean step = sim.automaticTransition(this.terminatingStates);
			if (step) {
				PathOnTheFly path = (PathOnTheFly) sim.getPath();
				parseLastStep(path);
				samples += 1;
				i -= 1;
			}
			else {
				sim.createNewOnTheFlyPath();
				sim.initialisePath(null);
			}
		}
		return samples;
	}


	public int simulateWithPessimisticRobustStrategy(long size, String propertyString, IMDP<Double> estimate, String robustSpec) throws PrismException {
		resetObservationSequence();
		int samples = 0;

		MinMax minMax = MinMax.max().setMinUnc(true);
		IMDPModelChecker mc = new IMDPModelChecker(this.prism);
		mc.setGenStrat(true);
		mc.setErrorOnNonConverge(false);
		PropertiesFile pf = prism.parsePropertiesString(robustSpec);
		ModulesFileModelGenerator<?> modelGen = ModulesFileModelGenerator.create(modulesFileIMDP, this.prism);
		mc.setModelCheckingInfo(modelGen, pf, modelGen);
		Expression exprTarget = this.prism.parsePropertiesString(propertyString).getProperty(0);
		//BitSet target = mc.check(estimate, exprTarget).getBitSet();
		//ModelCheckerResult result = mc.computeReachRewards(estimate, rewards, target, minMax);
		//ModelCheckerResult result = mc.computeReachProbs(estimate, target, minMax);
		Result result = mc.check(estimate, exprTarget);
		MDStrategy strat = (MDStrategy) result.getStrategy();

		sim.loadStrategy(strat);
		long i = size;
		sim.createNewOnTheFlyPath();
		sim.initialisePath(null);
		while (i > 0) {
			boolean step = sim.automaticTransition(this.terminatingStates);
			if (step) {
				PathOnTheFly path = (PathOnTheFly) sim.getPath();
				parseLastStep(path);
				samples += 1;
				i -= 1;
			}
			else {
				sim.createNewOnTheFlyPath();
				sim.initialisePath(null);
			}
		}
		return samples;
	}


	public void parseStep(State s, String a, State sprime) {
		int currentState = getIndexFromState(s);
		int successorState = getIndexFromState(sprime);
		String action = a.replace("[", "");
		action = action.replace("]", "");

		//System.out.println("("+currentState+", " + action + ", " + successorState + ")");

		StateActionPair sa = new StateActionPair(currentState, action);
		TransitionTriple t = new TransitionTriple(currentState, action, successorState);
		
		//only process t if it is a transition of 0 < p < 1
		if (transitionsOfInterest.contains(t)) {
			if (this.samplesMap.containsKey(t)) {
				this.samplesMap.put(t, this.samplesMap.get(t)+1);
			}
			else {
				this.samplesMap.put(t, 1);
			}

			if (this.sampleSizeMap.containsKey(sa)) {
				this.sampleSizeMap.put(sa, this.sampleSizeMap.get(sa)+1);
			}
			else {
				this.sampleSizeMap.put(sa, 1);
			}
		}

	}

	

    
	/**
	 * Reset observation sequence
	 */
    public void resetObservationSequence() {
		incrementAccumulatedSamples();
		this.sampleSizeMap.clear();
		this.samplesMap.clear();
    }

	private void incrementAccumulatedSamples() {
		this.sampleSizeMap.forEach((sa, counter) -> {
			this.accumulatedSamples.put(sa, this.accumulatedSamples.getOrDefault(sa, 0) + counter);
		});
	}


	public int simulateWithRewardStrategy(long size, String propertyString, HashMap<TransitionTriple, Interval<Double>> intervalsMap, IMDP<Double> estimate) throws PrismException
	{
		//ArrayList<TransitionTriple> observations = new ArrayList<>();
		resetObservationSequence();
		int samples = 0;
		// Load model into simulator
		//this.prism.loadModelIntoSimulator();
		//SimulatorEngine sim = prism.getSimulator();
		
		long startTime = System.currentTimeMillis();
		int numStates = this.sul.getNumStates();
		MDPRewardsSimple<Interval<Double>> rewards = new MDPRewardsSimple<>(numStates);
		rewards.setEvaluator(Evaluator.createForDoubleIntervals());
		for (int s = 0; s < numStates; s++) 
		{
			int numChoices = this.sul.getNumChoices(s);
			for (int i = 0; i < numChoices; i++) 
			{
				String action = getActionString(this.sul, s,i);
				int count = 0;
				double sum = 0;
				for (int successor = 0; successor < numStates; successor++) {
					Interval<Double> interval = intervalsMap.get(new TransitionTriple(s, action, successor));
					if (interval != null) {
						count += 1;
						double width = ((Double) interval.getUpper()) - ((Double) interval.getLower());
						sum += width;
					}
				} 
				double rank = sum / count;
				Interval<Double> interval = new Interval(rank,rank);
				rewards.addToTransitionReward(s, i, interval);
			}
		}
		
		
		//MinMax minMax = MinMax.max().setMinUnc(true);
		MinMax minMax = MinMax.max().setMinUnc(false);
		IMDPModelChecker mc = new IMDPModelChecker(this.prism);
		mc.setGenStrat(true);
		mc.setErrorOnNonConverge(false);
		Expression exprTarget = this.prism.parsePropertiesString(propertyString).getProperty(0);
		BitSet target = mc.checkExpression(estimate, exprTarget, null).getBitSet();
		ModelCheckerResult result = mc.computeReachRewards(estimate, rewards, target, minMax);
		MDStrategy strat = (MDStrategy) result.strat;
		long stopTime = System.currentTimeMillis();
		//System.out.println("Reward strategy computation time = " + (stopTime - startTime)/1000);

		sim.loadStrategy(strat);
		long i = size;
		sim.createNewOnTheFlyPath();
		sim.initialisePath(null);
		while (i > 0) {
			boolean step = sim.automaticTransition(this.terminatingStates);
			if (step) {
				PathOnTheFly path = (PathOnTheFly) sim.getPath();
				parseLastStep(path);
				samples += 1;
				i -= 1;
			}
			else {
				sim.createNewOnTheFlyPath();
				sim.initialisePath(null);
			}
		}
		return samples;
	}







}




/**
 *   OLD STUFF DELETE LATER
 * 
 * 
 * 
 * 
	public int simulateWithUniformStrategy(long size) throws PrismException
	{
		int samples = 0;
		resetObservationSequence();
		//ArrayList<TransitionTriple> observations = new ArrayList<>();
		// Load model into simulator
		//this.prism.loadModelIntoSimulator();
		//SimulatorEngine sim = prism.getSimulator();
		//sim.loadReachableStates(this.sul.getStatesList());
		int numStates = this.sul.getNumStates();
		// Create a uniform strategy
		if (this.uniformStrat == null) {
			long startTime = System.currentTimeMillis();
			MRStrategy strat = new MRStrategy(this.sul);
			for (int s = 0; s < numStates; s++) {
				int numChoices = this.sul.getNumChoices(s);
				for (int i = 0; i < numChoices; i++) {
					strat.setActionProbability(s, getActionString(this.sul, s, i), 1.0 / numChoices);
				}
			}
			this.uniformStrat = strat;
			long stopTime = System.currentTimeMillis();
			//System.out.println("Uniform strategy computation time = " + (stopTime - startTime)/1000);
		}
		sim.loadStrategy(this.uniformStrat);
		long i = size;
		//int steps = 0;
		sim.createNewOnTheFlyPath();
		sim.initialisePath(null);
		while (i > 0) {
			//sim.createNewPath();
			
			//sim.automaticTransitions(100*numStates, true, this.terminatingStates);
			boolean step = sim.automaticTransition(this.terminatingStates);
			if (step) {
				PathOnTheFly path = (PathOnTheFly) sim.getPath();
				State s = path.getPreviousState();
				String a = path.getPreviousActionString();
				State successor = path.getCurrentState();

				parseStep(s,a,successor);
				samples += 1;
				i -= 1;
			}
			else {
				sim.createNewOnTheFlyPath();
				sim.initialisePath(null);
			}

		}
		return samples;
	}

 
	public int simulateWithRankedStrategy(long size, HashMap<TransitionTriple, Interval<Double>> intervalsMap) throws PrismException
	{
		resetObservationSequence();
		int samples = 0;
		// Load model into simulator
		//this.prism.loadModelIntoSimulator();
		//SimulatorEngine sim = prism.getSimulator();
		//sim.loadReachableStates(this.sul.getStatesList());

		// Create a uniform strategy
		long startTime = System.currentTimeMillis();
		MRStrategy strat = new MRStrategy(this.sul);
		int numStates = this.sul.getNumStates();
		for (int s = 0; s < numStates; s++) {
			int numChoices = this.sul.getNumChoices(s);
			for (int i = 0; i < numChoices; i++) {
				String action = getActionString(this.sul, s,i);
				int count = 0;
				double sum = 0;
				for (int successor = 0; successor < numStates; successor++) {
					Interval<Double> interval = intervalsMap.get(new TransitionTriple(s, action, successor));
					if (interval != null) {
						count += 1;
						double width = ((Double) interval.getUpper()) - ((Double) interval.getLower());
						sum += width;
					}
				} 
				double rank = sum / count;
				strat.setActionProbability(s, getActionString(this.sul, s, i), rank);
			}
		}
		long stopTime = System.currentTimeMillis();
		if (DEBUG) {System.out.println("Ranked strategy computation time = " + (stopTime - startTime)/1000);}

		// Load strategy into simulator
		if (DEBUG) {System.out.println("loading strat in sim");}
		sim.loadStrategy(strat);
		long i = size;
		if (DEBUG) {System.out.println("create new path");}
		sim.createNewOnTheFlyPath();
		sim.initialisePath(null);
		while (i > 0) {
			boolean step = sim.automaticTransition(this.terminatingStates);
			if (step) {
				
				PathOnTheFly path = (PathOnTheFly) sim.getPath();
				State s = path.getPreviousState();
				String a = path.getPreviousActionString();
				State successor = path.getCurrentState();

				parseStep(s,a,successor);
				samples += 1;
				i -= 1;
				if (DEBUG) { System.out.println("step");}
			}
			else {
				sim.createNewOnTheFlyPath();
				sim.initialisePath(null);
			}
		}
		//this.observationSequence.addAll(observations);
        //System.out.println(this.observationSequence);
        //return observations;
		return samples;
	}
	


*/






/*





 * 
 * 
 * 
 * 
 * 
 */