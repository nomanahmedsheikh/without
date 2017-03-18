package org.utd.cs.mln.learning;

import org.utd.cs.gm.core.LogDouble;
import org.utd.cs.gm.utility.Timer;
import org.utd.cs.mln.alchemy.core.*;
import org.utd.cs.mln.inference.GibbsSampler_v2;

import java.io.*;
import java.util.*;

/**
 * Created by Happy on 3/11/17.
 */
public class DiscLearner {

    public enum Method {
        CG;
    };

    public List<GibbsSampler_v2> inferences = new ArrayList<>();
    public double[] weights, oldWeights, averageWeights, gradient, old_gradient, d, oldd,
            delta_pred, priorMeans, priorStdDevs;
    public double[][] formulaTrainCnts;
    public int num_iter, domain_cnt, backtrackCount, maxBacktracks;
    public double cg_lambda, cg_max_lambda, alpha, min_ll_change;
    public boolean withEM = false, backtracked = false, preConditionCG, dldebug=false;
    public Method method = Method.CG;

    public DiscLearner(List<GibbsSampler_v2> inferences, int num_iter, double lambda, double min_ll_change, double max_lambda,
                       boolean withEM, boolean predConditionCG)
    {
        this.inferences = inferences;
        this.num_iter = num_iter;
        this.backtrackCount = 0;
        this.maxBacktracks = 1000;
        this.cg_lambda = lambda;
        this.min_ll_change = min_ll_change;
        this.cg_max_lambda = max_lambda;
        this.alpha = 1;
        this.withEM = withEM;
        this.preConditionCG = predConditionCG;
        this.domain_cnt = inferences.size();
        int numFormulas = inferences.get(0).mln.formulas.size();
        weights = new double[numFormulas];
        oldWeights = new double[numFormulas];
        averageWeights = new double[numFormulas];
        gradient = new double[numFormulas];
        old_gradient = new double[numFormulas];
        d = new double[numFormulas];
        oldd = new double[numFormulas];
        delta_pred = null;
        formulaTrainCnts = new double[domain_cnt][numFormulas];
        priorMeans = new double[numFormulas];
        priorStdDevs = new double[numFormulas];
        Arrays.fill(priorStdDevs, 2);
    }

    public double[] learnWeights()
    {
        findFormulaTrainCnts();
        if(withEM)
        {
            // TODO : implement for EM
        }

        for (int i = 0; i < domain_cnt; i++) {
            inferences.get(i).saveAllCounts(true);
        }
        long time = System.currentTimeMillis();
        boolean burningIn = true, isInit = true;
        for(int iter = 1 ; iter <= num_iter ; iter++)
        {
            if(iter%1 == 0) {
                System.out.println("iter : " + iter + ", Elapsed Time : " + Timer.time((System.currentTimeMillis() - time) / 1000.0));
            }
            setMLNWeights();
            if(withEM)
            {
                //TODO: fillinmissingvalues
            }
            System.out.println("Running inference...");
            if(backtracked) {
                burningIn = true;
                isInit = true;
            }
            infer(burningIn, isInit);
            burningIn = false;
            isInit = false;
            System.out.println("Done Inference");
            System.out.println("Getting gradient...");
            findGradient();
            if(dldebug)
                System.out.println("gradient = " + Arrays.toString(gradient));
            if(method == Method.CG) {
                int status = updateWtsByCG(iter);
                if(status == -1)
                    break;
                if (backtracked)
                    iter--;
            }
        }
        System.out.println("Learning done...");
        System.out.println("Final weights : " + Arrays.toString(weights));
        return weights;
    }

