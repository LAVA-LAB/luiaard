package prism;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.BitSet;
import java.util.Map;
import java.util.Iterator;
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
import java.io.File;
import java.io.FileNotFoundException;


// class for maximum likelihood estimation, unused.
public class PointEstimator
{
	private Prism prism;
	private String robustSpec;
	private String target;
	private ModulesFile modulesFileIMDP;
    private HashMap<TransitionTriple, Integer> samplesMap;
	private HashMap<StateActionPair, Integer> sampleSizeMap;
    private HashMap<TransitionTriple, Interval<Double>> intervalsMap;
	
	private MDP<Double> mdp;
	private IMDPSimple<Double> pointEstimate;

    public PointEstimator(Prism prism, String robustSpec, String target, String prismModelFile, MDP<Double> mdp) throws PrismException {
        this.samplesMap = new HashMap<>();
        this.sampleSizeMap = new HashMap<>();
        this.intervalsMap = new HashMap<>();

		this.prism = prism;
		this.robustSpec = robustSpec;
		this.target = target;
		this.mdp = mdp;
		try {
			this.modulesFileIMDP = this.prism.parseModelFile(new File(prismModelFile), ModelType.IMDP);
		} catch(FileNotFoundException e) {
			System.out.println("Error: FileNotFoundException in PointEstimator.java \t" + e.toString());
			System.exit(1);
		}
	}

	public Result iterate(double epsilon, boolean verbose) throws PrismException {
		buildPointIMDP(mdp, epsilon);
		Result result = modelCheckPointEstimate(verbose);
		return result;
	}

    public void setIntervalsMap(HashMap<TransitionTriple, Interval<Double>> im) {
        this.intervalsMap = im;
    }

	public String getActionString(MDP<Double> mdp, int s, int i) {
		String action = (String) mdp.getAction(s,i);
		if (action == null) {
			action = "_empty";
		}
		return action;
	}

	public void setSamplesMap(HashMap<TransitionTriple, Integer> samplesMap) {
		this.samplesMap = samplesMap;
	}

	public void setSampleSizeMap(HashMap<StateActionPair, Integer> sampleSizeMap) {
		this.sampleSizeMap = sampleSizeMap;
	}

	public void setObservationMaps(HashMap<TransitionTriple, Integer> samplesMap, HashMap<StateActionPair, Integer> sampleSizeMap) {
		setSampleSizeMap(sampleSizeMap);
		setSamplesMap(samplesMap);
	}

	public void addSamplesMap(HashMap<TransitionTriple, Integer> samplesMapToAdd) {
		for (TransitionTriple t : samplesMapToAdd.keySet()) {
			int toAdd = samplesMapToAdd.get(t);
			if (this.samplesMap.get(t) == null) {
				this.samplesMap.put(t, toAdd);
			}
			else {
				this.samplesMap.put(t, this.samplesMap.get(t)+toAdd);
			}
		}
	}

	public void addSampleSizeMap(HashMap<StateActionPair, Integer> sampleSizeMapToAdd) {
		//System.out.println("---\n"+this.sampleSizeMap.get(new StateActionPair(7, "attack13_23")));
		//System.out.println("to add: " + sampleSizeMapToAdd.get(new StateActionPair(7, "attack13_23")));
		for (StateActionPair sa : sampleSizeMapToAdd.keySet()) {
			int toAdd = sampleSizeMapToAdd.get(sa);
			if (this.sampleSizeMap.get(sa) == null) {
				this.sampleSizeMap.put(sa, toAdd);
			}
			else {
				this.sampleSizeMap.put(sa, this.sampleSizeMap.get(sa)+toAdd);
			}
		}

		//samplesMapToAdd.entrySet().forEach(entry -> this.samplesMap.merge(entry.getKey(), entry.getValue(), (key,value) -> entry.getValue() + value));
		//System.out.println(this.sampleSizeMap.get(new StateActionPair(7, "attack13_23")));
		//sampleSizeMapToAdd.entrySet().forEach(entry -> this.sampleSizeMap.merge(entry.getKey(), entry.getValue(), (key,value) -> entry.getValue() + value));
	}

