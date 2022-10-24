//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
//	* Nishan Kamaleson <nxk249@bham.ac.uk> (University of Birmingham)
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

import java.util.Collections;
import java.util.List;

import parser.Values;
import parser.VarList;
import parser.ast.DeclarationBool;
import parser.ast.DeclarationIntUnbounded;
import parser.ast.DeclarationType;
import parser.type.Type;
import parser.type.TypeBool;
import parser.type.TypeInt;

/**
 * Interface for classes that provide some basic (syntactic) information about a probabilistic model.
 */
public interface ModelInfo
{
	/**
	 * Get the type of probabilistic model.
	 */
	public ModelType getModelType();

	/**
	 * Set values for *some* undefined constants.
	 * If there are no undefined constants, {@code someValues} can be null.
	 * Undefined constants can be subsequently redefined to different values with the same method.
	 * The current constant values (if set) are available via {@link #getConstantValues()}.
	 * <br>
	 * Constant values are evaluated using standard (integer, floating-point) arithmetic.
	 */
	public default void setSomeUndefinedConstants(Values someValues) throws PrismException
	{
		// By default, assume there are no constants to define 
		if (someValues != null && someValues.getNumValues() > 0)
			throw new PrismException("This model has no constants to set");
	}

	/**
	 * Set values for *some* undefined constants.
	 * If there are no undefined constants, {@code someValues} can be null.
	 * Undefined constants can be subsequently redefined to different values with the same method.
	 * The current constant values (if set) are available via {@link #getConstantValues()}.
	 * <br>
	 * Constant values are evaluated using either using standard (integer, floating-point) arithmetic
	 * or exact arithmetic, depending on the value of the {@code exact} flag.
	 */
	public default void setSomeUndefinedConstants(Values someValues, boolean exact) throws PrismException
	{
		// default implementation: use implementation for setSomeUndefinedConstants(Values)
		// for non-exact, error for exact
		//
		// implementers should override both this method and setSomeUndefinedConstants(Values)
		// above
		if (!exact)
			setSomeUndefinedConstants(someValues);
		else
			throw new PrismException("This model can not set constants in exact mode");
	}

	/**
	 * Get access to the values for all constants in the model, including the 
	 * undefined constants set previously via the method {@link #setUndefinedConstants(Values)}.
	 * Until they are set for the first time, this method returns null.  
	 */
	public default Values getConstantValues()
	{
		// By default, assume there are no constants to define 
		return new Values();
	}

	/**
	 * Does the model contain unbounded variables?
	 */
	public default boolean containsUnboundedVariables()
	{
		// By default, assume all variables are finite-ranging
		return false;
	}

	/**
	 * Get the number of variables in the model. 
	 */
	public default int getNumVars()
	{
		// Default implementation just extracts from getVarNames() 
		return getVarNames().size();
	}
	
	/**
	 * Get the names of all the variables in the model.
	 */
	public List<String> getVarNames();
	
	/**
	 * Look up the index of a variable in the model by name.
	 * Returns -1 if there is no such variable.
	 */
	public default int getVarIndex(String name)
	{
		// Default implementation just extracts from getVarNames() 
		return getVarNames().indexOf(name);
	}

	/**
	 * Get the name of the {@code i}th variable in the model.
	 * {@code i} should always be between 0 and getNumVars() - 1. 
	 */
	public default String getVarName(int i)
	{
		// Default implementation just extracts from getVarNames() 
		return getVarNames().get(i);
	}

	/**
	 * Get the types of all the variables in the model.
	 */
	public List<Type> getVarTypes();

	/**
	 * Get the type of the {@code i}th variable in the model.
	 * {@code i} should always be between 0 and getNumVars() - 1. 
	 */
	public default Type getVarType(int i) throws PrismException
	{
		// Default implementation just extracts from getVarTypes() 
		return getVarTypes().get(i);
	}

