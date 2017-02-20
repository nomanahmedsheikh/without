package org.utd.cs.mln.marginalmap;

import org.utd.cs.mln.alchemy.core.MLN;

/**
 * Created by vishal on 20/1/17.
 */
/*
This is exact copy of the MLN class MINUS whatever is not required.

Counting formula is nothing but an MLN

 */
public class CountingFormula extends MLN{

    public CountingFormula marginalizeOverPredicate(){
        CountingFormula finalFormula=null;


        return finalFormula;
    }

    // Adds two counting formulas.
    // Which is row wise addition of the Factors.
    public CountingFormula addCF(){
        CountingFormula finalFormula=null;


        return finalFormula;
    }

    // Multiplies two counting formulas.
    // Which is just concating the two MLN assuming it is used only in decomposer.
    // On initial thought it might even work
    public CountingFormula multiplyCF(){
        CountingFormula finalFormula=null;


        return finalFormula;
    }

    // Multiply CF by a scalar
    public CountingFormula scaleCF(){
        CountingFormula finalFormula=null;


        return finalFormula;
    }

    // get MAP assignment
    // TODO : Replace void with an assignment
    public void getMAP() {

    }
}
