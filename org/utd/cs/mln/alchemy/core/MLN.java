package org.utd.cs.mln.alchemy.core;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.utd.cs.mln.lmap.ClauseRoot;
import org.utd.cs.mln.lmap.LiftedPTP;
import org.utd.cs.mln.lmap.Node;

import org.utd.cs.gm.utility.DeepCopyUtil;

/**
 * @author happy
 *
 */
public class MLN {

	public int max_predicate_id;
	public int maxDegree;
	public List<PredicateSymbol> symbols = new ArrayList<PredicateSymbol>();
	public List<Domain> domainList;
	public Map<Integer, ArrayList<Integer>> predicateDomainMap;
	public List<WClause> clauses = new ArrayList<WClause>();
	public List<Formula> formulas = new ArrayList<Formula>();
	public double numSubNetworks = 1;
	public ArrayList<ArrayList<Boolean>> validPredPos = new ArrayList<ArrayList<Boolean>>();
	
	public MLN(MLN mln){
		LiftedPTP.numMLNs++;
		for(WClause clause : mln.clauses){
			WClause newClause = MLN.create_new_clause(clause);
			clauses.add(newClause);
		}
		for(int i  = 0  ; i <= LiftedPTP.maxPredId ; i++){
			validPredPos.add(new ArrayList<Boolean>());
		}
		for(WClause clause : mln.clauses){
			//clause.print();
			for(Atom atom : clause.atoms){
				int predId = atom.symbol.id;
				for(int i = atom.terms.size() - 1 ; i >= 0 ; i--){
					validPredPos.get(predId).add(mln.validPredPos.get(predId).get(i));
				}
			}
		}
	}

	public static WClause create_new_clause(WClause clause) {

		WClause new_clause = new WClause();
		new_clause.sign = new ArrayList<Boolean>(clause.sign);
		new_clause.satisfied = clause.satisfied;
		new_clause.weight = clause.weight;
		new_clause.root = MLN.copyRoot(clause.root);
		new_clause.hcCount = (ArrayList<Integer>)DeepCopyUtil.copy(clause.hcCount);
		/*for(ArrayList<Integer> tuple : clause.tuples){
			new_clause.tuples.add(new ArrayList<Integer>(tuple));
		}*/
		//if atoms have common terms their relationship must be maintained when new clause is created
		List<Term> newTerms = new ArrayList<Term>();
		List<Term> oldTerms = new ArrayList<Term>();

		for (int i = 0; i < clause.atoms.size(); i++) 
		{
			for (int j = 0; j < clause.atoms.get(i).terms.size(); j++)
			{
				int termPosition=-1;
				for(int m=0;m<oldTerms.size();m++)
				{
					if(oldTerms.get(m)==clause.atoms.get(i).terms.get(j))
					{
						termPosition = m;
					}
				}
				if(termPosition==-1)
				{
					Term term = new Term();
					term.type = clause.atoms.get(i).terms.get(j).type;
					for(int k=0;k<clause.atoms.get(i).terms.get(j).domain.size();k++)
						term.domain.add(clause.atoms.get(i).terms.get(j).domain.get(k));
					newTerms.add(term);
					oldTerms.add(clause.atoms.get(i).terms.get(j));
				}
				else
				{
					newTerms.add(newTerms.get(termPosition));
					oldTerms.add(clause.atoms.get(i).terms.get(j));
				}
			}
		}
		int ind=0;
		new_clause.atoms = new ArrayList<Atom>(clause.atoms.size());
		for (int i = 0; i < clause.atoms.size(); i++) {
			new_clause.atoms.add(new Atom());
			new_clause.atoms.get(i).symbol = create_new_symbol(clause.atoms.get(i).symbol);
			ArrayList<HyperCube> predHyperCubes = new ArrayList<HyperCube>();
			predHyperCubes.addAll(clause.atoms.get(i).hyperCubes);
			new_clause.atoms.get(i).hyperCubes = predHyperCubes;
			new_clause.atoms.get(i).terms = new ArrayList<Term>(clause.atoms.get(i).terms.size());
			for (int j = 0; j < clause.atoms.get(i).terms.size(); j++) {
				new_clause.atoms.get(i).terms.add(newTerms.get(ind));
				ind++;
			}
		}
		//System.out.println("new_clause terms : " + new_clause.terms);
		new_clause.terms.clear();
		new_clause.terms.addAll(clause.terms);
		if(clause.terms.size() > 0){
			for(int atomId = 0 ; atomId < new_clause.atoms.size() ; atomId++){
				Atom new_atom = new_clause.atoms.get(atomId);
				Atom old_atom = clause.atoms.get(atomId);
				for(int termId = 0 ; termId < new_atom.terms.size() ; termId++){
					Term new_term = new_atom.terms.get(termId);
					Term old_term = old_atom.terms.get(termId);
					int old_term_index = clause.terms.indexOf(old_term);
					new_clause.terms.set(old_term_index, new_term);
				}
			}
		}
		for(Term term : clause.partiallyGroundedTerms){
			new_clause.partiallyGroundedTerms.add(new_clause.terms.get(clause.terms.indexOf(term)));
		}
		return new_clause;
	}

