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
import org.utd.cs.mln.alchemy.core.HyperCube;
import org.utd.cs.mln.alchemy.core.MLN;
import org.utd.cs.mln.alchemy.core.Term;
import org.utd.cs.mln.alchemy.core.WClause;
import org.utd.cs.mln.alchemy.util.Parser;
public class Decomposer {

	public static boolean hasCommonAtom(WClause clause1, WClause clause2){
		for(Atom atom1 : clause1.atoms){
			for(Atom atom2 : clause2.atoms){
				if(atom1.symbol.id == atom2.symbol.id)
					return true;
			}
		}
		return false;
	}
	
	public static void createDisjointMLNs(MLN mln, ArrayList<MLN> disjointMlns){
		int numClauses = mln.clauses.size();
		boolean adj_matrix[][] = new boolean[numClauses][numClauses];
		for(int i = 0 ; i < numClauses ; i++){
			for(int j = i ; j < numClauses ; j++){
				if(hasCommonAtom(mln.clauses.get(i),mln.clauses.get(j))){
					adj_matrix[i][j] = adj_matrix[j][i] = true;
				}
				else{
					adj_matrix[i][j] = adj_matrix[j][i] = false;
				}
			}
		}
		
		boolean final_adj_matrix[][] = new boolean[numClauses][numClauses];
		boolean vertexSeen[] = new boolean[numClauses];
		for(int i = 0 ; i < numClauses ; i++)
		{
			if(!vertexSeen[i])
			{
				boolean mark[] = NonSameEquivConverter.BFS(adj_matrix,numClauses,i);
				mark[i] = true;
				final_adj_matrix[i] = mark.clone();
				for(int j = i+1 ; j < numClauses ; j++)
				{
					if(mark[j] == true)
					{
						vertexSeen[j] = true;
						final_adj_matrix[j] = mark.clone();
					}
				}
			}
		}

		vertexSeen = new boolean[numClauses];
		for(int i = 0 ; i < numClauses ; i++){
			if(!vertexSeen[i]){
				MLN newMln = new MLN();
				for(int j = 0 ; j < numClauses ; j++){
					if(final_adj_matrix[i][j] == true){
						vertexSeen[j] = true;
						WClause newClause = MLN.create_new_clause(mln.clauses.get(j));
						newMln.clauses.add(newClause);
					}
				}
				disjointMlns.add(newMln);
			}
		}
	}
	
	public void findEquivalenceClasses(MLN mln, ArrayList<Set<Pair>> predEquivalenceClass){
		NonSameEquivConverter.findEquivalenceClasses(mln, predEquivalenceClass);
		///System.out.println("Equivalence classes are : ");
		///System.out.println(predEquivalenceClass);
	}
	
	// Given an equivalence class and MLN, checks if equivalence class is decomposer or not. An equivalence class is a decomposer if
	// (1) If a variable of an equivalence class appears in a clause, then it should occur in all atoms of that clause,
	// (2) No two distinct variables of same equivalence class can appear in same clause. (This also takes care of another condition of decomposer that
	// if a variable of decomposer appears in a predicate R at position i, then it should always come at ith position of predicate R)
	// Also, this function also checks if no variable of equivalence class occurs in a clause, then also it can be a potential decomposer (although
	// this check is not required because we always pass MLN in which all clauses have some predicate common
	
	public boolean isDecomposer(Set<Pair> equiClass, MLN mln){
		System.out.println("checking for zerostep decomposer...");
		for(WClause clause : mln.clauses){
			if(clause.atoms.size() == 0)
				continue;
			Term firstTerm = null; // Stores firstTerm found of this equivalence (required to check against any other term found of this class in same clause
			int atomId = 0;
			// we return true for a clause if either all atoms contain equivalence class variable or no atom contains eq class variable.
			// andAccumulator stores if all contain eq class
			// orAccumulator stores if none contains eq class
			// If they are different at the end of clause, then it is not a decomposer
			boolean andAccumulator = true, orAccumulator = false; 
			
			for(atomId = 0 ; atomId < clause.atoms.size() ; atomId++){
				Atom atom = clause.atoms.get(atomId);
				boolean equiClassFound = false; // stores if eq class variable appears in this atom or not
				for(int termId = 0 ; termId < atom.terms.size() ; termId++){
					Pair pair = new Pair(atom.symbol.id,termId); // creates a <pred,pos> pair for this term
					// If this pair is in eq class, then check : (1) If it is the first time we encountered eq class variable in this clause, then set firstTerm to this term
					// (2) If we have already found first term, then check if this term is same as first term, if it is, then good, else return false
					if(equiClass.contains(pair)){
						if(firstTerm == null){
							firstTerm = atom.terms.get(termId);
							equiClassFound = true;
						}
						else if(atom.terms.get(termId) != firstTerm){
							return false;
						}
						else{
							equiClassFound = true;
						}
					}
				}
				andAccumulator &= equiClassFound;
				orAccumulator |= equiClassFound;
			}
			if(andAccumulator != orAccumulator){
				return false;
			}
		}
		return true;
	}
	
