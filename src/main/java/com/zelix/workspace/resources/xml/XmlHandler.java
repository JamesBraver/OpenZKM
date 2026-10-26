package com.zelix.workspace.resources.xml;

import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.utils.collections.DualKeyMap;
import java.util.List;
import java.util.Map;

public interface XmlHandler {
	void handleProcessingInstruction(
		String processingInstruction, Map<ClassNode, String> classReferences, Map<FieldNode, String> fieldReferences, Map<MethodNode, String> methodReferences, DualKeyMap<MethodNode, String, Object> methodAccessorReferences
	);

	void recordStartTagReferences(
		XmlElement element,
		Map<ClassNode, String> classReferences,
		Map<FieldNode, String> fieldReferences,
		Map<MethodNode, String> methodReferences,
		DualKeyMap<MethodNode, String, Object> methodAccessorReferences
	);

	void initialize();

	void resolveDeferredReferences(
		Map<FieldNode, String> fieldReferences, Map<MethodNode, String> methodReferences, DualKeyMap<MethodNode, String, Object> methodAccessorReferences
	);

	boolean supportsHierarchy();

	void handleEndTagByName(String tagName, List<Object> outputFragments);

	void handleCharacters(String text, List<Object> outputFragments);

	void handleEndTag(XmlElement element, List<Object> outputFragments);

	void recordEndTagReferences(
		XmlElement element,
		Map<ClassNode, String> classReferences,
		Map<FieldNode, String> fieldReferences,
		Map<MethodNode, String> methodReferences,
		DualKeyMap<MethodNode, String, Object> methodAccessorReferences
	);

	void handleCharacterData(String characterData, String tagName, List<Object> outputFragments);

	void recordTextReferences(
		String characterData,
		String tagName,
		Map<ClassNode, String> classReferences,
		Map<FieldNode, String> fieldReferences,
		Map<MethodNode, String> methodReferences,
		DualKeyMap<MethodNode, String, Object> methodAccessorReferences
	);

	void handleStartTag(XmlElement element, List<Object> outputFragments);
}
