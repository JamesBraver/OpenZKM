package com.zelix.klassmaster.script.statements.signature;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.annotation.ComplexAnnotationSpecifier;
import com.zelix.klassmaster.script.statements.base.MethodSignatureBase;
import com.zelix.klassmaster.script.statements.javaccInterface.NameAcceptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class MethodArguments extends AbstractScriptNode implements NameAcceptor {
	private ArrayList<String> argumentDescriptors = new ArrayList<>();
	private ArrayList<ComplexAnnotationSpecifier> annotationSpecifiers = new ArrayList<>();

	public MethodArguments(int nodeType) {
		super(nodeType);
	}

	public static String escapeRegex(String argumentDescriptor) {
		StringBuilder escapedBuffer = new StringBuilder();

		for (int i = 0; i < argumentDescriptor.length(); i++) {
			char currentChar = argumentDescriptor.charAt(i);
			switch (currentChar) {
				case '$':
					escapedBuffer.append("\\$");
					break;
				case '[':
					escapedBuffer.append("\\[");
					break;
				default:
					escapedBuffer.append(currentChar);
			}
		}

		return escapedBuffer.toString();
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		int childCount = this.getChildrenCount();

		for (int childIndex = 0; childIndex < childCount; childIndex++) {
			this.getChild(childIndex).parseArguments(this, scriptContext);
		}

		MethodSignatureBase methodSignatureBase = (MethodSignatureBase)parent;
		methodSignatureBase.setAnnotationSpecifiers(this.annotationSpecifiers.toArray(new ComplexAnnotationSpecifier[this.annotationSpecifiers.size()]));
		methodSignatureBase.setMethodArgumentSpecifier(new MethodArgumentSpecifier(this.argumentDescriptors.size(), this.getRawDescriptor(), this.buildMatchingPattern()));
	}

	public String getRawDescriptor() {
		StringBuffer descriptorBuffer = new StringBuffer();
		descriptorBuffer.append('(');
		if (this.argumentDescriptors != null) {
			int argumentCount = this.argumentDescriptors.size();

			for (int i = 0; i < argumentCount; i++) {
				String argumentDescriptor = this.argumentDescriptors.get(i);
				descriptorBuffer.append(argumentDescriptor);
			}
		}

		descriptorBuffer.append(')');
		return descriptorBuffer.toString();
	}

	public void addAnnotationSpecifier(ComplexAnnotationSpecifier annotationSpecifier) {
		this.annotationSpecifiers.add(annotationSpecifier);
	}

	@Override
	public void addName(String name) {
		this.argumentDescriptors.add(name);
	}

	private Pattern buildMatchingPattern() {
		StringBuilder patternBuffer = new StringBuilder();
		patternBuffer.append("\\(");

		for (String argumentDescriptor : this.argumentDescriptors) {
			if (argumentDescriptor.equals("*")) {
				if (this.argumentDescriptors.size() == 1) {
					patternBuffer.append(".*");
				} else {
					patternBuffer.append("((\\[*)(([BCDFIJSZ]|L[^;]+;)))");
				}
			} else if (argumentDescriptor.equals("?")) {
				patternBuffer.append("((\\[*)(([BCDFIJSZ]|L[^;]+;)))");
			} else {
				patternBuffer.append(escapeRegex(argumentDescriptor));
			}
		}

		patternBuffer.append("\\)");
		return Pattern.compile(patternBuffer.toString());
	}
}