	// Creates disjoint segments of two intersecting hyperCubes of variable count1 (thats why it creates segments). 
	// Input : h1Id, h2Id : hyperCube indices of both hypeCubes to create disjoint segments from.
	// hyperCubesIntersectionList : For each hypercube index of a hypercube in array hypercubes, it stores list of indices of hypercubes with which this hyperCube intersects 
	// hyperCubes : List of hyperCubes
	// hyperCubeClauseList : For each hypercube index of a hypercube in array hypercubes, it stores a list of clause indices in which this hyperCube is present
	public static void createDisjointSegments(int h1Id, int h2Id, ArrayList<ArrayList<Integer>> hyperCubesIntersectionList, ArrayList<HyperCube>hyperCubes, ArrayList<Set<Integer>> hyperCubeClauseList){
		HyperCube h1 = hyperCubes.get(h1Id);
		HyperCube h2 = hyperCubes.get(h2Id);
		//System.out.println("Inside createDisjointHyperCubesVar function with input hyperCubes : "+h1.toString()+" and "+h2.toString());
		// create 3 sets : intSectHc = h1^h2, h1-intSectHc, h2-intSectHc, all on variable varId
		Set<Integer>hc1VarConstants = new HashSet<Integer>(h1.varConstants.get(0));
		Set<Integer>hc2VarConstants = new HashSet<Integer>(h2.varConstants.get(0));
		
		Set<Integer>intSectVarConstants = new HashSet<Integer>(hc1VarConstants);
		intSectVarConstants.retainAll(hc2VarConstants);
		
		Set<Integer>onlyHc1VarConstants = new HashSet<Integer>(hc1VarConstants);
		onlyHc1VarConstants.removeAll(intSectVarConstants);
		
		Set<Integer>onlyHc2VarConstants = new HashSet<Integer>(hc2VarConstants);
		onlyHc2VarConstants.removeAll(intSectVarConstants);
		
		//ArrayList<HyperCube>mergedIntSectHyperCubes = new ArrayList<HyperCube>();
		HyperCube onlyHc1HyperCube = null, onlyHc2HyperCube = null;
		//System.out.println("Now creating onlyh1 hyperCube");
		if(onlyHc1VarConstants.size() > 0){
			onlyHc1HyperCube = new HyperCube(h1);
			onlyHc1HyperCube.setVarConstants(onlyHc1VarConstants, 0);
			hyperCubes.add(onlyHc1HyperCube); // Add this new hyperCube into hyperCubes List
			ArrayList<Integer>onlyHc1HyperCubesList = new ArrayList<Integer>(); // Intersection list of this new hyperCube, we will fill this now
			Set<Integer>onlyHc1ClauseList = new HashSet<Integer>(hyperCubeClauseList.get(h1Id)); // Clause list of this new hypercube, it will be same as h1Id's clause list
			
			// Fill intersection list of new hypercube and also update rest's lists with index of this new hyperCube
			for(Integer hcId : hyperCubesIntersectionList.get(h1Id)){
				if(!onlyHc1HyperCube.areDisjointOrIdenticalVar(hyperCubes.get(hcId), 0)){
					onlyHc1HyperCubesList.add(hcId);
					hyperCubesIntersectionList.get(hcId).add(hyperCubes.size()-1);
				}
			}
			hyperCubesIntersectionList.add(onlyHc1HyperCubesList);
			hyperCubeClauseList.add(onlyHc1ClauseList);
			///System.out.println(hyperCubes);
			///System.out.println(hyperCubesIntersectionList);
			///System.out.println(onlyHc1ClauseList);
		}
		//System.out.println("Onlyh1 hyperCube created");
		//System.out.println("Now creating onlyh2 hyperCube");
		if(onlyHc2VarConstants.size() > 0){
			onlyHc2HyperCube = new HyperCube(h2);
			onlyHc2HyperCube.setVarConstants(onlyHc2VarConstants, 0);
			hyperCubes.add(onlyHc2HyperCube);
			ArrayList<Integer>onlyHc2HyperCubesList = new ArrayList<Integer>();
			Set<Integer>onlyHc2ClauseList = new HashSet<Integer>(hyperCubeClauseList.get(h2Id));
			for(Integer hcId : hyperCubesIntersectionList.get(h2Id)){
				if(!onlyHc2HyperCube.areDisjointOrIdenticalVar(hyperCubes.get(hcId), 0)){
					onlyHc2HyperCubesList.add(hcId);
					hyperCubesIntersectionList.get(hcId).add(hyperCubes.size()-1);
				}
			}
			hyperCubesIntersectionList.add(onlyHc2HyperCubesList);
			hyperCubeClauseList.add(onlyHc2ClauseList);
			///System.out.println(hyperCubes);
			///System.out.println(hyperCubesIntersectionList);
			//System.out.println(onlyHc2ClauseList);
		}
		//System.out.println("onlyh2 hyperCube created");
		//System.out.println("Now hyperCubeIntersectionList is : ");
		//System.out.println(hyperCubesIntersectionList);
		//System.out.println("Now removing h1Id = "+h1Id+" from all places");
		for(Integer hcId : hyperCubesIntersectionList.get(h1Id)){
			///System.out.println("hcId = "+hcId);
			int h1Index = hyperCubesIntersectionList.get(hcId).indexOf(h1Id);
			///System.out.println("h1Index = "+h1Index);
			hyperCubesIntersectionList.get(hcId).remove(h1Index);
			///System.out.println("hyperCubeIntersectionList = ");
			///System.out.println(hyperCubesIntersectionList);
		}
		//System.out.println("h1Id removed from all places and now hyperCubeIntersectionList is : ");
		//System.out.println(hyperCubesIntersectionList);
		//System.out.println(hyperCubes);
		//System.out.println(hyperCubesIntersectionList);
		//System.out.println("Now removing h2Id = "+h2Id+" from all places");
		for(Integer hcId : hyperCubesIntersectionList.get(h2Id)){
			///System.out.println("hcId = "+hcId);
			int h2Index = hyperCubesIntersectionList.get(hcId).indexOf(h2Id);
			///System.out.println("h2Index = "+h2Index);
			hyperCubesIntersectionList.get(hcId).remove(h2Index);
		}
		///System.out.println("h2Id removed from all places and now hyperCubeIntersectionList is : ");
		///System.out.println(hyperCubesIntersectionList);
		if(intSectVarConstants.size() > 0){
			HyperCube intSectHyperCube1 = new HyperCube(h1);
			intSectHyperCube1.setVarConstants(intSectVarConstants, 0);
			hyperCubes.set(h1Id,intSectHyperCube1);
			ArrayList<Integer>intSectHyperCubesList1 = new ArrayList<Integer>();
			Set<Integer> intSectHyperCubesClauseSet1 = new HashSet<Integer>(hyperCubeClauseList.get(h1Id));
			intSectHyperCubesClauseSet1.addAll(hyperCubeClauseList.get(h2Id));
			for(Integer hcId : hyperCubesIntersectionList.get(h1Id)){
				if(hcId == h2Id)
					continue;
				if(!intSectHyperCube1.areDisjointOrIdenticalVar(hyperCubes.get(hcId), 0)){
					intSectHyperCubesList1.add(hcId);
					hyperCubesIntersectionList.get(hcId).add(h1Id);
				}
			}
			hyperCubesIntersectionList.get(h1Id).clear();
			hyperCubesIntersectionList.get(h1Id).addAll(intSectHyperCubesList1);
			hyperCubeClauseList.get(h1Id).clear();
			hyperCubeClauseList.get(h1Id).addAll(intSectHyperCubesClauseSet1);
			HyperCube intSectHyperCube2 = new HyperCube(h2); 
			intSectHyperCube2.setVarConstants(intSectVarConstants, 0);
			hyperCubes.set(h2Id,intSectHyperCube2);
			ArrayList<Integer>intSectHyperCubesList2 = new ArrayList<Integer>();
			Set<Integer> intSectHyperCubesClauseSet2 = new HashSet<Integer>(hyperCubeClauseList.get(h1Id));
			intSectHyperCubesClauseSet2.addAll(hyperCubeClauseList.get(h2Id));
			for(Integer hcId : hyperCubesIntersectionList.get(h2Id)){
				if(hcId == h1Id)
					continue;
				if(!intSectHyperCube2.areDisjointOrIdenticalVar(hyperCubes.get(hcId), 0)){
					intSectHyperCubesList2.add(hcId);
					hyperCubesIntersectionList.get(hcId).add(h2Id);
				}
			}
			hyperCubesIntersectionList.get(h2Id).clear();
			hyperCubesIntersectionList.get(h2Id).addAll(intSectHyperCubesList2);
			hyperCubeClauseList.get(h1Id).clear();
			hyperCubeClauseList.get(h1Id).addAll(intSectHyperCubesClauseSet2);
		}
		
		///System.out.println(hyperCubes);
		///System.out.println(hyperCubesIntersectionList);
		///System.out.println(hyperCubeClauseList);
		//System.out.println(mergedIntSectHyperCubes);
		//System.out.println(onlyHc1HyperCube);
		//System.out.println(onlyHc2HyperCube);
		
	}
	
