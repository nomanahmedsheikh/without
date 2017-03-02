package org.utd.cs.mln.alchemy.core;

import org.utd.cs.gm.utility.Pair;

import java.util.*;

/**
 * Created by Happy on 2/23/17.
 */
public class State {
    public GroundMLN groundMLN;
    public List<Integer> truthVals = new ArrayList<>(); // For each groundPredicate in mln.groundPredicates, stores its truthval
    public List<List<Integer>> numFalseClauseTrueLiterals = new ArrayList<>(); // for each groundformula, first entry contains num of clauses in that formula which are false, then subsequent entries contain numSatLiterals for each clause in that formula

    public State(GroundMLN groundMLN) {
        this.groundMLN  = groundMLN;
        int numGroundPreds = groundMLN.groundPredicates.size();
        for(int i = 0 ; i < numGroundPreds ; i++)
        {
            truthVals.add(0);
        }
        int numGroundFormulas = groundMLN.groundFormulas.size();
        for(int i = 0 ; i < numGroundFormulas ; i++)
        {
            int numGroundClauses = groundMLN.groundFormulas.get(i).groundClauses.size();
            numFalseClauseTrueLiterals.add(new ArrayList<>(Collections.nCopies(numGroundClauses+1,0)));
        }
    }
}
