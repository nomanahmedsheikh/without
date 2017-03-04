
package org.utd.cs.mln.inference;

import org.utd.cs.gm.core.LogDouble;
import org.utd.cs.gm.utility.DeepCopyUtil;
import org.utd.cs.gm.utility.Pair;
import org.utd.cs.mln.alchemy.core.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Happy on 3/1/17.
 */
/*
public class GibbsSampler {
    public State state;
    public List<List<Integer>> countNumAssignments = new ArrayList<>(); // For each groundPred in state.mln.groundPreds, stores how many times this groundpred gets assigned to a  particular value. Used for calculating marginal prob
    int numBurnSteps, numIter;

    GibbsSampler(GroundMLN groundMLN, int numBurnSteps, int numIter)
    {
        state = new State(groundMLN);
        this.numBurnSteps = numBurnSteps;
        this.numIter = numIter;
        int numGroundPreds = groundMLN.groundPredicates.size();

        // Intialize all counts of assignments to 0.
        for(int i = 0 ; i < numGroundPreds ; i++)
        {
            int numValues = state.groundMLN.groundPredicates.get(i).numPossibleValues;
            countNumAssignments.add(new ArrayList<>(Collections.nCopies(numValues,0)));
        }
    }

    private void init()
    {
        doInitialRandomAssignment();
        initializeNumSatValues(); // Initialize numFalseClauseIds and numSatLiterals in the clauses
    }

    private void doInitialRandomAssignment() {
        GroundMLN gmln = state.groundMLN;
        List<GroundPredicate> gpList = gmln.groundPredicates;
        int numGps = gpList.size();
        for(int i = 0 ; i < numGps ; i++)
        {
            int numPossibleVals = gpList.get(i).numPossibleValues;
            int assignment = getUniformAssignment(numPossibleVals);
            state.truthVals.set(i,assignment);
        }
    }

    // According to present state, initialize state.numFalseClauseTrueLiterals
    private void initializeNumSatValues() {
        GroundMLN gm = state.groundMLN;
        int numFormulas = state.groundMLN.groundFormulas.size();
        for(int i = 0 ; i < numFormulas ; i++)
        {
            GroundFormula gf = gm.groundFormulas.get(i);
            int numFalseClauses = 0;
            int numClauses = gf.groundClauses.size();
            for(int j = 0 ; j < numClauses ; j++)
            {
                GroundClause gc = gf.groundClauses.get(j);
                int numSatLiterals = 0;
                int numAtoms = gc.groundAtoms.size();
                for(int k = 0 ; k < numAtoms ; k++)
                {
                    GroundAtom ga = gc.groundAtoms.get(k);
                    // TODO : INEFFICIENT, instead we could store reverse index in each groundpredicate which tells at what index this groundpredicate occurs in mln's list of groundpredicates
                    int gndPredIndex = gm.groundPredicates.indexOf(ga.groundPredicate);
                    int currentAssignment = state.truthVals.get(gndPredIndex);
                    // If current atom is true literal
                    if((ga.valTrue == currentAssignment) != ga.sign)
                    {
                        numSatLiterals++;
                    }
                } // end of clause

                if(numSatLiterals == 0)
                {
                    numFalseClauses++;
                }
                state.numFalseClauseTrueLiterals.get(i).set(j+1,numSatLiterals); //j+1 because first entry in this list tells about numFalseClauses.
            }// end of formula
            state.numFalseClauseTrueLiterals.get(i).set(0,numFalseClauses);
        }
    }


    public void infer(String out_file)
    {
        init();

        // Burning in
        int numGndPreds = state.groundMLN.groundPredicates.size();
        System.out.println("Burning in started...");
        long time = System.currentTimeMillis();
        for(int iter = 0 ; iter < numBurnSteps ; iter++)
        {
            for(int gndPredIndex = 0 ; gndPredIndex < numGndPreds ; gndPredIndex++)
            {
                performGibbsStep(gndPredIndex);
            }
            if(iter%1 == 0) {
                System.out.println("iter : " + iter + ", Elapsed Time : " + (System.currentTimeMillis() - time) / 1000.0 + " s");
            }

        }
        System.out.println("Time taken to burn in : " + (System.currentTimeMillis() - time)/1000.0 + " s");

        // After burning in
        System.out.println("Gibbs sampling started...");
        time = System.currentTimeMillis();

        for(int iter = 0 ; iter < numIter ; iter++)
        {
            for(int gndPredIndex = 0 ; gndPredIndex < numGndPreds ; gndPredIndex++)
            {
                int assignment = performGibbsStep(gndPredIndex);
                // TODO : This is wrong here, we need to increment counter for every groundPred, but that would be costly, is there an efficient way for this ?
                countNumAssignments.get(gndPredIndex).set(assignment, countNumAssignments.get(gndPredIndex).get(assignment) + 1);
            }
            //System.out.println(assignment);
            // Increment the count of assignments

        }
        calculateAndWriteMarginal(countNumAssignments, numIter, out_file);
        System.out.println("Gibbs sampling completed in : " + (System.currentTimeMillis() - time)/1000.0 + " s");
    }

    private int performGibbsStep(int gndPredIndex)
    {
        GroundPredicate gp = state.groundMLN.groundPredicates.get(gndPredIndex);
        int currentVal = state.truthVals.get(gndPredIndex);
        // First find sat weight for each of the value of this groundPredicate
        List<Double> satWeights = new ArrayList<>(Collections.nCopies(gp.numPossibleValues,0.0));
        // For each value, we will also store numFalseClauseTrueLiterals, so that when we flip, we can update state's numFalseClauseTrueLiterals attribute
        List<List<List<Integer>>> allNumFalseClauseTrueLiterals = new ArrayList<>();
        for(int val = 0 ; val < gp.numPossibleValues ; val++)
        {
            //TODO : May be inefficient copying whole list, instead just copy for those formulas in which this grndPred occurs
            allNumFalseClauseTrueLiterals.add((List<List<Integer>>) DeepCopyUtil.copy(state.numFalseClauseTrueLiterals));
            double satWeight = getSatWeight(gp, val, currentVal, allNumFalseClauseTrueLiterals.get(allNumFalseClauseTrueLiterals.size()-1));
            satWeights.set(val, satWeight);
        }

        // Now get an assignment according to satWeights probability distribution
        int assignment = get_probabilistic_assignment(satWeights);
        state.truthVals.set(gndPredIndex,assignment);
        state.numFalseClauseTrueLiterals = (List<List<Integer>>)DeepCopyUtil.copy(allNumFalseClauseTrueLiterals.get(assignment));
        System.gc();
        return assignment;
    }

    private double getSatWeight(GroundPredicate gp, int val, int currentVal, List<List<Integer>> tempNumFalseClauseTrueLiterals)
    {
        double wt = 0.0;
        // Loop over all the formulas in which this predicate occurs
        Map<Integer, Set<Integer>> gfIds = gp.groundFormulaIds;
        for(int formulaId : gfIds.keySet())
        {
            boolean sat = checkFormulaSatisfiability(gp, gfIds, formulaId, val, currentVal, tempNumFalseClauseTrueLiterals);
            if(sat)
            {
                wt += state.groundMLN.groundFormulas.get(formulaId).weight.getValue();
            }
        }
        return wt;
    }

    private boolean checkFormulaSatisfiability(GroundPredicate gp, Map<Integer, Set<Integer>> gfIds, int formulaId,
                                               int val, int currentVal, List<List<Integer>> tempNumFalseClauseTrueLiterals)
    {
        int numFalseClauses = state.numFalseClauseTrueLiterals.get(formulaId).get(0);
        for(int cid : gfIds.get(formulaId))
        {
            boolean sat = checkClauseSatisfiability(gp, formulaId, cid, val, currentVal, tempNumFalseClauseTrueLiterals);
            boolean currentSat = state.numFalseClauseTrueLiterals.get(formulaId).get(cid+1) > 0;
            if(sat == currentSat)
                continue;
            else if(sat)
                numFalseClauses--;
            else
                numFalseClauses++;
        }
        tempNumFalseClauseTrueLiterals.get(formulaId).set(0,numFalseClauses);
        if(numFalseClauses == 0)
            return true;
        else
            return false;
    }

    private boolean checkClauseSatisfiability(GroundPredicate gp, int formulaId, int cid, int val,
                                              int currentVal, List<List<Integer>> tempNumFalseClauseTrueLiterals)
    {
        int numSatLiterals = state.numFalseClauseTrueLiterals.get(formulaId).get(cid+1);
        GroundClause gc = state.groundMLN.groundFormulas.get(formulaId).groundClauses.get(cid);
        for(GroundAtom ga : gc.groundAtoms)
        {
            // TODO : May be inefficient, may be just check if symbols match
            if(ga.groundPredicate.equals(gp))
            {
                boolean sat = (ga.valTrue == val) != ga.sign;
                boolean currentSat = (ga.valTrue == currentVal) != ga.sign;
                if(sat == currentSat)
                    continue;
                else if(sat)
                    numSatLiterals++;
                else
                    numSatLiterals--;
            }
        }
        tempNumFalseClauseTrueLiterals.get(formulaId).set(cid+1,numSatLiterals);
        if(numSatLiterals > 0)
            return true;
        else
            return false;
    }


    private void calculateAndWriteMarginal(List<List<Integer>> countNumAssignments, int numIter, String out_file)
    {
        try
        {
            PrintWriter writer = new PrintWriter(out_file);
            for(int i = 0 ; i < countNumAssignments.size() ; i++)
            {
                for(int j = 0 ; j < countNumAssignments.get(i).size() ; j++)
                {
                    double marginal = countNumAssignments.get(i).get(j)/(double)numIter;
                    writer.println(state.groundMLN.groundPredicates.get(i) + " = " + j + " " + marginal);
                }
            }
            writer.close();
        }
        catch(IOException e) {
        }
    }


    // Given a list of sat weights for each possible assignment, find assignment probabilistically
    private int get_probabilistic_assignment(List<Double> satWeights)
    {
        int numPossibleValues = satWeights.size();
        List<Double> probabilities = new ArrayList<Double>();

        //First calculate sum of all exponentiated satWeights
        double sum = 0.0;
        for(Double wt : satWeights)
        {
            sum += Math.exp(wt);
        }
        // Now calculate probabilities
        for(Double wt : satWeights)
        {
            probabilities.add(Math.exp(wt)/sum);
        }
        int assignment = getRandomAssignment(probabilities);
        return assignment;
    }

    private int getRandomAssignment(List<Double> probabilities)
    {
        double p = Math.random();
        double cumulativeProbability = 0.0;
        for (int i = 0 ; i < probabilities.size() ; i++) {
            cumulativeProbability += probabilities.get(i);
            if (p <= cumulativeProbability) {
                return i;
            }
        }
        return probabilities.size()-1;
    }


    private int getUniformAssignment(int numPossibleVals) {
        return ThreadLocalRandom.current().nextInt(0,numPossibleVals);
    }
}
*/