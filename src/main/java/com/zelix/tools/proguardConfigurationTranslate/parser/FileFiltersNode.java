package com.zelix.tools.proguardConfigurationTranslate.parser;

import com.zelix.tools.proguardConfigurationTranslate.ProguardNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.ProguardNodeBase;

public class FileFiltersNode extends ProguardNodeBase {
	public FileFiltersNode(int commandId) {
		super(commandId);
	}

	@Override
	public void parseArguments(ProguardNode parent, ProguardScriptTranslator scriptTranslator) {
		ClassPathFiltersNode classPathFiltersNode = (ClassPathFiltersNode)this.getParent();
		int filterNodeCount = this.getChildrenCount();

		for (int i = 0; i < filterNodeCount; i++) {
			QuotedStringNode quotedStringNode = (QuotedStringNode)this.getChild(i);
			quotedStringNode.parseArguments(this, scriptTranslator);
			classPathFiltersNode.addFileFilter(quotedStringNode.getValue());
		}
	}
}
