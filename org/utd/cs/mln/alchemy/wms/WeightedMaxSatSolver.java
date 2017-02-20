package org.utd.cs.mln.alchemy.wms;

public interface WeightedMaxSatSolver {
	
	public void setNoVar(int noVar);
	
	public void setNoClauses(int nclauses);
	
	public void addHardClause(int[] clause);
	
	public void addSoftClause(double weight, int[] clause);
	
	public void solve();
	
	public int[] model();
	
	public double bestValue();

	public void setTimeLimit(double timeLimitSec);

}
