package org.utd.cs.gm.core;

import java.util.ArrayList;
import java.util.List;

public class Variable implements Comparable<Variable> {
	
	private int id;
	
	private List<Integer> domain = new ArrayList<Integer>();
	
	private Integer value;
	
	private Integer addressValue;
	
	public Variable() {
	}

	public Variable(int id, List<Integer> domain) {
		this.id = id;
		this.domain = domain;
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public List<Integer> getDomain() {
		return domain;
	}

	public void setDomain(List<Integer> domain) {
		this.domain = domain;
	}
	
	public Integer getValue() {
		return value;
	}
	
	public void setValue(Integer value) {
		this.value = value;
	}
	
	public int getDomainSize() {
		return domain.size();
	}
	
	public Integer getAddressValue() {
		return addressValue;
	}
	
	public void setAddressValue(Integer addressValue) {
		this.addressValue = addressValue;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false;
		if (obj instanceof Variable) {
			Variable var = (Variable) obj;
			return (var.id == this.id);
		}
		return false;
	}
	
	@Override
	public int compareTo(Variable t) {
        if(this.id > t.id)
            return 1;
        
        if(this.id < t.id)
            return -1;
        
        return 0;
	}

	public static int getAddress(List<Variable> variables) {
		int address = 0;
		int multiplier = 1;
		
		for (int i = variables.size() - 1; i >= 0; i--) {
			Variable variable = variables.get(i);
			address += (multiplier * variable.getAddressValue());
			multiplier *= variable.getDomainSize();
		}
		
		return address;
	}
	
	public static void setAddress(List<Variable> variables, int address) {

		for (int i = variables.size() - 1; i >= 0; i--) {
			Variable variable = variables.get(i);
			variable.setAddressValue( address % variable.getDomainSize());
			address /= variable.getDomainSize();
		}
		
	}

	public static int getDomainSize(List<Variable> variables) {
		int domainSize = 1;
		
		for (Variable variable : variables) {
			domainSize *= variable.getDomainSize();
		}
		
		return domainSize;
	}
	
}
