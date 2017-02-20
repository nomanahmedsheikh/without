package org.utd.cs.mln.lmap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.sat4j.specs.ContradictionException;
import org.utd.cs.gm.core.LogDouble;
import org.utd.cs.mln.alchemy.core.Atom;
import org.utd.cs.mln.alchemy.core.Evidence;
import org.utd.cs.mln.alchemy.core.MLN;
import org.utd.cs.mln.alchemy.core.PredicateNotFound;
import org.utd.cs.mln.alchemy.core.WClause;
import org.utd.cs.mln.alchemy.util.Parser;
import org.utd.cs.mln.alchemy.wms.GurobiWmsSolver;
import org.utd.cs.mln.alchemy.wms.WeightedMaxSatSolver;

public class NonSameEquivClass {
	
	private WeightedMaxSatSolver solver;
	
	private static boolean print = true;
	
	public long networkConstructionTime = 0 ;
	public long solverTime = 0;
	public double bestValue = Double.NEGATIVE_INFINITY;
	
	public NonSameEquivClass(WeightedMaxSatSolver _solver) {
		solver = _solver;
	}
	
	private void convertToWeightedMaxSat(MLN mln) {
		
		solver.setNoVar(mln.symbols.size());
		solver.setNoClauses(mln.clauses.size());
		
		for (WClause clause : mln.clauses) {
			//int power = 1;
			int[] c = new int[clause.atoms.size()];
			
			for (int i = 0; i < clause.atoms.size(); i++) {
				Atom atom = clause.atoms.get(i);
				//power *= atom.getNumberOfGroundings();
				
				if(clause.sign.get(i)) {
					c[i] = -atom.symbol.id-1;
				} else {
					c[i] = atom.symbol.id+1;
				}
			}
			
			//LogDouble weight = clause.weight.power(power);
			solver.addSoftClause(clause.weight.getValue()*mln.numSubNetworks, c);
			
		}

	}
	
	static void removeTransitiveClauses(MLN mln)
	{
		ArrayList<Boolean> clausesToRemove = new ArrayList<Boolean>();
		for(WClause clause : mln.clauses)
		{
			int i = 0;
			Map<Integer,Boolean> predMap = new HashMap<Integer,Boolean>();
			for(i = 0 ; i < clause.atoms.size() ; i++)
			{
				boolean sign = clause.sign.get(i);
				int pred_id = clause.atoms.get(i).symbol.id;
				if(predMap.containsKey(pred_id))
				{
					if(sign != predMap.get(pred_id))
					{
						clausesToRemove.add(true);
						break;
					}
				}
				else
				{
					predMap.put(pred_id,sign);
				}
				
			}
			if(i == clause.atoms.size())
			{
				clausesToRemove.add(false);
			}
			
		}
		
		for(int i = mln.clauses.size() - 1 ; i >= 0 ; i--)
		{
			if(clausesToRemove.get(i))
			{
				mln.clauses.remove(i);
			}
		}
		//System.out.println("ClausesToRemove : "+clausesToRemove);
	}
	
