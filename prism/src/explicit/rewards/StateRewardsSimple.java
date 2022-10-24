//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

package explicit.rewards;

import java.util.ArrayList;

import explicit.Model;
import explicit.Product;

/**
 * Explicit-state storage of just state rewards (mutable).
 */
public class StateRewardsSimple<Value> extends StateRewards<Value>
{
	/** Number of states */
	protected int numStates;
	/** State rewards **/
	protected ArrayList<Value> stateRewards;

	/**
	 * Constructor: all zero rewards.
	 */
	public StateRewardsSimple(int numStates)
	{
		this.numStates = numStates;
		// Initially lists are just null (denoting all 0)
		stateRewards = null;
	}

	/**
	 * Copy constructor
	 * @param rews Rewards to copy
	 */
	public StateRewardsSimple(StateRewardsSimple<Value> rews)
	{
		numStates = rews.numStates;
		if (rews.stateRewards == null) {
			stateRewards = null;
		} else {
			stateRewards = new ArrayList<>(rews.stateRewards);
		}
	}

	// Mutators

	/**
	 * Set the reward for state {@code s} to {@code r}.
	 */
	public void setStateReward(int s, Value r)
	{
		// If no rewards array created yet, create it
		if (stateRewards == null) {
			stateRewards = new ArrayList<Value>(numStates);
			for (int j = 0; j < numStates; j++)
				stateRewards.add(getEvaluator().zero());
		}
		// Set reward
		stateRewards.set(s, r);
	}

	/**
	 * Add {@code r} to the state reward for state {@code s}.
	 */
	public void addToStateReward(int s, Value r)
	{
		setStateReward(s, getEvaluator().add(getStateReward(s), r));
	}

	// Accessors

	@Override
	public Value getStateReward(int s)
	{
		if (stateRewards == null)
			return getEvaluator().zero();
		return stateRewards.get(s);
	}

	// Converters
	
	@Override
	public StateRewards<Value> liftFromModel(Product<? extends Model<Value>> product)
	{
		Model<Value> modelProd = product.getProductModel();
		int numStatesProd = modelProd.getNumStates();
		StateRewardsSimple<Value> rewardsProd = new StateRewardsSimple<>(numStatesProd);
		for (int s = 0; s < numStatesProd; s++) {
			rewardsProd.setStateReward(s, getStateReward(product.getModelState(s)));
		}
		return rewardsProd;
	}
	
	// Other

	@Override
	public StateRewardsSimple<Value> deepCopy()
	{
		return new StateRewardsSimple<>(this);
	}
}
