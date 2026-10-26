package com.zelix.framework.reference;

import com.zelix.framework.SettingFlag;
import com.zelix.framework.instructions.LabelNode;

public interface IGraphElement {
	SettingFlag getSettingFlag();

	void setLabel(Integer pos, LabelNode lbl);

	String getMnemonics();

	void registerToGraph(ReferenceGraph<LabelNode, IGraphElement> graph);
}
