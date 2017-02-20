package org.utd.cs.mln.lmap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.utd.cs.gm.utility.DeepCopyUtil;
import org.utd.cs.gm.utility.Pair;
import org.utd.cs.mln.alchemy.core.Atom;
import org.utd.cs.mln.alchemy.core.HyperCube;
import org.utd.cs.mln.alchemy.core.MLN;
import org.utd.cs.mln.alchemy.core.Term;
import org.utd.cs.mln.alchemy.core.WClause;
import org.utd.cs.mln.alchemy.util.comb;

public class LiftedSplit_new {

	// Task : Given an MLN, it splits (in lifted manner) an atom and returns Z
	// Input : MLN mln
	// Output : A Double number indicating Z of input MLN
	static int count = 0;
	public static double liftedSplit(MLN mln, boolean approximate){
		
		// see if decomposer can be applied after 1 partial grounding
		ArrayList<Set<Pair>> eqClasses = new ArrayList<Set<Pair>>();
		NonSameEquivConverter.findEquivalenceClasses(mln, eqClasses);
		ArrayList<Integer>sizeOfSegments = new ArrayList<Integer>();
		ArrayList<MLN> decomposedMLNs = zeroStepDecomposer(mln,eqClasses,approximate,sizeOfSegments);
		double totalZ = 1;
		if(decomposedMLNs != null && decomposedMLNs.size() > 0){
			System.out.println("zero step Decomposition Done...");
			///System.out.println("Printing decomposed MLNs : ");
			for(int mlnId = 0 ; mlnId < decomposedMLNs.size() ; mlnId++){
				//System.out.println("MLN no. : " + mlnId+1);
				MLN decomposedMln = decomposedMLNs.get(mlnId);
				/*//
				for(WClause clause : decomposedMln.clauses){
					clause.print();
					System.out.println("HyperCubes : ");
					for(HyperCube hc : clause.hyperCubes){
						System.out.println(hc);
					}
				}*///
				totalZ *= Math.pow(LiftedPTP.getPartition(decomposedMln, approximate),sizeOfSegments.get(mlnId));
			}
			return totalZ;
		}
		sizeOfSegments.clear();
		ArrayList<Set<Pair>> resultEqClasses = oneStepDecomposer(mln,eqClasses);
		if(resultEqClasses != null){
			System.out.println("1 step decomposition check passed, doing partial grounding...");
			applyPartialGrounding(mln, resultEqClasses.get(0));
			decomposedMLNs = Decomposer.ApplyDecomposer(mln, resultEqClasses.get(1), approximate, sizeOfSegments);
			if(decomposedMLNs != null && decomposedMLNs.size() > 0){
				System.out.println("1 step Decomposition Done...");
				///System.out.println("Printing decomposed MLNs : ");
				for(int mlnId = 0 ; mlnId < decomposedMLNs.size() ; mlnId++){
					//System.out.println("MLN no. : " + mlnId+1);
					MLN decomposedMln = decomposedMLNs.get(mlnId);
					/*//
					for(WClause clause : decomposedMln.clauses){
						clause.print();
						System.out.println("HyperCubes : ");
						for(HyperCube hc : clause.hyperCubes){
							System.out.println(hc);
						}
					}*///
					totalZ *= Math.pow(LiftedPTP.getPartition(decomposedMln, approximate),sizeOfSegments.get(mlnId));
				}
				return totalZ;
			}
			else{
				System.out.println("1 step decomposition not applicable, checking for binomials...");
				return applyRemainingLiftedSplit(mln, eqClasses, approximate);
			}
		}
		else{
			return applyRemainingLiftedSplit(mln, eqClasses, approximate);
		}
	}
	
	private static double applyRemainingLiftedSplit(MLN mln, ArrayList<Set<Pair>> eqClasses, boolean approximate){
		Pair predPosPair = zeroStepBinomial(mln,eqClasses);
		if(predPosPair != null){
			System.out.println("Doing zero step binomial...");
			return applyBinomial(mln,predPosPair,approximate);
		}
		else{
			ArrayList<Set<Pair>> resultBinomialEqClasses = oneStepBinomial(mln,eqClasses);
			if(resultBinomialEqClasses != null){
				System.out.println("Doing 1 step binomial...");
				applyPartialGrounding(mln, resultBinomialEqClasses.get(0));
				return applyBinomial(mln,resultBinomialEqClasses.get(1).iterator().next(), approximate);
			}
			else{
				System.out.println("No rule applicable, doing partial grounding...");
				applyPartialGrounding(mln, eqClasses.get(0));
				return liftedSplit(mln, approximate);
			}
		}
	}
	
