package org.utd.cs.mln.alchemy.core;

import org.utd.cs.gm.utility.Pair;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.*;

public class Evidence {
	public Map<Integer, Integer> predIdVal; // Each pair is a groundPredIndex and value given in the evidence. GroundPredIndex is according to
	public Evidence() {
		predIdVal = new HashMap<>();
	}


}


