package org.utd.cs.mln.lmap;

import java.util.*;

import org.utd.cs.gm.utility.DeepCopyUtil;
import org.utd.cs.gm.utility.Pair;

import org.utd.cs.mln.alchemy.core.Atom;
import org.utd.cs.mln.alchemy.core.EquivalenceClass;
import org.utd.cs.mln.alchemy.core.MLN;
import org.utd.cs.mln.alchemy.core.PredicateSymbol;
import org.utd.cs.mln.alchemy.core.Term;
import org.utd.cs.mln.alchemy.core.WClause;
import org.utd.cs.mln.alchemy.util.GrindingMill;

public class NonSameEquivConverter {
	
	public static void findNumPreds(int [][]numPredInClauses, int clause_index, WClause clause)
	{
		for(Atom atom : clause.atoms)
		{
			int predId = atom.symbol.id;
			numPredInClauses[clause_index][predId]++;
		}
	}
	
	public static boolean[] BFS(boolean[][] adjacencyMatrix, int vertexCount, int givenVertex){
	      // Result array.
	      boolean[] mark = new boolean[vertexCount];

	      Queue<Integer> queue = new LinkedList<Integer>();
	      queue.add(givenVertex);
	      mark[givenVertex] = true;

	      while (!queue.isEmpty())
	      {
	        Integer current = queue.remove();

	        for (int i = 0; i < vertexCount; ++i)
	            if (adjacencyMatrix[current][i] && !mark[i])
	            {
	                mark[i] = true;
	                queue.add(i);
	            }
	      }

	      return mark;
	  }
	
	static boolean set_contains(Set<Pair> s, Pair p)
	{
		for(Pair p1 : s)
		{
			if(p.equals(p1))
			{
				return true;
			}
		}
		return false;
	}
	
	static ArrayList<Integer> findPredCount(MLN mln)
	{
		int [][]numPredInClauses = new int[mln.clauses.size()][mln.getMaxPredicateId()];
		for(int i = 0 ; i < mln.clauses.size() ; i++)
		{
			findNumPreds(numPredInClauses, i, mln.clauses.get(i));
		}
		ArrayList<Integer>predCount = new ArrayList<Integer>();
		for(int i = 0 ; i < mln.getMaxPredicateId() ; i++)
		{
			predCount.add(0);
		}
		for(int i = 0 ; i < mln.clauses.size() ; i++)
		{
			for(int j = 0 ; j < mln.getMaxPredicateId() ; j++)
			{
				predCount.set(j, predCount.get(j) + numPredInClauses[i][j]);
			}
		}
		//System.out.println("Printing predCount : "+predCount);
		return predCount;
	}
	
	static int findMaxPredIndex(ArrayList<ArrayList<Integer>> termsToGround, List<Integer>predCount)
	{
		int max_pred_count = -1;
		int max_pred_index = -1;
		for(int i = 0 ; i < termsToGround.size() ; i++)
		{
			if(!termsToGround.get(i).isEmpty())
			{
				if(predCount.get(i) >= max_pred_count)
				{
					max_pred_count = predCount.get(i);
					max_pred_index = i;
				}
			}
		}
		return max_pred_index;
	}
	
