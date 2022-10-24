package strat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import explicit.NondetModel;
import prism.PrismLog;
import simulator.RandomNumberGenerator;

public class MRStrategy implements Strategy
{
	// Model that strategy is for
	protected NondetModel<?> model;
	
	// Probability of action in each state 
	protected List<HashMap<Object,Double>> actionProbs;
	
	public MRStrategy(NondetModel<?> model)
	{
		this.model = model;
		int numStates = this.model.getNumStates();
		actionProbs = new ArrayList<>(numStates);
		for (int i = 0; i < numStates; i++) {
			actionProbs.add(new HashMap<>());
		}
	}
	
	/**
	 * Set the probability of choosing action a in state s to p
	 */
	public void setActionProbability(int s, Object a, double p)
	{
		HashMap<Object, Double> dist = actionProbs.get(s);
		dist.put(a, p);
	}
	
	/**
	 * Sample the (index of the) choice to take in state s
	 */
	public int getChoice(int s, RandomNumberGenerator rng)
	{
		HashMap<Object, Double> dist = actionProbs.get(s);
		Object action = sampleFromHashMap(dist, rng.randomUnifDouble());
		int i = model.getChoiceByAction(s, action);
		return i;
	}
	
	private Object sampleFromHashMap(HashMap<Object, Double> dist, double x)
	{
		Object action = null;
		double tot = 0.0;
		for (Map.Entry<Object,Double> entry : dist.entrySet()) {
			tot += entry.getValue();
			if (x < tot) {
				action = entry.getKey();
				break;
			}
		}
		return action;
	}

	// ---------------------------------------------------------
	
	@Override
	public void exportActions(PrismLog out)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void exportIndices(PrismLog out)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void exportInducedModel(PrismLog out)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void exportDotFile(PrismLog out)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void initialise(int s)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void update(Object action, int s)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object getChoiceAction()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void clear()
	{
		// TODO Auto-generated method stub
		
	}
}