	private static ClauseRoot copyRoot(ClauseRoot root) {
		ClauseRoot newRoot = new ClauseRoot();
		for(int phId = 0 ; phId < root.hyperCubesList.size() ; phId++){
			ArrayList<HyperCube> hcList = new ArrayList<HyperCube>();
			for(HyperCube hc : root.hyperCubesList.get(phId)){
				hcList.add(hc);
			}
			newRoot.hyperCubesList.add(hcList);
		}
		newRoot.children.clear();
		for(Node child : root.children){
			Node newChild = copyChild(child);
			newRoot.children.add(newChild);
			newChild.rootParent = newRoot;
		}
		
		return newRoot;
	}

	private static Node copyChild(Node child) {
		Node newChild = new Node(LiftedPTP.maxPredId);
		for(int predId = 0 ; predId <= LiftedPTP.maxPredId ; predId++){
			ArrayList<ArrayList<Integer>> predPartialGroundings = child.partialGroundings.get(predId);
			if(predPartialGroundings.size() > 0){
				for(ArrayList<Integer>predPartialGrounding : predPartialGroundings){
					newChild.partialGroundings.get(predId).add(new ArrayList<Integer>(predPartialGrounding));
				}
			}
		}
		newChild.placeHolderList = (Set<Integer>)DeepCopyUtil.copy(child.placeHolderList);
		return newChild;
	}

	public static Atom create_new_atom(Atom atom) {
		List<Term> terms = new ArrayList<Term>();
		for(int i=0;i<atom.terms.size();i++)
		{
			Term newTerm = create_new_term(atom.terms.get(i));
			terms.add(newTerm);
		}
		//Atom newAtom = new Atom(atom.symbol,terms);
		Atom newAtom = new Atom(create_new_symbol(atom.symbol),terms);
		// copy hypercubes
		for(HyperCube hyperCube : atom.hyperCubes){
			newAtom.hyperCubes.add(new HyperCube(hyperCube));
		}
		return newAtom;
	}

	public static Term create_new_term(Term term) {
		int type = term.type;
		List<Integer> domain = new ArrayList<Integer>();
		for(int k=0;k<term.domain.size();k++)
			domain.add(term.domain.get(k));
		return new Term(type,domain);
	}
	public static PredicateSymbol create_new_symbol(PredicateSymbol symbol) {
		List<Integer> var_types = new ArrayList<Integer>();
		for(int i=0;i<symbol.variable_types.size();i++)
			var_types.add(symbol.variable_types.get(i));
		PredicateSymbol newSymbol = new PredicateSymbol(symbol.id,symbol.symbol,var_types,symbol.pweight,symbol.nweight);
		newSymbol.parentId = symbol.parentId;
		return newSymbol;
	}
	
	public void hcCountUpdate(){
		for(WClause clause : clauses){
			clause.hcCountUpdate();
		}
	}

