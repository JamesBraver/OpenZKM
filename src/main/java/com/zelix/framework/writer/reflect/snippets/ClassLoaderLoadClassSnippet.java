package com.zelix.framework.writer.reflect.snippets;

import com.zelix.framework.instructions.AbstractInsnNode;
import com.zelix.framework.instructions.InsnNode;
import com.zelix.framework.instructions.WideConstantPoolInsnNode;
import com.zelix.framework.writer.MethodCallWriter;
import com.zelix.framework.writer.reflect.ReflectionRewriteSnippet;
import java.util.ArrayList;
import java.util.List;

import static com.zelix.framework.instructions.OpcodeConstants.INVOKESTATIC;
import static com.zelix.framework.instructions.OpcodeConstants.SWAP;

public class ClassLoaderLoadClassSnippet implements ReflectionRewriteSnippet {
	@Override
	public String[] getRequiredHelperClasses() {
		return null;
	}

	@Override
	public List<AbstractInsnNode> generateBytecode(MethodCallWriter handlerMethod, MethodCallWriter[] helperMethods) {
		ArrayList<AbstractInsnNode> instructions = new ArrayList<>();
		instructions.add(InsnNode.of(SWAP));
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, handlerMethod));
		instructions.add(InsnNode.of(SWAP));
		return instructions;
	}

	@Override
	public String getHandlerMethodName() {
		return "a";
	}

	@Override
	public String[] getHelperMethodDescriptors() {
		return null;
	}

	@Override
	public int getAdditionalStackRequired() {
		return 0;
	}

	@Override
	public String[] getHelperMethodNames() {
		return null;
	}

	@Override
	public String getHandlerMethodDescriptor() {
		return "(Ljava/lang/String;)Ljava/lang/String;";
	}
}
