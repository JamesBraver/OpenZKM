package com.zelix.utils.comparators;

import com.zelix.framework.wrappers.FieldNode;
import java.util.Comparator;

public class FieldNodeComparator implements Comparator<FieldNode> {
	public int compareByName(FieldNode field1, FieldNode field2) {
		String field1DisplayName = field1.getNameDisplay();
		String field2DisplayName = field2.getNameDisplay();
		return field1DisplayName.compareTo(field2DisplayName);
	}

	@Override
	public int compare(FieldNode o1, FieldNode o2) {
		return this.compareByName(o1, o2);
	}
}
