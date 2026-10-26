package com.zelix.workspace.resources.xml;

import com.zelix.enums.PlatformType;
import com.zelix.exceptions.FileException;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.signature.FieldSignature;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.klassmaster.config.Options;
import com.zelix.logging.AbstractLogger;
import com.zelix.repository.ClassProvider;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.PathRemapper;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.readOnly.ReadOnlyMap;
import com.zelix.utils.collections.readOnly.ReadOnlyRegistryView;
import com.zelix.utils.dataset.AtomicBool;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.ClassHierarchyNode;
import com.zelix.workspace.resources.ResourceIndex;
import com.zelix.workspace.resources.xml.pattern.MethodPattern;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;

public abstract class AbstractFrameworkXmlHandler extends AbstractXmlTagHandler {
	private ReadOnlyMap<String, String> classNameMapping;
	private ReadOnlyRegistryView<AbstractClassNode, FieldSignature, FieldSignature> fieldMappingRegistry;
	private ReadOnlyRegistryView<AbstractClassNode, MethodSignature, MethodSignature> methodMappingRegistry;
	public PathRemapper resourcePathTransformer;
	public final AbstractWorkspace classHierarchy;
	public final ClassProvider classProvider;
	public final AbstractLogger logger;
	private DualKeyMap<FieldSignature, AbstractClassNode, FieldSignature> fieldLookupCache;
	private DualKeyMap<String, AbstractClassNode, MethodSignature> methodLookupCache;

	public AbstractFrameworkXmlHandler(
		String fullResourcePath,
		ReadOnlyMap<String, String> classNameMapping,
		ReadOnlyRegistryView<AbstractClassNode, FieldSignature, FieldSignature> fieldMappingRegistry,
		ReadOnlyRegistryView<AbstractClassNode, MethodSignature, MethodSignature> methodMappingRegistry,
		PathRemapper resourcePathTransformer,
		AbstractWorkspace classHierarchy,
		ClassProvider classProvider,
		AbstractLogger logger
	) {
		super(fullResourcePath);
		this.classNameMapping = classNameMapping;
		this.fieldMappingRegistry = fieldMappingRegistry;
		this.methodMappingRegistry = methodMappingRegistry;
		this.resourcePathTransformer = resourcePathTransformer;
		this.classHierarchy = classHierarchy;
		this.classProvider = classProvider;
		this.logger = logger;
	}

	public AbstractFrameworkXmlHandler(String fullResourcePath, AbstractWorkspace classHierarchy, ClassProvider classProvider, AbstractLogger logger) {
		super(fullResourcePath);
		this.classHierarchy = classHierarchy;
		this.classProvider = classProvider;
		this.logger = logger;
	}

	public static XmlHandler createHandler(String resourcePath, String xmlContent, ResourceIndex fileAdaptor, AbstractWorkspace workspace, ClassProvider classProvider, AbstractLogger logger) {
		PlatformType platformType = detectPlatformType(resourcePath, xmlContent);
		switch (platformType) {
			case EJB_JAR:
				return new EjbJarXmlHandler(resourcePath, workspace, classProvider, logger);
			case APPLICATION:
				return new ApplicationXmlHandler(resourcePath, workspace, classProvider, logger);
			case CLIENT:
				return new ApplicationClientXmlHandler(resourcePath, workspace, classProvider, logger);
			case WEB:
				return new WebXmlHandler(resourcePath, workspace, classProvider, logger);
			case SPRING:
				return new SpringBeanXmlHandler(resourcePath, fileAdaptor, workspace, classProvider, logger);
			case HIBERNATE:
				return new HibernateMappingXmlHandler(resourcePath, workspace, classProvider, logger);
			case ANDROID:
				return new AndroidManifestXmlHandler(resourcePath, workspace, classProvider, logger);
			case IBATIS:
				return new IBatisSqlMapXmlHandler(resourcePath, workspace, classProvider, logger);
			case TAGLIB:
				return new TaglibXmlHandler(resourcePath, workspace, classProvider, logger);
			case FXML:
				return new FxmlHandler(resourcePath, workspace, classProvider, logger);
			case E4XMI:
				return new E4XmiHandler(resourcePath, workspace, classProvider, logger);
			case BLUEPRINT:
				return new BlueprintXmlHandler(resourcePath, workspace, classProvider, logger);
			case GENERIC:
				return new GenericXmlHandler(resourcePath, workspace, classProvider, logger);
			default:
				return new GenericXmlHandler(resourcePath, workspace, classProvider, logger);
		}
	}

