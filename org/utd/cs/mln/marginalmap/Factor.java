package org.utd.cs.mln.marginalmap;


import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by vishal on 12/10/16.
 */


public class Factor {

    ArrayList<String> scope;
    HashMap<HashSet<String>,Double> factorTable;
    //public String[] characters={"d","o","i","r","a","h","t","n","s","e"};
    public static String[] characters={"e","t","i","o","a","n","s","h","r","d"};

    public Factor(HashMap<HashSet<String>,Double> factorTable,ArrayList<String> scope){
        this.scope=scope;
        this.factorTable=factorTable;
    }

    public Factor(ArrayList<String> scope){
        this.scope=new ArrayList<>(scope);
        this.factorTable=null;
    }

    public Factor(){
        this.scope=new ArrayList<>();
        this.factorTable=null;
    }

    // create a factor from a keyset and
    public static Factor createNewFactor(ArrayList<String> scope){
        Factor result=null;
        return result;
    }

    public static Factor createIdentityFactor(ArrayList<String > scope){
        return new Factor(Factor.createFactorTableFromScope(scope),scope);
    }

    public static ArrayList<String> mergeScopes(ArrayList<String> s1,ArrayList<String> s2){
        ArrayList<String> newScope=new ArrayList<>(s1);
        for (int i = 0; i < s2.size(); i++) {
            if (!s1.contains(s2.get(i))){
                newScope.add(s2.get(i));
            }
        }
        return newScope;
    }

    public static ArrayList<String> mergeScopes(ArrayList<ArrayList<String>> allScopes){
        ArrayList<String> newScope=new ArrayList<>(allScopes.get(0));
        for (int i = 1; i < allScopes.size(); i++) {
            newScope=mergeScopes(newScope,allScopes.get(i));
        }
        return newScope;
    }

    public static ArrayList<String> intersectionOfScopes(ArrayList<String> s1,ArrayList<String> s2){
        ArrayList<String> newScope=new ArrayList<>();
        for (int i = 0; i < s2.size(); i++) {
            if (s1.contains(s2.get(i))){
                newScope.add(s2.get(i));
            }
        }
        return newScope;
    }

    // Calculates s1-s2
    public static ArrayList<String> differenceOfScopes(ArrayList<String> s1,ArrayList<String> s2){
        ArrayList<String> newScope=new ArrayList<>();
        for (int i = 0; i < s1.size(); i++) {
            if (!s2.contains(s1.get(i))){
                newScope.add(s1.get(i));
            }
        }
        return newScope;
    }

    // Returns a factor table with all possible assignments and corresponding value as 1.
    public static HashMap<HashSet<String>,Double> createFactorTableFromScope(ArrayList<String> scope){
        ArrayList<HashSet<String>> allAssignments=Assignment3.getSetPossibleValues(Assignment3.characters,scope);
        HashMap<HashSet<String>,Double> newFactorTable=new HashMap<>();

        for (int i = 0; i < allAssignments.size(); i++) {
            //System.out.println("Entering Assignment = "+ allAssignments.get(i));
            newFactorTable.put(allAssignments.get(i),1.0);
        }
        return newFactorTable;
    }

    public static HashMap<HashSet<String>,Double> createFactorForSampling(ArrayList<String> scope, ArrayList<String[]> possibleValues,double initValue){
        ArrayList<HashSet<String>> allAssignments=Factor.getSetPossibleValues(scope,possibleValues,0);
        HashMap<HashSet<String>,Double> newFactorTable=new HashMap<>();

        for (int i = 0; i < allAssignments.size(); i++) {
            //System.out.println("Entering Assignment = "+ allAssignments.get(i));
            newFactorTable.put(allAssignments.get(i),initValue);
        }
        return newFactorTable;
    }

