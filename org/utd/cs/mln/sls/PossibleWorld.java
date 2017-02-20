package org.utd.cs.mln.sls;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.utd.cs.mln.alchemy.core.PredicateSymbol;

public class PossibleWorld implements Cloneable{
	
	private Random random = new Random(System.currentTimeMillis());
	
	private List<PredicateSymbol> symbols = new ArrayList<PredicateSymbol>();
	
	private List<Integer> numberOfGroundings;
	
	private List<Integer> assignments = new ArrayList<Integer>();
	
	private PossibleWorld() {
	}
	
	public PossibleWorld(List<PredicateSymbol> symbols, List<Integer> numberOfGroundings) {
		this.symbols = symbols;
		this.numberOfGroundings = numberOfGroundings;
		
		for (int i = 0; i < symbols.size(); i++) {
			assignments.add(0);
		}
	}
	
	public void setRandomState() {
		for (int i = 0; i < symbols.size(); i++) {
			int symbolId = symbols.get(i).id;
			int groundings = numberOfGroundings.get(symbolId);
			int noOfTrueGrounding;
			if(random.nextBoolean()){
				noOfTrueGrounding = groundings;
			} else {
				noOfTrueGrounding = 0;
			}
				
//			assignments.set(symbolId, random.nextInt(groundings+1));
			assignments.set(symbolId, noOfTrueGrounding);
		}
	}
	
	public void setAllTrue() {
		for (int i = 0; i < symbols.size(); i++) {
			int symbolId = symbols.get(i).id;
			int groundings = numberOfGroundings.get(symbolId);
			assignments.set(symbolId, groundings);
		}
	}
	
	public void setAllFalse() {
		for (int i = 0; i < symbols.size(); i++) {
			int symbolId = symbols.get(i).id;
			assignments.set(symbolId, 0);
		}
	}
	
	public PossibleWorld clone() {
		PossibleWorld clone = new PossibleWorld();
		
		clone.symbols = symbols;
		clone.numberOfGroundings = numberOfGroundings;
		clone.assignments = new ArrayList<Integer>(assignments);
		
		return clone;
	}
	
	public int noOfGrounding(PredicateSymbol symbol) {
		return numberOfGroundings.get(symbol.id);
	}
	
	public int getNoOfTrueGrounding(PredicateSymbol symbol) {
		return assignments.get(symbol.id);
	}

	public void setNoOfTrueGrounding(PredicateSymbol symbol, int noOfTrueGrounding) {
		assert(noOfTrueGrounding >= 0 && noOfTrueGrounding <= numberOfGroundings.get(symbol.id));
		assignments.set(symbol.id, noOfTrueGrounding);
	}
	
	@Override
	public String toString() {
		String printString = "";
		for (PredicateSymbol symbol : symbols) {
			printString += symbol + " :: " + assignments.get(symbol.id) + "; \n";
		}
		return printString;
	}

}
