package prism;

import java.util.*;
import java.util.Map.Entry;
import explicit.IMDP;
import explicit.IMDPSimple;
import explicit.MDP;
import common.Interval;
import explicit.Distribution;
import explicit.IMDPModelChecker;
import parser.ast.PropertiesFile;
import simulator.ModulesFileModelGenerator;
import parser.ast.ModulesFile;
import strat.MDStrategy;
import parser.ast.Expression;
import explicit.MDPExplicit;
import explicit.DTMCModelChecker;
import explicit.DTMC;
import strat.Strategy;


public class MAPEstimator extends Estimator
{
    protected HashMap<TransitionTriple, Integer> dirichletPriorsMap;
	protected HashMap<StateActionPair, HashSet<Integer>> successorStatesMap;

	private int distance;


    public MAPEstimator(Prism prism, Experiment ex) {
		super(prism, ex);
        this.dirichletPriorsMap = new HashMap<>();
		this.successorStatesMap = new HashMap<>();
		this.setPriors(ex.alpha);
		this.name = "MAP";
	}

    public void setIntervalsMap(HashMap<TransitionTriple, Interval<Double>> im) {
        this.intervalsMap = im;
    }

    public void setPriors(int alpha) {
		int numStates = mdp.getNumStates();
		for (int s = 0; s < numStates; s++) {
			final int state = s;
			int numChoices = mdp.getNumChoices(s);
			for (int i = 0 ; i < numChoices; i++) {
				final String action = getActionString(mdp, s, i);
				final StateActionPair sa = new StateActionPair(state, action);
				HashSet<Integer> successors = new HashSet<>();
				Distribution<Interval<Double>> distrNew = new Distribution<>(Evaluator.createForDoubleIntervals());
				mdp.forEachDoubleTransition(s, i, (int sFrom, int sTo, double p)->{
					if (p != 0.0) {
						final TransitionTriple t = new TransitionTriple(state, action, sTo);
						successors.add(sTo);
						this.dirichletPriorsMap.put(t, alpha);
					}
				});
				this.successorStatesMap.put(sa, successors);
			}
		}
    }

	public Double mode(TransitionTriple t) {
		int num = dirichletPriorsMap.get(t)-1;
		int denum = 0;
		int count = 0;
		StateActionPair sa = t.getStateAction();
		HashSet<Integer> successors = successorStatesMap.get(sa);
		for (int successor : successors) {
			TransitionTriple sas = new TransitionTriple(sa.getState(), sa.getAction(), successor);
			int alpha = dirichletPriorsMap.get(sas);
			//System.out.println(alpha);
			denum += alpha;
			count += 1;
		}
		denum -= count;


		//System.out.println("num = " + num);
		//System.out.println("denum = " + denum);
		return (double) num / (double) denum;
	}

	public int getTransitionCount(TransitionTriple t) {
		return dirichletPriorsMap.get(t);
	}

	public int getTotalTransitionCount() {
		int count = 0;
		for (TransitionTriple t : dirichletPriorsMap.keySet()) {
			count += getTransitionCount(t);
		}
		return count;
	}

	public int getStateActionCount(StateActionPair sa) {
		int count = 0;
		HashSet<Integer> successors = successorStatesMap.get(sa);
		for (int successor : successors) { 
			TransitionTriple sas = new TransitionTriple(sa.getState(), sa.getAction(), successor);
			count += dirichletPriorsMap.get(sas);
		}
		return count;
	}




	public void updatePriors() {
		boolean needsNormalization = false;
		for (TransitionTriple t : this.dirichletPriorsMap.keySet()) {
			if (this.samplesMap.containsKey(t)) {
				this.dirichletPriorsMap.put(t, this.dirichletPriorsMap.get(t) + this.samplesMap.get(t));
				if (this.dirichletPriorsMap.get(t) + this.samplesMap.get(t) > ex.maxMAPStrength) {
					needsNormalization = true;
				}
			}
		}
		if (needsNormalization)
			normalizePriors();
	}

	public void normalizePriors() {
		//System.out.println(this.dirichletPriorsMap);
		int maximum = 0;
		double scale = 1.0;
		for (TransitionTriple t : this.dirichletPriorsMap.keySet()) {
			maximum = Integer.max(maximum, this.dirichletPriorsMap.get(t));
			scale = (double) ex.maxMAPStrength / (double) maximum; 
		}
		for (TransitionTriple t : this.dirichletPriorsMap.keySet()) {
			double scaledValue = scale * (double) this.dirichletPriorsMap.get(t);
			int intValue = (int) Math.ceil(scaledValue);
			if (intValue < 2)
				intValue = 2;
			this.dirichletPriorsMap.put(t, intValue);
		}
		//System.out.println(this.dirichletPriorsMap);
	}



	public Result iterateMDP(boolean robust) throws PrismException {
		return iterateMDP(robust, false);
	}


