package org.utd.cs.mln.marginalmap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by vishal on 11/10/16.
 */
public class Assignment3 {
    public static String[] characters={"d","o","i","r","a","h","t","n","s","e"};

    static long totalTime=0;
    //public static String[] characters={"a","b"};

    String absDataPath="OCRdataset-2"+ File.separator+"data"+File.separator;
    String absPotenetialPath="OCRdataset-2"+File.separator+"potentials"+File.separator;

    public static HashMap<String,Double> ocr_potentials;
    public static HashMap<String,Double> trans_potentials;
    public static HashMap<String,Double> skip_potentials;
    public static HashMap<String,Double> cross_potentials;

    public ArrayList<ArrayList<Integer>> dataImages;
    public ArrayList<String> dataWords;

    public static double skipValue=5.0;
    public static double defaultSkipValue=1.0;

    public static double crossValue=5.0;
    public static double defaultCrossValue=1.0;

    ArrayList<String> predictedWords;

    public Assignment3(){
        ocr_potentials=new HashMap<>();
        trans_potentials=new HashMap<>();
        dataImages=new ArrayList<>();
        dataWords=new ArrayList<>();
        predictedWords=new ArrayList<>();
    }

    public void readOCRFile(String ocr_file){
        try {
            URL path = Assignment3.class.getResource(ocr_file);
            File f = new File(path.getFile());
            BufferedReader d = new BufferedReader(new FileReader(f));
            String line;
            String[] splitted;
            while ((line = d.readLine()) != null) {
                splitted = line.split("\t");
                ocr_potentials.put(splitted[0] + splitted[1], Double.parseDouble(splitted[2]));
            }
            //System.out.println(ocr_potentials.size());
            d.close();
        }
        catch (Exception e){
            System.out.println("Error");
            e.printStackTrace();
        }
    }

    public void readTransFile(String trans_file){
        try {
            URL path = Assignment3.class.getResource(trans_file);
            File f = new File(path.getFile());
            BufferedReader d = new BufferedReader(new FileReader(f));
            String line;
            String[] splitted;
            while ((line = d.readLine()) != null) {
                splitted = line.split("\t");
                trans_potentials.put(splitted[0] + splitted[1], Double.parseDouble(splitted[2]));
            }
            //System.out.println(trans_potentials.size());
            d.close();
        }
        catch (Exception e){
            System.out.println("Error");
            e.printStackTrace();
        }
    }

    public void readDatasetImages(String imageFile){
        try {
            URL path = Assignment3.class.getResource(imageFile);
            File f = new File(path.getFile());
            BufferedReader d = new BufferedReader(new FileReader(f));
            String line;
            String[] splitted;
            ArrayList<Integer> temp;
            int counter=0;
            while ((line = d.readLine()) != null) {
                //System.out.println(line);
                if(counter==2){
                    counter=0;
                    continue;
                }
                if (line.equals(""))
                    continue;
                temp=new ArrayList<>();
                splitted = line.split("\t");
                for (int i = 0; i < splitted.length; i++) {
                    temp.add(Integer.parseInt(splitted[i]));
                }
                dataImages.add(temp);
                counter++;
            }
            //System.out.println(dataImages.size());
            //System.out.println(dataImages.get(1).get(2));
            d.close();
        }
        catch (Exception e){
            System.out.println("Error");
            e.printStackTrace();
        }
    }

    public void readDatasetWords(String wordsFile){
        try {
            URL path = Assignment3.class.getResource(wordsFile);
            //System.out.println(wordsFile);
            File f = new File(path.getFile());
            BufferedReader d = new BufferedReader(new FileReader(f));
            String line;
            int counter=0;
            while ((line = d.readLine()) != null) {
                if(counter==2){
                    counter=0;
                    continue;
                }
                if (line.equals(""))
                    continue;
                String[] temp=line.split("(?!^)");
                dataWords.add(line);
                counter++;
            }
            //System.out.println(dataWords.size());
            //System.out.println(dataWords.get(1));
            d.close();
        }
        catch (Exception e){
            System.out.println("Error");
            e.printStackTrace();
        }
    }




/*
=========================================================================================================
*/

    public static String[] permutations(String[] characters,int r){
        if(r==0){
            String[] result=new String[1];
            result[0]="";
            return result;
        }
        String currentChar;
        String result[]=permutations(characters,r-1);
        String newResult[]=new String[result.length*characters.length];

        int k=0;
        for (int i = 0; i < characters.length; i++) {
            currentChar=characters[i];
            for (int j = 0; j < result.length; j++) {
                newResult[k]=currentChar+result[j];
                k++;
            }
        }
        return newResult;
    }

