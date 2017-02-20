package org.utd.cs.mln.lve.core;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.utd.cs.mln.alchemy.core.Atom;
import org.utd.cs.mln.alchemy.core.MLN;
import org.utd.cs.mln.alchemy.util.Parser;

public class LiftedVarElimTest {
	
	public static void main(String[] args) throws FileNotFoundException {
		
		MLN mln = new MLN();
		Parser parser = new Parser(mln);
		parser.parseInputMLNFile("testfiles/simple_mln1.txt");
		
		List<SimpleLogicalPotential>  potentials = new ArrayList<SimpleLogicalPotential>();

		for(int i=0; i< mln.formulas.size(); i++ ) {
			SimpleLogicalPotential p = new SimpleLogicalPotential(mln.formulas.get(i), mln);
			potentials.add(p);
		}
		
		SimpleLogicalPotential p = potentials.get(0);
		Atom atom = p.meeFormulas.get(0).cnf.get(0).atoms.get(0);

		System.out.println("Eliminating atom " + atom.symbol.symbol + " from the CNF: ");
		System.out.println();

		long time = System.currentTimeMillis();
		SimpleLogicalPotential result = p.maxOut(atom);
		time = System.currentTimeMillis() - time;

		System.out.println("After elimination the potential is:- ");

		for(int i=0; i<result.meeFormulas.size(); i++) {
			WeightedFormula f = result.meeFormulas.get(i);
			
			System.out.println("Weighted Formula " + i + " : ");
			System.out.println("CNF is: ");
			for(int j=0; j<f.cnf.size(); j++) {
				f.cnf.get(j).print();
			}
			System.out.println("Weight is: "+ f.weight);
			System.out.println();
		}
		
		System.out.println("Time taken to compute: " + time);
		
	}

}