	public  void run(String filename) throws FileNotFoundException, PredicateNotFound  {
		long time = System.currentTimeMillis();
		long startTime = time;
		
		networkConstructionTime = time;
		solverTime = 0;
		
		MLN mln = new MLN();
		Parser parser = new Parser(mln);
		parser.parseInputMLNFile(filename);
		// remove transitive clauses
		removeTransitiveClauses(mln);
		if(print)
			System.out.println("Time to parse = " + (System.currentTimeMillis() - time) + " ms");
		//System.out.println("MLN is  : ");
		//mln.print(mln.clauses, "printing MLN...");
		time = System.currentTimeMillis();
		//System.out.println("Before converting : first clause's first pred's second term's domain size : "+mln.clauses.get(0).atoms.get(0).terms.get(1).domain);
		//ArrayList<Evidence> evid_list = parser.parseInputEvidenceFile("entity_resolution/er-test-eclipse.db");
		//mln.convertToNormalForm(mln,evid_list); // added by Happy
		//System.out.println("No. of clauses : "+mln.clauses.size());
		MLN nonSameEquivMln = NonSameEquivConverter.convert(mln);
		//System.out.println("second clause's weight : "+nonSameEquivMln.clauses.get(1).weight.getValue());
		//System.out.println("After converting, no. of clauses : "+nonSameEquivMln.clauses.size());
		//System.out.println("After converting : first clause's first pred's first term's domain size : "+nonSameEquivMln.clauses.get(0).atoms.get(0).terms.get(0).domain);
		//PrintWriter out = new PrintWriter(new File ("student/student_9.cnf"));
		//PrintWriter out = new PrintWriter(new File ("smoke/friends_transitive_smoke_99.cnf"));
		//PrintWriter out = new PrintWriter(new File ("ancestor/ancestor_9.cnf"));
		//PrintWriter out = new PrintWriter(new File ("student/smoke.cnf"));
		//PrintWriter out = new PrintWriter(new File ("webkb/webkb_mln_int_30.cnf"));
		//PrintWriter out = new PrintWriter(new File ("segment/segment_10.cnf"));
		//writeDimacs(nonSameEquivMln,out);
		
		if(print)
			System.out.println("Time to convert = " + (System.currentTimeMillis() - time) + " ms");
		
		time = System.currentTimeMillis();
		networkConstructionTime = time - networkConstructionTime;
		
		 //Following lines commented by Happy
		this.convertToWeightedMaxSat(nonSameEquivMln);
		String path = System.getProperty("java.library.path");
		System.out.println("Java path : "+path);
		solver.solve();
		
		long endTime = System.currentTimeMillis();
		solverTime = endTime - time;
		
		bestValue = solver.bestValue();
		
		if(print) {
			System.out.println("Running time of LMAP = " + (endTime -  time) + " ms");
			System.out.println("Total running time is " + (endTime -  startTime) + " ms");
		}
		
	}
	
	public static void main(String[] args) throws IOException, ContradictionException, PredicateNotFound {
		WeightedMaxSatSolver solver = new GurobiWmsSolver();
		solver.setTimeLimit(20);
		
		NonSameEquivClass lmap = new NonSameEquivClass(solver);
//		print = false;
		
//		lmap.run("random/random_4_50_2_10.txt");
//		lmap.run("student/student_mln_29.txt");
//		lmap.run("segment/segment_mln_int_lifted_100.txt");
		lmap.run("smoke/smoke_mln_99.txt");
//		lmap.run("testfiles/test_mln.txt");
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
			Double power = (double) clause.getNumberOfGroundings();
			int[] c = new int[clause.atoms.size()];
			
			for (int i = 0; i < clause.atoms.size(); i++) {
				Atom atom = clause.atoms.get(i);
				//power *= atom.getNumberOfGroundings();
				//System.out.println("number of terms for pred "+atom.symbol.symbol+" : "+atom.terms.size());
				//System.out.println("first term's domain size : "+atom.terms.get(0).domain.size());
				//System.out.println("second term's domain size : "+atom.terms.get(1).domain.size());
				//System.out.println("number of groundings = "+atom.getNumberOfGroundings());
				/*
				if(power < 0) {
					System.err.println("Arithmatic error occurred!");
					System.exit(1);
				}*/
				
				if(clause.sign.get(i)) {
					c[i] = -atom.symbol.id-1;
				} else {
					c[i] = atom.symbol.id+1;
				}
			}
			//System.out.println("power = "+power);
			//LogDouble weight = clause.weight.power(power);
			out.print(Math.round(clause.weight.getValue()*mln.numSubNetworks) + " "); // commented by Happy to print scaled weights 
			//out.print(Math.round(weight.getValue())+" ");
			for (int i = 0; i < c.length; i++) {
				out.print(c[i] + " ");
			}
			out.println("0");
			out.flush();
			
		}
		out.close();

	}

}
