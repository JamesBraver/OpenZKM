package com.zelix.framework.annotation;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.script.ScriptContext;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public interface AnnotationElement {
	boolean isValid();

	void syncName();

	void acceptVisitor(ConstantPoolVisitor visitor);

	void write(DataOutputStream dataOutputStream) throws IOException;

	void writeRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> remappedConstants, ScriptContext scriptContext) throws IOException;

	void remapDescriptors(HashMap<String, String> packageMappings, HashMap<String, String> classNameMappings);

	void collectClassReferences(Set<AbstractClassNode> classReferences);

	void updateConstants();

	int getSize();
}
