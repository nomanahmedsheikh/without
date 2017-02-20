package org.utd.cs.mln.lmap;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.utd.cs.gm.utility.DeepCopyUtil;
import org.utd.cs.gm.utility.Pair;
import org.utd.cs.mln.alchemy.core.Atom;
import org.utd.cs.mln.alchemy.core.Evidence;
import org.utd.cs.mln.alchemy.core.HyperCube;
import org.utd.cs.mln.alchemy.core.MLN;
import org.utd.cs.mln.alchemy.core.PredicateNotFound;
import org.utd.cs.mln.alchemy.core.PredicateSymbol;
import org.utd.cs.mln.alchemy.core.Term;
import org.utd.cs.mln.alchemy.core.WClause;
import org.utd.cs.mln.alchemy.util.DoublePair;
import org.utd.cs.mln.alchemy.util.Parser;
import org.utd.cs.mln.alchemy.util.comb;

public class Binomial {
	
	public static double initBinomial(MLN mln, int predId){
		ArrayList<HashMap<Integer,Integer>> clausesTermFreqCountList = new ArrayList<HashMap<Integer,Integer>>();
		ArrayList<MLN> binomialMLNs = new ArrayList<MLN>();
		/*MLN withoutBinomial = new MLN();
		for(WClause clause : mln.clauses){
			boolean binomialAtomFound = false;
			for(Atom atom : clause.atoms){
				if(atom.symbol.id == predId)
					binomialAtomFound = true;
			}
			if(binomialAtomFound == false){
				withoutBinomial.clauses.add(MLN.create_new_clause(clause));
				mln.clauses.remove(clause);
			}
		}
		if(withoutBinomial.clauses.size() > 0){
			binomialMLNs.add(withoutBinomial);
		}*/
		ArrayList<HyperCube> segments = new ArrayList<HyperCube>();
		ArrayList<Set<Integer>> segmentClauseList = new ArrayList<Set<Integer>>();
		HashMap<HyperCube,Set<Integer>> hyperCubeClauseListHashMap = new HashMap<HyperCube,Set<Integer>>();
		HashMap<Integer,ArrayList<Integer>> clausesBinomialTermIndices = new HashMap<Integer,ArrayList<Integer>>();
		HashMap<Integer,ArrayList<Boolean>> clausesBinomialPredSigns = new HashMap<Integer,ArrayList<Boolean>>();
		for(int clauseId = 0 ; clauseId < mln.clauses.size() ; clauseId++){
			WClause clause = mln.clauses.get(clauseId);
			System.out.println("Clause HyperCube : " + clause.hyperCubes);
			ArrayList<Integer> binomialClauseTermIndices = new ArrayList<Integer>();
			ArrayList<Boolean> binomialClausePredSigns = new ArrayList<Boolean>();
			HashMap<Integer,Integer> termFreqCount = new HashMap<Integer,Integer>();
			for(Atom atom : clause.atoms){
				for(Term term : atom.terms){
					if(termFreqCount.containsKey(clause.terms.indexOf(term))){
						termFreqCount.put(clause.terms.indexOf(term), termFreqCount.get(term)+1);
					}
					else{
						termFreqCount.put(clause.terms.indexOf(term), 1);
					}
				}
				if(predId == atom.symbol.id){
					binomialClauseTermIndices.add(clause.terms.indexOf(atom.terms.get(0)));
					binomialClausePredSigns.add(clause.sign.get(clause.atoms.indexOf(atom)));
				}
			}
			clausesTermFreqCountList.add(termFreqCount);
			clausesBinomialTermIndices.put(clauseId, binomialClauseTermIndices);
			clausesBinomialPredSigns.put(clauseId, binomialClausePredSigns);
			System.out.println("termFreqCount : " + termFreqCount.toString());
			System.out.println("binomialClauseTermIndices : " + binomialClauseTermIndices.toString());
			System.out.println("binomialClausePredSigns : " + binomialClausePredSigns.toString());
			if(binomialClauseTermIndices.size() == 0)
				continue;
			for(HyperCube hyperCube : clause.hyperCubes){
				for(Integer binomialTermIndex : binomialClauseTermIndices){
					HyperCube projectedHyperCube = new HyperCube(hyperCube.varConstants.get(binomialTermIndex));
					if(hyperCubeClauseListHashMap.containsKey(projectedHyperCube)){
						hyperCubeClauseListHashMap.get(projectedHyperCube).add(clauseId);
					}
					else{
						hyperCubeClauseListHashMap.put(projectedHyperCube, new HashSet<Integer>(Arrays.asList(clauseId)));
					}
				}
			}
		}
		System.out.println("hyperCubeClauseListHashMap : " + hyperCubeClauseListHashMap);
		for(HyperCube hyperCube : hyperCubeClauseListHashMap.keySet()){
			segments.add(hyperCube);
			segmentClauseList.add(hyperCubeClauseListHashMap.get(hyperCube));
		}
		//System.out.println("HyperCubes list : " + segments);
		//System.out.println("ClauseList : " + segmentClauseList);
		//System.out.println("Final hyperCubes size : " + segments.size());
		Decomposer.createDisjointHyperCubes(segments, segmentClauseList, hyperCubeClauseListHashMap);
		System.out.println("After creating disjoint segments");
		System.out.println("hyperCubeClauseListHashMap : " + hyperCubeClauseListHashMap);
		return binomial(mln, hyperCubeClauseListHashMap, clausesBinomialTermIndices, clausesBinomialPredSigns, clausesTermFreqCountList, predId);
	}
	/*
	public static double initBase(MLN mln){
		createGroundMLN(mln);
		return binomial(mln, hyperCubeClauseListHashMap, clausesBinomialTermIndices, clausesBinomialPredSigns, clauseTermFreqCountList, predId);
	}
	*/
	
	
	
	
	public static ArrayList<ArrayList<Integer>> cartesianProd(ArrayList<Set<Integer>> sets){
		ArrayList<ArrayList<Integer>> result = new ArrayList<ArrayList<Integer>>();
		int numTuples = 1;
		for(int i = 0 ; i < sets.size() ; i++){
			numTuples *= sets.get(i).size();
		}
		//Create arraylist of size numTuples and add first index's numbers
		int numRepeats = numTuples/sets.get(0).size();
		for(Integer i : sets.get(0)){
			for(int j = 0 ; j < numRepeats ; j++){
				result.add(new ArrayList<Integer>(Arrays.asList(i)));
			}
		}
		//System.out.println(result);
		// Now fill remaining tuples
		for(int i = 1 ; i < sets.size() ; i++){
			numRepeats = numRepeats/sets.get(i).size();
			int numFilledEntries = 0;
			while(numFilledEntries != numTuples){
				for(Integer elem : sets.get(i)){
					for(int j = 0 ; j < numRepeats ; j++){
						result.get(numFilledEntries).add(elem);
						numFilledEntries++;
					}
				}
				//System.out.println(result);
			}
		}
		return result;
	}
	public static void createGroundMLN(MLN mln){
		for(WClause clause : mln.clauses){
			for(HyperCube hc : clause.hyperCubes){
				clause.tuples.addAll(cartesianProd(hc.varConstants));
			}
			System.out.println("Clause : ");
			clause.print();
			System.out.println("HyperCubes : " + clause.hyperCubes);
			System.out.println("Tuples : " + clause.tuples);
		}
	}
	/*
	public ArrayList<MLN> initBinomialOld(MLN mln){
		int predPosition = -1;
		int predId = -1;
		outerloop:
		for(WClause clause : mln.clauses){
			for(Atom atom : clause.atoms){
				predPosition = atom.hasSingletonSegment(clause);
				if(predPosition != -1){
					predId = atom.symbol.id;
					break outerloop;
				}
			}
		}
		if(predPosition == -1){
			binomialMLNs.add(mln);
			return binomialMLNs;
		}
		MLN withoutBinomial = new MLN();
		for(WClause clause : mln.clauses){
			boolean binomialAtomFound = false;
			for(Atom atom : clause.atoms){
				if(atom.symbol.id == predId)
					binomialAtomFound = true;
			}
			if(binomialAtomFound == false){
				withoutBinomial.clauses.add(MLN.create_new_clause(clause));
				mln.clauses.remove(clause);
			}
		}
		if(withoutBinomial.clauses.size() > 0){
			binomialMLNs.add(withoutBinomial);
		}
		ArrayList<HyperCube> segments = new ArrayList<HyperCube>();
		ArrayList<Set<Integer>> segmentClauseList = new ArrayList<Set<Integer>>();
		HashMap<HyperCube,Set<Integer>> hyperCubeClauseListHashMap = new HashMap<HyperCube,Set<Integer>>();
		HashMap<Integer,Integer> clauseBinomialTermIndices = new HashMap<Integer,Integer>();
		for(int clauseId = 0 ; clauseId < mln.clauses.size() ; clauseId++){
			WClause clause = mln.clauses.get(clauseId);
			System.out.println("Clause HyperCube : " + clause.hyperCubes);
			int binomialTermIndex = -1;
			for(Atom atom : clause.atoms){
				if(predId == atom.symbol.id){
					binomialTermIndex = clause.terms.indexOf(atom.terms.get(predPosition));
					clauseBinomialTermIndices.put(clauseId, binomialTermIndex);
					break;
				}
			}
			for(HyperCube hyperCube : clause.hyperCubes){
				HyperCube projectedHyperCube = new HyperCube(hyperCube.varConstants.get(binomialTermIndex));
				if(hyperCubeClauseListHashMap.containsKey(projectedHyperCube)){
					hyperCubeClauseListHashMap.get(projectedHyperCube).add(clauseId);
				}
				else{
					hyperCubeClauseListHashMap.put(projectedHyperCube, new HashSet<Integer>(Arrays.asList(clauseId)));
				}
			}
		}
		for(HyperCube hyperCube : hyperCubeClauseListHashMap.keySet()){
			segments.add(hyperCube);
			segmentClauseList.add(hyperCubeClauseListHashMap.get(hyperCube));
		}
		System.out.println("HyperCubes list : " + segments);
		System.out.println("ClauseList : " + segmentClauseList);
		System.out.println("Final hyperCubes size : " + segments.size());
		Decomposer.createDisjointHyperCubes(segments, segmentClauseList, hyperCubeClauseListHashMap);
		return binomial(MLN mln, hyperCubeClauseListHashMap);
	}
	*/
	public static double binomial(MLN mln, HashMap<HyperCube,Set<Integer>> hyperCubeClauseListHashMap, HashMap<Integer, ArrayList<Integer>> clausesBinomialTermIndices, HashMap<Integer, ArrayList<Boolean>> clausesBinomialPredSigns, ArrayList<HashMap<Integer, Integer>> clausesTermFreqCountList, int predId){
		
		if(hyperCubeClauseListHashMap.isEmpty()){
			for(WClause clause : mln.clauses){
				for(int atomId = clause.atoms.size()-1 ; atomId >= 0 ; atomId--){
					if(clause.atoms.get(atomId).symbol.id == predId){
						clause.atoms.remove(atomId);
					}
				}
				ArrayList<Integer> removeTermIndices = new ArrayList<Integer>();
				if(clause.hyperCubes.size() > 0){
					HyperCube firstHyperCube = clause.hyperCubes.get(0);
					for(int termId =  firstHyperCube.varConstants.size() - 1; termId >= 0 ; termId--){
						if(firstHyperCube.varConstants.get(termId).isEmpty()){
							removeTermIndices.add(termId);
						}
					}
					for(Integer termId : removeTermIndices){
						clause.terms.remove(termId);
					}
					
					for(HyperCube hyperCube : clause.hyperCubes){
						for(Integer termId : removeTermIndices){
							hyperCube.varConstants.remove(termId);
							
						}
					}
				}
			}
			return LiftedPTP.getPartition(mln,false);
		}
		HyperCube segment = null;
		Set<Integer> keyClauseList = null;
		for(HyperCube hyperCube : hyperCubeClauseListHashMap.keySet()){
			segment = new HyperCube(hyperCube);
			keyClauseList = new HashSet<Integer>(hyperCubeClauseListHashMap.get(hyperCube));
			hyperCubeClauseListHashMap.remove(hyperCube);
			break;
		}
		System.out.println("Segment picked : " + segment);
		System.out.println("Its clause list  :" + keyClauseList);
		ArrayList<MLN> binomialCasesMLNs = new ArrayList<MLN>();
		ArrayList<Double> binomialMLNCoeff = new ArrayList<Double>();
		createDisjointClauseHyperCubesSegment(segment, mln, clausesBinomialTermIndices, clausesBinomialPredSigns, clausesTermFreqCountList, keyClauseList, predId, binomialCasesMLNs, binomialMLNCoeff);
		double sum = 0.0;
		for(int mlnId = 0 ; mlnId < binomialCasesMLNs.size() ; mlnId++){
			MLN binomialMln = binomialCasesMLNs.get(mlnId);
			sum += binomialMLNCoeff.get(mlnId)*binomial(binomialMln, hyperCubeClauseListHashMap, clausesBinomialTermIndices, clausesBinomialPredSigns, clausesTermFreqCountList, predId);
		}
		return sum;
	}
	
