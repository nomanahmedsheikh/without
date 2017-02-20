package org.utd.cs.mln.test.lmap;

import java.io.FileNotFoundException;
import java.io.PrintWriter;


public class CreateStudentCnf {

	public static void main(String[] args) throws FileNotFoundException {
		int domainSize = 20;
		String input = "student/student_mln_"+domainSize+".txt";
		String output = "student/student_"+domainSize+".txt";
		
		int noVar = 3*(domainSize+1)*(domainSize+1);
		int noClauses = (domainSize+1)*(domainSize+1)*(domainSize+1)*(domainSize+1) + 3*(domainSize+1)*(domainSize+1);

	    PrintWriter writer = new PrintWriter (output);
	    
	    writer.println("p wcnf " + noVar + " " + noClauses);
	    
	    int serial = 1;
	    for (int i = 0; i < (domainSize+1)*(domainSize+1); i++) {
			writer.println("1.91852 " + serial + " 0");
	        writer.flush();
			serial++;
		}

	    for (int i = 0; i < (domainSize+1)*(domainSize+1); i++) {
			writer.println("1.74715 " + serial + " 0");
	        writer.flush();
			serial++;
		}

	    for (int i = 0; i < (domainSize+1)*(domainSize+1); i++) {
			writer.println("1.99759 -" + serial + " 0");
	        writer.flush();
			serial++;
		}
	    
	    int teachesStart = 1;
	    int takesStart = 1 + (domainSize+1)*(domainSize+1);
	    int offeredStart =  1 + 2*(domainSize+1)*(domainSize+1);
	    
	    for (int t = 0; t < domainSize+1; t++) {
	    	for (int c = 0; c < domainSize+1; c++) {
				for (int s = 0; s < domainSize+1; s++) {
					for (int j = 0; j < domainSize+1; j++) {
						int teachId = teachesStart + t + (domainSize+1)*c;
						int takeId = takesStart + s + (domainSize+1)*c;
						int offeredId = offeredStart + s + (domainSize+1)*j;
						
						writer.println("0.40965 -" + teachId+ " -"+takeId+" "+offeredId+" 0");
						writer.flush();
					}
				}
			}
		}

        writer.close();
	}

}
