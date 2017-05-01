package org.utd.cs.mln.inference;

import org.utd.cs.gm.core.LogDouble;
import org.utd.cs.gm.utility.Timer;
import org.utd.cs.mln.alchemy.core.*;
import org.utd.cs.mln.alchemy.util.FullyGrindingMill;
import org.utd.cs.mln.alchemy.util.Parser;

import java.io.*;
import java.util.*;

/**
 * Created by Happy on 2/28/17.
 */
public class InferTest {

    private static String mlnFile, outFile, evidenceFile, softEvidenceFile, goldFile;
    private static boolean queryEvidence=false, trackFormulaCounts = false, calculateMarginal = true;
    private static int NumBurnIn = 100, NumSamples = 500;
    private static List<String> evidPreds, queryPreds = null, openWorldPreds, closedWorldPreds;

    private enum ArgsState {
        MlnFile,
        EvidFile,
        softEvidFile,
        GoldFile,
        OutFile,
        OpenWorld,
        ClosedWorld,
        EvidPreds, QueryPreds, Flag, NumSamples
    }

    public static void main(String[] args) throws FileNotFoundException, CloneNotSupportedException {
        parseArgs(args);
        Parser.open_world.addAll(openWorldPreds);
        Parser.closed_world.addAll(closedWorldPreds);
        FullyGrindingMill.queryEvidence = queryEvidence;
        FullyGrindingMill fgm = new FullyGrindingMill();
        MLN mln = new MLN();
        Parser parser = new Parser(mln);
        parser.parseInputMLNFile(mlnFile);
        String files[] = new String[1];
        //files[0] = evidenceFile;
        files[0] = goldFile;
        Map<String, Set<Integer>> varTypeToDomain = parser.collectDomain(files);
        mln.overWriteDomain(varTypeToDomain);
        System.out.println("Creating MRF...");
        long time = System.currentTimeMillis();
        GroundMLN groundMln = fgm.ground(mln);
        System.out.println("Total number of ground formulas before hadnling evidence : " + groundMln.groundFormulas.size());
        Evidence evidence = parser.parseEvidence(groundMln, evidenceFile);
        Evidence gold = parser.parseEvidence(groundMln, goldFile);
        //Map<Integer, List<Integer>> featureVectors = fgm.getFeatureVectors(groundMln, mln.formulas.size(), evidence, "person", varTypeToDomain.get("person"), false);
        //writeFeatures(featureVectors,1,100);
        GroundMLN newGroundMln = fgm.handleEvidence(groundMln, evidence, gold, evidPreds, queryPreds, null, false);
        newGroundMln = fgm.addSoftEvidence(newGroundMln, softEvidenceFile);

        System.out.println("Time taken to create MRF : " + Timer.time((System.currentTimeMillis() - time) / 1000.0));
        System.out.println("Total number of ground formulas after hadnling evidence : " + newGroundMln.groundFormulas.size());

        GibbsSampler_v2 gs = new GibbsSampler_v2(mln, newGroundMln, gold, NumBurnIn, NumSamples, trackFormulaCounts, calculateMarginal);
        PrintWriter writer = null;
        try{
            writer = new PrintWriter(new FileOutputStream(outFile));
        }
        catch (IOException e) {
        }
        gs.infer(true, true);
        gs.writeMarginal(writer);
        writer.close();
    }