	public int getNumGroundingsFromTuples(){
		HashMap<Integer,Set<ArrayList<Integer>>> predicateGroundingsHashMap = new HashMap<Integer,Set<ArrayList<Integer>>>();
		for(WClause clause : clauses){
			for(Atom atom : clause.atoms){
				for(ArrayList<Integer> tuple : clause.tuples){
					ArrayList<Integer> projection = new ArrayList<Integer>();
					for(Term t : atom.terms){
						projection.add(tuple.get(clause.terms.indexOf(t)));
					}
					if(!predicateGroundingsHashMap.containsKey(atom.symbol.id))
						predicateGroundingsHashMap.put(atom.symbol.id, new HashSet<ArrayList<Integer>>());
					predicateGroundingsHashMap.get(atom.symbol.id).add(projection);
					System.out.println(predicateGroundingsHashMap.get(atom.symbol.id));
				}
			}
		}
		int numGroundings = 0;
		for(Integer i : predicateGroundingsHashMap.keySet()){
			numGroundings += predicateGroundingsHashMap.get(i).size();
		}
		return numGroundings;
	}

	
	public static void toNormalForm(PredicateSymbol symbol,List<WClause> clauses) {


		List<WClause> new_clauses = new ArrayList<WClause>();
		List<WClause> symbol_clauses = new ArrayList<WClause>();
		List<Integer> symbol_loc = new ArrayList<Integer>();
		for (int i = 0; i < clauses.size(); i++) {
			boolean relevant = false;
			for (int j = 0; j < clauses.get(i).atoms.size(); j++) {
				if (clauses.get(i).atoms.get(j).symbol.id == symbol.id) 
				{
					symbol_clauses.add(create_new_clause(clauses.get(i)));
					symbol_loc.add(j);
					relevant = true;
					break;
				}
			}
			if (!relevant)
				new_clauses.add(create_new_clause(clauses.get(i)));
		}
		for (int i = 0; i < symbol.variable_types.size(); i++) {
			boolean changed = true;
			while (changed) {
				changed = false;
				for (int a = 0; a < symbol_clauses.size(); a++) {
					if (!symbol_clauses.get(a).valid()) continue;
					for (int b = a + 1; b < symbol_clauses.size(); b++) {
						if (!symbol_clauses.get(b).valid()) continue;
						int j = symbol_loc.get(a);
						int k = symbol_loc.get(b);

						Set<Integer> set1 = new HashSet<Integer>(symbol_clauses.get(a).atoms.get(j).terms.get(i).domain);
						Set<Integer> set2 = new HashSet<Integer>(symbol_clauses.get(b).atoms.get(k).terms.get(i).domain);

						// if the domains are disjoint
						Set<Integer> intersection = new HashSet<Integer>(set1);
						intersection.retainAll(set2);

						if (set1.equals(set2) || intersection.isEmpty()) {
							continue;
						} else {
							changed = true;
							WClause clause1 = create_new_clause(symbol_clauses.get(a));
							WClause clause2 = create_new_clause(symbol_clauses.get(b));

							Set<Integer> difference12 = new HashSet<Integer>(set1);
							difference12.removeAll(set2);
							clause1.atoms.get(j).terms.get(i).domain = new ArrayList<Integer>(difference12);

							Set<Integer> difference21 = new HashSet<Integer>(set2);
							difference21.removeAll(set1);
							clause2.atoms.get(k).terms.get(i).domain = new ArrayList<Integer>(difference21);

							// do set intersection
							set1.retainAll(set2);

							set2 = set1;
							symbol_clauses.add(clause1);
							symbol_loc.add(j);
							symbol_clauses.add(clause2);
							symbol_loc.add(k);
							break;
						}
					}
					if (changed)
						break;
				}
			}
		}
		for (int i = 0; i < symbol_clauses.size(); i++){
			if(symbol_clauses.get(i).valid())
				new_clauses.add(create_new_clause(symbol_clauses.get(i)));
		}

		//cleanup old clauses that are no longer used
		clauses.clear();
		clauses.addAll(new_clauses);
	}

	public static void copyAllClauses(List<WClause> origClauses, List<WClause> newClauses) {
		for(int i=0;i<origClauses.size();i++)
		{
			WClause newClause = create_new_clause(origClauses.get(i));
			newClauses.add(newClause);
		}
	}
	public static void print(List<WClause> clauses,String banner)
	{
		StringBuilder tmp = new StringBuilder("###########");
		tmp.append(banner);
		tmp.append("-START");
		tmp.append("###########");
		System.out.println(tmp);
		for(int i=0;i<clauses.size();i++)
			clauses.get(i).print();

		StringBuilder tmp1 = new StringBuilder("###########");
		tmp1.append(banner);
		tmp1.append("-END");
		tmp1.append("###########");
		System.out.println(tmp1);
	}