	public static void createDisjointHyperCubes(ArrayList<HyperCube>hyperCubes, ArrayList<Set<Integer>>hyperCubesClauseList, HashMap<HyperCube, Set<Integer>> hyperCubeClauseListHashMap){
		if(hyperCubes.size() < 2){
			return;
		}
		HyperCube hyperCube = hyperCubes.get(0);
		ArrayList<ArrayList<Integer>> hyperCubesIntersectionList = new ArrayList<ArrayList<Integer>>();
		//ArrayList<Boolean> doneList = new ArrayList<Boolean>();
		for(int hcId = 0 ; hcId < hyperCubes.size() ; hcId++){
			hyperCubesIntersectionList.add(new ArrayList<Integer>());
			//doneList.add(false);
		}
		for(int hc1Id = 0 ; hc1Id < hyperCubes.size() ; hc1Id++){
			for(int hc2Id = 0 ; hc2Id < hc1Id ; hc2Id++){
				/*
				Set<Integer>hc1VarConstants = hyperCubes.get(hc1Id).varConstants.get(varId);
				Set<Integer>hc2VarConstants = hyperCubes.get(hc2Id).varConstants.get(varId);
				Set<Integer>intersectionHcVarConstants = new HashSet<Integer>(hc1VarConstants);
				intersectionHcVarConstants.retainAll(hc2VarConstants);
				*//*
				if(varId == 1 && hc1Id == 1 && hc2Id == 0){
					System.out.println("printing varConstants");
					System.out.println(hyperCubes.get(hc1Id).varConstants.get(varId));
					System.out.println(hyperCubes.get(hc2Id).varConstants.get(varId));
				}*/
				if(!hyperCubes.get(hc1Id).areDisjointOrIdenticalVar(hyperCubes.get(hc2Id), 0)){
					hyperCubesIntersectionList.get(hc1Id).add(hc2Id);
					hyperCubesIntersectionList.get(hc2Id).add(hc1Id);
					/*
					if(varId == 1 && hc1Id == 1 && hc2Id == 0){
						System.out.println("hyperCube added");
					}*/
				}
			}
		}
		//System.out.println("Initial hyperCubeIntersectionList created and list size is " + hyperCubesIntersectionList.size());
		//System.out.println("first hypercube's intersection list size : " + hyperCubesIntersectionList.get(0).size());
		/*
		for(int i = 0 ; i < hyperCubesIntersectionList.size() ; i++){
			System.out.println(hyperCubesIntersectionList.get(i));
		}*/
		// Repetitively create disjoint(or same) hypercubes on variable varId
		//System.out.println(segmentsIndexList);
		while(true){
			boolean done = true;
			int hcListId = 0;
			for(hcListId = 0 ; hcListId < hyperCubesIntersectionList.size() ; hcListId++){
				ArrayList<Integer>hyperCubeList = hyperCubesIntersectionList.get(hcListId);
				if(hyperCubeList.size() > 0){
					done = false;
					break;
				}
			}
			// If there is no non empty list, we are done 
			if(done == true)
				break;
			/*for(int hcListId = hyperCubesIntersectionList.size() - 1 ; hcListId >= 0 ; hcListId--){
				if(hyperCubesIntersectionList.get(hcListId).size() > 0){
					int hc2Id = hyperCubes.indexOf(hyperCubesIntersectionList.get(hcListId).get(0));
					createDisjointHyperCubesVar(varId,hcListId,hc2Id,hyperCubesIntersectionList,hyperCubes);
				}
			}*/
			int hc2Id = hyperCubesIntersectionList.get(hcListId).get(0);
			//System.out.println("Now calling disjoint on var function with hcListId = "+hcListId+" and hc2Id = "+hc2Id);
			//System.out.println(hyperCubes);
			createDisjointSegments(hcListId,hc2Id,hyperCubesIntersectionList,hyperCubes,hyperCubesClauseList);
			//System.out.println(hyperCubes);
			//System.out.println(hyperCubesIntersectionList);
		}
		//System.out.println("Disjoint on variable "+ varId + "done, now number of hyperCubes = "+hyperCubes.size());
		// Remove duplicates
		hyperCubeClauseListHashMap.clear();
		for(int hcId = 0 ; hcId < hyperCubes.size() ; hcId++){
			HyperCube hc = hyperCubes.get(hcId);
			if(hyperCubeClauseListHashMap.containsKey(hc)){
				hyperCubeClauseListHashMap.get(hc).addAll(hyperCubesClauseList.get(hcId));
			}
			else{
				hyperCubeClauseListHashMap.put(hc, hyperCubesClauseList.get(hcId));
			}
		}
	}
	
	
	
