package org.utd.cs.mln.test.lmap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import org.utd.cs.mln.alchemy.GroundMaxWalkSat;
import org.utd.cs.mln.alchemy.core.PredicateNotFound;
import org.utd.cs.mln.alchemy.wms.GurobiWmsSolver;
import org.utd.cs.mln.alchemy.wms.WeightedMaxSatSolver;
import org.utd.cs.mln.lmap.NonSameEquivClass;
import org.utd.cs.mln.lmap.NonSharedLiftedMAP;

public class RunLmap {

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 * @throws PredicateNotFound 
	 */
	public static void main(String[] args) throws FileNotFoundException, PredicateNotFound {
		GurobiWmsSolver solver = new GurobiWmsSolver();
		solver.setTimeLimit(15.0);
		//GroundMaxWalkSat gmws = new GroundMaxWalkSat(solver);
		//WeightedMaxSatSolver solver = new WeightedMaxSatSolver();
		//NonSharedLiftedMAP lmap = new NonSharedLiftedMAP(solver);
		NonSameEquivClass lmap = new NonSameEquivClass(solver);
		
	    //File file = new File ("smoke/gurobi/smoke_run_lmap_aistats.data");
	    //PrintWriter writer = new PrintWriter (new FileOutputStream(file,true));
	    //writer.println("Domain\tLmap-NC\tLmap-ST\tLmap-Tot\tLmap-BestCost");
	    //writer.flush();
		
		for (int i = 25; i <= 25; i=i+25) {
			System.out.println("i = "+i);

			String datasetLocation=System.getProperty("user.dir")+File.separator+"datasets"+File.separator+"mln_files"+File.separator;
			String mlnFilename=datasetLocation+"smoke_mln_25.txt";

			//String fileName = "student/mln_files/student_mln_" + i + ".txt";
			//String fileName = "smoke/mln_files/smoke_mln_"+i+".txt";//
			String fileName = mlnFilename;

			//String fileName = "segment/mln_files/segment_mln_int_lifted_"+i+".txt";
			//String fileName = "webkb/webkb_mln_int_30.txt";
			//String fileName = "ancestor/ancestor_mln.txt";
			//String fileName = "student/student_mln_499.txt";
			//String fileName = "entity_resolution/er-bnct-eclipse.mln";
			long time = System.currentTimeMillis();
			lmap.run(fileName);
			long lmap_tot = System.currentTimeMillis() - time;
			
//			time = System.currentTimeMillis();
		//	gmws.run(fileName);
		//	long gmws_tot = System.currentTimeMillis() - time;
		    
			//writer.println(i+"\t"+lmap.networkConstructionTime+"\t"+lmap.solverTime+"\t"+lmap_tot+"\t"+lmap.bestValue);
			//writer.println(i+"\t"+gmws.networkConstructionTime+"\t"+gmws.solverTime+"\t"+gmws_tot);
		    //writer.flush();
		    solver.reset();
		    System.gc();
		}
	    //writer.close();
	}

}