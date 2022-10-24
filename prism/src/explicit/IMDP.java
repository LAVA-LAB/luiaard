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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator;

import common.Interval;
import common.IterableStateSet;
import explicit.rewards.MDPRewards;
import parser.State;
import prism.Evaluator;
import prism.ModelType;
import prism.PrismException;

/**
 * Interface for classes that provide (read) access to an explicit-state interval MDP.
 */
public interface IMDP<Value> extends MDP<Interval<Value>>
{
	// Accessors (for Model) - default implementations
	
	@Override
	public default ModelType getModelType()
	{
		return ModelType.IMDP;
	}

	// Accessors
	
	/**
	 * Checks that transition probability interval lower bounds are positive
	 * and throws an exception if any are not.
	 */
	public default void checkLowerBoundsArePositive() throws PrismException
	{
		Evaluator<Interval<Value>> eval = getEvaluator();
		int numStates = getNumStates();
		for (int s = 0; s < numStates; s++) {
			int numChoices = getNumChoices(s);
			for (int j = 0; j < numChoices; j++) {
				Iterator<Map.Entry<Integer, Interval<Value>>> iter = getTransitionsIterator(s, j);
				while (iter.hasNext()) {
					Map.Entry<Integer, Interval<Value>> e = iter.next();
					// NB: we phrase the check as an operation on intervals, rather than
					// accessing the lower bound directly, to make use of the evaluator
					if (!eval.gt(e.getValue(), eval.zero())) {
						List<State> sl = getStatesList();
						String state = sl == null ? "" + s : sl.get(s).toString();
						throw new PrismException("Transition probability has lower bound of 0 in state " + state);
					}
				}
			}
		}
	}
	
	/**
	 * Do a matrix-vector multiplication for
	 * the DTMC's transition probability matrix P and the vector {@code vect} passed in.
	 * i.e. for all s: result[s] = sum_j P(s,j)*vect[j]
	 * @param vect Vector to multiply by
	 * @param minMax Min or max info
	 * @param result Vector to store result in
	 * @param subset Only do multiplication for these rows (ignored if null)
	 * @param complement If true, {@code subset} is taken to be its complement (ignored if {@code subset} is null)
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 */
	public default void mvMult(double vect[], MinMax minMax, double result[], BitSet subset, boolean complement, int[] strat)
	{
		mvMult(vect, minMax, result, new IterableStateSet(subset, getNumStates(), complement).iterator(), strat);
	}

	/**
	 * Do a matrix-vector multiplication for the DTMC's transition probability matrix P
	 * and the vector {@code vect} passed in, for the state indices provided by the iterator,
	 * i.e., for all s of {@code states}: result[s] = sum_j P(s,j)*vect[j]
	 * <p>
	 * If the state indices in the iterator are not distinct, the result will still be valid,
	 * but this situation should be avoided for performance reasons.
	 * @param vect Vector to multiply by
	 * @param minMax Min or max info
	 * @param result Vector to store result in
	 * @param states Perform multiplication for these rows, in the iteration order
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 */
	public default void mvMult(double vect[], MinMax minMax, double result[], PrimitiveIterator.OfInt states, int[] strat)
	{
		while (states.hasNext()) {
			int s = states.nextInt();
			result[s] = mvMultSingle(s, vect, minMax, strat);
		}
	}

	/**
	 * Do a single row of matrix-vector multiplication for
	 * the DTMC's transition probability matrix P and the vector {@code vect} passed in.
	 * i.e. return sum_j P(s,j)*vect[j]
	 * @param s Row index
	 * @param vect Vector to multiply by
	 * @param minMax Min or max info
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 */
	public default double mvMultSingle(int s, double vect[], MinMax minMax, int[] strat)
	{
		int stratCh = -1;
		double minmax = 0;
		boolean first = true;
		boolean min = minMax.isMin();

		for (int choice = 0, numChoices = getNumChoices(s); choice < numChoices; choice++) {
			// Compute sum for this distribution
			double d = mvMultSingle(s, choice, vect, minMax);

			// Check whether we have exceeded min/max so far
			if (first || (min && d < minmax) || (!min && d > minmax)) {
				minmax = d;
				// If strategy generation is enabled, remember optimal choice
				if (strat != null)
					stratCh = choice;
			}
			first = false;
		}
		// If strategy generation is enabled, store optimal choice
		if (strat != null && !first) {
			// For max, only remember strictly better choices
			if (min) {
				strat[s] = stratCh;
			} else if (strat[s] == -1 || minmax > vect[s]) {
				strat[s] = stratCh;
			}
		}

		return minmax;	
	}
	
