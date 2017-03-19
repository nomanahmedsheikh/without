package org.utd.cs.mln.learning;

import org.utd.cs.gm.utility.Timer;
import org.utd.cs.mln.alchemy.core.Evidence;
import org.utd.cs.mln.alchemy.core.Formula;
import org.utd.cs.mln.alchemy.core.GroundMLN;
import org.utd.cs.mln.alchemy.core.MLN;
import org.utd.cs.mln.alchemy.util.FullyGrindingMill;
import org.utd.cs.mln.alchemy.util.Parser;
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
    private static int numIter=100, numSamples=200;
    private static String mlnFile, outFile;
    private static String []evidenceFiles, truthFiles;
    private static boolean queryEvidence=false, withEM=false, usePrior=false;
    private static int numDb;
    private static double minllChange = 1; // changed from 10^-5 to 1 so that numIter reduces
    private static List<String> evidPreds, hiddenPreds, queryPreds = null, openWorldPreds, closedWorldPreds;

    public static long getSeed(){
        //return 123456789;
        return System.currentTimeMillis();
    }

    private enum ArgsState{
        MlnFile,
        EvidFiles,
        TruthFiles,
        OutFile,
        OpenWorld,
        ClosedWorld,
        NumIter,
        EvidPreds, QueryPreds, HiddenPreds, Flag, NumSamples
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

        List<GibbsSampler_v2> inferences = new ArrayList<>();
        List<GibbsSampler_v2> inferencesEM = new ArrayList<GibbsSampler_v2>();
        FullyGrindingMill fgm = new FullyGrindingMill();
        for(int i = 0 ; i < numDb ; i++)
        {
            MLN mln = new MLN();
            mlns.add(mln);
            Parser parser = new Parser(mln);
            parser.parseInputMLNFile(mlnFile);
            System.out.println("DB file "+(i+1));
            String files[] = new String[2];
            files[0] = evidenceFiles[i];
            files[1] = truthFiles[i];
            Map<String, Set<Integer>> varTypeToDomain = parser.collectDomain(files);
            mln.overWriteDomain(varTypeToDomain);
            System.out.println("Creating MRF...");
            long time = System.currentTimeMillis();
            GroundMLN groundMln = fgm.ground(mln);
            Evidence evidence = parser.parseEvidence(groundMln,evidenceFiles[i]);
            Evidence truth = parser.parseEvidence(groundMln,truthFiles[i]);
            //GroundMLN newGroundMln = fgm.handleEvidence(groundMln, evidence, truth, evidPreds, queryPreds, hiddenPreds, false);
            GroundMLN newGroundMln = groundMln;
            GibbsSampler_v2 gs = new GibbsSampler_v2(mln, newGroundMln, truth, 100, numSamples, true, false);
            inferences.add(gs);

            if(withEM)
            {
                List<String> evidEmPreds = new ArrayList<String>();
                evidEmPreds.addAll(evidPreds);
                evidEmPreds.addAll(queryPreds);
                GroundMLN EMNewGroundMln = fgm.handleEvidence(groundMln, Evidence.mergeEvidence(evidence,truth), null, evidEmPreds, null, hiddenPreds, true);
                GibbsSampler_v2 gsEM = new GibbsSampler_v2(mln, EMNewGroundMln, truth, 100, numSamples, true, false);
                inferencesEM.add(gsEM);
            }
            System.out.println("Time taken to create MRF : " + Timer.time((System.currentTimeMillis() - time)/1000.0));
            System.out.println("Total number of ground formulas : " + newGroundMln.groundFormulas.size());

        }

        // Start learning
        DiscLearner dl = new DiscLearner(inferences, inferencesEM, numIter, 100.0, minllChange, Double.MAX_VALUE, withEM, true, usePrior);

        double [] weights = dl.learnWeights();
        dl.writeWeights(mlnFile, outFile, weights);
        System.out.println("Total Time taken : " + Timer.time((System.currentTimeMillis() - totaltime)/1000.0));
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

                case NumSamples: // by default, it is 100
                    numSamples = Integer.parseInt(arg);

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
                    else if(arg.equals(("-NumSamples")))
                    {
                        state = ArgsState.NumSamples;
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
        System.out.println("-NumSamples = " + numSamples);
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
