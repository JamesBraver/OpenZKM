package com.zelix.klassmaster.mapping.name;

import com.zelix.klassmaster.mapping.base.MappingNodeBase;
import com.zelix.klassmaster.mapping.javaccInterface.MappingNode;
import com.zelix.klassmaster.mapping.javaccInterface.NameConsumer;
import com.zelix.tools.proguardConfigurationTranslate.ProguardTranslateWriter;
import java.util.ArrayList;
import java.util.List;

public class MappingNameListNode extends MappingNodeBase implements NameConsumer {
	private List<String> nameParts = new ArrayList<>();

	public MappingNameListNode(int nodeType) {
		super(nodeType);
	}

	@Override
	public void setName(String name) {
		this.nameParts.add(name);
	}

	@Override
	public void interpret(MappingNode parent, ProguardTranslateWriter writer) {
		super.interpret(parent, writer);
		NameConsumer nameConsumer = (NameConsumer)parent;
		int namePartCount = this.nameParts.size();
		StringBuilder nameBuffer = new StringBuilder();

		for (int namePartIndex = 0; namePartIndex < namePartCount; namePartIndex++) {
			nameBuffer.append(this.nameParts.get(namePartIndex));
			if (namePartIndex < namePartCount - 1) {
				nameBuffer.append('.');
			}
		}

		nameConsumer.setName(nameBuffer.toString());
	}
}
