package org.utd.cs.mln.alchemy.core;

import org.utd.cs.gm.utility.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Happy on 2/27/17.
 */
public class GroundPredicate {
    public GroundPredicateSymbol symbol;
    public List<Integer> terms = new ArrayList<>();
    public int numPossibleValues; //Stores number of possible values of this groundPred
    public List<List<Pair>> groundFormulaIds = new ArrayList<>(); // for each value, stores list of pair of <formula Id,clauseIndex> in which this groundPred appears. In case of non-multivalued, stores only one list of formula Ids as truth value doesn't affect M.B

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