    public static Factor multiplyFactors(Factor f1, Factor f2){
        if (f1==null || f1.factorTable==null)
            return f2;
        else if (f2==null || f2.factorTable==null)
            return f1;

        Factor tempFactor1;
        if (f1.scope.size()<f2.scope.size()){
            tempFactor1=f1;
            f1=f2;
            f2=tempFactor1;
        }
        //System.out.println("Size of f1= "+f1.factorTable.size());
        //System.out.println("Size of f2= "+f2.factorTable.size());
        //System.out.println("Multiplying factors: ");
        //System.out.println(f1.scope);
        //System.out.println(f2.scope);

        ArrayList<String> newScope=mergeScopes(f1.scope,f2.scope);
        //System.out.println("New Scope is= "+newScope);
        HashMap<HashSet<String>,Double> newFactorTable=createFactorTableFromScope(newScope);
        //System.out.println("Size of new factor= "+newFactorTable.size());

        //System.out.println("Size of merged scope= "+newFactorTable.size());
        //Iterate through f1 and multiply rows with subset key with corresponding value
        ArrayList<HashSet<String>> commonRows;

        // Getting all assignments of variables in f1
        HashMap<HashSet<String>,Double> f1Table=f1.getFactorTable();
        Iterator assignmentsOfF1=f1Table.entrySet().iterator();
        //System.out.println("Size of all assignments of F1= "+f1Table.size());

        HashSet<String> currentAssignmentF1;

        Double previousValue=0.0,f1Value;
        double tempdouble;
        //System.out.println("test");
        while (assignmentsOfF1.hasNext()){
            Map.Entry pair=(Map.Entry)assignmentsOfF1.next();
            //System.out.println("Pair found is: "+pair);
            currentAssignmentF1=(HashSet<String>) pair.getKey();
            commonRows=getRowswithVar(newFactorTable,currentAssignmentF1);
            f1Value=f1Table.get(currentAssignmentF1);
            //System.out.println("Checking fro: "+currentAssignmentF1);
            //System.out.println("Size of common Rows= "+commonRows.size());
            //System.out.println(commonRows);
            for (int i = 0; i < commonRows.size(); i++) {
                previousValue=(newFactorTable.get(commonRows.get(i)));
                newFactorTable.put(commonRows.get(i),Math.exp(Math.log(previousValue )+Math.log(f1Value)));
            }
        }
        //System.out.println("T");
        //Iterate through f1 and multiply rows with subset key with corresponding value
        ArrayList<HashSet<String>> commonRowsF2;

        // Getting all assignments of variables in f1
        HashMap<HashSet<String>,Double> f2Table=f2.getFactorTable();
        Iterator assignmentsOfF2=f2Table.entrySet().iterator();

        HashSet<String> currentAssignmentF2;

        Double previousValueF2=0.0,f2Value;
        while (assignmentsOfF2.hasNext()){
            Map.Entry pair=(Map.Entry)assignmentsOfF2.next();
            currentAssignmentF2=(HashSet<String>)pair.getKey();
            commonRowsF2=getRowswithVar(newFactorTable,currentAssignmentF2);
            f2Value=f2Table.get(currentAssignmentF2);
            //System.out.println("Checking fro: "+currentAssignmentF2);
            //System.out.println("Size of common Rows= "+commonRowsF2.size());
            //System.out.println(commonRowsF2);
            for (int i = 0; i < commonRowsF2.size(); i++) {
                previousValueF2=(newFactorTable.get(commonRowsF2.get(i)));
                newFactorTable.put(commonRowsF2.get(i),Math.exp(Math.log(previousValueF2)+Math.log(f2Value)));
            }
        }

        Factor newFactor=new Factor(newFactorTable,newScope);
        return newFactor;
    }


