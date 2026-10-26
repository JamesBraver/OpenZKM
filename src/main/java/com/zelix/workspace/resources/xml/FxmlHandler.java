package com.zelix.workspace.resources.xml;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.signature.FieldSignature;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.logging.AbstractLogger;
import com.zelix.repository.ClassProvider;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.PathRemapper;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.readOnly.ReadOnlyMap;
import com.zelix.utils.collections.readOnly.ReadOnlyRegistryView;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.resources.xml.pattern.JavaFXMethodMatcher;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FxmlHandler extends BeanPropertyXmlHandler {
	private Set<String> wildcardImportPackages = GlobalUtils.newHashSet();
	private Set<String> importedClassNames = GlobalUtils.newHashSet();
	private Map<XmlElement, AbstractClassNode> classNodesByElement = GlobalUtils.newHashMap();
	private Map<String, AbstractClassNode> classNodesByName = GlobalUtils.newHashMap();
	private Map<String, XmlElement> elementsById = GlobalUtils.newHashMap();
	private String fxNamespacePrefix = "fx:";
	private ReadOnlyMap<String, String> fullRemappingMap;
	private final JavaFXMethodMatcher javaFXMethodMatcher;
	private ClassNode controllerClassNode;
	private FxmlElementCallback renameCallback;

	public FxmlHandler(String fullResourcePath, AbstractWorkspace classHierarchy, ClassProvider classProvider, AbstractLogger logger) {
		super(fullResourcePath, classHierarchy, classProvider, logger);
		this.javaFXMethodMatcher = new JavaFXMethodMatcher(classHierarchy, logger);
	}

	public FxmlHandler(
		String fullResourcePath,
		ReadOnlyMap<String, String> classNameMapping,
		ReadOnlyMap<String, String> fullRemappingMap,
		ReadOnlyRegistryView<AbstractClassNode, FieldSignature, FieldSignature> fieldMappingRegistry,
		ReadOnlyRegistryView<AbstractClassNode, MethodSignature, MethodSignature> methodMappingRegistry,
		PathRemapper pathRemapper,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		AbstractLogger logger
	) {
		super(fullResourcePath, classNameMapping, fieldMappingRegistry, methodMappingRegistry, pathRemapper, workspace, classProvider, logger);
		this.fullRemappingMap = fullRemappingMap;
		this.javaFXMethodMatcher = new JavaFXMethodMatcher(workspace, logger);
	}

	@Override
	public final void handleProcessingInstruction(
		String processingInstruction, Map<ClassNode, String> classReferences, Map<FieldNode, String> fieldReferences, Map<MethodNode, String> methodReferences, DualKeyMap<MethodNode, String, Object> methodAccessorReferences
	) {
		if (processingInstruction.startsWith("import")) {
			this.processImport(processingInstruction, false);
		}
	}

	private void renameMethodReference(XmlElement element, String attributeName, String attributeValue, AtomicReference<String> valueRef) {
		String handlerName = attributeValue.substring(1);
		boolean resolved = false;
		if (this.controllerClassNode != null) {
			resolved = this.renameCallback.handleHandlerInController(element, this.controllerClassNode, attributeName, handlerName, valueRef);
		}

		if (!resolved) {
			this.renameCallback.handleHandlerByAnnotation(element, attributeName, handlerName, valueRef);
		}
	}

	private String getControllerAttribute(XmlElement element, AtomicReference<String> controllerAttributeKey) {
		String controllerAttributeName = this.fxNamespacePrefix + "controller";
		controllerAttributeKey.set(controllerAttributeName);
		Enumeration<String> attributeNames = element.getAttributeNames();

		while (attributeNames.hasMoreElements()) {
			String attributeName = attributeNames.nextElement();
			if (attributeName.equals(controllerAttributeName)) {
				return element.getAttributeValueRef(attributeName).get();
			}
		}

		return null;
	}

	private boolean findJavaFXNamespacePrefix(XmlElement rootElement) {
		boolean prefixFound = false;
		Enumeration<String> attributeNames = rootElement.getAttributeNames();

		while (attributeNames.hasMoreElements()) {
			String attributeName = attributeNames.nextElement();
			if (attributeName.startsWith("xmlns")) {
				int colonIndex = attributeName.indexOf(58);
				if (colonIndex > 0 && colonIndex < attributeName.length() - 1 && rootElement.getAttributeValueRef(attributeName).get().equalsIgnoreCase("http://javafx.com/fxml")) {
					this.fxNamespacePrefix = attributeName.substring(colonIndex + 1) + ':';
					prefixFound = true;
				}
			}
		}

		return prefixFound;
	}

	private AbstractClassNode findEnclosingClassNode() {
		int stackLevel = 1;

		XmlElement tagElement;
		do {
			tagElement = this.getTagFromStack(stackLevel);
			if (tagElement != null) {
				AbstractClassNode classNode = this.classNodesByElement.get(tagElement);
				if (classNode != null) {
					return classNode;
				}
			}

			stackLevel++;
		} while (tagElement != null);

		return null;
	}

	public String processImport(String importText, boolean remapNames) {
		StringBuilder builder = new StringBuilder();
		builder.append("import");
		builder.append(' ');
		String importTarget = importText.substring(6).trim();
		if (importTarget.indexOf(42) > -1) {
			int lastDotIndex = importTarget.lastIndexOf(46);
			this.wildcardImportPackages.add(importTarget.substring(0, lastDotIndex + 1).replace('.', '/'));
			if (remapNames) {
				String remappedPackageName = this.remapPackageName(importTarget.substring(0, lastDotIndex));
				importTarget = remappedPackageName + ".*";
			}
		} else {
			this.importedClassNames.add(importTarget.replace('.', '/'));
			if (remapNames) {
				importTarget = this.mapClassName(importTarget);
			}
		}

		builder.append(importTarget);
		return builder.toString();
	}

	@Override
	public void remapAttributeValues(XmlElement element, List<Object> outputFragments) {
		if (this.renameCallback == null) {
			this.renameCallback = new FxmlRenameCallback(this);
		}

		this.processTag(element);
		outputFragments.add(element);
	}

	@Override
	public void collectAttributeReferences(
		XmlElement element, Map<ClassNode, String> classReferences, Map<FieldNode, String> fieldReferences, Map<MethodNode, String> methodReferences, DualKeyMap<MethodNode, String, Object> methodAccessorReferences
	) {
		if (this.renameCallback == null) {
			this.renameCallback = new FxmlReferenceCollector(this, classReferences, fieldReferences, methodReferences, methodAccessorReferences);
		}

		this.processTag(element);
	}

	private void processQualifiedAttribute(XmlElement element, String attributeName) {
		int firstDotIndex = attributeName.indexOf(46);
		int lastDotIndex = attributeName.lastIndexOf(46);
		if (firstDotIndex > 0 && lastDotIndex < attributeName.length() - 1) {
			String className = attributeName.substring(0, lastDotIndex);
			AbstractClassNode classNode;
			if (className.indexOf(46) == -1) {
				classNode = this.resolveClassNode(className);
			} else {
				classNode = this.loadClassNode(className);
			}

			if (classNode != null && classNode.isWorkspaceNode()) {
				String memberName = attributeName.substring(lastDotIndex + 1);
				this.renameCallback.handleQualifiedAttribute(element, (ClassNode)classNode, attributeName, className, memberName);
			}
		}
	}

	private void processTag(XmlElement element) {
		String tagName = element.getName();
		String parentTagName = this.getTagNameFromStack(1);
		if (parentTagName == null) {
			this.findJavaFXNamespacePrefix(element);
			AtomicReference<String> controllerAttributeKeyRef = new AtomicReference<>();
			String controllerClassName = this.getControllerAttribute(element, controllerAttributeKeyRef);
			if (controllerClassName != null) {
				this.controllerClassNode = this.getWorkspaceClassNode(controllerClassName);
				if (this.controllerClassNode != null) {
					AtomicReference<String> controllerAttributeRef = element.getAttributeValueRef(controllerAttributeKeyRef.get());
					this.renameCallback.handleControllerAttribute(element, this.controllerClassNode, controllerAttributeKeyRef.get(), controllerAttributeRef);
				}
			}
		}

		AbstractClassNode classNode = null;
		int firstDotIndex = tagName.indexOf(46);
		int lastDotIndex = tagName.lastIndexOf(46);
		if (tagName.startsWith(this.fxNamespacePrefix)) {
			String localTagName = tagName.substring(tagName.indexOf(58) + 1);
			if (localTagName.equals("root")) {
				AtomicReference<String> typeValueRef = element.getAttributeValueRef("type");
				if (typeValueRef != null) {
					String typeValue = typeValueRef.get();
					if (typeValue != null && typeValue.length() > 0) {
						if (typeValue.indexOf(46) > 0) {
							classNode = this.loadClassNode(typeValue);
						} else {
							classNode = this.resolveClassNode(typeValue);
						}
					}
				}
			} else if (localTagName.equals("reference") || localTagName.equals("copy")) {
				String sourceValue = element.getAttributeValueRef("source").get();
				if (sourceValue != null && sourceValue.length() > 0) {
					XmlElement sourceElement = this.elementsById.get(sourceValue);
					if (sourceElement != null) {
						classNode = this.classNodesByElement.get(sourceElement);
					}
				}
			} else if (localTagName.equals("script")) {
				this.renameCallback.logWarning("WARNING:", "FXML file '" + super.fullResourcePath + "' contains the '" + tagName + "' tag. Its contents will not be changed.");
			} else if (localTagName.equals("include")) {
			}
		} else if (firstDotIndex > 0 && lastDotIndex < tagName.length() - 1) {
			if (Character.isUpperCase(tagName.charAt(0)) && firstDotIndex == lastDotIndex && Character.isLowerCase(tagName.charAt(firstDotIndex + 1))) {
				String className = tagName.substring(0, firstDotIndex);
				classNode = this.resolveClassNode(className);
				if (classNode != null && classNode.isWorkspaceNode()) {
					String propertyName = tagName.substring(firstDotIndex + 1);
					this.renameCallback.handleQualifiedTag(element, (ClassNode)classNode, className, propertyName);
				}
			} else {
				classNode = this.loadClassNode(tagName);
				if (classNode != null && classNode.isWorkspaceNode()) {
					this.renameCallback.handleFqnClassTag(element, (ClassNode)classNode);
				}
			}
		} else if (Character.isUpperCase(tagName.charAt(0))) {
			classNode = this.resolveClassNode(tagName);
			if (classNode != null && classNode.isWorkspaceNode()) {
				this.renameCallback.handleSimpleClassTag(element, (ClassNode)classNode);
			}
		} else if (parentTagName != null) {
			AbstractClassNode enclosingClassNode = this.findEnclosingClassNode();
			if (enclosingClassNode != null && enclosingClassNode.isWorkspaceNode()) {
				this.renameCallback.handlePropertyTag(element, (ClassNode)enclosingClassNode);
			}
		}

		if (classNode != null) {
			this.classNodesByElement.put(element, classNode);
			if (classNode.isWorkspaceNode()) {
				this.renameCallback.handleReferencedClass(element, (ClassNode)classNode);
			}

			Enumeration<String> attributeNames = element.getAttributeNames();

			while (attributeNames.hasMoreElements()) {
				String attributeName = attributeNames.nextElement();
				AtomicReference<String> attributeRef = element.getAttributeValueRef(attributeName);
				String attributeValue = attributeRef.get();
				if (attributeName.startsWith(this.fxNamespacePrefix)) {
					String localAttributeName = attributeName.substring(attributeName.indexOf(58) + 1);
					if (localAttributeName.equals("id")) {
						this.elementsById.put(localAttributeName, element);
						if (classNode != null) {
							this.renameFieldReference(element, classNode, attributeName, attributeValue, attributeRef);
						}
					} else if (localAttributeName.equals("constant")) {
						if (classNode.isWorkspaceNode()) {
							this.renameCallback.handleConstantAttribute(element, (ClassNode)classNode, attributeName, attributeRef);
						}
					} else if (localAttributeName.equals("factory") && classNode.isWorkspaceNode()) {
						this.renameCallback.handleFactoryAttribute(element, (ClassNode)classNode, attributeName, attributeRef);
					}
				} else if (attributeName.indexOf(46) > -1) {
					this.processQualifiedAttribute(element, attributeName);
				} else {
					if (classNode.isWorkspaceNode()) {
						this.renameCallback.handlePropertyAttribute(element, (ClassNode)classNode, attributeName);
					}

					if (attributeValue.length() > 1) {
						if (attributeValue.charAt(0) == '#') {
							this.renameMethodReference(element, attributeName, attributeValue, attributeRef);
						} else if (attributeValue.charAt(0) == '@') {
							this.renameCallback.handleResourceReference(element, attributeName, attributeValue, attributeRef);
						} else if (attributeValue.charAt(0) == '$') {
						}
					}
				}
			}
		}
	}

	@Override
	public void handleCharacters(String text, List<Object> outputFragments) {
		if (text.startsWith("import")) {
			text = this.processImport(text, true);
		}

		outputFragments.add(text);
	}

	private AbstractClassNode resolveClassNode(String xmlClassName) {
		AbstractClassNode classNode = this.classNodesByName.get(xmlClassName);
		if (classNode != null) {
			return classNode;
		} else {
			classNode = this.loadClassNode(xmlClassName);
			if (classNode != null) {
				this.classNodesByName.put(xmlClassName, classNode);
				return classNode;
			} else {
				for (String importedClassName : this.importedClassNames) {
					if (importedClassName.endsWith(xmlClassName) && importedClassName.charAt(importedClassName.length() - xmlClassName.length() - 1) == '/') {
						classNode = this.loadClassNode(importedClassName);
						if (classNode != null) {
							this.classNodesByName.put(xmlClassName, classNode);
							return classNode;
						}
					}
				}

				StringBuilder builder = new StringBuilder();

				for (String wildcardImportPackage : this.wildcardImportPackages) {
					builder.setLength(0);
					builder.append(wildcardImportPackage);
					builder.append(xmlClassName);
					classNode = this.loadClassNode(builder.toString());
					if (classNode != null) {
						this.classNodesByName.put(xmlClassName, classNode);
						return classNode;
					}
				}

				return null;
			}
		}
	}

	private void renameFieldReference(XmlElement element, AbstractClassNode elementClassNode, String attributeName, String attributeValue, AtomicReference<String> valueRef) {
		boolean resolved = false;
		if (this.controllerClassNode != null) {
			resolved = this.renameCallback.handleFxIdInController(element, this.controllerClassNode, elementClassNode, attributeName, attributeValue, valueRef);
		}

		if (!resolved) {
			this.renameCallback.handleFxIdInElementClass(element, elementClassNode, attributeName, attributeValue, valueRef);
		}
	}

	public static ReadOnlyMap<String, String> getFullRemappingMap(FxmlHandler handler) {
		return handler.fullRemappingMap;
	}

	public static String getResourceName(FxmlHandler handler) {
		return handler.resourceName;
	}

	public static String getResourceNameForResolve(FxmlHandler handler) {
		return handler.resourceName;
	}

	public static String getResourceNameUnused(FxmlHandler handler) {
		return handler.resourceName;
	}

	public static String getFullResourcePath(FxmlHandler handler) {
		return handler.fullResourcePath;
	}

	public static JavaFXMethodMatcher getMethodMatcher(FxmlHandler handler) {
		return handler.javaFXMethodMatcher;
	}
}
