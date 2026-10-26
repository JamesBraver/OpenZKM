package com.zelix.klassmaster.script.statements.annotation;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.javaccInterface.AnnotationMatcher;
import com.zelix.klassmaster.script.statements.javaccInterface.WildcardNameMatcher;
import com.zelix.utils.StringUtils;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

public class Annotation extends AbstractScriptNode implements AnnotationMatcher {
	private String dottedName;
	private String slashedName;

	public Annotation(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		StringBuilder dottedNameBuffer = new StringBuilder();
		int childrenCount = this.getChildrenCount();

		for (int i = 0; i < childrenCount; i++) {
			ScriptNode childNode = this.getChild(i);
			childNode.parseArguments(this, scriptContext);
			WildcardNameMatcher nameMatcher = (WildcardNameMatcher)childNode;
			dottedNameBuffer.append(nameMatcher.toRuleString());
		}

		this.dottedName = dottedNameBuffer.toString();
		this.slashedName = this.dottedName.replace('.', '/');
	}

	@Override
	public String toRuleString() {
		return "@" + this.dottedName;
	}

	public boolean isExactName() {
		return this.slashedName.indexOf("*") == -1;
	}

	@Override
	public boolean matchesAnnotationTypes(Set<String> annotationTypeNames) {
		Iterator<String> annotationTypeNameIterator = annotationTypeNames.iterator();

		while (annotationTypeNameIterator.hasNext()) {
			if (StringUtils.matchesWildcard(annotationTypeNameIterator.next(), this.slashedName)) {
				return true;
			}
		}

		return false;
	}
}
