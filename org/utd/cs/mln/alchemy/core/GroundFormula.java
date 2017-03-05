package org.utd.cs.mln.alchemy.core;

import org.utd.cs.gm.core.LogDouble;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Happy on 2/21/17.
 */
public class GroundFormula {
    public List<GroundClause> groundClauses = new ArrayList<>();
    public int formulaId; // index of this formula in the MLN's list of ground formulas
    public int parentFormulaId; // id of first order formula from which this came
    public LogDouble weight;
    public boolean isSatisfied;
}
