package org.utd.cs.mln.alchemy.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Happy on 2/21/17.
 */
public class GroundAtom {
    public GroundPredicate groundPredicate;
    public int groundPredIndex; // index of ground predicate in MLN's groundPredicateList
    public int clauseGroundPredIndex; // index of ground predicate in GroundClause's groundPredicateList
    public int valTrue; // At what value does this atom becomes true
    public boolean sign;

    public GroundAtom(GroundPredicate groundPredicate, int groundPredIndex, int clauseGroundPredIndex,
                      int valTrue, boolean sign) {
        this.groundPredicate = groundPredicate;
        this.groundPredIndex = groundPredIndex;
        this.clauseGroundPredIndex = clauseGroundPredIndex;
        this.valTrue = valTrue;
        this.sign = sign;
    }
}
