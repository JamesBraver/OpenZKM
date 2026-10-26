package com.zelix.klassmaster.script.statements.javaccInterface;

import com.zelix.klassmaster.script.statements.filter.MatchAllFilter;

public interface PathFilter {
	MatchAllFilter MATCH_ALL = new MatchAllFilter();

	boolean matches(String path);

	String getDefinition();
}
