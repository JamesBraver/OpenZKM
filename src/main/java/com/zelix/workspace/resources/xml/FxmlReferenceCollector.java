package com.zelix.workspace.resources.xml;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.references.AtomicReference;
import java.util.Map;

public class FxmlReferenceCollector implements FxmlElementCallback {
	public final FxmlHandler handler;
	private final String referencePrefix;
	private final Map<ClassNode, String> classReferences;
	private final Map<FieldNode, String> fieldReferences;
	private final Map<MethodNode, String> methodReferences;
	private final DualKeyMap<MethodNode, String, Object> resourceToClassReferences;

	public FxmlReferenceCollector(
		FxmlHandler handler, Map<ClassNode, String> classReferences, Map<FieldNode, String> fieldReferences, Map<MethodNode, String> methodReferences, DualKeyMap<MethodNode, String, Object> resourceToClassReferences
	) {
		this.handler = handler;
		this.referencePrefix = "reference in '" + FxmlHandler.getFullResourcePath(this.handler) + "' by '";
		this.classReferences = classReferences;
		this.fieldReferences = fieldReferences;
		this.methodReferences = methodReferences;
		this.resourceToClassReferences = resourceToClassReferences;
	}

	@Override
	public void handleQualifiedAttribute(XmlElement element, ClassNode classNode, String attributeName, String className, String memberName) {
		classNode.flagAsUsedInFxml();
		String referenceReason = GlobalUtils.toString(new String[]{this.referencePrefix, element.getName(), "' tag '", attributeName, "' attribute"});
		this.classReferences.put(classNode, referenceReason);
		this.handler.registerPropertyAccessors(classNode, memberName, this.methodReferences, this.resourceToClassReferences, referenceReason);
	}

	@Override
	public boolean handleFxIdInController(XmlElement element, ClassNode controllerClass, AbstractClassNode elementClass, String attributeName, String idValue, AtomicReference<String> idRef) {
		String referenceReason = GlobalUtils.toString(new String[]{this.referencePrefix, element.getName(), "' tag '", attributeName, "' attribute '", idRef.get(), "' value"});
		return this.handler.registerFieldInHierarchy(controllerClass, elementClass, idValue, this.fieldReferences, referenceReason);
	}

	@Override
	public boolean handleHandlerInController(XmlElement element, ClassNode controllerClass, String attributeName, String handlerName, AtomicReference<String> handlerRef) {
		String referenceReason = GlobalUtils.toString(new String[]{this.referencePrefix, element.getName(), "' tag '", attributeName, "' attribute '", handlerRef.get(), "' value"});
		return this.handler.registerMatchingMethods(controllerClass, handlerName, null, null, FxmlHandler.getMethodMatcher(this.handler), this.methodReferences, this.resourceToClassReferences, referenceReason);
	}

	@Override
	public void handlePropertyTag(XmlElement element, ClassNode ownerClass) {
		this.handler.registerPropertyAccessors(ownerClass, element.getName(), this.methodReferences, this.resourceToClassReferences, GlobalUtils.toString(new String[]{this.referencePrefix, element.getName(), "' tag"}));
	}

	@Override
	public void handleQualifiedTag(XmlElement element, ClassNode classNode, String simpleClassName, String propertyName) {
		classNode.flagAsUsedInFxml();
		this.handler.registerPropertyAccessors(classNode, propertyName, this.methodReferences, this.resourceToClassReferences, GlobalUtils.toString(new String[]{this.referencePrefix, element.getName(), "' tag"}));
	}

	@Override
	public void handleFactoryAttribute(XmlElement element, ClassNode ownerClass, String attributeName, AtomicReference<String> factoryRef) {
		String factoryName = factoryRef.get();
		String referenceReason = GlobalUtils.toString(new String[]{this.referencePrefix, element.getName(), "' tag '", attributeName, "' attribute '", factoryName, "' value"});
		this.handler.registerMatchingMethods(ownerClass, factoryName, null, null, FxmlHandler.getMethodMatcher(this.handler), this.methodReferences, this.resourceToClassReferences, referenceReason);
	}

	@Override
	public boolean handleHandlerByAnnotation(XmlElement element, String attributeName, String handlerName, AtomicReference<String> handlerRef) {
		String referenceReason = GlobalUtils.toString(new String[]{this.referencePrefix, element.getName(), "' tag '", attributeName, "' attribute '", handlerRef.get(), "' value"});
		return this.handler.registerWorkspaceMethods(handlerName, null, null, FxmlHandler.getMethodMatcher(this.handler), "javafx/fxml/FXML", 0, this.classReferences, this.methodReferences, this.resourceToClassReferences, referenceReason);
	}

	@Override
	public void handleReferencedClass(XmlElement element, ClassNode classNode) {
		this.classReferences.put(classNode, GlobalUtils.toString(new String[]{this.referencePrefix, element.getName(), "' tag"}));
	}

	@Override
	public void handleConstantAttribute(XmlElement element, ClassNode ownerClass, String attributeName, AtomicReference<String> constantRef) {
		this.handler
			.registerFieldsInOuterClasses(ownerClass, constantRef.get(), this.fieldReferences, GlobalUtils.toString(new String[]{this.referencePrefix, element.getName(), "' tag '", attributeName, "' attribute '", constantRef.get(), "' value"}));
	}

	@Override
	public void handleResourceReference(XmlElement element, String attributeName, String resourcePath, AtomicReference<String> resourcePathRef) {
	}

	@Override
	public void handleFqnClassTag(XmlElement element, ClassNode classNode) {
		classNode.flagAsFxmlFqnTag();
		classNode.flagAsUsedInFxml();
	}

	@Override
	public void handleSimpleClassTag(XmlElement element, ClassNode classNode) {
		classNode.flagAsUsedInFxml();
	}

	@Override
	public void logWarning(String level, String message) {
	}

	@Override
	public void handlePropertyAttribute(XmlElement element, ClassNode elementClass, String attributeName) {
		String referenceReason = GlobalUtils.toString(new String[]{this.referencePrefix, element.getName(), "' tag '", attributeName, "' attribute"});
		this.handler.registerPropertyAccessors(elementClass, attributeName, this.methodReferences, this.resourceToClassReferences, referenceReason);
	}

	@Override
	public boolean handleFxIdInElementClass(XmlElement element, AbstractClassNode elementClass, String attributeName, String idValue, AtomicReference<String> idRef) {
		String referenceReason = GlobalUtils.toString(new String[]{this.referencePrefix, element.getName(), "' tag '", attributeName, "' attribute '", idRef.get(), "' value"});
		return this.handler.registerMatchingFields(elementClass, idValue, "javafx/fxml/FXML", 0, this.fieldReferences, referenceReason);
	}

	@Override
	public void handleControllerAttribute(XmlElement element, ClassNode controllerClass, String attributeName, AtomicReference<String> controllerRef) {
		String referenceReason = GlobalUtils.toString(new String[]{this.referencePrefix, element.getName(), "' tag '", attributeName, "' attribute"});
		this.classReferences.put(controllerClass, referenceReason);
	}
}