	public void setMaxPredicateId(int Id)
	{
		max_predicate_id = Math.max(max_predicate_id,Id);
	}

	public int getMaxPredicateId()
	{
		if(max_predicate_id == 0)
			setMaxPredicateId(symbols.size());
		return max_predicate_id;
	}

	//	void preprocessEvidence();
	public void putWeightsOnClauses() {

		for(int i=0;i<formulas.size();i++)
		{
			//cout<<mln.formulas.get(i).weight<<endl;
			//FOR NOW EACH FORMULA HAS EXACTLY ONE CLAUSE
			clauses.get(i).weight = formulas.get(i).weight;
		}
	}

	public void clearData()
	{
		symbols.clear();
		clauses.clear();
		formulas.clear();
		maxDegree = -1;
		max_predicate_id = 0;
	}

	public void setMLNPoperties()
	{
		//max degree of MLN
		int maxterms = 0;
		for(int i=0;i<symbols.size();i++)
		{
			if(symbols.get(i).variable_types.size() > maxterms)
				maxterms = symbols.get(i).variable_types.size();
		}
		maxDegree = maxterms;
	}

	public int getMaxDegree()
	{
		if(maxDegree == -1)
		{
			setMLNPoperties();
		}
		return maxDegree;
	}

	public MLN() {
		LiftedPTP.numMLNs++;
		max_predicate_id = (0);
		maxDegree = (-1);
	}

	/*
	 * This method takes an mln and evidence list as input, and modifies input mln into new normal mln.
	 */
	public void convertToNormalForm(MLN mln, ArrayList<Evidence> evid_list) {
		ArrayList<Integer> predIndices = new ArrayList<Integer>();
		/*
		for(PredicateSymbol symbol : mln.symbols){
			predIndices.add(0);
		}
		*/
		ArrayList<Atom> atoms_created = new ArrayList<Atom>();
		// Run through each evidence one by one and apply them to increasing set of clauses. 
		for(Evidence evidence : evid_list){
			ArrayList<WClause> allNewClauses = new ArrayList<WClause>();
			int sizeOrig = mln.clauses.size(); // Loop over all clauses currently we have
			for(int i = 0 ; i < sizeOrig ; i++)
			{
				WClause clause = mln.clauses.get(i);
				ArrayList<Integer> atomIndices = new ArrayList<Integer>(); // stores indices of atoms which are same as evidence atom
				// Find atomIndices by looping over all atoms and matching their parentId with evidence's parentId
				for(Atom atom : clause.atoms){
					if(atom.symbol.parentId == evidence.symbol.parentId){
						boolean toSplit = true;
						for(int j = 0 ; j < evidence.values.size() ; j++){
							if(!atom.terms.get(j).domain.contains(evidence.values.get(j))){
								toSplit = false;
							}
						}
						if(toSplit){
							atomIndices.add(clause.atoms.indexOf(atom));
						}
					}
				}
				//System.out.println("atomIndices : "+atomIndices);
				ArrayList<WClause> newClauses = new ArrayList<WClause>();
				newClauses.add(clause);
				applyEvidence(atomIndices, evidence, atoms_created, newClauses); // Fills in the list newClauses (which contains only one clause now, the original one) with splitted clauses according to evidence.
				allNewClauses.addAll(newClauses); // add new clauses generated into global list.
			}
			mln.clauses.clear();
			mln.clauses.addAll(allNewClauses);
		}
		
		print(mln.clauses, "new MLN printing...");
	}

