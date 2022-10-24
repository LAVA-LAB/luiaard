package prism;

import common.Interval;

public class PACIntervalEstimator extends MAPEstimator {

	protected double error_tolerance;

    public PACIntervalEstimator(Prism prism, Experiment ex) {
		super(prism, ex);
		error_tolerance = ex.error_tolerance;
		this.name = "PAC";
    }

	@Override
	protected Interval<Double> getTransitionInterval(TransitionTriple t) {
		double point = mode(t);
		//System.out.println("Point = " + point);
		double confidence_interval = confidenceInterval(t);
		//System.out.println("confidence_interval = " + confidence_interval);
		double precision = 1e-8;
		double lower_bound = Math.max(point - confidence_interval, precision);
		double upper_bound = Math.min(point + confidence_interval, 1-precision);
		//System.out.println("confidence interval: " + confidence_interval);
		//System.out.println("[l, u]: [" + lower_bound +", "+  upper_bound+ "]");
		return new Interval<>(lower_bound, upper_bound);
	}

	@Override
	public double averageDistanceToSUL() {
		double totalDist = 0.0;

		for (TransitionTriple t : super.trueProbabilitiesMap.keySet()) {
			Interval<Double> interval = this.intervalsMap.get(t);
			double p = super.trueProbabilitiesMap.get(t);
			double dist = maxIntervalPointDistance(interval, p);
			totalDist += dist;
		}

		double averageDist = totalDist / super.trueProbabilitiesMap.keySet().size();
		return averageDist;

	}

	protected Double confidenceInterval(TransitionTriple t) {
		return computePACBound(t);
	}

	private Double computePACBound(TransitionTriple t) {
		double alpha = error_tolerance; // probability of error (i.e. 1-alpha is probability of correctly specifying the interval)
		int m = this.getNumLearnableTransitions();
		//System.out.println("m = " + m);
		int n = getStateActionCount(t.getStateAction());
		alpha = (error_tolerance*(1.0/(double) m))/((double) this.mdp.getNumChoices(t.getStateAction().getState())); // distribute error over all transitions
		//System.out.println("alpha = " + alpha);
		double delta = Math.sqrt((Math.log(2 / alpha))/(2*n));
		return delta;
	}
}