    private int updateWtsByCG(int iter)
    {
        double realdist = 1.0;
        double preddist = 1.0;
        int numWts = weights.length;
        if (iter > 1 && delta_pred != null && !backtracked)
        {
            double []dist = new double[numWts];
            for (int i = 0; i < dist.length; i++) {
                dist[i] = weights[i] - oldWeights[i];
            }
            double []avgPred = new double[numWts];
            for (int i = 0; i < numWts; i++)
                avgPred[i] = old_gradient[i] + delta_pred[i]/2.0;

            preddist = dotprod(avgPred, dist, numWts);

            // Real change is lower bound on actual change
            realdist = dotprod(gradient, dist, numWts);
            System.out.println("pred*dist = " + preddist);
            System.out.println("real*dist = " + realdist);

        }
        if(iter > 1) {
            double delta = realdist / preddist;

            if (!backtracked && preddist == 0)
                cg_lambda /= 4;

            if (!backtracked && preddist != 0 && delta > 0.75)
                cg_lambda /= 2;
            // if (delta < 0.25)   // Update schedule from (Fletcher, 1987)
            if (delta < 0.0)       // Gentler update schedule, to handle noise
            {
                if (cg_lambda * 4 > cg_max_lambda)
                    cg_lambda = cg_max_lambda;
                else
                    cg_lambda *= 4;
            }
            if (delta < 0.0 && backtrackCount < maxBacktracks) {
                System.out.println("Backtracking...");
                for (int i = 0; i < numWts; i++)
                    weights[i] = oldWeights[i];

                for (int i = 0; i < domain_cnt; i++)
                    inferences.get(i).restoreCnts();

                backtracked = true;
                backtrackCount++;
            } else {
                backtracked = false;
                backtrackCount = 0;
            }
        }

        if (!backtracked)
        {
            for (int i = 0; i < domain_cnt; i++)
                inferences.get(i).saveCnts();
            double preCond[] = new double[numWts];
            Arrays.fill(preCond, 1.0);
            if(preConditionCG)
            {
                double variance[] = getVariance(numWts);
                if(dldebug)
                    System.out.println("variance : " + Arrays.toString(variance));
                for (int formulaNum = 0; formulaNum < numWts; formulaNum++)
                    preCond[formulaNum] = 1.0/variance[formulaNum];
            }
            double beta = 0.0;

            // Compute beta using Polak-Ribiere form:
            //   beta = g_j+1 (g_j+1 - g_j) / (g_j g_j)
            // Preconditioned:
            //   beta = g_j+1 M-1 (g_j+1 - g_j) / (g_j M-1 g_j)
            double beta_num = 0.0;
            double beta_denom = 0.0;
            if (iter > 1)
            {
                for (int i = 0; i < numWts; i++)
                {
                    beta_num   += gradient[i] * preCond[i] * (gradient[i] - old_gradient[i]);
                    beta_denom += old_gradient[i] * preCond[i] * old_gradient[i];
                }
                beta = beta_num/beta_denom;
            }
            else
                beta = 0.0;
            
            if(dldebug)
                System.out.println("beta = " + beta);

            // Compute new direction
            for (int w = 0; w < numWts; w++)
                d[w] = -preCond[w]*gradient[w] + beta*oldd[w];

            double Hd[] = getHessianVectorProduct(d);
            alpha = computeQuadraticStepLength(Hd);
            if (alpha < 0.0)
            {
                for (int w = 0; w < numWts; w++)
                    d[w] = -preCond[w]*gradient[w];
                Hd = getHessianVectorProduct(d);
                alpha = computeQuadraticStepLength(Hd);
            }
        }

        if (!backtracked && alpha <= 0.0)
        {
            // If alpha is negative, then either the direction or the
            // Hessian is in error.  We call this a backtrack so that
            // we can gather more samples while keeping the old samples.
            backtracked = true;
        }
        if (!backtracked) {
            // Compute total weight change
            double wchange[] = new double[numWts];
            for (int w = 0; w < numWts; w++) {
                //wchange[w] = d[w] * alpha + (weights[w] - oldWeights[w]) * momentum;
                // above line was present in alchemy, but momentum is 0 always for disc learning
                wchange[w] = d[w] * alpha;
            }

            // Convergence criteria for 2nd order methods:
            // Stop when the maximum predicted improvement in log likelihood
            // is very small.
            double maxchange = -dotprod(gradient, wchange, numWts);
            System.out.println("Maximum Estimated Improvement = " + maxchange);
            if ((method == Method.CG) && maxchange < min_ll_change) {
                System.out.println("Upper bound is less than " + min_ll_change + ", halting learning.");
                return -1;
            }

            // Save weights, gradient, and direction and adjust the weights
            for (int w = 0; w < numWts; w++) {
                oldWeights[w] = weights[w];
                oldd[w] = d[w];
                old_gradient[w] = gradient[w];

                weights[w] += wchange[w];
                averageWeights[w] = ((iter - 1) * averageWeights[w] + weights[w]) / iter;
            }
            delta_pred = getHessianVectorProduct(wchange);
            for (int i = 0; i < domain_cnt; i++)
                inferences.get(i).resetCnts();
            System.out.println("weights = " + Arrays.toString(weights));
            System.out.println("Avg weights = " + Arrays.toString(averageWeights));
        }
        return 0;
    }

    private double computeQuadraticStepLength(double[] hd) {
        int numWeights = d.length;
        // Compute step length using trust region approach
        double dHd = dotprod(d, hd, numWeights);
        double dd = dotprod(d, d, numWeights);
        double dg = dotprod(gradient, d, numWeights);
        double alpha = -dg/(dHd + cg_lambda * dd);

        if(dldebug)
        {
            System.out.println("dHd = " + dHd);
            System.out.println("dd = " + dd);
            System.out.println("dg = " + dg);
            System.out.println("alpha = " + alpha);
        }

        // Because the problem is convex, the Hessian should always
        // be positive definite, and so alpha should always be non-negative.
        // Since our Hessian is approximate, we may end up with a negative
        // alpha.  In these cases, we used to make alpha zero (i.e., take a
        // step of size zero), but now we return the negative alpha and
        // let the caller deal with it.
        if (alpha < 0.0)
        {
            System.out.println("Alpha < 0!  Bad direction or Hessian.");
        }

        return alpha;
    }