	private static ArrayList<MLN> zeroStepDecomposer(MLN mln,
			ArrayList<Set<Pair>> eqClasses, boolean approximate, ArrayList<Integer>sizeOfSegments) { 
		ArrayList<ArrayList<Boolean>> validPredPosPairs = (ArrayList<ArrayList<Boolean>>)DeepCopyUtil.copy(mln.validPredPos);
		Decomposer d = new Decomposer();
		ArrayList<MLN> decomposedMLNs = null;
		for(Set<Pair> eqClass : eqClasses){
			if(d.isDecomposer(eqClass, mln)){
				decomposedMLNs = Decomposer.ApplyDecomposer(mln, eqClass, approximate, sizeOfSegments);
				if(decomposedMLNs != null && decomposedMLNs.size() > 0){
					return decomposedMLNs;
				}
			}
		}
		return null;	
	}


	private static double applyBinomial(MLN mln, Pair predPosPair, boolean approximate) {
		// HashMap -> key : predGrounding, value : Pair(clauseId,childIndex) 
		HashMap<ArrayList<Integer>, ArrayList<Pair>> predGroundToClauseMap= new HashMap<ArrayList<Integer>,ArrayList<Pair>>();
		HashMap<Integer,ArrayList<Integer>> clausesBinomialTermIndices = new HashMap<Integer,ArrayList<Integer>>(); // For each clauseId, it stores list of terms which are arguments of predicate predId
		HashMap<Integer,ArrayList<Boolean>> clausesBinomialPredSigns = new HashMap<Integer,ArrayList<Boolean>>(); // For each clauseId, it stores list of signs for each occurrence of predId. Sign = True means predId came with negative sign, else positive.
		for(WClause clause : mln.clauses){
			ArrayList<Integer> binomialClauseTermIndices = new ArrayList<Integer>();
			ArrayList<Boolean> binomialClausePredSigns = new ArrayList<Boolean>();
			int clauseId = mln.clauses.indexOf(clause);
			//ArrayList<Integer>predPosOrderIndices = new ArrayList<Integer>();
			boolean atomFound = false;
			/*ArrayList<Pair> predPosList = new ArrayList<Pair>();
			for(Integer termId : clause.root.groundedTermList){
				Term term = clause.terms.get(termId);
				for(Atom atom : clause.atoms){
					for(int termPos = 0 ; termPos < atom.terms.size() ; termPos++){
						if(atom.terms.get(termPos) == term){
							predPosList.add(new Pair(clause.atoms.indexOf(atom),termPos));
						}
					}
				}
			}*/
			for(Atom atom : clause.atoms){
				if(atom.symbol.id == predPosPair.first){
					/*Pair predPos = new Pair(atom.symbol.id,-1);
					for(int pos = 0 ; pos < atom.terms.size() ; pos++){
						if(pos != predPosPair.second){
							predPos.second = pos;
							predPosOrderIndices.add(predPosList.indexOf(predPos));
						}
					}*/
					binomialClauseTermIndices.add(clause.terms.indexOf(atom.terms.get(predPosPair.second))); // We need to get only first term because binomial atom is unary
					binomialClausePredSigns.add(clause.sign.get(clause.atoms.indexOf(atom)));
					atomFound = true;
				}
			}
			if(atomFound == true){
				for(int childId = 0 ; childId < clause.root.children.size() ; childId++){
					ArrayList<Integer> key = new ArrayList<Integer>();
					/*
					for(int i = 0 ; i < predPosOrderIndices.size() ; i++){
						key.add(clause.root.children.get(childId).segment.get((int)predPosOrderIndices.get(i)));
					}*/
					ArrayList<ArrayList<Integer>> predLeaf = clause.root.children.get(childId).partialGroundings.get(predPosPair.first); 
					int numOccur = predLeaf.size();
					int numPos = predLeaf.get(0).size();
					for(int predOccur = 0 ; predOccur < numOccur ; predOccur++){ 
						for(int predPos = 0 ; predPos < numPos ; predPos++){
							int constant = predLeaf.get(predOccur).get(predPos);
							if(constant != -1){
								key.add(constant);
							}
						}
						if(predGroundToClauseMap.containsKey(key)){
							predGroundToClauseMap.get(key).add(new Pair(clauseId,childId));
						}
						else{
							ArrayList<Pair> valueList = new ArrayList<Pair>();
							valueList.add(new Pair(clauseId,childId));
							predGroundToClauseMap.put(key,valueList);
						}
					}
					
				}
			}
			clausesBinomialTermIndices.put(clauseId, binomialClauseTermIndices);
			clausesBinomialPredSigns.put(clauseId, binomialClausePredSigns);
		}
		mln.hcCountUpdate();
		return binomialLevel1(mln, predPosPair, predGroundToClauseMap, new ArrayList<Pair>(), new ArrayList<HyperCube>(),-1, -1, clausesBinomialTermIndices, clausesBinomialPredSigns, approximate);
	}

