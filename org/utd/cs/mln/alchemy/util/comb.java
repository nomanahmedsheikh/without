package org.utd.cs.mln.alchemy.util;

public class comb {
	
	public static double findComb(int n, int k){
		return Math.exp(fact(n) - fact(n-k) - fact(k));
	}
	public static double fact(int n){
		double result = 0.0;
		for(int i = 1 ; i <= n ; i++){
			result += Math.log(i);
		}
		return result;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println(findComb(10,5));

	}

}
