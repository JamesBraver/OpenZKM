package com.zelix.utils.dataset;

public class PriorityEntry<T> implements Comparable<PriorityEntry<T>> {
	private int priority;
	private T value;

	public PriorityEntry(int priority, T value) {
		this.priority = priority;
		this.value = value;
	}

	public final T getValue() {
		return this.value;
	}

	public final int getPriority() {
		return this.priority;
	}

	@Override
	@SuppressWarnings("unchecked")
	public int compareTo(PriorityEntry<T> o) {
		if (this.priority < o.priority) {
			return -1;
		} else if (this.priority != o.priority) {
			return 1;
		} else {
			return this.value instanceof Comparable && o.value instanceof Comparable ? ((Comparable<Object>)this.value).compareTo(o.value) : 0;
		}
	}
}
