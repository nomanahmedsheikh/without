package org.utd.cs.mln.test.lmap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Random;


public class CreateStudentMLN {

	Random random = new Random(System.currentTimeMillis());

	void printDomains(int domainSize, PrintWriter out) {
		out.println("#domains");
		out.print("dom1={0, ..., "+domainSize +"}");
		out.println();
	}

	void printPredicates(PrintWriter out) {
		out.println("#predicates");
		out.println("Takes(dom1, dom1)");
		out.println("Teaches(dom1, dom1)");
		out.println("JobOffered(dom1, dom1)");
	}
	
	void printFormulas(PrintWriter out) {
		out.println("#formulas");
		out.println("( !Teaches(t, c) | !Takes(s, c) |  JobOffered(s, j)) ::" + 3);
		out.println("( Teaches(t, c)) ::" + 1);
		out.println("( Takes(s, c)) ::" + 5);
		out.println("( !JobOffered(s, j)) ::" + 2);
	}
	
	void print(int domainSize, PrintWriter out) {
		printDomains(domainSize, out);
		out.println();
		printPredicates(out);
		out.println();
		printFormulas(out);
	}

	public static void main(String[] args) throws FileNotFoundException {
		CreateStudentMLN testMln = new CreateStudentMLN();
		for (int i = 1; i < 19; i++) {
		    File file = new File ("student/student_mln_" + i + ".txt");
		    PrintWriter writer = new PrintWriter (file);
			testMln.print(i, writer);
	        writer.flush();
	        writer.close();
		}
	}

}