	public default double mvMultSingle(int s, int choice, double vect[], MinMax minMax)
	{
		// One step of value iteration for IDTMCs
		// Avoid enumeration of all extreme distributions using optimisation from:
		// Three-valued abstraction for probabilistic systems,
		// Joost-Pieter Katoen, Daniel Klink, Martin Leucker and Verena Wolf
		// (Defn 17, p.372, and p.380)
		
		// Extract, for each transition, the probability interval (lo/hi)
		// and the value from vector vect for the successor state
		int numTransitions = getNumTransitions(s, choice);
		double[] probsLo = new double[numTransitions];
		double[] probsHi = new double[numTransitions];
		double[] succVals = new double[numTransitions];
		List<Integer> indices = new ArrayList<>();
		int i = 0;
		Iterator<Map.Entry<Integer, Interval<Value>>> iter = getTransitionsIterator(s, choice);
		while (iter.hasNext()) {
			Map.Entry<Integer, Interval<Value>> e = iter.next();
			@SuppressWarnings("unchecked")
			Interval<Double> intv = (Interval<Double>) e.getValue();
			probsLo[i] = intv.getLower();
			probsHi[i] = intv.getUpper();
			succVals[i] = vect[e.getKey()];
			indices.add(i);
			i++;
		}
		// Get a list of indices for the transitions,
		// sorted according to the successor values
		if (minMax.isMaxUnc()) {
			Collections.sort(indices, (o1, o2) -> -Double.compare(succVals[o1], succVals[o2]));
		} else {
			Collections.sort(indices, (o1, o2) -> Double.compare(succVals[o1], succVals[o2]));
		}
		// First add products of probability lower bounds and successor values
		double res = 0.0;
		double totP = 1.0;
		for (i = 0; i < numTransitions; i++) {
			res += succVals[i] * probsLo[i];
			totP -= probsLo[i];
		}
		// Then add remaining ones in descending order
		for (i = 0; i < numTransitions; i++) {
			int j = indices.get(i);
			double delta = probsHi[j] - probsLo[j];
			if (delta < totP) {
				res += delta * succVals[j];
				totP -= delta;
			} else {
				res += totP * succVals[j];
				break;
			}
		}
		return res;
	}
	
	/**
	 * Do a matrix-vector multiplication and sum of rewards followed by min/max, i.e. one step of value iteration.
	 * i.e. for all s: result[s] = min/max_k { rew(s) + rew_k(s) + sum_j P_k(s,j)*vect[j] }
	 * @param vect Vector to multiply by
	 * @param mdpRewards The rewards
	 * @param minMax Min or max info
	 * @param result Vector to store result in
	 * @param subset Only do multiplication for these rows (ignored if null)
	 * @param complement If true, {@code subset} is taken to be its complement (ignored if {@code subset} is null)
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 */
	public default void mvMultRew(double vect[], MDPRewards<Double> mdpRewards, MinMax minMax, double result[], BitSet subset, boolean complement, int[] strat)
	{
		mvMultRew(vect, mdpRewards, minMax, result, new IterableStateSet(subset, getNumStates(), complement).iterator(), strat);
	}

	/**
	 * Do a matrix-vector multiplication and sum of rewards followed by min/max, i.e. one step of value iteration.
	 * i.e. for all s: result[s] = min/max_k { rew(s) + rew_k(s) + sum_j P_k(s,j)*vect[j] }
	 * @param vect Vector to multiply by
	 * @param mdpRewards The rewards
	 * @param minMax Min or max info
	 * @param result Vector to store result in
	 * @param states Perform multiplication for these rows, in the iteration order
	 */
	public default void mvMultRew(double vect[], MDPRewards<Double> mdpRewards, MinMax minMax, double result[], PrimitiveIterator.OfInt states, int[] strat)
	{
		while (states.hasNext()) {
			int s = states.nextInt();
			result[s] = mvMultRewSingle(s, vect, mdpRewards, minMax, strat);
		}
	}

	/**
	 * Do a single row of matrix-vector multiplication and sum of rewards followed by min/max.
	 * i.e. return min/max_k { rew(s) + rew_k(s) + sum_j P_k(s,j)*vect[j] }
	 * Optionally, store optimal (memoryless) strategy info.
	 * @param s Row index
	 * @param vect Vector to multiply by
	 * @param mdpRewards The rewards
	 * @param minMax Min or max info
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 */
	public default double mvMultRewSingle(int s, double vect[], MDPRewards<Double> mdpRewards, MinMax minMax, int[] strat)
	{
		int stratCh = -1;
		double minmax = 0;
		boolean first = true;
		boolean min = minMax.isMin();

		for (int choice = 0, numChoices = getNumChoices(s); choice < numChoices; choice++) {
			double d = mvMultRewSingle(s, choice, vect, mdpRewards, minMax);
			// Check whether we have exceeded min/max so far
			if (first || (min && d < minmax) || (!min && d > minmax)) {
				minmax = d;
				// If strategy generation is enabled, remember optimal choice
				if (strat != null)
					stratCh = choice;
			}
			first = false;
		}
		// If strategy generation is enabled, store optimal choice
		if (strat != null && !first) {
			// For max, only remember strictly better choices
			if (min) {
				strat[s] = stratCh;
			} else if (strat[s] == -1 || minmax > vect[s]) {
				strat[s] = stratCh;
			}
		}

		return minmax;
	}

