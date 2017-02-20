package org.utd.cs.mln.alchemy.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.utd.cs.mln.lmap.TupleConstants;

public class CreateHyperCubeBasic {

	/**
	 * @param args
	 */
	public ArrayList<HyperCube> createGroundHyperCubes(ArrayList<TupleConstants> tuplesConstants){
		ArrayList<HyperCube> hypercubes = new ArrayList<HyperCube>();
		for(TupleConstants tuple : tuplesConstants){
			ArrayList<Set<Integer>>varConstants = new ArrayList<Set<Integer>>();
			for(Integer constant : tuple.constants){
				Set<Integer> s = new HashSet<Integer>();
				s.add(constant);
				varConstants.add(s);
			}
			HyperCube hypercube = new HyperCube(varConstants);
			hypercubes.add(hypercube);
		}
		return hypercubes;
	}
	
	public boolean isHyperCubePresent(HyperCube hyperCube, int varId, HashMap<HyperCube,ArrayList<HyperCube>> hyperCubeHashMap){
		Set<Integer>origVarConstant = hyperCube.varConstants.get(varId);
		hyperCube.setVarConstants(new HashSet<Integer>(Arrays.asList(-1)), varId);
		boolean result = false;
		if(hyperCubeHashMap.containsKey(hyperCube)){
			result = true;
		}
		else{
			result = false;
		}
		hyperCube.setVarConstants(origVarConstant, varId);
		return result;
	}
	
	public void addHyperCubeToVarHashMap(HyperCube hyperCube, int varId, HashMap<HyperCube,ArrayList<HyperCube>> hyperCubeHashMap, Set<HyperCube> set){
		boolean hyperCubePresent = isHyperCubePresent(hyperCube,varId,hyperCubeHashMap);
		HyperCube keyHyperCube = new HyperCube(hyperCube);
		keyHyperCube.setVarConstants(new HashSet<Integer>(Arrays.asList(-1)), varId);
		ArrayList<HyperCube> valueHyperCubes;
		if(hyperCubePresent == false){
			valueHyperCubes = new ArrayList<HyperCube>();
			hyperCubeHashMap.put(keyHyperCube, valueHyperCubes);
		}
		else{
			valueHyperCubes = hyperCubeHashMap.get(keyHyperCube);
		}
		valueHyperCubes.add(new HyperCube(hyperCube));
		if(valueHyperCubes.size() >= 2){
			set.add(keyHyperCube);
		}
	}
	
	public void removeHyperCubeFromVarHashMap(HyperCube hyperCube, int varId, HashMap<HyperCube,ArrayList<HyperCube>> hyperCubeHashMap, Set<HyperCube> set){
		boolean hyperCubePresent = isHyperCubePresent(hyperCube,varId,hyperCubeHashMap);
		if(hyperCubePresent == false){
			return;
		}
		else{
			HyperCube keyHyperCube = new HyperCube(hyperCube);
			keyHyperCube.setVarConstants(new HashSet<Integer>(Arrays.asList(-1)), varId);
			hyperCubeHashMap.get(keyHyperCube).remove(hyperCube);
			if(hyperCubeHashMap.get(keyHyperCube).size() < 2){
				set.remove(keyHyperCube);
			}
			if(hyperCubeHashMap.get(keyHyperCube).size() < 1){
				hyperCubeHashMap.remove(keyHyperCube);
			}
		}
	}
	
	public void printVarHashMap(HashMap<HyperCube,ArrayList<HyperCube>> HyperCubesHashMap){
		System.out.println(HyperCubesHashMap);
	}
	
