# without

Important data structures :
First order level :
1. MLN :
(a) List of formulas

2. Formula
(a) List of clauses
(b) Weight

3. WClause
(a) Weight
(b) List of atoms
(c) Boolean list of signs. This stores the sign of each atom in this clause.
(d) Integer list of valTrue. For each atom in this clause, this stores at what value this atom becomes true.

4. Atom
(a) Predicate symbol
(b) List of terms

5. Term
(a) Domain : List of integers

Ground level : 
1. GroundMLN
(a) List of ground Formulas
(b) List of ground Predicates

2. GroundFormula
(a) List of Ground Clauses
(b) formulaId : stores index of this formula in GroundMLN's list of ground formulas.
(c) parentFormulaId : stores id of first order formula from which it came.
(d) weight
(e) isSatisfied

3. GroundClause
(a) List of groundAtoms
(b) formulaId : id of groundFormula of which this is a part
(c) weight
(d) isSatisfied

4. GroundAtom
(a) GroundPredicate : This stores grounding of this atom, for example, Smokes(Ana)
(b) Boolean sign
(c) int valTrue : at what value does this atom becomes true (excluding sign)

5. GroundPredicate
(a) GroundPredicateSymbol : stores for example Smokes
(b) List of integer terms
(c) int truthVal : current truth value of this groundPred
(d) int numPossibleValues : How mahy values can this groundPredicate take. Assumption : values always start from 0.
So if this attribute is 3, then this groundPred can take values 0,1,2.
(f) groundFormulaIds : 2D list. For each possible value of this groundPred, stores list of Pairs. Each pair
is <groundFormulaId, groundClauseId>

6. GroundPredicateSymbol
(a) int id
(b) String symbol
(c) Values values : stores list of values this type of groundPredSymbol can take.

TODO :
1. error checking in MLN file
2. IMP : In file FullyGrindingMill.java, groundPredicateList is a list for now, and when we create a new
groundPredicate in function ground(), we have to check whether this already exists in groundPredicateList.
This is O(n) in list. If we make it set, then we have a problem that, although we can check for containment,
but we can't get original element of set on which we can operate.
3. In gibbsSampler.java : // Read TODOs