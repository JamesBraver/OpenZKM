package com.zelix.klassmaster.script.statements.specifier;

import com.zelix.framework.AccessFlags;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.annotation.ComplexAnnotationSpecifier;
import com.zelix.klassmaster.script.statements.base.ComplexMemberSpecifierBase;
import com.zelix.klassmaster.script.statements.javaccInterface.WildcardNameMatcher;
import com.zelix.klassmaster.script.statements.name.FieldName;
import com.zelix.klassmaster.script.statements.signature.FieldSignature;
import java.io.IOException;

public class ComplexFieldSpecifier extends ComplexMemberSpecifierBase {
	public ComplexFieldSpecifier(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		this.getChild(0).parseArguments(this, scriptContext);
		if (parent instanceof FieldSignature) {
			((FieldSignature)parent).setFieldSpecifier(this);
		}
	}

	public double calculateMatchScore(AccessFlags accessFlags, String fieldTypeDescriptor, ComplexAnnotationSpecifier annotationSpecifier) {
		double matchScore = 1.0;
		WildcardNameMatcher nameMatcher = (WildcardNameMatcher)this.getChild(0);
		if (nameMatcher instanceof FieldName) {
			String namePattern = nameMatcher.toRuleString();
			if (namePattern != null && !RenameFilterParameter.isWildcardStar(namePattern)) {
				if (!RenameFilterParameter.containsWildcard(namePattern)) {
					matchScore = 0.1;
				} else {
					matchScore = 0.5;
				}
			}
		}

		if (accessFlags != null) {
			if (accessFlags.isPositivePublic()) {
				matchScore *= 0.2;
			} else if (accessFlags.isDefaultAccess()) {
				matchScore *= 0.25;
			} else if (accessFlags.isPositiveProtected()) {
				matchScore *= 0.25;
			} else if (accessFlags.isPositivePrivate()) {
				matchScore *= 0.25;
			}

			if (accessFlags.isPositiveStatic()) {
				matchScore *= 0.2;
			}

			if (accessFlags.isPositiveFinal()) {
				matchScore *= 0.2;
			} else if (accessFlags.isPositiveVolatile()) {
				matchScore *= 0.01;
			}

			if (accessFlags.isPositiveTransient()) {
				matchScore *= 0.01;
			}
		}

		if (annotationSpecifier != null) {
			matchScore *= 0.1;
		}

		return matchScore;
	}

	@Override
	public boolean isExactName() {
		WildcardNameMatcher nameMatcher = (WildcardNameMatcher)this.getChild(0);
		return nameMatcher instanceof FieldName ? ((FieldName)nameMatcher).hasNoWildcard() : false;
	}
}