	public Result iterateMDP(boolean robust, boolean verbose) throws PrismException{
		updatePriors();
		buildPointIMDP(mdp);
		Result result = modelCheckPointEstimate(true, false);
		return result;
	}


	public Result iterateDTMC() throws PrismException {
		updatePriors();
		buildPointIMDP(mdp);
		MDStrategy strat = computeStrategyFromEstimate(this.estimate);
		Result resultDTMC = checkDTMC(strat);
		return resultDTMC;
	}

	public double[] getCurrentResults() throws PrismException {
		updatePriors();
		buildPointIMDP(mdp);
		double resultRobustMDP = round((Double) modelCheckPointEstimate(true, false).getResult());
		double resultOptimisticMDP = round((Double) modelCheckPointEstimate(false, false).getResult());
		MDStrategy robustStrat = computeStrategyFromEstimate(this.estimate, true);
		MDStrategy optimisticStrat = computeStrategyFromEstimate(this.estimate, false);
		double resultRobustDTMC = round((Double) checkDTMC(robustStrat).getResult());
		double resultOptimisticDTMC = round((Double) checkDTMC(optimisticStrat).getResult());
		double dist = round(this.averageDistanceToSUL());
		List<Double> lbs = this.getLowerBounds();
		List<Double> ubs = this.getUpperBounds();
		return new double[]{resultRobustMDP, resultRobustDTMC, dist, lbs.get(0), ubs.get(0), resultOptimisticMDP, resultOptimisticDTMC};
	}

	public MDStrategy computeStrategyFromEstimate(IMDP<Double> estimate) throws PrismException {
		IMDPModelChecker mc = new IMDPModelChecker(this.prism);
		mc.setGenStrat(true);
		mc.setErrorOnNonConverge(false);
		PropertiesFile pf = prism.parsePropertiesString(ex.robustSpec);
		ModulesFileModelGenerator<?> modelGen = ModulesFileModelGenerator.create(modulesFileIMDP, this.prism);
		mc.setModelCheckingInfo(modelGen, pf, modelGen);
		Expression exprTarget = this.prism.parsePropertiesString(ex.robustSpec).getProperty(0);
		Result result = mc.check(estimate, exprTarget);
		MDStrategy strat = (MDStrategy) result.getStrategy();
		//System.out.println("Strategy = " + strat);    // strat is null
		return strat;
	}


	@Override
	public double averageDistanceToSUL() {
		double totalDist = 0.0;

		for (TransitionTriple t : super.trueProbabilitiesMap.keySet()) {
			double value = mode(t);
			double p = super.trueProbabilitiesMap.get(t);
			double dist = Math.abs(value - p);
			totalDist += dist;
		}
		double averageDist = totalDist / super.trueProbabilitiesMap.keySet().size();
		return averageDist;
	}


	public Result checkDTMC(MDStrategy strat) throws PrismException {
		MDPExplicit<Double> mdp = (MDPExplicit<Double>) this.prism.getBuiltModelExplicit();
		DTMC<Double> dtmc = (DTMC<Double>) mdp.constructInducedModel(strat);
		DTMCModelChecker mc = new DTMCModelChecker(this.prism);
		mc.setErrorOnNonConverge(false);
		mc.setGenStrat(true);
		PropertiesFile pf = prism.parsePropertiesString(ex.dtmcSpec);
		ModulesFile modulesFileDTMC = (ModulesFile) modulesFileIMDP.deepCopy();
		modulesFileDTMC.setModelType(ModelType.DTMC);
		ModulesFileModelGenerator<?> modelGen = ModulesFileModelGenerator.create(modulesFileDTMC, this.prism);
		RewardGeneratorMDStrat<?> rewGen = new RewardGeneratorMDStrat(modelGen, mdp, strat);
		mc.setModelCheckingInfo(modelGen, pf, rewGen);
		Result result = mc.check(dtmc, pf.getProperty(0));
		return result;
	}



	public Result getInitialResult(boolean verbose) throws PrismException {
		buildPointIMDP(mdp);
		Result result = modelCheckPointEstimate(true, false);
		return result;
	}

	public double[] getInitialResults() throws PrismException {
		buildPointIMDP(mdp);
		double resultRobustMDP = round((Double) modelCheckPointEstimate(true, false).getResult());
		double resultOptimisticMDP = round((Double) modelCheckPointEstimate(false, false).getResult());
		MDStrategy robustStrat = computeStrategyFromEstimate(this.estimate, true);
		MDStrategy optimisticStrat = computeStrategyFromEstimate(this.estimate, false);
		double resultRobustDTMC = round((Double) checkDTMC(robustStrat).getResult());
		double resultOptimisticDTMC = round((Double) checkDTMC(optimisticStrat).getResult());
		double dist = round(this.averageDistanceToSUL());
		List<Double> lbs = this.getLowerBounds();
		List<Double> ubs = this.getUpperBounds();
		return new double[] {resultRobustMDP, resultRobustDTMC, dist, lbs.get(0), ubs.get(0), resultOptimisticMDP, resultOptimisticDTMC};
	}



