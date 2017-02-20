package org.utd.cs.mln.alchemy.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.*;

import org.utd.cs.gm.core.LogDouble;
import org.utd.cs.gm.utility.Pair;
import org.utd.cs.mln.alchemy.core.Atom;
import org.utd.cs.mln.alchemy.core.Domain;
import org.utd.cs.mln.alchemy.core.Evidence;
import org.utd.cs.mln.alchemy.core.Formula;
import org.utd.cs.mln.alchemy.core.MLN;
import org.utd.cs.mln.alchemy.core.PredicateNotFound;
import org.utd.cs.mln.alchemy.core.PredicateSymbol;
import org.utd.cs.mln.alchemy.core.Term;
import org.utd.cs.mln.alchemy.core.WClause;

public class Parser {
	public static final String DOMAINSTART = "#domains";
	public static final String PREDICATESTART = "#predicates";
	public static final String MAPSTART = "#map";
	public static final String MARGINALSTART = "#marginal";
	public static final String FORMULASTART = "#formulas";
	public static final String LEFTPRNTH = "(";
	public static final String RIGHTPRNTH = ")";
	public static final String NOTOPERATOR = "!";
	public static final String ANDOPERATOR = "^";
	public static final String OROPERATOR = "|";
	public static final String LEFTFLOWER = "{";
	public static final String RIGHTFLOWER = "}";
	public static final String WEIGHTSEPARATOR = "::";
	public static final String COMMASEPARATOR = ",";
	public static final String EQUALSTO = "=";
	public static final String ELLIPSIS = "...";
	
	private static final String REGEX_ESCAPE_CHAR = "\\";

	private enum ParserState {
		Domain,
		Predicate,
		Formula;
	};

	private MLN mln;

	private int predicateId;

	//set of domains with values in String format
	private List<Domain> domainList = new ArrayList<Domain>();

	//map key:predicate Id, value:for each of its terms, Index into the domainList List
	private Map<Integer,ArrayList<Integer>> predicateDomainMap = new HashMap<Integer, ArrayList<Integer>>();

	public Parser(MLN mln_) {
		mln = mln_;
		mln_.domainList = domainList;
		mln_.predicateDomainMap = predicateDomainMap;
		predicateId = 0;
	}

	// Added for Marginal Map
	public Parser(MLN mln_, boolean mlnAlreadyExists) {
		mln = mln_;
	}

	boolean isTermConstant(String term)
	{
		//if starts with a capital letter or number, it is taken as a constant
		return Character.isUpperCase(term.charAt(0));
	}

	Term create_new_term(int domainSize)
	{
		//map domain to an integer domain for ease of manipulation
		List<Integer> iDomain = new ArrayList<Integer>(domainSize);
		for(int k=0;k<domainSize;k++)
			iDomain.add(k);
		//create a new term
		Term term = new Term(0,iDomain);
		return term;
	}

	WClause create_new_clause(List<Integer> predicateSymbolIndex,List<Boolean> sign,
			List<List<Term> > iTermsList)
	{
		int numAtoms = predicateSymbolIndex.size();
		WClause clause = new WClause();
		clause.atoms = new ArrayList<Atom>(numAtoms);
		clause.satisfied = false;
		clause.sign = sign;
		for(int i=0;i<numAtoms;i++)
		{
			PredicateSymbol symbol = MLN.create_new_symbol(mln.symbols.get(predicateSymbolIndex.get(i)));
			List<Term> terms = iTermsList.get(i);
			Atom atom = new Atom(symbol,terms);
			clause.atoms.add(atom);
		}
		return clause;
	}

