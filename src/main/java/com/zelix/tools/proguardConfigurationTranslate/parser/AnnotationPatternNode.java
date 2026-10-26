package com.zelix.tools.proguardConfigurationTranslate.parser;

import com.zelix.tools.proguardConfigurationTranslate.ProguardNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.ProguardNodeBase;
import com.zelix.tools.proguardConfigurationTranslate.javaccInterface.AnnotationPatternConsumer;
import com.zelix.tools.proguardConfigurationTranslate.javaccInterface.NameConsumer;

public class AnnotationPatternNode extends ProguardNodeBase implements NameConsumer {
	private String annotationPattern;

	public AnnotationPatternNode(int commandId) {
		super(commandId);
	}

	@Override
	public void setName(String name) {
		this.annotationPattern = name;
	}

	@Override
	public final void parseArguments(ProguardNode parent, ProguardScriptTranslator scriptTranslator) {
		((ClassNameNode)this.getChild(0)).parseArguments(this, scriptTranslator);
		((AnnotationPatternConsumer)this.getParent()).setAnnotationPattern(this.annotationPattern);
	}
}
