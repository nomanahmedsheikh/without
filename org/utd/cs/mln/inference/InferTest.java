package org.utd.cs.mln.inference;

import org.utd.cs.gm.utility.Timer;
import org.utd.cs.mln.alchemy.core.Evidence;
import org.utd.cs.mln.alchemy.core.GroundMLN;
import org.utd.cs.mln.alchemy.core.MLN;
import org.utd.cs.mln.alchemy.util.FullyGrindingMill;
import org.utd.cs.mln.alchemy.util.Parser;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created by Happy on 2/28/17.
 */
public class InferTest {

    public static void main(String []args) throws FileNotFoundException, CloneNotSupportedException {
        String filename = "/Users/Happy/phd/experiments/without/data/Imdb/mln/imdb_mln.txt";
        String out_file = "/Users/Happy/phd/experiments/without/data/Imdb/results/imdb_results.txt";
        String evidence_file1 = "/Users/Happy/phd/experiments/without/data/Imdb/db/imdb.5_movie.actor.director.workedUnder_30.txt";
        //String evidence_file2 = "/Users/Happy/phd/experiments/without/data/MultiValued_data/empty_file.txt";
        String train_file1 = "/Users/Happy/phd/experiments/without/data/MultiValued_data/empty_file.txt";
        //String train_file2 = "/Users/Happy/phd/experiments/without/data/Imdb/imdb.2_train.txt";
        //List<String> evidence_preds = Arrays.asList(args[4].split(","));
        List<String> evidence_preds = Arrays.asList("actor,director,movie,workedUnder".split(","));
        List<String> query_preds = Arrays.asList("actor,director,movie,workedUnder".split(","));
        Parser.open_world.add("actor");
        Parser.open_world.add("director");
        Parser.open_world.add("movie");
        Parser.open_world.add("workedUnder");
//        open_world.add("C");
//        open_world.add("S");
//        open_world.add("F");
        //closed_world.add("S");
        List<MLN> mlns = new ArrayList<>();
        List<GroundMLN> groundMlns = new ArrayList<>();
        List<String> evidFiles = new ArrayList<>();
        List<String> trainFiles = new ArrayList<>();
        List<GibbsSampler_v2> inferences = new ArrayList<>();
        evidFiles.add(evidence_file1);
        //evidFiles.add(evidence_file2);
        trainFiles.add(train_file1);
        //trainFiles.add(train_file2);
        int numDb = trainFiles.size();
        FullyGrindingMill fgm = new FullyGrindingMill();
        for(int i = 0 ; i < numDb ; i++)
        {
            MLN mln = new MLN();
            mlns.add(mln);
            Parser parser = new Parser(mln);
            parser.parseInputMLNFile(filename);
            String files[] = new String[2];
            files[0] = evidFiles.get(i);
            files[1] = trainFiles.get(i);
            Map<String, Set<Integer>> varTypeToDomain = parser.collectDomain(files);
            mln.overWriteDomain(varTypeToDomain);
            System.out.println("Creating MRF...");
            long time = System.currentTimeMillis();
            GroundMLN groundMln = fgm.ground(mln);
            Evidence evidence = parser.parseEvidence(groundMln, evidFiles.get(i));
            Evidence truth = parser.parseEvidence(groundMln,trainFiles.get(i));
            GroundMLN newGroundMln = fgm.handleEvidence(groundMln, evidence, truth, evidence_preds, query_preds, null, false);

            groundMlns.add(newGroundMln);
            System.out.println("Time taken to create MRF : " + Timer.time((System.currentTimeMillis() - time)/1000.0));
            System.out.println("Total number of ground formulas : " + groundMln.groundFormulas.size());

            GibbsSampler_v2 gs = new GibbsSampler_v2(mln, newGroundMln, truth, 100, 1000, false, true);
            PrintWriter writer = null;
            try {
                if(i == 0)
                {
                    writer = new PrintWriter(out_file);
                }
                else
                {
                    writer = new PrintWriter(new FileOutputStream(out_file, true));
                }
            }
            catch(IOException e) {
            }
            gs.infer(true,true);
            gs.writeMarginal(writer);
            writer.close();
        }

    }
}