	public static XmlHandler createHandler(
		String resourcePath,
		String xmlContent,
		ReadOnlyMap<String, String> classNameMapping,
		ReadOnlyMap<String, String> classNameToInternalNameMap,
		ReadOnlyRegistryView<AbstractClassNode, FieldSignature, FieldSignature> fieldMappingRegistry,
		ReadOnlyRegistryView<AbstractClassNode, MethodSignature, MethodSignature> methodMappingRegistry,
		PathRemapper pathRemapper,
		ResourceIndex fileAdaptor,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		AbstractLogger logger
	) {
		PlatformType platformType = detectPlatformType(resourcePath, xmlContent);
		switch (platformType) {
			case EJB_JAR:
				return new EjbJarXmlHandler(resourcePath, classNameMapping, fieldMappingRegistry, methodMappingRegistry, pathRemapper, workspace, classProvider, logger);
			case APPLICATION:
				return new ApplicationXmlHandler(resourcePath, classNameMapping, fieldMappingRegistry, methodMappingRegistry, pathRemapper, workspace, classProvider, logger);
			case CLIENT:
				return new ApplicationClientXmlHandler(resourcePath, classNameMapping, fieldMappingRegistry, methodMappingRegistry, pathRemapper, workspace, classProvider, logger);
			case WEB:
				return new WebXmlHandler(resourcePath, classNameMapping, fieldMappingRegistry, methodMappingRegistry, pathRemapper, workspace, classProvider, logger);
			case SPRING:
				return new SpringBeanXmlHandler(resourcePath, classNameMapping, fieldMappingRegistry, methodMappingRegistry, pathRemapper, fileAdaptor, workspace, classProvider, logger);
			case HIBERNATE:
				return new HibernateMappingXmlHandler(resourcePath, classNameMapping, fieldMappingRegistry, methodMappingRegistry, pathRemapper, workspace, classProvider, logger);
			case ANDROID:
				return new AndroidManifestXmlHandler(resourcePath, classNameMapping, fieldMappingRegistry, methodMappingRegistry, pathRemapper, workspace, classProvider, logger);
			case IBATIS:
				return new IBatisSqlMapXmlHandler(resourcePath, classNameMapping, fieldMappingRegistry, methodMappingRegistry, pathRemapper, workspace, classProvider, logger);
			case TAGLIB:
				return new TaglibXmlHandler(resourcePath, classNameMapping, fieldMappingRegistry, methodMappingRegistry, pathRemapper, workspace, classProvider, logger);
			case FXML:
				return new FxmlHandler(resourcePath, classNameMapping, classNameToInternalNameMap, fieldMappingRegistry, methodMappingRegistry, pathRemapper, workspace, classProvider, logger);
			case E4XMI:
				return new E4XmiHandler(resourcePath, classNameMapping, fieldMappingRegistry, methodMappingRegistry, pathRemapper, workspace, classProvider, logger);
			case BLUEPRINT:
				return new BlueprintXmlHandler(resourcePath, classNameMapping, fieldMappingRegistry, methodMappingRegistry, pathRemapper, workspace, classProvider, logger);
			case GENERIC:
				return new GenericXmlHandler(resourcePath, classNameMapping, fieldMappingRegistry, methodMappingRegistry, pathRemapper, workspace, classProvider, logger);
			default:
				return new GenericXmlHandler(resourcePath, classNameMapping, fieldMappingRegistry, methodMappingRegistry, pathRemapper, workspace, classProvider, logger);
		}
	}

	public static String formatAccessorName(String accessorPrefix, String propertyName) {
		StringBuilder nameBuilder = new StringBuilder(accessorPrefix.length() + propertyName.length());
		nameBuilder.append(accessorPrefix);
		char propertyFirstChar = propertyName.charAt(0);
		if (Character.isLowerCase(propertyFirstChar) && (propertyName.length() == 1 || !Character.isUpperCase(propertyName.charAt(1)))) {
			nameBuilder.append(Character.toUpperCase(propertyFirstChar));
			nameBuilder.append(propertyName.substring(1));
		} else {
			nameBuilder.append(propertyName);
		}

		return nameBuilder.toString();
	}

	public static PlatformType detectPlatformType(String resourcePath, String xmlContent) {
		String lowerResourcePath = resourcePath.toLowerCase();
		String docType = AbstractXmlTagHandler.extractDocType(xmlContent);
		String schemaLocation = AbstractXmlTagHandler.extractSchemaLocation(xmlContent);
		if (resourcePath.equalsIgnoreCase("META-INF/ejb-jar.xml")) {
			return PlatformType.EJB_JAR;
		} else if (resourcePath.equalsIgnoreCase("META-INF/application.xml")) {
			return PlatformType.APPLICATION;
		} else if (resourcePath.equalsIgnoreCase("META-INF/application-client.xml")) {
			return PlatformType.CLIENT;
		} else if (resourcePath.equalsIgnoreCase("WEB-INF/web.xml")) {
			return PlatformType.WEB;
		} else if (docType.indexOf("spring-beans") <= -1 && schemaLocation.indexOf("spring-beans") <= -1) {
			if (!lowerResourcePath.endsWith(".hbm.xml") || docType.indexOf("hibernate-mapping") <= -1 && schemaLocation.indexOf("hibernate-mapping") <= -1) {
				if (lowerResourcePath.endsWith("androidmanifest.xml") && xmlContent.indexOf("xmlns:android") > -1) {
					return PlatformType.ANDROID;
				} else if (xmlContent.indexOf("ibatis") > -1 && docType.indexOf("sql-map") > -1) {
					return PlatformType.IBATIS;
				} else if (lowerResourcePath.endsWith(".tld")) {
					return PlatformType.TAGLIB;
				} else if (lowerResourcePath.endsWith(".fxml")) {
					return PlatformType.FXML;
				} else if (lowerResourcePath.endsWith(".e4xmi")) {
					return PlatformType.E4XMI;
				} else {
					return xmlContent.toLowerCase().indexOf("http://www.osgi.org/xmlns/blueprint") > -1 ? PlatformType.BLUEPRINT : PlatformType.GENERIC;
				}
			} else {
				return PlatformType.HIBERNATE;
			}
		} else {
			return PlatformType.SPRING;
		}
	}

