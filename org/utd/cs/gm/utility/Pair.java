package org.utd.cs.gm.utility;

public class Pair {
	public int first;
	public int second;
	public Pair(int f, int s){
		first = f;
		second = s;
	}
	public Pair(Pair oldPredPos) {
		first = oldPredPos.first;
		second = oldPredPos.second;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + first;
		result = prime * result + second;
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
		Pair other = (Pair) obj;
		if (first != other.first)
			return false;
		if (second != other.second)
			return false;
		return true;
	}
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "<"+first+","+second+">";
	}
	
}