	//void parseCNFString(String formula,List<PredicateSymbol> predicateList)
	void parseClausesString(String line)
	{
		String[] clauseArr = line.split(WEIGHTSEPARATOR);
		Double weight = Double.parseDouble(clauseArr[1]);
		List<WClause> CNF = new ArrayList<WClause>();

		String clauseString = clauseArr[0];

		//If a clause starts with parenthesis, remove it
		if(clauseString.startsWith(LEFTPRNTH)) {

			if(!clauseString.endsWith(RIGHTPRNTH)) {
				System.out.println("Missing right parenthesis in clause " + clauseString);
				System.exit(-1);
			}

			clauseString = clauseString.substring(1, clauseString.length() - 1);
		}

		String[] atomStrings = clauseString.split(REGEX_ESCAPE_CHAR + OROPERATOR);
		List<Boolean> sign = new ArrayList<Boolean>();
		List<Integer> predicateSymbolIndex = new ArrayList<Integer>();
		List<List<String>> sTermsList = new ArrayList<List<String>>();
		
		for (int i = 0; i < atomStrings.length; i++) {
			sign.add(false);
			predicateSymbolIndex.add(null);
		}
		for(int i=0; i<atomStrings.length; i++)
		{
			//find opening and closing braces
			int startpos=atomStrings[i].indexOf(LEFTPRNTH);
			String predicateName = atomStrings[i].substring(0,startpos);

			if(predicateName.startsWith(NOTOPERATOR))
			{
				//negation
				predicateName = predicateName.substring(1,predicateName.length());
				sign.set(i, true);
			}

			for(int k=0;k<mln.symbols.size();k++)
			{
				//found the predicate
				if(mln.symbols.get(k).symbol.equals(predicateName))
				{
					predicateSymbolIndex.set(i, k);
					break;
				}
			}

			int endpos = atomStrings[i].indexOf(RIGHTPRNTH);
			String termsString = atomStrings[i].substring(startpos+1, endpos);
			//System.out.println("termsString = "+termsString);
			String[] terms = termsString.split(COMMASEPARATOR);
			sTermsList.add(new ArrayList<String>(Arrays.asList(terms)));

			//check if the number of terms is equal to the declared predicate
			if(terms.length != mln.symbols.get(predicateSymbolIndex.get(i)).variable_types.size())
			{
				System.out.println("Error! Number/domain of terms in the predicate delcaration does not match in formula. " + predicateName);
				System.exit(-1);
			}
		}

		//create required terms
		List<List<Term> > iTermsList = new ArrayList<List<Term>>();
		for (int i = 0; i < atomStrings.length; i++) {
			iTermsList.add(null);
		}
		for(int j=0;j<atomStrings.length;j++)
		{
			//for each term of atom i, check if it has already appeared in previous atoms of clause
			List<Term> iTerms = new ArrayList<Term>();
			for (int i = 0; i < sTermsList.get(j).size(); i++) {
				iTerms.add(null);
			}

			for(int k=0; k<sTermsList.get(j).size(); k++)
			{
				int domainIndex = predicateDomainMap.get(predicateSymbolIndex.get(j)).get(k);

				//if term is a constant must be a unique term
				if(isTermConstant(sTermsList.get(j).get(k)))
				{
					//find the id of the term
					int id=-1;
					for(int m=0;m<domainList.get(domainIndex).values.size();m++)
					{
						if(domainList.get(domainIndex).values.get(m).equals(sTermsList.get(j).get(k)))
						{
							id=m;
							break;
						}
					}
					if(id==-1)
					{
						System.out.println("Constant does not match predicate's domain. "  + domainList.get(domainIndex).name );
						System.exit(-1);
					}
					iTerms.set(k, new Term(0,id));
				}
				else
				{
					int domainSize = domainList.get(domainIndex).values.size();
					boolean isExistingTerm = false;
					boolean sameTerminPred = false;
					int atomIndex=-1;
					int termIndex=-1;
					//check in term lists for atoms 0 to j;
					for(int m=0;m<=j;m++)
					{
						for(int n=0; n < (m != j ? sTermsList.get(m).size() : k) ; n++)
						{
							if(sTermsList.get(m).get(n).equals(sTermsList.get(j).get(k)))
							{
								//check if the domains of the matched variables are the same
								int atomSymbolIndex1 = predicateSymbolIndex.get(m);
								int atomId1 = mln.symbols.get(atomSymbolIndex1).id;
								int domainListIndex1 = predicateDomainMap.get(atomId1).get(n);

								int atomSymbolIndex2 = predicateSymbolIndex.get(j);
								int atomId2 = mln.symbols.get(atomSymbolIndex2).id;
								int domainListIndex2 = predicateDomainMap.get(atomId2).get(k);
								if(!domainList.get(domainListIndex1).name.equals(domainList.get(domainListIndex2).name))
								{
									System.out.println("Error! variables do not match type ." + atomStrings[j] + "(" + domainList.get(domainListIndex1).name + ", " + domainList.get(domainListIndex2).name + ")" );
									System.exit(-1);
								}
								//variable is repeated, use the term created for atom m, term n
								isExistingTerm = true;
								atomIndex = m;
								termIndex = n;
								if(m == j)
								{
									sameTerminPred = true;
								}
								break;
							}
						}
						if(isExistingTerm)
							break;
					}
					
					if(sameTerminPred)
					{
						iTerms.set(k, iTerms.get(termIndex));
					}
					else if(isExistingTerm)
					{
						//use the terms created for previous atoms
						iTerms.set(k, iTermsList.get(atomIndex).get(termIndex));
					}
					else
					{
						//create a new Term
						iTerms.set(k, create_new_term(domainSize));
					}
				}
			}
			iTermsList.set(j, iTerms);
		}//j atoms
		WClause newClause = create_new_clause(predicateSymbolIndex,sign,iTermsList);
		newClause.weight = new LogDouble(weight, true);
		CNF.add(newClause);

		int formulaStartIndex = mln.clauses.size();

		for(int i=0;i<CNF.size();i++)
		{
			mln.clauses.add(CNF.get(i));
		}
		int formulaEndIndex = mln.clauses.size();
		mln.formulas.add(new Formula(formulaStartIndex,formulaEndIndex, new LogDouble(weight, true)));

	}

