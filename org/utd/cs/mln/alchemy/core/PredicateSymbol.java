package org.utd.cs.mln.alchemy.core;

import java.util.ArrayList;
import java.util.List;

import org.utd.cs.gm.core.LogDouble;

public class PredicateSymbol {

	public int id;
	public int parentId;
	public String symbol;
	public List<Integer> variable_types = new ArrayList<Integer>();
	public boolean isOriginalSymbol;
	public LogDouble pweight;
	public LogDouble nweight;

	public String printString;

	// True : for MAP predicate, False: for marginal predicate
	public boolean queryType;

	public PredicateSymbol() {
	}

	public PredicateSymbol(int id_, String symbol_, List<Integer> var_types,
			LogDouble pweight_, LogDouble nweight_) {
		id = id_;
		symbol = symbol_;
		variable_types = var_types;
		pweight = pweight_;
		nweight = nweight_;
		parentId = id;
		
		printString = symbol_ + "(";
		for (int i = 0; i < var_types.size() - 1; i++) {
			printString += "_, ";
		}
		printString += "_)";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + (isOriginalSymbol ? 1231 : 1237);
		result = prime * result + ((nweight == null) ? 0 : nweight.hashCode());
		result = prime * result + parentId;
		result = prime * result
				+ ((printString == null) ? 0 : printString.hashCode());
		result = prime * result + ((pweight == null) ? 0 : pweight.hashCode());
		result = prime * result + ((symbol == null) ? 0 : symbol.hashCode());
		result = prime * result
				+ ((variable_types == null) ? 0 : variable_types.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PredicateSymbol other = (PredicateSymbol) obj;
		if (id != other.id)
			return false;
		if (isOriginalSymbol != other.isOriginalSymbol)
			return false;
		if (nweight == null) {
			if (other.nweight != null)
				return false;
		} else if (!nweight.equals(other.nweight))
			return false;
		if (parentId != other.parentId)
			return false;
		if (printString == null) {
			if (other.printString != null)
				return false;
		} else if (!printString.equals(other.printString))
			return false;
		if (pweight == null) {
			if (other.pweight != null)
				return false;
		} else if (!pweight.equals(other.pweight))
			return false;
		if (symbol == null) {
			if (other.symbol != null)
				return false;
		} else if (!symbol.equals(other.symbol))
			return false;
		if (variable_types == null) {
			if (other.variable_types != null)
				return false;
		} else if (!variable_types.equals(other.variable_types))
			return false;
		return true;
	}

	@Override
	public String toString() {
		if(printString == null)
			return symbol + "_" + id + "()";
		return printString;
	}

}
