package org.utd.cs.mln.lmap;

import java.util.ArrayList;
import java.util.HashMap;

import org.utd.cs.gm.utility.DeepCopyUtil;
import org.utd.cs.mln.alchemy.core.Atom;
import org.utd.cs.mln.alchemy.core.MLN;
import org.utd.cs.mln.alchemy.core.Term;
import org.utd.cs.mln.alchemy.core.WClause;
import org.utd.cs.mln.alchemy.util.DoublePair;

public class BaseSolver {

	public static void initProbBase(MLN mln, DoublePair p) {
		// Check for base condition : If every clauses is either satisfied or empty
		boolean baseCaseFound = true;
		for(WClause clause : mln.clauses)
		{
			/*
			if(clause.satisfied == false && clause.atoms.size() > 0){
				baseCaseFound = false;
				break;
			}
			*/
			if(clause.atoms.size() > 0){
				baseCaseFound = false;
				break;
			}
			
		}
		if(baseCaseFound == true){
			int numGroundings = mln.getNumGroundingsFromTuples();
			double totalWeightToReturn = 0.0;
			for(WClause clause : mln.clauses){
				if(clause.satisfied)
					totalWeightToReturn += clause.tuples.size()*clause.weight.getValue();
			}
			p.second = Math.pow(2, numGroundings)*Math.exp(totalWeightToReturn);
			return;
		}
		
		Atom atom = null;
		int clauseNumToSatisfy = 0;
		for(WClause clause : mln.clauses){
			if(clause.atoms.size() > 0){
				atom = clause.atoms.get(0);
				clauseNumToSatisfy = mln.clauses.indexOf(clause);
				break;
			}
		}
		int predId = atom.symbol.id;
		ArrayList<Integer> ground_atom = new ArrayList<Integer>();
		System.out.println("clause terms : " + mln.clauses.get(clauseNumToSatisfy).terms);
		for(Term term : atom.terms){
			int termId = mln.clauses.get(clauseNumToSatisfy).terms.indexOf(term);
			System.out.println("atom term : " + term);
			ground_atom.add(mln.clauses.get(clauseNumToSatisfy).tuples.get(0).get(termId));
		}
		System.out.println("predId = " + predId);
		System.out.println("groundAtom : " + ground_atom);
		ArrayList<MLN> resultMLNs = new ArrayList<MLN>();
		resultMLNs.add(new MLN());
		resultMLNs.add(new MLN());
		//double []satisfiedClauseWts = new double[]{0.0,0.0};
		for(WClause clause : mln.clauses){
			boolean predIdFound = false;
			ArrayList<ArrayList<Integer>> sameAtomTermIds = new ArrayList<ArrayList<Integer>>();
			ArrayList<Integer> sameAtomIndices = new ArrayList<Integer>();
			ArrayList<Integer> termFreqCount = new ArrayList<Integer>();
			for(int i = 0 ; i < clause.terms.size() ; i++){
				termFreqCount.add(0);
			}
			System.out.println("termFreqCount :" + termFreqCount);
			for(Atom a : clause.atoms){
				if(predId == a.symbol.id){
					predIdFound = true;
					sameAtomIndices.add(clause.atoms.indexOf(a));
					System.out.println("sameAtomIndices : " + sameAtomIndices);
					sameAtomTermIds.add(new ArrayList<Integer>());
				}
				for(Term term : a.terms){
					if(predId == a.symbol.id)
						sameAtomTermIds.get(sameAtomIndices.size()-1).add(clause.terms.indexOf(term));
					System.out.println("sameAtomTermIds : " + sameAtomTermIds);
					int clauseTermId = clause.terms.indexOf(term); 
					termFreqCount.set(clauseTermId, termFreqCount.get(clauseTermId)+1);
					System.out.println("termFreqCount : " + termFreqCount);
				}
			}
			ArrayList<ArrayList<WClause>> clauses = new ArrayList<ArrayList<WClause>>();
			clauses.add(new ArrayList<WClause>());
			clauses.add(new ArrayList<WClause>());
			ArrayList<ArrayList<HashMap<Term,Term>>> parentTermMap = new ArrayList<ArrayList<HashMap<Term,Term>>>();
			parentTermMap.add(new ArrayList<HashMap<Term,Term>>());
			parentTermMap.add(new ArrayList<HashMap<Term,Term>>());
			int numClauses = (int)Math.pow(2, sameAtomIndices.size());
			for(int i = 0 ; i < 2 ; i++){
				System.out.println("clauses in MLN " + i);
				for(int clauseId = 0 ; clauseId < numClauses ; clauseId++){
					clauses.get(i).add(MLN.create_new_clause(clause));
					HashMap<Term,Term> h = new HashMap<Term,Term>();
					for(int termId = 0 ; termId < clause.terms.size() ; termId++){
						h.put(clauses.get(i).get(clauseId).terms.get(termId), clause.terms.get(termId));
					}
					parentTermMap.get(i).add(h);
					System.out.println("clauseTermList : " + clauses.get(i).get(0).terms);
					clauses.get(i).get(clauseId).tuples.clear();
					clauses.get(i).get(clauseId).hyperCubes.clear();
					Integer tempTermFreqCount[] = termFreqCount.toArray(new Integer[termFreqCount.size()]);
					int temp = clauseId;
					int bitNum = sameAtomIndices.size()-1;
					while(temp != 0){
						if(temp%2 == 1){
							for(Integer termId : sameAtomTermIds.get(bitNum)){
								tempTermFreqCount[termId]--;
							}
							//System.out.println(clauses.get(i).get(clauseId).atoms);
							//System.out.println(sameAtomIndices.get(bitNum));
							//System.out.println(clauses.get(i).get(clauseId).atoms.size());
							clauses.get(i).get(clauseId).atoms.remove((int)sameAtomIndices.get(bitNum));
							if(i == 0){
								if(!clauses.get(i).get(clauseId).sign.get((int)sameAtomIndices.get(bitNum))){
									clauses.get(i).get(clauseId).satisfied = true;
								}
							}
							else{
								if(clauses.get(i).get(clauseId).sign.get((int)sameAtomIndices.get(bitNum))){
									clauses.get(i).get(clauseId).satisfied = true;
								}
							}
							
							clauses.get(i).get(clauseId).sign.remove((int)sameAtomIndices.get(bitNum));
							//System.out.println(clauses.get(i).get(clauseId).atoms.size());
							//System.out.println(clauses.get(i).get(clauseId).atoms);
						}
						temp/=2;
						bitNum--;
					}
					for(int termId = termFreqCount.size() - 1 ; termId >= 0 ; termId--){
						if(tempTermFreqCount[termId] == 0){
							clauses.get(i).get(clauseId).terms.remove(termId);
						}
					}
					clauses.get(i).get(clauseId).print();
				}
			}
			
			if(predIdFound == false){
				for(int i = 0 ; i < 2 ; i++){
					clauses.get(i).get(0).tuples = (ArrayList<ArrayList<Integer>>)DeepCopyUtil.copy(clause.tuples);
				}
			}
			else{
				for(ArrayList<Integer> tuple : clause.tuples){
					int finalClauseNum = 0;
					for(int i = sameAtomTermIds.size()-1 ; i >= 0 ; i--){
						boolean groundAtomFound = true;
						for(int j = 0 ; j < sameAtomTermIds.get(i).size() ; j++){
							int termId = sameAtomTermIds.get(i).get(j);
							if(!tuple.get(termId).equals(ground_atom.get(j))){
								groundAtomFound = false;
								break;
							}
						}
						finalClauseNum *= 2;
						if(groundAtomFound == true){
							finalClauseNum++;
						}
					}
		
					for(int i = 0 ; i < 2 ; i++){
						ArrayList<Integer> tempTuple = new ArrayList<Integer>();
						for(Term term : clauses.get(i).get(finalClauseNum).terms){
							int termIndex = clause.terms.indexOf(parentTermMap.get(i).get(finalClauseNum).get(term));
							tempTuple.add(tuple.get(termIndex));
						}
						//if(tempTuple.size() > 0)
						clauses.get(i).get(finalClauseNum).tuples.add(tempTuple);
					}
				}// end of for
			}
			
			for(int i = 0 ; i < 2 ; i++){
				for(WClause newClause : clauses.get(i)){
					if(newClause.tuples.size() > 0){
						resultMLNs.get(i).clauses.add(newClause);
					}
				}
			}
		}// end of clause
		for(int i = 0 ; i < 2 ; i++){
			System.out.println("MLN number : " + i);
			for(WClause clause : resultMLNs.get(i).clauses){
				clause.print();
				System.out.println(clause.tuples);
			}
		}
		boolean trueVal = (Math.random() <= 0.5);
		if(trueVal == true){
			initProbBase(resultMLNs.get(0),p);
			p.first = p.first + 1;
		}
		else{
			initProbBase(resultMLNs.get(1),p);
			p.first++;
		}
	}
	
