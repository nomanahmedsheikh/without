package org.utd.cs.gm.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CPT extends Function {
	
	private List<Variable> conditionalVariables = new ArrayList<Variable>();
	
	private Variable marginalVariable;
	
	public CPT() {
		
	}
	
	public CPT(List<Variable> conditionalVariables, Variable marginalVariable) {
		super(conditionalVariables);
		variables.add(marginalVariable);
		Collections.sort(variables);
		
		this.conditionalVariables.addAll(conditionalVariables);
		this.marginalVariable = marginalVariable;
		
		int tableSize = this.getFunctionSize();
		table = new ArrayList<LogDouble>(tableSize);
		for (int i = 0; i < tableSize; i++) {
			table.add(LogDouble.ZERO);
		}
	}

	public List<Variable> getConditionalVariables() {
		return conditionalVariables;
	}

	public void setConditionalVariables(List<Variable> conditionalVariables) {
		this.conditionalVariables.addAll(conditionalVariables);
	}

	public Variable getMarginalVariable() {
		return marginalVariable;
	}

	public void setMarginalVariable(Variable marginalVariable) {
		this.marginalVariable = marginalVariable;
	}
	
	
}
