package prism;

public class DataPoint {

    private int accumulated_samples;
    private double estimated_value;
    private double value;
    private double distance;
    private int episode;
    private double upper_bound;
    private double lower_bound;
    private double optimistic_estimated_value;
    private double optimistic_value;

    public DataPoint(int position, double value) {
        this.accumulated_samples = position;
        this.value = value;
    }

    public DataPoint(int accumulated_samples, int episode, double[] results) {
        this.accumulated_samples = accumulated_samples;
        this.estimated_value = results[0];
        this.value = results[1];
        this.distance = results[2];
        this.episode = episode;
        this.lower_bound = results[3];
        this.upper_bound = results[4];
        this.optimistic_estimated_value = results[5];
        this.optimistic_value = results[6];
    }

    public int getAccumulatedSamples() {
        return accumulated_samples;
    }

    public double getEstimatedValue() {
        return estimated_value;
    }

    public double getValue() {
        return value;
    }

    public double getDistance() {
        return distance;
    }

    public double getEpisode() {
        return episode;
    }

    public double getLowerBound() { return lower_bound; }

    public double getUpperBound() { return upper_bound; }

    public double getOptimisticValue() { return optimistic_value; }

    public double getOptimisticEstimatedValue() { return optimistic_estimated_value; }


    @Override
    public boolean equals(Object o) {
 
        if (o == this) {
            return true;
        }
 

        if (!(o instanceof DataPoint)) {
            return false;
        }
         

        DataPoint d = (DataPoint) o;
         
        boolean eq = false;
        // todo implement equality on d and this
        
        return eq;
    }
}
