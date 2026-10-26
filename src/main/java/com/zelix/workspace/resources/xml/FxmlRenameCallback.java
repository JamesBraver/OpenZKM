package com.zelix.workspace.resources.xml;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.utils.FileUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.references.AtomicReference;

public class FxmlRenameCallback implements FxmlElementCallback {
	public final FxmlHandler handler;

	public FxmlRenameCallback(FxmlHandler handler) {
		this.handler = handler;
	}

	@Override
	public void handleQualifiedAttribute(XmlElement element, ClassNode classNode, String attributeName, String className, String memberName) {
		String tagName = element.getName();
		AtomicReference<String> memberRef = new AtomicReference<>(memberName);
		String mappedInternalName = GlobalUtils.getMappedOrDefault(classNode.getInternalName(), FxmlHandler.getFullRemappingMap(this.handler));
		this.handler.updateBeanPropertyName(classNode.getBinaryName(), mappedInternalName, tagName, memberName, memberRef);
		if (!memberRef.get().equals(memberName) || !classNode.getInternalName().equals(mappedInternalName)) {
			String qualifiedMemberName = GlobalUtils.toString(new String[]{className.indexOf(46) == -1 ? classNode.getSimpleNameInternal() : classNode.getBinaryName(), ".", memberRef.get()});
			element.renameAttribute(attributeName, qualifiedMemberName);
		}
	}

	@Override
	public boolean handleFxIdInController(XmlElement element, ClassNode controllerClass, AbstractClassNode elementClass, String attributeName, String idValue, AtomicReference<String> idRef) {
		element.getName();
		String mappedIdName = this.handler.getMappedFieldName(controllerClass, elementClass, idValue);
		if (mappedIdName != null && !mappedIdName.equals(idValue)) {
			idRef.set(mappedIdName);
		}

		return mappedIdName != null;
	}

	@Override
	public boolean handleHandlerInController(XmlElement element, ClassNode controllerClass, String attributeName, String handlerName, AtomicReference<String> handlerRef) {
		element.getName();
		String mappedHandlerName = this.handler.findMappedMethodName(controllerClass.getBinaryName(), handlerName, FxmlHandler.getMethodMatcher(this.handler), 0);
		if (mappedHandlerName != null && !mappedHandlerName.equals(handlerName)) {
			String newHandlerValue = GlobalUtils.toString(new String[]{String.valueOf('#'), mappedHandlerName});
			handlerRef.set(newHandlerValue);
		}

		return mappedHandlerName != null;
	}

	@Override
	public void handlePropertyTag(XmlElement element, ClassNode ownerClass) {
		String tagName = element.getName();
		AtomicReference<String> tagNameRef = new AtomicReference<>(tagName);
		this.handler.updateBeanPropertyName(ownerClass.getBinaryName(), GlobalUtils.getMappedOrDefault(ownerClass.getInternalName(), FxmlHandler.getFullRemappingMap(this.handler)), tagName, tagName, tagNameRef);
		if (!tagNameRef.get().equals(tagName)) {
			element.setName(tagNameRef.get());
		}
	}

	@Override
	public void handleQualifiedTag(XmlElement element, ClassNode classNode, String simpleClassName, String propertyName) {
		String mappedPropertyName = this.handler.getMappedFieldName(classNode.getInternalName(), propertyName);
		if (!classNode.getSimpleNameInternal().equals(simpleClassName) || !propertyName.equals(mappedPropertyName)) {
			String qualifiedPropertyName = GlobalUtils.toString(new String[]{classNode.getSimpleNameInternal(), ".", mappedPropertyName});
			element.setName(qualifiedPropertyName);
		}
	}

	@Override
	public void handleFactoryAttribute(XmlElement element, ClassNode ownerClass, String attributeName, AtomicReference<String> factoryRef) {
		String factoryMethodName = factoryRef.get();
		String mappedFactoryMethodName = this.handler.findMappedMethodName(ownerClass.getBinaryName(), factoryMethodName, BeanPropertyXmlHandler.GETTER_PATTERN, 8);
		if (mappedFactoryMethodName != null && !mappedFactoryMethodName.equals(factoryMethodName)) {
			factoryRef.set(mappedFactoryMethodName);
		}
	}

