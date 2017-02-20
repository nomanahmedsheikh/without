package org.utd.cs.mln.alchemy.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Term {

	// There is a one to one mapping between constants and the domain
	public int type;
	public List<Integer> domain = new ArrayList<Integer>();
	public List<Set<Integer>> segmentDomain = new ArrayList<Set<Integer>>(); 

	public Term() {
	}

	public Term(int type_, List<Integer> domain_) {
		type = type_;
		domain = new ArrayList<Integer>(domain_);
	}
	/*
	public Term(int type_, List<Set<Integer>> segment){
		type = type_;
		segmentDomain = new ArrayList<>(hyperCube_);
	}
	*/
	public Term(int type_, int value) {
		type = type_;
		domain = new ArrayList<Integer>(1);
		domain.set(0, value);
	}

	Term(Term term) {
		type = term.type;
		for (int k = 0; k < term.domain.size(); k++)
			domain.add(term.domain.get(k));
	}

	/*
	@Override
	public String toString() {
		return "Term [type=" + type + ", domain=" + domain + ", segmentDomain="
				+ segmentDomain + "]";
	}
	*/
	// Marginal MAP
	public String toString() {
		return this.type+"";
	}

}