	public static boolean[][] find_adj_matrix(MLN mln, List<List<Integer>> finalTermsToGround, ArrayList<Set<Pair>> equiClass, ArrayList<Integer> varIndexToClauseIndex, ArrayList<Integer> varIndexToDomainSize)
	{
		int cum_index = 0, cur_index = 0;
		int clause_index = 0;
		for (WClause clause : mln.clauses) {
			
			cum_index = cur_index = clause.findEquivClass(equiClass,cum_index,cur_index,varIndexToClauseIndex,clause_index,finalTermsToGround, varIndexToDomainSize);
			clause_index++;
		}
		/*
		for(Set<Pair> s : equiClass){
			System.out.print("[");
			for(Pair p : s){
				System.out.print(p+",");
			}
			System.out.println("]");
		}*/
		boolean adj_matrix[][] = new boolean[cur_index][cur_index];
		//set all to 0
		for(int i = 0 ; i < cur_index ; i++)
		{
			for(int j = 0 ; j < cur_index ; j++)
			{
				adj_matrix[i][j] = false;
			}
		}
		
		for(int i = 0 ; i < cur_index - 1 ; i++)
		{
			for(int j = i+1 ; j < cur_index ; j++)
			{
				for(Pair p0 : equiClass.get(i))
				{
					for(Pair p1 : equiClass.get(j))
					{
						if(p0.equals(p1))
						{
							adj_matrix[i][j] = true;
							adj_matrix[j][i] = true;
							break;
						}
					}
				}
			}
		}
		
		// 	print adj matrix
		/*
		System.out.println("Initial adj matrix : ");
		for(int i = 0 ; i < cur_index ; i++)
		{
			System.out.print("[");
			for(int j = 0 ; j < cur_index ; j++)
			{
				System.out.print(adj_matrix[i][j]+",");
			}
			System.out.println("]");
		}
		*/
		boolean final_adj_matrix[][] = new boolean[cur_index][cur_index];
		boolean vertexSeen[] = new boolean[cur_index];
		for(int i = 0 ; i < cur_index ; i++)
		{
			if(equiClass.get(i).isEmpty())
			{
				vertexSeen[i] = true;
				continue;
			}
			if(!vertexSeen[i])
			{
				boolean mark[] = BFS(adj_matrix,cur_index,i);
				mark[i] = true;
				final_adj_matrix[i] = mark.clone();
				for(int j = i+1 ; j < cur_index ; j++)
				{
					if(mark[j] == true)
					{
						vertexSeen[j] = true;
						final_adj_matrix[j] = mark.clone();
					}
				}
			}
		}
		 // print adj matrix
		/*
		System.out.println("adj matrix : ");
		for(int i = 0 ; i < cur_index ; i++)
		{
			System.out.print("[");
			for(int j = 0 ; j < cur_index ; j++)
			{
				System.out.print(final_adj_matrix[i][j]+",");
			}
			System.out.println("]");
		}
		*/
		return final_adj_matrix;
	}

	public static void findEquivalenceClasses(MLN mln, ArrayList<Set<Pair>> transitiveClosureEquiClass)
	{
		ArrayList<Set<Pair>> equiClass = new ArrayList<Set<Pair>>();
		int cum_index = 0, cur_index = 0;
		for (WClause clause : mln.clauses) {
			cum_index = cur_index = clause.findEquivClass(equiClass,cum_index,cur_index,mln.validPredPos);
		}
		/*//
		for(Set<Pair> s : equiClass){
			System.out.print("[");
			for(Pair p : s){
				System.out.print(p+",");
			}
			System.out.println("]");
		}*///
		boolean adj_matrix[][] = new boolean[cur_index][cur_index];
		//set all to 0
		for(int i = 0 ; i < cur_index ; i++)
		{
			for(int j = 0 ; j < cur_index ; j++)
			{
				adj_matrix[i][j] = false;
			}
		}
		
		for(int i = 0 ; i < cur_index - 1 ; i++)
		{
			for(int j = i+1 ; j < cur_index ; j++)
			{
				for(Pair p0 : equiClass.get(i))
				{
					for(Pair p1 : equiClass.get(j))
					{
						if(p0.equals(p1))
						{
							adj_matrix[i][j] = true;
							adj_matrix[j][i] = true;
							break;
						}
					}
				}
			}
		}
		
		// 	print adj matrix
		/*
		System.out.println("Initial adj matrix : ");
		for(int i = 0 ; i < cur_index ; i++)
		{
			System.out.print("[");
			for(int j = 0 ; j < cur_index ; j++)
			{
				System.out.print(adj_matrix[i][j]+",");
			}
			System.out.println("]");
		}
		*/
		boolean final_adj_matrix[][] = new boolean[cur_index][cur_index];
		boolean vertexSeen[] = new boolean[cur_index];
		for(int i = 0 ; i < cur_index ; i++)
		{
			if(equiClass.get(i).isEmpty())
			{
				vertexSeen[i] = true;
				continue;
			}
			if(!vertexSeen[i])
			{
				boolean mark[] = BFS(adj_matrix,cur_index,i);
				mark[i] = true;
				final_adj_matrix[i] = mark.clone();
				for(int j = i+1 ; j < cur_index ; j++)
				{
					if(mark[j] == true)
					{
						vertexSeen[j] = true;
						final_adj_matrix[j] = mark.clone();
					}
				}
			}
		}
		 // print adj matrix
		/*//
		System.out.println("adj matrix : ");
		for(int i = 0 ; i < cur_index ; i++)
		{
			System.out.print("[");
			for(int j = 0 ; j < cur_index ; j++)
			{
				System.out.print(final_adj_matrix[i][j]+",");
			}
			System.out.println("]");
		}*///
		
		// print equiClass
		/*//
		System.out.println("Print equiClass");
		for(int i = 0 ; i < cur_index ; i++){
			System.out.println("equivalence class of variable " + i + " : " + equiClass.get(i));
		}*///
		vertexSeen = new boolean[cur_index];
		for(int i = 0 ; i < cur_index ; i++){
			if(!vertexSeen[i]){
				transitiveClosureEquiClass.add(new HashSet<Pair>());
				int transitiveClosureLastIndex = transitiveClosureEquiClass.size() - 1;
				for(int j = 0 ; j < cur_index ; j++){
					if(final_adj_matrix[i][j]==true){
						vertexSeen[j] = true;
						transitiveClosureEquiClass.get(transitiveClosureLastIndex).addAll(equiClass.get(j));
						///System.out.println("transitive EQ class becomes : " + transitiveClosureEquiClass);
					}
				}
			}
		}
	}

