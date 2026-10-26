package com.zelix.klassmaster.script.statements.specifier;

import com.zelix.klassmaster.script.statements.javaccInterface.Specifier;
import com.zelix.utils.StringUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class SimplePackageSpecifier implements Specifier {
	private String pattern;

	public SimplePackageSpecifier(String pattern) {
		this.pattern = pattern;
	}

	@Override
	public String toRuleString() {
		return this.pattern.replace('/', '.') + '.';
	}

	@Override
	public String getPackageNamePrefix() {
		return this.toRuleString();
	}

	@Override
	public boolean isPackageExactMatch() {
		return false;
	}

	@Override
	public boolean matchesWildcard(String name) {
		return StringUtils.matchesWildcard(name, this.pattern);
	}

	@Override
	public double getMatchScore() {
		int starIndex = this.pattern.lastIndexOf("*");
		if (this.pattern.equals("*")) {
			return 1.0;
		} else {
			return starIndex > -1 ? 0.5 : 0.1;
		}
	}

	@Override
	public boolean isPackageRecursive() {
		return false;
	}

	@Override
	public List<String> getPackagePatterns() {
		ArrayList<String> packagePatterns = new ArrayList<>();
		StringTokenizer pathTokenizer = new StringTokenizer("/");

		while (pathTokenizer.hasMoreTokens()) {
			packagePatterns.add(pathTokenizer.nextToken());
		}

		return packagePatterns;
	}

	@Override
	public boolean isExactName() {
		return this.pattern.indexOf("*") == -1;
	}
}
