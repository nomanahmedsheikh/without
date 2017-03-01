package org.utd.cs.mln.inference;

import org.utd.cs.gm.utility.Pair;
import org.utd.cs.mln.alchemy.core.GroundMLN;
import org.utd.cs.mln.alchemy.core.State;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Happy on 3/1/17.
 */
public class GibbsSampler {
    public State state;
    public List<Pair> countNumTrueFalse = new ArrayList<>(); // For each groundPred in state.mln.groundPreds, stores how many times this groundpred gets assigned true and false. Used for calculating marginal prob
    int numBurnSteps;
    GibbsSampler(GroundMLN groundMLN, int numBurnSteps)
    {
        state = new State(groundMLN);
        this.numBurnSteps = numBurnSteps;
        int numGroundPreds = groundMLN.groundPredicates.size();
        for(int i = 0 ; i < numGroundPreds ; i++)
        {
            countNumTrueFalse.add(new Pair(0,0));
        }
    }
}
