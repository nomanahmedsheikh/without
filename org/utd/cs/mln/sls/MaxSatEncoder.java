package org.utd.cs.mln.sls;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.sat4j.ILauncherMode;
import org.sat4j.core.VecInt;
import org.sat4j.maxsat.WeightedMaxSatDecorator;
import org.sat4j.pb.PseudoOptDecorator;
import org.sat4j.pb.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IProblem;
import org.utd.cs.mln.alchemy.core.Atom;
import org.utd.cs.mln.alchemy.core.MLN;
import org.utd.cs.mln.alchemy.core.Term;
import org.utd.cs.mln.alchemy.core.WClause;
import org.utd.cs.mln.sls.sat4j.NullLogable;
import org.utd.cs.mln.sls.sat4j.NullOutputStream;

public class MaxSatEncoder {
	
	private MLN mln;
	
	private List<List<Integer>> variables;
	private List<int[]> hard_clauses = new ArrayList<int[]>();
	private List<int[]> soft_clauses = new ArrayList<int[]>();
	private List<Integer> clause_weights = new ArrayList<Integer>();
	private int nbvar = 0;
	private int nbclauses = 0;
	private int top = 1;
	
	public MaxSatEncoder(MLN _mln) {
		mln = _mln;
		variables = new ArrayList<List<Integer>>(mln.symbols.size());
		for (int i = 0; i < mln.symbols.size(); i++) {
			variables.add(new ArrayList<Integer>());
		}
	}
	
	public void encode() {
		int satVariableId = 1;
		for (WClause clause : mln.clauses) {
			// Define variables
			for (Atom atom : clause.atoms) {
				if(variables.get(atom.symbol.id).isEmpty()) {
					for (Term term : atom.terms) {
						variables.get(atom.symbol.id).add(satVariableId);
						
						int[] scl = {-satVariableId};
						soft_clauses.add(scl);
						clause_weights.add(term.domain.size());
						top += term.domain.size();
						nbvar++;
						nbclauses++;
						
						satVariableId++;
					}
				}
			}
			
			//link clauses
			for (int i = 0; i < clause.atoms.size(); i++) 
			{
				for (int j = 0; j < clause.atoms.get(i).terms.size(); j++)
				{
					for (int k = i+1; k < clause.atoms.size(); k++) 
					{
						for (int l = 0; l < clause.atoms.get(k).terms.size(); l++)
						{
							if(clause.atoms.get(i).terms.get(j) == clause.atoms.get(k).terms.get(l)
									&& clause.atoms.get(i).symbol.id != clause.atoms.get(k).symbol.id)
							{
								// There is a link between Term_{i,j} and Term_{k,l}
//								int[] cl1 = {variables.get(clause.atoms.get(i).symbol.id).get(j),
//										-variables.get(clause.atoms.get(k).symbol.id).get(l)};
//								int[] cl2 = {-variables.get(clause.atoms.get(i).symbol.id).get(j),
//										variables.get(clause.atoms.get(k).symbol.id).get(l)};
								
								int[] cl1 = {variables.get(clause.atoms.get(i).symbol.id).get(j)};
								int[] cl2 = {variables.get(clause.atoms.get(k).symbol.id).get(l)};

								hard_clauses.add(cl1);
								hard_clauses.add(cl2);
								nbclauses++;
								nbclauses++;
							}
						}
						// Self-join clauses
						if(clause.atoms.get(i).symbol.id == clause.atoms.get(k).symbol.id
								&& clause.atoms.get(i).terms.get(j) != clause.atoms.get(k).terms.get(j))
						{
							// Term-j of re-occurring Symbol P_i, P_k are not equal. Hence it should be grounded
							int[] cl1 = {variables.get(clause.atoms.get(i).symbol.id).get(j)};
							int[] cl2 = {variables.get(clause.atoms.get(k).symbol.id).get(j)};
							
							hard_clauses.add(cl1);
							hard_clauses.add(cl2);
							nbclauses++;
							nbclauses++;
						}
					}
				}
			}
		}
		
		for (List<Integer> group : variables) {
			for (int i = 0; i < group.size(); i++) {
				for (int j = i+1; j < group.size(); j++) {
					int[] cl = {group.get(i), group.get(j)};
					hard_clauses.add(cl);
					nbclauses++;
				}
			}
			
		}
	}
	
	public void solve() throws ContradictionException {
		org.sat4j.pb.SolverFactory.instance();
		WeightedMaxSatDecorator wmsd = new WeightedMaxSatDecorator(SolverFactory.newDefault(), false);

		wmsd.newVar(nbvar);
		wmsd.setExpectedNumberOfClauses(nbclauses);
		
		for (int[] hard_clause : hard_clauses) {
			wmsd.addHardClause(new VecInt(hard_clause));
		}
		
		for (int i = 0; i < soft_clauses.size(); i++) {
			wmsd.addSoftClause(clause_weights.get(i), new VecInt(soft_clauses.get(i)));
		}
		
		IProblem problem = new PseudoOptDecorator(wmsd, false, false);
		
		ILauncherMode.OPTIMIZATION.solve(problem, null, new NullLogable(), new PrintWriter(NullOutputStream.NULL_OUTPUT_STREAM), 0);
		
		int[] model = problem.model();
		
		for (int i = 0; i < variables.size(); i++) {
			for (int j = 0; j < variables.get(i).size(); j++) {
				
				int negLit = -variables.get(i).get(j);
				
				for (int k = 0; k < model.length; k++) {
					if(model[k] == negLit) {
						variables.get(i).set(j, negLit);
						break;
					} else if(model[k] == -negLit) {
						break;
					}
				}
				
			}
			
		}
		
	}
	
	public void writeDimacsFile() throws IOException {
		
		PrintWriter writer = new PrintWriter("encode.wcnf", "UTF-8");
		writer.println("p wcnf " + nbvar + " " + " " + nbclauses + " " + top);
		
		for (int[] hard_clause : hard_clauses) {
			writer.print(top + " ");
			for (int i = 0; i < hard_clause.length; i++) {
				writer.print(hard_clause[i] + " ");
			}
			writer.println("0");
		}

		for (int i = 0; i < soft_clauses.size(); i++) {
		   int[] soft_clause = soft_clauses.get(i);
			writer.print(clause_weights.get(i) + " ");
			for (int j = 0; j < soft_clause.length; j++) {
				writer.print(soft_clause[j] + " ");
			}
			writer.println("0");
		}
		
		writer.flush();
		writer.close();	
	}
	
	public List<List<Integer>> model() {
		return variables;
	}
	
}
