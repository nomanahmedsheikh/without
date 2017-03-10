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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Happy on 2/28/17.
 */
public class InferTest {
    public static ArrayList<String> open_world = new ArrayList<>();
    public static ArrayList<String> closed_world = new ArrayList<>();
    public static ArrayList<String> hidden_world = new ArrayList<>();
    public static void main(String []args) throws FileNotFoundException, CloneNotSupportedException {
        String filename = "/Users/Happy/phd/experiments/without/data/MultiValued_data/smokes_mln.txt";
        String out_file = "/Users/Happy/phd/experiments/without/data/MultiValued_data/smokes_result.txt";
        String evidence_file1 = "/Users/Happy/phd/experiments/without/data/MultiValued_data/smokes_test1.txt";
        String evidence_file2 = "/Users/Happy/phd/experiments/without/data/MultiValued_data/smokes_test2.txt";
        open_world.add("C");
        closed_world.add("S");
        int numDb = 2;
        ArrayList<MLN> mlns = new ArrayList<>();
        ArrayList<GroundMLN> groundMlns = new ArrayList<>();
        ArrayList<String> dbFiles = new ArrayList<>();
        dbFiles.add(evidence_file1);
        dbFiles.add(evidence_file2);
        FullyGrindingMill fgm = new FullyGrindingMill();
        for(int i = 0 ; i < numDb ; i++)
        {
            MLN mln = new MLN();
            mlns.add(mln);
            Parser parser = new Parser(mln);
            parser.parseInputMLNFile(filename);
            Map<String, Set<Integer>> varTypeToDomain = parser.collectDomain(dbFiles.get(i));
            mln.overWriteDomain(varTypeToDomain);
            System.out.println("Creating MRF...");
            long time = System.currentTimeMillis();
            GroundMLN groundMln = fgm.ground(mln);
            Evidence evidence = parser.parseEvidence(groundMln, dbFiles.get(i));
            GroundMLN newGroundMln = fgm.handleEvidence(groundMln, evidence);
            groundMlns.add(newGroundMln);
            System.out.println("Time taken to create MRF : " + Timer.time((System.currentTimeMillis() - time)/1000.0));
            System.out.println("Total number of ground formulas : " + groundMln.groundFormulas.size());

            GibbsSampler_v2 gs = new GibbsSampler_v2(newGroundMln, 1000, 10000);
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
            gs.infer(writer);
            writer.close();
        }





    }
}
