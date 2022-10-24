//==============================================================================
//	
//	Copyright (c) 2021-
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

package prism;

import java.util.List;

import explicit.NondetModel;
import parser.State;
import parser.ast.RewardStruct;
import strat.MDStrategy;

public class RewardGeneratorMDStrat<Value> implements RewardGenerator<Value>
{
	private RewardGenerator<Value> rewGen;
	private NondetModel<?> model;
	private MDStrategy strat;
	
	public RewardGeneratorMDStrat(RewardGenerator<Value> rewGen, NondetModel<?> model, MDStrategy strat)
	{
		this.rewGen = rewGen;
		this.model = model;
		this.strat = strat;
	}
	
	@Override
	public Evaluator<Value> getEvaluator()
	{
		return rewGen.getEvaluator();
	}
	
	@Override
	public List<String> getRewardStructNames()
	{
		return rewGen.getRewardStructNames();
	}
	
	@Override
	public int getNumRewardStructs()
	{
		return rewGen.getNumRewardStructs();
	}
	
	@Override
	public boolean rewardStructHasStateRewards(int r)
	{
		return true;
	}
	
	@Override
	public boolean rewardStructHasTransitionRewards(int r)
	{
		// Transition rewards will be converted to state rewards
		return false;
	}
	
	@Override
	public boolean isRewardLookupSupported(RewardLookup lookup)
	{
		return rewGen.isRewardLookupSupported(lookup);
	}
	
	@Override
	public Value getStateReward(int r, State state) throws PrismException
	{
		Value sr = rewGen.getStateReward(r, state);
		int s = model.getStatesList().indexOf(state);
		Object action = model.getAction(s, strat.getChoiceIndex(s));
		Value tr = rewGen.getStateActionReward(r, state, action);
		return getEvaluator().add(sr, tr);
	}
	
	@Override
	public Value getStateActionReward(int r, State state, Object action) throws PrismException
	{
		throw new PrismException("Reward has not been defined");
	}
	
	@Override
	public Value getStateReward(int r, int s) throws PrismException
	{
		Value sr = rewGen.getStateReward(r, s);
		Object action = model.getAction(s, strat.getChoiceIndex(s));
		Value tr = rewGen.getStateActionReward(r, s, action);
		return getEvaluator().add(sr, tr);
	}

	@Override
	public Value getStateActionReward(int r, int s, Object action) throws PrismException
	{
		throw new PrismException("Reward has not been defined");
	}
	
	@Override
	public RewardStruct getRewardStruct(int r) throws PrismException
	{
		throw new PrismException("Reward has not been defined");
	}
}
