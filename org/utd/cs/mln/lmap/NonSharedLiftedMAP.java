package org.utd.cs.mln.lmap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import org.sat4j.specs.ContradictionException;
import org.utd.cs.gm.core.LogDouble;
import org.utd.cs.mln.alchemy.core.Atom;
import org.utd.cs.mln.alchemy.core.MLN;
import org.utd.cs.mln.alchemy.core.WClause;
import org.utd.cs.mln.alchemy.util.Parser;
import org.utd.cs.mln.alchemy.wms.GurobiWmsSolver;
import org.utd.cs.mln.alchemy.wms.WeightedMaxSatSolver;

public class NonSharedLiftedMAP {
	
	private WeightedMaxSatSolver solver;
	
	private static boolean print = true;
	
	public long networkConstructionTime = 0 ;
	public long solverTime = 0;
	public double bestValue = Double.NEGATIVE_INFINITY;
	
	public NonSharedLiftedMAP(WeightedMaxSatSolver _solver) {
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
	
	public  void run(String filename) throws FileNotFoundException {
		long time = System.currentTimeMillis();
		long startTime = time;
		
		networkConstructionTime = time;
		solverTime = 0;
		
		MLN mln = new MLN();
		Parser parser = new Parser(mln);
		parser.parseInputMLNFile(filename);

		if(print)
			System.out.println("Time to parse = " + (System.currentTimeMillis() - time) + " ms");
		//System.out.println("mln is : ");
		//mln.print(mln.clauses, "MLN...");
		//System.out.println("x's type : "+mln.clauses.get(0).atoms.get(0).terms.get(0));
		//System.out.println("y's type : "+mln.clauses.get(0).atoms.get(1).terms.get(0));
		time = System.currentTimeMillis();
		MLN nonSharedMln = NonSharedConverter.convert(mln);
		
		if(print)
			System.out.println("Time to convert = " + (System.currentTimeMillis() - time) + " ms");
		
		time = System.currentTimeMillis();
		networkConstructionTime = time - networkConstructionTime;
		long sumSolverTime = 0;
		for(int i = 0 ; i < 1 ; i++)
		{
			time = System.currentTimeMillis();
			this.convertToWeightedMaxSat(nonSharedMln);
			solver.solve();
			
			long endTime = System.currentTimeMillis();
			solverTime = endTime - time;
			
			bestValue = solver.bestValue();
			
			if(print) {
				System.out.println("Best value = "+bestValue);
				System.out.println("Running time of LMAP = " + (endTime -  time) + " ms");
				//System.out.println("Total running time is " + (endTime -  startTime) + " ms");
			}
			sumSolverTime += solverTime;
			((GurobiWmsSolver)solver).reset();
		}
		solverTime = (long)(sumSolverTime/1.0);
		//PrintWriter out = new PrintWriter(new File ("student/student_aistats_499.cnf"));
		//PrintWriter out = new PrintWriter(new File ("smoke/smoke_aistats_499.cnf"));
		//PrintWriter out = new PrintWriter(new File ("segment/segment_aistats_less_10.cnf"));
		//PrintWriter out = new PrintWriter(new File ("ancestor/ancestor_aistats_9.cnf"));
		//PrintWriter out = new PrintWriter(new File ("webkb/webkb_nonshared.cnf"));
		//writeDimacs(nonSharedMln,out);
		
		
	}
	
	public static void main(String[] args) throws IOException, ContradictionException {
		WeightedMaxSatSolver solver = new GurobiWmsSolver();
		solver.setTimeLimit(500);
		
		NonSharedLiftedMAP lmap = new NonSharedLiftedMAP(solver);
//		print = false;
		
//		lmap.run("random/random_4_50_2_10.txt");
//		lmap.run("student/student_mln_int_100.txt");
//		lmap.run("segment/segment_mln_int_lifted_100.txt");
//		lmap.run("smoker/smoker_mln_10.txt");
		//lmap.run("testfiles/test_mln.txt");
//		lmap.run("webkb/webkb_mln_100.txt");
		
//		System.out.println("Network Construction time: " + lmap.networkConstructionTime);
//		System.out.println("Solver time: " + lmap.solverTime);
		
//		List<Integer> domainList = new ArrayList<Integer>();
//		domainList.add(10);
//		domainList.add(25);
//		domainList.add(50);
//		domainList.add(100);
//		domainList.add(1000);
//		domainList.add(10000);
//		
//		for (Integer domainSize : domainList) {
//			int k = 2;
//			int noOfPredicates = 4;
//			int noOfClauses = 50;
//			
//			String inputFile = "random/random_" +noOfPredicates + "_" + noOfClauses + "_" +  k + "_" + domainSize +".txt" ;
//			
//			PrintWriter out = new PrintWriter(new File ("random/random_lifted_" +noOfPredicates + "_" + noOfClauses + "_" +  k + "_" + domainSize + ".cnf"));
//			MLN mln = new MLN();
//			Parser parser = new Parser(mln);
//			parser.parseInputMLNFile(inputFile);
//			MLN nonSharedMln = NonSharedConverter.convert(mln);
//			lmap.writeDimacs(nonSharedMln, out);
//			System.gc();
//		}
	}

	private void writeDimacs(MLN mln, PrintWriter out) {
		
		out.println("p wcnf " + mln.symbols.size() + " " + mln.clauses.size());
		
		for (WClause clause : mln.clauses) {
			Double power = 1.0;
			int[] c = new int[clause.atoms.size()];
			
			for (int i = 0; i < clause.atoms.size(); i++) {
				Atom atom = clause.atoms.get(i);
				power *= atom.getNumberOfGroundings();
				if(power < 0) {
					System.err.println("Arithmatic error occurred!");
					System.exit(1);
				}
				
				if(clause.sign.get(i)) {
					c[i] = -atom.symbol.id-1;
				} else {
					c[i] = atom.symbol.id+1;
				}
			}
			
			LogDouble weight = clause.weight.power(power);
			out.print(Math.round(weight.getValue()) + " ");
			for (int i = 0; i < c.length; i++) {
				out.print(c[i] + " ");
			}
			out.println("0");
			out.flush();
			
		}
		out.close();

	}

}