	public static List<List<Integer>> findTermsToGround(MLN mln, boolean [][]final_adj_matrix, ArrayList<Set<Pair>> equiClass, List<Integer>varIndexToClauseIndex)
	{
		int cur_index = varIndexToClauseIndex.size();
		List<List<Integer>> termsToGround = new ArrayList<List<Integer>>(mln.symbols.size());
		for (int i = 0; i < mln.symbols.size(); i++) {
			termsToGround.add(new ArrayList<Integer>());
		}
		// check same equivalence class
		for(int i = 0 ; i < cur_index - 1 ; i++)
		{
			for(int j = i+1 ; j < cur_index ; j++)
			{
				//System.out.println("i = "+i+", j = "+j);
				if(final_adj_matrix[i][j]==true && varIndexToClauseIndex.get(i) == varIndexToClauseIndex.get(j))
				{
					for(Pair p : equiClass.get(i))
					{
						if(!termsToGround.get(p.first).contains((p.second))){
							termsToGround.get(p.first).add(p.second);
						}	
					}
					for(Pair p : equiClass.get(j))
					{
						if(!termsToGround.get(p.first).contains((p.second))){
							termsToGround.get(p.first).add(p.second);
						}	
					}
					//System.out.println("Not in required form...Try another MLN!!!");
				}
			}
		}
		// Printing termsToGround
		/*
		System.out.println("Printing termsToGround");
		for(int i = 0 ; i < termsToGround.size() ; i++)
		{
			System.out.println("predId : "+ i +", positions : "+termsToGround.get(i));
		}*/
		return termsToGround;
	}
	/*
	public static boolean find_pred_to_ground(MLN mln, boolean [][]adj_matrix, ArrayList<Set<Pair>> equiClass, ArrayList<Integer> varIndexToClauseIndex, ArrayList<ArrayList<Integer>> termsToGround, List<List<Integer>> finalTermsToGround) // It modifies finalTermsToGround
	{
		System.out.println("Initial termsToGround : ");
		for(int i = 0 ; i < termsToGround.size() ; i++)
		{
			System.out.println("predId : "+ i +", positions : "+termsToGround.get(i));
		}
		//Pair best_pred_pos = new Pair(-1,-1);
	
		int min_conflict_count = Integer.MAX_VALUE;
		int cur_index = equiClass.size();
		boolean [][]best_adj_matrix = new boolean[cur_index][cur_index];
		ArrayList<Set<Pair>> best_equi_class = new ArrayList<Set<Pair>>();
		ArrayList<Integer> bestVarIndexToClauseIndex = new ArrayList<Integer>(); 
		List<List<Integer>> bestFinalTermsToGround = new ArrayList<List<Integer>>();
		for (int i = 0; i < mln.symbols.size(); i++) {
			bestFinalTermsToGround.add(new ArrayList<Integer>());
		}
		for(int pred_index = 0 ; pred_index < termsToGround.size() ; pred_index++)
		{
			for(int pos_index = 0 ; pos_index < termsToGround.get(pred_index).size() ; pos_index++)
			{
				int pos = termsToGround.get(pred_index).get(pos_index);
				//List<List<Integer>> tempTermsToGround = (List<List<Integer>>)((ArrayList<List<Integer>>)finalTermsToGround).clone();
				List<List<Integer>> tempTermsToGround = (List<List<Integer>>)DeepCopyUtil.copy(finalTermsToGround);
				
				System.out.println("Printing tempTermsToGround just after cloning");
				for(int i = 0 ; i < tempTermsToGround.size() ; i++)
				{
					System.out.println("predId : "+ i +", positions : "+tempTermsToGround.get(i));
				}
				if(tempTermsToGround.get(pred_index).contains(pos))
				{
					continue;
				}
				tempTermsToGround.get(pred_index).add(pos);
				Pair cur_term = new Pair(pred_index,pos);
				Set<Integer> varList = new HashSet<Integer>();
				//System.out.println("equiClass = "+equiClass);
				//System.out.println("cur_term = "+cur_term);
				for(int i = 0 ; i < equiClass.size() ; i++)
				{
					Set<Pair> s = equiClass.get(i);
					if(s.contains(cur_term))
					{
						varList.add(i);
					}
				}
				//System.out.println("varList = "+varList);
				Set<Integer> adjVarList = new HashSet<Integer>();
				for(int varIndex : varList)
				{
					for(int j = 0 ; j < cur_index ; j++)
					{
						if(adj_matrix[varIndex][j] == true)
						{
							adjVarList.add(j);
						}
					}
				}
				//System.out.println("adjVarList = "+adjVarList);
				Set<Pair> pairsToGround = new HashSet<Pair>();
				for(int varIndex : adjVarList)
				{
					pairsToGround.addAll(equiClass.get(varIndex));
				}
				
				System.out.println("pairsToGround = "+pairsToGround);
				for(Pair p : pairsToGround)
				{
					if(!tempTermsToGround.get(p.first).contains(p.second))
					{
						tempTermsToGround.get(p.first).add(p.second);
								
					}
				}
					
				// Printing finalTermsToGround
				
				System.out.println("Printing tempTermsToGround");
				for(int i = 0 ; i < tempTermsToGround.size() ; i++)
				{
					System.out.println("predId : "+ i +", positions : "+tempTermsToGround.get(i));
				}
				ArrayList<Set<Pair>> tempEquiClass = new ArrayList<Set<Pair>>();
				ArrayList<Integer>tempVarIndexToClauseIndex = new ArrayList<Integer>();
				boolean [][]temp_adj_matrix = find_adj_matrix(mln, tempTermsToGround, tempEquiClass, tempVarIndexToClauseIndex);
				int conflict_count = 0;
				for(int i = 0 ; i < cur_index ; i++)
				{
					for(int j = 0 ; j < i ; j++)
					{
						if(temp_adj_matrix[i][j]==true && tempVarIndexToClauseIndex.get(i) == tempVarIndexToClauseIndex.get(j))
						{
							conflict_count++;
						}
					}
				}
				//System.out.println("conflict count = "+conflict_count);
				if(conflict_count < min_conflict_count)
				{
					min_conflict_count = conflict_count;
					best_adj_matrix = (boolean[][])DeepCopyUtil.copy(temp_adj_matrix);
					best_equi_class = tempEquiClass;
					bestVarIndexToClauseIndex = tempVarIndexToClauseIndex;
					bestFinalTermsToGround = tempTermsToGround;
				}
				if(min_conflict_count == 0)
				{
					break;
				}
			}
			if(min_conflict_count == 0)
			{
				break;
			}
		}
		adj_matrix = (boolean[][])DeepCopyUtil.copy(best_adj_matrix);
		//System.out.println("equiClass in function : "+equiClass);
		//System.out.println("best_equiClass in function : "+equiClass);
		equiClass.clear();
		equiClass.addAll(best_equi_class);
		varIndexToClauseIndex.clear();
		varIndexToClauseIndex.addAll(bestVarIndexToClauseIndex);
		finalTermsToGround.clear();
		finalTermsToGround.addAll(bestFinalTermsToGround);
		System.out.println("min conflict count = "+min_conflict_count);
		if(min_conflict_count == 0 || min_conflict_count == Integer.MAX_VALUE)
		{
			return false;
		}
		else
		{
			return true;
		}
	}
	*/
	public static ArrayList<EquivalenceClass> find_equivalence_classes(boolean [][]adj_matrix, ArrayList<Integer>varIndexToClauseIndex, ArrayList<Integer>varIndexToDomainSize, ArrayList<Integer> varToEquivalenceClassIndex)
	{
		ArrayList<EquivalenceClass> equivalence_classes = new ArrayList<EquivalenceClass>();
		Set<Integer>variablesSeen = new HashSet<Integer>();
		//System.out.println("adj_matrix length = "+adj_matrix.length);
		for(int i = 0 ; i < adj_matrix.length ; i++)
		{
			boolean conflict_found = false;
			if(variablesSeen.contains(i))
			{
				for(int j = 0 ; j < adj_matrix[i].length ; j++)
				{
					if(i != j && adj_matrix[i][j]==true && varIndexToClauseIndex.get(i) == varIndexToClauseIndex.get(j))
						equivalence_classes.get(varToEquivalenceClassIndex.get(i)).isSingle = false;
				}
			}
			else
			{
				EquivalenceClass equivalence_class = new EquivalenceClass();
				for(int j = 0 ; j < adj_matrix[i].length ; j++)
				{
					if(adj_matrix[i][j] == true)
					{
						equivalence_class.varIndices.add(j);
						equivalence_class.clauseIndices.add(varIndexToClauseIndex.get(j));
						equivalence_class.domainSize = varIndexToDomainSize.get(j); 
						varToEquivalenceClassIndex.set(j, equivalence_classes.size());
						variablesSeen.add(j);
					}
				}
				equivalence_classes.add(equivalence_class);
			}
		}
		return equivalence_classes;
	}
	
