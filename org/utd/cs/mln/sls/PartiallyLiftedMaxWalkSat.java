package org.utd.cs.mln.sls;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.sat4j.specs.ContradictionException;
import org.utd.cs.gm.core.LogDouble;
import org.utd.cs.mln.alchemy.core.Atom;
import org.utd.cs.mln.alchemy.core.MLN;
import org.utd.cs.mln.alchemy.core.PredicateSymbol;
import org.utd.cs.mln.alchemy.core.WClause;
import org.utd.cs.mln.alchemy.util.Parser;

public class PartiallyLiftedMaxWalkSat {

	private Random random = new Random(System.currentTimeMillis());

//	private int maxTries;

	private int maxSteps = 100000000;
	
	private double p = 0.999;
	
	private int timeOut = Integer.MAX_VALUE;
	
	private int printInterval = 10;
	
	
	private MLN mln;

	private List<WClause> cnf;
	
	private List<List<Integer>> symbolClauseMap = new ArrayList<List<Integer>>();
	

	private List<WClause> unsatisfiedClauses = new ArrayList<WClause>();

	private List<LogDouble> unsatisfiedClauseWeights = new ArrayList<LogDouble>();
	
	private LogDouble minSum;

	private LogDouble unsSum;

	
	private PossibleWorld interpretation;

	@SuppressWarnings("unused")
	private PossibleWorld bestSolution;
	
	@SuppressWarnings("unused")
	private LogDouble hardClauseWeight;
	
	private static boolean print = true;
	
	private List<Double> costList = new ArrayList<Double>();

	
	public PartiallyLiftedMaxWalkSat(MLN mln_) {
		this.mln = mln_;
		this.cnf = mln_.clauses;
		
		List<Integer> numberOfGroundings = new ArrayList<Integer>();
		for (int i = 0; i < mln.symbols.size(); i++) {
			numberOfGroundings.add(null);
			symbolClauseMap.add(new ArrayList<Integer>());
		}
		
		for (int i=0; i<cnf.size(); i++) {
			WClause clause = cnf.get(i);
			for (Atom atom : clause.atoms) {
				Integer savedNumberOfGrounding = numberOfGroundings.get(atom.symbol.id);
				int atomsNumberOfGrounding = atom.getNumberOfGroundings();
				assert(savedNumberOfGrounding == null || savedNumberOfGrounding.equals(atomsNumberOfGrounding));
				numberOfGroundings.set(atom.symbol.id, atomsNumberOfGrounding);
				
				symbolClauseMap.get(atom.symbol.id).add(i);
			}
			unsatisfiedClauseWeights.add(null);
			unsatisfiedClauses.add(clause);
		}
		
		interpretation = new PossibleWorld(mln.symbols, numberOfGroundings);
		
		this.updateUnsatStat();
		hardClauseWeight = unsSum;
	}
	
	public void run() {
		
		long clockStartTime = System.currentTimeMillis();
		long lastPrintTime = clockStartTime;
		
		setState();
		this.updateUnsatStat();
		
		minSum = LogDouble.MAX_VALUE;
		int step = 1;
		int lastMinStep = 0;
		int minSteps = 0;

		String move = "";
		// run of the algorithm until condition of termination is reached
		bestSolution = interpretation.clone();
		while (step < maxSteps && unsatisfiedClauses.size() > 0) {
			boolean newBest = false;
			boolean moveChange = true;

			// choose between greedy step and random step
			if (random.nextDouble() < p) {
				greedyMove();
				moveChange = (move != "greedy");
				move = "greedy";
			} 
			else {
				stochasticMove();
				moveChange = (move != "random");
				move = "random";
			}
			step++;

			// if there is another new minimal unsatisfied value
			if (unsSum.compareTo(minSum) <= 0) {             
				if (unsSum.compareTo(minSum) < 0){
					newBest = true;
					// saves new minimum, resets the steps which shows how often the algorithm hits the minimum (minSteps)
					minSum = unsSum;
					minSteps = 0;
					// saves current best state
					bestSolution = interpretation.clone();
					// count of step in which the new minimum where found is saved (lastMinStep)
					lastMinStep = step;
				}
				// if the actual minimum is hit again minSteps where set +1
				if (unsSum.equals(minSum))
					minSteps++;
			}
			
			// print progress
			if(newBest || moveChange) {
				if(print)
					System.out.printf("  step %d: %s move, %d clauses are unsatisfied, sum of unsatisfied weights: %s, best: %s  %s\n", step, move, unsatisfiedClauses.size(), unsSum.toString(), minSum.toString(), newBest ? "[NEW BEST]" : "");
			}
			
			if(step%50 == 0) {
				long currentTime = System.currentTimeMillis();
				
				if(currentTime - lastPrintTime > 1000*printInterval) {
					lastPrintTime = currentTime;
					System.out.println(minSum.getValue());
					costList.add(minSum.getValue());
				}
				
				if(currentTime - clockStartTime > 1000*timeOut) {
					System.out.println("Time out reached!!!");
					break;
				}
			}
		}
		System.out.println("Solution found after " + step + " steps.");
	}

