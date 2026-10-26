package com.zelix.utils;

public class IntegerCache {
	public static final Integer NEGATIVE_ONE = -1;
	private static IntegerCache INSTANCE;
	private Integer[] cache;
	private int capacity;

	private IntegerCache(int capacity) {
		this.cache = new Integer[capacity];
		this.capacity = capacity;
	}

	public static IntegerCache getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new IntegerCache(1000);
		}

		return INSTANCE;
	}

	public Integer valueOf(int value) {
		if (value == -1) {
			return NEGATIVE_ONE;
		} else if (value > 32767) {
			return value;
		} else {
			synchronized (this) {
				if (value >= this.capacity) {
					int newCapacity = value * 2;
					Integer[] newCache = new Integer[newCapacity];
					System.arraycopy(this.cache, 0, newCache, 0, this.capacity);
					this.cache = newCache;
					this.capacity = newCapacity;
				}

				Integer cachedValue = this.cache[value];
				if (cachedValue == null) {
					cachedValue = value;
					this.cache[value] = cachedValue;
				}

				return cachedValue;
			}
		}
	}
}
