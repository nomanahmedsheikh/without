package org.utd.cs.mln.lmap;

import java.util.ArrayList;
import java.util.List;

import org.utd.cs.mln.alchemy.core.MLN;
import org.utd.cs.mln.alchemy.core.WClause;
import org.utd.cs.mln.alchemy.util.GrindingMill;

public class NonSharedConverter {
	
	public static MLN convert(MLN mln){
		long time = System.currentTimeMillis();

		List<List<Integer>> termsToGround = new ArrayList<List<Integer>>(mln.symbols.size());
		for (int i = 0; i < mln.symbols.size(); i++) {
			termsToGround.add(new ArrayList<Integer>());
		}
		System.out.println("termsToGround size : "+termsToGround.size());
		for (WClause clause : mln.clauses) {

			//link clauses
			for (int i = 0; i < clause.atoms.size(); i++) 
			{
				for (int j = 0; j < clause.atoms.get(i).terms.size(); j++)
				{
					for (int k = i+1; k < clause.atoms.size(); k++) 
					{
						for (int l = 0; l < clause.atoms.get(k).terms.size(); l++)
						{
							if(clause.atoms.get(i).terms.get(j) == clause.atoms.get(k).terms.get(l))
							{
								// There is a link between Term_{i,j} and Term_{k,l}

								termsToGround.get(clause.atoms.get(i).symbol.id).add(j);
								termsToGround.get(clause.atoms.get(k).symbol.id).add(l);
								//System.out.println("termsToGround : "+ termsToGround.get(0)+" "+termsToGround.get(1)+" "+termsToGround.get(2));
							}
						}
					}
				}
			}
		}
		System.out.println("Printing termsToGround sent to grinding mill");
		for(int i = 0 ; i < termsToGround.size() ; i++)
		{
			System.out.println("predId : "+ i +", positions : "+termsToGround.get(i));
		}
		MLN nonSharedMln = GrindingMill.ground(mln, termsToGround);
		System.out.println("In new MLN, no. of clauses : "+nonSharedMln.clauses.size());
		System.out.println("In new MLN, no of predicates : "+nonSharedMln.getMaxPredicateId());
		System.out.println("Time taken to ground is " + (System.currentTimeMillis() - time) + " ms");
		
		return nonSharedMln;
	}
}
