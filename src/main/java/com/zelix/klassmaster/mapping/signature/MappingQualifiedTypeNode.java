package com.zelix.klassmaster.mapping.signature;

import com.zelix.klassmaster.mapping.base.MappingNodeBase;
import com.zelix.klassmaster.mapping.javaccInterface.MappingNode;
import com.zelix.klassmaster.mapping.javaccInterface.QualifiedTypeConsumer;
import com.zelix.tools.proguardConfigurationTranslate.ProguardTranslateWriter;
import java.util.ArrayList;
import java.util.List;

public class MappingQualifiedTypeNode extends MappingNodeBase {
	private List<String> nameParts = new ArrayList<>();
	private int arrayLevel;

	public MappingQualifiedTypeNode(int nodeType) {
		super(nodeType);
	}

	public void addNamePart(String namePart) {
		this.nameParts.add(namePart);
	}

	public void incrementArrayLevel() {
		this.arrayLevel++;
	}

	@Override
	public void interpret(MappingNode parent, ProguardTranslateWriter writer) {
		super.interpret(parent, writer);
		QualifiedTypeConsumer typeConsumer = (QualifiedTypeConsumer)parent;
		StringBuilder signatureBuilder = new StringBuilder();
		int namePartCount = this.nameParts.size();

		for (int namePartIndex = 0; namePartIndex < namePartCount; namePartIndex++) {
			String namePart = this.nameParts.get(namePartIndex);
			signatureBuilder.append(namePart);
			if (namePartIndex < namePartCount - 1) {
				signatureBuilder.append('.');
			}
		}

		for (int arrayLevelIndex = 0; arrayLevelIndex < this.arrayLevel; arrayLevelIndex++) {
			signatureBuilder.append("[]");
		}

		typeConsumer.setTypeSignature(signatureBuilder.toString());
	}
}