	public final boolean registerMatchingMethods(
		ClassNode classNode,
		String methodName,
		String accessorPrefix,
		Object referenceSource,
		MethodPattern methodPattern,
		Map<MethodNode, String> methodReferences,
		DualKeyMap<MethodNode, String, Object> resourceToClassReferences,
		String referenceDescription
	) {
		return this.registerMatchingMethods(classNode, methodName, accessorPrefix, referenceSource, methodPattern, 0, methodReferences, resourceToClassReferences, referenceDescription);
	}

	@Override
	public void handleProcessingInstruction(
		String processingInstruction, Map<ClassNode, String> classReferences, Map<FieldNode, String> fieldReferences, Map<MethodNode, String> methodReferences, DualKeyMap<MethodNode, String, Object> methodAccessorReferences
	) {
	}

	public final void registerFieldsInOuterClasses(ClassNode classNode, String fieldName, Map<FieldNode, String> fieldReferences, String referenceDescription) {
		for (ClassHierarchyNode classHierarchyNode = ClassHierarchyNode.getNodeByInternalName(classNode.getInternalName()); classHierarchyNode != null; classHierarchyNode = classHierarchyNode.getOuterClass()) {
			FieldNode[] fieldNodes;
			if ((fieldNodes = this.classHierarchy.getFieldsByClassAndName(classHierarchyNode.getInternalName(), fieldName)) != null) {
				for (int i = 0; i < fieldNodes.length; i++) {
					fieldReferences.put(fieldNodes[i], referenceDescription);
				}
				break;
			}
		}
	}

	public final void processPotentialClassName(String candidateName, Map<ClassNode, String> classReferences, String referenceDescription, String attributeOrTagName, boolean useEnclosingTag) {
		if (candidateName.length() > Options.minDottedNameLength
			&& candidateName.indexOf(47) == -1
			&& candidateName.indexOf(91) == -1
			&& candidateName.indexOf(59) == -1
			&& candidateName.indexOf(60) == -1
			&& candidateName.indexOf(62) == -1
			&& candidateName.indexOf(47) == -1
			&& candidateName.lastIndexOf("*") == -1
			&& candidateName.indexOf(".") > 0
			&& candidateName.lastIndexOf(".") < candidateName.length() - 1) {
			if (!Options.dontSplitColonNames && candidateName.indexOf(58) > 0) {
				for (String namePart : candidateName.split(":")) {
					int dotIndex = namePart.indexOf(46);
					if (dotIndex > 0 && dotIndex < namePart.length() - 1 && namePart.length() > Options.minDottedNameLength) {
						this.findAndRegisterClassName(namePart, classReferences, referenceDescription);
					}
				}
			} else if (this.isReferenceAttribute(attributeOrTagName, useEnclosingTag, "class")) {
				this.findAndRegisterClassName(candidateName, classReferences, referenceDescription);
			}
		}
	}

	public final boolean registerFieldInHierarchy(ClassNode classNode, AbstractClassNode fieldOwner, String fieldName, Map<FieldNode, String> fieldReferences, String referenceDescription) {
		FieldSignature fieldSignature = new FieldSignature(fieldName, GlobalUtils.toString(new String[]{"L", fieldOwner.getInternalName(), ";"}));

		for (ClassNode currentClassNode = classNode; currentClassNode != null; currentClassNode = ClassHierarchyNode.getWorkspaceClassNode(currentClassNode.getSuperclassName())) {
			FieldNode fieldNode = currentClassNode.findFieldNode(fieldSignature);
			if (fieldNode != null) {
				fieldReferences.put(fieldNode, referenceDescription);
				return true;
			}
		}

		return false;
	}

	public final String findAndRegisterClassName(String className, Map<ClassNode, String> classReferences, String referenceDescription) {
		return this.registerClassName(className, classReferences, null, referenceDescription);
	}

	@Override
	public final void recordStartTagReferences(
		XmlElement element, Map<ClassNode, String> classReferences, Map<FieldNode, String> fieldReferences, Map<MethodNode, String> methodReferences, DualKeyMap<MethodNode, String, Object> methodAccessorReferences
	) {
		super.elementStack.push(element);
		this.collectAttributeReferences(element, classReferences, fieldReferences, methodReferences, methodAccessorReferences);
	}

	public final List<MethodNode> findMatchingMethods(String methodName, MethodPattern methodPattern, String annotationType, int accessFlag) {
		ArrayList<MethodNode> matchingMethods = new ArrayList<>();
		Enumeration<ClassNode> classEnumeration = this.classHierarchy.getClassesByMethodName(methodName);
		if (classEnumeration != null) {
			while (classEnumeration.hasMoreElements()) {
				for (AbstractMethodNode methodNode : classEnumeration.nextElement().findMethodsByName(methodName)) {
					if (methodNode.hasAccessFlag(accessFlag) && methodPattern.matches(methodNode.getDescriptor()) && (annotationType == null || methodNode.getAnnotationTypeNameSet().contains(annotationType))) {
						matchingMethods.add((MethodNode)methodNode);
					}
				}
			}
		}

		return matchingMethods;
	}

	public final String mapTypeName(String typeName) {
		int arrayBracketIndex = typeName.indexOf("[]");
		String arraySuffix;
		String elementTypeName;
		if (arrayBracketIndex == -1) {
			arraySuffix = "";
			elementTypeName = typeName;
		} else {
			arraySuffix = typeName.substring(arrayBracketIndex);
			elementTypeName = typeName.substring(0, arrayBracketIndex);
		}

		return !elementTypeName.equals("byte")
				&& !elementTypeName.equals("short")
				&& !elementTypeName.equals("char")
				&& !elementTypeName.equals("int")
				&& !elementTypeName.equals("long")
				&& !elementTypeName.equals("float")
				&& !elementTypeName.equals("double")
				&& !elementTypeName.equals("boolean")
			? this.mapClassName(elementTypeName) + arraySuffix
			: typeName;
	}

