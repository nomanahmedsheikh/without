package org.utd.cs.mln.alchemy.core;

import org.utd.cs.gm.core.LogDouble;
import org.utd.cs.gm.utility.Pair;

import java.util.*;

/**
 * Created by Happy on 2/23/17.
 */
public class State {
    public GroundMLN groundMLN;
    public List<Integer> truthVals = new ArrayList<>(); // For each groundPredicate in mln.groundPredicates, stores its truthval
    public List<Set<Integer>> falseClausesSet = new ArrayList<>(); // for each groundformula, stores set of groundClauseIds which are false in this state
    public List<List<Integer>> numTrueLiterals = new ArrayList<>(); // for each groundformula, for each clauseId, stores numSatLiterals in that clause
    public List<List<Double>> wtsPerPredPerVal = new ArrayList<>(); // For each GroundPred, stores sat wts for each value
    public ArrayList<Integer> groundedGfIndicesList = new ArrayList<>(); // Contains indices of those gfs in groundMLN, which were not present in first order mln, but directly added during code. Thses gfs have parentFormulaId as -1.

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
            falseClausesSet.add(new HashSet<Integer>());
            int numGroundClauses = groundMLN.groundFormulas.get(i).groundClauses.size();
            numTrueLiterals.add(new ArrayList<Integer>());
            for(int j = 0 ; j < numGroundClauses ; j++)
            {
                numTrueLiterals.get(i).add(0);
            }
            if(groundMLN.groundFormulas.get(i).parentFormulaId == -1)
                groundedGfIndicesList.add(i);
        }
    }

    public void setGroundFormulaWtsToParentWts(MLN mln) {
        for(GroundFormula gf : groundMLN.groundFormulas)
        {
            int parentFormulaId = gf.parentFormulaId;
            // If parent formula doesn't exist, don't do anything
            if(parentFormulaId != -1)
                gf.weight = new LogDouble(mln.formulas.get(parentFormulaId).weight.getValue(), true);
        }
    }

    public void setGroundFormulaWtsToParentWtsSoftEvidence(MLN mln, double lambda) {
        for(GroundFormula gf : groundMLN.groundFormulas)
        {
            int parentFormulaId = gf.parentFormulaId;
            if(parentFormulaId != -1)
                gf.weight = new LogDouble(mln.formulas.get(parentFormulaId).weight.getValue(), true);
            else
                gf.weight = new LogDouble(lambda*gf.originalWeight.getValue(),true);
        }
    }

    public double[] getNumTrueGndings(int numWts)
    {
        double []numTrueGndings = new double[numWts];
        for(GroundFormula gf : groundMLN.groundFormulas)
        {
            int parentFormulaId = gf.parentFormulaId;
            if(parentFormulaId == -1)
                continue;
            boolean isFormulaSatisfied = true;
            for(GroundClause gc : gf.groundClauses)
            {
                boolean isClauseSatisfied = false;
                for(Integer gpId : gc.groundPredIndices)
                {
                    BitSet b = gc.grounPredBitSet.get(gc.globalToLocalPredIndex.get(gpId));
                    int trueVal = truthVals.get(gpId);
                    isClauseSatisfied |= b.get(trueVal);
                    if(isClauseSatisfied)
                        break;
                }
                isFormulaSatisfied &= isClauseSatisfied;
                if(!isFormulaSatisfied)
                    break;
            }
            if(isFormulaSatisfied)
            {
                numTrueGndings[numWts] += gf.weight.getValue();
            }
        }
        return numTrueGndings;
    }
}