    public static Factor multiplyFactors(Factor f1, Factor f2, ArrayList<String[]> possibleValues,double initValue){
        if (f1==null || f1.factorTable==null)
            return f2;
        else if (f2==null || f2.factorTable==null)
            return f1;

        Factor tempFactor1;
        if (f1.scope.size()<f2.scope.size()){
            tempFactor1=f1;
            f1=f2;
            f2=tempFactor1;
        }
        //System.out.println("Size of f1= "+f1.factorTable.size());
        //System.out.println("Size of f2= "+f2.factorTable.size());
        //System.out.println("Multiplying factors: ");
        //System.out.println(f1.scope);
        //System.out.println(f2.scope);

        ArrayList<String> newScope=mergeScopes(f1.scope,f2.scope);
        //System.out.println("New Scope is= "+newScope);
        HashMap<HashSet<String>, Double> newFactorTable;
        if (initValue==1){
            newFactorTable = createFactorForSampling(newScope,possibleValues,1);
        }
        else {
            newFactorTable = createFactorTableFromScope(newScope);
        }
        //System.out.println("Size of new factor= "+newFactorTable.size());

        //System.out.println("Size of merged scope= "+newFactorTable.size());
        //Iterate through f1 and multiply rows with subset key with corresponding value
        ArrayList<HashSet<String>> commonRows;

        // Getting all assignments of variables in f1
        HashMap<HashSet<String>,Double> f1Table=f1.getFactorTable();
        Iterator assignmentsOfF1=f1Table.entrySet().iterator();
        //System.out.println("Size of all assignments of F1= "+f1Table.size());

        HashSet<String> currentAssignmentF1;

        Double previousValue=0.0,f1Value;
        double tempdouble;
        //System.out.println("test");
        while (assignmentsOfF1.hasNext()){
            Map.Entry pair=(Map.Entry)assignmentsOfF1.next();
            //System.out.println("Pair found is: "+pair);
            currentAssignmentF1=(HashSet<String>) pair.getKey();
            commonRows=getRowswithVar(newFactorTable,currentAssignmentF1);
            f1Value=f1Table.get(currentAssignmentF1);
            //System.out.println("Checking fro: "+currentAssignmentF1);
            //System.out.println("Size of common Rows= "+commonRows.size());
            //System.out.println(commonRows);
            for (int i = 0; i < commonRows.size(); i++) {
                previousValue=(newFactorTable.get(commonRows.get(i)));
                //System.out.println(Math.log(previousValue ));
                newFactorTable.put(commonRows.get(i),Math.exp(Math.log(previousValue )+Math.log(f1Value)));
            }
        }
        //System.out.println("T");
        //Iterate through f1 and multiply rows with subset key with corresponding value
        ArrayList<HashSet<String>> commonRowsF2;

        // Getting all assignments of variables in f1
        HashMap<HashSet<String>,Double> f2Table=f2.getFactorTable();
        Iterator assignmentsOfF2=f2Table.entrySet().iterator();

        HashSet<String> currentAssignmentF2;

        Double previousValueF2=0.0,f2Value;
        while (assignmentsOfF2.hasNext()){
            Map.Entry pair=(Map.Entry)assignmentsOfF2.next();
            currentAssignmentF2=(HashSet<String>)pair.getKey();
            commonRowsF2=getRowswithVar(newFactorTable,currentAssignmentF2);
            f2Value=f2Table.get(currentAssignmentF2);
            //System.out.println("Checking fro: "+currentAssignmentF2);
            //System.out.println("Size of common Rows= "+commonRowsF2.size());
            //System.out.println(commonRowsF2);
            for (int i = 0; i < commonRowsF2.size(); i++) {
                previousValueF2=(newFactorTable.get(commonRowsF2.get(i)));
                newFactorTable.put(commonRowsF2.get(i),Math.exp(Math.log(previousValueF2)+Math.log(f2Value)));
            }
        }

        Factor newFactor=new Factor(newFactorTable,newScope);
        return newFactor;
    }