	/**
	 * Do a single row of matrix-vector multiplication and sum of rewards for a specific choice.
	 * i.e. rew(s) + rew_i(s) + sum_j P_i(s,j)*vect[j]
	 * @param s State (row) index
	 * @param i Choice index
	 * @param vect Vector to multiply by
	 * @param mdpRewards The rewards (MDP rewards)
	 * @param minMax Min or max info
	 */
	public default double mvMultRewSingle(int s, int i, double vect[], MDPRewards<Double> mdpRewards, MinMax minMax)
	{
		double d = mdpRewards.getStateReward(s);
		d += mdpRewards.getTransitionReward(s, i);
		d += mvMultSingle(s, i, vect, minMax);
		return d;
	}

	/**
	 * Do a Gauss-Seidel-style matrix-vector multiplication followed by min/max.
	 * i.e. for all s: vect[s] = min/max_k { (sum_{j!=s} P_k(s,j)*vect[j]) / 1-P_k(s,s) }
	 * and store new values directly in {@code vect} as computed.
	 * The maximum (absolute/relative) difference between old/new
	 * elements of {@code vect} is also returned.
	 * Optionally, store optimal (memoryless) strategy info.
	 * @param vect Vector to multiply by (and store the result in)
	 * @param min Min or max for (true=min, false=max)
	 * @param subset Only do multiplication for these rows (ignored if null)
	 * @param complement If true, {@code subset} is taken to be its complement (ignored if {@code subset} is null)
	 * @param absolute If true, compute absolute, rather than relative, difference
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 * @return The maximum difference between old/new elements of {@code vect}
	 */
	public default double mvMultGS(double vect[], MinMax minMax, BitSet subset, boolean complement, boolean absolute, int[] strat)
	{
		return mvMultGS(vect, minMax, new IterableStateSet(subset, getNumStates(), complement).iterator(), absolute, strat);
	}

	/**
	 * Do a Gauss-Seidel-style matrix-vector multiplication followed by min/max.
	 * i.e. for all s: vect[s] = min/max_k { (sum_{j!=s} P_k(s,j)*vect[j]) / 1-P_k(s,s) }
	 * and store new values directly in {@code vect} as computed.
	 * The maximum (absolute/relative) difference between old/new
	 * elements of {@code vect} is also returned.
	 * Optionally, store optimal (memoryless) strategy info.
	 * @param vect Vector to multiply by (and store the result in)
	 * @param min Min or max for (true=min, false=max)
	 * @param states Perform computation for these rows, in the iteration order
	 * @param absolute If true, compute absolute, rather than relative, difference
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 * @return The maximum difference between old/new elements of {@code vect}
	 */
	public default double mvMultGS(double vect[], MinMax minMax, PrimitiveIterator.OfInt states, boolean absolute, int[] strat)
	{
		double d, diff, maxDiff = 0.0;
		while (states.hasNext()) {
			final int s = states.nextInt();
			//d = mvMultJacSingle(s, vect, minMax);
			// Just do a normal (non-Jacobi) state update - not so easy to adapt for intervals
			d = mvMultSingle(s, vect, minMax, strat);
			diff = absolute ? (Math.abs(d - vect[s])) : (Math.abs(d - vect[s]) / d);
			maxDiff = diff > maxDiff ? diff : maxDiff;
			vect[s] = d;
		}
		return maxDiff;
	}

	/**
	 * Do a Gauss-Seidel-style matrix-vector multiplication and sum of rewards followed by min/max.
	 * i.e. for all s: vect[s] = min/max_k { rew(s) + rew_k(s) + (sum_{j!=s} P_k(s,j)*vect[j]) / 1-P_k(s,s) }
	 * and store new values directly in {@code vect} as computed.
	 * The maximum (absolute/relative) difference between old/new
	 * elements of {@code vect} is also returned.
	 * Optionally, store optimal (memoryless) strategy info.
	 * @param vect Vector to multiply by (and store the result in)
	 * @param mdpRewards The rewards
	 * @param minMax Min or max info
	 * @param states Perform computation for these rows, in the iteration order
	 * @param absolute If true, compute absolute, rather than relative, difference
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 * @return The maximum difference between old/new elements of {@code vect}
	 */
	public default double mvMultRewGS(double vect[], MDPRewards<Double> mdpRewards, MinMax minMax, PrimitiveIterator.OfInt states, boolean absolute, int[] strat)
	{
		double d, diff, maxDiff = 0.0;
		while (states.hasNext()) {
			final int s = states.nextInt();
			//d = mvMultJacSingle(s, vect, minMax);
			// Just do a normal (non-Jacobi) state update - not so easy to adapt for intervals
			d = mvMultRewSingle(s, vect, mdpRewards, minMax, strat);
			diff = absolute ? (Math.abs(d - vect[s])) : (Math.abs(d - vect[s]) / d);
			maxDiff = diff > maxDiff ? diff : maxDiff;
			vect[s] = d;
		}
		return maxDiff;
	}
}