	public static ArrayList<MLN> initDecomposer(MLN mln, Set<Pair> equiClass, ArrayList<Integer> sizeOfSegments){
		ArrayList<HyperCube> segments = new ArrayList<HyperCube>();
		ArrayList<Set<Integer>> segmentClauseList = new ArrayList<Set<Integer>>();
		HashMap<HyperCube,Set<Integer>> hyperCubeClauseListHashMap = new HashMap<HyperCube,Set<Integer>>();
		HashMap<Integer,Integer> clauseEquiClassTermIndices = new HashMap<Integer,Integer>();
		for(int clauseId = 0 ; clauseId < mln.clauses.size() ; clauseId++){
			WClause clause = mln.clauses.get(clauseId);
			if(clause.atoms.size() == 0){
				continue;
			}
			///System.out.println("Clause HyperCube : " + clause.hyperCubes);
			Atom firstAtom = clause.atoms.get(0);
			Term equiClassTerm = null;
			for(int termId = 0 ; termId < firstAtom.terms.size() ; termId++){
				Pair pair = new Pair(firstAtom.symbol.id,termId);
				if(equiClass.contains(pair)){
					equiClassTerm = firstAtom.terms.get(termId);
					break;
				}
			}
			///System.out.println("clause terms : " + clause.terms);
			///System.out.println("equiClassTerm : " + equiClassTerm);
			int equiClassTermIndex = clause.terms.indexOf(equiClassTerm);
			if(equiClassTermIndex == -1){
				continue;
			}
			clauseEquiClassTermIndices.put(clauseId,equiClassTermIndex);
			for(int hcId = clause.root.hyperCubesList.size() - 1 ; hcId >= 0  ; hcId--){
				for(HyperCube hyperCube : clause.root.hyperCubesList.get(hcId)){
					HyperCube projectedHyperCube = new HyperCube(hyperCube.varConstants.get(equiClassTermIndex));
					if(hyperCubeClauseListHashMap.containsKey(projectedHyperCube)){
						hyperCubeClauseListHashMap.get(projectedHyperCube).add(clauseId);
					}
					else{
						hyperCubeClauseListHashMap.put(projectedHyperCube, new HashSet<Integer>(Arrays.asList(clauseId)));
					}
				}
			}
		}
		for(HyperCube hyperCube : hyperCubeClauseListHashMap.keySet()){
			segments.add(hyperCube);
			segmentClauseList.add(hyperCubeClauseListHashMap.get(hyperCube));
		}
		///System.out.println("HyperCubes list : " + segments);
		///System.out.println("ClauseList : " + segmentClauseList);
		///System.out.println("Final hyperCubes size : " + segments.size());
		Decomposer.createDisjointHyperCubes(segments, segmentClauseList, hyperCubeClauseListHashMap);
		///System.out.println("HyperCubeClauseListHashMap : ");
		///System.out.println(hyperCubeClauseListHashMap);
 
		// If there is only one segment of size 1, then don't apply decomposer, otherwise it will get stuck into infinite loop of applying decomposers 
		if(hyperCubeClauseListHashMap.keySet().size() == 1){
			for(HyperCube hyperCube : hyperCubeClauseListHashMap.keySet())
				if(hyperCube.varConstants.get(0).size() == 1){
					return null;
				}
		}
		ArrayList<MLN> decomposedMLNs = new ArrayList<MLN>();
		createDecomposedMLNs(hyperCubeClauseListHashMap, mln, clauseEquiClassTermIndices, decomposedMLNs, sizeOfSegments);
		///System.out.println("No. of decomposed mlns : " + decomposedMLNs.size());
		/*//
		for(MLN dmln : decomposedMLNs){
			System.out.println("Printing MLN : ");
			for(WClause clause : dmln.clauses){
				System.out.println(clause.hyperCubes);
			}
		}*///
		return decomposedMLNs;
	}
	
