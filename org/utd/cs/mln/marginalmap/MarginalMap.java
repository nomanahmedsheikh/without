package org.utd.cs.mln.marginalmap;

import org.utd.cs.mln.alchemy.core.MLN;
import org.utd.cs.mln.alchemy.core.PredicateNotFound;
import org.utd.cs.mln.alchemy.util.Parser;
import org.utd.cs.mln.lmap.*;
import org.utd.cs.mln.lmap.Decomposer;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Created by vishal on 14/1/17.
 */
public class MarginalMap {

    static int print=1;
    String datasetLocation=System.getProperty("user.dir")+File.separator+"datasets"+File.separator+"mln_files"+File.separator;

    public static void main(String[] args) {

        MarginalMap mp=new MarginalMap();
        //String mlnFilename=mp.datasetLocation+"smoke_mln_25.txt";
        //String queryFilename=mp.datasetLocation+"smoke_mln_25_query.txt";

        String mlnFilename=mp.datasetLocation+"dummy_test.txt";
        String queryFilename=mp.datasetLocation+"dummy_test_query.txt";

        //String mlnFilename=System.getProperty("user.dir") +"smoke_mln_25.txt";

        System.out.println(mlnFilename);

        MLN originalMLN=mp.readMLN(mlnFilename);

        mp.readQuery(queryFilename, originalMLN);

        if(MarginalMap.print>0)
            originalMLN.printMLN();

        // Code copied from LMAP.run()
        MLN nonSameEquivMln = NonSameEquivConverter.convert(originalMLN);

        System.out.println("CONVERTED MLN");
        if(MarginalMap.print>0)
            nonSameEquivMln .printMLN();
        //Decomposer.ApplyDecomposer(originalMLN,,false);

/*
        Controller ctl=new Controller();
        ctl.computeMLN(originalMLN);
*/
    }

    public MLN readMLN(String mlnFilename){
        MLN originalMLN = new MLN();
        try {

            Parser parser = new Parser(originalMLN);
            parser.parseInputMLNFile(mlnFilename);

            // remove transitive clauses
            //removeTransitiveClauses(mln);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return originalMLN;
    }

    public MLN readQuery(String queryFilename, MLN originalMLN){
        try {

            Parser parser = new Parser(originalMLN,true);
            parser.parseQueryMLNFile(queryFilename);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return originalMLN;

    }
}
