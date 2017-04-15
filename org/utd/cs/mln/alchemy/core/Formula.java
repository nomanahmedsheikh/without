package org.utd.cs.mln.alchemy.core;

import org.utd.cs.gm.core.LogDouble;
import org.utd.cs.mln.alchemy.util.GPUutil;

import java.io.*;
import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static jcuda.driver.JCudaDriver.*;

import jcuda.*;
import jcuda.driver.*;

public class Formula {

    public List<WClause> clauses = new ArrayList<>();
    public boolean isEvidence;
    public LogDouble weight;
    public int formulaId;

    //Fields for GPU operations

    int totalVars;
    long totalGroundings;
    int[] varDomainSizes;

    int maxThreads;
    List<Term> varslist;
    List<Clause> GPUclauses;

    public Formula(List<WClause> clauses_, LogDouble weight_) {
        this(clauses_, weight_, false);
    }

    public Formula(List<WClause> clauses_,
                   LogDouble weight_, boolean isEvidence_) {
        clauses = clauses_;
        weight = (weight_);
        isEvidence = (isEvidence_);
        buildGpuStructures();
    }

    public void buildGpuStructures() {
        maxThreads = 1024;

        Set<Term> vars = new HashSet<Term>();
        for (WClause clause : clauses)
            for (Atom atomi : clause.atoms)
                for (Term clauseatomterm : atomi.terms)
                    if (clauseatomterm.domain.size() > 1)
                        vars.add(clauseatomterm);

        varslist = new ArrayList<Term>(vars);

        varDomainSizes = new int[varslist.size()];
        totalVars = varDomainSizes.length;
        totalGroundings = 1;
        int i = 0;
        for (Term var : varslist) {
            varDomainSizes[i] = var.domain.size();
            totalGroundings *= var.domain.size();
            i = i + 1;
        }

        GPUclauses = new ArrayList<Clause>();
        for (WClause clause : clauses) {
            int[] predicates = new int[clause.atoms.size()];
            int[] predBaseid = new int[clause.atoms.size()];
            int[] vtrue = new int[clause.atoms.size()];

            int[] pvMat = new int[clause.atoms.size() * varslist.size()];
            i = 0;
            for (Atom atom : clause.atoms) {
                predicates[i] = atom.symbol.id + 1;
                vtrue[i] = clause.valTrue.get(i);
                predBaseid[i] = 0;
                for (int j = varslist.size() - 1; j >= 0; j--)
                    pvMat[j * clause.atoms.size() + i] = 0;

                int runningprod = 1;
                for (int j = atom.terms.size() - 1; j >= 0; j--) {
                    Term term = atom.terms.get(j);
                    if (term.domain.size() == 1) {
                        int toAdd = runningprod * term.domain.get(0);
                        predBaseid[i] = predBaseid[i] + toAdd;
                    }
                    if (term.domain.size() > 1) {
                        int k = 0;
                        for (Term termx : varslist) {
                            if (term == termx) {
                                pvMat[k * clause.atoms.size() + i] = runningprod;
                                break;
                            }
                            k = k + 1;
                        }
                    }
                    runningprod = runningprod * term.domain.size();
                }

                i = i + 1;
            }
            Clause newclause = new Clause(predicates, predBaseid, pvMat, vtrue, totalGroundings);
            initDbIndex(newclause);
            GPUclauses.add(newclause);
            System.out.println("Printing");
            newclause.displayPredVarMat();
        }
    }