	/*
	 * This method splits a clause according to evidence. It takes atomIndex as argument which tells which atom to break on. Also, termIndex tells
	 * which term to break on. Initially termIndex is always 0.
	 * It is a recursive function, which keeps calling itself with next termIndex until we reach end of terms.
	 * For example, initially termIndex is 0, so it breaks into 2 clauses according to that term lets say. Then for both of these new clauses, this
	 * function is again called with termIndex 1.
	 */
	private ArrayList<WClause> splitClauseAtTerm(WClause clause, int atomIndex, int termIndex, Evidence evidence){
		// Base case : If we reach at the end of terms, add this clause to final list and return final list.
		if(termIndex == clause.atoms.get(atomIndex).terms.size()){
			ArrayList<WClause>splittedClauses = new ArrayList<WClause>();
			splittedClauses.add(clause);
			return splittedClauses;
		}
		// If domain size of this term is 1, don't split on this term. Split on next term. 
		if(clause.atoms.get(atomIndex).terms.get(termIndex).domain.size() == 1){
			return splitClauseAtTerm(clause, atomIndex, termIndex+1, evidence);
		}
		// We will split this clause into two clauses
		WClause clause1 = create_new_clause(clause);
		WClause clause2 = create_new_clause(clause);
		// Get the variables which are involved in evidence. For ex : If evidence is F(1,2), and clause is !S(x) | !F(x,y) | S(y), and if atomIndex 
		// is 1 i.e. we are breaking on F, then get x and y variables, because we have to search them in whole clause and split the clause.
		List<Term> evidTerms = clause.atoms.get(atomIndex).terms;
		for(int i = 0 ; i < clause.atoms.size() ; i++){
			Atom atom = clause.atoms.get(i);
			//boolean changed = false;
			for(int  j = 0 ; j < atom.terms.size() ; j++){
				// If this variable is in evidence variables, we have to split
				if(atom.terms.get(j).equals(evidTerms.get(termIndex))){
						Atom atom1 = clause1.atoms.get(i);
						atom1.terms.get(j).domain.clear();
						atom1.terms.get(j).domain.add(evidence.values.get(termIndex)); // atom1's domain is evidence value itself 
						/*
						if(changed == false){
							if(already_created(atoms_created, atom1.symbol.parentId, atom1.terms.get(j).domain))
							atom1.symbol.symbol +=  "_" + predIndices.get(atom1.symbol.id);
							predIndices.set(atom1.symbol.id, predIndices.get(atom1.symbol.id)+1);
						}
						*/
						
						Atom atom2 = clause2.atoms.get(i);
						atom2.terms.get(j).domain.remove(evidence.values.get(termIndex)); // atom2's domain is original minus evidence's value
						/*
						if(changed == false){
							atom2.symbol.symbol +=  "_" + predIndices.get(atom2.symbol.id);
							predIndices.set(atom2.symbol.id, predIndices.get(atom2.symbol.id)+1);
						}
						*/
						
						//changed = true;
					}
				}
			}
		 ArrayList<WClause> splittedClauses = splitClauseAtTerm(clause1, atomIndex, termIndex+1, evidence); // recursively split clause1 on next term.
		 splittedClauses.add(clause2); // don't need to split clause2 as it doesn't contain evidence's value at one of the terms and hence can't contain exactly same evidence.
		 return splittedClauses;
	}
	/*
	 * This method applies an evidence onto a clause (contained in newClauses), and splits that clause accordingly, and modifies newClauses.
	 */
	private void applyEvidence(ArrayList<Integer> atomIndices, Evidence evidence, ArrayList<Atom> atoms_created,
			ArrayList<WClause> newClauses) {
		// Whole functionality divided into two parts : splitting clauses and then applying evidence (which can result into removal of atom from clause
		// whole clause itself. This following for loop does first part.
		// It loops over each atom position in clause which is same as evidence's symbol and then splits the clause.
		for(Integer i : atomIndices){
			ArrayList<WClause> splittedClauses = new ArrayList<WClause>();
			for(WClause clause : newClauses){
				//splitClause(clause,i,evidence,predIndices,atoms_created,splittedClauses);
				splittedClauses.addAll(splitClauseAtTerm(clause, i, 0, evidence)); // splitClauseAtTerm returns list of splitted clauses when splitted according atom position i. 0 as third argument in term index. It always starts from 0.
			}
			newClauses.clear();
			newClauses.addAll(splittedClauses);	
		}
		// This part applies evidence. Basically it loops over all clauses, and see if evidence is there or not. If it is not present anywhere,
		// then don't modify clause. If there is evidence, then see the sign. If the sign of evidence atom in clause matches with sign of evidence 
		// given, then that clause becomes true and hence remove that clause, otherwise, remove that particular atom as it becomes false.
		ArrayList<WClause> finalClauses = new ArrayList<WClause>();
		for(int i = 0 ; i < newClauses.size(); i++){
			boolean clauseToAdd = true;
			// Loop is from last to first because we are removing atoms inside the loop and hence this keeps indexing correct. 
			for(int j = newClauses.get(i).atoms.size() - 1 ; j >= 0 ; j--){
				// If atom's symbol is not evidence symbol, then this atom is not evidence and hence continue.
				if(newClauses.get(i).atoms.get(j).symbol.parentId != evidence.symbol.parentId){
					//finalClauses.add(newClauses.get(i)); //latest commented by happy : 03/09
					continue;
				}
				List<Term> termList = newClauses.get(i).atoms.get(j).terms;
				boolean evidenceFound = true;
				for(Term term : termList){
					// If term domain size > 1, then this can't be evidence and hence break from this loop
					if(term.domain.size() != 1){
						evidenceFound = false; //latest change by Happy : 03/09
						break;
					}
					// If term's domain is not same as evidence's value, then this is not evidence.
					if(term.domain.get(0) != evidence.values.get(termList.indexOf(term))){
						evidenceFound = false;
						break;
					}
				}
				if(evidenceFound){
					if(evidence.truthValue == newClauses.get(i).sign.get(j)){
						newClauses.get(i).removeAtom(j);
					}
					else{
						clauseToAdd = false;
						break; // added by happy 03/09 As soon as you find clause can't be added, break from atom loop
					}
				}
			}
			if(clauseToAdd){
				finalClauses.add(newClauses.get(i));
			}
		}
		newClauses.clear();
		newClauses.addAll(finalClauses);
	}
	
