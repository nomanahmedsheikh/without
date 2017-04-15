package org.utd.cs.mln.alchemy.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Happy on 2/28/17.
 */
public class GroundMLN {
    public List<GroundPredicateSymbol> symbols = new ArrayList<>();
    public List<GroundPredicate> groundPredicates = new ArrayList();
    public List<GroundFormula> groundFormulas = new ArrayList<>();

    public List<GroundPredicateSymbol> allSymbols = new ArrayList<>(); // Stores all symbols (query + evidence)
    public List<GroundPredicate> allGroundPredicates = new ArrayList(); // Stores all ground predicates (query + evidence)
    public Evidence evidence;
}
