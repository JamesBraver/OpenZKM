package com.zelix.klassmaster.script.statements.annotation;

import com.zelix.framework.AbstractClassNode;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.javaccInterface.NameAcceptor;
import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;
import com.zelix.klassmaster.script.statements.specifier.SimplePackageSpecifier;
import com.zelix.klassmaster.script.statements.specifier.WildcardMaskPatternMatcher;
import java.io.IOException;

public class StandAloneAnnotation extends AbstractScriptNode implements NameAcceptor {
	private String annotationName;

	public StandAloneAnnotation(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		this.getChildrenCount();
		this.getChild(0).parseArguments(this, scriptContext);
		RenameFilterParameter annotationFilterParameter = (RenameFilterParameter)parent;
		if (this.annotationName != null) {
			annotationFilterParameter.setIsAnnotationType();
			String packageName = AbstractClassNode.getPackageName(this.annotationName);
			String simpleName = AbstractClassNode.getSimpleNameFromInternalName(this.annotationName);
			if (packageName.length() > 0) {
				SimplePackageSpecifier packageSpecifier = new SimplePackageSpecifier(packageName);
				annotationFilterParameter.setPackageSpecifier(packageSpecifier);
			}

			if (simpleName.length() > 0) {
				WildcardMaskPatternMatcher classSpecifier = new WildcardMaskPatternMatcher(simpleName);
				annotationFilterParameter.setClassSpecifier(classSpecifier);
			}

			annotationFilterParameter.addImplementsInterface("java/lang/annotation/Annotation");
			annotationFilterParameter.addImplementsAnnotationSpecifier(null);
			annotationFilterParameter.addClassAccessFlag("annotation");
		}
	}

	@Override
	public void addName(String name) {
		this.annotationName = name;
	}
}
