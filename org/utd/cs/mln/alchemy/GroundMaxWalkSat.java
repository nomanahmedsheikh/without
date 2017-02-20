package org.utd.cs.mln.alchemy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.sat4j.specs.ContradictionException;
import org.utd.cs.gm.core.LogDouble;
import org.utd.cs.mln.alchemy.core.Atom;
import org.utd.cs.mln.alchemy.core.Evidence;
import org.utd.cs.mln.alchemy.core.MLN;
import org.utd.cs.mln.alchemy.core.PredicateNotFound;
import org.utd.cs.mln.alchemy.core.WClause;
import org.utd.cs.mln.alchemy.util.GrindingMill;
import org.utd.cs.mln.alchemy.util.Parser;
import org.utd.cs.mln.alchemy.wms.GurobiWmsSolver;
import org.utd.cs.mln.alchemy.wms.WeightedMaxSatSolver;

public class GroundMaxWalkSat {
	
	private WeightedMaxSatSolver solver;
	
	private static boolean print = true;
	
	public long networkConstructionTime = 0 ;
	public long solverTime = 0;
	
	public GroundMaxWalkSat(WeightedMaxSatSolver _solver) {
		solver = _solver;
	}
	
	private void convertToWeightedMaxSat(MLN mln) {
		
		solver.setNoVar(mln.symbols.size());
		solver.setNoClauses(mln.clauses.size());
		
		for (WClause clause : mln.clauses) {
			int power = 1;
			int[] c = new int[clause.atoms.size()];
			
			for (int i = 0; i < clause.atoms.size(); i++) {
				Atom atom = clause.atoms.get(i);
				power *= atom.getNumberOfGroundings();
				
				if(clause.sign.get(i)) {
					c[i] = -atom.symbol.id-1;
				} else {
					c[i] = atom.symbol.id+1;
				}
			}
			
			LogDouble weight = clause.weight.power(power);
			solver.addSoftClause(weight.getValue(), c);
			
		}

	}
	
