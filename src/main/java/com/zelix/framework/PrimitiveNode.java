package com.zelix.framework;

import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.GlobalUtils;
import java.util.Map;

public class PrimitiveNode implements Types {
	private static final Map<String, Class<?>> DESCRIPTOR_TO_CLASS = GlobalUtils.newHashMap();
	private static final Map<String, PrimitiveNode> DESCRIPTOR_TO_NODE = GlobalUtils.newHashMap();
	private final String descriptor;

	private PrimitiveNode(String descriptor) {
		this.descriptor = descriptor;
	}

	public static PrimitiveNode forDescriptor(String descriptor) {
		return DESCRIPTOR_TO_NODE.get(descriptor);
	}

	public static String getClassDescriptor() {
		return "Ljava/lang/Class;";
	}

	public static String getTypeFieldName() {
		return "TYPE";
	}

	@Override
	public boolean isPrimitive() {
		return true;
	}

	public String getWrapperName() {
		return TypeWriter.getWrapperName(this.descriptor);
	}

	@Override
	public String getInternalName() {
		return this.descriptor;
	}

	static {
		DESCRIPTOR_TO_CLASS.put("B", byte.class);
		DESCRIPTOR_TO_CLASS.put("Z", boolean.class);
		DESCRIPTOR_TO_CLASS.put("S", short.class);
		DESCRIPTOR_TO_CLASS.put("C", char.class);
		DESCRIPTOR_TO_CLASS.put("I", int.class);
		DESCRIPTOR_TO_CLASS.put("J", long.class);
		DESCRIPTOR_TO_CLASS.put("F", float.class);
		DESCRIPTOR_TO_CLASS.put("D", double.class);
		DESCRIPTOR_TO_CLASS.put("V", void.class);
		DESCRIPTOR_TO_NODE.put("B", new PrimitiveNode("B"));
		DESCRIPTOR_TO_NODE.put("Z", new PrimitiveNode("Z"));
		DESCRIPTOR_TO_NODE.put("S", new PrimitiveNode("S"));
		DESCRIPTOR_TO_NODE.put("C", new PrimitiveNode("C"));
		DESCRIPTOR_TO_NODE.put("I", new PrimitiveNode("I"));
		DESCRIPTOR_TO_NODE.put("J", new PrimitiveNode("J"));
		DESCRIPTOR_TO_NODE.put("F", new PrimitiveNode("F"));
		DESCRIPTOR_TO_NODE.put("D", new PrimitiveNode("D"));
		DESCRIPTOR_TO_NODE.put("V", new PrimitiveNode("V"));
	}
}
