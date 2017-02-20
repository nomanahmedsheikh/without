package org.utd.cs.mln.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GenerateRandomKSatMln {
	
	int domainSize;
	
	int k;
	
	int noOfPredicates;
	
	int noOfClauses;
	
	private static Random random = new Random(System.currentTimeMillis());
	
	private static DecimalFormat format = new DecimalFormat("#.#####");
	
	public GenerateRandomKSatMln(int domainSize, int noOfPredicates, int noOfClauses, int k) {
		this.domainSize = domainSize;
		this.noOfClauses = noOfClauses;
		this.noOfPredicates = noOfPredicates;
		this.k = k;
	}

	private static String randomWeight() {
		double rand = 2*random.nextDouble();
		return format.format(rand);
	}
	
	void printDomains(PrintWriter out) {
		out.println("#domains");
		out.print("dom1={0, ..., "+domainSize +"}");
		out.println();
	}


	void printPredicates(PrintWriter out) {
		out.println("#predicates");
		for (int i = 0; i < noOfPredicates; i++) {
			out.println("R" + i + "(dom1)");
		}
	}
	
	void printFormulas(PrintWriter out) {
		out.println("#formulas");

		List<Integer> predicateList = new ArrayList<Integer>();
		for (int i = 0; i < noOfPredicates; i++) {
			predicateList.add(i);
		}
		for (int i = 0; i < noOfClauses; i++) {
			Collections.shuffle(predicateList, random);
			
			out.print("( ");
			for (int j = 0; j < k - 1; j++) {
				if(random.nextBoolean()){
					out.print("!");
				}
				out.print("R"+predicateList.get(j) + "(x"+predicateList.get(j)+") | ");
			}

			if(random.nextBoolean()){
				out.print("!");
			}
			out.println("R"+predicateList.get(k-1) + "(x"+predicateList.get(k-1)+")) :: " + randomWeight());
			
		}
	}
	
	void print(PrintWriter out) {
		printDomains(out);
		out.println();
		out.flush();
		printPredicates(out);
		out.println();
		out.flush();
		printFormulas(out);
		out.flush();
	}

	public static void main(String[] args) throws FileNotFoundException {
		
		List<Integer> domainList = new ArrayList<Integer>();
		domainList.add(10);
		domainList.add(25);
		domainList.add(50);
		domainList.add(100);
		domainList.add(1000);
		domainList.add(10000);
		domainList.add(100000);
		
		for (Integer domainSize : domainList) {
			int k = 2;
			int noOfPredicates = 4;
			int noOfClauses = 50;
			
			String fileName = "random/random_" +noOfPredicates + "_" + noOfClauses + "_" +  k + "_" + domainSize +".txt" ;
			
			GenerateRandomKSatMln testMln = new GenerateRandomKSatMln(domainSize, noOfPredicates, noOfClauses, k);
			PrintWriter out = new PrintWriter(new File(fileName));
			testMln.print(out);
			out.close();
		}

	}

}
