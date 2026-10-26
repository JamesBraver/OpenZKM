package com.zelix.klassmaster.script.statements.filter;

import com.zelix.klassmaster.script.statements.javaccInterface.PathFilter;

public class MatchAllFilter implements PathFilter {
	@Override
	public boolean matches(String path) {
		return true;
	}

	@Override
	public String getDefinition() {
		return "";
	}
}
