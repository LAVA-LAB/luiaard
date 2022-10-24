package prism;

import java.io.FileWriter;
import java.io.IOException;


public class Experiment {


    public static enum Type {
        REACH,
        REWARD
    }

    public static enum Model {
        BETTING_GAME_FAVOURABLE,
        BETTING_GAME_UNFAVOURABLE,
        CHAIN_SMALL,
        CHAIN_SMALL2,
        CHAIN2,
        CHAIN_LARGE,
        CHAIN_LARGE2,
        CHAIN_test,
        GRID,
        GRID_SAFE_SE,
        TINY,
        TINY2,
        LOOP,
        AIRCRAFT,
        BANDIT,
    }


    public static enum InitialInterval {
        WIDE,
        UNIFORM,
    }



    private String modelInfo = "";
    public Model model;
    public Type type;

    public String goal;
	public String spec;
	public String robustSpec;
	public String optimisticSpec;
	public String modelFile;
	public String dtmcSpec;

    public String experimentInfo = "basic";

    public int seed = 1;
    public int initLowerStrength = 5;
    public int initUpperStrength = 10;
    public int lowerStrengthBound = Integer.MAX_VALUE;
    public int upperStrengthBound = Integer.MAX_VALUE;
    public int maxMAPStrength = Integer.MAX_VALUE;
    public double initGraphEpsilon = 1e-4;
    public int iterations = 10;
    public int max_episode_length = 100;
    public int alpha = 10;
    public double error_tolerance = 0.01; // 99% correctness guarantee
    public double strategyWeight = 1.0;

    public InitialInterval initialInterval = InitialInterval.WIDE;

    public double trueOpt = Double.NaN;

    public Experiment(Model model) {
        setModel(model);
    }

