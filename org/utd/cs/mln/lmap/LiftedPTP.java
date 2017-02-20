package org.utd.cs.mln.lmap;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.print.attribute.Size2DSyntax;

import org.utd.cs.gm.utility.Pair;
import org.utd.cs.mln.alchemy.core.Atom;
import org.utd.cs.mln.alchemy.core.Evidence;
import org.utd.cs.mln.alchemy.core.HyperCube;
import org.utd.cs.mln.alchemy.core.MLN;
import org.utd.cs.mln.alchemy.core.PredicateNotFound;
import org.utd.cs.mln.alchemy.core.PredicateSymbol;
import org.utd.cs.mln.alchemy.core.Term;
import org.utd.cs.mln.alchemy.core.WClause;
import org.utd.cs.mln.alchemy.util.Parser;

public class LiftedPTP {

	public static int maxPredId = 0;
	public static int numGroundings;
	public static int numMLNs;
	public static double getPartition(MLN mln, boolean approximate){
		// base case : If every hyperCube of every clause
		if(isBaseCaseReached(mln) == true){
			return findBaseCaseZ(mln);
		}
		///System.out.println("Doing Splitting...");
		return LiftedSplit_new.liftedSplit(mln, approximate);
	}
	
	// return (2^#groundings)*exp(sum(for each hypercube in each clause) #hyperCubegroundings*wt*num_copies
	private static double findBaseCaseZ(MLN mln) {
		double weightedSumClauses = 0.0;
		HashMap<Integer,Set<ArrayList<Integer>>> predsGroundings = new HashMap<Integer,Set<ArrayList<Integer>>>(); 
		for(WClause clause : mln.clauses){
			Set<Integer> termIndicesToAvoid = new HashSet<Integer>();
			for(Term term : clause.partiallyGroundedTerms){
				termIndicesToAvoid.add(clause.terms.indexOf(term));
			}
			Set<Integer> termIndicesNotToAvoid = new HashSet<Integer>();
			for(int termId = 0 ; termId < clause.terms.size() ; termId++){
				if(!termIndicesToAvoid.contains(termId)){
					termIndicesNotToAvoid.add(termId);
				}
			}
			for(int childId = 0 ; childId < clause.root.children.size() ; childId++){
				int numSatisfiedHyperCubes = 0;
				for(Integer phId : clause.root.children.get(childId).placeHolderList){
					for(HyperCube hc : clause.root.hyperCubesList.get((int)phId)){
						if(hc.satisfied == false){
							continue;
						}
						ArrayList<ArrayList<Set<Integer>>> hyperCubeTuples = LiftedSplit_new.cartesianProdVar(hc.varConstants,termIndicesNotToAvoid);
						for(Atom atom : clause.atoms){
							Set<ArrayList<Integer>> predGroundings = new HashSet<ArrayList<Integer>>();
							for(ArrayList<Set<Integer>> hyperCubeTuple : hyperCubeTuples){
								ArrayList<Integer> predGrounding = new ArrayList<Integer>();
								for(Term term : atom.terms){
									predGrounding.add(hyperCubeTuple.get(clause.terms.indexOf(term)).iterator().next());
								}
								predGroundings.add(predGrounding);
							}
							if(predsGroundings.containsKey(atom.symbol.id)){
								predsGroundings.get(atom.symbol.id).addAll(predGroundings);
							}
							else{
								predsGroundings.put(atom.symbol.id, predGroundings);
							}
						} // end of atom loop
						numSatisfiedHyperCubes += hyperCubeTuples.size()*hc.num_copies;
					}// end of hyperCube loop
				} // end of phloop
				weightedSumClauses += numSatisfiedHyperCubes * clause.weight.getValue();
			}
		} // end of clause loop
		int totalNumPredGroundings = 0;
		for(Integer predId : predsGroundings.keySet()){
			totalNumPredGroundings += predsGroundings.get(predId).size();
		}
		return Math.pow(2, totalNumPredGroundings)*Math.exp(weightedSumClauses);
	}
	
