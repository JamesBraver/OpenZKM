package com.zelix.framework.attributes.code;

import com.zelix.framework.instructions.LabelNode;
import com.zelix.framework.writer.ClassInfoWriter;

public class TryCatchBlockInfo {
	private final ClassInfoWriter exceptionType;
	private final LabelNode start;
	private final LabelNode end;
	private final LabelNode handler;

	public TryCatchBlockInfo(ClassInfoWriter exceptionType, LabelNode start, LabelNode end, LabelNode handler) {
		this.exceptionType = exceptionType;
		this.start = start;
		this.end = end;
		this.handler = handler;
	}

	public LabelNode getEndLabel() {
		return this.end;
	}

	public LabelNode getStartLabel() {
		return this.start;
	}

	public LabelNode getHandlerLabel() {
		return this.handler;
	}

	public ClassInfoWriter getExceptionType() {
		return this.exceptionType;
	}
}
