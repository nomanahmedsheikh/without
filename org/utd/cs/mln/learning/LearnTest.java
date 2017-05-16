package org.utd.cs.mln.learning;

import org.utd.cs.gm.utility.Timer;
import org.utd.cs.mln.alchemy.core.Evidence;
import org.utd.cs.mln.alchemy.core.Formula;
import org.utd.cs.mln.alchemy.core.GroundMLN;
import org.utd.cs.mln.alchemy.core.MLN;
import org.utd.cs.mln.alchemy.util.FullyGrindingMill;
//import org.utd.cs.mln.alchemy.util.GrindingMill;
import org.utd.cs.mln.alchemy.util.Pair;
import org.utd.cs.mln.alchemy.util.Parser;
import org.utd.cs.mln.inference.GibbsSampler;
import org.utd.cs.mln.inference.GibbsSampler_v2;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created by Happy on 3/11/17.
 */
public class LearnTest {

    private static final String REGEX_ESCAPE_CHAR = "\\";
    private static int numIter=100, numInferSamples=200, numBurnSamples = 100;
    private static String mlnFile, outFile, sePredName;
    private static String []evidenceFiles, truthFiles, softEvidenceFiles;
    private static boolean queryEvidence=false, withEM=false, usePrior=false;
    private static int numDb;
    private static double minllChange = 5, seLambda = 1.0; // minllChange changed from 10^-5 to 1 so that numIter reduces, seLambda is the lambda for softevidence
    private static List<String> evidPreds, hiddenPreds, queryPreds = null, openWorldPreds, closedWorldPreds;

    public static long getSeed(){
        //return 123456789;
        return System.currentTimeMillis();
    }

