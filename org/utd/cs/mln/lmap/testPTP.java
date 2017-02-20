package org.utd.cs.mln.lmap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class testPTP {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int size = 1000000;
		int arr[] = new int[size];
		ArrayList<Integer> a = new ArrayList<Integer>();
		for(int i = 0 ; i < size ; i++){
			a.add(arr[i]);
		}
		System.out.println("creating hashmap");
		long time = System.currentTimeMillis();
		HashMap<Integer,Integer> hash = new HashMap<Integer,Integer>();
		for(int i = 0 ; i < size ; i++){
			hash.put(i, i);
		}
		System.out.println("hashmap created and time taken is " + (long)(System.currentTimeMillis() - time) + " ms");
		time = System.currentTimeMillis();
		for(int i = 0 ; i < size ; i++){
			int index = (int)(Math.random()*size);
			if(arr[index] == 5){
				arr[index] = 10;
			}
			arr[index] = 15;
		}
		System.out.println("Time taken for array : " + (long)(System.currentTimeMillis() - time) + " ms");
		time = System.currentTimeMillis();
		for(int i = 0 ; i < size ; i++){
			int index = (int)(Math.random()*size);
			if(a.get(index) == 5){
				a.set(index,10);
			}
			a.set(index,15);
		}
		System.out.println("Time taken for arraylist : " + (long)(System.currentTimeMillis() - time) + " ms");
		time = System.currentTimeMillis();
		for(int i = 0 ; i < size ; i++){
			int index = (int)(Math.random()*size);
			if(hash.get(index) == 5){
				hash.put(index,10);
			}
			hash.put(index,15);
		}
		System.out.println("Time taken for hashmap : " + (long)(System.currentTimeMillis() - time) + " ms");
	}

}
