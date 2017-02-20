package org.utd.cs.mln.alchemy.core;

import java.util.ArrayList;

public class Evidence {
	public Evidence(PredicateSymbol symbol, ArrayList<Integer> values, boolean truthValue) {
		super();
		this.symbol = symbol;
		this.values = values;
		this.truthValue = truthValue;
	}
	public PredicateSymbol symbol;
	public ArrayList<Integer> values;
	public boolean truthValue;
}