    public int countTrueGroundings(State db) {
        GPUutil gpuUtil = new GPUutil();

        // Load the ptx file.
        CUmodule module = new CUmodule();
        assert cuModuleLoad(module, "cu_library/mlnCudaKernels.ptx") == CUresult.CUDA_SUCCESS;

        // Obtain a function pointer to the kernel function
        CUfunction function = new CUfunction();
        assert cuModuleGetFunction(function, module, "evalClauseKernel") == CUresult.CUDA_SUCCESS;

        CUdeviceptr d_satArray = new CUdeviceptr();
        CUdeviceptr d_interpretation = new CUdeviceptr();

        assert cuMemAlloc(d_satArray, totalGroundings * Sizeof.INT) == CUresult.CUDA_SUCCESS;
        assert cuMemAlloc(d_interpretation, db.getInterpretation().length * Sizeof.POINTER) == CUresult.CUDA_SUCCESS;

        gpuUtil.parallelInit(d_satArray, totalGroundings, 1, maxThreads);
        assert cuMemcpyHtoD(d_interpretation, Pointer.to(db.getInterpretation()), db.getInterpretation().length * Sizeof.POINTER)
                == CUresult.CUDA_SUCCESS;

        for (Clause clause : GPUclauses) {
            CUdeviceptr d_dbIndex = new CUdeviceptr();
            CUdeviceptr d_predicates = new CUdeviceptr();
            CUdeviceptr d_valTrue = new CUdeviceptr();

            assert cuMemAlloc(d_dbIndex, clause.dbIndex.length * Sizeof.INT) == CUresult.CUDA_SUCCESS;
            assert cuMemAlloc(d_predicates, clause.predicates.length * Sizeof.INT) == CUresult.CUDA_SUCCESS;
            assert cuMemAlloc(d_valTrue, clause.valTrue.length * Sizeof.INT) == CUresult.CUDA_SUCCESS;

            assert cuMemcpyHtoD(d_dbIndex, Pointer.to(clause.dbIndex), clause.dbIndex.length * Sizeof.INT) == CUresult.CUDA_SUCCESS;
            assert cuMemcpyHtoD(d_predicates, Pointer.to(clause.predicates), clause.predicates.length * Sizeof.INT) == CUresult.CUDA_SUCCESS;
            assert cuMemcpyHtoD(d_valTrue, Pointer.to(clause.valTrue), clause.valTrue.length * Sizeof.INT) == CUresult.CUDA_SUCCESS;

            Pointer kernelParameters = Pointer.to(
                    Pointer.to(d_satArray),
                    Pointer.to(d_interpretation),
                    Pointer.to(d_dbIndex),
                    Pointer.to(d_predicates),
                    Pointer.to(d_valTrue),
                    Pointer.to(new int[]{clause.totalPreds}),
                    Pointer.to(new long[]{totalGroundings})
            );

            int blockSizeX = Math.min(maxThreads, (int) totalGroundings);
            int gridSizeX = ((int) totalGroundings + blockSizeX - 1) / blockSizeX;
            System.out.println("Grid size: " + gridSizeX + ", Block size: " + blockSizeX + " :: evalClause");

            cuLaunchKernel(function,
                    gridSizeX, 1, 1,
                    blockSizeX, 1, 1,
                    0, null,
                    kernelParameters, null
            );
            cuCtxSynchronize();

            cuMemFree(d_dbIndex);
            cuMemFree(d_predicates);
            cuMemFree(d_valTrue);
        }

        int[] satArray = new int[(int) totalGroundings];
        assert cuMemcpyDtoH(Pointer.to(satArray), d_satArray, totalGroundings * Sizeof.INT) == CUresult.CUDA_SUCCESS;

        for (int i : satArray)
            System.out.print(i + " ");
        System.out.println();

        int totalSatGroundings = gpuUtil.parallelSum(d_satArray, totalGroundings, maxThreads);

        cuMemFree(d_satArray);
        cuMemFree(d_interpretation);

        return totalSatGroundings;
    }