	private static double binomialLevel1(MLN mln, Pair predPosPair,
			HashMap<ArrayList<Integer>, ArrayList<Pair>> predGroundToClauseMap, ArrayList<Pair> predGroundToClauses, ArrayList<HyperCube> disjointSegments, 
			int groundingIndex, int segmentIndex,
			HashMap<Integer, ArrayList<Integer>> clausesBinomialTermIndices,
			HashMap<Integer, ArrayList<Boolean>> clausesBinomialPredSigns, boolean approximate) {
		
		if(groundingIndex == predGroundToClauseMap.size() - 1 && segmentIndex == disjointSegments.size() - 1){
			// Cleaning to be done
			for(WClause clause : mln.clauses){
				int clauseId = mln.clauses.indexOf(clause);
				// Now remove terms.
				// TODO : There must be a better method to know which terms to remove
				ArrayList<Integer> clauseBinomialTermIds = clausesBinomialTermIndices.get(clauseId);
				Collections.sort(clauseBinomialTermIds,Collections.reverseOrder());
				for(Integer binomialTermId : clauseBinomialTermIds){
					boolean termToRemove = true;
					for(int atomId = 0 ; atomId < clause.atoms.size() ; atomId++){
						Atom atom = clause.atoms.get(atomId);
						///System.out.println(atom.symbol.id);
						if(atom.symbol.id != predPosPair.first){
							for(Term term : atom.terms){
								if(clause.terms.get(binomialTermId) == term){
									termToRemove = false;
									break;
								}
							}
						}
						if(termToRemove == false)
							break;
					}
					if(termToRemove == true){
						clause.terms.remove((int)binomialTermId);
						for(int i = 0 ; i < clause.root.hyperCubesList.size() ; i++){
							for(HyperCube hc : clause.root.hyperCubesList.get(i)){
								hc.num_copies *= hc.varConstants.get((int)binomialTermId).size();
								hc.varConstants.remove((int)binomialTermId);
							}
						}
					}
				}

				// Remove atoms with id = predId
				for(int atomId = clause.atoms.size()-1 ; atomId >= 0 ; atomId--){
					// If this atom is pred, then remove this atom
					if(clause.atoms.get(atomId).symbol.id == predPosPair.first){
						clause.atoms.remove(atomId);
						mln.validPredPos.get(predPosPair.first).set(predPosPair.second, false);
						clause.sign.remove(atomId);
					}
				}
			}// end of clause Loop
			return LiftedPTP.getPartition(mln, approximate);
		}
		else if(segmentIndex == disjointSegments.size() - 1){
			int groundingId = 0;
			Set<HyperCube> disjointHcSet = new HashSet<HyperCube>();
			ArrayList<HyperCube> segments = new ArrayList<HyperCube>();
			for(ArrayList<Integer> grounding: predGroundToClauseMap.keySet()){
				if(groundingId > groundingIndex)
				{
					predGroundToClauses = predGroundToClauseMap.get(grounding);
					for(Pair p : predGroundToClauseMap.get(grounding)){
						int clauseId = p.first;
						int childId = p.second;
						Set<Integer> placeHolderList = mln.clauses.get(clauseId).root.children.get(childId).placeHolderList;
						for(Integer place : placeHolderList){
							for(HyperCube hyperCube : mln.clauses.get(clauseId).root.hyperCubesList.get(place)){
								for(Integer binomialTermIndex : clausesBinomialTermIndices.get(clauseId)){
									HyperCube projectedHyperCube = new HyperCube(hyperCube.varConstants.get(binomialTermIndex));
									disjointHcSet.add(projectedHyperCube);
								}	
							}
						}
					}
					
					ArrayList<Set<Integer>> segmentClauseList = new ArrayList<Set<Integer>>();
					HashMap<HyperCube,Set<Integer>> hyperCubeClauseListHashMap = new HashMap<HyperCube,Set<Integer>>(); 
					for(HyperCube hyperCube : disjointHcSet){
						segments.add(hyperCube);
						segmentClauseList.add(new HashSet<Integer>());
						hyperCubeClauseListHashMap.put(hyperCube, new HashSet<Integer>());
					}
					Decomposer.createDisjointHyperCubes(segments, segmentClauseList, hyperCubeClauseListHashMap);
					segments.clear();
					for(HyperCube hc : hyperCubeClauseListHashMap.keySet()){
						segments.add(hc);
					}
					groundingIndex = groundingId;
					//hyperCubeClauseListHashMap.remove(hyperCube);
					break;
				}
				groundingId++;
			}
			return binomialLevel1(mln, predPosPair, predGroundToClauseMap, predGroundToClauses, segments, groundingIndex, -1, clausesBinomialTermIndices, clausesBinomialPredSigns, approximate);
		}
		else{
			HyperCube segment = disjointSegments.get(++segmentIndex);
			// Now when we apply binomial, we will get two things : binomialMLNs, which will be list of n+1 MLNs, where n is segment size, and binomialCoeff, which will be list of n+1 corresponding coefficients to be multiplied 
			ArrayList<MLN> binomialCasesMLNs = new ArrayList<MLN>();
			ArrayList<Double> binomialMLNCoeff = new ArrayList<Double>();
			double Z = 0.0;
			boolean oldApproximate = approximate;
			//approximate = false;
			if(approximate == true){
				// First generate random number and choose appropriate interval
				double rand_num = Math.random();
				int segmentSize = segment.varConstants.get(0).size();
				// Now create cumulative intervals of prob like [nc0/2^n, (nc0+nc1)/2^n,...(nc0+nc1+..ncn)/2^n]. We need not create all intervals, the
				// moment our cumulative sum exceeds rand_num, we can stop 
				double totalCases = Math.pow(2, segmentSize);
				double cumulativeSum = 0.0;
				int caseSelected = 0;
				for(int k = 0 ; k <= segmentSize ; k++){
					cumulativeSum += comb.findComb(segmentSize, k);
					if(cumulativeSum/totalCases > rand_num){
						caseSelected = k;
						break;
					}
				}
				binomialSplitterSegment(mln, predPosPair.first, segment, predGroundToClauses, clausesBinomialTermIndices, clausesBinomialPredSigns, binomialCasesMLNs, binomialMLNCoeff, oldApproximate, caseSelected);
				MLN binomialMln = binomialCasesMLNs.get(0);
				Z = binomialLevel1(binomialMln, predPosPair, predGroundToClauseMap, predGroundToClauses, disjointSegments, groundingIndex, segmentIndex, clausesBinomialTermIndices, clausesBinomialPredSigns, oldApproximate);
			}
			else{
				binomialSplitterSegment(mln, predPosPair.first, segment, predGroundToClauses, clausesBinomialTermIndices, clausesBinomialPredSigns, binomialCasesMLNs, binomialMLNCoeff, oldApproximate, 0);
				for(int mlnId = 0 ; mlnId < binomialCasesMLNs.size() ; mlnId++){
					MLN binomialMln = binomialCasesMLNs.get(mlnId);
					Z += binomialMLNCoeff.get(mlnId)*binomialLevel1(binomialMln, predPosPair, predGroundToClauseMap, predGroundToClauses, disjointSegments, groundingIndex, segmentIndex, clausesBinomialTermIndices, clausesBinomialPredSigns, oldApproximate);
				}
			}
			return Z;
		}
	}


