package org.utd.cs.mln.inference;

import org.utd.cs.gm.utility.DeepCopyUtil;
import org.utd.cs.mln.alchemy.core.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Happy on 3/2/17.
 */
public class GibbsSampler_v2 {
    public State state;
    public List<List<Integer>> countNumAssignments = new ArrayList<>(); // For each groundPred in state.mln.groundPreds, stores how many times this groundpred gets assigned to a  particular value. Used for calculating marginal prob
    public List<List<Double>> marginals = new ArrayList<>();
    int numBurnSteps, numIter;

    GibbsSampler_v2(GroundMLN groundMLN, int numBurnSteps, int numIter)
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

        // Intialize all counts of assignments to 0.
        for(int i = 0 ; i < numGroundPreds ; i++)
        {
            int numValues = state.groundMLN.groundPredicates.get(i).numPossibleValues;
            marginals.add(new ArrayList<>(Collections.nCopies(numValues,0.0)));
        }
    }

    private void init()
    {
        doInitialRandomAssignment();
        initializeNumSatValues(); // Initialize numFalseClauseIds and numSatLiterals in the clauses
        ArrayList<Integer> allGndPredIndices = new ArrayList<>();
        int numGndPreds = state.groundMLN.groundPredicates.size();
        for(int i = 0 ; i < numGndPreds ; i++)
        {
            allGndPredIndices.add(i);
        }
        updateWtsForGndPreds(allGndPredIndices);
    }

    private void doInitialRandomAssignment() {
        GroundMLN gmln = state.groundMLN;
        List<GroundPredicate> gpList = gmln.groundPredicates;
        int numGps = gpList.size();
        /*
        for(int i = 0 ; i < numGps ; i++)
        {
            int numPossibleVals = gpList.get(i).numPossibleValues;
            int assignment = getUniformAssignment(numPossibleVals);
            state.truthVals.set(i,assignment);
        }*/
        state.truthVals.set(0,0);
        state.truthVals.set(1,1);


    }

    // According to present state, initialize state.numFalseClausesSet and state.numTrueLiterals
    private void initializeNumSatValues() {
        GroundMLN gm = state.groundMLN;
        int numFormulas = state.groundMLN.groundFormulas.size();
        for(int i = 0 ; i < numFormulas ; i++)
        {
            GroundFormula gf = gm.groundFormulas.get(i);
            Set<Integer> falseClauseIds = new HashSet<Integer>();
            int numClauses = gf.groundClauses.size();
            for(int j = 0 ; j < numClauses ; j++)
            {
                GroundClause gc = gf.groundClauses.get(j);
                int numPreds = gc.groundPredIndices.size();
                ArrayList<Integer> numSatLiteralsPerPred = new ArrayList<>(Collections.nCopies(numPreds,0));
                int numAtoms = gc.groundAtoms.size();
                int sumSatLiterals = 0;
                for(int k = 0 ; k < numAtoms ; k++)
                {
                    GroundAtom ga = gc.groundAtoms.get(k);
                    int gndPredIndex = ga.groundPredIndex;
                    int currentAssignment = state.truthVals.get(gndPredIndex);
                    // If current atom is true literal
                    if((ga.valTrue == currentAssignment) != ga.sign)
                    {
                        int gpIndex = ga.clauseGroundPredIndex;
                        // TODO : This increment may be inefficient. Can we use AtomicInteger?
                        numSatLiteralsPerPred.set(gpIndex, numSatLiteralsPerPred.get(gpIndex)+1);
                        sumSatLiterals++;
                    }
                } // end of clause
                for(int l = 0 ; l < numPreds ; l++)
                {
                    numSatLiteralsPerPred.set(l, sumSatLiterals - numSatLiteralsPerPred.get(l));
                }

                if(sumSatLiterals == 0)
                {
                    falseClauseIds.add(j);
                }
                state.numTrueLiterals.get(i).set(j, numSatLiteralsPerPred);
            }// end of formula
            state.falseClausesSet.set(i,falseClauseIds);
        }
    }

    public void infer(String out_file)
    {
        init();

        int numGndPreds = state.groundMLN.groundPredicates.size();
        // Burning in

        System.out.println("Burning in started...");
        long time = System.currentTimeMillis();

        for(int i =0 ; i < numBurnSteps; i++)
        {
            for(int gpId =0; gpId < numGndPreds; gpId++){
                performGibbsStep(gpId);

            }
            if(i%1 == 0) {
                System.out.println("iter : " + i + ", Elapsed Time : " + (System.currentTimeMillis() - time) / 1000.0 + " s");
            }
        }


        System.out.println("Time taken to burn in : " + (System.currentTimeMillis() - time)/1000.0 + " s");

        System.out.println("Gibbs sampling started...");
        time = System.currentTimeMillis();
        for(int i =0 ; i < numIter; i++)
        {
            for(int gpId =0; gpId < numGndPreds; gpId++){
                int assignment = performGibbsStep(gpId);
                countNumAssignments.get(gpId).set(assignment, countNumAssignments.get(gpId).get(assignment)+1);
            }
            if(i%100 == 0) {
                System.out.println("iter : " + i + ", Elapsed Time : " + (System.currentTimeMillis() - time) / 1000.0 + " s");
            }
        }

        for(int i = 0 ; i < numGndPreds ; i++)
        {
            double sum = 0.0;
            int numValues = countNumAssignments.get(i).size();
            for(int j = 0 ; j < numValues ; j++)
            {
                sum += countNumAssignments.get(i).get(j);
            }
            for(int j = 0 ; j < numValues ; j++)
            {
                double marg = countNumAssignments.get(i).get(j)/sum;
                marginals.get(i).set(j,marg);
            }
        }
        writeMarginal(marginals, out_file);
        System.out.println("Gibbs sampling completed in : " + (System.currentTimeMillis() - time)/1000.0 + " s");
    }

    private void writeMarginal(List<List<Double>> marginals, String out_file)
    {
        try
        {
            PrintWriter writer = new PrintWriter(out_file);
            for(int i = 0 ; i < marginals.size() ; i++)
            {
                for(int j = 0 ; j < marginals.get(i).size() ; j++)
                {
                    double marginal = marginals.get(i).get(j);
                    writer.println(state.groundMLN.groundPredicates.get(i) + " = " + j + " " + marginal);
                }
            }
            writer.close();
        }
        catch(IOException e) {
        }
    }

    private int performGibbsStep(int gpId) {
        int assignment = get_probabilistic_assignment(state.wtsPerPredPerVal.get(gpId));
        int prev_assignment = state.truthVals.get(gpId);
        state.truthVals.set(gpId, assignment);
        if(assignment != prev_assignment)
        {
            List<Integer> affectedGndPredIndices = new ArrayList<>(); // Markov Blanket for current flipped atom. When flipping an atom, if the value changes then we need to update satWeights for all these M.B predicates.
            findMarkovBlanket(gpId, assignment, prev_assignment, affectedGndPredIndices);
            updateWtsForGndPreds(affectedGndPredIndices);
        }
        return assignment;
    }

    private void findMarkovBlanket(int gpId, int assignment, int prev_assignment, List<Integer> markov_blanket) {
        Set<Integer> mbSet = new HashSet<>();
        GroundPredicate gp = state.groundMLN.groundPredicates.get(gpId);
        for(int formulaId : gp.groundFormulaIds.keySet())
        {
            for(int cid : gp.groundFormulaIds.get(formulaId))
            {
                GroundClause gc = state.groundMLN.groundFormulas.get(formulaId).groundClauses.get(cid);
                int localPredindex = gc.globalToLocalPredIndex.get(gpId);
                int satDifference = 0; // numsatLiterals according to new assignment - old assignment
                int sumSatLiterals = 0;
                for(int aid : gc.localPredIndexToAtomIndices.get(localPredindex))
                {
                    GroundAtom ga = gc.groundAtoms.get(aid);
                    if((prev_assignment == ga.valTrue) != ga.sign)
                    {
                        satDifference--;
                    }
                    if((assignment == ga.valTrue) != ga.sign)
                    {
                        satDifference++;
                    }
                }
                for(int otherGpId : gc.groundPredIndices)
                {
                    if(otherGpId != gpId)
                    {
                        mbSet.add(otherGpId);
                        int localGpIndex = gc.globalToLocalPredIndex.get(otherGpId);
                        int oldNumSatLiterals = state.numTrueLiterals.get(formulaId).get(cid).get(localGpIndex);
                        int newNumSatLiterals = oldNumSatLiterals+satDifference;
                        state.numTrueLiterals.get(formulaId).get(cid).set(localGpIndex, newNumSatLiterals);
                        sumSatLiterals += newNumSatLiterals;
                    }
                }
                if(sumSatLiterals == 0 && state.numTrueLiterals.get(formulaId).get(cid).get(localPredindex) == 0)
                {
                    state.falseClausesSet.get(formulaId).remove(cid);
                }
                else
                {
                    state.falseClausesSet.get(formulaId).add(cid);
                }
            }
        }
        markov_blanket.addAll(mbSet);
    }

    private void updateWtsForGndPreds(List<Integer> affectedGndPredIndices) {
        List<GroundPredicate> groundPredicates = state.groundMLN.groundPredicates;
        for(Integer i : affectedGndPredIndices)
        {
            GroundPredicate gp = groundPredicates.get(i);
            int numPossibleVals =gp.numPossibleValues;
            double wtsPerVal[] = new double[numPossibleVals];
            Map<Integer, Set<Integer>> formulaIds = gp.groundFormulaIds;

            for(Integer formulaId : formulaIds.keySet())
            {
                GroundFormula gf = state.groundMLN.groundFormulas.get(formulaId);
                double wt = gf.weight.getValue();
                Set<Integer> tempSet = (Set<Integer>) DeepCopyUtil.copy(state.falseClausesSet.get(formulaId));
                tempSet.removeAll(formulaIds.get(formulaId));
                BitSet formulaBitSet = new BitSet(numPossibleVals);
                formulaBitSet.flip(0,numPossibleVals);
                // If there is a clause which is false, and not doesn't contain gp, then this formula is always false
                if(tempSet.size() == 0)
                {
                    for(Integer cid : formulaIds.get(formulaId))
                    {
                        BitSet clauseBitSet = new BitSet(numPossibleVals);
                        GroundClause gc = gf.groundClauses.get(cid);
                        int localPredIndex = gc.globalToLocalPredIndex.get(i);
                        int numSatLiterals = state.numTrueLiterals.get(formulaId).get(cid).get(localPredIndex);
                        if(numSatLiterals > 0)
                            clauseBitSet.flip(0,numPossibleVals);
                        else
                        {
                            List<Integer> atomIndices = gc.localPredIndexToAtomIndices.get(localPredIndex);
                            for(Integer aid : atomIndices)
                            {
                                BitSet atomBitSet = new BitSet(numPossibleVals);
                                GroundAtom ga = gc.groundAtoms.get(aid);
                                atomBitSet.set(ga.valTrue);
                                if(ga.sign)
                                {
                                    atomBitSet.flip(0,numPossibleVals);
                                }
                                clauseBitSet.or(atomBitSet);
                                if(clauseBitSet.cardinality() == numPossibleVals)
                                    break;
                            }
                        }
                        formulaBitSet.and(clauseBitSet);
                    }// end clauses loop
                    int startIndex = 0;
                    while(startIndex < numPossibleVals)
                    {
                        int index = formulaBitSet.nextSetBit(startIndex);
                        if(index == -1)
                            break;
                        wtsPerVal[index] += wt;
                        startIndex = index+1;
                    }
                }// end if condition
            } //end formulas loops

            List<Double> tempWts = new ArrayList<>();
            for(double d : wtsPerVal)
            {
                tempWts.add(d);
            }
            state.wtsPerPredPerVal.set(i, tempWts);
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