	/**
	 * Builds a point estimate IMDP of point intervals with laplace smoothing for the parameter epsilon
	 * @param mdp MDP for the underlying state space
	 * @return IMDP of point intervals
	 */
	public IMDP<Double> buildPointIMDP(MDP<Double> mdp) {
		int numStates = mdp.getNumStates();
		IMDPSimple<Double> imdp = new IMDPSimple<>(numStates);
		imdp.addInitialState(mdp.getFirstInitialState());
		imdp.setStatesList(mdp.getStatesList());
		imdp.setEvaluator(Evaluator.createForDoubleIntervals());

		for (int s = 0; s < numStates; s++) {
			int numChoices = mdp.getNumChoices(s);
			final int state = s;
			for (int i = 0 ; i < numChoices; i++) {
				final String action = getActionString(mdp, s, i);
				
				Distribution<Interval<Double>> distrNew = new Distribution<>(Evaluator.createForDoubleIntervals());
				mdp.forEachDoubleTransition(s, i, (int sFrom, int sTo, double p)->{
					TransitionTriple t = new TransitionTriple(state, action, sTo);
					Interval<Double> interval;
					if (0 < p && p < 1.0) {
						interval = getTransitionInterval(t);
						distrNew.add(sTo, interval);
						this.intervalsMap.put(t, interval);
					}
					else if (p == 1.0){
						interval = new Interval<Double>(p, p);
						distrNew.add(sTo, interval);
						this.intervalsMap.put(t, interval);
					}
				});
				imdp.addActionLabelledChoice(s, distrNew, getActionString(mdp, s, i));
			}
		}
		Map<String,BitSet> labels = mdp.getLabelToStatesMap();
		Iterator<Entry<String, BitSet>> it = labels.entrySet().iterator();
		while (it.hasNext()) {
   			Map.Entry<String, BitSet> entry = it.next();
    		imdp.addLabel(entry.getKey(), entry.getValue());
		}
		this.estimate = imdp;

		return imdp;
	}

	protected Interval<Double> getTransitionInterval(TransitionTriple t) {
		double point = mode(t);
		return new Interval<>(point, point);
	}

	/**
	 * Model check the point estimate stored in the class
	 * @return Result
	 * @throws PrismException
	 */
	public Result modelCheckPointEstimate(boolean robust, boolean verbose) throws PrismException {
		IMDPModelChecker mc = new IMDPModelChecker(this.prism);
		mc.setErrorOnNonConverge(false);
		PropertiesFile pf = prism.parsePropertiesString(ex.robustSpec);
		if (robust)
			pf = prism.parsePropertiesString(ex.robustSpec);
		else 
			pf = prism.parsePropertiesString(ex.optimisticSpec);

		ModulesFileModelGenerator<?> modelGen = ModulesFileModelGenerator.create(modulesFileIMDP, this.prism);
		mc.setModelCheckingInfo(modelGen, pf, modelGen);
		Result result = mc.check(this.estimate, pf.getProperty(0));
		if (verbose) {
			System.out.println("\nModel checking point estimate MDP:");
			System.out.println(ex.robustSpec + " : " + result.getResultAndAccuracy());
		}
		return result;
    }

	/**
	 * Model check a given point estimate IMDP
	 * @param pointEstimate IMDP
	 * @return Result
	 * @throws PrismException
	 */
	public Result modelCheckPointEstimate(IMDP<Double> pointEstimate) throws PrismException {
		IMDPModelChecker mc = new IMDPModelChecker(this.prism);
		mc.setErrorOnNonConverge(false);
		PropertiesFile pf = prism.parsePropertiesString(ex.robustSpec);
		ModulesFileModelGenerator<?> modelGen = ModulesFileModelGenerator.create(modulesFileIMDP, this.prism);
		mc.setModelCheckingInfo(modelGen, pf, modelGen);
		Result result = mc.check(pointEstimate, pf.getProperty(0));
		System.out.println("\nModel checking point estimate MDP:");
		System.out.println(ex.robustSpec + " : " + result.getResultAndAccuracy());
		return result;
    }

	public Strategy buildStrategy() throws PrismException{
		return super.buildUniformStrat();
	}
}


class MAPEstimatorOptimistic extends MAPEstimator {
    public MAPEstimatorOptimistic(Prism prism, Experiment ex) {
        super(prism, ex);
    }

    public Strategy buildStrategy() throws PrismException {
        return this.buildWeightedOptimisticStrategy(this.getEstimate(), this.ex.strategyWeight);
    }
}