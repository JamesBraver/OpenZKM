package com.zelix.utils.dataset;

public class RenameProperty implements Comparable<RenameProperty> {
	private String name;
	private Object value;
	private Object extra;
	private Object changed;

	public RenameProperty(String name, Object value, Object extra) {
		this(name, value, extra, null);
	}

	public RenameProperty(String name, Object value, Object extra, Object changed) {
		this.name = name;
		this.value = value;
		this.extra = extra;
		this.changed = changed;
	}

	public RenameProperty(String name, Object value) {
		this(name, value, null, null);
	}

	public Object getExtra() {
		return this.extra;
	}

	public int compareByName(RenameProperty other) {
		return this.name.compareTo(other.name);
	}

	@Override
	public int compareTo(RenameProperty other) {
		return this.compareByName(other);
	}

	public String getName() {
		return this.name;
	}

	public Object isChanged() {
		return this.changed;
	}

	public Object getValue() {
		return this.value;
	}
}
