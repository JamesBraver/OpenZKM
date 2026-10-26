package com.zelix.framework.writer.reflect.snippets;

import com.zelix.framework.instructions.AbstractInsnNode;
import com.zelix.framework.instructions.InsnNode;
import com.zelix.framework.instructions.WideConstantPoolInsnNode;
import com.zelix.framework.writer.MethodCallWriter;
import com.zelix.framework.writer.reflect.ReflectionRewriteSnippet;
import java.util.ArrayList;
import java.util.List;

import static com.zelix.framework.instructions.OpcodeConstants.*;

public class LookupFindConstructorSnippet implements ReflectionRewriteSnippet {
	public static final String[] requiredHelperClasses = new String[]{"java/lang/invoke/MethodType"};
	public static final String[] helperMethodNames = new String[]{"parameterArray"};
	public static final String[] helperMethodDescriptors = new String[]{"()[Ljava/lang/Class;"};

	@Override
	public String[] getRequiredHelperClasses() {
		return requiredHelperClasses.clone();
	}

	@Override
	public List<AbstractInsnNode> generateBytecode(MethodCallWriter handlerMethod, MethodCallWriter[] helperMethods) {
		ArrayList<AbstractInsnNode> instructions = new ArrayList<>();
		instructions.add(InsnNode.of(DUP2_X1));
		instructions.add(InsnNode.of(SWAP));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, helperMethods[0]));
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, handlerMethod));
		instructions.add(InsnNode.of(DUP_X2));
		instructions.add(InsnNode.of(POP));
		return instructions;
	}

	@Override
	public String getHandlerMethodName() {
		return "b";
	}

	@Override
	public String[] getHelperMethodDescriptors() {
		return helperMethodDescriptors.clone();
	}

	@Override
	public int getAdditionalStackRequired() {
		return 2;
	}

	@Override
	public String[] getHelperMethodNames() {
		return helperMethodNames.clone();
	}

	@Override
	public String getHandlerMethodDescriptor() {
		return "(Ljava/lang/String;Ljava/lang/Class;[Ljava/lang/Class;)Ljava/lang/String;";
	}
}
