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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Happy on 3/11/17.
 */
public class LearnTest {

    private static final String REGEX_ESCAPE_CHAR = "\\";
    public static void main(String []args) throws FileNotFoundException, CloneNotSupportedException {
        long totaltime = System.currentTimeMillis();
        String filename = args[0];
        String evidence_files[] = args[1].split(",");
        String train_files[] = args[2].split(",");
        String out_file = args[3];
        Parser.open_world.add("actor");
        Parser.open_world.add("director");
        Parser.open_world.add("movie");
        Parser.open_world.add("workedUnder");
//        open_world.add("C");
//        open_world.add("S");
//        open_world.add("F");
        List<MLN> mlns = new ArrayList<>();
        List<GroundMLN> groundMlns = new ArrayList<>();

        List<GibbsSampler_v2> inferences = new ArrayList<>();
        int numDb = train_files.length;
        FullyGrindingMill fgm = new FullyGrindingMill();
        for(int i = 0 ; i < numDb ; i++)
        {
            MLN mln = new MLN();
            mlns.add(mln);
            Parser parser = new Parser(mln);
            parser.parseInputMLNFile(filename);
            System.out.println("DB file "+(i+1));
            Map<String, Set<Integer>> varTypeToDomain = parser.collectDomain(evidence_files[i], train_files[i]);
            mln.overWriteDomain(varTypeToDomain);
            System.out.println("Creating MRF...");
            long time = System.currentTimeMillis();
            GroundMLN groundMln = fgm.ground(mln);
            Evidence evidence = parser.parseEvidence(groundMln,evidence_files[i]);
            GroundMLN newGroundMln = fgm.handleEvidence(groundMln, evidence);
            Evidence truth = parser.parseEvidence(groundMln,train_files[i]);
            groundMlns.add(newGroundMln);
            System.out.println("Time taken to create MRF : " + Timer.time((System.currentTimeMillis() - time)/1000.0));
            System.out.println("Total number of ground formulas : " + groundMln.groundFormulas.size());

            GibbsSampler_v2 gs = new GibbsSampler_v2(mln, newGroundMln, evidence, truth, 100, 200, true);
            inferences.add(gs);
        }

        // Start learning
        DiscLearner dl = new DiscLearner(inferences, 50, 100.0, 0.00001, Double.MAX_VALUE, false, true);
        double [] weights = dl.learnWeights();
        dl.writeWeights(out_file, weights);
        System.out.println("Total Time taken : " + Timer.time((System.currentTimeMillis() - totaltime)/1000.0));
    }
}
