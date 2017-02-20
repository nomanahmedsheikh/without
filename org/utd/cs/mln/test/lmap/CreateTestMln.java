package org.utd.cs.mln.test.lmap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class CreateTestMln {
	
	private static final String DOMAIN_SIZE = "\\$domSize";

	private static final String WEIGHT = "\\$wt";
	
	private static Random random = new Random(System.currentTimeMillis());
	
	private static DecimalFormat format = new DecimalFormat("#.#####");
	
	private static void createMln(String inputMln, String outputMln, int domainSize) throws FileNotFoundException {
		Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(inputMln))));
	    PrintWriter writer = new PrintWriter (new File (outputMln));
		
		while(scanner.hasNextLine()) {
			String line = scanner.nextLine();
			line = line.replaceAll(DOMAIN_SIZE, ""+domainSize);
			line = line.replaceAll(WEIGHT, randomWeight());
			
			writer.println(line);
			writer.flush();
		}
		writer.close();

	}
	
	private static String randomWeight() {
		int rand = 1+random.nextInt(5);
		return ""+rand;
	}
	
	public static void main(String[] args) throws FileNotFoundException {
		
//		List<Integer> domainList = new ArrayList<Integer>();
//		domainList.add(10);
//		domainList.add(25);
//		domainList.add(35);
		
		for (int domain = 10; domain < 101; domain=domain+10) {
			
//		}
		
//		for (Integer domain : domainList) {
			createMln("student_mln_int_30.txt", "student/student_mln_int_" + domain + ".txt", domain);
		}
	}

}