	// Merge input hypercubes. Pick a variable, merge along it until further not possible. Then for next variable, 
	// do the same, and so on.
	public ArrayList<HyperCube> mergeHyperCubes(ArrayList<HyperCube> inpHyperCubes){
		if(inpHyperCubes.size() == 0){
			return new ArrayList<HyperCube>();
		}
		HyperCube hyperCube = inpHyperCubes.get(0);
		int varCnt = hyperCube.getVarCount();
		/*
		if(varCnt == 1){
			Set<HyperCube> hyperCubeHashSet = new HashSet<HyperCube>();
			for(HyperCube hc : inpHyperCubes){
				hyperCubeHashSet.add(hc);
			}
			ArrayList<HyperCube>mergedHyperCubes = new ArrayList<HyperCube>();
			mergedHyperCubes.addAll(hyperCubeHashSet);
			return mergedHyperCubes;
		}
		*/
		//To merge along a variable varId, make a hashMap where key is a hyperCube whose varId's varConstants is 
		//converted to -1 ,and values are hyperCubes with same key.
		// Ex : H1 = <0,1><0,1>, H2 = <2><0,1>, H3 = <0,1><2> and if we want to merge along 1st variable, then
		// keys will be <-1><0,1>, <-1><2>. In first key, H1 and H2 will come, and in second key, H3 will come
		// Now merge hyperCubes in one key i.e. merge H1 and H2 to get H4 = <0,1,2><0,1>
		// Also, for fast merge, store the keyHyperCubes in forMergeHyperCubes array for which we have to merge i.e. whose value contains
		// atleast 2 hyperCubes
		// So we make hashMap for each variable
		ArrayList<HashMap<HyperCube,ArrayList<HyperCube>>> HyperCubesHashMap = new ArrayList<HashMap<HyperCube,ArrayList<HyperCube>>>();
		ArrayList<Set<HyperCube>> forMergeHyperCubes = new ArrayList<Set<HyperCube>>();
		for(int i = 0 ; i < varCnt ; i++){
			HyperCubesHashMap.add(new HashMap<HyperCube,ArrayList<HyperCube>>());
			forMergeHyperCubes.add(new HashSet<HyperCube>());
		}
		// Create hash table of hypercubes for each variable
		for(int i = 0 ; i < inpHyperCubes.size() ; i++){
			hyperCube = inpHyperCubes.get(i);
			for(int varId = 0 ; varId < varCnt ; varId++){
				addHyperCubeToVarHashMap(hyperCube,varId,HyperCubesHashMap.get(varId),forMergeHyperCubes.get(varId));
			}
		}
		//printVarHashMap(HyperCubesHashMap.get(0));
		//printVarHashMap(HyperCubesHashMap.get(1));
		
		while(true){
			int indexVarId = -1;
			// If there is nothing to be merged along any variable, exit
			for(int varId = 0 ; varId < varCnt ; varId++){
				if(forMergeHyperCubes.get(varId).size() > 0){
					indexVarId = varId;
					break;
				}
			}
			if(indexVarId < 0)
				break;
			// Key HyperCube whose value hyperCubes are to be merged 
			HyperCube keyHyperCubeToMerge = forMergeHyperCubes.get(indexVarId).iterator().next();
			//System.out.println(keyHyperCubeToMerge);
			ArrayList<HyperCube> hyperCubesToMerge = HyperCubesHashMap.get(indexVarId).get(keyHyperCubeToMerge);
			HyperCube hyperCube1 = hyperCubesToMerge.get(0);
			HyperCube hyperCube2 = hyperCubesToMerge.get(1);
			//System.out.println(hyperCube1);
			//System.out.println(hyperCube2);
			//System.out.println(HyperCubesHashMap.get(0).keySet().size());
			for(int varId = 0 ; varId < varCnt ; varId++){
				removeHyperCubeFromVarHashMap(hyperCube1,varId,HyperCubesHashMap.get(varId),forMergeHyperCubes.get(varId));
				removeHyperCubeFromVarHashMap(hyperCube2,varId,HyperCubesHashMap.get(varId),forMergeHyperCubes.get(varId));
			}
			hyperCube1.addVarConstants(hyperCube2.varConstants.get(indexVarId), indexVarId);
			for(int varId = 0 ; varId < varCnt ; varId++){
				addHyperCubeToVarHashMap(hyperCube1, varId, HyperCubesHashMap.get(varId), forMergeHyperCubes.get(varId));
			}
			//System.out.println(hyperCube1);
			//System.out.println(HyperCubesHashMap.get(0).keySet().size());
			//printVarHashMap(HyperCubesHashMap.get(0));
			//System.out.println(hyperCube1);
		}
		ArrayList<HyperCube> mergedHyperCubes = new ArrayList<HyperCube>();
		for(ArrayList<HyperCube> hyperCubes : HyperCubesHashMap.get(0).values()){
			mergedHyperCubes.addAll(hyperCubes);
		}
		//System.out.println(mergedHyperCubes);
		return mergedHyperCubes;
	}
	
