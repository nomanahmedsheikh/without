package org.utd.cs.mln.alchemy.core;

import java.util.ArrayList;
import java.util.List;

public class Atom {
	public PredicateSymbol symbol;
	// terms may be shared across atoms
	public List<Term> terms;
	public ArrayList<HyperCube> hyperCubes = new ArrayList<HyperCube>();

	// Conditions: terms.size()=symbol.variables.size() and
	// terms[i].type=symbol.variables[i]
	public Atom() {
		terms = new ArrayList<Term>();
	}

	public Atom(PredicateSymbol symbol_, List<Term> terms_) {
		symbol = (symbol_);
		terms = (terms_);
	}

	public Atom(Atom atom) {
		symbol = atom.symbol;
		terms = new ArrayList<Term>();
		for (int i = 0; i < atom.terms.size(); i++) {
			Term tm = new Term(atom.terms.get(i));
			terms.add(tm);
		}
	}

	public boolean isConstant() {
		boolean constantDomain = true;
		for(int i=0;i<terms.size();i++)
		{
			if(terms.get(i).domain.size() > 1)
			{
				constantDomain = false;
				break;
			}
		}
		return constantDomain;

	}

	public boolean isSingletonAtom(int variableIndex) {

		if(terms.size() == 1)
		{
			variableIndex = 0;
			return true;
		}
		variableIndex = -1;
		int numVariables = 0;
		for(int i=0;i<terms.size();i++)
		{
			if(terms.get(i).domain.size() > 1)
			{
				//variable
				numVariables++;
				if(numVariables > 1)
					break;
				//store index of the first variable
				if(variableIndex == -1)
					variableIndex = i;
			}
		}
		//singleton if no variables or 1 variable
		if(numVariables == 1)
			return true;
		else
			return false;

	}

	public int getNumberOfGroundings() {

		int numberOfGroundings=1;
		for(int i=0;i<terms.size();i++)
		{
			numberOfGroundings *= terms.get(i).domain.size();
		}
		return numberOfGroundings;
	}

	public void print() {

		System.out.print(symbol.symbol + "[ID:: " +  symbol.id + " wt:: " + symbol.pweight + "," + symbol.nweight + "]" + " ( ");
		for(int j=0;j<terms.size();j++){
			if((int)terms.get(j).domain.size()==1){
				System.out.print(terms.get(j).domain.get(0));
			}
			else{
				System.out.print(terms.get(j) + "[#" + terms.get(j).domain.size() + "]");
			}
			if(j!=terms.size()-1)
				System.out.print(", ");
		}
		System.out.print(")");

	}

	public int hasSingletonSegment(WClause clause) {
		ArrayList<Integer> clauseTermIds = new ArrayList<Integer>();
		for(Term term : terms){
			clauseTermIds.add(clause.terms.indexOf(term));
		}
		int predPosition = -1;
		for(HyperCube hyperCube : clause.hyperCubes){
			int nonSingletonSegmentTermId = -1;
			int countNonSingletonSegments = 0;
			for(Integer termId : clauseTermIds){
				if(hyperCube.varConstants.get(termId).size() > 1){
					nonSingletonSegmentTermId = termId;
					countNonSingletonSegments++;
				}
			}
			if(countNonSingletonSegments <= 1){
				predPosition = terms.indexOf(clause.terms.get(nonSingletonSegmentTermId));
				return predPosition;
			}
		}
		return predPosition;	
	}

    // Marginal Map
    public String toString(){
        String result="";
        String predicateSymbolName=this.symbol.toString()+"(";
        Term tempTerm;
        for (int i = 0; i < terms.size(); i++) {
            tempTerm=terms.get(i);
            result+=tempTerm.toString()+", ";
        }
        return result+") ";
    }
	
} // Class ends here
