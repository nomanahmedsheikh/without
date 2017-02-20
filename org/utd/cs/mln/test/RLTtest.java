package org.utd.cs.mln.test;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

public class RLTtest {

	public static void main(String[] args) {
		try {
			GRBEnv env = new GRBEnv("mip1.log");
			GRBModel model = new GRBModel(env);

			// Create variables

			GRBVar r = model.addVar(0.0, 2.0, 0.0, GRB.INTEGER, "r");
			GRBVar s1 = model.addVar(0.0, 2.0, 0.0, GRB.INTEGER, "s1");
			GRBVar s2 = model.addVar(0.0, 2.0, 0.0, GRB.INTEGER, "s2");
			GRBVar t = model.addVar(0.0, 2.0, 0.0, GRB.INTEGER, "t");
			GRBVar x__s1_t = model.addVar(0.0, 4.0, 0.0, GRB.INTEGER, "x__s1_t");
			GRBVar x__s2_t = model.addVar(0.0, 4.0, 0.0, GRB.INTEGER, "x__s2_t");

			// Integrate new variables

			model.update();

			// Set objective: maximize x + y + 2 z

			GRBLinExpr expr = new GRBLinExpr();
			expr.addConstant(20.0);
			expr.addTerm(-6.0, r);
			expr.addTerm(-4.0, s1);
			expr.addTerm(-7.0, s2);
			expr.addTerm(-4.0, t);
			expr.addTerm(2.0, x__s1_t);
			expr.addTerm(2.0, x__s2_t);
			model.setObjective(expr, GRB.MINIMIZE);

			// Add constraint: x + 2 y + 3 z <= 4

			expr = new GRBLinExpr();
			expr.addTerm(1.0, s1);
			expr.addTerm(-1.0, r);
			model.addConstr(expr, GRB.LESS_EQUAL, 0.0, "c0");

			// Add constraint: x + y >= 1

			expr = new GRBLinExpr();
			expr.addTerm(1.0, s2);
			expr.addTerm(1.0, r);
			model.addConstr(expr, GRB.LESS_EQUAL, 2.0, "c1");

			expr = new GRBLinExpr();
			expr.addTerm(2.0, s1);
			expr.addTerm(-1.0, x__s1_t);
			model.addConstr(expr, GRB.GREATER_EQUAL, 0.0, "c2");

			expr = new GRBLinExpr();
			expr.addTerm(2.0, t);
			expr.addTerm(-1.0, x__s1_t);
			model.addConstr(expr, GRB.GREATER_EQUAL, 0.0, "c3");

			expr = new GRBLinExpr();
			expr.addTerm(2.0, s1);
			expr.addTerm(2.0, t);
			expr.addTerm(-1.0, x__s1_t);
			model.addConstr(expr, GRB.LESS_EQUAL, 4.0, "c4");
			
			expr = new GRBLinExpr();
			expr.addTerm(2.0, s2);
			expr.addTerm(-1.0, x__s2_t);
			model.addConstr(expr, GRB.GREATER_EQUAL, 0.0, "c5");

			expr = new GRBLinExpr();
			expr.addTerm(2.0, t);
			expr.addTerm(-1.0, x__s2_t);
			model.addConstr(expr, GRB.GREATER_EQUAL, 0.0, "c6");

			expr = new GRBLinExpr();
			expr.addTerm(2.0, s2);
			expr.addTerm(2.0, t);
			expr.addTerm(-1.0, x__s2_t);
			model.addConstr(expr, GRB.LESS_EQUAL, 4.0, "c7");
			
			// Optimize model

			model.optimize();

			System.out.println(r.get(GRB.StringAttr.VarName) + " " + r.get(GRB.DoubleAttr.X));
			System.out.println(s1.get(GRB.StringAttr.VarName) + " " + s1.get(GRB.DoubleAttr.X));
			System.out.println(s2.get(GRB.StringAttr.VarName) + " " + s2.get(GRB.DoubleAttr.X));
			System.out.println(t.get(GRB.StringAttr.VarName) + " " + t.get(GRB.DoubleAttr.X));
			System.out.println(x__s1_t.get(GRB.StringAttr.VarName) + " " + x__s1_t.get(GRB.DoubleAttr.X));
			System.out.println(x__s2_t.get(GRB.StringAttr.VarName) + " " + x__s2_t.get(GRB.DoubleAttr.X));

			System.out.println("Obj: " + model.get(GRB.DoubleAttr.ObjVal));

			// Dispose of model and environment

			model.dispose();
			env.dispose();

		} catch (GRBException e) {
			System.out.println("Error code: " + e.getErrorCode() + ". "
					+ e.getMessage());
		}
	}
}
