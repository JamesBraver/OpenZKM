package com.zelix.framework.analyzer.dataset;

import com.zelix.framework.analyzer.reference.ArrayReference;
import com.zelix.framework.code.InstructionListNode;
import com.zelix.framework.constant.StringNode;

public class MethodAnalysisResult {
	public int paramSlot = Integer.MIN_VALUE;
	private StringNode stringNode;
	private String errorMessage;
	public ArrayReference reference;
	private InstructionListNode insnList;

	public MethodAnalysisResult(ArrayReference reference, InstructionListNode insnList) {
		this.reference = reference;
		this.insnList = insnList;
	}

	public MethodAnalysisResult(String errorMessage, InstructionListNode insnList) {
		this.errorMessage = errorMessage;
		this.insnList = insnList;
	}

	public MethodAnalysisResult(int paramSlot, InstructionListNode insnList) {
		this.paramSlot = paramSlot;
		this.insnList = insnList;
	}

	public MethodAnalysisResult(StringNode stringNode, InstructionListNode insnList) {
		this.stringNode = stringNode;
		this.insnList = insnList;
	}

	public InstructionListNode getInsnList() {
		return this.insnList;
	}

	public int getParamSlot() {
		return this.paramSlot;
	}

	public boolean hasParamSlot() {
		return this.paramSlot != Integer.MIN_VALUE;
	}

	public ArrayReference getReference() {
		return this.reference;
	}

	public boolean hasReference() {
		return this.reference != null;
	}

	public String getErrorMessage() {
		return this.errorMessage;
	}

	public boolean hasStringNode() {
		return this.stringNode != null;
	}

	public boolean hasErrorMessage() {
		return this.errorMessage != null;
	}

	public StringNode getStringNode() {
		return this.stringNode;
	}
}
