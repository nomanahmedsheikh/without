package org.utd.cs.gm.core;

public class VariableValuePair {
	
	private int varibaleId;
	
	private int valueIndex;

	public VariableValuePair(int i, int j) {
		this.varibaleId = i;
		this.valueIndex = j;
	}

	public int getVaribaleId() {
		return varibaleId;
	}

	public void setVaribaleId(int varibaleId) {
		this.varibaleId = varibaleId;
	}

	public int getValueIndex() {
		return valueIndex;
	}

	public void setValueIndex(int valueIndex) {
		this.valueIndex = valueIndex;
	}
	
}