    private double[] getHessianVectorProduct(double[] d) {
        double Hd[] = inferences.get(0).getHessianVectorProduct(d);
        for (int i = 1; i < domain_cnt; i++) {
            double Hd_i[] = inferences.get(i).getHessianVectorProduct(d);
            for (int j = 0; j < Hd.length; j++) {
                Hd[j] += Hd_i[j];
            }
        }
        return Hd;
    }

    // get variance of inferred counts for each formula
    private double[] getVariance(int numWts) {
        double []variance = new double[numWts];
        for (int formulaNum = 0; formulaNum < numWts; formulaNum++) {
            double sd = priorStdDevs[formulaNum];
            variance[formulaNum] = 1.0/(sd * sd);
            for (int i = 0; i < domain_cnt; i++) {
                double trueCnts[] = inferences.get(i).numFormulaTrueCnts;
                double trueSqCnts[] = inferences.get(i).numFormulaTrueSqCnts;
                int numSamples = inferences.get(i).numIter;
                double x   = trueCnts[formulaNum];
                double xsq = trueSqCnts[formulaNum];

                // Add variance for this domain
                variance[formulaNum] += xsq/numSamples - (x/numSamples)*(x/numSamples);
            }
        }
        return variance;
    }

    private double dotprod(double[] v1, double[] v2, int numWts) {
        double total = 0.0;
        for (int i = 0; i < numWts ; i++) {
            total += v1[i]*v2[i];
        }
        return total;
    }

    private void findFormulaTrainCnts() {
        for (int i = 0; i < domain_cnt; i++) {
            GroundMLN gm = inferences.get(i).state.groundMLN;
            Evidence truth = inferences.get(i).truth;
            for(GroundFormula gf : gm.groundFormulas)
            {
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
                    int parentFormulaId = gf.parentFormulaId;
                    formulaTrainCnts[i][parentFormulaId]++;
                }
            }
        }
    }

    private void findGradient()
    {
        int numWeights = weights.length;
        Arrays.fill(gradient,0.0);
        for (int i = 0; i < domain_cnt ; i++) {
            if(dldebug)
                System.out.println("Finding gradient for domain " + i);
            getGradientForDomain(gradient, i);
        }
        for (int i = 0; i < numWeights; i++) {
            double sd = priorStdDevs[i];
            double priorDerivative = (weights[i]-priorMeans[i])/(sd*sd);
            gradient[i] += priorDerivative;
        }
    }

    private void getGradientForDomain(double []gradient, int domainIndex)
    {
        double []formulaInferredCnts = inferences.get(domainIndex).numFormulaTrueCnts;
        if(dldebug)
            System.out.println("FormulaNum\tactual Count\tInferred Count");
        for (int j = 0; j < formulaInferredCnts.length; j++) {
            double inferredCount = formulaInferredCnts[j]/inferences.get(domainIndex).numIter;
            if(dldebug)
                System.out.println(j + '\t' + formulaTrainCnts[domainIndex][j] + '\t' + inferredCount);
            gradient[j] -= (formulaTrainCnts[domainIndex][j] - inferredCount);
        }
    }

    private void infer(boolean burningIn, boolean isInit)
    {
        MLN mln = inferences.get(0).mln;
        for (int i = 0; i < domain_cnt; i++) {
            State state = inferences.get(i).state;
            state.setGroundFormulaWtsToParentWts(mln);
            inferences.get(i).updateWtsForNextGndPred(0);
            System.out.println("Doing inference for domain " + i);
            inferences.get(i).infer(burningIn, isInit);
        }
    }

    private void setMLNWeights()
    {
        MLN mln = inferences.get(0).mln;
        int numFormulas = mln.formulas.size();
        for (int i = 0; i < numFormulas ; i++)
        {
            mln.formulas.get(i).weight = new LogDouble(weights[i], true);
        }
    }

    public void writeWeights(String mln_file, String out_file, double []weights) throws FileNotFoundException {

        Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(mln_file))));
        PrintWriter writer = new PrintWriter(out_file);
        boolean formulaLine = false;
        int formulaNum = 0;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().replaceAll("\\s", "");
            if (line.isEmpty()) {
                writer.println(line);
                continue;
            }
            if (line.contains("#formulas")) {
                formulaLine = true;
                writer.println(line);
                continue;
            }
            if(formulaLine == false)
            {
                writer.println(line);
            }
            else
            {
                String[] formulaArr = line.split("::");
                writer.printf(formulaArr[0]+"::%.3f\n",weights[formulaNum]);
                formulaNum++;
            }
        }
        writer.close();
        scanner.close();
    }
}