	public  void run(String filename) throws FileNotFoundException, PredicateNotFound {
		long time = System.currentTimeMillis();
		long startTime = time;
		
		networkConstructionTime = time;
		solverTime = 0;
		
		MLN mln = new MLN();
		Parser parser = new Parser(mln);
		parser.parseInputMLNFile(filename);
		//System.out.println("mln is : ");
		//mln.print(mln.clauses, "MLN...");
		//System.out.println("After normalizing...");
		//ArrayList<Integer>values1 = new ArrayList<Integer>();
		//values1.add(0);
		ArrayList<Evidence> evid_list = parser.parseInputEvidenceFile("entity_resolution/er-test-eclipse.db");
		/*
		Evidence e1 = new Evidence(mln.clauses.get(0).atoms.get(0).symbol,values1);
		evid_list.add(e1);
		ArrayList<Integer>values2 = new ArrayList<Integer>();
		values2.add(1);
		//values2.add(2);
		Evidence e2 = new Evidence(mln.clauses.get(0).atoms.get(0).symbol,values2);
		evid_list.add(e2);
		*/
		mln.convertToNormalForm(mln,evid_list); // added by Happy
		System.out.println("No. of clauses : "+mln.clauses.size());
		//System.out.println("mln is : ");
		//mln.print(mln.clauses, "MLN...");
		if(print)
			System.out.println("Time to parse = " + (System.currentTimeMillis() - time) + " ms");
		
		time = System.currentTimeMillis();
		MLN groundMln = GrindingMill.ground(mln);
		if(print)
			System.out.println("Time to convert = " + (System.currentTimeMillis() - time) + " ms");
		//System.out.println("mln is : ");
		//groundMln.print(groundMln.clauses, "MLN...");
		//String path = System.getProperty("java.library.path");
		//System.out.println("Java pah : "+path);
		time = System.currentTimeMillis();
		networkConstructionTime = time - networkConstructionTime;
		//PrintWriter out = new PrintWriter(new File ("segment/segment_ground_6_formulas_29.cnf"));
		//PrintWriter out = new PrintWriter(new File ("smoke/gurobi/gurobi_friends_smoke_ground_499.data"));
		//PrintWriter out = new PrintWriter(new File ("ancestor/ancestor_9.cnf"));
		//PrintWriter out = new PrintWriter(new File ("student/smoke.cnf"));
		//PrintWriter out = new PrintWriter(new File ("webkb/webkb_mln_5_10.cnf"));
		//writeDimacs(groundMln,out);
		long sumSolverTime = 0;
		for(int i = 0 ; i < 5 ; i++)
		{
			time = System.currentTimeMillis();
			this.convertToWeightedMaxSat(groundMln);
			solver.solve();
			
			long endTime = System.currentTimeMillis();
			solverTime = endTime - time;
			
			//bestValue = solver.bestValue();
			
			if(print) {
				//System.out.println("Best value = "+bestValue);
				System.out.println("Running time of LMAP = " + (endTime -  time) + " ms");
				//System.out.println("Total running time is " + (endTime -  startTime) + " ms");
			}
			sumSolverTime += solverTime;
			((GurobiWmsSolver)solver).reset();
		}
		solverTime = (long)(sumSolverTime/5.0);
		
		//this.convertToWeightedMaxSat(groundMln);
		/*
		for(int timelimit = 5 ; timelimit <= 60 ; timelimit+=5)
		{
			solver.setTimeLimit((double)timelimit);
			solver.solve();
			out.println(timelimit+"\t"+solver.bestValue());
		}*/
		//solver.solve();
		//out.println(60+"\t"+solver.bestValue());
		//out.flush();
		
		//long endTime = System.currentTimeMillis();
		//solverTime = endTime - time;
		/*
		if(print) {
			System.out.println("Running time of LMAP = " + (endTime -  time) + " ms");
			System.out.println("Total running time is " + (endTime -  startTime) + " ms");
		}
		*/
	}
	
	
	public static void main(String[] args) throws IOException, ContradictionException, PredicateNotFound {
		WeightedMaxSatSolver solver = new GurobiWmsSolver();
		//solver.setTimeLimit(80.0);
		GroundMaxWalkSat gmws = new GroundMaxWalkSat(solver);
//		print = false;
		
	    gmws.run("smoke/smoke_normal.txt");
		//gmws.run("segment/segment_mln_int_lifted_30.txt");
//		lmap.run("testfiles/simple_mln1.txt");
//		lmap.run("testfiles/ground_mln1.txt");
//		gmws.run("smoker/friends_smokers.txt");
		
//		System.out.println("Network Construction time: " + gmws.networkConstructionTime);
//		System.out.println("Solver time: " + gmws.solverTime);
		
		//List<String> domainList = new ArrayList<String>();
//		domainList.add("20");
//		domainList.add("25");
//		domainList.add("30");
//		domainList.add("35");
//		domainList.add("35_50");
//		domainList.add("35_100");
		//domainList.add("29");
		/*
		for (String domainSize : domainList) {
			int k = 2;
			//int noOfPredicates = 4;
			//int noOfClauses = 50;
			
			String inputFile = "student/student_mln_" + domainSize +".txt" ;
			String outFile = "student/student_ground_" + domainSize +".cnf" ;
//			String inputFile = "random/random_" +noOfPredicates + "_" + noOfClauses + "_" +  k + "_" + domainSize +".txt" ;
//			String outFile = "random/random_" +noOfPredicates + "_" + noOfClauses + "_" +  k + "_" + domainSize + ".cnf";
			
			PrintWriter out = new PrintWriter(new File (outFile));
			MLN mln = new MLN();
			Parser parser = new Parser(mln);
			parser.parseInputMLNFile(inputFile);
			MLN groundMln = GrindingMill.ground(mln);
			gmws.writeDimacs(groundMln, out);
			System.gc();
		}
		*/
		
	}

	private void writeDimacs(MLN mln, PrintWriter out) {
		
		out.println("p wcnf " + mln.symbols.size() + " " + mln.clauses.size());
		
		for (WClause clause : mln.clauses) {
			int power = 1;
			int[] c = new int[clause.atoms.size()];
			
			for (int i = 0; i < clause.atoms.size(); i++) {
				Atom atom = clause.atoms.get(i);
				power *= atom.getNumberOfGroundings();
				
				if(clause.sign.get(i)) {
					c[i] = -atom.symbol.id-1;
				} else {
					c[i] = atom.symbol.id+1;
				}
			}
			
			LogDouble weight = clause.weight.power(power);
			//out.print(weight.getValue() + " "); // commented by happy
			out.print((int)weight.getValue() + " "); // added by Happy
			for (int i = 0; i < c.length; i++) {
				out.print(c[i] + " ");
			}
			out.println("0");
			out.flush();
			
		}
		out.close();

	}
}
