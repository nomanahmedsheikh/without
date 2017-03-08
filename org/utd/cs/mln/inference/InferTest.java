package org.utd.cs.mln.inference;

import org.utd.cs.gm.utility.Timer;
import org.utd.cs.mln.alchemy.core.GroundMLN;
import org.utd.cs.mln.alchemy.core.MLN;
import org.utd.cs.mln.alchemy.util.FullyGrindingMill;
import org.utd.cs.mln.alchemy.util.Parser;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Happy on 2/28/17.
 */
public class InferTest {
    public static void main(String []args) throws FileNotFoundException {
        MLN mln = new MLN();
        String filename = "/Users/Happy/phd/experiments/without/data/MultiValued_data/smokes_mln.txt";
        Parser parser = new Parser(mln);
        parser.parseInputMLNFile(filename);
        FullyGrindingMill fgm = new FullyGrindingMill();
        System.out.println("Creating MRF...");
        long time = System.currentTimeMillis();
        GroundMLN groundMln = fgm.ground(mln);
        System.out.println("Time taken to create MRF : " + Timer.time((System.currentTimeMillis() - time)/1000.0));
        System.out.println("Total number of ground formulas : " + groundMln.groundFormulas.size());

        GibbsSampler_v2 gs = new GibbsSampler_v2(groundMln, 1000, 100000);
        gs.infer("/Users/Happy/phd/experiments/without/data/MultiValued_data/smokes_result.txt");
    }
}