	public final String remapRelativePath(String basePath, String relativePath, boolean allowAbsolutePath) {
		return this.resourcePathTransformer.remapRelativePath(basePath, relativePath, allowAbsolutePath);
	}

	public void remapAttributeValues(XmlElement element, List<Object> outputFragments) {
		Enumeration<String> attributeNameEnumeration = element.getAttributeNames();

		while (attributeNameEnumeration.hasMoreElements()) {
			String attributeName = attributeNameEnumeration.nextElement();
			AtomicReference<String> attributeValueRef = element.getAttributeValueRef(attributeName);
			attributeValueRef.set(this.remapAttributeValue(attributeValueRef.get(), attributeName, false));
		}

		outputFragments.add(element);
	}

	public void collectAttributeReferences(
		XmlElement element, Map<ClassNode, String> classReferences, Map<FieldNode, String> fieldReferences, Map<MethodNode, String> methodReferences, DualKeyMap<MethodNode, String, Object> methodAccessorReferences
	) {
		String referencePrefix = "'" + super.fullResourcePath + "' '" + element.getName() + "' tag '";
		Enumeration<String> attributeNameEnumeration = element.getAttributeNames();

		while (attributeNameEnumeration.hasMoreElements()) {
			String attributeName = attributeNameEnumeration.nextElement();
			String attributeValue = element.getAttributeValueRef(attributeName).get();
			String referenceDescription = referencePrefix + attributeName + "' attribute";
			this.processPotentialClassName(attributeValue, classReferences, referenceDescription, attributeName, false);
		}
	}

	private boolean isReferenceAttribute(String attributeOrTagName, boolean useEnclosingTag, String keyword) {
		if (attributeOrTagName == null || attributeOrTagName.length() == 0) {
			return false;
		} else if (!attributeOrTagName.endsWith("name")) {
			return attributeOrTagName.equals("ejb-link") ? false : attributeOrTagName.indexOf("jndi") == -1;
		} else if (attributeOrTagName.toLowerCase().indexOf(keyword) > -1) {
			return true;
		} else {
			String tagName;
			if (useEnclosingTag) {
				tagName = this.getTagNameFromStack(1);
			} else {
				tagName = this.getCurrentTagName();
			}

			return tagName != null && tagName.toLowerCase().indexOf(keyword) > -1;
		}
	}

	public final String getMappedMethodName(String className, MethodSignature methodSignature, AtomicBool mappingFound) {
		if (this.methodMappingRegistry != null) {
			for (ClassNode classNode = ClassHierarchyNode.getWorkspaceClassNode(GlobalUtils.binaryToInternalName(className));
				classNode != null;
				classNode = ClassHierarchyNode.getWorkspaceClassNode(classNode.getSuperclassName())
			) {
				classNode.getInternalName();
				MethodSignature renamedSignature = this.methodMappingRegistry.get(classNode, methodSignature);
				if (renamedSignature != null) {
					mappingFound.set(true);
					return renamedSignature.getName();
				}
			}
		}

		mappingFound.set(false);
		return methodSignature.getName();
	}

	public final boolean registerMatchingFields(AbstractClassNode fieldOwner, String fieldName, String annotationType, int accessFlag, Map<FieldNode, String> fieldReferences, String referenceDescription) {
		List<FieldNode> matchingFields = this.findMatchingFields(fieldName, fieldOwner, annotationType, accessFlag);

		for (FieldNode fieldNode : matchingFields) {
			fieldReferences.put(fieldNode, referenceDescription);
		}

		return matchingFields.size() > 0;
	}

	public final void registerFieldsInOuterClasses(String binaryClassName, String fieldName, Map<FieldNode, String> fieldReferences, String referenceDescription) {
		for (ClassHierarchyNode classHierarchyNode = ClassHierarchyNode.getNodeByInternalName(GlobalUtils.binaryToInternalName(binaryClassName));
			classHierarchyNode != null;
			classHierarchyNode = classHierarchyNode.getOuterClass()
		) {
			FieldNode[] fieldNodes;
			if ((fieldNodes = this.classHierarchy.getFieldsByClassAndName(classHierarchyNode.getInternalName(), fieldName)) != null) {
				for (int i = 0; i < fieldNodes.length; i++) {
					fieldReferences.put(fieldNodes[i], referenceDescription);
				}
				break;
			}
		}
	}

	public final String findMappedMethodName(String className, String methodName, MethodPattern methodPattern, int accessFlag) {
		if (this.methodMappingRegistry != null) {
			for (ClassNode classNode = ClassHierarchyNode.getWorkspaceClassNode(GlobalUtils.binaryToInternalName(className));
				classNode != null;
				classNode = ClassHierarchyNode.getWorkspaceClassNode(classNode.getSuperclassName())
			) {
				classNode.getInternalName();
				ReadOnlyMap<MethodSignature, MethodSignature> methodSignatureMap = this.methodMappingRegistry.getReadOnlyGroup(classNode);
				if (methodSignatureMap != null) {
					Enumeration<MethodSignature> signatureEnumeration = methodSignatureMap.getKeyEnumeration();

					while (signatureEnumeration.hasMoreElements()) {
						MethodSignature originalSignature = signatureEnumeration.nextElement();
						MethodSignature renamedSignature = methodSignatureMap.get(originalSignature);
						if (originalSignature.getName().equals(methodName) && methodPattern.matches(originalSignature.getFullDescriptor()) && classNode.findMethod(renamedSignature).hasAccessFlag(accessFlag)) {
							return renamedSignature.getName();
						}
					}
				}
			}
		}

		return null;
	}