    public static ArrayList<HashSet<String>> getSetPossibleValues(String[] characters,ArrayList<String> scope){
        int numOfVariables=scope.size();
        String[] perm=permutations(characters,numOfVariables);
        ArrayList<HashSet<String>> allPossibleValues=new ArrayList<>(perm.length);
        HashSet<String> currentKey;
        String tempPerm;
        for (int i = 0; i < perm.length; i++) {
            currentKey=new HashSet<String>();
            tempPerm=perm[i];
            for (int j = 0; j < tempPerm.length(); j++) {
                currentKey.add(scope.get(j)+"="+tempPerm.charAt(j));
            }

            allPossibleValues.add(currentKey);
        }
        return allPossibleValues;
    }

    public void runOnDataSet(int model, String datasetImages,String datasetWords,String ocrFile,String transFile,int numberOfSamples,int numberOfBurns,int typeOfMarginal){
    /*    System.out.println("For dataset = "+datasetImages+"\n");
        readDatasetImages(absDataPath+datasetImages);
        readDatasetWords(absDataPath+datasetWords);
        readOCRFile(absPotenetialPath+ocrFile);
        readTransFile(absPotenetialPath+transFile);

        ArrayList<Integer> img1;
        ArrayList<Integer> img2;
        // Running on full dataset

        double avgLoglikelihood=0.0;

         GibbsSampler gb=null;
        int timeSteps;
        String[] result;
        ArrayList<String[]> posValues;

        for (int i = 0; i < dataImages.size()-1; i+=2) {
            System.out.println(i);
            //System.out.println("********************************************************************");
            img1=dataImages.get(i);
            img2=dataImages.get(i+1);
            //System.out.println(img1);
            //System.out.println(img2);

            posValues=new ArrayList<>();
            for (int j = 0; j < img1.size()+img2.size(); j++) {
                posValues.add(characters);
            }
            gb=new GibbsSampler(model,img1,img2,numberOfSamples,numberOfBurns,typeOfMarginal,posValues);

            long startT=System.currentTimeMillis();
            gb.runGibbsSampler();
            long endT=System.currentTimeMillis();
            Assignment3.totalTime+=(endT-startT);
            //System.out.println(gb.finalSamplesImg1);
            //System.out.println("");
            //System.out.println(gb.finalSamplesImg2);
            result=gb.getMaxProbableAssignment();
            predictedWords.add(result[0]);
            predictedWords.add(result[1]);
            System.out.println(result[0]+", "+result[1]);
            avgLoglikelihood+=gb.getLikelihood(0, result[0]);
            avgLoglikelihood+=gb.getLikelihood(1, result[1]);
            //System.out.println(result[0]+"\n"+result[1]+"\n");
            System.out.println("===");
        }
        System.out.println(predictedWords);
        //System.out.println(dataWords);
        double[] accuracies= AccuracyChecker.checkAccuracy(predictedWords,dataWords);
        System.out.println("Char Accuracy= "+accuracies[0]);
        System.out.println("word Accuracy= "+accuracies[1]);
        System.out.println("Likelihood= "+ (avgLoglikelihood/dataWords.size()));
        System.out.println("Time= "+ Assignment3.totalTime/1000d);
        Assignment3.totalTime=0;
        System.out.println("");*/
    }

    public static void main(String[] args) {
        Assignment3 ob=new Assignment3();

        //String datasetImages="data-loops.dat";
        //String datasetWords="truth-loops.dat";

        //String datasetImages="Test-Data.dat";
        //String datasetWords="Test-Truth.dat";

        String datasetImages="data-loopsWS.dat";
        String datasetWords="truth-loopsWS.dat";

        String ocr_file="ocr.dat";
        String trans_file="trans.dat";
        // String ocr_file="Temp-OCR.dat";
        // String trans_file="Temp-trans.dat";

        System.out.println("SP Inference");
        int model=4;
        int typeOfMarginal=1;
        int numberOfSamples=20000;
        int numberOfBurns=5000;

        ob.runOnDataSet(model,datasetImages,datasetWords,ocr_file,trans_file,numberOfSamples,numberOfBurns,typeOfMarginal);

    }



}