	private static void applyPartialGrounding(MLN mln, Set<Pair> set) {
		for(Pair p : set){
			mln.validPredPos.get(p.first).set(p.second,false);
		}
		for(WClause clause : mln.clauses){
			HashMap<Integer,Integer> predOccurHashMap = new HashMap<Integer,Integer>();
			HashMap<Integer,ArrayList<ArrayList<Integer>>> termTo3DMap = new HashMap<Integer,ArrayList<ArrayList<Integer>>>();
			HashMap<Integer,Integer> predArityMap = new HashMap<Integer,Integer>();
			for(Atom atom : clause.atoms){
				if(!predArityMap.containsKey(atom.symbol.id)){
					predArityMap.put(atom.symbol.id, atom.terms.size());
				}
				boolean atomFound = false;
				for(Pair p : set){
					if(atom.symbol.id == p.first){
						if(atomFound == false)
						{
							if(predOccurHashMap.containsKey(p.first)){
								predOccurHashMap.put(p.first,predOccurHashMap.get(p.first)+1);
							}
							else{
								predOccurHashMap.put(p.first,0);
							}
						}
						atomFound = true;
						int pos = p.second;
						int termIndex = clause.terms.indexOf(atom.terms.get(pos));
						clause.partiallyGroundedTerms.add(clause.terms.get(termIndex));
						ArrayList<Integer> newEntry = new ArrayList<Integer>();
						newEntry.add(p.first);
						newEntry.add(predOccurHashMap.get(p.first));
						newEntry.add(pos);
						if(termTo3DMap.containsKey(termIndex)){
							termTo3DMap.get(termIndex).add(newEntry);
						}
						else{
							ArrayList<ArrayList<Integer>> newEntryList = new ArrayList<ArrayList<Integer>>();
							newEntryList.add(newEntry);
							termTo3DMap.put(termIndex, newEntryList);
						}
					}
				}
			}
			ArrayList<Integer> termIndices = new ArrayList<Integer>(termTo3DMap.keySet());
			HashMap<ArrayList<Set<Integer>>, ArrayList<Integer>> groundingToPlaceHolderMap = new HashMap<ArrayList<Set<Integer>>, ArrayList<Integer>>();
			ArrayList<Set<ArrayList<Set<Integer>>>> allGroundings = new ArrayList<Set<ArrayList<Set<Integer>>>>(); 
			for(int i = 0 ; i < clause.root.hyperCubesList.size() ; i++){
				allGroundings.add(new HashSet<ArrayList<Set<Integer>>>());
				if(clause.hcCount.get(i) == 0)
					continue;				
				ArrayList<Set<Integer>> grounding = new ArrayList<Set<Integer>>();
				for(Integer termIndex : termIndices){
					grounding.add(clause.root.hyperCubesList.get(i).get(0).varConstants.get((int)termIndex));
				}
				Set<Integer> toGroundTermIdsSet = new HashSet<Integer>();
				for(int p = 0 ; p < grounding.size() ; p++){
					toGroundTermIdsSet.add(p);
				}
				ArrayList<ArrayList<Set<Integer>>> groundings = cartesianProdVar(grounding, toGroundTermIdsSet);
				for(ArrayList<Set<Integer>> g : groundings){
					allGroundings.get(i).add(g);
				}
				for(ArrayList<Set<Integer>> g : groundings){
					if(groundingToPlaceHolderMap.containsKey(g)){
						groundingToPlaceHolderMap.get(g).add(i);
					}
					else{
						ArrayList<Integer> pHList = new ArrayList<Integer>();
						pHList.add(i);
						groundingToPlaceHolderMap.put(g,pHList);
					}
				}
			}// end of placeholder loop
			
			/* Following case will never come
			// If leaf doesn't exist
			if(clause.root.children.size() == 0){
				for(ArrayList<Set<Integer>> k : groundingToPlaceHolderMap.keySet()){
					Node n = new Node(LiftedPTP.maxPredId);
					n.rootParent = clause.root;
					n.placeHolderList = groundingToPlaceHolderMap.get(k);
					for(Integer predId : predOccurHashMap.keySet()){
						for(int i = 0 ; i <= predOccurHashMap.get(predId) ; i++){
							ArrayList<Integer> entry = new ArrayList<Integer>();
							for(int j = 0 ; j < predArityMap.get(predId) ; j++){
								entry.add(-1);
							}
							n.partialGroundings.get(predId).add(entry);
						}
					}
					for(int index = 0 ; index < termIndices.size() ; index++){
						int termId = termIndices.get(index);
						int constant = k.get(index).iterator().next();
						for(ArrayList<Integer> entry : termTo3DMap.get(termId)){
							n.partialGroundings.get(entry.get(0)).get(entry.get(1)).set(entry.get(2), constant);
						}
					}
					clause.root.children.add(n);
				}
			} // end of if
			*/
			// If leaves already exist
			//else{
				for(int leafId = clause.root.children.size() - 1 ; leafId >= 0 ; leafId--){
					Node leaf = clause.root.children.get(leafId);
					Set<Integer> leafPlaceHolderList = leaf.placeHolderList;
					Set<ArrayList<Set<Integer>>> groundingsSet = new HashSet<ArrayList<Set<Integer>>>();
					for(Integer ph : leafPlaceHolderList){
						groundingsSet.addAll(allGroundings.get((int)ph));
					}
					for(ArrayList<Set<Integer>> g : groundingsSet){
						Set<Integer> placeHolderSet = new HashSet<Integer>(groundingToPlaceHolderMap.get(g));
						placeHolderSet.retainAll(leafPlaceHolderList);
						Node n = new Node(LiftedPTP.maxPredId);
						n.rootParent = clause.root;
						n.placeHolderList = placeHolderSet;
						n.partialGroundings = (ArrayList<ArrayList<ArrayList<Integer>>>)DeepCopyUtil.copy(leaf.partialGroundings);
						for(int index = 0 ; index < termIndices.size() ; index++){
							int termId = termIndices.get(index);
							int constant = g.get(index).iterator().next();
							for(ArrayList<Integer> entry : termTo3DMap.get(termId)){
								n.partialGroundings.get(entry.get(0)).get(entry.get(1)).set(entry.get(2), constant);
							}
						}
						clause.root.children.add(n);
					}
					clause.root.children.remove(leafId);
				}
			//} // end of else
		} // end of clause loop
		System.out.println("partially grounding done...");
	}

