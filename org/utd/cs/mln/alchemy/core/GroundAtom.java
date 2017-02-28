package org.utd.cs.mln.alchemy.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Happy on 2/21/17.
 */
public class GroundAtom {
    public GroundPredicate groundPredicate;
    public boolean sign ; // false: positive, true : negative
    public int valTrue; // At what value does this atom becomes true

    public GroundAtom(GroundPredicate groundPredicate, boolean sign, int valTrue) {
        this.groundPredicate = groundPredicate;
        this.sign = sign;
        this.valTrue = valTrue;
    }
}