	@Override
	public boolean handleHandlerByAnnotation(XmlElement element, String attributeName, String handlerName, AtomicReference<String> handlerRef) {
		element.getName();
		AtomicReference<String> errorMessageRef = new AtomicReference<>();
		String mappedHandlerName = this.handler.resolveMethodName(handlerName, FxmlHandler.getMethodMatcher(this.handler), "javafx/fxml/FXML", 0, errorMessageRef);
		if (mappedHandlerName != null && !mappedHandlerName.equals(handlerName)) {
			String newHandlerValue = GlobalUtils.toString(new String[]{String.valueOf('#'), mappedHandlerName});
			handlerRef.set(newHandlerValue);
		}

		return mappedHandlerName != null;
	}

	@Override
	public void handleReferencedClass(XmlElement element, ClassNode classNode) {
	}

	@Override
	public void handleConstantAttribute(XmlElement element, ClassNode ownerClass, String attributeName, AtomicReference<String> constantRef) {
		String constantName = constantRef.get();
		String mappedConstantName = this.handler.getMappedFieldName(ownerClass.getBinaryName(), constantName);
		if (!mappedConstantName.equals(constantName)) {
			constantRef.set(mappedConstantName);
		}
	}

	@Override
	public void handleResourceReference(XmlElement element, String attributeName, String resourcePath, AtomicReference<String> resourcePathRef) {
		FxmlHandler.getResourceNameUnused(this.handler);
		String remappedResourceDirectory = FileUtils.getParentDirectory(this.handler.resourcePathTransformer.remapPathWithCache(FxmlHandler.getResourceName(this.handler)));
		if (remappedResourceDirectory == null) {
			remappedResourceDirectory = "";
		}

		String baseDirectory = FileUtils.getParentDirectory(FxmlHandler.getResourceNameForResolve(this.handler));
		if (baseDirectory == null) {
			baseDirectory = "";
		}

		String relativePath = resourcePath.substring(1);
		String resolvedPath = FileUtils.resolveRelativeResourcePath(relativePath, baseDirectory);
		int colonIndex = resolvedPath.indexOf(58);
		if (colonIndex > -1 && colonIndex < resolvedPath.length() - 1) {
			resolvedPath = resolvedPath.substring(colonIndex + 1);
		}

		String remappedPath = this.handler.resourcePathTransformer.remapPathWithCache(resolvedPath);
		String newRelativePath = remappedPath;
		if (relativePath.charAt(0) != '/') {
			newRelativePath = FileUtils.getRelativePath(remappedResourceDirectory, remappedPath);
		}

		if (!newRelativePath.equals(relativePath)) {
			resourcePathRef.set('@' + newRelativePath);
		}
	}

	@Override
	public void handleFqnClassTag(XmlElement element, ClassNode classNode) {
		element.getName();
		element.setName(classNode.getBinaryName());
	}

	@Override
	public void handleSimpleClassTag(XmlElement element, ClassNode classNode) {
		element.getName();
		element.setName(classNode.getSimpleNameInternal());
	}

	@Override
	public void logWarning(String level, String message) {
		this.handler.logger.warn(level, message);
	}

	@Override
	public void handlePropertyAttribute(XmlElement element, ClassNode elementClass, String attributeName) {
		String tagName = element.getName();
		AtomicReference<String> attributeRef = new AtomicReference<>(attributeName);
		this.handler.updateBeanPropertyName(elementClass.getBinaryName(), GlobalUtils.getMappedOrDefault(elementClass.getInternalName(), FxmlHandler.getFullRemappingMap(this.handler)), tagName, attributeName, attributeRef);
		if (!attributeRef.get().equals(attributeName)) {
			element.renameAttribute(attributeName, attributeRef.get());
		}
	}

	@Override
	public void handleControllerAttribute(XmlElement element, ClassNode controllerClass, String attributeName, AtomicReference<String> controllerRef) {
		controllerRef.set(controllerClass.getBinaryName());
	}

	@Override
	public boolean handleFxIdInElementClass(XmlElement element, AbstractClassNode elementClass, String attributeName, String idValue, AtomicReference<String> idRef) {
		element.getName();
		AtomicReference<String> errorMessageRef = new AtomicReference<>();
		String mappedIdName = this.handler.resolveFieldName(elementClass, idValue, "javafx/fxml/FXML", 0, errorMessageRef);
		if (mappedIdName != null && !mappedIdName.equals(idValue)) {
			idRef.set(mappedIdName);
		}

		return mappedIdName != null;
	}
}