    private void setModel(Model model) {
        this.model = model;
        switch (model) {
			case CHAIN_SMALL:
			this.goal = "\"goal\"";
			this.spec = "Rmin=? [F \"goal\"]";
			this.robustSpec = "Rminmax=? [F \"goal\"]";
			this.optimisticSpec = "Rminmin=? [F \"goal\"]";
			this.dtmcSpec = "R=? [F \"goal\"]";
			this.modelFile = "models/chain.prism";
			this.type = Type.REWARD;
			break;
            case CHAIN_SMALL2:
            this.goal = "\"goal\"";
            this.spec = "Rmin=? [F \"goal\"]";
            this.robustSpec = "Rminmax=? [F \"goal\"]";
            this.optimisticSpec = "Rminmin=? [F \"goal\"]";
            this.dtmcSpec = "R=? [F \"goal\"]";
            this.modelFile = "models/chain_2.prism";
            this.type = Type.REWARD;
            break;
			case CHAIN2:
			this.goal = "\"goal\"";
			this.spec = "Pmax=? [F<=5 \"goal\"]";
			this.robustSpec = "Pmaxmin=? [F<=5 \"goal\"]";
			this.optimisticSpec = "Pmaxmax=? [F<=5 \"goal\"]";
			this.dtmcSpec = "P=? [F<=5 \"goal\"]";
			this.modelFile = "models/chain.prism";
			this.type = Type.REACH;
			break;
            case CHAIN_LARGE:
            this.goal = "\"goal\"";
            this.spec = "Rmin=? [F \"goal\"]";
            this.robustSpec = "Rminmax=? [F \"goal\"]";
            this.optimisticSpec = "Rminmin=? [F \"goal\"]";
            this.dtmcSpec = "R=? [F \"goal\"]";
            this.modelFile = "models/chain_large.prism";
            this.type = Type.REWARD;
            break;
            case CHAIN_LARGE2:
            this.goal = "\"goal\"";
            this.spec = "Rmin=? [F \"goal\"]";
            this.robustSpec = "Rminmax=? [F \"goal\"]";
            this.optimisticSpec = "Rminmin=? [F \"goal\"]";
            this.dtmcSpec = "R=? [F \"goal\"]";
            this.modelFile = "models/chain_large2.prism";
            this.type = Type.REWARD;
            break;
            case TINY:
            this.goal = "\"goal\"";
            this.spec = "Rmin=? [F \"goal\"]";
            this.robustSpec = "Rminmax=? [F \"goal\"]";
            this.optimisticSpec = "Rminmin=? [F \"goal\"]";
            this.dtmcSpec = "R=? [F \"goal\"]";
            this.modelFile = "models/tiny.prism";
            this.type = Type.REWARD;
            break;
            case TINY2:
            this.goal = "\"goal\"";
            this.spec = "Rmin=? [F \"goal\"]";
            this.robustSpec = "Rminmax=? [F \"goal\"]";
            this.optimisticSpec = "Rminmin=? [F \"goal\"]";
            this.dtmcSpec = "R=? [F \"goal\"]";
            this.modelFile = "models/tiny2.prism";
            this.type = Type.REWARD;
            break;
			case GRID:
			this.goal = "\"goal_NE\"";
			this.spec = "Pmax=? [!(\"trap\") U (\"goal_NE\")]";
			this.robustSpec = "Pmaxmin=? [!(\"trap\") U (\"goal_NE\") ]";
			this.optimisticSpec = "Pmaxmax=? [!(\"trap\") U (\"goal_NE\") ]";
			this.dtmcSpec = "P=? [!(\"trap\") U (\"goal_NE\")]";
			this.modelFile = "models/grid.prism";
			this.type = Type.REACH;
			break;
			case GRID_SAFE_SE:
			this.goal = "\"goal_SE\"";
			this.spec = "Pmax=? [!(\"trap\") U (\"goal_SE\")]";
			this.robustSpec = "Pmaxmin=? [!(\"trap\") U (\"goal_SE\") ]";
			this.optimisticSpec = "Pmaxmax=? [!(\"trap\") U (\"goal_SE\") ]";
			this.dtmcSpec = "P=? [!(\"trap\") U (\"goal_SE\")]";
			this.modelFile = "models/grid.prism";
			this.type = Type.REACH;
			break;
			case CHAIN_test:
			this.goal = "\"goal\"";
			this.spec = "Pmax=? [F<=100 \"goal\"]";
			this.robustSpec = "Pmaxmin=? [F<=100 \"goal\"]";
			this.optimisticSpec = "Pmaxmax=? [F<=100 \"goal\"]";
			this.dtmcSpec = "P=? [F<=100 \"goal\"]";
			this.modelFile = "models/chain_test.prism";
			this.type = Type.REACH;
			break;
            case LOOP:
            this.goal = "\"goal\"";
            this.spec = "Rmax=? [F \"goal\"]";
            this.robustSpec = "Rmaxmin=? [F \"goal\"]";
            this.optimisticSpec = "Rmaxmax=? [F \"goal\"]";
            this.dtmcSpec = "R=? [F \"goal\"]";
            this.modelFile = "models/loop.prism";
            this.type = Type.REWARD;
            break;
            case AIRCRAFT:
            this.goal = "\"goal\"";
            this.spec = "Pmax=? [!collision U \"goal\"]";
            this.robustSpec = "Pmaxmin=? [!collision U \"goal\"]";
            this.optimisticSpec = "Pmaxmax=? [!collision U \"goal\"]";
            this.dtmcSpec = "P=?  [!collision U \"goal\"]";
            this.modelFile = "models/aircraft_tiny.prism";
            this.type = Type.REACH;
            break;
            case BANDIT:
            this.goal = "\"goal\"";
            this.spec = "Rmax=? [F \"goal\"]";
            this.robustSpec = "Rmaxmin=? [F \"goal\"]";
            this.optimisticSpec = "Rmaxmax=? [F \"goal\"]";
            this.dtmcSpec = "R=?  [F \"goal\"]";
            this.modelFile = "models/bandit.prism";
            this.type = Type.REACH; // TODO: change to REWARD?
            break;
            case BETTING_GAME_FAVOURABLE:
            this.goal = "\"done\"";
            this.spec = "Rmax=? [F \"done\"]";
            this.robustSpec = "Rmaxmin=? [F \"done\"]";
            this.optimisticSpec = "Rmaxmax=? [F \"done\"]";
            this.dtmcSpec = "R=?  [F \"done\"]";
            this.modelFile = "models/bet_fav.prism";
            this.type = Type.REWARD;
            break;
            case BETTING_GAME_UNFAVOURABLE:
            this.goal = "\"done\"";
            this.spec = "Rmax=? [F \"done\"]";
            this.robustSpec = "Rmaxmin=? [F \"done\"]";
            this.optimisticSpec = "Rmaxmax=? [F \"done\"]";
            this.dtmcSpec = "R=?  [F \"done\"]";
            this.modelFile = "models/bet_unfav.prism";
            this.type = Type.REWARD;
            break;
		}
    }