	/*

	private void splitClause(WClause origClause, Integer evidIndex, Evidence evidence, ArrayList<Integer> predIndices,
			ArrayList<Atom> atoms_created, ArrayList<WClause> newClauses) {
		ArrayList<WClause> allSplittedClauses = new ArrayList<WClause>();
		allSplittedClauses.add(origClause);
		List<Term> evidTerms = origClause.atoms.get(evidIndex).terms;
		ArrayList<WClause> splittedClauses = new ArrayList<WClause>(allSplittedClauses);
		for(int t = 0 ; t < evidTerms.size() ; t++){
			if(evidTerms.get(t).domain.size() == 1){
				splittedClauses.addAll(allSplittedClauses);
				continue;
			}
			splittedClauses.clear();
			for(int c = 0 ; c < allSplittedClauses.size() ; c++){
				WClause clause = allSplittedClauses.get(c);
				WClause clause1 = create_new_clause(clause);
				WClause clause2 = create_new_clause(clause);
				splittedClauses.add(clause1);
				splittedClauses.add(clause2);
				for(int i = 0 ; i < clause.atoms.size() ; i++){
					Atom atom = clause.atoms.get(i);
					//boolean changed = false;
					for(int  j = 0 ; j < atom.terms.size() ; j++){
						if(atom.terms.get(j).equals(evidTerms.get(t))){
								Atom atom1 = clause1.atoms.get(i);
								atom1.terms.get(j).domain.clear();
								atom1.terms.get(j).domain.add(evidence.values.get(t));
								
								Atom atom2 = clause2.atoms.get(i);
								atom2.terms.get(j).domain.remove(evidence.values.get(t));
								
								
								//changed = true;
							}
						}
					}
				}
				allSplittedClauses.clear();
				allSplittedClauses.addAll(splittedClauses);
			}
		 	newClauses.addAll(allSplittedClauses);
		}
		*/


	//===========================================
	// Added for MarginalMAP
	//===========================================

	public void printMLN(){

        for (int i = 0; i < this.clauses.size(); i++) {
            
        }
        print(this.clauses,"");
		printQuery();
	}

	public void printQuery(){
		for (int i = 0; i < this.symbols.size() ; i++) {
			System.out.print(this.symbols.get(i).symbol+"\t");
			if(this.symbols.get(i).queryType){
				System.out.println("MAP");
			}
			else {
				System.out.println("MAR");
			}
		}


	}

}
