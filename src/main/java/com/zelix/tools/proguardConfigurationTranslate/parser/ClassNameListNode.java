package com.zelix.tools.proguardConfigurationTranslate.parser;

import com.zelix.tools.proguardConfigurationTranslate.ProguardNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.ProguardNodeBase;
import com.zelix.tools.proguardConfigurationTranslate.javaccInterface.ClassPatternConsumer;

public class ClassNameListNode extends ProguardNodeBase {
	public ClassNameListNode(int commandId) {
		super(commandId);
	}

	@Override
	public void parseArguments(ProguardNode parent, ProguardScriptTranslator scriptTranslator) {
		int childCount = this.getChildrenCount();

		for (int i = 0; i < this.getChildrenCount(); i++) {
			this.getChild(i).parseArguments(this, scriptTranslator);
		}

		ClassPatternConsumer classPatternConsumer = (ClassPatternConsumer)this.getParent();

		for (int classNameIndex = 0; classNameIndex < childCount; classNameIndex++) {
			ClassNameNode classNameNode = (ClassNameNode)this.getChild(classNameIndex);
			String className = classNameNode.getValue();
			boolean negated = classNameNode.isNegated();
			boolean inList = childCount > 1;
			classPatternConsumer.addClassPattern(new ClassPattern(className, negated, inList));
		}
	}
}
