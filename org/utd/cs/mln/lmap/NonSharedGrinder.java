package org.utd.cs.mln.lmap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.utd.cs.gm.core.LogDouble;
import org.utd.cs.mln.alchemy.core.Atom;
import org.utd.cs.mln.alchemy.core.MLN;
import org.utd.cs.mln.alchemy.core.WClause;
import org.utd.cs.mln.alchemy.util.GrindingMill;
import org.utd.cs.mln.alchemy.util.Parser;

public class NonSharedGrinder {
	
	public static void main(String[] args) throws FileNotFoundException {
		if(args.length < 3) {
			System.err.println("Usage: Ground <how> <input-mln> <output-cnf>");
			return;
		}
		
		String grindingMethod = args[0];
		
		String inputFile = args[1] ;
		String outFile = args[2];
		
		PrintWriter out = new PrintWriter(new File (outFile));
		MLN mln = new MLN();
		Parser parser = new Parser(mln);
		parser.parseInputMLNFile(inputFile);
		MLN groundMln;
		if(grindingMethod.startsWith("N")) {
			groundMln = NonSharedConverter.convert(mln);;
		} else {
			groundMln = GrindingMill.ground(mln);
		}
		
		writeDimacs(groundMln, out);

	}

	private static void writeDimacs(MLN mln, PrintWriter out) {
		
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
			out.print(weight.getValue() + " ");
			for (int i = 0; i < c.length; i++) {
				out.print(c[i] + " ");
			}
			out.println("0");
			out.flush();
			
		}
		out.close();

	}

}