	public final boolean registerMatchingMethods(
		ClassNode classNode,
		String methodName,
		String accessorPrefix,
		Object referenceSource,
		MethodPattern methodPattern,
		int accessFlag,
		Map<MethodNode, String> methodReferences,
		DualKeyMap<MethodNode, String, Object> resourceToClassReferences,
		String referenceDescription
	) {
		ClassNode currentClassNode = classNode;

		boolean matched;
		for (matched = false; currentClassNode != null; currentClassNode = ClassHierarchyNode.getWorkspaceClassNode(currentClassNode.getSuperclassName())) {
			List<AbstractMethodNode> methods = currentClassNode.findMethodsByName(methodName);

			for (AbstractMethodNode methodNode : methods) {
				if (methodNode.hasAccessFlag(accessFlag) && methodPattern.matches(methodNode.getDescriptor())) {
					matched = true;
					methodReferences.put((MethodNode)methodNode, referenceDescription);
					if (accessorPrefix != null) {
						resourceToClassReferences.put((MethodNode)methodNode, accessorPrefix, referenceSource);
					}
				}
			}

			if (methods.size() > 0) {
				break;
			}
		}

		return matched;
	}

	public final String mapClassName(String className) {
		return GlobalUtils.getMappedOrDefault(className.replace('.', '/'), this.classNameMapping).replace('/', '.');
	}

	public final List<FieldNode> findMatchingFields(String fieldName, AbstractClassNode fieldOwner, String annotationType, int accessFlag) {
		ArrayList<FieldNode> matchingFields = new ArrayList<>();
		FieldNode[] fieldNodes = this.classHierarchy.getFieldsBySignature(new FieldSignature(fieldName, GlobalUtils.toString(new String[]{"L", fieldOwner.getInternalName(), ";"})));
		if (fieldNodes != null) {
			for (FieldNode fieldNode : fieldNodes) {
				if (fieldNode.hasAccessFlag(accessFlag) && (annotationType == null || fieldNode.getAnnotationTypeNameSet().contains(annotationType))) {
					matchingFields.add(fieldNode);
				}
			}
		}

		return matchingFields;
	}

	public final String getAccessorFieldName(String accessorPrefix, String accessorName, String propertyName) {
		if (accessorName.startsWith(accessorPrefix) && accessorName.length() > accessorPrefix.length()) {
			String fieldName = MethodSignature.getFieldNameFromAccessor(accessorName, accessorPrefix);
			return propertyName != null && fieldName.toUpperCase().equals(propertyName.toUpperCase()) ? propertyName : fieldName;
		} else {
			return null;
		}
	}

	public List<AbstractMethodNode> findMethodsByName(String methodName) {
		if (this.methodMappingRegistry == null) {
			return null;
		} else {
			if (this.methodLookupCache == null) {
				this.methodLookupCache = new DualKeyMap<>();
				Enumeration<AbstractClassNode> classEnumeration = this.methodMappingRegistry.getGroupKeys();

				while (classEnumeration.hasMoreElements()) {
					AbstractClassNode groupClassNode = classEnumeration.nextElement();
					Iterator<Entry<MethodSignature, MethodSignature>> entryIterator = this.methodMappingRegistry.getReadOnlyGroup(groupClassNode).entrySet().iterator();

					while (entryIterator.hasNext()) {
						MethodSignature originalSignature = entryIterator.next().getKey();
						this.methodLookupCache.put(originalSignature.getName(), groupClassNode, originalSignature);
					}
				}
			}

			ArrayList<AbstractMethodNode> matchingMethods = new ArrayList<>();
			Map<AbstractClassNode, MethodSignature> signatureByClass = this.methodLookupCache.getGroup(methodName);
			if (signatureByClass != null) {
				for (Entry<AbstractClassNode, MethodSignature> cacheEntry : signatureByClass.entrySet()) {
					AbstractClassNode classNode = cacheEntry.getKey();
					MethodSignature resolvedSignature = GlobalUtils.getFromRegistryOrDefault(classNode, cacheEntry.getValue(), this.methodMappingRegistry);
					AbstractMethodNode methodNode = classNode.findMethod(resolvedSignature);
					matchingMethods.add(methodNode);
				}
			}

			return matchingMethods;
		}
	}

	public final String getMappedFieldName(String className, String fieldName) {
		if (this.fieldMappingRegistry != null) {
			for (ClassNode classNode = ClassHierarchyNode.getWorkspaceClassNode(GlobalUtils.binaryToInternalName(className));
				classNode != null;
				classNode = ClassHierarchyNode.getWorkspaceClassNode(classNode.getSuperclassName())
			) {
				classNode.getInternalName();
				ReadOnlyMap<FieldSignature, FieldSignature> fieldSignatureMap = this.fieldMappingRegistry.getReadOnlyGroup(classNode);
				if (fieldSignatureMap != null) {
					HashMap<FieldSignature, FieldSignature> reversedSignatureMap = GlobalUtils.newHashMap();
					Enumeration<FieldSignature> signatureEnumeration = fieldSignatureMap.getKeyEnumeration();

					while (signatureEnumeration.hasMoreElements()) {
						FieldSignature originalSignature = signatureEnumeration.nextElement();
						FieldSignature renamedSignature = fieldSignatureMap.get(originalSignature);
						reversedSignatureMap.put(renamedSignature, originalSignature);
						if (originalSignature.getName().equals(fieldName)) {
							return renamedSignature.getName();
						}
					}
				}
			}
		}

		return fieldName;
	}