	public void addObservationMaps(HashMap<TransitionTriple, Integer> samplesMap, HashMap<StateActionPair, Integer> sampleSizeMap) {
		addSampleSizeMap(sampleSizeMap);
		addSamplesMap(samplesMap);
	}


	/**
	 * Builds a point estimate IMDP of point intervals with laplace smoothing for the parameter epsilon
	 * @param mdp MDP for the underlying state space
	 * @param epsilon laplace smoothing parameter
	 * @return IMDP of point intervals
	 */
	public IMDP<Double> buildPointIMDP(MDP<Double> mdp, double epsilon) {
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
				int temp = 0;
				for (int successor = 0; successor < numStates; successor++) {
					TransitionTriple t = new TransitionTriple(s, action, successor);
					if (this.intervalsMap.containsKey(t)) {
						temp += 1;
					}
				}
				final int numSuccs = temp;
				
				Distribution<Interval<Double>> distrNew = new Distribution<>(Evaluator.createForDoubleIntervals());
				mdp.forEachDoubleTransition(s, i, (int sFrom, int sTo, double p)->{
					if (0 < p && p < 1.0) {
						final TransitionTriple t = new TransitionTriple(state, action, sTo);
						final StateActionPair sa = new StateActionPair(state, action);
						Object samples = this.samplesMap.get(t);
						Object samplesize = this.sampleSizeMap.get(sa);
						double point = 1.0 / numSuccs;

						if (this.sampleSizeMap.containsKey(sa)) {
							if (this.samplesMap.containsKey(t)) {
								point = (((double) this.samplesMap.get(t)) + epsilon)/((double) this.sampleSizeMap.get(sa) + numSuccs*epsilon);
							}
							else {
								point = (epsilon)/((double)this.sampleSizeMap.get(sa) + numSuccs*epsilon);
							}
						}
						else {
							point = (1.0)/(numSuccs);
						}

						distrNew.add(sTo, new Interval<Double>(point, point));
					}
					else if (p == 1.0){
						distrNew.add(sTo, new Interval<Double>(p, p));
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
		pointEstimate = imdp;

		return imdp;
	}


	/**
	 * Model check the point estimate stored in the class
	 * @return Result
	 * @throws PrismException
	 */
	public Result modelCheckPointEstimate(boolean verbose) throws PrismException {
		IMDPModelChecker mc = new IMDPModelChecker(this.prism);
		mc.setErrorOnNonConverge(false);
		PropertiesFile pf = prism.parsePropertiesString(this.robustSpec);
		ModulesFileModelGenerator<?> modelGen = ModulesFileModelGenerator.create(modulesFileIMDP, this.prism);
		mc.setModelCheckingInfo(modelGen, pf, modelGen);
		Result result = mc.check(pointEstimate, pf.getProperty(0));
		if (verbose) {
			System.out.println("\nModel checking point estimate MDP: "+this.robustSpec + " = " + result.getResultAndAccuracy());
		}
		return result;
    }

	/**
	 * Model check a given point estimate IMDP
	 * @param pointEstimate IMDP
	 * @return Result
	 * @throws PrismException
	 */
	public Result modelCheckPointEstimate(IMDP<Double> pointEstimate, boolean verbose) throws PrismException {
		IMDPModelChecker mc = new IMDPModelChecker(this.prism);
		mc.setErrorOnNonConverge(false);
		PropertiesFile pf = prism.parsePropertiesString(this.robustSpec);
		ModulesFileModelGenerator<?> modelGen = ModulesFileModelGenerator.create(modulesFileIMDP, this.prism);
		mc.setModelCheckingInfo(modelGen, pf, modelGen);
		Result result = mc.check(pointEstimate, pf.getProperty(0));
		if (verbose) {
			System.out.println("\nModel checking point estimate MDP:");
			System.out.println(this.robustSpec + " : " + result.getResultAndAccuracy());
		}
		return result;
    }


}
