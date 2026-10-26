package com.zelix.utils.comparators;

import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;
import java.util.Comparator;

public class ExcludeSorter implements Comparator<RenameFilterParameter> {
	public int compareByTargetType(RenameFilterParameter filterParameter1, RenameFilterParameter filterParameter2) {
		if (filterParameter1.determineTargetType() < filterParameter2.determineTargetType()) {
			return -1;
		} else {
			return filterParameter1.determineTargetType() > filterParameter2.determineTargetType() ? 1 : filterParameter1.compareTo(filterParameter2);
		}
	}

	@Override
	public int compare(RenameFilterParameter o1, RenameFilterParameter o2) {
		return this.compareByTargetType(o1, o2);
	}
}
