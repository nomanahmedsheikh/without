package org.utd.cs.mln.alchemy.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.utd.cs.gm.utility.DeepCopyUtil;

public class HyperCube {
	public ArrayList<Set<Integer>> varConstants;
	public boolean satisfied;
	public int num_copies;
	public HyperCube(){
		init();
	}
	public HyperCube(int varCnt){
		init();
		for(int i = 0 ; i < varCnt ; i++){
			varConstants.add(new HashSet<Integer>());
		}
	}
	/*
	// Input is 1 constant per variable
	public HyperCube(ArrayList<Integer>constants){
		init();
		for(Integer val : constants){
			Set<Integer>constantList = new HashSet<Integer>();
			constantList.add(val);
			varConstants.add(constantList);
		}
	}
	*/
	public HyperCube(Set<Integer> singleVarConstants_){
		init();
		varConstants.add(new HashSet<Integer>(singleVarConstants_));
	}
	
	public HyperCube(ArrayList<Set<Integer>> varConstants_){
		init();
		for(int varId = 0 ; varId < varConstants_.size() ; varId++)
			varConstants.add(new HashSet<Integer>(varConstants_.get(varId)));
	}
	
	public HyperCube(HyperCube paramHyperCube){
		init();
		for(int i = 0 ; i < paramHyperCube.getVarCount() ; i++){
			Set<Integer>constantList = new HashSet<Integer>();
			Set<Integer>paramConstantList = paramHyperCube.varConstants.get(i);
			for(Integer elem : paramConstantList){
				constantList.add(elem);
			}
			varConstants.add(constantList);
		}
		satisfied = paramHyperCube.satisfied;
		num_copies = paramHyperCube.num_copies;
	}
	void init(){
		varConstants = new ArrayList<Set<Integer>>();
		satisfied = false;
		num_copies = 1;
	}
	public int getVarCount(){
		return varConstants.size();
	}
	public void setVarConstants(Set<Integer>constants,int varId){
		varConstants.set(varId, constants);
	}
	public void addVarConstants(Set<Integer>constants,int varId){
		varConstants.get(varId).addAll(constants);
	}
	public boolean areDisjointOrIdenticalVar(HyperCube h, int varId){
		if(this.varConstants.get(varId).equals(h.varConstants.get(varId))){
			return true;
		}
		Set<Integer>intersection = new HashSet<Integer>(this.varConstants.get(varId));
		intersection.retainAll(h.varConstants.get(varId));
		if(intersection.size() > 0){
			return false;
		}
		return true;
	}
	// return true if this hyperCube is subset of input hyperCube 
	public boolean isSubsetOf(HyperCube hyperCube){
		int varCnt = hyperCube.getVarCount();
		for(int varId = 0 ; varId < varCnt ; varId++){
			if(!hyperCube.varConstants.get(varId).containsAll(this.varConstants.get(varId))){
				return false;
			}
		}
		return true;
	}
	
	public boolean hasIntersection(HyperCube hyperCube){
		int varCnt = hyperCube.getVarCount();
		for(int varId = 0 ; varId < varCnt ; varId++){
			Set<Integer> intersection = new HashSet<Integer>(this.varConstants.get(varId));
			intersection.retainAll(hyperCube.varConstants.get(varId));
			if(intersection.size() == 0){
				return false;
			}
		}
		return true;
	}
	
	// Find intersection of this hypercube with hyperCube passed. Intersection is valid if all the variables has some
	// intersection in both hyoerCubes. Returns intersect HyperCube if they intersect, otherwise returns empty hyperCube
	public HyperCube intersect(HyperCube hyperCube){
		int varCnt = hyperCube.getVarCount();
		HyperCube intersectHyperCube = new HyperCube();
		for(int varId = 0 ; varId < varCnt ; varId++){
			Set<Integer> intersection = new HashSet<Integer>(this.varConstants.get(varId));
			intersection.retainAll(hyperCube.varConstants.get(varId));
			if(intersection.size() == 0){
				intersectHyperCube.varConstants.clear();
				return intersectHyperCube;
			}
			intersectHyperCube.varConstants.add(new HashSet<Integer>(intersection));
		}
		return intersectHyperCube;
	}
	