	/**
	 * Get a declaration providing more information about
	 * the type of the {@code i}th variable in the model.
	 * For example, for integer variables, this can define
	 * the lower and upper bounds of the range of the variable.
	 * This is specified using a subclass of {@link DeclarationType},
	 * which specifies info such as bounds using {@link Expression} objects.
	 * These can use constants which will later be supplied,
	 * e.g., via the {@link #setSomeUndefinedConstants(Values) method.
	 * If this method is not provided, a default implementation supplies sensible
	 * declarations, but these are _unbounded_ for integers.
	 * {@code i} should always be between 0 and getNumVars() - 1.
	 */
	public default DeclarationType getVarDeclarationType(int i) throws PrismException
	{
		Type type = getVarType(i);
		// Construct default declarations for basic types (int/boolean)
		if (type instanceof TypeInt) {
			return new DeclarationIntUnbounded();
		} else if (type instanceof TypeBool) {
			return new DeclarationBool();
		}
		throw new PrismException("No default declaration avaiable for type " + type);
	}
	
	/**
	 * Get the (optionally specified) "module" that the {@code i}th variable in
	 * the model belongs to (e.g., the PRISM language divides models into such modules).
	 * This method returns the index of module; use {@link #getModuleName(int)}
	 * to obtain the name of the corresponding module.
	 * If there is no module info, or the variable is "global" and does not belong
	 * to a specific model, this returns -1. A default implementation always returns -1.
	 * {@code i} should always be between 0 and getNumVars() - 1.
	 */
	public default int getVarModuleIndex(int i)
	{
		// Default is -1 (unspecified or "global")
		return -1;
	}

	/**
	 * Get the name of the {@code i}th "module"; these are optionally used to
	 * organise variables within the model, e.g., in the PRISM modelling language.
	 * The module containing a variable is available via {@link #getVarModuleIndex(int)}.
	 * This method should return a valid module name for any (non -1)
	 * value returned by {@link #getVarModuleIndex(int)}.
	 */
	public default String getModuleName(int i)
	{
		// No names needed by default
		return null;
	}

	/**
	 * Create a {@link VarList} object, collating information about all the variables
	 * in the model. This provides various helper functions to work with variables.
	 * This list is created once all undefined constant values have been provided.
	 * A default implementation of this method creates a {@link VarList} object
	 * automatically from the variable info supplied by {@link ModelInfo}.
	 * Generally, this requires {@link #getVarDeclarationType(int)} to
	 * be properly implemented (beyond the default implementation) so that
	 * variable ranges can be determined.
	 */
	public default VarList createVarList() throws PrismException
	{
		return new VarList(this); 
	}
	
	/**
	 * Get a list of the names of variables that have been declared to be observable
	 * (for partially observable models)
	 */
	public default List<String> getObservableVars()
	{
		// Default implementation assumes no observable variables 
		return Collections.emptyList();
	}
	
	/**
	 * Get a short description of the action strings associated with transitions/choices.
	 * For example, for a PRISM model, this is "Module/[action]".
	 * The default implementation just returns "Action".
  	 */
	public default String getActionStringDescription()
	{
		return "Action";
	}
	
	/**
	 * Get the number of labels (atomic propositions) defined for the model. 
	 */
	public default int getNumLabels()
	{
		// Default implementation just extracts from getLabelNames() 
		return getLabelNames().size();
	}
	
	/**
	 * Get the names of all the labels in the model.
	 */
	public default List<String> getLabelNames()
	{
		// No labels by default
		return Collections.emptyList();
	}
	
	/**
	 * Get the name of the {@code i}th label of the model.
	 * {@code i} should always be between 0 and getNumLabels() - 1. 
	 */
	public default String getLabelName(int i) throws PrismException
	{
		// Default implementation just extracts from getLabelNames() 
		try {
			return getLabelNames().get(i);
		} catch (IndexOutOfBoundsException e) {
			throw new PrismException("Label number " + i + " not defined");
		}
	}
	
	/**
	 * Get the index of the label with name {@code name}.
	 * Indexed from 0. Returns -1 if label of that name does not exist.
	 */
	public default int getLabelIndex(String name)
	{
		// Default implementation just extracts from getLabelNames() 
		return getLabelNames().indexOf(name);
	}
}
