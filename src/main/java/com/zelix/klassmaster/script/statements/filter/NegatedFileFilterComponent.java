package com.zelix.klassmaster.script.statements.filter;

import com.zelix.klassmaster.script.statements.base.FileFilterComponentBase;

public class NegatedFileFilterComponent extends FileFilterComponentBase {
	public NegatedFileFilterComponent(int nodeType) {
		super(nodeType, true);
	}
}
