package org.utd.cs.mln.alchemy.wms;


public class NullWmsSolver implements WeightedMaxSatSolver {

	@Override
	public void setNoVar(int noVar) {
	}

	@Override
	public void setNoClauses(int nclauses) {
	}

	@Override
	public void addHardClause(int[] clause) {
	}

	@Override
	public void addSoftClause(double weight, int[] clause) {
	}

	@Override
	public void solve() {
	}

	@Override
	public int[] model() {
		return null;
	}

	@Override
	public double bestValue() {
		return 0;
	}

	@Override
	public void setTimeLimit(double timeLimitSec) {
	}

}
