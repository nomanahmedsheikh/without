package org.utd.cs.mln.alchemy.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Happy on 2/21/17.
 */
public class GroundAtom {
    public int groundPredIndex; // index of ground predicate in MLN's groundPredicateList
    public int clauseGroundPredIndex; // index of ground predicate in GroundClause's groundPredicateList
    public int valTrue; // At what value does this atom becomes true
    public boolean sign;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GroundAtom that = (GroundAtom) o;

        if (groundPredIndex != that.groundPredIndex) return false;
        if (valTrue != that.valTrue) return false;
        return sign == that.sign;
    }

    @Override
    public int hashCode() {
        int result = groundPredIndex;
        result = 31 * result + valTrue;
        result = 31 * result + (sign ? 1 : 0);
        return result;
    }

    public GroundAtom(int groundPredIndex, int clauseGroundPredIndex,
                      int valTrue, boolean sign) {
        this.groundPredIndex = groundPredIndex;
        this.clauseGroundPredIndex = clauseGroundPredIndex;

        this.valTrue = valTrue;
        this.sign = sign;
    }
}