	// Task : For a given segment of a unary predicate, apply binomial rule
	/* Input : Note : predicate on which binomial is to be applied will be referred as pred
	 * segment : Segment on which binomial rule is to be applied
	 * parentmln : mln from which new n+1 mlns are formed where n is size of segment
	 * clausesBinomialTermIndices : For each clauseId, stores indices of terms which appear as arguments of pred
	 * clausesBinomialPredSigns : For each clauseId, stores the sign for each occurrence of pred in that clause.
	 * clausesTermFreqCountList : For each clauseId, stores frequency of each term (referred by termId) appearing in that clause
	 * keyClauseList : list of clauseIds in which this segment occurs
	 * predId : Id of pred
	 * binomialMlns : empty list of mlns which will be filled by n+1 mlns at the end
	 * binomialCoeff : empty list of coeff which are to be multiplied by each corresponding mln in binomialMlns 
	 */
	private static void createDisjointClauseHyperCubesSegment(HyperCube segment,
			MLN parentMln, HashMap<Integer, ArrayList<Integer>> clausesBinomialTermIndices,
			HashMap<Integer, ArrayList<Boolean>> clausesBinomialPredSigns, ArrayList<HashMap<Integer, Integer>> clausesTermFreqCountList, Set<Integer> keyClauseList, int predId, ArrayList<MLN> binomialCasesMLNs, ArrayList<Double> binomialMLNCoeff) {
		
		/* Extract first element of segment, we need only first element to check if param segment is same as other 
		 * segment of pred, because this param segment belongs to basis of pred's segments
		 */
		int segmentFirstElement = segment.varConstants.get(0).iterator().next();
		// Extract the set from hyperCube param segment. We know size of this hyperCube is 1 because it is unary pred
		Set<Integer> segmentSet = new HashSet<Integer>(segment.varConstants.get(0));
		// Creates disjoint or identical segments wrt to param segment of all occurrences of a binomial atom in all clauses
		// For any two intersecting segments A and B, create B-A and A
		for(Integer clauseId : keyClauseList){
			System.out.println("In clauseId : " + clauseId);
			WClause clause = parentMln.clauses.get(clauseId);
			for(Integer segmentIndex : clausesBinomialTermIndices.get(clauseId)){
				int origNumHyperCubes = clause.hyperCubes.size();
				System.out.println("In segment Index : " + segmentIndex);
				System.out.println("Num of hyperCubes : " + origNumHyperCubes);
				for(int hcId = 0 ; hcId < origNumHyperCubes ; hcId++){
					if(clause.hyperCubes.get(hcId).varConstants.get(segmentIndex).contains(segmentFirstElement)){
						clause.hyperCubes.get(hcId).varConstants.get(segmentIndex).removeAll(segmentSet); // B-A
						if(!clause.hyperCubes.get(hcId).isEmpty())
							clause.hyperCubes.add(new HyperCube(clause.hyperCubes.get(hcId)));
						clause.hyperCubes.get(hcId).varConstants.set(segmentIndex, new HashSet<Integer>(segmentSet)); // A
					}
				}
				System.out.println("Now clause HyperCubes are : " + clause.hyperCubes);
			}
		}
		int segmentSize = segmentSet.size();
		// Now loop over segment size (n) to create n+1 binomialMlns
		for(int k = 0 ; k <= segmentSize ; k++){
			MLN mln = new MLN(); // create an empty mln
			double weightSatisfiedClauses = 0.0; // coeff to be multiplied
	
			// Go over only those clauses in which this segment occurs, rest of the clauses will be added as it is
			for(Integer clauseId : keyClauseList){
				System.out.println("In clauseId : " + clauseId);
				// For current clause, extract indices of terms which are arguments of pred
				ArrayList<Integer> binomialTermIndices = new ArrayList<Integer>(clausesBinomialTermIndices.get(clauseId));
				// For current clause, extract signs of all occurrences of pred in this clause. Note that size pf
				// binomialTermIndices and binomialPredSigns is same because for each occurrence of pred in a clause
				// different term appears i.e. no S(x) V S(x) type is allowed
				ArrayList<Boolean> binomialPredSigns = new ArrayList<Boolean>(clausesBinomialPredSigns.get(clauseId));
				// For current clause, extract freq count of each term
				HashMap<Integer,Integer> termFreqCount = clausesTermFreqCountList.get(clauseId);
				long numClauseTrueGroundings = 0;
				
				// Create a new clause which is identical to this clause, but in this new clause, we will apply binomial
				WClause clause = MLN.create_new_clause(parentMln.clauses.get(clauseId));
				mln.clauses.add(clause);
				
				// Go over each hyperCube of this clause and apply binomial
				for(int hcId = clause.hyperCubes.size() - 1 ; hcId >= 0 ; hcId--){
					HyperCube hyperCube = clause.hyperCubes.get(hcId); // Extract curreny hyperCube
					System.out.println("HyperCube : " + hyperCube);
					
					// For each occurrence of pred in clause, stores whether its term has same segment as param segment ?
					// So size(binomialTermIndices) = size(binomialPredSigns) = size(sameSegmentOccurs					
					ArrayList<Boolean> sameSegmentOccurs = new ArrayList<Boolean>();
					// This stores the clauseTermIds which have same segments as param segment
					ArrayList<Integer> sameSegmentOccurTermList = new ArrayList<Integer>();
					//int numSameSegments = 0;
					// Now fill in sameSegmentOccurs
					for(Integer binomialTermIndex : binomialTermIndices){
						if(hyperCube.varConstants.get((int)binomialTermIndex).contains(segmentFirstElement)){
							//numSameSegments++;
							sameSegmentOccurs.add(true);
							sameSegmentOccurTermList.add(binomialTermIndex);
						}
						else{
							sameSegmentOccurs.add(false);
						}
					}
					System.out.println("binomialTermIndices : " + binomialTermIndices.toString());
					System.out.println("SameSegmentOccurs : " + sameSegmentOccurs.toString());

					boolean toRemove = false; // Whether to remove this hyperCube ? This will only be removed when it is a 
					// tautology. Cases of tautology : 
					// TODO : (1) If any two binomial preds of opposite signs have samesegments true 
					// (2) if we are in case nC0 i.e. all false, and if any occurrence of pred with samesegment true
					// has negation sign
					// (3) Similar to case 2, but case is nCn and sign should be positive.
					

					// Number of groundings which has to be raised to 2 to count num of true assignments
					long numRedundantGroundings = 0; 
					Set<Integer> disjointSegmentSet = new HashSet<Integer>();
					int domainReduceAmount = segmentSize-k;
					
					// Now the counting of number of true groundings begins, some cases here : 
					// (1) : TODO : If toRemove is true, then this hyperCube is tautology and thus count all ground atoms
					// (2) : If toRemove is false, then
					// (2.1) : For preds pred, if their samesegment is true, then 
 
					for(int termId = 0 ; termId < binomialTermIndices.size() ; termId++){
						// Doubt here
						if(k == 0 && sameSegmentOccurs.get(termId) == true && binomialPredSigns.get(termId) == true){
							toRemove = true;
						}
						if(k == segmentSize && sameSegmentOccurs.get(termId) == true && binomialPredSigns.get(termId) == false){
							toRemove = true;
						}
						int clauseTermId = binomialTermIndices.get(termId);
						System.out.println("clauseTermId : " + clauseTermId);
						// If this term has same segment as input segment, only then remove this atom
						if(sameSegmentOccurs.get(termId) == true){
							System.out.println("This term has same segment");
							System.out.println("termFreqCount : " + termFreqCount);
							System.out.println("clause Terms : " + clause.terms);
							sameSegmentOccurTermList.add(clauseTermId);
							// If this atom comes with neg sign, then its no. of true groundings will be segment size - k 
							if(binomialPredSigns.get(termId) == false){
									domainReduceAmount = k;
							}
							/*
							// reduce domain by domainReduceAmount
							for(int i = 0 ; i < domainReduceAmount ; i++){
									hyperCube.varConstants.get(clauseTermId).remove(hyperCube.varConstants.get(clauseTermId).iterator().next());
								}
							
							// If this term doesn't appear anywhere else, then remove empty this term
							else{
								hyperCube.varConstants.get(clauseTermId).clear();
							}
							// If this atom is positive, then its no. of false groundings will be segment size - k, else k
							if(binomialPredSigns.get(termId) == false){
								numRedundantGroundings += segmentSize - k;
							}
							else{
								numRedundantGroundings += k;
							}*/
						}
						// Make set of segments disjoint from source segment
						else{
							disjointSegmentSet.addAll(hyperCube.varConstants.get(clauseTermId));
						}
						System.out.println("Now hyperCube becomes : " + hyperCube);
					} // end of term loop
					numRedundantGroundings += disjointSegmentSet.size();
					for(Atom atom : clause.atoms){
						if(atom.symbol.id != predId){
							int atomNumRedundantGroundings = 1;
							for(Term term : atom.terms){
								if(sameSegmentOccurTermList.contains(clause.terms.indexOf(term))){
									atomNumRedundantGroundings *= domainReduceAmount;
									for(int i = 0 ; i < domainReduceAmount ; i++){
										hyperCube.varConstants.get(clause.terms.indexOf(term)).remove(hyperCube.varConstants.get(clause.terms.indexOf(term)).iterator().next());
									}
								}
								else{
									atomNumRedundantGroundings *= hyperCube.varConstants.get(clause.terms.indexOf(term)).size();
								}
								// reduce domain by domainReduceAmount
								
							}
							numRedundantGroundings += atomNumRedundantGroundings;
						}
					}
					for(int termId = 0 ; termId < binomialTermIndices.size() ; termId++){
						if(sameSegmentOccurs.get(termId) == true){
							int clauseTermId = binomialTermIndices.get(termId);
							if(termFreqCount.get(clauseTermId) == 1){
								hyperCube.varConstants.get(clauseTermId).clear();
							}
						}
					}
					/*
					// Get no. of true groundings : total no. of groundings - false no. of groundings
					long numHyperCubeTrueGroundings = (long)Math.pow(segmentSize, numSameSegments) - numRedundantGroundings;
					// Get groundings of remaining terms which either don't have same segment, or of different predicate
					for(int clauseTermId = 0 ; clauseTermId < clause.terms.size() ; clauseTermId++){
						if(binomialTermIndices.contains(clauseTermId) == false){
							numHyperCubeTrueGroundings *= hyperCube.varConstants.get(clauseTermId).size();
						}
						else{
							if(sameSegmentOccurs.get(binomialTermIndices.indexOf(clauseTermId)) == false){
								numHyperCubeTrueGroundings *= hyperCube.varConstants.get(clauseTermId).size();
							}
						}
					}
					numClauseTrueGroundings += numHyperCubeTrueGroundings;
					*/
					if(toRemove == true){
						clause.hyperCubes.remove(hcId);
					}
				} // HyperCube loop ends here
				weightSatisfiedClauses += clause.weight.getValue() * numClauseTrueGroundings;
			}// clauses loop ends here
			binomialMLNCoeff.add(comb.findComb(segmentSize, k)*Math.exp(weightSatisfiedClauses));
			binomialCasesMLNs.add(mln);
		}	
	}
	/**
	 * @param args
	 * @throws FileNotFoundException 
	 * @throws PredicateNotFound 
	 */
	public static void main(String[] args) throws FileNotFoundException, PredicateNotFound {
		// TODO Auto-generated method stub
		MLN mln = new MLN();
		Parser parser = new Parser(mln);
		String filename = new String("smoke/smoke_mln.txt");
		//String filename = new String("entity_resolution/er-bnct-eclipse.mln");
		parser.parseInputMLNFile(filename);

		ArrayList<Evidence> evidList = parser.parseInputEvidenceFile("smoke/evidence.txt");
		//ArrayList<Evidence> evidList = parser.parseInputEvidenceFile("entity_resolution/er-test-eclipse.db");
		MlnToHyperCube mlnToHyperCube = new MlnToHyperCube();
		HashMap<PredicateSymbol,ArrayList<ArrayList<HyperCube>>> predsHyperCubeHashMap = mlnToHyperCube.createPredsHyperCube(evidList,mln);
	
		for(PredicateSymbol predSymbol : predsHyperCubeHashMap.keySet()){
			System.out.println(predSymbol.toString()+predsHyperCubeHashMap.get(predSymbol).get(0).toString()+" "+predsHyperCubeHashMap.get(predSymbol).get(1).toString() + " "+predsHyperCubeHashMap.get(predSymbol).get(2).toString());
		}
		int origNumClauses = mln.clauses.size();
		for(int clauseId = 0 ; clauseId < origNumClauses ; clauseId++){
			mln.clauses.addAll(mlnToHyperCube.createClauseHyperCube(mln.clauses.get(clauseId), predsHyperCubeHashMap,false));
		}
		for(int clauseId = origNumClauses-1 ; clauseId >= 0 ; clauseId--){
			mln.clauses.remove(clauseId);
		}
		createGroundMLN(mln);
		long numGroundAtoms = mln.getNumGroundingsFromTuples();
		System.out.println("Total num of Ground atoms : "+numGroundAtoms);
		//initBinomial(mln, 0);
		//System.out.println(initBase(mln));
		
		
		int numRuns = 10;
		Double depthsRuns[] = new Double[numRuns];
		Double ZRuns[] = new Double[numRuns];
		// Method 1
		double sumZ1 = 0.0;
		// Method 2
		double sumZ2 = 0.0;
		int iter = 0;
		int iter2 = 0;
		for(iter = 0 ; iter < numRuns ; iter++){
			DoublePair p = new DoublePair(0.0,0.0);
			BaseSolver.initProbBase(mln,p);
			depthsRuns[iter] = p.first;
			ZRuns[iter] = p.second;
			sumZ1 += p.second;
			double numSymmetricAssignments = Math.pow(2, numGroundAtoms - p.first);
			sumZ2 += numSymmetricAssignments*(p.second);
			iter2 += numSymmetricAssignments;
		}
		double totalInvQ = Math.pow(2, numGroundAtoms);
		System.out.println("Simple sample result Z : " + totalInvQ*(sumZ1/iter));
		System.out.println("weighed sample result Z : " + totalInvQ*(sumZ2/iter2));
	}
	
}
