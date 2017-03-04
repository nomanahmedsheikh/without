package org.utd.cs.mln.alchemy.core;

import org.utd.cs.gm.utility.Pair;

import java.util.*;

/**
 * Created by Happy on 2/23/17.
 */
public class State {
    public GroundMLN groundMLN;
    public List<Integer> truthVals = new ArrayList<>(); // For each groundPredicate in mln.groundPredicates, stores its truthval
    public List<Set<Integer>> falseClausesSet = new ArrayList<>(); // for each groundformula, stores set of groundClauseIds which are false in this state
    public List<List<List<Integer>>> numTrueLiterals = new ArrayList<>(); // for each groundformula, for each clauseId, stores numSatLiterals for each groundPred in that clause
    public List<List<Double>> wtsPerPredPerVal = new ArrayList<>(); // For each GroundPred, stores sat wts for each value

    public State(GroundMLN groundMLN) {
        this.groundMLN  = groundMLN;
        int numGroundPreds = groundMLN.groundPredicates.size();
        for(int i = 0 ; i < numGroundPreds ; i++)
        {
            truthVals.add(0);
            wtsPerPredPerVal.add(new ArrayList<>(Collections.nCopies(groundMLN.groundPredicates.get(i).numPossibleValues,0.0)));
        }
        int numGroundFormulas = groundMLN.groundFormulas.size();
        for(int i = 0 ; i < numGroundFormulas ; i++)
        {
            falseClausesSet.add(new HashSet<>());
            int numGroundClauses = groundMLN.groundFormulas.get(i).groundClauses.size();
            numTrueLiterals.add(new ArrayList<>());
            for(int j = 0 ; j < numGroundClauses ; j++)
            {
                GroundClause gndClause = groundMLN.groundFormulas.get(i).groundClauses.get(j);
                int numGndPredsPerClause = gndClause.groundPredIndices.size();
                numTrueLiterals.get(i).add(new ArrayList<>());
                for(int k = 0 ; k < numGndPredsPerClause ; k++)
                {
                    numTrueLiterals.get(i).get(j).add(0);
                }
            }
        }
    }
}
