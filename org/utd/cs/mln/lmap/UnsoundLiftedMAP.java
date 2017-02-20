package org.utd.cs.mln.lmap;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.sat4j.specs.ContradictionException;
import org.utd.cs.gm.core.LogDouble;
import org.utd.cs.mln.alchemy.core.Atom;
import org.utd.cs.mln.alchemy.core.MLN;
import org.utd.cs.mln.alchemy.core.WClause;
import org.utd.cs.mln.alchemy.util.Parser;
import org.utd.cs.mln.alchemy.wms.GurobiWmsSolver;
import org.utd.cs.mln.alchemy.wms.WeightedMaxSatSolver;

public class UnsoundLiftedMAP {
	
	private WeightedMaxSatSolver solver;
	
	private static boolean print = true;
	
	public long networkConstructionTime = 0 ;
	public long solverTime = 0;
	public double bestValue = Double.NEGATIVE_INFINITY;
	
	public UnsoundLiftedMAP(WeightedMaxSatSolver _solver) {
		solver = _solver;
	}
	
	private void convertToWeightedMaxSat(MLN mln) {
		
		solver.setNoVar(mln.symbols.size());
		solver.setNoClauses(mln.clauses.size());
		
		for (WClause clause : mln.clauses) {
			int power = clause.getNumberOfGroundings();
			int[] c = new int[clause.atoms.size()];
			
			for (int i = 0; i < clause.atoms.size(); i++) {
				Atom atom = clause.atoms.get(i);

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
		
		time = System.currentTimeMillis();
//		MLN nonSharedMln = NonSharedConverter.convert(mln);
		
//		if(print)
//			System.out.println("Time to convert = " + (System.currentTimeMillis() - time) + " ms");
		
		time = System.currentTimeMillis();
		networkConstructionTime = time - networkConstructionTime;
		
		this.convertToWeightedMaxSat(mln);
		solver.solve();
		
		long endTime = System.currentTimeMillis();
		solverTime = endTime - time;
		
		bestValue = solver.bestValue();
		
		if(print) {
			System.out.println("Running time of LMAP = " + (endTime -  time) + " ms");
			System.out.println("Total running time is " + (endTime -  startTime) + " ms");
		}
	}
	
	public static void main(String[] args) throws IOException, ContradictionException {
		WeightedMaxSatSolver solver = new GurobiWmsSolver();
		solver.setTimeLimit(500);
		
		UnsoundLiftedMAP lmap = new UnsoundLiftedMAP(solver);
//		lmap.run("student/student_mln_int_100.txt");
//		lmap.run("segment/segment_mln_int_100.txt");
//		lmap.run("smoker/smoker_mln_10.txt");
		lmap.run("negative_weight_positive.txt");
//		lmap.run("webkb/webkb_mln_100.txt");
	}


}
