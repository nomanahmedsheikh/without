package org.utd.cs.mln.alchemy.core;

import org.utd.cs.gm.core.LogDouble;

public class Formula {

	public int MLNClauseStartIndex;
	public int MLNClauseEndIndex;
	public boolean isEvidence;
	public LogDouble weight;

	public Formula(int MLNClauseStartIndex_, int MLNClauseEndIndex_, LogDouble weight_) {
		this(MLNClauseStartIndex_, MLNClauseEndIndex_, weight_, false);
	}

	public Formula(int MLNClauseStartIndex_, int MLNClauseEndIndex_,
			LogDouble weight_, boolean isEvidence_) {
		MLNClauseStartIndex = (MLNClauseStartIndex_);
		MLNClauseEndIndex = (MLNClauseEndIndex_);
		weight = (weight_);
		isEvidence = (isEvidence_);
	}

}
