package org.utd.cs.mln.lve.core;

import java.util.ArrayList;
import java.util.List;

import org.utd.cs.gm.core.LogDouble;
import org.utd.cs.mln.alchemy.core.Atom;
import org.utd.cs.mln.alchemy.core.Formula;
import org.utd.cs.mln.alchemy.core.MLN;
import org.utd.cs.mln.alchemy.core.WClause;

public class WeightedFormula {
	public List<WClause> cnf;
	
	public LogDouble weight;
	
	public boolean hasEmptyClause;

	public WeightedFormula(List<WClause> cnf_, LogDouble weight_) { 
		cnf = cnf_; 
		weight = weight_;
		hasEmptyClause = false;
	}

	public WeightedFormula(List<WClause> cnf_, LogDouble weight_, boolean hasEmptyClause_) { 
		cnf = cnf_; 
		weight = weight_;
		hasEmptyClause = hasEmptyClause_;
	}

	public WeightedFormula(Formula f, MLN m) {
		hasEmptyClause = false;
		weight = f.weight;
		cnf = new ArrayList<WClause>();

		for(int i=f.MLNClauseStartIndex; i<f.MLNClauseEndIndex; i++) {
			cnf.add(MLN.create_new_clause(m.clauses.get(i)));
		}
	}

	public WClause assign(Atom atom, boolean sign, WClause clause) {

		WClause new_clause = MLN.create_new_clause(clause);

		for(int k=0;k<new_clause.atoms.size();k++)
		{
			if(new_clause.atoms.get(k).symbol.id == atom.symbol.id)
			{
				if(new_clause.sign.get(k) != sign)
				{
					//conflicting atom in clause, remove it
					new_clause.removeAtom(k);
					break;
				}
				else
				{
					//set clause to satisfied
					new_clause.satisfied = true;
					//satisfied atom in clause, remove it
					new_clause.removeAtom(k);
					break;
				}
			}
		}


		return new_clause;
	}

	public WeightedFormula negate() {

		//TODO: How to negate a CNF? Solution: assume problem definition in clause. Hence we can negate this

		List<WClause> new_cnf = new ArrayList<WClause>();
		LogDouble wt = LogDouble.ONE;

		if(this.cnf.size() == 1) {
			// A single clause CNF
			WClause clause = this.cnf.get(0);
			for(int i=0; i< clause.atoms.size(); i++) {
				WClause new_clause = new WClause();
				new_clause.sign.add(!clause.sign.get(i));
				new_clause.weight = wt;

				Atom new_atom = MLN.create_new_atom(clause.atoms.get(i));
				new_clause.atoms = new ArrayList<Atom>();
				new_clause.atoms.add(new_atom);

				new_cnf.add(new_clause);
			}

		} else {
			throw new UnsupportedOperationException("Only negation of a clause is supported.");
		}

		return new WeightedFormula(new_cnf, wt);
	}


	public WeightedFormula multiply(WeightedFormula f) {

		List<WClause> cnf = new ArrayList<WClause>();
		LogDouble w =  this.weight.multiply(f.weight);

		MLN.copyAllClauses((f.cnf), cnf);
		MLN.copyAllClauses((this.cnf), cnf);

		boolean newCnfHasEmptyClause = f.hasEmptyClause || hasEmptyClause;

		return new WeightedFormula(cnf, w, newCnfHasEmptyClause);
	}

	public WeightedFormula assign(Atom atom, boolean sign) {
		
		List<WClause> newCnf = new ArrayList<WClause>();
		boolean newCnfHasEmptyClause = false;

		for(int i=0; i< cnf.size(); i++) {
			WClause assigned_clause = assign(atom, sign, cnf.get(i));

			if(assigned_clause.satisfied) {
				//Clause is satisfied. Do Nothing
				continue;
			}

			if(assigned_clause.atoms.size() == 0) {
				// Clause is NOT satisfied, and it is empty. Set Empty clause flag. No need to assign
				newCnfHasEmptyClause = true;
				break;
			}

			newCnf.add(assigned_clause);
		}

		//TODO: If a logical variable is removed adjust the weight accordingly, else weight remains unchanged
		LogDouble w = weight;

		return new WeightedFormula(newCnf, w, newCnfHasEmptyClause);
	}

	public static WeightedFormula max(WeightedFormula fi, WeightedFormula fj) {

		List<WClause> cnf = new ArrayList<WClause>();
		LogDouble w =  LogDouble.max(fi.weight , fj.weight);

		MLN.copyAllClauses((fi.cnf), cnf);
		MLN.copyAllClauses((fj.cnf), cnf);
		
		boolean newCnfHasEmptyClause = fi.hasEmptyClause || fj.hasEmptyClause;

		return new WeightedFormula(cnf, w, newCnfHasEmptyClause);
	}

	public boolean isSatisfiable() {

		if(this.cnf.size() == 0) {
			// No clause
			return true;
		}

		if(this.hasEmptyClause) {
			//Has conflict
			return false;
		}

		// FIXME: How to check for satisfiability?

		return true;
	}
}
