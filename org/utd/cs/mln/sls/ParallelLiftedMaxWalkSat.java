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
import org.utd.cs.mln.lmap.NonSharedConverter;

public class ParallelLiftedMaxWalkSat {

	private Random random = new Random(System.currentTimeMillis());

	private int maxSteps = Integer.MAX_VALUE;
	
	
	/* Configuration */
	private int timeOut = Integer.MAX_VALUE;
	

	/* For printing objective function in regular interval */
	private int printInterval = 10;
	

	/* Global state */
	private MLN mln;

	private List<WClause> cnf;
	
	private List<List<Integer>> symbolClauseMap = new ArrayList<List<Integer>>();
	
	List<Integer> numberOfGroundings = new ArrayList<Integer>();

	private volatile LogDouble minSum;

	@SuppressWarnings("unused")
	private PossibleWorld bestSolution;
	
	private volatile boolean solutionFound = false;
	

	private List<WalkSatAgent> workers = new ArrayList<ParallelLiftedMaxWalkSat.WalkSatAgent>();
	private IntervalPrintAgent printer = new IntervalPrintAgent();


	private static boolean print = true;
	
	private List<Double> costList = new ArrayList<Double>();

	
	public ParallelLiftedMaxWalkSat(MLN mln_) {
		this.mln = mln_;
		this.cnf = mln_.clauses;
		
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
		}
		
		// Six thread of workers
		workers.add(new WalkSatAgent(0.999, 0.0, 0, 1101));
//		workers.add(new WalkSatAgent(0.999, 0.0, 0, 1101));
		workers.add(new WalkSatAgent(0.999, 0.6, 0, 43));
//		workers.add(new WalkSatAgent(0.999, 0.1, 0, 17));
//		workers.add(new WalkSatAgent(0.999, 0.0, 0, 9));
		workers.add(new WalkSatAgent(0.99, 0.1, 0, 23));
//		workers.add(new WalkSatAgent(0.99, 0.0, 0, 29));
//		workers.add(new WalkSatAgent(0.4, 0.8, 0, 43));
		