	private void setState() {
		interpretation.setRandomState();
	}
	
	private void greedyMove() {
		// Select a clause
		WClause clause = selectClause();
		List<PredicateSymbol> candidates = new ArrayList<PredicateSymbol>();
		
		// Select the best assignment of the best atom from the clause
		for (Atom atom : clause.atoms) {
			PredicateSymbol symbol = atom.symbol;
			candidates.add(symbol);
		}
		
		pickAndFlipBestAtom(candidates);
	}
	
	private void stochasticMove() {
		// Select a clause
		WClause clause;
		
//		if(random.nextBoolean()) {
			clause = selectClause();
//		} else {
//			clause = randomClause();
//		}
		
		
		//Select a random atom from the clause
		int atomIndex = random.nextInt(clause.atoms.size());
		Atom atom = clause.atoms.get(atomIndex);
		
		// Select a random assignment of the atom, so that the unsatisfied weight of the selected clause STRICTLY decreases
		int totalGroundingCount = atom.getNumberOfGroundings();
//		int trueGroundingCount = interpretation.getNoOfTrueGrounding(atom.symbol);
		
		int newTrueGroundingCount;
//		if(clause.sign.get(atomIndex)) {
//			// Select a number between zero and trueGroundingCount
//			newTrueGroundingCount = random.nextInt(trueGroundingCount);
////			newTrueGroundingCount = 0;
//		} else {
//			// Select a number between trueGroundingCount and totalGroundingCount
//			newTrueGroundingCount = trueGroundingCount + 1 + random.nextInt(totalGroundingCount - trueGroundingCount);
////			newTrueGroundingCount = totalGroundingCount;
//		}
		
		newTrueGroundingCount = random.nextInt(totalGroundingCount+1);
		
		interpretation.setNoOfTrueGrounding(atom.symbol, newTrueGroundingCount);
		// Update the solver state, 
		updateUnsatStat(atom.symbol);
		
	}

	private WClause selectClause() {
		//Random selection of clauses
		int clauseIndex = random.nextInt(unsatisfiedClauses.size());
		return unsatisfiedClauses.get(clauseIndex);
	}
	
	private WClause randomClause() {
		//Random selection of clauses
		int clauseIndex = random.nextInt(cnf.size());
		return cnf.get(clauseIndex);
	}
	
	private LogDouble clauseOverhead(WClause clause) {
		Double unsatClauseCount = 1d;
		
		for (int i=0; i<clause.atoms.size(); i++) {
			Atom atom = clause.atoms.get(i);
			int totalGroundings = atom.getNumberOfGroundings();
			
			double falseGroundings;
			if(clause.sign.get(i)) {
				falseGroundings = interpretation.getNoOfTrueGrounding(atom.symbol);
				if(falseGroundings < 0) {
					System.err.println("Unexpected!!");
				}
			} else {
				falseGroundings = totalGroundings - interpretation.getNoOfTrueGrounding(atom.symbol);
				if(falseGroundings < 0) {
					System.err.println("Unexpected!!");
				}
			}
			
			unsatClauseCount *= falseGroundings;
			
			if(unsatClauseCount < -1) {
				System.err.println("Arithmatic overflow occurred!");
				System.exit(1);
			}
		}
		
		LogDouble clauseOverhead = clause.weight.power(unsatClauseCount);
		
		if(clauseOverhead.getValue() < 0) {
			System.err.println("Unexpected!!");
		}
		
		return clauseOverhead;
	}
	