	// Base case is reached when every hyperCube of every clause is either satisfied or empty
	private static boolean isBaseCaseReached(MLN mln) {
		for(WClause clause : mln.clauses){
			Set<Integer> termIndicesToAvoid = new HashSet<Integer>();
			for(Term term : clause.partiallyGroundedTerms){
				termIndicesToAvoid.add(clause.terms.indexOf(term));
			}
			for(int i = 0 ; i < clause.root.hyperCubesList.size() ; i++){
				for(HyperCube hc : clause.root.hyperCubesList.get(i)){
					if(hc.satisfied == false && !hc.isEmpty(termIndicesToAvoid)){
						return false;
					}
				}
			}
		}
		return true;
	}
	/*
	public static ArrayList<MLN> baseSolver(MLN mln){
		
	}*/
	/**
	 * @param args
	 * @throws FileNotFoundException 
	 * @throws PredicateNotFound 
	 */
	public static void main(String[] args) throws FileNotFoundException, PredicateNotFound {
		for(int i = 5 ; i < 6 ; i++){
			LiftedPTP.numGroundings = 0;
			System.out.println("evidence file : " + i);
			long time = System.currentTimeMillis();
			MLN mln = new MLN();
			Parser parser = new Parser(mln);
			//String filename = new String("smoke/smoke_mln.txt");
			String fn="PTP_data/random_pkb_mln_"+args[0]+".txt";
			String filename = new String(fn);
			//String filename = new String("PTP_data/ternary_mln.txt");
			parser.parseInputMLNFile(filename);
			String fn2="PTP_data/random_evidence_"+args[0]+"_"+i+".txt";
			ArrayList<Evidence> evidList = parser.parseInputEvidenceFile(fn2);
			//ArrayList<Evidence> evidList = parser.parseInputEvidenceFile("PTP_data/ternary_evidence.txt");
			//ArrayList<Evidence> evidList = parser.parseInputEvidenceFile("smoke/evidence.txt");
			MlnToHyperCube mlnToHyperCube = new MlnToHyperCube();
			HashMap<PredicateSymbol,ArrayList<ArrayList<HyperCube>>> predsHyperCubeHashMap = mlnToHyperCube.createPredsHyperCube(evidList,mln);
		
			int origNumClauses = mln.clauses.size();
			boolean isNormal = false;
			for(int clauseId = 0 ; clauseId < origNumClauses ; clauseId++){
				mln.clauses.addAll(mlnToHyperCube.createClauseHyperCube(mln.clauses.get(clauseId), predsHyperCubeHashMap, isNormal));
			}
			for(int clauseId = origNumClauses-1 ; clauseId >= 0 ; clauseId--){
				mln.clauses.remove(clauseId);
			}
			if(isNormal == true){
				ArrayList<Set<Pair>> predEquivalenceClasses = new ArrayList<Set<Pair>>();
				NonSameEquivConverter.findEquivalenceClasses(mln, predEquivalenceClasses);
				for(WClause clause : mln.clauses){
					for(ArrayList<HyperCube> hc : clause.root.hyperCubesList){
						clause.hyperCubes.addAll(hc);
					}
				}
				for(Set<Pair> eqClass : predEquivalenceClasses){
					createDisjoint(eqClass,mln);
				}
			}
			LiftedPTP.maxPredId = mln.max_predicate_id;
			System.out.println("Time taken to create clauses in hyperCube form : " + (long)(System.currentTimeMillis() - time) + " ms");
			long numHyperCubes = 0;
			for(WClause clause : mln.clauses){
				numHyperCubes += clause.hyperCubes.size();
			}
			time = System.currentTimeMillis();
			/*
			mln.clauses.get(0).hyperCubes.get(0).varConstants.set(0,new HashSet<Integer>(Arrays.asList(0,1,2)));
			mln.clauses.get(0).hyperCubes.get(0).varConstants.set(1,new HashSet<Integer>(Arrays.asList(0)));*/
			//mln.clauses.get(0).hyperCubes.get(0).satisfied = true;
			//mln.clauses.get(1).hyperCubes.get(0).satisfied = true;
			//mln.clauses.get(1).hyperCubes.get(0).varConstants.set(0,new HashSet<Integer>(Arrays.asList(0)));*/
			/*
			HyperCube h = new HyperCube();
			h.varConstants.add(new HashSet<Integer>(Arrays.asList(1,2)));
			h.varConstants.add(new HashSet<Integer>(Arrays.asList(2)));
			mln.clauses.get(0).hyperCubes.add(new HyperCube(h));
			*
			*/
			setValidPredPos(mln);
			mln.hcCountUpdate();
			for(WClause clause : mln.clauses){
				createDummyLeaves(clause);
			}
			System.out.println("Printing initial set of hyperCubes for each clause");
			double Z = 0.0;
			boolean approximate = false;
			time = System.currentTimeMillis();
			if(approximate == true){
				int num_iter = 10;
				for(int iter = 0 ; iter < num_iter ; iter++){
					MLN tempMln = new MLN(mln);
					double curZ = getPartition(tempMln, approximate);
					System.out.println("Iteration no. : " + (int)(iter+1));
					//System.out.println("curZ = " + curZ);
					Z += curZ;
				}
				Z = (Z/num_iter);
				System.out.println("Time taken for " + num_iter + " iterations : " + (double)(System.currentTimeMillis()-time)/1000.0 + " sec");
			}
			else{
				Z = getPartition(mln, approximate);
				System.out.println("Time taken for exact inference : " + (double)(System.currentTimeMillis()-time)/1000.0 + " sec");
				System.out.println("No. of groundings handled : " + numGroundings);	
				System.out.println("No. of mlns formed : " + numMLNs);
				System.out.println("No. of hyperCubes formed : " + numHyperCubes);
			}
			
			System.out.println(Z);

		}
	}

	private static void setValidPredPos(MLN mln) {
		for(int i  = 0  ; i <= LiftedPTP.maxPredId ; i++){
			mln.validPredPos.add(new ArrayList<Boolean>());
		}
		for(WClause clause : mln.clauses){
			//clause.print();
			for(Atom atom : clause.atoms){
				int predId = atom.symbol.id;
				if(mln.validPredPos.get(predId).size() == 0){
					for(int i = atom.terms.size() - 1 ; i >= 0 ; i--){
						mln.validPredPos.get(predId).add(true);
					}
				}
			}
		}
		
	}