	public final boolean registerWorkspaceMethods(
		String methodName,
		String accessorPrefix,
		Object referenceSource,
		MethodPattern methodPattern,
		String annotationType,
		int accessFlag,
		Map<ClassNode, String> classReferences,
		Map<MethodNode, String> methodReferences,
		DualKeyMap<MethodNode, String, Object> resourceToClassReferences,
		String referenceDescription
	) {
		List<MethodNode> matchingMethods = this.findMatchingMethods(methodName, methodPattern, annotationType, accessFlag);

		for (MethodNode methodNode : matchingMethods) {
			methodReferences.put(methodNode, referenceDescription);
			classReferences.put(methodNode.getMethodOwner(), referenceDescription);
			if (accessorPrefix != null) {
				resourceToClassReferences.put(methodNode, accessorPrefix, referenceSource);
			}
		}

		return matchingMethods.size() > 0;
	}

	public final AbstractClassNode loadClassNode(String binaryClassName) {
		String mappedInternalName = GlobalUtils.getMappedOrDefault(GlobalUtils.binaryToInternalName(binaryClassName), this.classNameMapping);
		AbstractClassNode classNode = null;

		try {
			classNode = this.classProvider.getClassNode(mappedInternalName, false);
		} catch (FileException ignored) {
		}

		return classNode;
	}

