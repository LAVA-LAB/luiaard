package prism;

import strat.Strategy;

public class UCRL2IntervalEstimatorOptimistic extends UCRL2IntervalEstimator {
    public UCRL2IntervalEstimatorOptimistic(Prism prism, Experiment ex) {
        super(prism, ex);
    }

    public Strategy buildStrategy() throws PrismException {
        return this.buildWeightedOptimisticStrategy(this.getEstimate(), this.ex.strategyWeight);
    }
}


