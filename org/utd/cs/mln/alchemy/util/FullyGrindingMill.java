package org.utd.cs.mln.alchemy.util;

import org.utd.cs.gm.core.LogDouble;
import org.utd.cs.mln.alchemy.core.*;

import java.util.*;

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
        int[][] permutations = permute(terms);

        for(int i = 0 ; i < permutations.length ; i++)
        {
            GroundFormula newFormula = new GroundFormula();
            int currentFormulaId = groundMln.groundFormulas.size();
            newFormula.formulaId = currentFormulaId;
            newFormula.parentFormulaId = formula.formulaId;
            newFormula.weight = new LogDouble(formula.weight.getValue(), true);
            List<GroundClause> newGroundClauseList = new ArrayList<GroundClause>();
            for(int c = 0 ; c < formula.clauses.size() ; c++)
            {
                WClause clause = formula.clauses.get(c);
                GroundClause newGroundClause = new GroundClause();
                newGroundClause.formulaId = currentFormulaId;
                newGroundClause.weight = new LogDouble(clause.weight.getValue(), true);
                Map<Integer, BitSet> gpIndexToSatVals = new HashMap<>();
                List<GroundAtom> newGroundAtoms = new ArrayList<>();
                List<GroundPredicate> newGroundPreds = new ArrayList<>(); // We need this list because once a groundClause is created, we want to
                // update formulaIds info of each groundPred. We can't do it on the go because we don't know whether groundClause will be created
                // or not, since it can be removed due to preprocessing.
                boolean clauseToRemove = false;

                // Iterate over each first order atom, and create ground atom for it.
                for(int j = 0 ; j < clause.atoms.size() ; j++)
                {
                    boolean sign = clause.sign.get(j);
                    Atom oldAtom = clause.atoms.get(j); // first order atom
                    int valTrue = clause.valTrue.get(j);
                    GroundPredicate gp = new GroundPredicate(); // GroundPredicate to create
                    gp.symbol = new GroundPredicateSymbol(oldAtom.symbol.id,oldAtom.symbol.symbol,oldAtom.symbol.values);
                    // Fill in the terms with constants
                    for(Term term : oldAtom.terms)
                    {
                        int termIndex = terms.indexOf(term);
                        gp.terms.add(permutations[i][termIndex]);
                    }

                    // Check if this groundPredicate already exists, if it does not, then add it to groundPredicate List.
                    // Note that it may happen that this clause gets removed later due to preprocessing, but still,
                    // we need this groundPredicate, so there is no harm in adding it to groundPredicate List.
                    int gpIndex = groundPredicatesList.indexOf(gp);
                    if(gpIndex == -1) {
                        groundPredicatesList.add(gp);
                        int numPossibleValues = oldAtom.symbol.values.values.size();
                        gp.numPossibleValues = numPossibleValues;
                        gpIndex = groundPredicatesList.size()-1;
                    }
                    gp = groundPredicatesList.get(gpIndex);

                    // Check if this groundPredicate occurs first time in this ground clause. then update
                    // groundClause's data structures about this groundPredicate.
                    int gpIndexInClause = newGroundClause.groundPredIndices.indexOf(gpIndex);
                    GroundAtom newGroundAtom = new GroundAtom(gpIndex, gpIndexInClause, valTrue, sign);
                    if(gpIndexInClause == -1)
                    {
                        newGroundPreds.add(gp);
                        newGroundClause.groundPredIndices.add(gpIndex);
                        gpIndexInClause = newGroundClause.groundPredIndices.size()-1;
                        newGroundAtom.clauseGroundPredIndex = gpIndexInClause;
                        newGroundClause.globalToLocalPredIndex.put(gpIndex,gpIndexInClause);
                        newGroundClause.localPredIndexToAtomIndices.put(gpIndexInClause, new ArrayList<>());
                        gpIndexToSatVals.put(gpIndexInClause, new BitSet(gp.numPossibleValues));
                    }
                    else
                    {
                        // If this groundAtom has already come, then don't add it and move to next atom
                        if(newGroundAtoms.contains(newGroundAtom))
                            continue;
                    }
                    newGroundAtoms.add(newGroundAtom);

                    // If this new ground atom gets added successfully, then update localPredIndexToAtomIndices list
                    newGroundClause.localPredIndexToAtomIndices.get(gpIndexInClause).add(newGroundAtoms.size()-1);

                    // Now once we have added new ground Atom, we need to check if ground clause gets satisfied or not.
                    BitSet gpBitSet = new BitSet(gp.numPossibleValues);
                    gpBitSet.set(valTrue);
                    if(sign == true)
                        gpBitSet.flip(0,gp.numPossibleValues);
                    gpBitSet.or(gpIndexToSatVals.get(gpIndexInClause));

                    // If all bits are set for this groundPred, then this clause will always be satisfied and hence,
                    // shouldn't be added into groundformula. Note that, although at this point, we know that
                    // this clause shouldn't be added, but still we shouldn't just break out of this loop, as we
                    // need to add groundPredicates, but we shouldn't add any clauseInfo into groundPredicates appearing
                    // in this clause.
                    if(gpBitSet.cardinality() == gp.numPossibleValues)
                        clauseToRemove = true;
                    gpIndexToSatVals.put(gpIndexInClause, gpBitSet);

                }

                // If this clause is to be added, then only update all gp's formulaId's info
                if(clauseToRemove == false)
                {
                    newGroundClause.groundAtoms = newGroundAtoms;
                    newGroundClauseList.add(newGroundClause);
                    for(GroundPredicate gp : newGroundPreds)
                    {
                        if(!gp.groundFormulaIds.containsKey(currentFormulaId))
                        {
                            gp.groundFormulaIds.put(currentFormulaId, new HashSet<>());
                        }
                        gp.groundFormulaIds.get(currentFormulaId).add(newGroundClauseList.size()-1);
                    }
                }
            }
            if(newGroundClauseList.size() > 0)
            {
                newFormula.groundClauses.addAll(newGroundClauseList);
                groundMln.groundFormulas.add(newFormula);
            }
        }
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