	private static Pair zeroStepBinomial(MLN mln,
			ArrayList<Set<Pair>> eqClasses) { 
			for(int predId = 0 ; predId <= LiftedPTP.maxPredId ; predId++){
				int countTrue = 0;
				int truePos = 0; 
				for(int pos = 0 ; pos < mln.validPredPos.get(predId).size() ; pos++){
					if(mln.validPredPos.get(predId).get(pos) == true){
						countTrue++;
						truePos = pos;
					}
				}
				if(countTrue == 1){
					Pair resultPair = new Pair(predId,truePos);
					return resultPair;
				}
			}
		return null;	
		}

	/*
	private static void partialGround(MLN mln, Set<Pair>eqClass){
		for(WClause clause : mln.clauses){
			// Find variables of equivalence class
			for(Atom )
		}
	}
	*/
	private static ArrayList<Set<Pair>> oneStepBinomial(MLN mln,
			ArrayList<Set<Pair>> eqClasses) {
		ArrayList<Set<Pair>> resultEqClasses = new ArrayList<Set<Pair>>(); 
		ArrayList<ArrayList<Boolean>> validPredPosPairs = (ArrayList<ArrayList<Boolean>>)DeepCopyUtil.copy(mln.validPredPos);
		for(int eqId = eqClasses.size() - 1 ; eqId >= 0 ; eqId--){
			for(Pair p : eqClasses.get(eqId)){
				mln.validPredPos.get(p.first).set(p.second,false);
			}
			for(int predId = 0 ; predId <= mln.max_predicate_id ; predId++){
				int countTrue = 0;
				int truePos = 0; 
				for(int pos = 0 ; pos < mln.validPredPos.get(predId).size() ; pos++){
					if(mln.validPredPos.get(predId).get(pos) == true){
						countTrue++;
						truePos = pos;
					}
				}
				if(countTrue == 1){
					resultEqClasses.add(eqClasses.get(eqId));
					Set<Pair>resultSet = new HashSet<Pair>();
					resultSet.add(new Pair(predId,truePos));
					resultEqClasses.add(resultSet);
					return resultEqClasses;
				}
			}
			for(Pair p : eqClasses.get(eqId)){
				mln.validPredPos.get(p.first).set(p.second,true);
			}
		}
		return null;
	}

