package org.utd.cs.mln.alchemy.core;

import org.utd.cs.gm.core.LogDouble;
import org.utd.cs.gm.utility.Pair;

import static jcuda.driver.JCudaDriver.*;

import jcuda.*;
import jcuda.driver.*;

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

    int totalPredicates; //Count of distinct predicates in database
    CUdeviceptr[] interpretation; //Truth values for different groundings of predicates


    public State(GroundMLN groundMLN) {
        this.groundMLN = groundMLN;
        int numGroundPreds = groundMLN.groundPredicates.size();
        for (int i = 0; i < numGroundPreds; i++) {
            truthVals.add(0);
            wtsPerPredPerVal.add(new ArrayList<>(Collections.nCopies(groundMLN.groundPredicates.get(i).numPossibleValues, 0.0)));
        }
        int numGroundFormulas = groundMLN.groundFormulas.size();
        for (int i = 0; i < numGroundFormulas; i++) {
            falseClausesSet.add(new HashSet<Integer>());
            int numGroundClauses = groundMLN.groundFormulas.get(i).groundClauses.size();
            numTrueLiterals.add(new ArrayList<Integer>());
            for (int j = 0; j < numGroundClauses; j++) {
                numTrueLiterals.get(i).add(0);
            }
        }

        buildGpuStructures();
    }

    public void buildGpuStructures() {
        totalPredicates = groundMLN.allSymbols.size();
        interpretation = new CUdeviceptr[totalPredicates + 1];
        for (int i = 0; i <= totalPredicates; i++) {
            interpretation[i] = new CUdeviceptr();
        }

        int[][] groundings = new int[totalPredicates + 1][];
        int numGroundPreds = groundMLN.allGroundPredicates.size();
        Set<Integer> symbolsdone = new HashSet<Integer>();
        for (int i = 0; i < numGroundPreds; i++) {
            GroundPredicate gp = groundMLN.allGroundPredicates.get(i);
            int gpsym = gp.symbol.id + 1;
            if (!symbolsdone.contains(gpsym)) {
                groundings[gpsym] = new int[gp.totalGroundings];
                symbolsdone.add(gpsym);
            }
            if (groundMLN.evidence.predIdVal.containsKey(i))
                groundings[gpsym][gp.indexInState] = groundMLN.evidence.predIdVal.get(i);
        }

        for (int i = 1; i <= totalPredicates; i++)
            initInterpretation(i, groundings[i]);
    }

    public void setGroundFormulaWtsToParentWts(MLN mln) {
        for (GroundFormula gf : groundMLN.groundFormulas) {
            int parentFormulaId = gf.parentFormulaId;
            gf.weight = new LogDouble(mln.formulas.get(parentFormulaId).weight.getValue(), true);
        }
    }

    public int[] getNumTrueGndings(int numWts) {
        int[] numTrueGndings = new int[numWts];
        for (GroundFormula gf : groundMLN.groundFormulas) {
            boolean isFormulaSatisfied = true;
            for (GroundClause gc : gf.groundClauses) {
                boolean isClauseSatisfied = false;
                for (Integer gpId : gc.groundPredIndices) {
                    BitSet b = gc.grounPredBitSet.get(gc.globalToLocalPredIndex.get(gpId));
                    int trueVal = truthVals.get(gpId);
                    isClauseSatisfied |= b.get(trueVal);
                    if (isClauseSatisfied)
                        break;
                }
                isFormulaSatisfied &= isClauseSatisfied;
                if (!isFormulaSatisfied)
                    break;
            }
            if (isFormulaSatisfied) {
                int parentFormulaId = gf.parentFormulaId;
                numTrueGndings[parentFormulaId]++;
            }
        }
        return numTrueGndings;
    }

    public int getTruthVals(int id) {
        return truthVals.get(id);
    }

    public void setTruthVals(int id, int assignment) {
        truthVals.set(id, assignment);
        GroundPredicate gp = groundMLN.groundPredicates.get(id);
        int predId = gp.symbol.id + 1;
        int stateIdx = gp.indexInState;

        CUdeviceptr interpretationWithOffset = interpretation[predId].withByteOffset(stateIdx * Sizeof.INT);
        assert cuMemcpyHtoD(interpretationWithOffset, Pointer.to(new int[]{assignment}), Sizeof.INT) ==
                CUresult.CUDA_SUCCESS;
    }


    public void initInterpretation(int predId, int[] groundings) {
        if (predId <= 0 || predId > totalPredicates)
            throw new IndexOutOfBoundsException("Index " + predId + " for interpretation is out of bound. " +
                    "Index range is " + 1 + " to " + totalPredicates);

        assert cuMemAlloc(interpretation[predId], groundings.length * Sizeof.INT) == CUresult.CUDA_SUCCESS;
        assert cuMemcpyHtoD(interpretation[predId], Pointer.to(groundings), groundings.length * Sizeof.INT) ==
                CUresult.CUDA_SUCCESS;
    }

    public CUdeviceptr[] getInterpretation() {
        return interpretation;
    }


}
