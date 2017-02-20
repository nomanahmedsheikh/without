import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class GurobiILPWMSSolver {
	
	public static void main(String[] args) throws FileNotFoundException {
		
		List<String> list = new ArrayList<String>();
		
		List<String> domainList = new ArrayList<String>();
		domainList.add("10");
		domainList.add("20");
		domainList.add("30");
		domainList.add("40");
		domainList.add("50");
//		domainList.add("60");
//		domainList.add("70");
//		domainList.add("80");
//		domainList.add("90");
//		domainList.add("100");
//		domainList.add("500");
		
		for (String domainSize : domainList) {
			
			String fileName = "webkb/webkb_int_" + domainSize +".cnf" ;
			
			int timeLimit = 500;
			String oneRow = "";
			for (int i = 1; i < 2; i++) {
				double cost = run(fileName, timeLimit);
				oneRow += "\t" + cost; 
				timeLimit += 10; 
				if(cost <= 0.0) {
					break;
				}
			}
			list.add(oneRow);
		}
		
		
		System.out.println();
		System.out.println();
		for (String oneRow : list) {
			System.out.println(oneRow);
		}
	}
	
	private static Double run(String filename, int timeLimit) throws FileNotFoundException {
		double cost = Double.NEGATIVE_INFINITY;
		try {
			double totalWeight = 0.0;
			GRBEnv    env   = new GRBEnv("mip1.log");
			GRBModel  model = new GRBModel(env);

			List<GRBVar> predicateVars = new ArrayList<GRBVar>();
			List<GRBVar> clauseVars = new ArrayList<GRBVar>();

			Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(filename))));
			String line = scanner.nextLine();
			String[] tokens = line.split("\\s");
			int noVar = Integer.parseInt(tokens[2]);
			int noSoftClauses = Integer.parseInt(tokens[3]);
			
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
			GRBLinExpr objective = new GRBLinExpr();
			int i=0;
			while(scanner.hasNextLine()) {
				line = scanner.nextLine();
				tokens = line.split("\\s");

				GRBVar clauseVar = clauseVars.get(i);
				Double weight = Double.parseDouble(tokens[0]);
				objective.addTerm(weight, clauseVar); 
				totalWeight += weight;
				
				GRBLinExpr expr = new GRBLinExpr();
				double rhs = 0.0;
				for (int j = 1; j < tokens.length - 1; j++) {
					
					int literal = Integer.parseInt(tokens[j]);
					double sign = 1.0;
					if(literal < 0) {
						sign = -1.0;
						rhs = rhs - 1.0;
						literal = -literal;
					}
					GRBVar v = predicateVars.get(literal-1);
					expr.addTerm(sign, v);
				}
				
				if(weight > 0) {
					expr.addTerm(-1.0, clauseVar);
					model.addConstr(expr, GRB.GREATER_EQUAL, rhs, "c_"+i);
				} else {
					double multiplier = (tokens.length - 2) * (-1.0d);
					expr.addTerm(multiplier, clauseVar);
					model.addConstr(expr, GRB.LESS_EQUAL, rhs, "c_"+i);
				}
				
				i++;
			
			}
			
			System.out.println("Total Weight == " + totalWeight);

			model.setObjective(objective, GRB.MAXIMIZE);
			model.getEnv().set(GRB.DoubleParam.TimeLimit, timeLimit);
//			model.getEnv().set(GRB.IntParam.Presolve, 1);
//			model.getEnv().set(GRB.DoubleParam.Heuristics, 0.15);
			
			// Optimize model

			model.optimize();
			
			cost = totalWeight - model.get(GRB.DoubleAttr.ObjVal);

			System.out.println("Obj: " + cost);

			// Dispose of model and environment

			model.dispose();
			env.dispose();

		} catch (GRBException e) {
			System.out.println("Error code: " + e.getErrorCode() + ". " +
					e.getMessage());
		}
		
		return cost;
	}

}