	public static void reduceToSubNetwork(MLN mln, ArrayList<EquivalenceClass> equivalence_classes, ArrayList<Set<Pair>>varIndexToPredPosIndex,List<List<Integer>> finalTermsToGroundMarginalMap)
	{
		for(EquivalenceClass ec : equivalence_classes)
		{
			if(ec.isSingle)
			{
				if (isMarMapApplicable(mln, ec, varIndexToPredPosIndex)) {
					mln.numSubNetworks *= ec.domainSize;
					for (int i = 0; i < mln.clauses.size(); i++) {
						if (!ec.clauseIndices.contains(i)) {
							//System.out.println("ec = "+ec+", i = "+i+" weight = "+mln.clauses.get(i).weight.getValue());
							mln.clauses.get(i).weight = mln.clauses.get(i).weight.power((double) 1 / ec.domainSize);
							//System.out.println("ec = "+ec+", i = "+i+" weight = "+mln.clauses.get(i).weight.getValue());
						}
					}
					// Marginal Map remove it
					//ec.domainSize=1;
				}
				else {
					// keeping track of such equivalence classes for future use

					/*
					Add compliment of our rule
					Pair wise just like adjacency matrix
					 */
				}
			}
		}
		System.out.println("no. of subnetworks : "+mln.numSubNetworks);
	}

