package org.utd.cs.mln.alchemy.core;

import org.utd.cs.gm.utility.Pair;

import java.util.*;

/**
 * Created by Happy on 2/27/17.
 */
public class GroundPredicate {
    public GroundPredicateSymbol symbol;
    public List<Integer> terms = new ArrayList<>();
    public int numPossibleValues; //Stores number of possible values of this groundPred
    public Map<Integer, Set<Integer>> groundFormulaIds = new HashMap<>(); // Stores which clauses this groundPred occurs in. Key : FormulaId, value : Set of cluaseIds in that formulaId in which this groundpred occurs.
    public int indexInState;    //Stores the index of the grounding in the database for GPU computation
    public int totalGroundings;

    @Override
    public String toString() {
        return symbol.symbol + terms;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GroundPredicate that = (GroundPredicate) o;

        if (!symbol.equals(that.symbol)) return false;
        return terms.equals(that.terms);
    }

    @Override
    public int hashCode() {
        int result = symbol.hashCode();
        result = 31 * result + terms.hashCode();
        return result;
    }

}
