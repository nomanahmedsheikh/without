package org.utd.cs.mln.alchemy.core;

import java.util.List;

public class Domain {

	public String name;
	public List<String> values;

	public Domain(String name_,List<String> values_){ 
		name = name_;
		values = values_;
	}

	@Override
	public String toString() {
		return "Domain [name=" + name + ", values=" + values + "]";
	}
}