    private enum ArgsState{
        MlnFile,
        EvidFiles,
        SoftEvidFiles,
        TruthFiles,
        OutFile,
        OpenWorld,
        ClosedWorld,
        NumIter,
        EvidPreds, QueryPreds, HiddenPreds, Flag, NumInferSamples,
        MinLLChange,
        SELambda,
        SEPredName
    }
    public static void main(String []args) throws FileNotFoundException, CloneNotSupportedException {
        long totaltime = System.currentTimeMillis();
        parseArgs(args);
        //String filename = "/Users/Happy/phd/experiments/without/data/Imdb/mln/imdb_mln.txt";
        //String evidence_files[] = "/Users/Happy/phd/experiments/without/data/Imdb/db/empty_file.txt".split(",");
        //String train_files[] = "/Users/Happy/phd/experiments/without/data/Imdb/db/imdb.1_train.txt".split(",");
        //String out_file = "/Users/Happy/phd/experiments/without/data/Imdb/results/imdb_results_out.txt";
        //List<String> evidence_preds = Arrays.asList("".split(","));
        //List<String> query_preds = Arrays.asList("actor,director,movie,workedUnder".split(","));;
        Parser.open_world.addAll(openWorldPreds);
        Parser.closed_world.addAll(closedWorldPreds);
        FullyGrindingMill.queryEvidence = queryEvidence;
        List<MLN> mlns = new ArrayList<>();
        List<GroundMLN> groundMlns = new ArrayList<>();

        List<GibbsSampler> inferences = new ArrayList<>();
        List<GibbsSampler> inferencesEM = new ArrayList<>();
        FullyGrindingMill fgm = new FullyGrindingMill();
        //GrindingMill gm = new GrindingMill();
        List<Evidence> truths = new ArrayList<>();

        // For each training file, create an inference object, which contains whole groundMLN for that domain.
        for(int i = 0 ; i < numDb ; i++)
        {
            MLN mln = new MLN();
            mlns.add(mln);
            Parser parser = new Parser(mln);
            parser.parseInputMLNFile(mlnFile);
            System.out.println("DB file "+(i+1));
            String files[] = new String[1];
            //files[0] = evidenceFiles[i];
            files[0] = truthFiles[i];
            //TODO : for now, we send only truth file to collectDomain, otherwise send both truth and evidence.
            // We assume that there is no new constant in evidence.
            Map<String, Set<Integer>> varTypeToDomain = parser.collectDomain(files);
            mln.overWriteDomain(varTypeToDomain);
            System.out.println("Creating MRF...");
            long time = System.currentTimeMillis();
            //Set<String> evidenceSet = parser.createEvidenceSet(evidenceFiles[i]);
            GroundMLN groundMln = fgm.ground(mln);
            System.out.println("Total number of ground formulas before handling evidence : " + groundMln.groundFormulas.size());
            Evidence evidence = parser.parseEvidence(groundMln,evidenceFiles[i]);
            Evidence truth = parser.parseEvidence(groundMln,truthFiles[i]);
            truths.add(truth);
            //Map<Integer, List<Integer>> featureVectors = fgm.getFeatureVectors(groundMln, mln.formulas.size(), truth, "person", varTypeToDomain.get("person"), true);
            //writeFeatures(featureVectors,i+1);

            GroundMLN newGroundMln = fgm.handleEvidence(groundMln, evidence, truth, evidPreds, queryPreds, hiddenPreds, false);

            // If there is a soft evidence present, then add it.
            if(softEvidenceFiles != null)
            {
                newGroundMln = fgm.addSoftEvidence(newGroundMln, softEvidenceFiles[i], seLambda, sePredName);
            }
            GibbsSampler gs = new GibbsSampler(mln, newGroundMln, truth, numBurnSamples, numInferSamples, true, false);
            inferences.add(gs);

            if(withEM)
            {
                List<String> evidEmPreds = new ArrayList<String>();
                evidEmPreds.addAll(evidPreds);
                evidEmPreds.addAll(queryPreds);
                GroundMLN EMNewGroundMln = fgm.handleEvidence(groundMln, Evidence.mergeEvidence(evidence,truth), null, evidEmPreds, null, hiddenPreds, true);
                GibbsSampler gsEM = new GibbsSampler(mln, EMNewGroundMln, truth, numBurnSamples, numInferSamples, true, false);
                inferencesEM.add(gsEM);
            }
            System.out.println("Time taken to create MRF : " + Timer.time((System.currentTimeMillis() - time)/1000.0));
            System.out.println("Total number of ground formulas : " + newGroundMln.groundFormulas.size());
            System.out.println("Total number of ground preds : " + newGroundMln.groundPredicates.size());

        }


        DiscriminativeLearner dl = new CGLearner(inferences, inferencesEM, numIter, withEM, null, null, usePrior, 100.0, minllChange, Double.MAX_VALUE, true);
        dl.learnWeights();

        System.out.println("Total Time taken : " + Timer.time((System.currentTimeMillis() - totaltime)/1000.0));
    }

    private static void writeFeatures(Map<Integer, List<Integer>> featureVectors, int db) throws FileNotFoundException {
        PrintWriter pw = new PrintWriter("data/imdb/features/features."+db+".txt");
        for(int constant : featureVectors.keySet())
        {
            for(int val : featureVectors.get(constant))
            {
                pw.write(val+",");
            }
            pw.write(constant+"\n");
        }
        pw.close();
    }