    // Implements f1/f2
    public static Factor divideFactors(Factor f1, Factor f2){
        if (f2==null || f2.factorTable==null)
            return f1;

        Factor newFactor=new Factor(f1.scope);
        newFactor.factorTable=new HashMap<>();
        HashMap<HashSet<String>,Double> newFactorTable=newFactor.factorTable;
        newFactorTable.putAll(f1.factorTable);

        ArrayList<HashSet<String>> commonRowsF2;
        // Getting all assignments of variables in f2
        HashMap<HashSet<String>,Double> f2Table=f2.getFactorTable();
        Iterator assignmentsOfF2=f2Table.entrySet().iterator();

        HashSet<String> currentAssignmentF2;

        Double previousValueF2=0.0,f2Value;
        while (assignmentsOfF2.hasNext()){
            Map.Entry pair=(Map.Entry)assignmentsOfF2.next();
            currentAssignmentF2=(HashSet<String>)pair.getKey();
            commonRowsF2=getRowswithVar(newFactorTable,currentAssignmentF2);
            f2Value=f2Table.get(currentAssignmentF2);
            //System.out.println("Checking fro: "+currentAssignmentF2);
            //System.out.println("Size of common Rows= "+commonRowsF2.size());
            //System.out.println(commonRowsF2);
            for (int i = 0; i < commonRowsF2.size(); i++) {
                previousValueF2=(newFactorTable.get(commonRowsF2.get(i)));
                if (previousValueF2!=0.0) {
                    newFactorTable.put(commonRowsF2.get(i), Math.exp(Math.log(previousValueF2)-Math.log(f2Value)));
                }
            }
        }
        return newFactor;
    }

    public static ArrayList<HashSet<String>> getRowswithVar(HashMap<HashSet<String>,Double> start, HashSet<String> keysToFind){

        ArrayList<HashSet<String>> rows=new ArrayList<>();

        Iterator it=start.entrySet().iterator();
        HashSet<String> currentKey;
        boolean flag;
        while (it.hasNext()){
            flag=false;
            currentKey=(HashSet<String>)((Map.Entry) it.next()).getKey();
            if(currentKey.containsAll(keysToFind)){
                rows.add(currentKey);
            }
        }
        return rows;
    }