	private static ArrayList<Set<Pair>> oneStepDecomposer(MLN mln, ArrayList<Set<Pair>> eqClasses) {
		ArrayList<Set<Pair>> resultEqClasses = new ArrayList<Set<Pair>>(); 
		ArrayList<ArrayList<Boolean>> validPredPosPairs = (ArrayList<ArrayList<Boolean>>)DeepCopyUtil.copy(mln.validPredPos);
		Decomposer d = new Decomposer();
		for(int eqId = eqClasses.size() - 1 ; eqId >= 0 ; eqId--){
			for(Pair p : eqClasses.get(eqId)){
				mln.validPredPos.get(p.first).set(p.second,false);
			}
			ArrayList<Set<Pair>> tempEqClasses = new ArrayList<Set<Pair>>();
			NonSameEquivConverter.findEquivalenceClasses(mln, tempEqClasses);
			for(Set<Pair> eqClass : tempEqClasses){
				if(d.isDecomposer(eqClass, mln)){
					resultEqClasses.add(eqClasses.get(eqId));
					resultEqClasses.add(eqClass);
					eqClasses = tempEqClasses;
					return resultEqClasses;
				}
			}
			for(Pair p : eqClasses.get(eqId)){
				mln.validPredPos.get(p.first).set(p.second,true);
			}
		}
		return null;
	}

	
		// TODO : Handle segments which could get lost due to tautology of hyperCube
	/**
	 * Task : Apply binomial rule on an MLN wrt to singleton predicate with id = predId, and a segment.
	 * @param parentMln
	 * @param predId
	 * @param segment
	 * @param predGroundToClauses 
	 * @param keyClauseList
	 * @param clausesBinomialTermIndices
	 * @param clausesBinomialPredSigns
	 * @param binomialCasesMLNs
	 * @param binomialMLNCoeff
	 */
	private static void binomialSplitterSegment(MLN parentMln, int predId,
			HyperCube segment,
			ArrayList<Pair> predGroundToClauses, HashMap<Integer, ArrayList<Integer>> clausesBinomialTermIndices,
			HashMap<Integer, ArrayList<Boolean>> clausesBinomialPredSigns,
			ArrayList<MLN> binomialCasesMLNs, ArrayList<Double> binomialMLNCoeff, boolean approximate, int choice) {

		// First, update the hyperCubes of each clause such that they become disjoint (or identical) wrt to param segment
		/* Extract first element of segment, we need only first element to check if param segment is same as other 
		 * segment of pred, because this param segment belongs to basis of pred's segments
		 */
		int segmentFirstElement = segment.varConstants.get(0).iterator().next();
		// Extract the set from hyperCube param segment. We know size of this hyperCube is 1 because it is unary pred
		Set<Integer> segmentSet = new HashSet<Integer>(segment.varConstants.get(0));
		
		// Create disjoint or identical segments wrt to param segment of all occurrences of a binomial atom in all clauses
		// For any two intersecting segments A and B, create B-A and A
		int segmentSize = segmentSet.size();
		for(Pair clauseChildPair : predGroundToClauses){
			int clauseId = clauseChildPair.first;
			WClause clause = parentMln.clauses.get(clauseId);
			int childId = clauseChildPair.second;
			Set<Integer> placeHolders = clause.root.children.get(childId).placeHolderList;
			for(Integer termIndex : clausesBinomialTermIndices.get(clauseId)){
				for(Integer ph : placeHolders){	
					for(int hcId = 0 ; hcId < clause.hcCount.get((int)ph) ; hcId++){
						HyperCube hc = clause.root.hyperCubesList.get((int)ph).get(hcId);
						if(hc.varConstants.get(termIndex).contains(segmentFirstElement)){
							if(hc.varConstants.get(termIndex).size() != segmentSize){
								hc.varConstants.get(termIndex).removeAll(segmentSet); // B-A
								clause.root.hyperCubesList.get((int)ph).add(new HyperCube(clause.root.hyperCubesList.get((int)ph).get(hcId)));
								clause.root.hyperCubesList.get((int)ph).get(hcId).varConstants.set(termIndex, new HashSet<Integer>(segmentSet)); // A
							}
						}
					}
				}
			}
		}
		
		int endIndex = segmentSize;
		if(approximate == true){
			endIndex = choice;
		}
		for(int k = choice ; k <= endIndex ; k++){
			MLN newMln = new MLN(); // create an empty mln
			//double weightSatisfiedClauses = 0.0; // coeff to be multiplied
	
			// Go over all clauses. Clauses in which param segment doesn't appear are added as it is, and apply binomial on clauses in which param segment occurs
			for(int clauseId = 0 ; clauseId < parentMln.clauses.size() ; clauseId++){
				WClause newClause = MLN.create_new_clause(parentMln.clauses.get(clauseId));
				newMln.clauses.add(newClause);
			}
			newMln.validPredPos = (ArrayList<ArrayList<Boolean>>)DeepCopyUtil.copy(parentMln.validPredPos);
			binomialCasesMLNs.add(newMln);
			binomialMLNCoeff.add(comb.findComb(segmentSize, k));
		}
	
		for(Pair clauseChildPair : predGroundToClauses){
			int clauseId = clauseChildPair.first;
			// For current clause, extract indices of terms which are arguments of pred
			ArrayList<Integer> binomialTermIndices = new ArrayList<Integer>(clausesBinomialTermIndices.get(clauseId));
			// For current clause, extract signs of all occurrences of pred in this clause. Note that size pf
			// binomialTermIndices and binomialPredSigns is same because for each occurrence of pred in a clause
			// different term appears i.e. no S(x) V S(x) type is allowed
			ArrayList<Boolean> binomialPredSigns = new ArrayList<Boolean>(clausesBinomialPredSigns.get(clauseId));
			WClause clause = parentMln.clauses.get(clauseId);
			int childId = clauseChildPair.second;
			Set<Integer> placeHolders = clause.root.children.get(childId).placeHolderList;
			for(Integer ph : placeHolders){	
				for(int hcId = clause.hcCount.get((int)ph) - 1 ; hcId >= 0 ; hcId--){
					HyperCube hc = clause.root.hyperCubesList.get((int)ph).get(hcId);
					// For each occurrence of pred in clause, stores whether its term has same segment as param segment ?
					// So size(binomialTermIndices) = size(binomialPredSigns) = size(sameSegmentOccurs)					
					ArrayList<Boolean> sameSegmentOccurs = new ArrayList<Boolean>();
					// This stores the termIds which have same segments as param segment. This list will ease the calculation
					ArrayList<Integer> sameSegmentOccurTermList = new ArrayList<Integer>();
					// Now fill in sameSegmentOccurs
					for(Integer binomialTermIndex : binomialTermIndices){
						///System.out.println("hyperCubes : " + newClause.hyperCubes.get(hcId));
						if(hc.varConstants.get((int)binomialTermIndex).contains(segmentFirstElement)){
							//numSameSegments++;
							sameSegmentOccurs.add(true);
							sameSegmentOccurTermList.add(binomialTermIndex);
						}
						else{
							sameSegmentOccurs.add(false);
						}
					}
					for(int k = choice ; k <= endIndex ; k++){
						ArrayList<ArrayList<Set<Integer>>> sameSegmentsPartitions = new ArrayList<ArrayList<Set<Integer>>>();
						for(int i = 0 ; i < sameSegmentOccurs.size() ; i++){
							if(sameSegmentOccurs.get(i) == true){
								Set<Integer> falsePartiton = new HashSet<Integer>(segment.varConstants.get(0));
								Set<Integer> truePartiton = new HashSet<Integer>(segment.varConstants.get(0));
								if(binomialPredSigns.get(i) == false){
									for(int d = 0 ; d < k ; d++){
										falsePartiton.remove(falsePartiton.iterator().next());
									}
									truePartiton.removeAll(falsePartiton);
								}
								else{
									for(int d = 0 ; d < k ; d++){
										truePartiton.remove(truePartiton.iterator().next());
									}
									falsePartiton.removeAll(truePartiton);
								}
								ArrayList<Set<Integer>> partitions = new ArrayList<Set<Integer>>();
								partitions.add(falsePartiton);
								partitions.add(truePartiton);
								sameSegmentsPartitions.add(partitions);
							}
						}
						HyperCube curHyperCube = binomialCasesMLNs.get(k).clauses.get(clauseId).root.hyperCubesList.get((int)ph).get(hcId); // Extract current hyperCube
						ArrayList<HyperCube> newSatHyperCubes = new ArrayList<HyperCube>();
						int numHyperCubes = (int)Math.pow(2, sameSegmentOccurTermList.size());
						int numBits = sameSegmentOccurTermList.size();
						for(int c = 0 ; c < numHyperCubes ; c++){
							int temp = c;
							HyperCube h = new HyperCube(curHyperCube);
							int sameSegmentNumTuples = 1;
							for(int bitNum = 0 ; bitNum < numBits ; bitNum++){
								int termId = sameSegmentOccurTermList.get(bitNum);
								h.varConstants.set(termId, sameSegmentsPartitions.get(bitNum).get(temp%2));
								sameSegmentNumTuples *= sameSegmentsPartitions.get(bitNum).get(temp%2).size();
								temp = temp/2;
							}
							if(c > 0){
								h.satisfied = true;
							}
							if(sameSegmentNumTuples > 0){
								newSatHyperCubes.add(h);
							}
						} // end of c loop
						
						binomialCasesMLNs.get(k).clauses.get(clauseId).root.hyperCubesList.get((int)ph).remove(hcId);
						binomialCasesMLNs.get(k).clauses.get(clauseId).root.hyperCubesList.get((int)ph).addAll(newSatHyperCubes);
						///System.out.println("newClauseHyperCubes : " + newClause.hyperCubes);
					}
				}
			}
		}
		for(int k = choice ; k <= endIndex ; k++){
			binomialCasesMLNs.get(k).hcCountUpdate();
		}
		parentMln.clauses.clear();
		parentMln = null;
	}


