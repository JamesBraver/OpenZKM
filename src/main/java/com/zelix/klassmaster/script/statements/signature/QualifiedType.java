package com.zelix.klassmaster.script.statements.signature;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.javaccInterface.NameAcceptor;
import com.zelix.utils.GlobalUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class QualifiedType extends AbstractScriptNode {
	public static final Map<String, String> primitiveTypeCodes = GlobalUtils.newHashMap(23);
	public ArrayList<String> nameParts = new ArrayList<>();
	public int arrayLevel;

	public QualifiedType(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		int childCount = this.getChildrenCount();

		for (int childIndex = 0; childIndex < childCount; childIndex++) {
			this.getChild(childIndex).parseArguments(this, scriptContext);
		}

		((NameAcceptor)parent).addName(this.getDescriptor());
	}

	public void addArrayLevel() {
		this.arrayLevel++;
	}

	public String getDescriptor() {
		StringBuffer descriptorBuffer = new StringBuffer();
		int namePartCount = this.nameParts.size();
		if (namePartCount != 0) {
			for (int dimensionIndex = 0; dimensionIndex < this.arrayLevel; dimensionIndex++) {
				descriptorBuffer.append("[");
			}

			if (namePartCount == 1) {
				String typeName = this.nameParts.get(0);
				String primitiveTypeCode = primitiveTypeCodes.get(typeName);
				if (primitiveTypeCode != null) {
					descriptorBuffer.append(primitiveTypeCode);
				} else {
					descriptorBuffer.append("L");
					descriptorBuffer.append(typeName);
					descriptorBuffer.append(";");
				}
			} else {
				descriptorBuffer.append("L");

				for (int namePartIndex = 0; namePartIndex < namePartCount; namePartIndex++) {
					String namePart = this.nameParts.get(namePartIndex);
					if (namePartIndex > 0) {
						descriptorBuffer.append("/");
					}

					descriptorBuffer.append(namePart);
				}

				descriptorBuffer.append(";");
			}
		}

		return descriptorBuffer.toString();
	}

	public void addNamePart(String namePart) {
		this.nameParts.add(namePart);
	}

	static {
		primitiveTypeCodes.put("byte", "B");
		primitiveTypeCodes.put("char", "C");
		primitiveTypeCodes.put("double", "D");
		primitiveTypeCodes.put("float", "F");
		primitiveTypeCodes.put("int", "I");
		primitiveTypeCodes.put("long", "J");
		primitiveTypeCodes.put("short", "S");
		primitiveTypeCodes.put("boolean", "Z");
	}
}
