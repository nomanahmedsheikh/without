package org.utd.cs.mln.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CreateTestMln {
	
	int domainSize;

	public CreateTestMln(int domainSize) {
		this.domainSize = domainSize;
	}

	void printDomains() {
		System.out.println("#domains");
		System.out.print("dom1={");
		for (int j = 0; j < domainSize; j++) {
			System.out.print(j);
			if (j < domainSize - 1) {
				System.out.print(", ");
			}
		}
		System.out.println("}");
		System.out.println("cdom1={0}");
	}

	void printPredicates() {
		System.out.println("#predicates");
		for (int i = 0; i < domainSize; i++) {
			System.out.println("R" + i + "(dom1)");
		}

		for (int i = 0; i < domainSize; i++) {
			for (int j = 0; j < domainSize; j++) {
				System.out.println("S" + i + "" + j + "(cdom1)");
			}
		}

		for (int i = 0; i < domainSize; i++) {
			System.out.println("T" + i + "(dom1)");
		}

	}
	
	void printFormulas() {
		System.out.println("#formulas");

		for (int i = 0; i < domainSize; i++) {
			for (int j = 0; j < domainSize; j++) {
				System.out.println("( R" + i + "(y) |  S" + i + "" + j + "(c) |  T" + j + "(u)) ::1.02");
				System.out.println("( R" + i + "(y) |  S" + i + "" + j + "(c) | !T" + j + "(u)) ::1.03");
				System.out.println("( R" + i + "(y) | !S" + i + "" + j + "(c) |  T" + j + "(u)) ::1.04");
				System.out.println("( R" + i + "(y) | !S" + i + "" + j + "(c) | !T" + j + "(u)) ::1.05");
//				System.out.println("(!R" + i + "(y) |  S" + i + "" + j + "(c) |  T" + j + "(u)) ::1.06");
				System.out.println("(!R" + i + "(y) |  S" + i + "" + j + "(c) | !T" + j + "(u)) ::1.07");
//				System.out.println("(!R" + i + "(y) | !S" + i + "" + j + "(c) |  T" + j + "(u)) ::1.08");
				System.out.println("(!R" + i + "(y) | !S" + i + "" + j + "(c) | !T" + j + "(u)) ::1.09");
			}
		}
	}
	
	void print() {
		printDomains();
		System.out.println();
		printPredicates();
		System.out.println();
		printFormulas();
	}

	public static void main(String[] args) {
//		try {
//
//			String content = "This is the content to write into file";
//
//			File file = new File("polyadic_mln.txt");
//
//			// if file doesnt exists, then create it
//			if (!file.exists()) {
//				file.createNewFile();
//			}
//
//			FileWriter fw = new FileWriter(file.getAbsoluteFile());
//			BufferedWriter bw = new BufferedWriter(fw);
//			bw.write(content);
//			bw.close();
//
//			System.out.println("Done");
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
		CreateTestMln testMln = new CreateTestMln(20);
		testMln.print();

	}

}