	private static boolean isSingletonAtom(Atom atom) {
		if(atom.terms.size() == 1){
			return true;
		}
		return false;
	}

	private static Atom choosePredToSplitOn(MLN mln) {
		// Return first singleton pred found, otherwise return first pred found
		Atom atomOfSelectedPred = null;
		for(WClause clause : mln.clauses){
			for(Atom atom : clause.atoms){
				if(atomOfSelectedPred == null){
					atomOfSelectedPred = atom;
				}
				if(isSingletonAtom(atom) == true){
					atomOfSelectedPred = atom;
					return atomOfSelectedPred;
				}
			}
		}
		return atomOfSelectedPred;
	}

	public static ArrayList<ArrayList<Set<Integer>>> cartesianProdVar(ArrayList<Set<Integer>> sets, Set<Integer> toGroundTermIdsSet){
		ArrayList<ArrayList<Set<Integer>>> result = new ArrayList<ArrayList<Set<Integer>>>();
		int numTuples = 1;
		for(Integer i : toGroundTermIdsSet){
			numTuples *= sets.get(i).size();
		}
		
		// add numTuples lists into result
		for(int i = 0 ; i < numTuples ; i++){
			result.add(new ArrayList<Set<Integer>>());
		}
		
		int numRepeats = numTuples;
		// Now fill each element of input sets
		for(int i = 0 ; i < sets.size() ; i++){
			// If this index's elements are to be remained as it is, then just copy them numTuples times 
			if(!toGroundTermIdsSet.contains(i)){
				for(int j = 0 ; j < numTuples ; j++){
					result.get(j).add(new HashSet<Integer>(sets.get(i)));
				}
			}
			// else create the set of single integers
			else{
				numRepeats = numRepeats/sets.get(i).size();
				int numFilledEntries = 0;
				while(numFilledEntries != numTuples){
					for(Integer elem : sets.get(i)){
						for(int j = 0 ; j < numRepeats ; j++){
							result.get(numFilledEntries).add(new HashSet<Integer>(Arrays.asList(elem)));
							numFilledEntries++;
						}
					}
				}
			}
		}
		return result;
	}
	
	public static ArrayList<ArrayList<Set<Integer>>> cartesianProd(ArrayList<Set<Integer>> sets){
		ArrayList<ArrayList<Set<Integer>>> result = new ArrayList<ArrayList<Set<Integer>>>();
		int numTuples = 1;
		for(int i = 0 ; i < sets.size() ; i++){
			numTuples *= sets.get(i).size();
		}
		
		// add numTuples lists into result
		for(int i = 0 ; i < numTuples ; i++){
			result.add(new ArrayList<Set<Integer>>());
		}
		
		int numRepeats = numTuples;
		// Now fill each element of input sets
		for(int i = 0 ; i < sets.size() ; i++){
			numRepeats = numRepeats/sets.get(i).size();
			int numFilledEntries = 0;
			while(numFilledEntries != numTuples){
				for(Integer elem : sets.get(i)){
					for(int j = 0 ; j < numRepeats ; j++){
						result.get(numFilledEntries).add(new HashSet<Integer>(Arrays.asList(elem)));
						numFilledEntries++;
					}
				}
			}
		}
		return result;
	}
/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
