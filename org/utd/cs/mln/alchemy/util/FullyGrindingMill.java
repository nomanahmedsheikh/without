package org.utd.cs.mln.alchemy.util;

import org.utd.cs.gm.core.LogDouble;
import org.utd.cs.gm.utility.Pair;
import org.utd.cs.mln.alchemy.core.*;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Happy on 2/28/17.
 */
public class FullyGrindingMill {

    private GroundMLN groundMln;
    private List<GroundPredicate> groundPredicatesList;

    private void init() {
        groundMln = new GroundMLN();
        groundPredicatesList = new ArrayList<>();
    }

    public GroundMLN ground(MLN mln) {
        init();
        for(Formula formula : mln.formulas)
        {
            Set<Term> formulaWiseTermToGround = new HashSet<Term>();
            for (WClause clause : formula.clauses) {
                for (Atom atom : clause.atoms) {
                    for (int j = 0; j < atom.terms.size(); j++) {
                        Term term = atom.terms.get(j);
                        formulaWiseTermToGround.add(term);
                    }
                }
            }
            ground(formula, new ArrayList<Term>(formulaWiseTermToGround));
        }

        groundMln.groundPredicates.addAll(groundPredicatesList);
        return groundMln;
    }

    private void ground(Formula formula, ArrayList<Term> terms) {
        List<GroundFormula> groundFormulas = new ArrayList<>();
        int[][] permutations = permute(terms);

        for(int i = 0 ; i < permutations.length ; i++)
        {
            GroundFormula newFormula = new GroundFormula();
            int currentFormulaId = groundMln.groundFormulas.size()+i;
            newFormula.formulaId = currentFormulaId;
            newFormula.parentFormulaId = formula.formulaId;
            newFormula.weight = new LogDouble(formula.weight.getValue(), true);
            groundFormulas.add(newFormula);
            for(int c = 0 ; c < formula.clauses.size() ; c++)
            {
                WClause clause = formula.clauses.get(c);
                GroundClause newGroundClause = new GroundClause();
                newGroundClause.formulaId = currentFormulaId;
                newGroundClause.weight = new LogDouble(clause.weight.getValue(), true);
                List<GroundAtom> newGroundAtoms = new ArrayList<>();
                for(int j = 0 ; j < clause.atoms.size() ; j++)
                {
                    Atom oldAtom = clause.atoms.get(j);
                    Boolean sign = clause.sign.get(j);
                    int valTrue = clause.valTrue.get(j);
                    GroundPredicate gp = new GroundPredicate();
                    gp.symbol = new GroundPredicateSymbol(oldAtom.symbol.id,oldAtom.symbol.symbol,oldAtom.symbol.values);
                    for(Term term : oldAtom.terms)
                    {
                        int termIndex = terms.indexOf(term);
                        gp.terms.add(permutations[i][termIndex]);
                    }
                    int gpIndex = groundPredicatesList.indexOf(gp);
                    if(gpIndex == -1) {
                        groundPredicatesList.add(gp);
                        int numPossibleValues = oldAtom.symbol.values.values.size();
                        for(int k = 0 ; k < numPossibleValues ; k++)
                        {
                            gp.groundFormulaIds.add(new ArrayList<Pair>());
                            gp.totalSatWeight.add(0.0);
                        }
                        gp.numPossibleValues = numPossibleValues;
                        gpIndex = groundPredicatesList.size()-1;
                    }
                    gp = groundPredicatesList.get(gpIndex);
                    gp.groundFormulaIds.get(valTrue).add(new Pair(currentFormulaId,c));
                    newGroundAtoms.add(new GroundAtom(gp, sign, valTrue));
                }
                newGroundClause.groundAtoms = newGroundAtoms;
                newFormula.groundClauses.add(newGroundClause);
            }
        }
        groundMln.groundFormulas.addAll(groundFormulas);
    }

    /**
     * Create all possible permutation of a the domains of the terms
     * @param terms
     * @return
     */

    private int[][] permute(List<Term> terms) {

        int permutaionSize = 1;
        for (Term term : terms) {
            permutaionSize *= term.domain.size();
        }

        int[][] permuations = new int[permutaionSize][terms.size()];

        for (int i = 0; i < permuations.length; i++) {
            int residue = i;
            for (int j = 0; j < terms.size(); j++) {
                int index = residue % terms.get(j).domain.size();
                residue = residue / terms.get(j).domain.size();
                permuations[i][j] = terms.get(j).domain.get(index);
            }
        }

        return permuations;

    }

}
