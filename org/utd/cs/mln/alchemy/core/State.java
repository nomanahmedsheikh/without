package org.utd.cs.mln.alchemy.core;

import org.utd.cs.gm.utility.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Happy on 2/23/17.
 */
public class State {
    public GroundMLN groundMLN;
    public List<Integer> truthVals = new ArrayList<>(); // For each groundPredicate in mln.groundPredicates, stores its truthval

    public State(GroundMLN groundMLN) {
        this.groundMLN  = groundMLN;
        int numGroundPreds = groundMLN.groundPredicates.size();
        for(int i = 0 ; i < numGroundPreds ; i++)
        {
            truthVals.add(0);
        }
    }
}
