package org.utd.cs.mln.alchemy.core;

import java.util.HashSet;
import java.util.Set;

public class EquivalenceClass {
	public Set<Integer> varIndices = new HashSet<Integer>();
	public boolean isSingle;
	public Set<Integer> clauseIndices = new HashSet<Integer>();
	public int domainSize;
	@Override
	public String toString() {
		return "EquivalenceClass [varIndices=" + varIndices + ", isSingle="
				+ isSingle + ", clauseIndices=" + clauseIndices
				+ ", domainSize=" + domainSize + "]";
	}
	public EquivalenceClass() {
		super();
		isSingle = true;
		domainSize = 0;
	}
}
