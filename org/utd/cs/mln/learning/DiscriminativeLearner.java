package org.utd.cs.mln.learning;

import org.utd.cs.mln.alchemy.core.*;
import org.utd.cs.mln.inference.GibbsSampler;
import org.utd.cs.mln.inference.GibbsSampler_v2;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Random;

/**
 * Created by Happy on 5/13/17.
 */
public abstract class DiscriminativeLearner {
    public List<GibbsSampler> inferences; // List of Inferences objects, each object refers to different domain
    public List<GibbsSampler> inferencesEM; // List of Inferences objects for EM, each object refers to different domain. If withEM = false, then it is null.
    public int numIter, domain_cnt, numFormulas; // numIter -> Number of learning iterations, domain_cnt -> Number of different domains, numFormulas -> Number of first order formulas
    public double[] weights; // weights : list of weights learned for first order formulas
    public double[] priorMeans, priorStdDevs; // list of priorMean and stdDevs for first order formulas when randomly initialzed from Gaussian Distribution. If usePrior is false, then they are null
    public double[][] formulaTrainCnts; // formulaTrainCnts[i][j] is the number of satisfied groundings of jth first order formula through ith domain
    public boolean withEM, usePrior, dldebug = true;

    public DiscriminativeLearner(List<GibbsSampler> inferences, List<GibbsSampler> inferencesEM, int numIter, boolean withEM, double []priorMeans, double []priorStdDevs, boolean usePrior) {
        this.inferences = inferences;
        this.inferencesEM = inferencesEM;
        this.numIter = numIter;
        this.withEM = withEM;
        this.usePrior = usePrior;
        this.numFormulas = inferences.get(0).mln.formulas.size();
        this.domain_cnt = inferences.size();
        this.weights = new double[numFormulas];
        this.formulaTrainCnts = new double[domain_cnt][numFormulas];
        this.priorMeans = priorMeans;
        this.priorStdDevs = priorStdDevs;
        if(usePrior == false || priorStdDevs == null)
        {
            this.priorMeans = new double[numFormulas];
            this.priorStdDevs = new double[numFormulas];
            Arrays.fill(this.priorStdDevs, 2);
        }
    }


    protected void findFormulaTrainCnts() {
        for (int i = 0; i < domain_cnt; i++) {
            GroundMLN gm = inferences.get(i).state.groundMLN;
            Evidence truth = inferences.get(i).truth;
            for(int gfId = 0 ; gfId < gm.groundFormulas.size() ; gfId++)
            {
                GroundFormula gf = gm.groundFormulas.get(gfId);
                int parentFormulaId = gf.parentFormulaId;
                // If parent formula doesn't exist for this ground formula, then don't do anything.
                if(parentFormulaId == -1)
                    continue;
                boolean isFormulaSatisfied = true;
                for(GroundClause gc : gf.groundClauses)
                {
                    boolean isClauseSatisfied = false;
                    for(int gpId : gc.groundPredIndices)
                    {
                        int trueVal = 0;
                        if(truth.predIdVal.containsKey(gpId))
                            trueVal = truth.predIdVal.get(gpId);
                        BitSet b = gc.grounPredBitSet.get(gc.globalToLocalPredIndex.get(gpId));
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

                    formulaTrainCnts[i][parentFormulaId]++;
                }
            }
        }
    }

    protected void initWeights() {
        MLN mln = inferences.get(0).mln;
        if(usePrior)
        {
            Random rand = new Random();
            for (int i = 0; i < mln.formulas.size(); i++) {
                double p = rand.nextGaussian()*priorStdDevs[i]+priorMeans[i];
                //weights[i] = p; // TODO : later uncomment this line
                weights[i]=0; // TODO : later comment this line
            }
            if(dldebug)
            {
                System.out.print("Initial weights : ");
                for (int i = 0; i < weights.length; i++) {
                    System.out.print(weights[i]+",");
                }
                System.out.println();
            }
        }
        else
        {
            Random rand = new Random();
            for (int i = 0; i < mln.formulas.size(); i++) {
                double p = rand.nextDouble()*2-1; // generate random number b/w -1 and 1
                weights[i] = p;
            }
        }
        if(dldebug)
        {
            System.out.println("Initial weights : " + Arrays.toString(weights));
        }
    }

    abstract void learnWeights();
}
