package com.zelix.workspace.resources.xml;

import com.zelix.enums.PlatformType;
import com.zelix.exceptions.ZKMException;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.logging.AbstractLogger;
import com.zelix.repository.ClassProvider;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.resources.ResourceIndex;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SpringBeanImportXmlHandler extends ImportCapableXmlHandler {
	private final Set<String> importedResources;

	public SpringBeanImportXmlHandler(
		String fullResourcePath,
		ResourceIndex fileAdaptor,
		Set<String> importedResources,
		Map<String, ClassNode> beanClasses,
		Map<String, SpringBeanNode> abstractBeanElements,
		Map<String, String> beanParentNames,
		AbstractWorkspace classHierarchy,
		ClassProvider classProvider,
		AbstractLogger logger
	) {
		super(fullResourcePath, fileAdaptor, classHierarchy, classProvider, logger);
		this.importedResources = importedResources;
		importedResources.add(fullResourcePath);
		super.beanClasses.putAll(beanClasses);
		super.abstractBeanElements.putAll(abstractBeanElements);
		super.beanParentNames.putAll(beanParentNames);
	}

	@Override
	public List<String> importResources(String location, String baseDirectory) {
		ArrayList<String> importedResourcePaths = new ArrayList<>();

		for (ResourceIndex.XmlResourceInfo resourceInfo : super.fileAdaptor.collectPlatformResources(location, baseDirectory, PlatformType.SPRING, super.logger)) {
			if (!this.importedResources.contains(resourceInfo.path)) {
				try {
					SpringBeanImportXmlHandler importHandler = new SpringBeanImportXmlHandler(
						resourceInfo.path, super.fileAdaptor, this.importedResources, super.beanClasses, super.abstractBeanElements, super.beanParentNames, super.classHierarchy, super.classProvider, super.logger
					);
					new XmlParserRunner(resourceInfo.path, resourceInfo.content, resourceInfo.skipBytes, resourceInfo.bomLength, resourceInfo.byteOrder, resourceInfo.encoding.get(), importHandler);
					importedResourcePaths.add(resourceInfo.path);
					Map<String, ClassNode> importedBeanClasses = importHandler.getBeanClasses();
					super.beanClasses.putAll(importedBeanClasses);
					Map<String, SpringBeanNode> importedBeanElements = importHandler.getAbstractBeanElements();
					super.abstractBeanElements.putAll(importedBeanElements);
					Map<String, String> importedBeanParentNames = importHandler.getBeanParentNames();
					super.beanParentNames.putAll(importedBeanParentNames);
				} catch (ZKMException zkmException) {
					super.logger.error("FILE ERROR:", "Invalid XML file '" + resourceInfo.path + "' (B) : " + zkmException.getMessage());
				} catch (IOException ioException) {
					super.logger.error("FILE ERROR:", "Error reading XML file '" + resourceInfo.path + "' (B) : " + ioException.getMessage());
				} catch (Exception unexpectedException) {
					super.logger.error("FILE ERROR:", "Unexpected error analyzing XML file '" + super.fullResourcePath + "' (B) : " + unexpectedException.getMessage());
				}
			}
		}

		return importedResourcePaths;
	}

	@Override
	public void remapAttributeValues(XmlElement element, List<Object> outputFragments) {
	}

	@Override
	public void collectAttributeReferences(
		XmlElement element, Map<ClassNode, String> classReferences, Map<FieldNode, String> fieldReferences, Map<MethodNode, String> methodReferences, DualKeyMap<MethodNode, String, Object> methodAccessorReferences
	) {
		String tagName = element.getName();
		String parentTagName = this.getTagNameFromStack(1);
		if (tagName.equalsIgnoreCase("bean") && parentTagName != null && parentTagName.equalsIgnoreCase("beans")) {
			String referenceContext = "reference in '" + super.fullResourcePath + "' by '" + tagName + "' tag";
			String binaryClassName = null;
			AtomicReference<String> classAttributeRef = element.getAttributeValueRef("class");
			List<String> beanKeys = null;
			AtomicReference<String> idRef = element.getAttributeValueRef("id");
			if (idRef != null) {
				beanKeys = new ArrayList<>(1);
				beanKeys.add(idRef.get());
			} else {
				AtomicReference<String> nameRef = element.getAttributeValueRef("name");
				if (nameRef != null) {
					beanKeys = this.splitBeanNames(nameRef.get());
				}
			}

			AtomicReference<String> parentRef = element.getAttributeValueRef("parent");
			if (classAttributeRef != null) {
				String classAttributeValue = classAttributeRef.get();
				AtomicReference<ClassNode> classNodeRef = new AtomicReference<>();
				binaryClassName = this.registerClassName(classAttributeValue, classReferences, classNodeRef, referenceContext + " 'class' attribute");
				if (!classNodeRef.isNull() && beanKeys != null) {
					for (String classBeanKey : beanKeys) {
						super.beanClasses.put(classBeanKey, classNodeRef.get());
					}
				}
			}

			if (binaryClassName == null) {
				AtomicReference<String> factoryBeanRef = element.getAttributeValueRef("factory-bean");
				if (factoryBeanRef != null) {
					String factoryBeanName = factoryBeanRef.get();
					ClassNode factoryClassNode = super.beanClasses.get(factoryBeanName);
					if (factoryClassNode != null) {
						binaryClassName = factoryClassNode.getBinaryName();
						if (beanKeys != null) {
							for (String factoryBeanKey : beanKeys) {
								super.beanClasses.put(factoryBeanKey, factoryClassNode);
							}
						}
					}
				}
			}

			if (binaryClassName == null && parentRef != null && beanKeys != null) {
				String parentName = parentRef.get();

				for (String parentBeanKey : beanKeys) {
					super.beanParentNames.put(parentBeanKey, parentName);
					String ancestorName = parentName;
					ClassNode resolvedClassNode = null;

					while (ancestorName != null && resolvedClassNode == null) {
						resolvedClassNode = super.beanClasses.get(parentName);
						if (resolvedClassNode != null) {
							binaryClassName = resolvedClassNode.getBinaryName();
						} else {
							ancestorName = super.beanParentNames.get(ancestorName);
						}
					}

					if (resolvedClassNode != null) {
						super.beanClasses.put(parentBeanKey, resolvedClassNode);
					}
				}
			}

			if (binaryClassName == null) {
				AtomicReference<String> abstractRef = element.getAttributeValueRef("abstract");
				if (abstractRef != null && abstractRef.get().equalsIgnoreCase("true")) {
					XmlElement abstractParentElement = this.getTagFromStack(1);
					SpringBeanNode beanElement = new SpringBeanNode(element, abstractParentElement);
					if (beanKeys != null) {
						for (String abstractBeanKey : beanKeys) {
							super.abstractBeanElements.put(abstractBeanKey, beanElement);
						}
					}
				}
			}

			if (parentRef != null && beanKeys != null) {
				for (String fallbackBeanKey : beanKeys) {
					if (!super.beanParentNames.containsKey(fallbackBeanKey)) {
						super.beanParentNames.put(fallbackBeanKey, parentRef.get());
					}
				}
			}
		} else if (tagName.equalsIgnoreCase("property") && parentTagName != null && parentTagName.equalsIgnoreCase("bean")) {
			XmlElement propertyParentElement = this.getTagFromStack(1);
			AtomicReference<String> parentAbstractRef = propertyParentElement.getAttributeValueRef("abstract");
			if (parentAbstractRef != null && parentAbstractRef.get().equalsIgnoreCase("true")) {
				AtomicReference<String> parentIdRef = propertyParentElement.getAttributeValueRef("id");
				if (parentIdRef != null) {
					SpringBeanNode referencedBeanElement = super.abstractBeanElements.get(parentIdRef.get());
					if (referencedBeanElement != null) {
						referencedBeanElement.addPropertyElement(element);
					}
				}
			}
		} else if (tagName.equalsIgnoreCase("import") && parentTagName != null && parentTagName.equalsIgnoreCase("beans")) {
			Enumeration<String> attributeNames = element.getAttributeNames();

			while (attributeNames.hasMoreElements()) {
				String attributeName = attributeNames.nextElement();
				if (attributeName.equalsIgnoreCase("resource")) {
					String attributeValue = element.getAttributeValueRef(attributeName).get();
					this.processImport(attributeValue);
				}
			}
		}
	}

	public Map<String, SpringBeanNode> getAbstractBeanElements() {
		return GlobalUtils.newHashMap(super.abstractBeanElements);
	}

	public Map<String, String> getBeanParentNames() {
		return GlobalUtils.newHashMap(super.beanParentNames);
	}

	public Map<String, ClassNode> getBeanClasses() {
		return GlobalUtils.newHashMap(super.beanClasses);
	}
}