	public static double initBase(MLN mln) {
		// Check for base condition : If every clauses is either satisfied or empty
		boolean baseCaseFound = true;
		for(WClause clause : mln.clauses)
		{
			if(clause.satisfied == false && clause.atoms.size() > 0){
				baseCaseFound = false;
				break;
			}
			/*if(clause.atoms.size() > 0){
				baseCaseFound = false;
				break;
			}*/
			
		}
		if(baseCaseFound == true){
			int numGroundings = mln.getNumGroundingsFromTuples();
			double totalWeightToReturn = 0.0;
			for(WClause clause : mln.clauses){
				clause.print();
				System.out.println(clause.tuples);
				if(clause.satisfied)
					totalWeightToReturn += clause.tuples.size()*clause.weight.getValue();
			}
			return Math.pow(2, numGroundings)*Math.exp(totalWeightToReturn);
		}
		Atom atom = null;
		int clauseNumToSatisfy = 0;
		for(WClause clause : mln.clauses){
			if(clause.atoms.size() > 0 && !clause.satisfied){
				atom = clause.atoms.get(0);
				clauseNumToSatisfy = mln.clauses.indexOf(clause);
				break;
			}
		}
		int predId = atom.symbol.id;
		ArrayList<Integer> ground_atom = new ArrayList<Integer>();
		System.out.println("clause terms : " + mln.clauses.get(clauseNumToSatisfy).terms);
		for(Term term : atom.terms){
			int termId = mln.clauses.get(clauseNumToSatisfy).terms.indexOf(term);
			System.out.println("atom term : " + term);
			ground_atom.add(mln.clauses.get(clauseNumToSatisfy).tuples.get(0).get(termId));
		}
		System.out.println("predId = " + predId);
		System.out.println("groundAtom : " + ground_atom);
		ArrayList<MLN> resultMLNs = new ArrayList<MLN>();
		resultMLNs.add(new MLN());
		resultMLNs.add(new MLN());
		//double []satisfiedClauseWts = new double[]{0.0,0.0};
		for(WClause clause : mln.clauses){
			boolean predIdFound = false;
			ArrayList<ArrayList<Integer>> sameAtomTermIds = new ArrayList<ArrayList<Integer>>();
			ArrayList<Integer> sameAtomIndices = new ArrayList<Integer>();
			ArrayList<Integer> termFreqCount = new ArrayList<Integer>();
			for(int i = 0 ; i < clause.terms.size() ; i++){
				termFreqCount.add(0);
			}
			System.out.println("termFreqCount :" + termFreqCount);
			for(Atom a : clause.atoms){
				if(predId == a.symbol.id){
					predIdFound = true;
					sameAtomIndices.add(clause.atoms.indexOf(a));
					System.out.println("sameAtomIndices : " + sameAtomIndices);
					sameAtomTermIds.add(new ArrayList<Integer>());
				}
				for(Term term : a.terms){
					if(predId == a.symbol.id)
						sameAtomTermIds.get(sameAtomIndices.size()-1).add(clause.terms.indexOf(term));
					System.out.println("sameAtomTermIds : " + sameAtomTermIds);
					int clauseTermId = clause.terms.indexOf(term); 
					termFreqCount.set(clauseTermId, termFreqCount.get(clauseTermId)+1);
					System.out.println("termFreqCount : " + termFreqCount);
				}
			}
			ArrayList<ArrayList<WClause>> clauses = new ArrayList<ArrayList<WClause>>();
			clauses.add(new ArrayList<WClause>());
			clauses.add(new ArrayList<WClause>());
			ArrayList<ArrayList<HashMap<Term,Term>>> parentTermMap = new ArrayList<ArrayList<HashMap<Term,Term>>>();
			parentTermMap.add(new ArrayList<HashMap<Term,Term>>());
			parentTermMap.add(new ArrayList<HashMap<Term,Term>>());
			int numClauses = (int)Math.pow(2, sameAtomIndices.size());
			for(int i = 0 ; i < 2 ; i++){
				System.out.println("clauses in MLN " + i);
				for(int clauseId = 0 ; clauseId < numClauses ; clauseId++){
					clauses.get(i).add(MLN.create_new_clause(clause));
					HashMap<Term,Term> h = new HashMap<Term,Term>();
					for(int termId = 0 ; termId < clause.terms.size() ; termId++){
						h.put(clauses.get(i).get(clauseId).terms.get(termId), clause.terms.get(termId));
					}
					parentTermMap.get(i).add(h);
					System.out.println("clauseTermList : " + clauses.get(i).get(0).terms);
					clauses.get(i).get(clauseId).tuples.clear();
					clauses.get(i).get(clauseId).hyperCubes.clear();
					Integer tempTermFreqCount[] = termFreqCount.toArray(new Integer[termFreqCount.size()]);
					int temp = clauseId;
					int bitNum = sameAtomIndices.size()-1;
					while(temp != 0){
						if(temp%2 == 1){
							for(Integer termId : sameAtomTermIds.get(bitNum)){
								tempTermFreqCount[termId]--;
							}
							//System.out.println(clauses.get(i).get(clauseId).atoms);
							//System.out.println(sameAtomIndices.get(bitNum));
							//System.out.println(clauses.get(i).get(clauseId).atoms.size());
							clauses.get(i).get(clauseId).atoms.remove((int)sameAtomIndices.get(bitNum));
							if(i == 0){
								if(!clauses.get(i).get(clauseId).sign.get((int)sameAtomIndices.get(bitNum))){
									clauses.get(i).get(clauseId).satisfied = true;
								}
							}
							else{
								if(clauses.get(i).get(clauseId).sign.get((int)sameAtomIndices.get(bitNum))){
									clauses.get(i).get(clauseId).satisfied = true;
								}
							}
							
							clauses.get(i).get(clauseId).sign.remove((int)sameAtomIndices.get(bitNum));
							//System.out.println(clauses.get(i).get(clauseId).atoms.size());
							//System.out.println(clauses.get(i).get(clauseId).atoms);
						}
						temp/=2;
						bitNum--;
					}
					for(int termId = termFreqCount.size() - 1 ; termId >= 0 ; termId--){
						if(tempTermFreqCount[termId] == 0){
							clauses.get(i).get(clauseId).terms.remove(termId);
						}
					}
					clauses.get(i).get(clauseId).print();
				}
			}
			
			if(predIdFound == false){
				for(int i = 0 ; i < 2 ; i++){
					clauses.get(i).get(0).tuples = (ArrayList<ArrayList<Integer>>)DeepCopyUtil.copy(clause.tuples);
				}
			}
			else{
				for(ArrayList<Integer> tuple : clause.tuples){
					int finalClauseNum = 0;
					for(int i = sameAtomTermIds.size()-1 ; i >= 0 ; i--){
						boolean groundAtomFound = true;
						for(int j = 0 ; j < sameAtomTermIds.get(i).size() ; j++){
							int termId = sameAtomTermIds.get(i).get(j);
							if(!tuple.get(termId).equals(ground_atom.get(j))){
								groundAtomFound = false;
								break;
							}
						}
						finalClauseNum *= 2;
						if(groundAtomFound == true){
							finalClauseNum++;
						}
					}
		
					for(int i = 0 ; i < 2 ; i++){
						ArrayList<Integer> tempTuple = new ArrayList<Integer>();
						for(Term term : clauses.get(i).get(finalClauseNum).terms){
							int termIndex = clause.terms.indexOf(parentTermMap.get(i).get(finalClauseNum).get(term));
							tempTuple.add(tuple.get(termIndex));
						}
						//if(tempTuple.size() > 0)
						clauses.get(i).get(finalClauseNum).tuples.add(tempTuple);
					}
				}// end of for
			}
			
			for(int i = 0 ; i < 2 ; i++){
				for(WClause newClause : clauses.get(i)){
					if(newClause.tuples.size() > 0){
						resultMLNs.get(i).clauses.add(newClause);
					}
				}
			}
		}// end of clause
		for(int i = 0 ; i < 2 ; i++){
			System.out.println("MLN number : " + i);
			for(WClause clause : resultMLNs.get(i).clauses){
				clause.print();
				System.out.println(clause.tuples);
			}
		}
		return initBase(resultMLNs.get(0)) + initBase(resultMLNs.get(1)); 
	}// end of function


	//public static void initBaseLifted
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
