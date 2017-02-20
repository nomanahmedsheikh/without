package org.utd.cs.mln.lmap;

import java.util.ArrayList;

public class TupleConstants {

	/**
	 * @param args
	 */
	public ArrayList<Integer> constants;
	public TupleConstants(){
		constants = new ArrayList<Integer>();
	}
	public TupleConstants(ArrayList<Integer> values){
		constants = new ArrayList<Integer>();
		for(int i = 0 ; i < values.size() ; i++){
			constants.add(values.get(i));
		}
	}
	@Override
	public String toString() {
		return "TupleConstants [constants=" + constants + "]";
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
