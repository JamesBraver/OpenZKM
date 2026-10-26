package com.zelix.utils.dataset;

import java.io.Serializable;

public class IntPair implements Serializable {
	private int key;
	private int value;

	public IntPair(int key, int value) {
		this.key = key;
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

	public int getKey() {
		return this.key;
	}
}
