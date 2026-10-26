package com.zelix.klassmaster.mapping.entry;

import com.zelix.klassmaster.mapping.base.MappingNodeBase;
import com.zelix.klassmaster.mapping.javaccInterface.MappingNameChangeNode;
import com.zelix.klassmaster.mapping.javaccInterface.MappingNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardTranslateWriter;

public class MappingClassBlockNode extends MappingNodeBase {
	public MappingClassBlockNode(int nodeType) {
		super(nodeType);
	}

	@Override
	public void interpret(MappingNode parent, ProguardTranslateWriter writer) {
		super.interpret(parent, writer);
		int childCount = this.getChildCount();
		MappingClassNameChangeNode classNameChange = (MappingClassNameChangeNode)this.getChild(0);
		String oldClassName = classNameChange.getOldName();
		writer.addClassMapping(classNameChange.getOldName(), classNameChange.getNewName());

		for (int childIndex = 1; childIndex < childCount; childIndex++) {
			MappingNameChangeNode memberNameChange = (MappingNameChangeNode)this.getChild(childIndex);
			memberNameChange.writeNameChange(oldClassName, writer);
		}
	}
}
