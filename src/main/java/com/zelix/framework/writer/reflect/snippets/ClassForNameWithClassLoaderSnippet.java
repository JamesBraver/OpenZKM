package com.zelix.framework.writer.reflect.snippets;

import com.zelix.framework.instructions.AbstractInsnNode;
import com.zelix.framework.instructions.InsnNode;
import com.zelix.framework.instructions.WideConstantPoolInsnNode;
import com.zelix.framework.writer.MethodCallWriter;
import com.zelix.framework.writer.reflect.ReflectionRewriteSnippet;
import java.util.ArrayList;
import java.util.List;

import static com.zelix.framework.instructions.OpcodeConstants.*;

public class ClassForNameWithClassLoaderSnippet implements ReflectionRewriteSnippet {
	@Override
	public List<AbstractInsnNode> generateBytecode(MethodCallWriter handlerMethod, MethodCallWriter[] helperMethods) {
		ArrayList<AbstractInsnNode> instructions = new ArrayList<>();
		instructions.add(InsnNode.of(DUP2_X1));
		instructions.add(InsnNode.of(POP2));
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, handlerMethod));
		instructions.add(InsnNode.of(DUP_X2));
		instructions.add(InsnNode.of(POP));
		return instructions;
	}

	@Override
	public String[] getRequiredHelperClasses() {
		return null;
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
		return 2;
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
