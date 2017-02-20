package org.utd.cs.mln.alchemy.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DoublePair {
	public double first,second;
	public DoublePair(double first, double second) {
		super();
		this.first = first;
		this.second = second;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Set<ArrayList<Integer>>a = new HashSet<ArrayList<Integer>>();
		a.add(new ArrayList<Integer>(Arrays.asList(1,2)));
		a.add(new ArrayList<Integer>(Arrays.asList(1,2)));
		System.out.println(a);
	}

}
