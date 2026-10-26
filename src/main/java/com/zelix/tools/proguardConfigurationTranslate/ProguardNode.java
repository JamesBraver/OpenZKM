package com.zelix.tools.proguardConfigurationTranslate;

public interface ProguardNode {
	int getChildrenCount();

	void setParent(ProguardNode parent);

	ProguardNode getParent();

	void onParseStart();

	ProguardNode getChild(int index);

	void parseArguments(ProguardNode parent, ProguardScriptTranslator scriptTranslator);

	void setChild(ProguardNode child, int index);

	void onParseComplete();
}