	public final String resolveMappedMethodName(String className, String methodName, int accessFlag, AtomicReference<String> errorMessage, AtomicBool ambiguous) {
		ambiguous.set(false);
		if (this.methodMappingRegistry != null) {
			ClassNode classNode = ClassHierarchyNode.getWorkspaceClassNode(GlobalUtils.binaryToInternalName(className));
			HashMap<MethodSignature, MethodSignature> reversedSignatureMap = null;

			ArrayList<MethodSignature> matchingSignatures;
			for (matchingSignatures = new ArrayList<>(); classNode != null && matchingSignatures.size() == 0; classNode = ClassHierarchyNode.getWorkspaceClassNode(classNode.getSuperclassName())) {
				classNode.getInternalName();
				ReadOnlyMap<MethodSignature, MethodSignature> methodSignatureMap = this.methodMappingRegistry.getReadOnlyGroup(classNode);
				if (methodSignatureMap != null) {
					reversedSignatureMap = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(methodSignatureMap.size()));
					Enumeration<MethodSignature> signatureEnumeration = methodSignatureMap.getKeyEnumeration();

					while (signatureEnumeration.hasMoreElements()) {
						MethodSignature originalSignature = signatureEnumeration.nextElement();
						MethodSignature renamedSignature = methodSignatureMap.get(originalSignature);
						reversedSignatureMap.put(renamedSignature, originalSignature);
						if (originalSignature.getName().equals(methodName) && classNode.findMethod(renamedSignature).hasAccessFlag(accessFlag)) {
							matchingSignatures.add(renamedSignature);
						}
					}
				}
			}

			if (matchingSignatures.size() == 1) {
				return matchingSignatures.get(0).getName();
			}

			if (matchingSignatures.size() >= 1) {
				HashSet<String> distinctNames = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(matchingSignatures.size()));
				ArrayList<String> candidateDescriptions = new ArrayList<>(matchingSignatures.size());

				for (MethodSignature matchingSignature : matchingSignatures) {
					distinctNames.add(matchingSignature.getName());
					candidateDescriptions.add(GlobalUtils.getMappedOrDefault(matchingSignature, reversedSignatureMap).toHumanReadableString());
				}

				if (distinctNames.size() == 1) {
					return distinctNames.iterator().next();
				}

				ambiguous.set(true);
				errorMessage.set("More than one method in class '" + className + "' or its hierarchy matches method name '" + methodName + "' : " + GlobalUtils.toQuotedString(candidateDescriptions));
				return null;
			}
		}

		return null;
	}

	public final String resolveFieldName(AbstractClassNode fieldOwner, String fieldName, String annotationType, int accessFlag, AtomicReference<String> errorMessage) {
		List<AbstractFieldNode> candidateFields = this.findFieldsByName(fieldName, fieldOwner);
		if (candidateFields == null) {
			errorMessage.set("No field method found matching '" + fieldName + "' (2)");
			return null;
		} else {
			ArrayList<AbstractFieldNode> matchingFields = new ArrayList<>();

			for (AbstractFieldNode candidateField : candidateFields) {
				if (candidateField.hasAccessFlag(accessFlag) && (annotationType == null || candidateField.getAnnotationTypeNameSet().contains(annotationType))) {
					matchingFields.add(candidateField);
				}
			}

			int matchCount = matchingFields.size();
			if (matchCount == 0) {
				errorMessage.set("No field method found matching '" + fieldName + "' (1)");
				return null;
			} else if (matchCount == 1) {
				return matchingFields.get(0).getNameDisplay();
			} else {
				HashSet<String> distinctNames = GlobalUtils.newHashSet();

				for (AbstractFieldNode matchingField : matchingFields) {
					distinctNames.add(matchingField.getNameDisplay());
				}

				if (distinctNames.size() == 1) {
					return distinctNames.iterator().next();
				} else {
					errorMessage.set("More than one candidate method found matching '" + fieldName + "' : " + GlobalUtils.toQuotedString(distinctNames));
					return null;
				}
			}
		}
	}

	public final void registerMethodBySignature(String className, MethodSignature methodSignature, Map<MethodNode, String> methodReferences, DualKeyMap<MethodNode, String, Object> resourceToClassReferences, String referenceDescription) {
		if (className != null) {
			for (ClassHierarchyNode classHierarchyNode = ClassHierarchyNode.getNodeByInternalName(GlobalUtils.binaryToInternalName(className));
				classHierarchyNode != null;
				classHierarchyNode = classHierarchyNode.getOuterClass()
			) {
				ClassNode classNode = classHierarchyNode.getClassNodeIfWorkspace();
				if (classNode != null) {
					MethodNode methodNode = this.classHierarchy.getMethodBySignature(classNode, methodSignature);
					if (methodNode != null) {
						methodReferences.put(methodNode, referenceDescription);
						break;
					}
				}
			}
		}
	}

	public void registerTypeReference(String typeName, Map<ClassNode, String> classReferences, String referenceDescription) {
		int arrayBracketIndex = typeName.indexOf("[]");
		String elementTypeName;
		if (arrayBracketIndex == -1) {
			elementTypeName = typeName;
		} else {
			elementTypeName = typeName.substring(0, arrayBracketIndex);
		}

		if (!elementTypeName.equals("byte")
			&& !elementTypeName.equals("short")
			&& !elementTypeName.equals("char")
			&& !elementTypeName.equals("int")
			&& !elementTypeName.equals("long")
			&& !elementTypeName.equals("float")
			&& !elementTypeName.equals("double")
			&& !elementTypeName.equals("boolean")) {
			this.findAndRegisterClassName(elementTypeName, classReferences, referenceDescription);
		}
	}

	public final String remapAttributeValue(String attributeValue, String attributeOrTagName, boolean useEnclosingTag) {
		String mappedValue = attributeValue;
		if (attributeValue.length() >= Options.minDottedNameLength && attributeOrTagName != null && attributeOrTagName.length() > 0) {
			if (attributeValue.indexOf(58) == -1
				&& attributeValue.indexOf(33) == -1
				&& attributeValue.indexOf(60) == -1
				&& attributeValue.indexOf(62) == -1
				&& attributeValue.indexOf(38) == -1
				&& attributeValue.indexOf(35) == -1
				&& attributeValue.indexOf(91) == -1
				&& attributeValue.indexOf(61) == -1) {
				int firstSlashIndex = attributeValue.indexOf(47, 1);
				if (firstSlashIndex > -1) {
					if (firstSlashIndex < attributeValue.length() - 1 && this.isReferenceAttribute(attributeOrTagName, useEnclosingTag, "file")) {
						mappedValue = this.resourcePathTransformer.remapPathWithCache(attributeValue);
					}
				} else if (attributeValue.indexOf(47) == -1) {
					int classNameDotIndex = attributeValue.indexOf(46);
					if (classNameDotIndex > 0 && classNameDotIndex < attributeValue.length() - 1 && this.isReferenceAttribute(attributeOrTagName, useEnclosingTag, "class")) {
						mappedValue = this.mapClassName(attributeValue);
					}
				}
			} else if (!Options.dontSplitColonNames
				&& attributeOrTagName.equals("class")
				&& attributeValue.indexOf(58) > Options.minDottedNameLength
				&& attributeValue.indexOf(33) == -1
				&& attributeValue.indexOf(60) == -1
				&& attributeValue.indexOf(62) == -1
				&& attributeValue.indexOf(38) == -1
				&& attributeValue.indexOf(35) == -1
				&& attributeValue.indexOf(91) == -1
				&& attributeValue.indexOf(61) == -1) {
				boolean allPartsMapped = true;
				StringBuilder mappedValueBuffer = new StringBuilder();
				String[] nameParts = attributeValue.split(":");

				for (int partIndex = 0; partIndex < nameParts.length && allPartsMapped; partIndex++) {
					String namePart = nameParts[partIndex];
					int partDotIndex = namePart.indexOf(46);
					if (partDotIndex > 0 && partDotIndex < namePart.length() - 1 && namePart.length() > Options.minDottedNameLength) {
						String mappedPart = this.mapClassName(namePart);
						mappedValueBuffer.append(mappedPart);
						if (partIndex < nameParts.length - 1) {
							mappedValueBuffer.append(":");
						}
					} else {
						allPartsMapped = false;
					}
				}

				if (allPartsMapped) {
					mappedValue = mappedValueBuffer.toString();
				}
			}
		}

		return mappedValue;
	}

	public final String registerClassName(String className, Map<ClassNode, String> classReferences, AtomicReference<ClassNode> resolvedClassRef, String referenceDescription) {
		String mappedInternalName = GlobalUtils.getMappedOrDefault(GlobalUtils.binaryToInternalName(className), this.classNameMapping);
		ClassNode classNode = ClassHierarchyNode.getWorkspaceClassNode(mappedInternalName);
		if (classNode != null) {
			classReferences.put(classNode, referenceDescription);
			if (resolvedClassRef != null) {
				resolvedClassRef.set(classNode);
			}
		}

		return GlobalUtils.internalToBinaryName(mappedInternalName);
	}

	@Override
	public final void handleEndTag(XmlElement element, List<Object> outputFragments) {
		super.elementStack.push(element);
		this.remapAttributeValues(element, outputFragments);
		super.elementStack.pop();
	}

	@Override
	public final void recordEndTagReferences(
		XmlElement element, Map<ClassNode, String> classReferences, Map<FieldNode, String> fieldReferences, Map<MethodNode, String> methodReferences, DualKeyMap<MethodNode, String, Object> methodAccessorReferences
	) {
		super.elementStack.push(element);
		this.collectAttributeReferences(element, classReferences, fieldReferences, methodReferences, methodAccessorReferences);
		super.elementStack.pop();
	}

	public final ClassNode getWorkspaceClassNode(String binaryClassName) {
		return ClassHierarchyNode.getWorkspaceClassNode(GlobalUtils.getMappedOrDefault(GlobalUtils.binaryToInternalName(binaryClassName), this.classNameMapping));
	}

	public final String resolveMethodName(String methodName, MethodPattern methodPattern, String annotationType, int accessFlag, AtomicReference<String> errorMessage) {
		if (this.methodMappingRegistry == null) {
			return methodName;
		} else {
			ArrayList<AbstractMethodNode> matchingMethods = new ArrayList<>();

			for (AbstractMethodNode candidateMethod : this.findMethodsByName(methodName)) {
				if (candidateMethod.hasAccessFlag(accessFlag) && methodPattern.matches(candidateMethod.getDescriptor()) && (annotationType == null || candidateMethod.getAnnotationTypeNameSet().contains(annotationType))) {
					matchingMethods.add(candidateMethod);
				}
			}

			int matchCount = matchingMethods.size();
			if (matchCount == 0) {
				errorMessage.set("No method found matching '" + methodName + "'");
				return null;
			} else if (matchCount == 1) {
				return matchingMethods.get(0).getNameDisplay();
			} else {
				HashSet<String> distinctNames = GlobalUtils.newHashSet();

				for (AbstractMethodNode matchingMethod : matchingMethods) {
					distinctNames.add(matchingMethod.getNameDisplay());
				}

				if (distinctNames.size() == 1) {
					return distinctNames.iterator().next();
				} else {
					errorMessage.set("More than one candidate method found matching '" + methodName + "' : " + GlobalUtils.toQuotedString(distinctNames));
					return null;
				}
			}
		}
	}

	public List<AbstractFieldNode> findFieldsByName(String fieldName, AbstractClassNode fieldOwner) {
		if (this.fieldMappingRegistry == null) {
			return null;
		} else {
			if (this.fieldLookupCache == null) {
				this.fieldLookupCache = new DualKeyMap<>();
				Enumeration<AbstractClassNode> classEnumeration = this.fieldMappingRegistry.getGroupKeys();

				while (classEnumeration.hasMoreElements()) {
					AbstractClassNode groupClassNode = classEnumeration.nextElement();

					for (Entry<FieldSignature, FieldSignature> mappingEntry : this.fieldMappingRegistry.getReadOnlyGroup(groupClassNode).entrySet()) {
						this.fieldLookupCache.put(mappingEntry.getKey(), groupClassNode, mappingEntry.getValue());
					}
				}
			}

			FieldSignature fieldSignature = new FieldSignature(fieldName, GlobalUtils.toString(new String[]{"L", fieldOwner.getClassName(), ";"}));
			ArrayList<AbstractFieldNode> matchingFields = new ArrayList<>();
			Map<AbstractClassNode, FieldSignature> signatureByClass = this.fieldLookupCache.getGroup(fieldSignature);
			if (signatureByClass != null) {
				for (Entry<AbstractClassNode, FieldSignature> cacheEntry : signatureByClass.entrySet()) {
					AbstractClassNode classNode = cacheEntry.getKey();
					FieldSignature resolvedSignature = GlobalUtils.getFromRegistryOrDefault(classNode, cacheEntry.getValue(), this.fieldMappingRegistry);
					AbstractFieldNode fieldNode = classNode.findField(resolvedSignature);
					matchingFields.add(fieldNode);
				}
			}

			return matchingFields;
		}
	}

	public final String remapPackageName(String packageName) {
		String internalPackageName = packageName.replace('.', '/');
		String remappedPackageName = this.resourcePathTransformer.getExactRemappedPath(internalPackageName);
		return packageName.indexOf(46) > -1 ? remappedPackageName.replace('/', '.') : remappedPackageName;
	}

	@Override
	public final void handleStartTag(XmlElement element, List<Object> outputFragments) {
		super.elementStack.push(element);
		this.remapAttributeValues(element, outputFragments);
	}

	public final String getMappedFieldName(AbstractClassNode classNode, AbstractClassNode fieldOwner, String fieldName) {
		if (this.fieldMappingRegistry != null) {
			FieldSignature fieldSignature = new FieldSignature(fieldName, GlobalUtils.toString(new String[]{"L", fieldOwner.getClassName(), ";"}));

			for (AbstractClassNode currentClassNode = classNode; currentClassNode != null; currentClassNode = ClassHierarchyNode.getWorkspaceClassNode(currentClassNode.getSuperclassName())) {
				FieldSignature renamedSignature = this.fieldMappingRegistry.get(classNode, fieldSignature);
				if (renamedSignature != null) {
					return renamedSignature.getName();
				}
			}

			return null;
		} else {
			return fieldName;
		}
	}

	public final String remapResourcePath(String resourcePath) {
		return this.resourcePathTransformer.remapPath(resourcePath);
	}
}
