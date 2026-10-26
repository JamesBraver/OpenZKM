package com.zelix.framework.variable;

public interface Indexable {
	int add(int increment);

	int increaseAndGet();

	int get();

	int getAndIncrease();
}