    public void setPriors(double epsilon, int lower, int upper) {
        this.initGraphEpsilon = epsilon;
        this.initLowerStrength = lower;
        this.initUpperStrength = upper;
    }

    public Experiment config(int max_episode_length, int iterations, int repetitions) {
        this.max_episode_length = max_episode_length;
        this.iterations = iterations;
        this.seed = repetitions;
        return this;
    }

    public Experiment config(int max_episode_length, int iterations, int repetitions, int lowerStrength, int upperStrength) {
        this.max_episode_length = max_episode_length;
        this.iterations = iterations;
        this.seed = repetitions;
        this.initLowerStrength = lowerStrength;
        this.initUpperStrength = upperStrength;
        return this;
    }

    public void setModelInfo(String modelInfo) {
        this.modelInfo = modelInfo;
    }

    public Experiment info(String experimentInfo) {
        this.experimentInfo = experimentInfo;
        return this;
    }

    public Experiment setStrengthBounds(int lowerStrengthBound, int upperStrengthBound, int maxMAPStrength) {
        this.lowerStrengthBound = lowerStrengthBound;
        this.upperStrengthBound = upperStrengthBound;
        this.maxMAPStrength = maxMAPStrength;
        return this;
    }

    public Experiment stratWeight(double weight) {
        this.strategyWeight = weight;
        return this;
    }

    public Experiment setErrorTol(double errorTolerance) {
        this.error_tolerance = errorTolerance;
        return this;
    }

    public void setTrueOpt(double opt) {
        this.trueOpt = opt;
    }

    public void dumpConfiguration(String pathPrefix, String file_name, String algorithm){

        try {
            FileWriter writer = new FileWriter(pathPrefix + "/"+ file_name + ".yaml");
            writer.write("modelInfo: " + modelInfo + "\n");
            writer.write("model: " + model + "\n");
            writer.write("type: " + type + "\n");
            writer.write("goal: " + goal + "\n");
            writer.write("spec: " + spec + "\n");
            writer.write("robustSpec: " + robustSpec + "\n");
            writer.write("modelFile: " + modelFile + "\n");
            writer.write("dtmcSpec: " + dtmcSpec + "\n");
            writer.write("experimentInfo: " + experimentInfo + "\n");
            writer.write("seed: " + seed + "\n");
            writer.write("initLowerStrength: " + initLowerStrength + "\n");
            writer.write("initUpperStrength: " + initUpperStrength + "\n");
            writer.write("lowerStrengthBound: " + lowerStrengthBound + "\n");
            writer.write("upperStrengthBound: " + upperStrengthBound + "\n");
            writer.write("strategyWeight: " + strategyWeight + "\n");
            writer.write("initGraphEpsilon: " + initGraphEpsilon + "\n");
            writer.write("iterations: " + iterations + "\n");
            writer.write("max_episode_length: " + max_episode_length + "\n");
            writer.write("alpha: " + alpha + "\n");
            writer.write("error_tolerance: " + error_tolerance + "\n");
            writer.write("trueOpt: " + trueOpt + "\n");
            writer.write("prefix: " + file_name + "\n");
            writer.write("algorithm: " + algorithm + "\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Dump experiment setting to "+ pathPrefix);
    }


}
