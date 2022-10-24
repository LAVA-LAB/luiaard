package prism;

/**
 * Small class for state-action pairs (s,a)
 */
public class StateActionPair 
{
    private int s;
    private String action;

    public StateActionPair(int s, String action) {
        this.s = s;
        this.action = action;
    }

    public int getState() {
        return this.s;
    }

    public String getAction() {
        return this.action;
    }

    @Override
    public int hashCode() {
        //return Objects.hash(this.s, this.action);
        return this.toString().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        
        if (o == null)
            return false;

        if (this.getClass() != o.getClass())
            return false;
        
        StateActionPair other = (StateActionPair) o;
        return (this.s == other.getState()) && this.action.equals(other.getAction());
    }

    @Override
    public String toString() {
        return "(" + this.s + ", " + this.action + ")";
    }
}