	// Creates a list of hyperCubes from input list of tuples.
	// Algo : First create ground hyperCube for each tuple, and then merge these hyperCubes
	public ArrayList<HyperCube> createHyperCubesBasic(ArrayList<TupleConstants> tuplesConstants){
		ArrayList<HyperCube> hyperCubes = createGroundHyperCubes(tuplesConstants);
		/*//
		for(HyperCube hyperCube : hyperCubes){
			System.out.println(hyperCube);
		}*///
		
		//ArrayList<HyperCube> mergedHyperCubes = mergeHyperCubes(hyperCubes);
		//System.out.println(mergedHyperCubes);
		//createDisjointHyperCubes(mergedHyperCubes);
		//System.out.println(mergedHyperCubes);
		//return mergedHyperCubes;
		return hyperCubes;
	}
	
	public void createDisjointHyperCubesVar(int varId, int h1Id, int h2Id, ArrayList<ArrayList<Integer>> hyperCubesIntersectionList, ArrayList<HyperCube>hyperCubes){
		HyperCube h1 = hyperCubes.get(h1Id);
		HyperCube h2 = hyperCubes.get(h2Id);
		//System.out.println("Inside createDisjointHyperCubesVar function with input hyperCubes : "+h1.toString()+" and "+h2.toString());
		// create 3 sets : intSectHc = h1^h2, h1-intSectHc, h2-intSectHc, all on variable varId
		Set<Integer>hc1VarConstants = new HashSet<Integer>(h1.varConstants.get(varId));
		Set<Integer>hc2VarConstants = new HashSet<Integer>(h2.varConstants.get(varId));
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
			onlyHc1HyperCube.setVarConstants(onlyHc1VarConstants, varId);
			hyperCubes.add(onlyHc1HyperCube);
			ArrayList<Integer>onlyHc1HyperCubesList = new ArrayList<Integer>();
			for(Integer hcId : hyperCubesIntersectionList.get(h1Id)){
				if(!onlyHc1HyperCube.areDisjointOrIdenticalVar(hyperCubes.get(hcId), varId)){
					onlyHc1HyperCubesList.add(hcId);
					hyperCubesIntersectionList.get(hcId).add(hyperCubes.size()-1);
				}
			}
			hyperCubesIntersectionList.add(onlyHc1HyperCubesList);
			//System.out.println(hyperCubes);
			//System.out.println(hyperCubesIntersectionList);
		}
		//System.out.println("Onlyh1 hyperCube created");
		//System.out.println("Now creating onlyh2 hyperCube");
		if(onlyHc2VarConstants.size() > 0){
			onlyHc2HyperCube = new HyperCube(h2);
			onlyHc2HyperCube.setVarConstants(onlyHc2VarConstants, varId);
			hyperCubes.add(onlyHc2HyperCube);
			ArrayList<Integer>onlyHc2HyperCubesList = new ArrayList<Integer>();
			for(Integer hcId : hyperCubesIntersectionList.get(h2Id)){
				if(!onlyHc2HyperCube.areDisjointOrIdenticalVar(hyperCubes.get(hcId), varId)){
					onlyHc2HyperCubesList.add(hcId);
					hyperCubesIntersectionList.get(hcId).add(hyperCubes.size()-1);
				}
			}
			hyperCubesIntersectionList.add(onlyHc2HyperCubesList);
			//System.out.println(hyperCubes);
			//System.out.println(hyperCubesIntersectionList);
		}
		//System.out.println("onlyh2 hyperCube created");
		//System.out.println("Now hyperCubeIntersectionList is : ");
		//System.out.println(hyperCubesIntersectionList);
		//System.out.println("Now removing h1Id = "+h1Id+" from all places");
		for(Integer hcId : hyperCubesIntersectionList.get(h1Id)){
			//System.out.println("hcId = "+hcId);
			int h1Index = hyperCubesIntersectionList.get(hcId).indexOf(h1Id);
			//System.out.println("h1Index = "+h1Index);
			hyperCubesIntersectionList.get(hcId).remove(h1Index);
			//System.out.println("hyperCubeIntersectionList = ");
			//System.out.println(hyperCubesIntersectionList);
		}
		//System.out.println("h1Id removed from all places and now hyperCubeIntersectionList is : ");
		//System.out.println(hyperCubesIntersectionList);
		//System.out.println(hyperCubes);
		//System.out.println(hyperCubesIntersectionList);
		//System.out.println("Now removing h2Id = "+h2Id+" from all places");
		for(Integer hcId : hyperCubesIntersectionList.get(h2Id)){
			//System.out.println("hcId = "+hcId);
			int h2Index = hyperCubesIntersectionList.get(hcId).indexOf(h2Id);
			//System.out.println("h2Index = "+h2Index);
			hyperCubesIntersectionList.get(hcId).remove(h2Index);
		}
		//System.out.println("h2Id removed from all places and now hyperCubeIntersectionList is : ");
		//System.out.println(hyperCubesIntersectionList);
		if(intSectVarConstants.size() > 0){
			HyperCube intSectHyperCube1 = new HyperCube(h1); 
			intSectHyperCube1.setVarConstants(intSectVarConstants, varId);
			hyperCubes.set(h1Id,intSectHyperCube1);
			ArrayList<Integer>intSectHyperCubesList1 = new ArrayList<Integer>();
			for(Integer hcId : hyperCubesIntersectionList.get(h1Id)){
				if(hcId == h2Id)
					continue;
				if(!intSectHyperCube1.areDisjointOrIdenticalVar(hyperCubes.get(hcId), varId)){
					intSectHyperCubesList1.add(hcId);
					hyperCubesIntersectionList.get(hcId).add(h1Id);
				}
			}
			hyperCubesIntersectionList.get(h1Id).clear();
			hyperCubesIntersectionList.get(h1Id).addAll(intSectHyperCubesList1);
			HyperCube intSectHyperCube2 = new HyperCube(h2); 
			intSectHyperCube2.setVarConstants(intSectVarConstants, varId);
			hyperCubes.set(h2Id,intSectHyperCube2);
			ArrayList<Integer>intSectHyperCubesList2 = new ArrayList<Integer>();
			for(Integer hcId : hyperCubesIntersectionList.get(h2Id)){
				if(hcId == h1Id)
					continue;
				if(!intSectHyperCube2.areDisjointOrIdenticalVar(hyperCubes.get(hcId), varId)){
					intSectHyperCubesList2.add(hcId);
					hyperCubesIntersectionList.get(hcId).add(h2Id);
				}
			}
			hyperCubesIntersectionList.get(h2Id).clear();
			hyperCubesIntersectionList.get(h2Id).addAll(intSectHyperCubesList2);
		}
		
