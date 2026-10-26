package com.zelix.framework.attributes.annotations;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.StructureNode;
import com.zelix.framework.annotation.AnnotationElementNode;
import com.zelix.framework.annotation.AnnotationNode;
import com.zelix.framework.annotation.AnnotationParameterNode;
import com.zelix.framework.annotation.EnumAnnotationElement;
import com.zelix.framework.attributes.base.AbstractAnnotationsAttributeNode;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.klassmaster.config.ObfuscateConfig;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class RuntimeInvisibleAnnotationsAttribute extends AbstractAnnotationsAttributeNode {
	public RuntimeInvisibleAnnotationsAttribute(
		StructureNode parent, int nameIndex, String attributeName, ByteDataBuffer buf, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References, PrintWriter printWriter
	) throws IOException {
		super(parent, nameIndex, attributeName, buf, utf8References, printWriter, "RuntimeInvisibleAnnotations");
		if (super.isParsedSuccessfully && this.getClassNode().isWorkspaceNode()) {
			AnnotationNode classLevelAnnotation = this.getAnnotationByType("com/zelix/annotation/ZKMClassLevel", true);
			if (classLevelAnnotation == null) {
				AnnotationNode methodLevelAnnotation = this.getAnnotationByType("com/zelix/annotation/ZKMMethodLevel", true);
				if (methodLevelAnnotation != null) {
					MethodNode methodNode = (MethodNode)this.getParent();
					this.applyMethodConfigAnnotation(methodNode, methodLevelAnnotation, printWriter);
				}
			} else {
				ClassNode classNode = (ClassNode)this.getParent();
				this.applyClassConfigAnnotation(classNode, classLevelAnnotation, printWriter);
			}
		}
	}

	private AnnotationSettings applyMethodConfigAnnotation(MethodNode methodNode, AnnotationNode annotationNode, PrintWriter printWriter) {
		AnnotationSettings methodConfig = null;
		if (annotationNode.params.length > 0) {
			methodConfig = new AnnotationSettings(this);
			this.applyEnumAnnotationParams(annotationNode, methodConfig, printWriter);
			methodNode.setMethodConfig(methodConfig);
		}

		return methodConfig;
	}

	private void applyEnumAnnotationParams(AnnotationNode annotationNode, AnnotationSettings annotationSet, PrintWriter printWriter) {
		for (AnnotationParameterNode parameter : annotationNode.params) {
			String parameterName = parameter.getName();
			AnnotationElementNode annotationElement = parameter.getElement();
			if (annotationElement.getTypeTag() == 101) {
				EnumAnnotationElement enumElement = (EnumAnnotationElement)annotationElement;
				enumElement.getEnumTypeDescriptor();
				String enumValueName = enumElement.getEnumValueName();
				annotationSet.put(parameterName, ObfuscateConfig.policyNameToConfigName(enumValueName));
			} else {
				printWriter.println("ERROR: " + this.getRawSourcePath() + " :  Invalid tag '" + (char)annotationElement.getTypeTag() + "' in 'com/zelix/annotation/ZKMClassLevel'");
			}
		}
	}

	private AnnotationSettings applyClassConfigAnnotation(ClassNode classNode, AnnotationNode annotationNode, PrintWriter printWriter) {
		AnnotationSettings classConfig = null;
		if (annotationNode.params.length > 0) {
			classConfig = new AnnotationSettings(this);
			this.applyEnumAnnotationParams(annotationNode, classConfig, printWriter);
			classNode.setClassAnnotationSet(classConfig);
		}

		return classConfig;
	}

	public void removeAnnotationsByType(String typeInternalName, ScriptContext scriptContext) {
		ArrayList<AnnotationNode> retainedAnnotations = new ArrayList<>(super.annotations.length);

		for (AnnotationNode annotation : super.annotations) {
			if (annotation.getTypeInternalName().equals(typeInternalName)) {
				if (scriptContext != null && scriptContext.isVerbose()) {
					StructureNode parentNode = this.getParent();
					scriptContext.getWriter()
						.println(
							"Removing the processing specification annotation '"
								+ AbstractClassNode.internalToBinaryName(typeInternalName)
								+ "' from "
								+ (
									parentNode.isClassNode()
										? " class '" + ((ClassNode)parentNode).getRawSourcePath() + "'"
										: " method '" + ((MethodNode)parentNode).getSignature() + "' in class '" + ((ClassNode)parentNode.getParent()).getRawSourcePath() + "'"
								)
						);
				}
			} else {
				retainedAnnotations.add(annotation);
			}
		}

		super.annotations = retainedAnnotations.toArray(new AnnotationNode[retainedAnnotations.size()]);
	}
}