    private static void writeFeatures(Map<Integer, List<Integer>> featureVectors, int db, int evidPer) throws FileNotFoundException {
        PrintWriter pw = new PrintWriter("data/imdb/features/features_infer."+db+"."+evidPer+".txt");
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
        if (args.length == 0) {
            System.out.println("No flags provided ");
            System.out.println("Following are the allowed flags : ");
            System.out.println(manual);
            System.exit(0);
        }
        System.out.println("Inference parameters given : ");
        for (String arg : args) {
            switch (state) {
                case MlnFile: // necessary
                    mlnFile = arg;
                    System.out.println("-i = " + arg);
                    state = ArgsState.Flag;
                    continue;

                case EvidFile: // if not given, will be empty file
                    evidenceFile = arg;
                    System.out.println("-e = " + arg);
                    state = ArgsState.Flag;
                    continue;

                case softEvidFile: // if not given, will be empty file
                    softEvidenceFile = arg;
                    System.out.println("-se = " + arg);
                    state = ArgsState.Flag;
                    continue;

                case GoldFile:  // necessary
                    goldFile = arg;
                    System.out.println("-g = " + arg);
                    state = ArgsState.Flag;
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

                case NumSamples: // by default, it is 100
                    NumSamples = Integer.parseInt(arg);
                    state = ArgsState.Flag;
                    continue;

                case Flag:
                    if (arg.equals("-i")) {
                        state = ArgsState.MlnFile;
                    } else if (arg.equals(("-e"))) {
                        state = ArgsState.EvidFile;
                    } else if (arg.equals(("-se"))) {
                        state = ArgsState.softEvidFile;
                    } else if (arg.equals(("-g"))) {
                        state = ArgsState.GoldFile;
                    } else if (arg.equals(("-o"))) {
                        state = ArgsState.OutFile;
                    } else if (arg.equals(("-ep"))) {
                        state = ArgsState.EvidPreds;
                    } else if (arg.equals(("-qp"))) {
                        state = ArgsState.QueryPreds;
                    } else if (arg.equals(("-ow"))) {
                        state = ArgsState.OpenWorld;
                    } else if (arg.equals(("-cw"))) {
                        state = ArgsState.ClosedWorld;
                    } else if (arg.equals(("-queryEvidence"))) {
                        queryEvidence = true;
                    } else if (arg.equals(("-NumSamples"))) {
                        state = ArgsState.NumSamples;
                    } else {
                        System.out.println("Unknown flag " + arg);
                        System.out.println("Following are the allowed flags : ");
                        System.out.println(manual);
                        System.exit(0);
                    }

            }
        }
        if (mlnFile == null) {
            System.out.println("Necessary to provide MLN file, exiting !!!");
            System.exit(0);
        }
        if (goldFile == null) {
            System.out.println("Necessary to provide at least one training file, exiting !!!");
            System.exit(0);
        }
        if (outFile == null) {
            System.out.println("Necessary to provide output file, exiting !!!");
            System.exit(0);
        }
        if (queryPreds == null) {
            System.out.println("Necessary to provide query predicates, exiting !!!");
            System.exit(0);
        }
        if (evidenceFile == null) {
            System.out.println("-e = " + evidenceFile);
        }
        if (softEvidenceFile == null) {
            System.out.println("-se = " + softEvidenceFile);
        }
        if (evidPreds == null) {
            evidPreds = new ArrayList<>();
            System.out.println("-ep = " + evidPreds);
        }
        if (openWorldPreds == null) {
            openWorldPreds = new ArrayList<>();
            openWorldPreds.addAll(queryPreds);
            System.out.println("-ow = " + openWorldPreds);
        }
        if (closedWorldPreds == null) {
            closedWorldPreds = new ArrayList<>();
            closedWorldPreds.addAll(evidPreds);
            System.out.println("-cw = " + closedWorldPreds);
        }

        System.out.println("-NumSamples = " + NumSamples);
        System.out.println("-queryEvidence = " + queryEvidence);
    }

        private static String manual = "-i\t(necessary) Input mln file\n" +
                "-e\t(optional) Evidence file\n" +
                "-se\t(optional) Soft Evidence file\n" +
                "-g\t(Necessary) gold file\n" +
                "-o\t(Necessary) output file\n" +
                "-ep\t(Optional) Comma separated evidence predicates\n" +
                "-qp\t(Necessary) Comma separated query Predicates\n" +
                "-ow\t(Optional) Comma separated open world predicates, by default all query preds are open world\n" +
                "-cw\t(Optional) Comma separated closed world predicates, by default all evidence preds are closed world\n" +
                "-NumSamples\t(Optional, default 1000) number of Gibbs Samples\n" +
                "-queryEvidence\t(Optional, default false) If specified, then groundings of query predicates not present in " +
                "gold file are taken to be false evidence";


}
