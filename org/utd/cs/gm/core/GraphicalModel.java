package org.utd.cs.gm.core;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class GraphicalModel {
	
	public static final String TYPE_BAYES = "BAYES";
	
	private List<Variable> variables;
	
	private List<Function> functions;
	
	private String type;
	
	public List<Variable> getVariables() {
		return variables;
	}
	
	public List<Function> getFunctions() {
		return functions;
	}
	
	public void readUAI08(String uaiFIle) throws IOException {
		Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(uaiFIle))));
		
		type = scanner.next();
		
		if(type.equalsIgnoreCase(TYPE_BAYES)) {
			
			int varCount = scanner.nextInt();
			variables = new ArrayList<Variable>();
			
			for (int i = 0; i < varCount; i++) {
				
				int domainSize = scanner.nextInt();
				List<Integer> domain = new ArrayList<Integer>();
				
				for (int j = 0; j < domainSize; j++) {
					domain.add(j);
				}
				
				variables.add(new Variable(i,domain));
				
			}
			
			int functionCount = scanner.nextInt();
			Map<Integer, List<Variable>> parents = new HashMap<Integer, List<Variable>>();
			List<Integer> functionOrder = new ArrayList<Integer>();
			
			for (int i = 0; i < functionCount; i++) {
				int parentCount = scanner.nextInt();
				parentCount--;
				
				List<Variable> currentParent = new ArrayList<Variable>();
				for (int j = 0; j < parentCount; j++) {
					currentParent.add(variables.get(scanner.nextInt()));
				}
				int var = scanner.nextInt();
				functionOrder.add(var);
				parents.put(var,currentParent);
			}

			functions = new ArrayList<Function>(functionCount);
			for (int i = 0; i < functionCount; i++) {
				functions.add(null);
			}
			
			for (int i = 0; i < functionCount; i++) {
				int var = functionOrder.get(i);
				int numOfProbabilities = scanner.nextInt();
				
				CPT cpt = new CPT(parents.get(var), variables.get(var));
				functions.set(var, cpt);
				assert(numOfProbabilities == functions.get(var).getFunctionSize());
				
				int condVarDomainSize = Variable.getDomainSize(parents.get(var));
				
				for (int j = 0; j < condVarDomainSize; j++) {
					Variable.setAddress(cpt.getConditionalVariables(), j);
					
					for (int k = 0; k < cpt.getMarginalVariable().getDomainSize(); k++) {
						cpt.getMarginalVariable().setAddressValue(k);
						int address = Variable.getAddress(cpt.getVariables());
						
						double value = scanner.nextDouble();
						cpt.setTableEntry(new LogDouble(value), address);
						
					}
				}
				
				Collections.sort(cpt.getConditionalVariables());
			}
		}
		scanner.close();
	}
	
	public void readEvidence(String evidanceFile) throws IOException {
		Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(evidanceFile))));

		int num_evidence = scanner.nextInt();
		for (int i = 0; i < num_evidence; i++) {
			int var = scanner.nextInt();
			int val = scanner.nextInt();
			this.variables.get(var).setValue(val);
		}
//		System.out.println("Evidence read");
		scanner.close();

	}
	
	public static void main(String[] args) throws IOException {
		GraphicalModel graphicalModel = new GraphicalModel();
		graphicalModel.readUAI08("/Users/somdeb/Work/EclipseWorkSpace/Research/fMPE/FormulaBasedMPE/uai/darwicheEx1.uai");
	}

}