	void parseDomainString(String line)
	{
		String[] domainArr = line.split(EQUALSTO);
		String domainName = domainArr[0];

		String[] domValArr = domainArr[1].replace(LEFTFLOWER, "").replace(RIGHTFLOWER, "").split(COMMASEPARATOR);
		List<String> domainValues = new ArrayList<String>();
		for (int i = 0; i < domValArr.length; i++) {
			if(domValArr[i].equals(ELLIPSIS)) {
				Integer startNumber = Integer.parseInt(domValArr[i-1]);
				Integer endNumber   = Integer.parseInt(domValArr[i+1]);
				for (int j = startNumber+1; j < endNumber; j++) {
					domainValues.add(Integer.toString(j));
				}
			} else {
				domainValues.add(domValArr[i]);
			}
		}

		Domain domain = new Domain(domainName,domainValues);
		domainList.add(domain);
	}

	void parsePredicateString(String line)
	{
		String[] predArr = line.split(REGEX_ESCAPE_CHAR + LEFTPRNTH);
		String symbolName = predArr[0];
		String[] termNames = predArr[1].replace(RIGHTPRNTH, "").split(COMMASEPARATOR);

		List<Integer> var_types = new ArrayList<Integer>();
		for(int m=0; m < termNames.length; m++) {
			var_types.add(0);
		}
		//create a new predicate symbol
		PredicateSymbol p = new PredicateSymbol(predicateId,symbolName,var_types,LogDouble.ONE,LogDouble.ONE);
		//predicateList.push_back(p);
		mln.symbols.add(p);

		//Build the map for this predicate;
		//For predicateid, generate a List of domainIds that index Domains
		List<Integer> domainIndex = new ArrayList<Integer>();
		for(int i=0; i<termNames.length; i++)
		{
			int matchingIndex = -1;
			for(int j=0;j<domainList.size();j++)
			{
				if(termNames[i].equals(domainList.get(j).name))
				{
					matchingIndex = j;
					break;
				}
			}
			if(matchingIndex == -1)
			{
				System.out.println("Error! Domain does not exist for predicate. " + symbolName );
				System.exit(-1);
			}
			domainIndex.add(matchingIndex);
		}
		predicateDomainMap.put(predicateId, (ArrayList<Integer>) domainIndex);
		//increment predicateid
		predicateId++;
	}

	public ArrayList<Evidence> parseInputEvidenceFile(String filename) throws FileNotFoundException, PredicateNotFound
	{
		Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(filename))));
		ArrayList<Evidence> evidList = new ArrayList<Evidence>();
		while(scanner.hasNextLine()) {
			String line = scanner.nextLine().replaceAll("\\s","");

			if(line.isEmpty()) {
				continue;
			}
			
			evidList.add(parseEvidenceString(line));
		}
		scanner.close();
		return evidList;
	}
	private Evidence parseEvidenceString(String line) throws PredicateNotFound{
		String[] predArr = line.split(REGEX_ESCAPE_CHAR + LEFTPRNTH);
		String symbolName = predArr[0];
		boolean truthValue = (symbolName.charAt(0) != '!');
		if(!truthValue){
			symbolName = symbolName.substring(1);
		}
		String[] termNames = predArr[1].replace(RIGHTPRNTH, "").split(COMMASEPARATOR);
		ArrayList<Integer> values = new ArrayList<Integer>();
		for(String term : termNames){
			values.add(Integer.parseInt(term));
		}
		for(PredicateSymbol symbol : mln.symbols){
			if(symbolName.equals(symbol.symbol)){
				return new Evidence(MLN.create_new_symbol(symbol),values,truthValue);
			}
		}
		throw new PredicateNotFound("wrong predicate in evidence");
	}

	public void parseInputMLNFile(String filename) throws FileNotFoundException
	{
		Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(filename))));
		ParserState state = null;

		while(scanner.hasNextLine()) {
			String line = scanner.nextLine().replaceAll("\\s","");

			if(line.isEmpty()) {
				continue;
			}

			if(line.contains(DOMAINSTART)) {
				state = ParserState.Domain;
				continue;
			} else if (line.contains(PREDICATESTART)) {
				state = ParserState.Predicate;
				continue;
			} else if (line.contains(FORMULASTART)) {
				state = ParserState.Formula;
				continue;
			}

			switch (state) {
			case Domain:
				parseDomainString(line);
				break;

			case Predicate:
				parsePredicateString(line);
				mln.max_predicate_id = predicateId-1;
				break;

			case Formula:
				parseClausesString(line);
				break;

			default:
				break;
			}
		}

		scanner.close();
	}

	public void parseQueryMLNFile(String filename) throws FileNotFoundException
	{
		Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(filename))));
		int state = 0;

		String[] splitted;
		int tempPredId=0;
		while(scanner.hasNextLine()) {
			String line = scanner.nextLine();

			if(line.isEmpty()) {
				continue;
			}

			splitted=line.split(" ");
			if(splitted[1].equals("MAR")){
				// Set the property of the Atom
				mln.symbols.get(tempPredId).queryType=false;
			}
			else if(splitted[1].equals("MAP")){
				mln.symbols.get(tempPredId).queryType=true;
			}
			tempPredId++;
		}
		scanner.close();

        // Code to make mapping from variable id to Predicate Id
	}
}
