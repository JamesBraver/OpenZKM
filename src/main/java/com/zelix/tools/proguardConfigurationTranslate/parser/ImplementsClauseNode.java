package com.zelix.tools.proguardConfigurationTranslate.parser;

import com.zelix.tools.proguardConfigurationTranslate.ProguardNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.ProguardNodeBase;
import com.zelix.tools.proguardConfigurationTranslate.javaccInterface.AnnotationPatternConsumer;
import com.zelix.tools.proguardConfigurationTranslate.javaccInterface.NameConsumer;

public class ImplementsClauseNode extends ProguardNodeBase implements AnnotationPatternConsumer, NameConsumer {
	private String implementsClassName;
	private String implementsAnnotation;

	public ImplementsClauseNode(int commandId) {
		super(commandId);
	}

	@Override
	public void setName(String name) {
		this.implementsClassName = name;
	}

	@Override
	public void setAnnotationPattern(String annotationPattern) {
		this.implementsAnnotation = annotationPattern;
	}

	@Override
	public final void parseArguments(ProguardNode parent, ProguardScriptTranslator scriptTranslator) {
		for (int i = 0; i < this.getChildrenCount(); i++) {
			this.getChild(i).parseArguments(this, scriptTranslator);
		}

		ClassSpecification classSpecification = (ClassSpecification)this.getParent();
		classSpecification.setImplementsClassName(this.implementsClassName);
		if (this.implementsAnnotation != null) {
			classSpecification.setImplementsAnnotation(this.implementsAnnotation, scriptTranslator);
		}
	}
}