    private void initDbIndex(Clause clause) {
        // Load the ptx file.
        CUmodule module = new CUmodule();
        assert cuModuleLoad(module, "cu_library/mlnCudaKernels.ptx") == CUresult.CUDA_SUCCESS;

        // Obtain a function pointer to the kernel function
        CUfunction function = new CUfunction();
        assert cuModuleGetFunction(function, module, "initDbIndexKernel") == CUresult.CUDA_SUCCESS;


        CUdeviceptr d_varDomainSizes = new CUdeviceptr();
        CUdeviceptr d_predBaseIdx = new CUdeviceptr();
        CUdeviceptr d_predVarMat = new CUdeviceptr();
        CUdeviceptr d_dbIndex = new CUdeviceptr();

        assert cuMemAlloc(d_varDomainSizes, totalVars * Sizeof.INT) == CUresult.CUDA_SUCCESS;
        assert cuMemAlloc(d_predBaseIdx, clause.predBaseIdx.length * Sizeof.INT) == CUresult.CUDA_SUCCESS;
        assert cuMemAlloc(d_predVarMat, clause.predVarMat.length * Sizeof.INT) == CUresult.CUDA_SUCCESS;
        assert cuMemAlloc(d_dbIndex, clause.dbIndex.length * Sizeof.INT) == CUresult.CUDA_SUCCESS;

        assert cuMemcpyHtoD(d_varDomainSizes, Pointer.to(varDomainSizes), totalVars * Sizeof.INT) == CUresult.CUDA_SUCCESS;
        assert cuMemcpyHtoD(d_predBaseIdx, Pointer.to(clause.predBaseIdx), clause.predBaseIdx.length * Sizeof.INT) == CUresult.CUDA_SUCCESS;
        assert cuMemcpyHtoD(d_predVarMat, Pointer.to(clause.predVarMat), clause.predVarMat.length * Sizeof.INT) == CUresult.CUDA_SUCCESS;
        assert cuMemcpyHtoD(d_dbIndex, Pointer.to(clause.dbIndex), clause.dbIndex.length * Sizeof.INT) == CUresult.CUDA_SUCCESS;

        Pointer kernelParameters = Pointer.to(
                Pointer.to(new int[]{totalVars}),
                Pointer.to(new int[]{clause.totalPreds}),
                Pointer.to(d_varDomainSizes),
                Pointer.to(d_predBaseIdx),
                Pointer.to(d_predVarMat),
                Pointer.to(d_dbIndex),
                Pointer.to(new long[]{totalGroundings})
        );

        int blockSizeX = Math.min(maxThreads, (int) totalGroundings);
        int gridSizeX = ((int) totalGroundings + blockSizeX - 1) / blockSizeX;
        System.out.println("Grid size: " + gridSizeX + ", Block size: " + blockSizeX + " :: initDbIndex");

        cuLaunchKernel(function,
                gridSizeX, 1, 1,
                blockSizeX, 1, 1,
                0, null,
                kernelParameters, null
        );
        cuCtxSynchronize();

        assert cuMemcpyDtoH(Pointer.to(clause.dbIndex), d_dbIndex, clause.dbIndex.length * Sizeof.INT) == CUresult.CUDA_SUCCESS;

        cuMemFree(d_varDomainSizes);
        cuMemFree(d_predBaseIdx);
        cuMemFree(d_predVarMat);
        cuMemFree(d_dbIndex);
    }
}

class Clause {
    int totalPreds;
    public int[] predicates; //Predicates in clause
    public int[] predBaseIdx; //Base value for indexing in db
    public int[] predVarMat;
    public int[] valTrue; //Value of predicate for which it is true.
    public int[] dbIndex; //Index of each grounding of predicate in database

    public Clause(int[] _predicates, int[] _predBaseIdx, int[] _predVarMat, int[] _valTrue, long totalGroundings) {
        totalPreds = _predicates.length;
        predicates = _predicates;
        predBaseIdx = _predBaseIdx;
        predVarMat = _predVarMat;
        valTrue = _valTrue;
        dbIndex = new int[predicates.length * (int) totalGroundings];
    }

    public void displayPredVarMat() {
        int rows = predVarMat.length / predicates.length;
        int cols = predicates.length;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++)
                System.out.print(predVarMat[i * cols + j] + "\t");
            System.out.println("");
        }
    }
}