	private void pickAndFlipBestAtom(List<PredicateSymbol> candidates) {
		PredicateSymbol bestAtom = null;
		int bestTrueGroundingCount = -1;
		LogDouble bestCost = LogDouble.MAX_VALUE;
		
		for (PredicateSymbol candidate : candidates) {
			List<Object> returned_object = this.pickBestGrounding(candidate);
			LogDouble groundingCost = (LogDouble) returned_object.get(0);
			int groundingCount = (Integer) returned_object.get(1);
			
			// check whether the candidate is better
			boolean newBest = false;
			if (groundingCost.compareTo(bestCost) < 0) {
				// if the deltacosts enhances the state we found a new best candidate
				newBest = true;
			} else if (groundingCost.equals(bestCost) && random.nextBoolean()) {
				// ties broken at random
				newBest = true;
			}
			if (newBest) {
				bestAtom = candidate;
				bestTrueGroundingCount = groundingCount;
				bestCost = groundingCost;
			}
		}
		interpretation.setNoOfTrueGrounding(bestAtom, bestTrueGroundingCount);
		updateUnsatStat(bestAtom);
		
//		List<Object> returnList = new ArrayList<Object>();
//		returnList.add(bestDelta);
//		returnList.add(bestAtom);
//		returnList.add(bestTrueGroundingCount);
//		
//		return returnList;
	}
	
	private List<Object> pickBestGrounding(PredicateSymbol symbol) {
		// Find the best delta cost of the symbol, and the best truth assignment
		LogDouble leastOverhead = LogDouble.MAX_VALUE;
		int bestTrueGroundingCount = -1;
		
		int noOfGrounding = interpretation.noOfGrounding(symbol);
		int noOfTrueGrounding = interpretation.getNoOfTrueGrounding(symbol);
		
		List<Integer> groundingsToConsider = new ArrayList<Integer>();
		groundingsToConsider.add(0);
		groundingsToConsider.add(noOfGrounding);
		
		for (Integer i : groundingsToConsider) {
			interpretation.setNoOfTrueGrounding(symbol, i);
			
			LogDouble overhead = LogDouble.ONE;
			
			for (Integer clauseId : symbolClauseMap.get(symbol.id)) {
				WClause clause = cnf.get(clauseId);
				LogDouble clauseOverhead = this.clauseOverhead(clause);
				overhead = overhead.multiply(clauseOverhead);
			}
			
			// check whether the candidate is better
			boolean newBest = false;
			if (overhead.compareTo(leastOverhead) < 0) {
				// if the deltacosts enhances the state we found a new best candidate
				newBest = true;
			} else if (overhead.equals(leastOverhead) && random.nextBoolean()) {
				// ties broken at random
				newBest = true;
			}
			if (newBest) {
				bestTrueGroundingCount = i;
				leastOverhead = overhead;
			}
			
		}
		interpretation.setNoOfTrueGrounding(symbol, noOfTrueGrounding);
		
		List<Object> returnList = new ArrayList<Object>();
		returnList.add(leastOverhead);
		returnList.add(bestTrueGroundingCount);
		
		return returnList;
	}
	
	private void updateUnsatStat(PredicateSymbol symbol) {
		LogDouble deltaOverhead = LogDouble.ONE;
		
		for (Integer clauseId : symbolClauseMap.get(symbol.id)) {
			WClause clause = cnf.get(clauseId);
			
			LogDouble previousClauseOverHead = unsatisfiedClauseWeights.get(clauseId);
			LogDouble clauseOverhead = this.clauseOverhead(clause);
			deltaOverhead = deltaOverhead.multiply(clauseOverhead).divide(previousClauseOverHead);
			
			unsatisfiedClauseWeights.set(clauseId, clauseOverhead);
			if(clauseOverhead.equals(LogDouble.ONE)) {
				unsatisfiedClauses.remove(clause);
			} else {
				if(!unsatisfiedClauses.contains(clause))
					unsatisfiedClauses.add(clause);
			}
				
		}
		
		unsSum = unsSum.multiply(deltaOverhead);
		
	}