	public boolean isEmpty(){
		int varCnt = this.getVarCount();
		if(varCnt == 0){
			return true;
		}
		for(int varId = 0 ; varId < varCnt ; varId++){
			if(this.varConstants.get(varId).size() == 0){
				return true;
			}
		}
		return false;
	}
	public boolean isEmpty(Set<Integer> indicesToAvoid){
		int varCnt = this.getVarCount() - indicesToAvoid.size();
		if(varCnt == 0){
			return true;
		}
		for(int varId = 0 ; varId < varCnt ; varId++){
			if(indicesToAvoid.contains(varId))
				continue;
			if(this.varConstants.get(varId).size() == 0){
				return true;
			}
		}
		return false;
	}
	
	// get set of hypercubes after subtracting input hyoerCube from this hyperCube
	public ArrayList<HyperCube> getMinus(HyperCube hyperCube){
		ArrayList<HyperCube> minusHyperCubes = new ArrayList<HyperCube>();
		if(this.equals(hyperCube) || this.isSubsetOf(hyperCube)){
			return minusHyperCubes;
		}
		if(!this.hasIntersection(hyperCube)){
			minusHyperCubes.add(this);
			return minusHyperCubes;
		}
		HyperCube primaryMinusHyperCube = new HyperCube(this);
		int varCnt = hyperCube.getVarCount();
		for(int varId = 0 ; varId < varCnt ; varId++){
			primaryMinusHyperCube.varConstants.get(varId).removeAll(hyperCube.varConstants.get(varId));
		}
		HyperCube intersectionHyperCubeVar = new HyperCube(this);
		for(int varId = 0 ; varId < varCnt ; varId++){
			intersectionHyperCubeVar.varConstants.get(varId).retainAll(hyperCube.varConstants.get(varId));
		}
		//System.out.println("intersection hypercube");
		//System.out.println(intersectionHyperCubeVar);
		for(int index = 0 ; index < varCnt ; index++){
			HyperCube minusHyperCube = new HyperCube(this);
			for(int varId = 0 ; varId < index ; varId++){
				minusHyperCube.setVarConstants(intersectionHyperCubeVar.varConstants.get(varId), varId);
			}
			minusHyperCube.setVarConstants(new HashSet<Integer>(primaryMinusHyperCube.varConstants.get(index)), index);
			if(!minusHyperCube.isEmpty()){
				minusHyperCubes.add(minusHyperCube);
			}
		}
		return minusHyperCubes;
	}
	
	@Override
	public String toString() {
		return "HyperCube [varConstants=" + varConstants + ", satisfied="
				+ satisfied + ", num_copies=" + num_copies + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((varConstants == null) ? 0 : varConstants.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		HyperCube other = (HyperCube) obj;
		if (varConstants == null) {
			if (other.varConstants != null)
				return false;
		} else if (!varConstants.equals(other.varConstants))
			return false;
		return true;
	}
	//For test purpose
	public static void main(String []args){
		Set<Integer> set1 = new HashSet<Integer>(Arrays.asList(1,2));
		HyperCube hyperCube1 = new HyperCube(set1);
		Set<Integer> set2 = new HashSet<Integer>(Arrays.asList(2,3));
		HyperCube hyperCube2 = new HyperCube(set2);
		hyperCube1.varConstants.addAll(hyperCube2.varConstants);
		System.out.println(hyperCube1);
		hyperCube2.varConstants.get(0).add(4);
		System.out.println(hyperCube1);
		ArrayList<HyperCube> hcList = new ArrayList<HyperCube>();
		hcList.add(new HyperCube(hyperCube1));
		hcList.get(0).varConstants.get(1).add(5);
		System.out.println(hyperCube1);
		ArrayList<ArrayList<Integer>> a = new ArrayList<ArrayList<Integer>>();
		a.add(new ArrayList<Integer>(Arrays.asList(1,2)));
		a.add(new ArrayList<Integer>(Arrays.asList(3,4)));
		ArrayList<ArrayList<Integer>> b = new ArrayList<ArrayList<Integer>>();
		//b.addAll(a);
		b = (ArrayList<ArrayList<Integer>>)DeepCopyUtil.copy(a);
		b.get(0).set(0, 15);
		System.out.println(a);
		ArrayList<Integer> ar = new ArrayList<Integer>(Arrays.asList(10,15,20));
		int s = 20;
		System.out.println(ar.indexOf(s));
	}
	
}