    private static void parseArgs(String[] args) {
        ArgsState state = ArgsState.Flag;
        if(args.length == 0)
        {
            System.out.println("No flags provided ");
            System.out.println("Following are the allowed flags : ");
            System.out.println(manual);
            System.exit(0);
        }
        System.out.println("Learning parameters given : ");
        for(String arg : args)
        {
            switch(state)
            {
                case MlnFile: // necessary
                    mlnFile = arg;
                    System.out.println("-i = " + arg);
                    state = ArgsState.Flag;
                    continue;

                case EvidFiles: // if not given, will be empty file
                    evidenceFiles = arg.split(",");
                    System.out.println("-e = " + arg);
                    state = ArgsState.Flag;
                    continue;

                case SoftEvidFiles: // if not given, will be empty file
                    softEvidenceFiles = arg.split(",");
                    System.out.println("-se = " + arg);
                    state = ArgsState.Flag;
                    continue;

                case TruthFiles:  // necessary
                    truthFiles = arg.split(",");
                    System.out.println("-t = " + arg);
                    state = ArgsState.Flag;
                    numDb = truthFiles.length;
                    continue;

                case OutFile: // necessary
                    outFile = arg;
                    System.out.println("-o = " + arg);
                    state = ArgsState.Flag;
                    continue;

                case EvidPreds: // If not given, will be an empty list
                    evidPreds = Arrays.asList(arg.split(","));
                    System.out.println("-ep = " + arg);
                    state = ArgsState.Flag;
                    continue;

                case QueryPreds: // necessary
                    queryPreds = Arrays.asList(arg.split(","));
                    System.out.println("-qp = " + arg);
                    state = ArgsState.Flag;
                    continue;

                case HiddenPreds: // necessary
                    hiddenPreds = Arrays.asList(arg.split(","));
                    System.out.println("-hp = " + arg);
                    state = ArgsState.Flag;
                    continue;

                case OpenWorld: // by default, all queryPreds are openworld
                    openWorldPreds = Arrays.asList(arg.split(","));
                    System.out.println("-ow = " + arg);
                    state = ArgsState.Flag;
                    continue;

                case ClosedWorld: // by default, all evidence preds are closedworld
                    closedWorldPreds = Arrays.asList(arg.split(","));
                    System.out.println("-cw = " + arg);
                    state = ArgsState.Flag;
                    continue;


                case NumIter: // by default, it is 100
                    numIter = Integer.parseInt(arg);
                    state = ArgsState.Flag;
                    continue;

                case NumInferSamples: // by default, it is 100
                    numInferSamples = Integer.parseInt(arg);
                    state = ArgsState.Flag;
                    continue;

                case MinLLChange: // by default, it is 5
                    minllChange = Double.parseDouble(arg);
                    state = ArgsState.Flag;
                    continue;

                case SELambda: // by default, it is 1.0
                    seLambda = Double.parseDouble(arg);
                    state = ArgsState.Flag;
                    continue;

                case SEPredName: // by default, it is null
                    sePredName = arg;
                    state = ArgsState.Flag;
                    continue;

                case Flag:
                    if(arg.equals("-i"))
                    {
                        state = ArgsState.MlnFile;
                    }
                    else if(arg.equals(("-e")))
                    {
                        state = ArgsState.EvidFiles;
                    }
                    else if(arg.equals(("-se")))
                    {
                        state = ArgsState.SoftEvidFiles;
                    }
                    else if(arg.equals(("-t")))
                    {
                        state = ArgsState.TruthFiles;
                    }
                    else if(arg.equals(("-o")))
                    {
                        state = ArgsState.OutFile;
                    }
                    else if(arg.equals(("-ep")))
                    {
                        state = ArgsState.EvidPreds;
                    }
                    else if(arg.equals(("-qp")))
                    {
                        state = ArgsState.QueryPreds;
                    }
                    else if(arg.equals(("-hp")))
                    {
                        state = ArgsState.HiddenPreds;
                    }

                    else if(arg.equals(("-ow")))
                    {
                        state = ArgsState.OpenWorld;
                    }
                    else if(arg.equals(("-cw")))
                    {
                        state = ArgsState.ClosedWorld;
                    }
                    else if(arg.equals(("-queryEvidence")))
                    {
                        queryEvidence = true;
                    }
                    else if(arg.equals(("-usePrior")))
                    {
                        usePrior = true;
                    }
                    else if(arg.equals(("-NumIter")))
                    {
                        state = ArgsState.NumIter;
                    }
                    else if(arg.equals(("-NumInferSamples")))
                    {
                        state = ArgsState.NumInferSamples;
                    }
                    else if(arg.equals(("-minllchange")))
                    {
                        state = ArgsState.MinLLChange;
                    }
                    else if(arg.equals(("-seLambda")))
                    {
                        state = ArgsState.SELambda;
                    }
                    else if(arg.equals(("-sePred")))
                    {
                        state = ArgsState.SEPredName;
                    }
                    else if(arg.equals(("-withEM")))
                    {
                        withEM = true;
                    }

                    else
                    {
                        System.out.println("Unknown flag " + arg);
                        System.out.println("Following are the allowed flags : ");
                        System.out.println(manual);
                        System.exit(0);
                    }

            }
        }
        if(mlnFile == null)
        {
            System.out.println("Necessary to provide MLN file, exiting !!!");
            System.exit(0);
        }
        if(truthFiles == null)
        {
            System.out.println("Necessary to provide at least one training file, exiting !!!");
            System.exit(0);
        }
        if(outFile == null)
        {
            System.out.println("Necessary to provide output file, exiting !!!");
            System.exit(0);
        }
        if(queryPreds == null)
        {
            System.out.println("Necessary to provide query predicates, exiting !!!");
            System.exit(0);
        }
        if(evidenceFiles == null)
        {
            evidenceFiles = new String[numDb];
            System.out.println("-e = " + Arrays.asList(evidenceFiles));
        }
        if(softEvidenceFiles == null)
        {
            softEvidenceFiles = new String[numDb];
            System.out.println("-se = " + Arrays.asList(softEvidenceFiles));
        }
        if(evidPreds == null)
        {
            evidPreds = new ArrayList<>();
            System.out.println("-ep = " + evidPreds);
        }
        if(hiddenPreds == null)
        {
            hiddenPreds = new ArrayList<>();
            System.out.println("-hp = " + hiddenPreds);
        }

        if(openWorldPreds == null)
        {
            openWorldPreds = new ArrayList<>();
            openWorldPreds.addAll(queryPreds);
            openWorldPreds.addAll(hiddenPreds);
            System.out.println("-ow = " + openWorldPreds);
        }
        if(closedWorldPreds == null)
        {
            closedWorldPreds = new ArrayList<>();
            closedWorldPreds.addAll(evidPreds);
            System.out.println("-cw = " + closedWorldPreds);
        }

        System.out.println("-NumIter = " + numIter);
        System.out.println("-NumInferSamples = " + numInferSamples);
        System.out.println("-minllchange = " + minllChange);
        System.out.println("-seLambda = " + seLambda);
        System.out.println("-sePred = " + sePredName);
        System.out.println("-queryEvidence = " + queryEvidence);
        System.out.println("-usePrior = " + usePrior);
        System.out.println("-withEM = " + withEM);

    }

    private static String manual = "-i\t(necessary) Input mln file\n" +
            "-e\t(optional) Evidence file\n" +
            "-t\t(Necessary) Comma separated training files\n" +
            "-o\t(Necessary) output file\n" +
            "-ep\t(Optional) Comma separated evidence predicates\n" +
            "-qp\t(Necessary) Comma separated query Predicates\n" +
            "-hp\t(Necessary with EM) Comma separated hidden Predicates\n" +
            "-ow\t(Optional) Comma separated open world predicates, by default all query preds are open world\n" +
            "-cw\t(Optional) Comma separated closed world predicates, by default all evidence preds are closed world\n" +
            "-NumIter\t(Optional, default 100) number of learning iterations\n" +
            "-NumSamples\\t(Optional, default 200) number of inference iterations\\n\"" +
            "-queryEvidence\t(Optional, default false) If specified, then groundings of query predicates not present in " +
            "training file are taken to be false evidence\n" +
            "-withEM\t(Optional) If specified, learn using EM" +
            "-usePrior\t(Optional) If specified, initialize weights to MLN weights";

}