		//System.out.println(hyperCubes);
		//System.out.println(hyperCubesIntersectionList);
		//System.out.println(mergedIntSectHyperCubes);
		//System.out.println(onlyHc1HyperCube);
		//System.out.println(onlyHc2HyperCube);
		
	}
	
	public void createDisjointHyperCubesVarOld(int varId, int h1Id, int h2Id, ArrayList<ArrayList<HyperCube>> hyperCubesIntersectionList, ArrayList<HyperCube>hyperCubes){
		HyperCube h1 = hyperCubes.get(h1Id);
		HyperCube h2 = hyperCubes.get(h2Id);
		// create 3 sets : intSectHc = h1^h2, h1-intSectHc, h2-intSectHc, all on variable varId
		Set<Integer>hc1VarConstants = new HashSet<Integer>(h1.varConstants.get(varId));
		Set<Integer>hc2VarConstants = new HashSet<Integer>(h2.varConstants.get(varId));
		Set<Integer>intSectVarConstants = new HashSet<Integer>(hc1VarConstants);
		intSectVarConstants.retainAll(hc2VarConstants);
		Set<Integer>onlyHc1VarConstants = new HashSet<Integer>(hc1VarConstants);
		onlyHc1VarConstants.removeAll(intSectVarConstants);
		Set<Integer>onlyHc2VarConstants = new HashSet<Integer>(hc2VarConstants);
		onlyHc2VarConstants.removeAll(intSectVarConstants);
		ArrayList<HyperCube>mergedIntSectHyperCubes = new ArrayList<HyperCube>();
		HyperCube onlyHc1HyperCube = null, onlyHc2HyperCube = null;
		if(intSectVarConstants.size() > 0){
			HyperCube intSectHyperCube1 = new HyperCube(h1); 
			intSectHyperCube1.setVarConstants(intSectVarConstants, varId);
			HyperCube intSectHyperCube2 = new HyperCube(h2); 
			intSectHyperCube2.setVarConstants(intSectVarConstants, varId);
			Set<HyperCube> intSectHyperCubes = new HashSet<HyperCube>();
			intSectHyperCubes.add(intSectHyperCube1);
			intSectHyperCubes.add(intSectHyperCube2);
			//System.out.println(intSectHyperCubes);
			//mergedIntSectHyperCubes = mergeHyperCubes(intSectHyperCubes);
			mergedIntSectHyperCubes.addAll(intSectHyperCubes);
			//System.out.println(mergedIntSectHyperCubes);
			for(HyperCube intSectHyperCube : mergedIntSectHyperCubes){
				ArrayList<HyperCube>intSectHyperCubesList = new ArrayList<HyperCube>();
				for(HyperCube hc : hyperCubesIntersectionList.get(h1Id)){
					int hcId = hyperCubes.indexOf(hc);
					if(hc.equals(h2))
						continue;
					if(!intSectHyperCube.areDisjointOrIdenticalVar(hc, varId)){
						intSectHyperCubesList.add(hc);
						hyperCubesIntersectionList.get(hcId).add(intSectHyperCube);
					}
				}
				for(HyperCube hc : hyperCubesIntersectionList.get(h2Id)){
					int hcId = hyperCubes.indexOf(hc);
					if(hc.equals(h1))
						continue;
					if(!intSectHyperCube.areDisjointOrIdenticalVar(hc, varId)){
						intSectHyperCubesList.add(hc);
						hyperCubesIntersectionList.get(hcId).add(intSectHyperCube);
					}
				}
				hyperCubesIntersectionList.add(intSectHyperCubesList);
				hyperCubes.add(intSectHyperCube);
				//System.out.println(hyperCubes);
				//System.out.println(hyperCubesIntersectionList);
			}
			
		}
		if(onlyHc1VarConstants.size() > 0){
			onlyHc1HyperCube = new HyperCube(h1);
			onlyHc1HyperCube.setVarConstants(onlyHc1VarConstants, varId);
			ArrayList<HyperCube>onlyHc1HyperCubesList = new ArrayList<HyperCube>();
			for(HyperCube hc : hyperCubesIntersectionList.get(h1Id)){
				int hcId = hyperCubes.indexOf(hc);
				if(!onlyHc1HyperCube.areDisjointOrIdenticalVar(hc, varId)){
					onlyHc1HyperCubesList.add(hc);
					hyperCubesIntersectionList.get(hcId).add(onlyHc1HyperCube);
				}
			}
			hyperCubesIntersectionList.add(onlyHc1HyperCubesList);
			hyperCubes.add(onlyHc1HyperCube);
			//System.out.println(hyperCubes);
			//System.out.println(hyperCubesIntersectionList);
		}
		if(onlyHc2VarConstants.size() > 0){
			onlyHc2HyperCube = new HyperCube(h2);
			onlyHc2HyperCube.setVarConstants(onlyHc2VarConstants, varId);
			ArrayList<HyperCube>onlyHc2HyperCubesList = new ArrayList<HyperCube>();
			for(HyperCube hc : hyperCubesIntersectionList.get(h2Id)){
				int hcId = hyperCubes.indexOf(hc);
				if(!onlyHc2HyperCube.areDisjointOrIdenticalVar(hc, varId)){
					onlyHc2HyperCubesList.add(hc);
					hyperCubesIntersectionList.get(hcId).add(onlyHc2HyperCube);
				}
			}
			hyperCubesIntersectionList.add(onlyHc2HyperCubesList);
			hyperCubes.add(onlyHc2HyperCube);
			//System.out.println(hyperCubes);
			//System.out.println(hyperCubesIntersectionList);
		}
		for(HyperCube hc : hyperCubesIntersectionList.get(h1Id)){
			int hcIndex = hyperCubes.indexOf(hc);
			hyperCubesIntersectionList.get(hcIndex).remove(hyperCubes.get(h1Id));
		}
		//System.out.println(hyperCubes);
		//System.out.println(hyperCubesIntersectionList);
		for(HyperCube hc : hyperCubesIntersectionList.get(h2Id)){
			int hcIndex = hyperCubes.indexOf(hc);
			hyperCubesIntersectionList.get(hcIndex).remove(hyperCubes.get(h2Id));
		}
		//System.out.println(hyperCubes);
		//System.out.println(hyperCubesIntersectionList);
		int firstIndexToRemove = h1Id, secondIndexToRemove = h2Id;
		
		if(h2Id > h1Id){
			firstIndexToRemove = h2Id;
			secondIndexToRemove = h1Id;
		}
		hyperCubesIntersectionList.remove(firstIndexToRemove);
		hyperCubesIntersectionList.remove(secondIndexToRemove);
		hyperCubes.remove(firstIndexToRemove);
		hyperCubes.remove(secondIndexToRemove);
		//System.out.println(hyperCubes);
		//System.out.println(hyperCubesIntersectionList);
		//System.out.println(mergedIntSectHyperCubes);
		//System.out.println(onlyHc1HyperCube);
		//System.out.println(onlyHc2HyperCube);
		
	}
	
	public void createDisjointHyperCubes(ArrayList<HyperCube>hyperCubes){
		if(hyperCubes.size() < 2){
			return;
		}
		HyperCube hyperCube = hyperCubes.get(0);
		int varCnt = hyperCube.getVarCount();
		for(int varId = 0 ; varId < varCnt ; varId++){
			//System.out.println("Creating disjoints on variable " + varId);
			/*
			for(HyperCube hc : hyperCubes){
				System.out.println(hc);
			}*/
			ArrayList<ArrayList<Integer>> hyperCubesIntersectionList = new ArrayList<ArrayList<Integer>>();
			//ArrayList<Boolean> doneList = new ArrayList<Boolean>();
			for(int hcId = 0 ; hcId < hyperCubes.size() ; hcId++){
				hyperCubesIntersectionList.add(new ArrayList<Integer>());
				//doneList.add(false);
			}
			//System.out.println("Input hyperCubes are : ");
			//System.out.println(hyperCubes);
			// Populate hyperCubesIntersectionList
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
					if(!hyperCubes.get(hc1Id).areDisjointOrIdenticalVar(hyperCubes.get(hc2Id), varId)){
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
				createDisjointHyperCubesVar(varId,hcListId,hc2Id,hyperCubesIntersectionList,hyperCubes);
				//System.out.println(hyperCubes);
				//System.out.println(hyperCubesIntersectionList);
			}
			//System.out.println("Disjoint on variable "+ varId + "done, now number of hyperCubes = "+hyperCubes.size());
		}
		// Remove duplicates
		Set<HyperCube> setHyperCube = new HashSet<HyperCube>();
		for(HyperCube hc : hyperCubes){
			setHyperCube.add(hc);
		}
		hyperCubes.clear();
		hyperCubes.addAll(setHyperCube);
	}
	
	public void createDisjointHyperCubesOld(ArrayList<HyperCube>hyperCubes){
		if(hyperCubes.size() < 2){
			return;
		}
		HyperCube hyperCube = hyperCubes.get(0);
		int varCnt = hyperCube.getVarCount();
		for(int varId = 0 ; varId < varCnt ; varId++){
			System.out.println("Creating disjoints on variable " + varId);
			/*
			for(HyperCube hc : hyperCubes){
				System.out.println(hc);
			}*/
			ArrayList<ArrayList<HyperCube>> hyperCubesIntersectionList = new ArrayList<ArrayList<HyperCube>>();
			for(int hcId = 0 ; hcId < hyperCubes.size() ; hcId++){
				hyperCubesIntersectionList.add(new ArrayList<HyperCube>());
			}
			// Populate hyperCubesIntersectionList
			for(int hc1Id = 0 ; hc1Id < hyperCubes.size() ; hc1Id++){
				for(int hc2Id = 0 ; hc2Id < hc1Id ; hc2Id++){
					/*
					Set<Integer>hc1VarConstants = hyperCubes.get(hc1Id).varConstants.get(varId);
					Set<Integer>hc2VarConstants = hyperCubes.get(hc2Id).varConstants.get(varId);
					Set<Integer>intersectionHcVarConstants = new HashSet<Integer>(hc1VarConstants);
					intersectionHcVarConstants.retainAll(hc2VarConstants);
					*/
					if(varId == 1 && hc1Id == 1 && hc2Id == 0){
						System.out.println("printing varConstants");
						System.out.println(hyperCubes.get(hc1Id).varConstants.get(varId));
						System.out.println(hyperCubes.get(hc2Id).varConstants.get(varId));
					}
					if(!hyperCubes.get(hc1Id).areDisjointOrIdenticalVar(hyperCubes.get(hc2Id), varId)){
						hyperCubesIntersectionList.get(hc1Id).add(hyperCubes.get(hc2Id));
						hyperCubesIntersectionList.get(hc2Id).add(hyperCubes.get(hc1Id));
						if(varId == 1 && hc1Id == 1 && hc2Id == 0){
							System.out.println("hyperCube added");
						}
					}
				}
			}
			System.out.println("Initial hyperCubeIntersectionList created and list size is " + hyperCubesIntersectionList.size());
			System.out.println("first hypercube's intersection list size : " + hyperCubesIntersectionList.get(0).size());
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
					ArrayList<HyperCube>hyperCubeList = hyperCubesIntersectionList.get(hcListId);
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
				int hc2Id = hyperCubes.indexOf(hyperCubesIntersectionList.get(hcListId).get(0));
				//System.out.println(hyperCubes);
				createDisjointHyperCubesVarOld(varId,hcListId,hc2Id,hyperCubesIntersectionList,hyperCubes);
				//System.out.println(hyperCubes);
				//System.out.println(hyperCubesIntersectionList);
			}
			System.out.println("Disjoint on variable "+ varId + "done, now number of hyperCubes = "+hyperCubes.size());
		}
	}
	
	public ArrayList<HyperCube> createComplementaryHyperCubes(ArrayList<HyperCube> inpHyperCubes, HyperCube domainHyperCube){
		ArrayList<HyperCube> complementaryHyperCubes = new ArrayList<HyperCube>();
		complementaryHyperCubes.add(new HyperCube(domainHyperCube));
		for(HyperCube hyperCube : inpHyperCubes){
			//System.out.println("input hypercube number : "+inpHyperCubes.indexOf(hyperCube));
			ArrayList<HyperCube> minusHyperCubes = new ArrayList<HyperCube>();
			for(HyperCube srcHyperCube : complementaryHyperCubes){
				//System.out.println("source hyperCube number : "+complementaryHyperCubes.indexOf(srcHyperCube));
				ArrayList<HyperCube> minusHyperComponents = new ArrayList<HyperCube>();
				minusHyperComponents.addAll(srcHyperCube.getMinus(hyperCube));
				//System.out.println("printing minusHyperComponents");
				//System.out.println("srcHyperCube : ");
				//System.out.println(srcHyperCube);
				//System.out.println("hyperCube");
				//System.out.println(hyperCube);
				//System.out.println(minusHyperComponents);
				minusHyperCubes.addAll(minusHyperComponents);
			}
			complementaryHyperCubes = minusHyperCubes;
			//System.out.println("size of complementaryHyperCubes becomes : "+complementaryHyperCubes.size());
		}
		return complementaryHyperCubes;
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ArrayList<TupleConstants> tuplesConstants = new ArrayList<TupleConstants>();
		//TupleConstants a = new TupleConstants(new ArrayList<Integer>(Arrays.asList(0)));
		//TupleConstants b = new TupleConstants(new ArrayList<Integer>(Arrays.asList(0,1,3)));
		//TupleConstants c = new TupleConstants(new ArrayList<Integer>(Arrays.asList(2,3)));
		//TupleConstants d = new TupleConstants(new ArrayList<Integer>(Arrays.asList(0,4)));
		//tuplesConstants.add(a);
		//tuplesConstants.add(b);
		//tuplesConstants.add(c);
		//tuplesConstants.add(d);
		/*
		for(int i = 0 ; i < 3; i++){
			for(int j = 0 ; j < 3 ; j++){
				for(int k = 0 ; k < 3 ; k++){
					tuplesConstants.add(new TupleConstants(new ArrayList<Integer>(Arrays.asList(i,j,k))));
				}
			}
		}*/
		//System.out.println(tuplesConstants);
		//tuplesConstants.remove(100);
		//tuplesConstants.remove(70);
		//tuplesConstants.remove(50);
		//tuplesConstants.remove(26);
		//tuplesConstants.remove(0);
		//System.out.println(tuplesConstants);
		CreateHyperCubeBasic chcb = new CreateHyperCubeBasic();
		ArrayList<HyperCube>mergedHyperCubes = chcb.createHyperCubesBasic(tuplesConstants);
		HyperCube hc = new HyperCube();
		Set<Integer>s1 = new HashSet<Integer>();
		s1.add(1);
		Set<Integer>s2 = new HashSet<Integer>();
		s2.add(2);
		s2.add(3);
		ArrayList<Set<Integer>> v1 = new ArrayList<Set<Integer>>();
		v1.add(s1);
		v1.add(s2);
		hc.varConstants = v1;
		Set<Integer>s3 = new HashSet<Integer>(s1);
		Set<Integer>s4 = new HashSet<Integer>();
		s4.add(2);
		s4.add(4);
		ArrayList<Set<Integer>> v2 = new ArrayList<Set<Integer>>();
		v2.add(s3);
		v2.add(s4);
		HyperCube hc1 = new HyperCube();
		hc1.varConstants = v2;
		ArrayList<HyperCube>inpHyperCubes = new ArrayList<HyperCube>();
		inpHyperCubes.add(hc);
		inpHyperCubes.add(hc1);
		mergedHyperCubes = chcb.mergeHyperCubes(inpHyperCubes);
		System.out.println(mergedHyperCubes);
		//chcb.createDisjointHyperCubes(mergedHyperCubes);
		System.out.println(mergedHyperCubes.size());
	}

}
