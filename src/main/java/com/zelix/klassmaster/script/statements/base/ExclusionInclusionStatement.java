package com.zelix.klassmaster.script.statements.base;

import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;
import com.zelix.utils.enumerations.ArrayEnumeration;
import java.util.Enumeration;

public abstract class ExclusionInclusionStatement extends ExclusionInclusionStatementBase {
	public ExclusionInclusionStatement(int nodeType) {
		super(nodeType);
	}

	public final Enumeration<RenameFilterParameter> getRenameFilterParameters() {
		int filterCount = super.filterParameters.size();
		RenameFilterParameter[] filters = new RenameFilterParameter[filterCount];

		for (int i = 0; i < filterCount; i++) {
			filters[i] = (RenameFilterParameter)super.filterParameters.get(i);
		}

		return new ArrayEnumeration<>(filters);
	}
}
