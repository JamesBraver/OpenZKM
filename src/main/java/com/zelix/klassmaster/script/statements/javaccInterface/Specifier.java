package com.zelix.klassmaster.script.statements.javaccInterface;

import java.util.List;

public interface Specifier {
	String toRuleString();

	String getPackageNamePrefix();

	boolean isPackageExactMatch();

	boolean matchesWildcard(String name);

	double getMatchScore();

	boolean isPackageRecursive();

	List<String> getPackagePatterns();

	boolean isExactName();
}