	private void updateUnsatStat() {
		LogDouble overhead = LogDouble.ONE;
		unsatisfiedClauses.clear();
		
		for (int i = 0; i < cnf.size(); i++) {
			WClause clause = cnf.get(i);
			
			LogDouble clauseOverhead = this.clauseOverhead(clause);
			overhead = overhead.multiply(clauseOverhead);
			
			unsatisfiedClauseWeights.set(i, clauseOverhead);
			if(!clauseOverhead.equals(LogDouble.ONE))
				unsatisfiedClauses.add(clause);
			
		}
		
		unsSum = overhead;
		
		
	}

	public static void main(String[] args) throws IOException, ContradictionException {
		
		int noOfRun = 5;
		
		List<String> domainList = new ArrayList<String>();
//		domainList.add(10);
//		domainList.add(25);
//		domainList.add(50);
//		domainList.add(100);
//		domainList.add(1000);
//		domainList.add(10000);
//		domainList.add(100000);
		
//		domainList.add("20");
//		domainList.add("25");
		domainList.add("30");
//		domainList.add("35");
//		domainList.add("35_50");
//		domainList.add("35_100");
//		domainList.add("35_500");
		
		List<List<List<Double>>> allCosts = new ArrayList<List<List<Double>>>();
		
		for (String domainSize : domainList) {
			int k = 2;
			int noOfPredicates = 4;
			int noOfClauses = 50;
			
//			String fileName = "webkb/webkb_mln_" + domainSize +".txt" ;
			String fileName = "student/student_mln_" + domainSize +".txt" ;
//			String fileName = "random/random_" +noOfPredicates + "_" + noOfClauses + "_" +  k + "_" + domainSize +".txt" ;
			
			System.out.println();
			List<List<Double>> costsForOneFile = new ArrayList<List<Double>>();
			for (int i = 0; i < noOfRun; i++) {
				System.out.println("Run " + (i+1) +" for file "+fileName );
				List<Double> costList = runFor(fileName);
				costsForOneFile.add(costList);
			}
			System.out.println();
			
			allCosts.add(costsForOneFile);
		}
		
		int i=0;
		for (List<List<Double>> costsForOneFile : allCosts) {
//			int domainSize = domainList.get(i);
			
			for (int j = 0; j < 10; j++) {
				Double mean = 0.0;
				Double variance = 0.0;
				
				for (List<Double> costsPerRun : costsForOneFile) {
					mean += costsPerRun.get(j);
				}
				mean = mean/costsForOneFile.size();

				for (List<Double> costsPerRun : costsForOneFile) {
					Double costForRunJ = costsPerRun.get(j);
					variance += (mean - costForRunJ)*(mean - costForRunJ);
				}
				variance = variance/costsForOneFile.size();
				
				System.out.println(mean + "\t" + Math.sqrt(variance));
			}
			
			i++;
		}

//		parser.parseInputMLNFile("testfiles/simple_mln1.txt");
//		parser.parseInputMLNFile("testfiles/ground_mln1.txt");
//		parser.parseInputMLNFile("testfiles/friends_smokers.txt");
		
	}
	
	public static List<Double> runFor(String mlnFile) throws FileNotFoundException, ContradictionException {
		long time = System.currentTimeMillis();
		long startTime = time;
		
		MLN mln = new MLN();
		Parser parser = new Parser(mln);
		parser.parseInputMLNFile(mlnFile);
		
		print = false;

		if(print)
			System.out.println("Time to parse = " + (System.currentTimeMillis() - time) + " ms");
		
		time = System.currentTimeMillis();
		MonadicConverter converter = new MonadicConverter(mln);
		MLN monadicMln = converter.convert();
		
		if(print)
			System.out.println("Time to convert = " + (System.currentTimeMillis() - time) + " ms");
		
		time = System.currentTimeMillis();
		PartiallyLiftedMaxWalkSat slsAlgo = new PartiallyLiftedMaxWalkSat(monadicMln);
		slsAlgo.printInterval = 10;
		slsAlgo.timeOut = 101;
		slsAlgo.run();
		long endTime = System.currentTimeMillis();
		
		if(print) {
			System.out.println();
			System.out.println("Best Solution is:-");
			System.out.println(slsAlgo.minSum.getValue());
		}
		
		System.out.println();
		System.out.println(slsAlgo.minSum.getValue());
		
		return slsAlgo.costList;
		
//		System.out.println("Running time of SLS = " + (endTime -  time) + " ms");
//		System.out.println("Total running time is " + (endTime -  startTime) + " ms");
	}

}
