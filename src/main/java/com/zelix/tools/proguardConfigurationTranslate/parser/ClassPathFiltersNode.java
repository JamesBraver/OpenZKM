package com.zelix.tools.proguardConfigurationTranslate.parser;

import com.zelix.tools.proguardConfigurationTranslate.ProguardNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.ProguardNodeBase;
import com.zelix.tools.proguardConfigurationTranslate.javaccInterface.ClassPathFilterAcceptor;

public class ClassPathFiltersNode extends ProguardNodeBase implements ClassPathFilterAcceptor {
	public ClassPathFiltersNode(int commandId) {
		super(commandId);
	}

	@Override
	public boolean addFileFilter(String filter) {
		return ((ClassPathEntryNode)this.getParent()).addFileFilter(filter);
	}

	@Override
	public boolean addArchiveFilter(String filter) {
		return ((ClassPathEntryNode)this.getParent()).addArchiveFilter(filter);
	}

	@Override
	public void parseArguments(ProguardNode parent, ProguardScriptTranslator scriptTranslator) {
		int childCount = this.getChildrenCount();

		for (int i = 0; i < childCount; i++) {
			this.getChild(i).parseArguments(this, scriptTranslator);
		}
	}
}
