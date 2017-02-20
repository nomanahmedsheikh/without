package org.utd.cs.gm.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class Function {
	
	/**
	 * The scope of the function. It is assumed that the variable list is sorted in ascending order
	 */
	protected List<Variable> variables = new ArrayList<Variable>();
	
	protected List<LogDouble> table;
	
	public Function() {
		
	}
	
	public Function(List<Variable> variables) {
		this.variables.addAll(variables);
	}

	public List<Variable> getVariables() {
		return variables;
	}

	public void setVariables(List<Variable> variables) {
		this.variables.addAll(variables);
	}

	public List<LogDouble> getTable() {
		return table;
	}

	public void setTable(List<LogDouble> table) {
		this.table = table;
	}
	
	public void setTableEntry(LogDouble value, int address) {
		this.table.set(address, value);
	}
	
	public int getFunctionSize() {
		return Variable.getDomainSize(variables);
	}
	
	public Function multiply(Function function) {
		
		//Perform the set union of two function scopes.
		Set<Variable> set = new TreeSet<Variable>(); //To preserve the sorted ordering
		set.addAll(variables);
		set.addAll(function.variables);
		
		Function newFunction = new Function();
		newFunction.variables = new ArrayList<Variable>(set);
		
		newFunction.table = new ArrayList<LogDouble>();
		for (int i = 0; i < newFunction.getFunctionSize(); i++) {
			
			//Project the new set of variables in existing functions
			Variable.setAddress(newFunction.variables, i);
			int thisEntry = Variable.getAddress(variables);
			int functionEntry = Variable.getAddress(function.variables);
			
			//Multiply the two projected function entries
			newFunction.table.add(table.get(thisEntry).multiply(function.table.get(functionEntry))); 
		}
		
		return newFunction;
	}
	
	public Function maxOut(Variable v) {
		Function newFunction = new Function();
		newFunction.variables = new ArrayList<Variable>(variables);
		newFunction.variables.remove(v);
		
		newFunction.table = new ArrayList<LogDouble>();
		for (int i = 0; i < newFunction.getFunctionSize(); i++) {
			newFunction.table.add(LogDouble.ZERO);
		}
		
		for (int i = 0; i < table.size(); i++) {
			Variable.setAddress(variables, i);
			int newEntry = Variable.getAddress(newFunction.variables);
			newFunction.table.set(newEntry, LogDouble.max(newFunction.table.get(newEntry), table.get(i))); //Do a max operation
		}
		
		return newFunction;
	}
	
	public boolean contains(int varId) {
		
		for (Variable variable : variables) {
			if(variable.getId() == varId) {
				return true;
			}
		}
		
		return false;
	}

}
