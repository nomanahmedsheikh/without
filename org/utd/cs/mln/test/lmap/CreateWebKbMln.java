package org.utd.cs.mln.test.lmap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Random;


public class CreateWebKbMln {

	Random random = new Random(System.currentTimeMillis());

	DecimalFormat format = new DecimalFormat("#.#####");
	
	private String randomWeight() {
		double rand = 2*random.nextDouble();
		return format.format(rand);
	}
	
	void printDomains(int domainSize, int isolatedDomainSize, PrintWriter out) {
		out.println("#domains");
		out.println("dom1={0, ..., "+domainSize +"}");
		out.println("dom2={0, ..., "+isolatedDomainSize +"}");
	}

	void printPredicates(PrintWriter out, int domainSize) {
		out.println("#predicates");
		out.println("Has(dom1, dom2)");
		out.println("Linked(dom1, dom1)");
		//PageClass(p,+c)
		for (int i = 0; i <= domainSize; i++) {
			out.println("PageClass"+i+"(dom1)");
		}
	}
	
	void printFormulas(PrintWriter out, int domainSize) {
		out.println("#formulas");
		//PageClass(p,+c)
		for (int i = 0; i <= domainSize; i++) {
			out.println("( PageClass"+i+"(p)) ::" + randomWeight());
		}
		
		//Has(p,w) => PageClass(p,+c)
		for (int i = 0; i <= domainSize; i++) {
			out.println("( !Has(p,w) | PageClass"+i+"(p)) ::" + randomWeight());
		}
		
		//PageClass(p1,+c1) ^ Linked(p1,p2) => PageClass(p2,+c2)
		for (int i = 0; i <= domainSize; i++) {
			for (int j = 0; j <= domainSize; j++) {
				out.println("( !PageClass"+i+"(p1)) | !Linked(p1,p2) | PageClass"+j+"(p2)) ::" + randomWeight());
			}
		}
		
		//Linked(p1,p2) => Linked(p2,p1)
		out.println("( !Linked(p1,p2) |  Linked(p2,p1)) ::" + format.format(200*random.nextDouble()));
	}
	
	void print(int domainSize, int isolatedDomainSize, PrintWriter out) {
		printDomains(domainSize, isolatedDomainSize, out);
		out.println();
		printPredicates(out, domainSize);
		out.println();
		printFormulas(out, domainSize);
	}

	public static void main(String[] args) throws FileNotFoundException {
		CreateWebKbMln testMln = new CreateWebKbMln();
		for (int i = 29; i < 30; i=i+50) {
		    File file = new File ("webkb/webkb_mln_9_" + i + ".txt");
		    PrintWriter writer = new PrintWriter (file);
			testMln.print(9, i, writer);
	        writer.flush();
	        writer.close();
		}
	}

}