	public static boolean isMarMapApplicable(MLN mln, EquivalenceClass ec, ArrayList<Set<Pair>>varIndexToPredPosIndex){

		// Go to every variable in equivalence class ec
		for(int v : ec.varIndices)
		{
			// For every predicate in which v occurs
			for(Pair p : varIndexToPredPosIndex.get(v))
			{
				// Check if p is a MAP predicate symbol then return true
				if (mln.symbols.get(p.first).queryType){
					return true;
				}
			}
		}
		return false;
	}
	
	public static MLN convert(MLN mln){
		long time = System.currentTimeMillis();
		Scanner keyScanner = new Scanner(System.in);
		
		List<List<Integer>> finalTermsToGround = new ArrayList<List<Integer>>(mln.symbols.size());
		List<List<Integer>> finalTermsToGroundMarginalMap = new ArrayList<List<Integer>>(mln.symbols.size());
		for (int i = 0; i < mln.symbols.size(); i++) {
			finalTermsToGround.add(new ArrayList<Integer>());
			finalTermsToGroundMarginalMap.add(new ArrayList<Integer>());
		}
		ArrayList<Set<Pair>> equiClass = new ArrayList<Set<Pair>>();
		ArrayList<Integer>varIndexToClauseIndex = new ArrayList<Integer>();
		ArrayList<Integer>varIndexToDomainSize = new ArrayList<Integer>();
		boolean [][]adj_matrix = find_adj_matrix(mln, finalTermsToGround, equiClass, varIndexToClauseIndex, varIndexToDomainSize);
		ArrayList<Integer>varToEquivalenceClassIndex = new ArrayList<Integer>();
		for(int i = 0 ; i < varIndexToClauseIndex.size() ; i++)
			varToEquivalenceClassIndex.add(0);
		//ArrayList<Boolean> isEquivalentClassSingle = new ArrayList<Boolean>();
		/*
		System.out.println("adj matrix : ");
		for(int i = 0 ; i < adj_matrix.length ; i++)
		{
			System.out.print("[");
			for(int j = 0 ; j < adj_matrix[i].length ; j++)
			{
				System.out.print(adj_matrix[i][j]+",");
			}
			System.out.println("]");
		}
		*/
		ArrayList<EquivalenceClass> equivalence_classes = find_equivalence_classes(adj_matrix, varIndexToClauseIndex, varIndexToDomainSize, varToEquivalenceClassIndex);
		
		// print equivalent classes 
		System.out.println("equivalence classes : "+equivalence_classes);
		// reduce to sub network
		ArrayList<Set<Pair>>varIndexToPredPosIndex = new ArrayList<Set<Pair>>();
		int termCountTillPrevClause = 0;
		for(WClause c : mln.clauses)
		{
			ArrayList<Term> termsSeen = new ArrayList<Term>();
			for(Atom a : c.atoms)
			{
				int predId = a.symbol.id;
				for(int t_index = 0 ; t_index < a.terms.size() ; t_index++)
				{
					Term t = a.terms.get(t_index);
					int indexOfTerm = termsSeen.indexOf(t);
					if(indexOfTerm != -1)
					{
						varIndexToPredPosIndex.get(termCountTillPrevClause + indexOfTerm).add(new Pair(predId,t_index));
					}
					else
					{
						varIndexToPredPosIndex.add(new HashSet<Pair>());
						int numVars = varIndexToPredPosIndex.size();
						varIndexToPredPosIndex.get(numVars-1).add(new Pair(predId,t_index));
						termsSeen.add(t);
					}
				}
			}
			//System.out.println(varIndexToPredPosIndex);
			termCountTillPrevClause = varIndexToPredPosIndex.size();
		}
		//Printing varIndexToPredPosIndex
		System.out.println(varIndexToPredPosIndex);
		ArrayList<Set<Pair>> predEquivalenceClass = new ArrayList<Set<Pair>>();
		for(EquivalenceClass e : equivalence_classes)
		{
			Set<Pair> s = new HashSet<Pair>();
			predEquivalenceClass.add(s);
			int set_index = predEquivalenceClass.size()-1;
			for(int v : e.varIndices)
			{
				for(Pair p : varIndexToPredPosIndex.get(v))
				{
					predEquivalenceClass.get(set_index).add(p);
					//System.out.println(predEquivalenceClass.get(set_index).size());
				}
			}
		}
		//Printing predEquivalenceClass
		System.out.println(predEquivalenceClass);
		reduceToSubNetwork(mln, equivalence_classes, varIndexToPredPosIndex, finalTermsToGroundMarginalMap);
		//System.out.println("second clause's weight : "+mln.clauses.get(1).weight.getValue());
		finalTermsToGround = findTermsToGround(mln, adj_matrix, equiClass, varIndexToClauseIndex);
		/*
		while(true)
		{
			ArrayList<ArrayList<Integer>> termsToGround = findTermsToGround(mln, adj_matrix, equiClass, varIndexToClauseIndex);
			int cur_index = varIndexToClauseIndex.size();
			//ArrayList<Integer>predCount = findPredCount(mln);		
			//System.out.println("termsToGround size : "+termsToGround.size());
			//int max_pred_index = findMaxPredIndex(termsToGround, predCount);
			boolean mlnToGround = find_pred_to_ground(mln, adj_matrix, equiClass, varIndexToClauseIndex, termsToGround, finalTermsToGround); // It modifies finalTermsToGround
				
			if(!mlnToGround)
			{
				break;
			}
			//keyScanner.next();
		}
		// added by Happy
		*/
		//System.out.println("Printing finalTermsToGround sent to grinding mill");
		/*
		for(int i = 0 ; i < finalTermsToGround.size() ; i++)
		{
			System.out.println("predId : "+ i +", positions : "+finalTermsToGround.get(i));
		}*/
		//keyScanner.next();

		// MARGINAL MAP

		mln.printMLN();

		MLN nonSameEquivMln = GrindingMill.ground(mln, finalTermsToGround);
		nonSameEquivMln.numSubNetworks = mln.numSubNetworks;
		//System.out.println("second clause's weight : "+nonSameEquivMln.clauses.get(1).weight.getValue());
		System.out.println("In new MLN, no. of clauses : "+nonSameEquivMln.clauses.size());
		System.out.println("In new MLN, no of predicates : "+nonSameEquivMln.getMaxPredicateId());
		/*
		for(int i = 0 ; i < 4 ; i++)
		{
			System.out.println(mln.clauses.get(i).atoms.get(0).symbol + "," + mln.clauses.get(i).atoms.get(1).symbol + "," + mln.clauses.get(i).atoms.get(2).symbol);
		}
		*/
		System.out.println("Time taken to ground is " + (System.currentTimeMillis() - time) + " ms");
		return nonSameEquivMln;
	}
}