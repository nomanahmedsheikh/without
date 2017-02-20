package org.utd.cs.mln.lve.core;

import java.util.ArrayList;
import java.util.List;

import org.utd.cs.mln.alchemy.core.Atom;
import org.utd.cs.mln.alchemy.core.Formula;
import org.utd.cs.mln.alchemy.core.MLN;

public class SimpleLogicalPotential {
	public List<WeightedFormula> meeFormulas;

	public SimpleLogicalPotential() {
		meeFormulas = new ArrayList<WeightedFormula>();
	}

	public SimpleLogicalPotential(Formula f, MLN m) {
		meeFormulas = new ArrayList<WeightedFormula>();

		WeightedFormula wf = new WeightedFormula(f, m);
		meeFormulas.add(wf);
		meeFormulas.add(wf.negate());
	}

	public SimpleLogicalPotential multiply(SimpleLogicalPotential p) {

		SimpleLogicalPotential retVal = new SimpleLogicalPotential();

		for (int i=0; i < meeFormulas.size(); i++) {
			for (int j=0; j < p.meeFormulas.size(); j++) {

				WeightedFormula f = meeFormulas.get(i).multiply(p.meeFormulas.get(j));

				if(f.isSatisfiable()) {
					retVal.meeFormulas.add(f);
				}

			}
		}

		return retVal;
	}

	public SimpleLogicalPotential assign(Atom atom, boolean sign) {

		SimpleLogicalPotential retVal = new SimpleLogicalPotential();

		for (int i=0; i < meeFormulas.size(); i++) {
			WeightedFormula f = meeFormulas.get(i).assign(atom, sign);

			if(f.isSatisfiable()) {
				retVal.meeFormulas.add(f);
			}
		}

		return retVal;

	}

	public SimpleLogicalPotential maxOut(Atom atom) {
		SimpleLogicalPotential mX = assign(atom, false);
		SimpleLogicalPotential mNotX = assign(atom, true);

		SimpleLogicalPotential retVal = new SimpleLogicalPotential();

		for (int i=0; i < mX.meeFormulas.size(); i++) {
			for (int j=0; j < mNotX.meeFormulas.size(); j++) {

				WeightedFormula f = WeightedFormula.max(mX.meeFormulas.get(i), mNotX.meeFormulas.get(j));

				if(f.isSatisfiable()) {
					retVal.meeFormulas.add(f);
				}

			}
		}

		return retVal;
	}
}