    public static Factor marginalizeOverSet(Factor f, ArrayList<String> variablesToEliminate, int type){
        if(type==1) { // sum Product
            if (variablesToEliminate == null || variablesToEliminate.size() == 0)
                return f;
            //Factor.printFactor(f);

            HashMap<HashSet<String>, Double> originalFactorTable = f.getFactorTable();

            //Create a new Factor
            ArrayList<String> newScope = Factor.differenceOfScopes(f.scope, variablesToEliminate);
            //System.out.println(" New Scope = "+newScope);
            HashMap<HashSet<String>, Double> newFactorTable = createFactorTableFromScope(newScope);
            Factor newFactor = new Factor(newFactorTable, newScope);

            // Get the set of all possible assignments for variables to be eliminated
            // OPTIMIZATION: THIS CALL CAN BE AVOIDED if we iterate over hash
            ArrayList<HashSet<String>> assignmentsToMerge = Assignment3.getSetPossibleValues(Assignment3.characters, newScope);

            // Take sum over all rows in original Hashtable for a perticular assignment to be eliminated.


            ArrayList<HashSet<String>> commonRows;
            HashSet<String> currentAssignment;

            Double previousValue = 0.0, f1Value;

            Iterator it = newFactor.getFactorTable().entrySet().iterator();
            double sum = 0.0;
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                currentAssignment = (HashSet<String>) pair.getKey();
                commonRows = getRowswithVar(originalFactorTable, currentAssignment);

                //System.out.println("Common Rows size= "+commonRows.size());
                sum = 0.0;
                for (int i = 0; i < commonRows.size(); i++) {
                    //System.out.println("Looking for= "+ commonRows.get(i));
                    sum += (originalFactorTable.get(commonRows.get(i)));
                }
                newFactorTable.put(currentAssignment, sum);
            }
            return newFactor;
        }
        if(type==2) { // Max Product
            if (variablesToEliminate == null || variablesToEliminate.size() == 0)
                return f;
            //Factor.printFactor(f);

            HashMap<HashSet<String>, Double> originalFactorTable = f.getFactorTable();

            //Create a new Factor
            ArrayList<String> newScope = Factor.differenceOfScopes(f.scope, variablesToEliminate);
            //System.out.println(" New Scope = "+newScope);
            HashMap<HashSet<String>, Double> newFactorTable = createFactorTableFromScope(newScope);
            Factor newFactor = new Factor(newFactorTable, newScope);

            // Get the set of all possible assignments for variables to be eliminated
            // OPTIMIZATION: THIS CALL CAN BE AVOIDED if we iterate over hash
            ArrayList<HashSet<String>> assignmentsToMerge = Assignment3.getSetPossibleValues(Assignment3.characters, newScope);

            // Take sum over all rows in original Hashtable for a perticular assignment to be eliminated.


            ArrayList<HashSet<String>> commonRows;
            HashSet<String> currentAssignment;

            Double previousValue = 0.0, f1Value;

            Iterator it = newFactor.getFactorTable().entrySet().iterator();
            double maxVal = Double.MIN_VALUE;
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                currentAssignment = (HashSet<String>) pair.getKey();
                commonRows = getRowswithVar(originalFactorTable, currentAssignment);

                //System.out.println("Common Rows size= "+commonRows.size());
                maxVal = Double.MIN_VALUE;
                for (int i = 0; i < commonRows.size(); i++) {
                    //System.out.println("Looking for= "+ commonRows.get(i));
                    maxVal= Math.max(maxVal,originalFactorTable.get(commonRows.get(i)));
                }
                newFactorTable.put(currentAssignment, maxVal);
            }
            return newFactor;
        }
        return null;
    }

    public static Factor createOCRFactor(String x1, int imgId){
        String img=Integer.toString(imgId);
        ArrayList<String> scope=new ArrayList<>();
        scope.add(x1);

        String[] possibilities=Assignment3.permutations(Assignment3.characters,2);
        HashSet<String> tempAssignment;
        String[] splitted;
        HashMap<HashSet<String>,Double> factorTable=new HashMap<>();

        double tempValue;
        String currAssignment;
        for (int i = 0; i < Assignment3.characters.length; i++) {
            currAssignment=Assignment3.characters[i];
            tempAssignment=new HashSet<>();
            tempAssignment.add(x1+"="+currAssignment);
            tempValue=Assignment3.ocr_potentials.get(img+currAssignment);
            factorTable.put(tempAssignment,tempValue);
        }
        Factor f=new Factor(factorTable,scope);
        return f;
    }

    public static Factor createTransFactor(String x1, String x2){
        ArrayList<String> scope=new ArrayList<>();
        scope.add(x1);
        scope.add(x2);

        String[] possibilities=Assignment3.permutations(Assignment3.characters,2);
        HashSet<String> tempAssignment;
        String[] splitted;
        HashMap<HashSet<String>,Double> factorTable=new HashMap<>();

        double tempValue;
        for (int i = 0; i < possibilities.length; i++) {
            splitted=possibilities[i].split("");
            tempAssignment=new HashSet<>();
            tempAssignment.add(x1+"="+splitted[0]);
            tempAssignment.add(x2+"="+splitted[1]);
            tempValue=Assignment3.trans_potentials.get(possibilities[i]);
            factorTable.put(tempAssignment,tempValue);
        }
        Factor f=new Factor(factorTable,scope);
        return f;
    }

    public static Factor createSkipFactor(String x1, String x2){
        ArrayList<String> scope=new ArrayList<>();
        scope.add(x1);
        scope.add(x2);

        String[] possibilities=Assignment3.permutations(Assignment3.characters,2);
        HashSet<String> tempAssignment;
        String[] splitted;
        HashMap<HashSet<String>,Double> factorTable=new HashMap<>();

        for (int i = 0; i < possibilities.length; i++) {
            splitted=possibilities[i].split("");
            tempAssignment=new HashSet<>();
            tempAssignment.add(x1+"="+splitted[0]);
            tempAssignment.add(x2+"="+splitted[1]);
            if (splitted[0].equals(splitted[1])){
                factorTable.put(tempAssignment,Assignment3.skipValue);
            }
            else {
                factorTable.put(tempAssignment,Assignment3.defaultSkipValue);
            }
        }
        Factor f=new Factor(factorTable,scope);
        return f;
    }

    public static Factor createCrossFactor(String x1, String x2){
        ArrayList<String> scope=new ArrayList<>();
        scope.add(x1);
        scope.add(x2);

        String[] possibilities=Assignment3.permutations(Assignment3.characters,2);
        HashSet<String> tempAssignment;
        String[] splitted;
        HashMap<HashSet<String>,Double> factorTable=new HashMap<>();

        for (int i = 0; i < possibilities.length; i++) {
            splitted=possibilities[i].split("");
            tempAssignment=new HashSet<>();
            tempAssignment.add(x1+"="+splitted[0]);
            tempAssignment.add(x2+"="+splitted[1]);
            if (splitted[0].equals(splitted[1])){
                factorTable.put(tempAssignment,Assignment3.crossValue);
            }
            else {
                factorTable.put(tempAssignment,Assignment3.defaultCrossValue);
            }
        }
        Factor f=new Factor(factorTable,scope);
        return f;
    }

    /*
    public static ArrayList<HashSet<String>> getRowswithVar(HashMap<HashSet<String>,Double> start, ArrayList<String> keysToFind){

        ArrayList<HashSet<String>> rows=new ArrayList<>();

        Iterator it=start.entrySet().iterator();
        HashSet<String> currentKey;
        boolean flag;
        while (it.hasNext()){
            flag=false;
            currentKey=(HashSet<String>)((Map.Entry) it.next()).getKey();
            for (int i = 0; i < keysToFind.size(); i++) {
                if (currentKey.contains(keysToFind.get(i))){
                    flag=true;
                }
            }
            if (flag){
                rows.add(currentKey);
            }

        }
        return rows;
    }
     */

    public static String getBestMarginal(Factor f){
        Iterator it=f.factorTable.entrySet().iterator();
        double max=Double.MIN_VALUE;
        HashSet<String> result=null;
        while (it.hasNext()){
            Map.Entry pair=(Map.Entry)it.next();
            if (((double)pair.getValue())>=max){
                max=(double)pair.getValue();
                result=(HashSet<String>)pair.getKey();
            }
        }
        Object[] temp=result.toArray();
        return ((String)temp[0]).split("=")[1];

    }

    public static void printFactor(Factor f){
        System.out.println("\nFACTOR==========");
        if (f==null)
            System.out.println("Found f as null");
        System.out.println("Scope: "+f.scope);
        Iterator it=f.getFactorTable().entrySet().iterator();
        while (it.hasNext()){
            System.out.println(it.next());
        }
    }

    public static Factor normalize(Factor f){
        if (f==null || f.factorTable==null)
            return f;
        HashMap<HashSet<String>,Double> factorTable=f.factorTable;

        double z=0.0;
        Iterator it=f.factorTable.entrySet().iterator();
        while (it.hasNext()){
            Map.Entry pair=(Map.Entry)it.next();
            z+=(double)pair.getValue();
        }

        double normalizedValue;
        it=f.factorTable.entrySet().iterator();
        while (it.hasNext()){
            Map.Entry pair=(Map.Entry)it.next();
            normalizedValue=(Math.exp(Math.log((double)pair.getValue())-Math.log(z)));
            factorTable.put((HashSet<String>)pair.getKey(),normalizedValue);
        }
        return f;
    }

    public ArrayList<String> getScope() {
        return scope;
    }

    public void setScope(ArrayList<String> scope) {
        this.scope = scope;
    }

    public HashMap<HashSet<String>, Double> getFactorTable() {
        return factorTable;
    }

    public void setFactorTable(HashMap<HashSet<String>, Double> factorTable) {
        this.factorTable = factorTable;
    }


    /*

    ==========================================================================
    For Assignment 3

     */

    // Reduces the factor according to the given evidence.
    // If the evidence variable is not in the scope of factor to be reduced then we ignore it.
    public static Factor reduceFactor(Factor f, String variableBeingSampled, ArrayList<String> evidenceVariables, ArrayList<String> evidenceAssignments){
        if(f ==null || f.getFactorTable()==null)
            return f;
        HashSet<String> evidenceToReduceOn=new HashSet<>();
        String nextEvidence;
        for (int i = 0; i < evidenceVariables.size(); i++) {
            nextEvidence=evidenceVariables.get(i);
            // Take all values except for the one being sampled and that don't exist in this factor's scope
            if (!variableBeingSampled.equals(nextEvidence) && f.scope.contains(nextEvidence)) {
                evidenceToReduceOn.add(nextEvidence + "=" + evidenceAssignments.get(i));
            }
        }
        ArrayList<String> newScope=new ArrayList<>();
        newScope.add(variableBeingSampled);
        Factor newFactor=new Factor(newScope);
        // Right now factor table is empty
        newFactor.factorTable=new HashMap<>();

        // Get the rows with the given evidence
        ArrayList<HashSet<String>> commonRows;
        HashSet<String> nextRow;
        commonRows=getRowswithVar(f.factorTable,evidenceToReduceOn);
        // Now we pick up all those rows and put them in the new table
        for (int i = 0; i < commonRows.size(); i++) {
            nextRow=commonRows.get(i);
            newFactor.factorTable.put(reduceScopeForEvidence(nextRow,variableBeingSampled),f.factorTable.get(nextRow));
        }


        // The factor is now reduced to the given evidence.
        return newFactor;
    }

    public static Factor reduceFactorWhole(Factor f, ArrayList<String> evidenceVariables, ArrayList<String> evidenceAssignments){
        if(f ==null || f.getFactorTable()==null)
            return f;
        HashSet<String> evidenceToReduceOn=new HashSet<>();
        String nextEvidence;
        for (int i = 0; i < evidenceVariables.size(); i++) {
            nextEvidence=evidenceVariables.get(i);
            // Take all values except for the one being sampled and that don't exist in this factor's scope
            if (f.scope.contains(nextEvidence)) {
                evidenceToReduceOn.add(nextEvidence + "=" + evidenceAssignments.get(i));
            }
        }
        Factor newFactor=new Factor(f.scope);
        // Right now factor table is empty
        newFactor.factorTable=new HashMap<>();

        // Get the rows with the given evidence
        ArrayList<HashSet<String>> commonRows;
        HashSet<String> nextRow;
        commonRows=getRowswithVar(f.factorTable,evidenceToReduceOn);
        // Now we pick up all those rows and put them in the new table
        for (int i = 0; i < commonRows.size(); i++) {
            nextRow=commonRows.get(i);
            newFactor.factorTable.put(nextRow,f.factorTable.get(nextRow));
        }

        // The factor is now reduced to the given evidence.
        return newFactor;
    }

    public static HashSet<String> reduceScopeForEvidence(HashSet<String> h, String variableToSample){
        HashSet<String> res=new HashSet<>();
        String temp;
        Iterator it=h.iterator();
        while (it.hasNext()){
            temp=(String)it.next();
            if (temp.contains(variableToSample+"=")){
                res.add(temp);
            }
        }
        return res;
    }

    public static ArrayList<HashSet<String>> getSetPossibleValues(ArrayList<String> scope, ArrayList<String[]> possibleValues, int index){
        ArrayList<HashSet<String>> newResult=new ArrayList<>();
        if (index==scope.size()) {
            HashSet<String> r=new HashSet<>();
            //r.add("");
            newResult.add(r);
            return newResult;
        }
        ArrayList<HashSet<String>> result=getSetPossibleValues(scope,possibleValues,index+1);
        String[] possibleValuesOfVar=possibleValues.get(index);
        HashSet<String> tempHash;
        for (int i = 0; i < possibleValuesOfVar.length; i++) {
            // add each value one by one to each HashSet.
            for (int j = 0; j < result.size(); j++) {
                tempHash=(HashSet<String>) result.get(j).clone();
                tempHash.add(scope.get(index)+"="+possibleValuesOfVar[i]);
                newResult.add(tempHash);
            }
        }
        return newResult;
    }


    public static ArrayList<ArrayList<String>> getSetPossibleValuesAsArrayList(ArrayList<String> scope, ArrayList<String[]> possibleValues, int index){
        ArrayList<ArrayList<String>> newResult=new ArrayList<>();
        if (index==scope.size()) {
            ArrayList<String> r=new ArrayList<>();
            //r.add("");
            newResult.add(r);
            return newResult;
        }
        ArrayList<ArrayList<String>> result=getSetPossibleValuesAsArrayList(scope,possibleValues,index+1);
        String[] possibleValuesOfVar=possibleValues.get(index);
        ArrayList<String> tempHash;
        for (int i = 0; i < possibleValuesOfVar.length; i++) {
            // add each value one by one to each HashSet.
            for (int j = 0; j < result.size(); j++) {
                tempHash=(ArrayList<String>) result.get(j).clone();
                //tempHash.add(""+possibleValuesOfVar[i]);
                tempHash.add(0,""+possibleValuesOfVar[i]);
                newResult.add(tempHash);
            }
        }
        //ArrayList<ArrayList<String>> newResult1=new ArrayList<>();
        /*
        for (int i = 0; i < newResult.size(); i++) {
            Collections.reverse(newResult.get(i));
        }*/
        return newResult;
    }



    public static void main(String[] args) {
        tester();
    }

    public static void tester(){

        HashSet<String> temp=new HashSet<>();

        ArrayList<String> f1Scope=new ArrayList<>();
        f1Scope.add("X1");
        //f1Scope.add("X2");

        HashMap<HashSet<String>,Double> f1FactorTable= Factor.createFactorTableFromScope(f1Scope);

        temp.add("X1=a");
        //temp.add("X2=a");
        f1FactorTable.put(temp,2.0);
        temp.clear();

        temp.add("X1=b");
        //temp.add("X2=b");
        f1FactorTable.put(temp,3.0);
        temp.clear();
        Factor f1=new Factor(f1FactorTable,f1Scope);


        // CREATING F2
        ArrayList<String> f2Scope=new ArrayList<>();
        f2Scope.add("X2");
        //f1Scope.add("X2");

        HashMap<HashSet<String>,Double> f2FactorTable= Factor.createFactorTableFromScope(f2Scope);

        temp.add("X2=a");
        //temp.add("X2=a");
        f2FactorTable.put(temp,4.0);
        temp.clear();

        temp.add("X2=b");
        //temp.add("X2=b");
        f2FactorTable.put(temp,3.0);
        temp.clear();
        Factor f2=new Factor(f2FactorTable,f2Scope);

        Factor.printFactor(f1);
        Factor.printFactor(f2);

        Factor f3= Factor.multiplyFactors(f1,f2);
        Factor.printFactor(f3);

        temp.clear();
        // CREATING F4:
        ArrayList<String> f4Scope=new ArrayList<>();
        f4Scope.add("X4");
        //f1Scope.add("X2");

        HashMap<HashSet<String>,Double> f4FactorTable= Factor.createFactorTableFromScope(f4Scope);

        temp.add("X4=a");
        //temp.add("X2=a");
        f4FactorTable.put(temp,10.0);
        temp.clear();

        temp.add("X4=b");
        //temp.add("X2=b");
        f4FactorTable.put(temp,100.0);
        temp.clear();
        Factor f4=new Factor(f4FactorTable,f4Scope);

        Factor.printFactor(f4);

        Factor f5= Factor.multiplyFactors(f3,f4);
        Factor.printFactor(f5);

        System.out.println("Division 1");
        Factor f10= Factor.divideFactors(f5,f4);
        Factor.printFactor(f10);

        System.out.println("\n\n======================================================");
        ArrayList<String> elimnateOver=new ArrayList<>();
        elimnateOver.add("X1");
        Factor f6= Factor.marginalizeOverSet(f5,elimnateOver,1);
        Factor.printFactor(f6);

        elimnateOver.add("X2");
        f6= Factor.marginalizeOverSet(f5,elimnateOver,1);
        Factor.printFactor(f6);

        elimnateOver.add("X4");
        f6= Factor.marginalizeOverSet(f5,elimnateOver,1);
        Factor.printFactor(f6);
    }
}
