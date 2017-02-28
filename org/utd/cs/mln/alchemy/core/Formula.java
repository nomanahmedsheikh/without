package org.utd.cs.mln.alchemy.core;

import org.utd.cs.gm.core.LogDouble;

import java.util.ArrayList;
import java.util.List;

public class Formula {

	public List<WClause> clauses = new ArrayList<>();
	public boolean isEvidence;
	public LogDouble weight;
	public int formulaId;

	public Formula(List<WClause> clauses_, LogDouble weight_) {
		this(clauses_, weight_, false);
	}

	public Formula(List<WClause> clauses_,
			LogDouble weight_, boolean isEvidence_) {
		clauses = clauses_;
		weight = (weight_);
		isEvidence = (isEvidence_);
	}

}
