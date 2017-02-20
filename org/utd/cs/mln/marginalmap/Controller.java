package org.utd.cs.mln.marginalmap;

import org.utd.cs.mln.alchemy.core.MLN;

import java.util.List;

/**
 * Created by vishal on 14/1/17.
 */

/*
Main file that runs the code.
 */
public class Controller {

    // Takes in root MLN to be computed.
    public Factor computeMLN(MLN rootMLN){

        // Holds the result on all MAP variables for initial rootMLN
        Factor finalComputedResult=null;

        Rule nextRule=ApplyRule.getNextRuleToApply(rootMLN);

        // No rule can be applied anymore so we need to compute on the ground MLN
        if(nextRule.ruleType==0){
            // Ground the MLN and compute the Factor on MAP



            // Store result in finalComputedResult
        }
        else {
            // Apply the next rule that is selected
            List<MLN> subMLN =ApplyRule.applyRule(rootMLN,nextRule);

            for (int i = 0; i < subMLN.size(); i++) {
                // Result will be a CF
                computeMLN(subMLN.get(i));
            }

            finalComputedResult=combineResult(nextRule);
        }

        return finalComputedResult;
    }

    public Factor combineResult(Rule ruleApplied){
        Factor combinedFactor=null;


        return combinedFactor;
    }
}