	private static void createDecomposedMLNs(
			HashMap<HyperCube, Set<Integer>> hyperCubeClauseListHashMap,
			MLN parentMln, HashMap<Integer, Integer> clauseEquiClassTermIndices,
			ArrayList<MLN> decomposedMLNs, ArrayList<Integer> sizeOfSegments) {
		// TODO Auto-generated method stub
		
		for(HyperCube segment : hyperCubeClauseListHashMap.keySet()){
			MLN decomposerMln = new MLN();
			int segmentFirstElement = segment.varConstants.get(0).iterator().next();
			// For each clause, create two clauses : one which contains hyperCubes having segment segment and size of segment reduced to one, other 
			// containing hyperCubes which either didn't have segment segment or having hyperCubes resulting from removing segment from hyperCube. This
			// second clause will be formed simply by subtracting segment from hyperCube of parentMln clauses
			for(Integer clauseId : hyperCubeClauseListHashMap.get(segment)){
				WClause parentClause = parentMln.clauses.get(clauseId);
				WClause decomposerClause = MLN.create_new_clause(parentMln.clauses.get(clauseId)); // First clause in above description
				decomposerClause.root.hyperCubesList.clear();
				int equiClassTermIndex = clauseEquiClassTermIndices.get(clauseId);
				// We are running loop from last hypercube because some hyperCube may get deleted (when it contains only segment segment)
				int numPlaceHolders = parentClause.root.hyperCubesList.size();
				// This arraylist holds which new children to remove, all those children whose placeHolderlist doesn't contain any 
				// placeHolderId which cntains segment
				for(int hcListId = 0 ; hcListId < numPlaceHolders ; hcListId++){
					ArrayList<HyperCube> newHyperCubes = new ArrayList<HyperCube>();
					ArrayList<HyperCube> oldHyperCubes = parentClause.root.hyperCubesList.get(hcListId);
					for(int hcId = oldHyperCubes.size() - 1 ; hcId >= 0; hcId--){
						if(oldHyperCubes.get(hcId).varConstants.get(equiClassTermIndex).contains(segmentFirstElement)){
							HyperCube newHyperCube = new HyperCube(oldHyperCubes.get(hcId));
							newHyperCube.varConstants.get(equiClassTermIndex).clear();
							newHyperCube.varConstants.get(equiClassTermIndex).add(segmentFirstElement);
							newHyperCubes.add(newHyperCube);
							oldHyperCubes.get(hcId).varConstants.get(equiClassTermIndex).removeAll(segment.varConstants.get(0));
							if(oldHyperCubes.get(hcId).varConstants.get(equiClassTermIndex).size() == 0){
								oldHyperCubes.remove(hcId);
							}
						}
					}
					decomposerClause.root.hyperCubesList.add(newHyperCubes);
				}	// Traverse the tree
				//clauseDFS(decomposerClause.root.hyperCubesList, parentClause.root.hyperCubesList, decomposerClause.root, parentClause.root);
				decomposerMln.clauses.add(decomposerClause);
			}
			sizeOfSegments.add(segment.varConstants.get(0).size());
			decomposerMln.hcCountUpdate();
			decomposerMln.validPredPos = (ArrayList<ArrayList<Boolean>>)DeepCopyUtil.copy(parentMln.validPredPos);
			decomposedMLNs.add(decomposerMln);
		}
		// Now add MLN containing clauses which don't contain decomposer
		MLN newMLN = new MLN();
		for(int clauseId = 0 ; clauseId < parentMln.clauses.size() ; clauseId++){
			if(!clauseEquiClassTermIndices.containsKey(clauseId)){
				WClause newClause = MLN.create_new_clause(parentMln.clauses.get(clauseId));
				newMLN.clauses.add(newClause);
			}
		}
		if(newMLN.clauses.size() > 0){
			decomposedMLNs.add(newMLN);
			newMLN.validPredPos = (ArrayList<ArrayList<Boolean>>)DeepCopyUtil.copy(parentMln.validPredPos);
			newMLN.hcCountUpdate();
			sizeOfSegments.add(1);
		}
		parentMln.clauses.clear();
		parentMln = null;
	}
	/*
	private static void clauseDFS(ArrayList<ArrayList<HyperCube>> newHyperCubesList,
			ArrayList<ArrayList<HyperCube>> oldHyperCubesList, ClauseRoot newParent,
			ClauseRoot oldParent) {
		if(oldParent.children.size() > 0){
			for(Node oldChild : oldParent.children){
				Node newChild = null;
				boolean isValid = false;
				for(int phIndex = oldChild.placeHolderList.size() - 1 ; phIndex >= 0 ; phIndex--){
					if(newParent != null){
						if(newHyperCubesList.get(oldChild.placeHolderList.get(phIndex)).size() > 0){
							isValid = true;
							newChild.placeHolderList.add(oldChild.placeHolderList.get(phIndex));
						}
					}
					if(oldHyperCubesList.get(oldChild.placeHolderList.get(phIndex)).size() > 0){
						oldChild.placeHolderList.remove(phIndex);
					}
				}
				if(newParent != null && isValid == true){
					newChild = new Node();
					newChild.partialGroundings.addAll(oldChild.partialGroundings);
					/*
					for(Pair oldPredPos : oldChild.predPosList){
						Pair newPredPos = new Pair(oldPredPos);
						newChild.predPosList.add(newPredPos);
					}*/
					/*
					if(newParent.isRoot){
						newChild.rootParent = newParent;
					}
							
					else{
						newChild.rootParent = newParent.rootParent;
					}
				}
				
				clauseDFS(newHyperCubesList, oldHyperCubesList, newChild, oldChild);
				if(oldChild.placeHolderList.size() == 0){
					oldChild = null;
				}
			}
		}		
	}
	*/

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		// TODO Auto-generated method stub
		/*
		MLN mln = new MLN();
		Parser parser = new Parser(mln);
		String filename = new String("smoke/smoke_mln_29.txt");
		//String filename = new String("entity_resolution/er-bnct-eclipse.mln");
		parser.parseInputMLNFile(filename);
		//Decomposer d = new Decomposer();
		//ArrayList<Set<Pair>> predEquivalenceClasses = new ArrayList<Set<Pair>>();
		//d.findEquivalenceClasses(mln, predEquivalenceClasses);
		//System.out.println(d.isDecomposer(predEquivalenceClasses.get(0), mln));
		//d.initDecomposer(mln,predEquivalenceClasses.get(0));
		ArrayList<MLN> disjointMlns = new ArrayList<MLN>();
		createDisjointMLNs(mln, disjointMlns);
		System.out.println("No. of disjoint mlns : " + disjointMlns.size());*/
		HashMap<ArrayList<Integer>,Integer> map = new HashMap<ArrayList<Integer>,Integer>();
		ArrayList<Integer> a = new ArrayList<Integer>(Arrays.asList(1,2,3));
		map.put(a, 10);
		ArrayList<Integer> b = new ArrayList<Integer>(Arrays.asList(1,2,3));
		System.out.println(map.get(b));
		
	}

	public static ArrayList<MLN> ApplyDecomposer(MLN mln, Set<Pair> set, boolean approximate, ArrayList<Integer>sizeOfSegments) {
		ArrayList<MLN> decomposedMLNs = Decomposer.initDecomposer(mln, set, sizeOfSegments);
		return decomposedMLNs;
	}

}