		minSum = LogDouble.MAX_VALUE;
	}
	
	public void start() throws InterruptedException {

		List<Thread> threads = new ArrayList<Thread>();
		for (WalkSatAgent agent : workers) {
			Thread worker = new Thread(agent);
			worker.setName("Agent_"+agent.greedyProb+"_"+agent.randomClauseProb);
			worker.start();
			threads.add(worker);
		}
		
		Thread printThread = new Thread(printer);
		printThread.setName("Print_Thread");
		printThread.start();
		threads.add(printThread);
		
		for (Thread thread : threads) {
			thread.join();
		}

	}
	
	class IntervalPrintAgent implements Runnable {

		@Override
		public void run() {
			int noOfPrint = timeOut/printInterval;
			int sleepTime = printInterval * 950;
			
			try {
				
				Thread.sleep(sleepTime);
				for (int i = 0; i < noOfPrint; i++) {
					System.out.println(minSum.getValue());
					costList.add(minSum.getValue());
					
					if(minSum.getValue() < 0.00001 ||  solutionFound){
						break;
					}
					
					Thread.sleep(sleepTime);
				}
				
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	class WalkSatAgent implements Runnable {
		
		/* Configuration */
		double greedyProb = 0.999;
		
		double randomClauseProb = 0.01;
		
		int randomRestartInterval = Integer.MAX_VALUE;
		
		int stateInitStrategy;
		
		private List<WClause> unsatisfiedClauses = new ArrayList<WClause>();

		private List<LogDouble> unsatisfiedClauseWeights = new ArrayList<LogDouble>();
		
		private LogDouble unsSum;
		
		private PossibleWorld interpretation;
		
		public WalkSatAgent(double greedyProb, double randomClauseProb, int stateInitStrategy, int randomRestartInterval) {
			
			this.greedyProb = greedyProb;
			this.randomClauseProb = randomClauseProb;
			this.randomRestartInterval = randomRestartInterval;
			this.stateInitStrategy = stateInitStrategy;
			
			for (int i=0; i<cnf.size(); i++) {
				WClause clause = cnf.get(i);
				unsatisfiedClauseWeights.add(null);
				unsatisfiedClauses.add(clause);
			}
			
			interpretation = new PossibleWorld(mln.symbols, numberOfGroundings);
		}

		@Override
		public void run() {
			
			long clockStartTime = System.currentTimeMillis();
			long lastRestartTime = clockStartTime;
			
			setState();
			updateUnsatStat();

			synchronized (minSum) {
				// if there is another new minimal unsatisfied value
				if (unsSum.compareTo(minSum) < 0){
					// saves new minimum, resets the steps which shows how often the algorithm hits the minimum (minSteps)
					minSum = unsSum;
				}
			}
			
			int step = 1;

			String move = "";
			// run of the algorithm until condition of termination is reached
			bestSolution = interpretation.clone();
			while (step < maxSteps && unsatisfiedClauses.size() > 0 && !solutionFound) {
				boolean newBest = false;
				boolean moveChange = true;
				long currentTime = System.currentTimeMillis();

					
				if(currentTime - lastRestartTime > 950*randomRestartInterval) {
					lastRestartTime = currentTime;

					if(print)
						System.out.println("Random restart...");

					setState();
					updateUnsatStat();
				} else {
					// choose between greedy step and random step
					if (random.nextDouble() < greedyProb) {
						greedyMove();
						moveChange = (move != "greedy");
						move = "greedy";
					} 
					else {
						stochasticMove();
						moveChange = (move != "random");
						move = "random";
					}
				}
				step++;

				synchronized (minSum) {
					// if there is another new minimal unsatisfied value
					if (unsSum.compareTo(minSum) < 0){
						newBest = true;
						// saves new minimum, resets the steps which shows how often the algorithm hits the minimum (minSteps)
						minSum = unsSum;
						// saves current best state
						bestSolution = interpretation.clone();
					}
				}
				
				// print progress
				if(newBest || moveChange) {
					if(print)
						System.out.printf("  step %d: %s move, %d clauses are unsatisfied, sum of unsatisfied weights: %s, best: %s  %s\n", step, move, unsatisfiedClauses.size(), unsSum.toString(), minSum.toString(), newBest ? "[NEW BEST]" : "");
				}
				
				if(step%500 == 0) {
					
					if(currentTime - clockStartTime > 1000*timeOut) {
						System.out.println("Time out reached!!!");
						break;
					}
				}
				
				
			}
			solutionFound = true;
			System.out.println("Solution found after " + step + " steps.");
		}
		
		private void updateUnsatStat(PredicateSymbol symbol) {
			LogDouble deltaOverhead = LogDouble.ONE;
			
			for (Integer clauseId : symbolClauseMap.get(symbol.id)) {
				WClause clause = cnf.get(clauseId);
				
				LogDouble previousClauseOverHead = unsatisfiedClauseWeights.get(clauseId);
				LogDouble clauseOverhead = clauseOverhead(clause);
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
				
				LogDouble clauseOverhead = clauseOverhead(clause);
				overhead = overhead.multiply(clauseOverhead);
				
				unsatisfiedClauseWeights.set(i, clauseOverhead);
				if(!clauseOverhead.equals(LogDouble.ONE))
					unsatisfiedClauses.add(clause);
				
			}
			
			unsSum = overhead;
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
			
			if(random.nextDouble() < randomClauseProb) {
				clause = randomClause();
			} else {
				clause = selectClause();
			}
			
			
			//Select a random atom from the clause
			int atomIndex = random.nextInt(clause.atoms.size());
			Atom atom = clause.atoms.get(atomIndex);
			
			// Select a random assignment of the atom, so that the unsatisfied weight of the selected clause STRICTLY decreases
			int totalGroundingCount = atom.getNumberOfGroundings();
			int trueGroundingCount = interpretation.getNoOfTrueGrounding(atom.symbol);
			
			int newTrueGroundingCount;
			
//			newTrueGroundingCount = random.nextInt(totalGroundingCount+1);
			if(trueGroundingCount == 0) {
				newTrueGroundingCount = totalGroundingCount;
			} else {
				newTrueGroundingCount = 0;
			}
			
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

		private void setState() {
			switch (stateInitStrategy) {
			case 1:
				interpretation.setAllFalse();
				break;

			case 2:
				interpretation.setAllTrue();
				break;

			default:
				interpretation.setRandomState();
				break;
			}
		}
		
	}
	

	


	public static void main(String[] args) throws IOException, ContradictionException, InterruptedException {
		
		int noOfRun = 5;
		int timeLimit = 201;
		int printInterval = 10;
		
		List<String> domainList = new ArrayList<String>();
//		domainList.add("10");
//		domainList.add("20");
//		domainList.add("30");
//		domainList.add("40");
//		domainList.add("50");
//		domainList.add("60");
//		domainList.add("70");
//		domainList.add("80");
//		domainList.add("90");
//		domainList.add("100");
		domainList.add("500");
		
		for (String domainSize : domainList) {
			
			String fileName = "webkb/webkb_mln_int_" + domainSize +".txt" ;

			System.out.println();
			List<List<Double>> costsForOneFile = new ArrayList<List<Double>>();
			for (int i = 0; i < noOfRun; i++) {
				System.out.println("Run " + (i+1) +" for file "+fileName );
				List<Double> costList = runFor(fileName, timeLimit, printInterval);
				costsForOneFile.add(costList);
			}
			System.out.println();
			
			for (int j = 0; j < timeLimit/printInterval; j++) {
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
			System.out.println();
			
			
//			allCosts.add(costsForOneFile);
		}
		
//		List<List<Double>> allmeans = new ArrayList<List<Double>>();
//		List<List<Double>> allStdDevs = new ArrayList<List<Double>>();
//		
//		for (List<List<Double>> costsForOneFile : allCosts) {
//			
//			List<Double> means = new ArrayList<Double>();
//			List<Double> stddevs = new ArrayList<Double>();
//			
//			for (int j = 0; j < timeLimit/printInterval; j++) {
//				Double mean = 0.0;
//				Double variance = 0.0;
//				
//				for (List<Double> costsPerRun : costsForOneFile) {
//					mean += costsPerRun.get(j);
//				}
//				mean = mean/costsForOneFile.size();
//
//				for (List<Double> costsPerRun : costsForOneFile) {
//					Double costForRunJ = costsPerRun.get(j);
//					variance += (mean - costForRunJ)*(mean - costForRunJ);
//				}
//				variance = variance/costsForOneFile.size();
//				
//				System.out.println(mean + "\t" + Math.sqrt(variance));
//				means.add(mean);
//				stddevs.add(Math.sqrt(variance));
//			}
//			System.out.println();
//			
//			allmeans.add(means);
//			allStdDevs.add(stddevs);
//		}
//		
//		System.out.println();
//		for (int j = 0; j < 11; j++) {
//			for (int i = 0; i < allmeans.size(); i++) {
//				System.out.print(allmeans.get(i).get(j) + "\t");
//				System.out.print(allStdDevs.get(i).get(j) + "\t");
//			}
//			System.out.println();
//		}

//		parser.parseInputMLNFile("testfiles/simple_mln1.txt");
//		parser.parseInputMLNFile("testfiles/ground_mln1.txt");
//		parser.parseInputMLNFile("testfiles/friends_smokers.txt");
		
	}
	
	public static List<Double> runFor(String mlnFile, int timeLimit, int printInterval) throws FileNotFoundException, ContradictionException, InterruptedException {
		long time = System.currentTimeMillis();
//		long startTime = time;
		
		MLN mln = new MLN();
		Parser parser = new Parser(mln);
		parser.parseInputMLNFile(mlnFile);
		
		print = false;

		if(print)
			System.out.println("Time to parse = " + (System.currentTimeMillis() - time) + " ms");
		
		time = System.currentTimeMillis();
		MLN nonSharedMln = NonSharedConverter.convert(mln);
		
		if(print)
			System.out.println("Time to convert = " + (System.currentTimeMillis() - time) + " ms");
		
		time = System.currentTimeMillis();
		ParallelLiftedMaxWalkSat slsAlgo = new ParallelLiftedMaxWalkSat(nonSharedMln);
		slsAlgo.printInterval = printInterval;
		slsAlgo.timeOut = timeLimit;
		slsAlgo.start();
//		long endTime = System.currentTimeMillis();
		
		if(print) {
			System.out.println();
			System.out.println("Best Solution is:-");
			System.out.println(slsAlgo.minSum.getValue());
		}
		
		System.out.println();
		System.out.println(slsAlgo.minSum.getValue());
		
		for (int i = 0; i < (slsAlgo.timeOut/slsAlgo.printInterval); i++) {
			slsAlgo.costList.add(slsAlgo.minSum.getValue());
		}
		
		return slsAlgo.costList;
		
//		System.out.println("Running time of SLS = " + (endTime -  time) + " ms");
//		System.out.println("Total running time is " + (endTime -  startTime) + " ms");
	}

}
