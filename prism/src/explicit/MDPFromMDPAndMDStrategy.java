//==============================================================================
//	
//	Copyright (c) 2020-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package explicit;

import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import explicit.modelviews.MDPView;
import parser.State;
import parser.Values;
import parser.VarList;
import prism.PrismException;
import strat.MDStrategy;

/**
* Explicit-state representation of am MDP, constructed (implicitly)
* from an MDP and a memoryless deterministic (MD) adversary,
* i.e., the MDP is restricted to a single choice in each state. 
* This class is read-only: most of the data is pointers to other model info.
*/
public class MDPFromMDPAndMDStrategy<Value> extends MDPView<Value>
{
	// Parent MDP
	protected MDP<Value> mdp;
	// MD strategy
	protected MDStrategy strat;

	/**
	 * Constructor: create from MDP and memoryless adversary.
	 */
	public MDPFromMDPAndMDStrategy(MDP<Value> mdp, MDStrategy strat)
	{
		this.mdp = mdp;
		this.strat = strat;
	}
	
	private int getChoiceIndex(int s)
	{
		return strat.isChoiceDefined(s) ? strat.getChoiceIndex(s) : 0;
	}
	
	// Accessors (for Model)

	public int getNumStates()
	{
		return mdp.getNumStates();
	}

	public int getNumInitialStates()
	{
		return mdp.getNumInitialStates();
	}

	public Iterable<Integer> getInitialStates()
	{
		return mdp.getInitialStates();
	}

	public int getFirstInitialState()
	{
		return mdp.getFirstInitialState();
	}

	public boolean isInitialState(int i)
	{
		return mdp.isInitialState(i);
	}

	public boolean isDeadlockState(int i)
	{
		return mdp.isDeadlockState(i);
	}

	public List<State> getStatesList()
	{
		return mdp.getStatesList();
	}

	public Values getConstantValues()
	{
		return mdp.getConstantValues();
	}

	public int getNumTransitions(int s)
	{
		return mdp.getNumTransitions(s, getChoiceIndex(s));
	}

	public SuccessorsIterator getSuccessors(final int s)
	{
		return mdp.getSuccessors(s, getChoiceIndex(s));
	}

	public int getNumChoices(int s)
	{
		return 1;
	}

	public void findDeadlocks(boolean fix) throws PrismException
	{
		// No deadlocks by definition
	}

	public void checkForDeadlocks() throws PrismException
	{
		// No deadlocks by definition
	}

	public void checkForDeadlocks(BitSet except) throws PrismException
	{
		// No deadlocks by definition
	}

	// Accessors (for DTMC)

	@Override
	public Iterator<Entry<Integer, Value>> getTransitionsIterator(int s, int i)
	{
		return mdp.getTransitionsIterator(s, getChoiceIndex(s));
	}


	/*
	
	@Override
	public void forEachTransition(int s, TransitionConsumer<Value> c)
	{
		if (!strat.isChoiceDefined(s)) {
			return;
		}
		mdp.forEachTransition(s, strat.getChoiceIndex(s), c::accept);
	}

	@Override
	public double mvMultSingle(int s, double vect[])
	{
		return strat.isChoiceDefined(s) ? mdp.mvMultSingle(s, strat.getChoiceIndex(s), vect) : 0;
	}

	@Override
	public double mvMultJacSingle(int s, double vect[])
	{
		return strat.isChoiceDefined(s) ? mdp.mvMultJacSingle(s, strat.getChoiceIndex(s), vect) : 0;
	}

	@Override
	public double mvMultRewSingle(int s, double vect[], MCRewards<Double> mcRewards)
	{
		return strat.isChoiceDefined(s) ? mdp.mvMultRewSingle(s, strat.getChoiceIndex(s), vect, mcRewards) : 0;
	}

	@Override
	public void vmMult(double vect[], double result[])
	{
		throw new RuntimeException("Not implemented yet");
	}

*/
	
	@Override
	public boolean equals(Object o)
	{
		throw new RuntimeException("Not implemented yet");
	}


	@Override
	public Object getAction(int s, int i)
	{
		return mdp.getAction(s, getChoiceIndex(s));
	}


	@Override
	public VarList getVarList()
	{
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public BitSet getLabelStates(String name)
	{
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Set<String> getLabels()
	{
		return mdp.getLabels();
	}


	@Override
	public boolean hasLabel(String name)
	{
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	protected void fixDeadlocks()
	{
		// TODO Auto-generated method stub
		
	}
}
