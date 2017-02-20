package org.utd.cs.mln.marginalmap;

import org.utd.cs.mln.alchemy.core.Atom;
import org.utd.cs.mln.alchemy.core.Term;

import java.util.List;

/**
 * Created by vishal on 14/1/17.
 */

/*

Creates a tuple that tells how a rule is to be applied.
A unique id will be generated for the ruleId.
*/

public class Rule {

    static int ruleCounter=0;

    // Unique identifier for a  particular application of the applied rule tuple.
    // Unique identifier for every tuple created by application of a particular rule.
    int ruleId;

    public Rule(){
        ruleCounter++;
        this.ruleId=ruleCounter;
    }

    // Binomial/Decomposer
    // Return 0 if nothing could be applied
    // 1- Binomial
    // 2- Decomposer
    int ruleType=0;

    // Sum or MAP
    int PredicateType;

    // Index of c lause containing the predicate/variable on which the rule is to be applied
    int clauseId;

    // Predicate on which the rule is applied
    Atom predicateName;

    // List of terms on which the rule is being applied. eg in case of Decomposer
    // Size of terms for now will be 1. We are maintaining a list in case any future rule requires it.
    List<Term> terms;
}
