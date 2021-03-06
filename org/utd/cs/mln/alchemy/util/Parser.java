package org.utd.cs.mln.alchemy.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.*;

import org.utd.cs.gm.core.LogDouble;
import org.utd.cs.mln.alchemy.core.*;

public class Parser {
	public static final String DOMAINSTART = "#domains";
	public static final String VALUESSTART = "#values";
	public static final String PREDICATESTART = "#predicates";
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
    public static final String COMMENT = "//";
	
	private static final String REGEX_ESCAPE_CHAR = "\\";

    public static ArrayList<String> open_world = new ArrayList<>();
    public static ArrayList<String> closed_world = new ArrayList<>();
    public static ArrayList<String> hidden_world = new ArrayList<>();


    public Map<String, Set<Integer>> collectDomain(String files[]) throws FileNotFoundException {

        Map<String, Set<Integer>> varTypeToDomain = new HashMap<>();
        for(String file : files)
        {
            if(file != null)
            {
                Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(file))));
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine().replaceAll("\\s", "");

                    if (line.isEmpty()) {
                        continue;
                    }
                    String[] predArr = line.split(REGEX_ESCAPE_CHAR + LEFTPRNTH);
                    String symbolName = predArr[0];
                    String[] predArr2 = predArr[1].split(EQUALSTO);
                    String[] termNames = predArr2[0].replace(RIGHTPRNTH, "").split(COMMASEPARATOR);
                    for(PredicateSymbol symbol : mln.symbols)
                    {
                        if(symbol.symbol.equals((symbolName)))
                        {
                            for(int i = 0 ; i < termNames.length ; i++)
                            {
                                String var_type = symbol.variable_types.get(i);
                                if(!varTypeToDomain.containsKey(var_type))
                                {
                                    varTypeToDomain.put(var_type, new HashSet<Integer>());
                                }
                                varTypeToDomain.get(var_type).add(Integer.parseInt(termNames[i]));
                            }
                            break;
                        }
                    }

                }
                scanner.close();
            }
        }

        return varTypeToDomain;
    }

    public Set<String> createEvidenceSet(String evidenceFile) throws FileNotFoundException
    {
        Set<String> evidenceSet = new HashSet<String>();
        Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(evidenceFile))));
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().replaceAll("\\s", "");

            if (line.isEmpty()) {
                continue;
            }
            evidenceSet.add(line);

        }
        return evidenceSet;
    }

    public Map<String, Map<List<Integer>, Integer>> createEvidenceTable(String evidenceFile) throws FileNotFoundException {
        Map<String, Map<List<Integer>,Integer>> evidenceTable = new HashMap<>();
        Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(evidenceFile))));
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().replaceAll("\\s", "");

            if (line.isEmpty()) {
                continue;
            }
            String[] predArr = line.split(REGEX_ESCAPE_CHAR + LEFTPRNTH);
            String symbolName = predArr[0];
            String[] predArr2 = predArr[1].split(EQUALSTO);
            String[] termNames = predArr2[0].replace(RIGHTPRNTH, "").split(COMMASEPARATOR);
            String truthVal = predArr2[1];
            if(!evidenceTable.containsKey(symbolName))
            {
                evidenceTable.put(symbolName, new HashMap<List<Integer>,Integer>());
            }
            List<Integer> terms = new ArrayList<>();
            for(String str : termNames)
            {
                terms.add(Integer.parseInt(str));
            }
            evidenceTable.get(symbolName).put(terms, Integer.parseInt(truthVal));
        }
        return evidenceTable;
    }

    private enum ParserState {
		Domain,
		Values, // Added by Happy
		Predicate,
		Formula;
	};

	private MLN mln;

	private int predicateId;

	//set of domains with values in String format
	private List<Domain> domainList = new ArrayList<Domain>();

	//List of Values
	private List<Values> valuesList = new ArrayList<Values>();

	//map key:predicate Id, value:for each of its terms, Index into the domainList List
	private Map<Integer,ArrayList<Integer>> predicateDomainMap = new HashMap<Integer, ArrayList<Integer>>();


	public Parser(MLN mln_) {
		mln = mln_;
		mln_.domainList = domainList;
		mln_.predicateDomainMap = predicateDomainMap;
		predicateId = 0;
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
		Term term = new Term("0",iDomain);
		return term;
	}

	WClause create_new_clause(List<Integer> predicateSymbolIndex,List<Boolean> sign,
			List<List<Term> > iTermsList, List<Integer> valTrueList)
	{
		int numAtoms = predicateSymbolIndex.size();
		WClause clause = new WClause();
		clause.atoms = new ArrayList<Atom>(numAtoms);
		clause.satisfied = false;
		clause.sign = sign;
		clause.valTrue = valTrueList;
		for(int i=0;i<numAtoms;i++)
		{
			PredicateSymbol symbol = MLN.create_new_symbol(mln.symbols.get(predicateSymbolIndex.get(i)));
			List<Term> terms = iTermsList.get(i);
			for(int j = 0 ; j < terms.size() ; j++)
            {
                terms.get(j).type = new String(symbol.variable_types.get(j));
            }
			Atom atom = new Atom(symbol,terms);
			clause.atoms.add(atom);
		}
		return clause;
	}

    private Formula create_new_formula(Double weight, List<Integer> predicateSymbolIndex, List<Boolean> sign,
                                       List<Integer> valTrueList, List<List<Term>> iTermsList,
                                       List<Integer> clausePartitionIndex)
    {
        List<WClause> CNF = new ArrayList<WClause>();
        for(int i = 0 ; i < clausePartitionIndex.size()-1 ; i++)
        {
            int from = clausePartitionIndex.get(i);
            int to = clausePartitionIndex.get(i+1);
            WClause new_clause = create_new_clause(predicateSymbolIndex.subList(from,to), sign.subList(from,to), iTermsList.subList(from, to), valTrueList.subList(from,to));
            new_clause.weight = new LogDouble(weight, true);
            CNF.add(new_clause);
        }
        Formula formula = new Formula(CNF, new LogDouble(weight, true));
        return formula;
    }

	// Parse formula string. String ex : !S(x)=1 ^ C(x)=3 | F(x,y)=1::5
    // This will be 2 clauses : 1. !S(x)=1, 2. C(x)=3 V F(x,y)=1, each with weight 5
    // This formula is true if (i) S(x) is not 1, and (ii) Either C(x) is 3 or F(x,y)=1

	void parseCNFString(String line)
	{
        String[] formulaArr = line.split(WEIGHTSEPARATOR);
        Double weight = Double.parseDouble(formulaArr[1]);
        List<Integer> clausePartitionIndex = new ArrayList<>(); // Stores list of indexes of atoms at which clause partitions. For example, if formula a | b ^ c ^ d|e, then this list will be [0,2,3,5] i.e. partition at atom b, then atom c, and then atom e.
        clausePartitionIndex.add(0);
        String formulaString = formulaArr[0];

        //If a formula starts with parenthesis, remove it
        if(formulaString.startsWith(LEFTPRNTH)) {

            if(!formulaString.endsWith(RIGHTPRNTH)) {
                System.out.println("Missing right parenthesis in clause " + formulaString);
                System.exit(-1);
            }

            formulaString = formulaString.substring(1, formulaString.length() - 1);
        }

        List<Boolean> sign = new ArrayList<Boolean>();
        List<Integer> valTrueList = new ArrayList<>();
        List<Integer> predicateSymbolIndex = new ArrayList<Integer>();
        List<List<String>> sTermsList = new ArrayList<List<String>>();

        String[] clauseStrings = formulaString.split(REGEX_ESCAPE_CHAR + ANDOPERATOR);
        ArrayList<String> atomStrings = new ArrayList<>();
        for(String clauseString : clauseStrings) {
            String[] clauseAtomStrings = clauseString.split(REGEX_ESCAPE_CHAR + OROPERATOR);
            for (int i = 0; i < clauseAtomStrings.length; i++) {
                atomStrings.add(clauseAtomStrings[i]);
            }
            clausePartitionIndex.add(atomStrings.size());
        }

        for (int i = 0; i < atomStrings.size(); i++) {
            sign.add(false);
            predicateSymbolIndex.add(null);
        }

        for(int i=0; i<atomStrings.size(); i++) {
            String[] atomStrings1 = atomStrings.get(i).split(REGEX_ESCAPE_CHAR + EQUALSTO);
            String atomString = atomStrings1[0];
            int valTrue = Integer.parseInt(atomStrings1[1]);
            valTrueList.add(valTrue);
            //find opening and closing braces
            int startpos = atomString.indexOf(LEFTPRNTH);
            String predicateName = atomString.substring(0, startpos);

            if(predicateName.startsWith(NOTOPERATOR))
            {
                //negation
                predicateName = predicateName.substring(1,predicateName.length());
                sign.set(i, true);
            }

            for (int k = 0; k < mln.symbols.size(); k++) {
                //found the predicate
                if (mln.symbols.get(k).symbol.equals(predicateName)) {
                    predicateSymbolIndex.set(i, k);
                    break;
                }
            }

            int endpos = atomString.indexOf(RIGHTPRNTH);
            String termsString = atomString.substring(startpos + 1, endpos);
            //System.out.println("termsString = "+termsString);
            String[] terms = termsString.split(COMMASEPARATOR);
            sTermsList.add(new ArrayList<String>(Arrays.asList(terms)));

            //check if the number of terms is equal to the declared predicate
            if (terms.length != mln.symbols.get(predicateSymbolIndex.get(i)).variable_types.size()) {
                System.out.println("Error! Number/domain of terms in the predicate delcaration does not match in formula. " + predicateName);
                System.exit(-1);
            }
        }

        //create required terms
        List<List<Term> > iTermsList = new ArrayList<List<Term>>();
        for (int i = 0; i < atomStrings.size(); i++) {
            iTermsList.add(null);
        }
        for(int j=0;j<atomStrings.size();j++)
        {
            //for each term of atom j, check if it has already appeared in previous atoms of formula
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
                    iTerms.set(k, new Term("0",id));
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
                                    System.out.println("Error! variables do not match type ." + atomStrings.get(j) + "(" + domainList.get(domainListIndex1).name + ", " + domainList.get(domainListIndex2).name + ")" );
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

        Formula newFormula = create_new_formula(weight, predicateSymbolIndex, sign, valTrueList, iTermsList, clausePartitionIndex);
        newFormula.formulaId = mln.formulas.size();
        mln.formulas.add(newFormula);
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

	// Added by Happy
    private void parseValuesString(String line) {
        String[] valuesArr = line.split(EQUALSTO);
        String valuesName = valuesArr[0];

        String[] valuesValArr = valuesArr[1].replace(LEFTFLOWER, "").replace(RIGHTFLOWER, "").split(COMMASEPARATOR);
        ArrayList<Integer> valuesVals = new ArrayList<Integer>();
        for (int i = 0; i < valuesValArr.length; i++) {
            if(valuesValArr[i].equals(ELLIPSIS)) {
                Integer startNumber = Integer.parseInt(valuesValArr[i-1]);
                Integer endNumber   = Integer.parseInt(valuesValArr[i+1]);
                for (int j = startNumber+1; j < endNumber; j++) {
                    valuesVals.add(j);
                }
            } else {
                valuesVals.add(Integer.parseInt(valuesValArr[i]));
            }
        }

        Values values = new Values(valuesName, valuesVals);
        valuesList.add(values);
    }

	void parsePredicateString(String line)
	{
		String[] predArr = line.split(REGEX_ESCAPE_CHAR + LEFTPRNTH);
		String symbolName = predArr[0];
		String[] predArr2 = predArr[1].split(EQUALSTO);
		String valuesName = predArr2[1];
		String[] termNames = predArr2[0].replace(RIGHTPRNTH, "").split(COMMASEPARATOR);

		List<String> var_types = new ArrayList<>();
		for(int m=0; m < termNames.length; m++) {
			var_types.add(termNames[m]);
		}

		int matchingIndex = -1;
		for(int i = 0 ; i < valuesList.size() ; i++)
        {
            if(valuesName.equals(valuesList.get(i).name))
            {
                matchingIndex = i;
                break;
            }
        }
        if(matchingIndex == -1)
        {
            System.out.println("Error! Value name does not exist for predicate. " + symbolName );
            System.exit(-1);
        }
		//create a new predicate symbol
		PredicateSymbol p = new PredicateSymbol(predicateId,symbolName,var_types, valuesList.get(matchingIndex), LogDouble.ONE,LogDouble.ONE);
		//predicateList.push_back(p);
		if(closed_world.contains(p.symbol))
        {
            p.world = PredicateSymbol.WorldState.closed;
        }
        else if(hidden_world.contains(p.symbol))
        {
            p.world = PredicateSymbol.WorldState.hidden;
        }
        else
        {
            p.world = PredicateSymbol.WorldState.open;
        }
        mln.symbols.add(p);

		//Build the map for this predicate;
		//For predicateid, generate a List of domainIds that index Domains
		List<Integer> domainIndex = new ArrayList<Integer>();
		for(int i=0; i<termNames.length; i++)
		{
			matchingIndex = -1;
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


	public void parseInputMLNFile(String filename) throws FileNotFoundException
	{
		Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(filename))));
		ParserState state = null;

		while(scanner.hasNextLine()) {
			String line = scanner.nextLine().replaceAll("\\s","");

			if(line.isEmpty() || line.contains(COMMENT)) {
				continue;
			}

			if(line.contains(DOMAINSTART)) {
				state = ParserState.Domain;
				continue;
			} else if(line.contains(VALUESSTART)) {
                state = ParserState.Values;
                continue;
            }
			else if (line.contains(PREDICATESTART)) {
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

            case Values:
                parseValuesString(line);
                break;

			case Predicate:
				parsePredicateString(line);
				mln.max_predicate_id = predicateId-1;
				break;

			case Formula:
				parseCNFString(line);
				break;

			default:
				break;
			}
		}

		scanner.close();
	}

    public Evidence parseEvidence(GroundMLN groundMln, String evidence_file) throws FileNotFoundException {
        Evidence evidence = new Evidence();
        if(evidence_file == null)
            return evidence;
        Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(evidence_file))));
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().replaceAll("\\s", "");

            if (line.isEmpty()) {
                continue;
            }
            String[] predArr = line.split(REGEX_ESCAPE_CHAR + LEFTPRNTH);
            String symbolName = predArr[0];
            String[] predArr2 = predArr[1].split(EQUALSTO);
            String value = predArr2[1];
            String[] termNames = predArr2[0].replace(RIGHTPRNTH, "").split(COMMASEPARATOR);

            GroundPredicate gp = new GroundPredicate();
            // Find groundPredicate symbol with string=symbolname
            for(GroundPredicateSymbol sym : groundMln.symbols)
            {
                if(sym.symbol.equals(symbolName))
                {
                    gp.symbol = new GroundPredicateSymbol(sym.id,symbolName, sym.values, sym.world, sym.variable_types);
                    break;
                }
            }
            for(String term : termNames)
            {
                gp.terms.add(Integer.parseInt(term));
            }

            int gpIndex = groundMln.groundPredicates.indexOf(gp);
            evidence.predIdVal.put(gpIndex, Integer.parseInt(value));
        }
        return evidence;
    }

}
