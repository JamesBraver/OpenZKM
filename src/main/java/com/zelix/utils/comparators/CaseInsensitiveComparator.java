package com.zelix.utils.comparators;

import java.util.Comparator;

public class CaseInsensitiveComparator implements Comparator<String> {
	private static final CaseInsensitiveComparator INSTANCE = new CaseInsensitiveComparator();

	private CaseInsensitiveComparator() {
	}

	public static CaseInsensitiveComparator getInstance() {
		return INSTANCE;
	}

	@Override
	public int compare(String o1, String o2) {
		return this.compare$synthetic(o1, o2);
	}

	public int compare$synthetic(String o1, String o2) {
		return o1.compareToIgnoreCase(o2);
	}
}