	private static void createDummyLeaves(WClause clause) {
		Node n = new Node(LiftedPTP.maxPredId);
		n.rootParent = clause.root;
		clause.root.children.add(n);
		for(int i = 0 ; i < clause.root.hyperCubesList.size() ; i++){
			n.placeHolderList.add(i);
		}
		HashMap<Integer,Integer> predOccurHashMap = new HashMap<Integer,Integer>();
		HashMap<Integer,Integer> predArityMap = new HashMap<Integer,Integer>();
		for(Atom atom : clause.atoms){
			if(!predArityMap.containsKey(atom.symbol.id)){
				predArityMap.put(atom.symbol.id, atom.terms.size());
			}
			if(predOccurHashMap.containsKey(atom.symbol.id)){
				predOccurHashMap.put(atom.symbol.id,predOccurHashMap.get(atom.symbol.id)+1);
			}
			else{
				predOccurHashMap.put(atom.symbol.id,0);
			}
		}
		for(Integer predId : predOccurHashMap.keySet()){
			for(int i = 0 ; i <= predOccurHashMap.get(predId) ; i++){
				ArrayList<Integer> entry = new ArrayList<Integer>();
				for(int j = 0 ; j < predArityMap.get(predId) ; j++){
					entry.add(-1);
				}
				n.partialGroundings.get(predId).add(entry);
			}
		}
		
	}
	private static void createDisjoint(Set<Pair> equiClass, MLN mln) {
		ArrayList<HyperCube> segments = new ArrayList<HyperCube>();
		ArrayList<Set<Integer>> segmentClauseList = new ArrayList<Set<Integer>>();
		HashMap<HyperCube,Set<Integer>> hyperCubeClauseListHashMap = new HashMap<HyperCube,Set<Integer>>();
		HashMap<Integer,Integer> clauseEquiClassTermIndices = new HashMap<Integer,Integer>();
		for(int clauseId = 0 ; clauseId < mln.clauses.size() ; clauseId++){
			WClause clause = mln.clauses.get(clauseId);
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
			for(HyperCube hyperCube : clause.hyperCubes){
				HyperCube projectedHyperCube = new HyperCube(hyperCube.varConstants.get(equiClassTermIndex));
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
		///System.out.println("HyperCubes list : " + segments);
		///System.out.println("ClauseList : " + segmentClauseList);
		///System.out.println("Final hyperCubes size : " + segments.size());
		Decomposer.createDisjointHyperCubes(segments, segmentClauseList, hyperCubeClauseListHashMap);	
	
		for(HyperCube segment : hyperCubeClauseListHashMap.keySet()){
			int segmentFirstElement = segment.varConstants.get(0).iterator().next();
			// For each clause, create two clauses : one which contains hyperCubes having segment segment and size of segment reduced to one, other 
			// containing hyperCubes which either didn't have segment segment or having hyperCubes resulting from removing segment from hyperCube. This
			// second clause will be formed simply by subtracting segment from hyperCube of parentMln clauses
			for(Integer clauseId : hyperCubeClauseListHashMap.get(segment)){
				WClause parentClause = mln.clauses.get(clauseId);
				int equiClassTermIndex = clauseEquiClassTermIndices.get(clauseId);
				// We are running loop from last hypercube because some hyperCube may get deleted (when it contains only segment segment)
				for(int hcId = parentClause.hyperCubes.size() - 1 ; hcId >= 0 ; hcId--){
					HyperCube newHyperCube = new HyperCube(parentClause.hyperCubes.get(hcId));
					//newHyperCube.varConstants.get(equiClassTermIndex).retainAll(segment.varConstants.get(0));
					if(newHyperCube.varConstants.get(equiClassTermIndex).contains(segmentFirstElement)){
						newHyperCube.varConstants.get(equiClassTermIndex).clear();
						newHyperCube.varConstants.get(equiClassTermIndex).addAll(segment.varConstants.get(0));
						parentClause.hyperCubes.add(newHyperCube);
					}
					// Now find remaining hyperCube
					parentClause.hyperCubes.get(hcId).varConstants.get(equiClassTermIndex).removeAll(segment.varConstants.get(0));
					if(parentClause.hyperCubes.get(hcId).varConstants.get(equiClassTermIndex).size() == 0){
						parentClause.hyperCubes.remove(hcId);
					}
				}
			}
		}
		for(WClause clause : mln.clauses){
			clause.root.hyperCubesList.clear();
			clause.root.children.get(0).placeHolderList.clear();
			for(HyperCube hc : clause.hyperCubes){
				ArrayList<HyperCube> hcList = new ArrayList<HyperCube>();
				hcList.add(hc);
				clause.root.hyperCubesList.add(hcList);
				clause.root.children.get(0).placeHolderList.add(clause.hyperCubes.indexOf(hc));
			}
		}
		mln.hcCountUpdate();
	}
}
