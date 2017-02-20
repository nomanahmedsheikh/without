package org.utd.cs.mln.marginalmap;

import org.utd.cs.mln.alchemy.core.MLN;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by vishal on 15/1/17.
 */

/*
Controller to compute ground marginal MAP for base lines on datasets
 */

public class GroundMarginalMap {

    // Computes the MLN and returns a factor over the MAP variables.
    // MAP assignment will be found by the caller function.
    // This is done in inhabit debugging and comparision purposes.
    // Will return a single row in case we are computing Z
    public Factor computeMLN(MLN mln){
    /*
        Algo
            Create a factor over all the MAP variables
            For each assignment in the final factor
                Calculate the weight for the assignment
        */
        Factor result=null;

        return result;

    }

    // Returns a MAP Assignment from a Factor
    // Should ideally be in Factor class
    // An assignment is:
    //  HashMap<HashSet<String>,Double>
    public HashSet<String> getMAPAssignment(){
        return null;
    }
}