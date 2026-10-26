package com.zelix.workspace.resources.xml;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.utils.references.AtomicReference;

public interface FxmlElementCallback {
	boolean handleFxIdInController(
		XmlElement element, ClassNode controllerClass, AbstractClassNode elementClass, String attributeName, String idValue, AtomicReference<String> idRef
	);

	void handleQualifiedAttribute(XmlElement element, ClassNode classNode, String attributeName, String className, String memberName);

	boolean handleHandlerInController(XmlElement element, ClassNode controllerClass, String attributeName, String handlerName, AtomicReference<String> handlerRef);

	void handlePropertyTag(XmlElement element, ClassNode ownerClass);

	void handleQualifiedTag(XmlElement element, ClassNode classNode, String simpleClassName, String propertyName);

	void handleFactoryAttribute(XmlElement element, ClassNode ownerClass, String attributeName, AtomicReference<String> factoryRef);

	boolean handleHandlerByAnnotation(XmlElement element, String attributeName, String handlerName, AtomicReference<String> handlerRef);

	void handleReferencedClass(XmlElement element, ClassNode classNode);

	void handleConstantAttribute(XmlElement element, ClassNode ownerClass, String attributeName, AtomicReference<String> constantRef);

	void handleResourceReference(XmlElement element, String attributeName, String resourcePath, AtomicReference<String> resourcePathRef);

	void handleFqnClassTag(XmlElement element, ClassNode classNode);

	void handleSimpleClassTag(XmlElement element, ClassNode classNode);

	void logWarning(String level, String message);

	void handlePropertyAttribute(XmlElement element, ClassNode elementClass, String attributeName);

	boolean handleFxIdInElementClass(XmlElement element, AbstractClassNode elementClass, String attributeName, String idValue, AtomicReference<String> idRef);

	void handleControllerAttribute(XmlElement element, ClassNode controllerClass, String attributeName, AtomicReference<String> controllerRef);
}
