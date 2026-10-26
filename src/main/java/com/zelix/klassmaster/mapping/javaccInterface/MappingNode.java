package com.zelix.klassmaster.mapping.javaccInterface;

import com.zelix.tools.proguardConfigurationTranslate.ProguardTranslateWriter;

public interface MappingNode {
	void close();

	void open();

	void setParent(MappingNode parent);

	void addChild(MappingNode child, int index);

	MappingNode getChild(int index);

	int getChildCount();

	void interpret(MappingNode parent, ProguardTranslateWriter writer);
}
