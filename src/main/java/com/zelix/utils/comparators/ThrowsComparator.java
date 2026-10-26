package com.zelix.utils.comparators;

import com.zelix.framework.attributes.code.ExceptionEntryNode;
import java.util.Comparator;

public class ThrowsComparator implements Comparator<ExceptionEntryNode> {
	public int compareByOffset(ExceptionEntryNode entry1, ExceptionEntryNode entry2) {
		int startOffsetDifference = entry1.getOffset(0) - entry2.getOffset(0);
		return startOffsetDifference == 0 ? entry1.getOffset(1) - entry2.getOffset(1) : startOffsetDifference;
	}

	@Override
	public int compare(ExceptionEntryNode o1, ExceptionEntryNode o2) {
		return this.compareByOffset(o1, o2);
	}
}
