package org.utd.cs.mln.alchemy.wms;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

import java.util.ArrayList;
import java.util.List;

public class GurobiWmsSolver implements WeightedMaxSatSolver {
	
	private int noVar;
	
	private int noSoftClauses = 0;

	@SuppressWarnings("unused")
	private int noHardClauses = 0;
	
	private List<int[]> hard_clauses = new ArrayList<int[]>();
	private List<int[]> soft_clauses = new ArrayList<int[]>();
	private List<Double> clause_weights = new ArrayList<Double>();
	private double timeLimit = -1;
	
	private Double totalWeight = 0.0;
	
	private Double bestValue;

	@Override
	public void setNoVar(int noVar) {
		this.noVar = noVar;
	}

	@Override
	public void setNoClauses(int nclauses) {
		// Do nothing

	}

	@Override
	public void addHardClause(int[] clause) {
		hard_clauses.add(clause);
		noHardClauses++;
	}

	@Override
	public void addSoftClause(double weight, int[] clause) {
		soft_clauses.add(clause);
		clause_weights.add(weight);
		if(weight > 0)
			totalWeight += weight;
		noSoftClauses++;

	}

	@Override
	public void solve() {

		try {
			GRBEnv    env   = new GRBEnv("mip1.log");
			GRBModel  model = new GRBModel(env);

			List<GRBVar> predicateVars = new ArrayList<GRBVar>();
			List<GRBVar> clauseVars = new ArrayList<GRBVar>();

			// Create variables
			for (int i = 0; i < noVar; i++) {
				GRBVar x = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "P_"+i);
				predicateVars.add(x);
			}

			for (int i = 0; i < noSoftClauses; i++) {
				GRBVar x = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "C_"+i);
				clauseVars.add(x);
			}

			// Integrate new variables
			model.update();

			// Set objective: maximize x + y + 2 z

			GRBLinExpr expr = new GRBLinExpr();
			for (int i = 0; i < noSoftClauses; i++) {
				GRBVar x = clauseVars.get(i);
				Double weight = clause_weights.get(i);
				
				expr.addTerm(weight, x); 
			}
			model.setObjective(expr, GRB.MAXIMIZE);

			// Add constraint: x + 2 y + 3 z <= 4
			for (int i = 0; i < noSoftClauses; i++) {
				expr = new GRBLinExpr();
				double rhs = 0.0;
				
				int[] softClause = soft_clauses.get(i);
				for (int j = 0; j < softClause.length; j++) {
					
					int literal = softClause[j];
					double sign = 1.0;
					if(literal < 0) {
						sign = -1.0;
						rhs = rhs - 1.0;
						literal = -literal;
					}
					GRBVar v = predicateVars.get(literal-1);
					expr.addTerm(sign, v);
				}
				GRBVar z = clauseVars.get(i);
				double weight = clause_weights.get(i);
				if(weight > 0) {
					expr.addTerm(-1.0, z);
					model.addConstr(expr, GRB.GREATER_EQUAL, rhs, "c_"+i);
				} else {
					double multiplier = softClause.length * (-1.0d);
					expr.addTerm(multiplier, z);
					model.addConstr(expr, GRB.LESS_EQUAL, rhs, "c_"+i);
				}
			}
			
			if(timeLimit > 0){
				model.getEnv().set(GRB.DoubleParam.TimeLimit, timeLimit);
			}

			// Optimize model

			model.optimize();
			
			double cost = totalWeight - model.get(GRB.DoubleAttr.ObjVal);
			bestValue = cost;

			System.out.println("Obj: " + cost);

			// Dispose of model and environment

			model.dispose();
			env.dispose();

		} catch (GRBException e) {
			System.out.println("Error code: " + e.getErrorCode() + ". " +
					e.getMessage());
		} 

	}

	@Override
	public int[] model() {
		return null;
	}

	@Override
	public double bestValue() {
		return bestValue;
	}
	
	public void reset() {
		noVar = 0;
		noSoftClauses = 0;
		noHardClauses = 0;
		hard_clauses.clear();
		soft_clauses.clear(); 
		clause_weights.clear();
		totalWeight = 0.0;
		bestValue = null;
		timeLimit = -1;
	}

	@Override
	public void setTimeLimit(double timeLimitSec) {
		this.timeLimit = timeLimitSec;
	}

}
