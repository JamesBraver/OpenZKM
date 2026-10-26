package com.zelix.workspace.resources.xml;

import com.zelix.enums.PlatformType;
import com.zelix.exceptions.ZKMException;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.signature.FieldSignature;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.klassmaster.config.Options;
import com.zelix.logging.AbstractLogger;
import com.zelix.repository.ClassProvider;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.FileUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.PathRemapper;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.collections.RingQueue;
import com.zelix.utils.collections.readOnly.ReadOnlyMap;
import com.zelix.utils.collections.readOnly.ReadOnlyRegistryView;
import com.zelix.utils.dataset.AtomicBool;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.ClassHierarchyNode;
import com.zelix.workspace.resources.ResourceIndex;
import com.zelix.workspace.resources.xml.pattern.MaskMethodPattern;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class SpringBeanXmlHandler extends ImportCapableXmlHandler {
	private MultiMap<String, String> multiMap;

	public SpringBeanXmlHandler(String fullResourcePath, ResourceIndex fileAdaptor, AbstractWorkspace classHierarchy, ClassProvider classProvider, AbstractLogger logger) {
		super(fullResourcePath, fileAdaptor, classHierarchy, classProvider, logger);
		Map<String, ClassNode> beanClassesMap = fileAdaptor.getBeanClasses(fullResourcePath);
		if (beanClassesMap != null) {
			super.beanClasses.putAll(beanClassesMap);
		}

		Map<String, String> beanParentNamesMap = fileAdaptor.getBeanParents(fullResourcePath);
		if (beanParentNamesMap != null) {
			super.beanParentNames.putAll(beanParentNamesMap);
		}
	}

	public SpringBeanXmlHandler(
		String fullResourcePath,
		ReadOnlyMap<String, String> classNameMapping,
		ReadOnlyRegistryView<AbstractClassNode, FieldSignature, FieldSignature> fieldMappingRegistry,
		ReadOnlyRegistryView<AbstractClassNode, MethodSignature, MethodSignature> methodMappingRegistry,
		PathRemapper resourcePathTransformer,
		ResourceIndex fileAdaptor,
		AbstractWorkspace classHierarchy,
		ClassProvider classProvider,
		AbstractLogger logger
	) {
		super(fullResourcePath, classNameMapping, fieldMappingRegistry, methodMappingRegistry, fileAdaptor, resourcePathTransformer, classHierarchy, classProvider, logger);
		Map<String, ClassNode> beanClassesMap = fileAdaptor.getBeanClasses(fullResourcePath);
		if (beanClassesMap != null) {
			super.beanClasses.putAll(beanClassesMap);
		}

		Map<String, String> beanParentNamesMap = fileAdaptor.getBeanParents(fullResourcePath);
		if (beanParentNamesMap != null) {
			super.beanParentNames.putAll(beanParentNamesMap);
		}
	}

	@Override
	public void resolveDeferredReferences(
		Map<FieldNode, String> fieldReferences, Map<MethodNode, String> methodReferences, DualKeyMap<MethodNode, String, Object> methodAccessorReferences
	) {
		super.fileAdaptor.addBeanParents(super.fullResourcePath, super.beanParentNames);
		super.fileAdaptor.addBeanClasses(super.fullResourcePath, super.beanClasses);
		List<String> imports = super.fileAdaptor.getImports(super.fullResourcePath);
		if (imports != null) {
			HashSet<String> seenImports = GlobalUtils.newHashSet();
			RingQueue<String> importQueue = new RingQueue<>();

			for (String importedResource : imports) {
				importQueue.push(importedResource);
				seenImports.add(importedResource);
			}

			while (!importQueue.isEmpty()) {
				String resourcePath = importQueue.pop();
				List<String> importers = super.fileAdaptor.getImporters(resourcePath);
				if (importers != null) {
					for (String importer : importers) {
						Map<String, String> beanParents = super.fileAdaptor.getBeanParents(importer);
						if (beanParents != null) {
							super.fileAdaptor.addBeanParents(resourcePath, beanParents);
						}

						Map<String, ClassNode> beanClassesMap = super.fileAdaptor.getBeanClasses(importer);
						if (beanClassesMap != null) {
							super.fileAdaptor.addBeanClasses(resourcePath, beanClassesMap);
						}

						List<String> resourceImports = super.fileAdaptor.getImports(resourcePath);
						if (resourceImports != null) {
							for (String resourceImport : resourceImports) {
								if (seenImports.add(resourceImport)) {
									importQueue.push(resourceImport);
								}
							}
						}
					}
				}
			}
		}
	}

	@Override
	public List<String> importResources(String location, String baseDirectory) {
		ArrayList<String> importedPaths = new ArrayList<>();

		for (ResourceIndex.XmlResourceInfo resourceInfo : super.fileAdaptor.collectPlatformResources(location, baseDirectory, PlatformType.SPRING, super.logger)) {
			try {
				HashSet<String> importedResources = GlobalUtils.newHashSet();
				SpringBeanImportXmlHandler importHandler = new SpringBeanImportXmlHandler(
					resourceInfo.path, super.fileAdaptor, importedResources, super.beanClasses, super.abstractBeanElements, super.beanParentNames, super.classHierarchy, super.classProvider, super.logger
				);
				new XmlParserRunner(resourceInfo.path, resourceInfo.content, resourceInfo.skipBytes, resourceInfo.bomLength, resourceInfo.byteOrder, resourceInfo.encoding.get(), importHandler);
				importedPaths.add(resourceInfo.path);
				Map<String, ClassNode> importedBeanClasses = importHandler.getBeanClasses();

				for (Entry<String, ClassNode> beanClassEntry : importedBeanClasses.entrySet()) {
					super.beanClasses.put(beanClassEntry.getKey(), beanClassEntry.getValue());
				}

				for (Entry<String, SpringBeanNode> abstractBeanEntry : importHandler.getAbstractBeanElements().entrySet()) {
					super.abstractBeanElements.put(abstractBeanEntry.getKey(), abstractBeanEntry.getValue());
				}

				Map<String, String> importedBeanParentNames = importHandler.getBeanParentNames();

				for (Entry<String, String> beanParentEntry : importedBeanParentNames.entrySet()) {
					super.beanParentNames.put(beanParentEntry.getKey(), beanParentEntry.getValue());
				}

				super.fileAdaptor.addBeanClasses(super.fullResourcePath, importedBeanClasses);
				super.fileAdaptor.addBeanParents(super.fullResourcePath, importedBeanParentNames);
			} catch (ZKMException ignored) {
				super.logger.error("FILE ERROR:", "Invalid XML file '" + resourceInfo.path + "' (C) : " + ignored.getMessage());
			} catch (IOException ignoredIo) {
				super.logger.error("FILE ERROR:", "Error reading XML file '" + resourceInfo.path + "' (C) : " + ignoredIo.getMessage());
			} catch (Exception ignoredException) {
				super.logger.error("FILE ERROR:", "Unexpected error analyzing XML file '" + super.fullResourcePath + "' (C) : " + ignoredException.getMessage());
			}
		}

		return importedPaths;
	}

	@Override
	public void remapAttributeValues(XmlElement element, List<Object> outputFragments) {
		String tagName = element.getName();
		String parentTagName = this.getTagNameFromStack(1);
		String beanClassName = this.findBeanClassName();
		String mappedClassName = null;
		if (beanClassName != null) {
			mappedClassName = this.mapClassName(beanClassName);
		}

		if (tagName.equalsIgnoreCase("bean") && parentTagName != null && parentTagName.equalsIgnoreCase("beans")) {
			AtomicReference<String> classAttributeRef = element.getAttributeValueRef("class");
			if (classAttributeRef != null && !classAttributeRef.get().equals(mappedClassName)) {
				classAttributeRef.set(mappedClassName);
			}
		}

		if (tagName.equalsIgnoreCase("beans") && parentTagName == null) {
			ReferenceGraph<String, ClassNode> defaultMethodClasses = super.fileAdaptor.getDefaultMethodClasses(super.fullResourcePath, "beans");
			if (defaultMethodClasses != null) {
				for (Entry<String, Set<ClassNode>> adjacencyEntry : defaultMethodClasses.getAdjacencyEntries()) {
					String trackedAttributeName = adjacencyEntry.getKey();
					AtomicReference<String> trackedAttributeRef = element.getAttributeValueRef(trackedAttributeName);
					if (trackedAttributeRef != null) {
						String trackedAttributeValue = trackedAttributeRef.get();
						MethodSignature methodSignature = new MethodSignature(trackedAttributeValue, "()V");
						Set<ClassNode> classNodes = adjacencyEntry.getValue();
						StringBuilder binaryNamesBuilder = new StringBuilder();
						String firstMappedMethodName = null;
						AtomicBool mappedFlag = new AtomicBool();

						for (ClassNode classNode : classNodes) {
							String mappedMethodName = this.getMappedMethodName(classNode.getBinaryName(), methodSignature, mappedFlag);
							if (binaryNamesBuilder.length() > 0) {
								binaryNamesBuilder.append(",");
							}

							binaryNamesBuilder.append(classNode.getBinaryName());
							if (firstMappedMethodName == null) {
								if (mappedFlag.get()) {
									firstMappedMethodName = mappedMethodName;
								}
							} else if (mappedFlag.get() && !mappedMethodName.equals(firstMappedMethodName)) {
								super.logger
									.error(
										"ERROR:",
										"Clashing new method names for attribute '"
											+ trackedAttributeName
											+ "' of tag 'beans' in  XML file '"
											+ super.fullResourcePath
											+ "' : '"
											+ firstMappedMethodName
											+ "' '"
											+ mappedMethodName
											+ "' : '"
											+ binaryNamesBuilder
											+ "'"
									);
							}
						}

						if (!trackedAttributeValue.equals(firstMappedMethodName)) {
							trackedAttributeRef.set(firstMappedMethodName);
						}
					}
				}
			}
		} else if (tagName.equalsIgnoreCase("bean") && parentTagName != null && parentTagName.equalsIgnoreCase("beans")) {
			if (mappedClassName == null) {
				outputFragments.add(element);
				return;
			}

			Enumeration<String> beanAttributeNames = element.getAttributeNames();

			while (beanAttributeNames.hasMoreElements()) {
				String beanAttributeName = beanAttributeNames.nextElement();
				AtomicReference<String> beanAttributeRef = element.getAttributeValueRef(beanAttributeName);
				String beanAttributeValue = beanAttributeRef.get();
				if (!beanAttributeName.equalsIgnoreCase("class")) {
					if (beanAttributeName.equalsIgnoreCase("init-method")) {
						MethodSignature initMethodSignature = new MethodSignature(beanAttributeValue, "()V");
						String mappedInitMethodName = this.getMappedMethodName(mappedClassName, initMethodSignature, new AtomicBool());
						if (!beanAttributeValue.equals(mappedInitMethodName)) {
							beanAttributeRef.set(mappedInitMethodName);
						}
					} else if (beanAttributeName.equalsIgnoreCase("destroy-method")) {
						MethodSignature destroyMethodSignature = new MethodSignature(beanAttributeValue, "()V");
						String mappedDestroyMethodName = this.getMappedMethodName(mappedClassName, destroyMethodSignature, new AtomicBool());
						if (!beanAttributeValue.equals(mappedDestroyMethodName)) {
							beanAttributeRef.set(mappedDestroyMethodName);
						}
					} else if (beanAttributeName.equalsIgnoreCase("factory-method")) {
						AtomicReference<String> candidateNameRef = new AtomicReference<>();
						AtomicBool factoryMappedFlag = new AtomicBool();
						String resolvedFactoryMethodName = this.resolveMappedMethodName(mappedClassName, beanAttributeValue, 8, candidateNameRef, factoryMappedFlag);
						if (resolvedFactoryMethodName != null) {
							if (!beanAttributeValue.equals(resolvedFactoryMethodName)) {
								beanAttributeRef.set(resolvedFactoryMethodName);
							}
						} else if (factoryMappedFlag.get()) {
							super.logger
								.error(
									"ERROR:",
									"Could not update '"
										+ beanAttributeName
										+ "' attribute of tag '"
										+ tagName
										+ "' in  XML file '"
										+ super.fullResourcePath
										+ "' : existing value is '"
										+ beanAttributeValue
										+ "' : \""
										+ candidateNameRef.get()
										+ "\""
								);
						}
					}
				}
			}
		} else if (tagName.equalsIgnoreCase("import") && parentTagName != null && parentTagName.equalsIgnoreCase("beans")) {
			Enumeration<String> importAttributeNames = element.getAttributeNames();

			while (importAttributeNames.hasMoreElements()) {
				String importAttributeName = importAttributeNames.nextElement();
				AtomicReference<String> importAttributeRef = element.getAttributeValueRef(importAttributeName);
				String resourceValue = importAttributeRef.get();
				if (importAttributeName.equalsIgnoreCase("resource")) {
					if (!resourceValue.startsWith("classpath:") && !resourceValue.startsWith("classpath*:") && !resourceValue.startsWith("file:")) {
						AtomicReference<String> resourceDirectoryRef = new AtomicReference<>();
						AtomicReference<String> resourceFileNameRef = new AtomicReference<>();
						FileUtils.splitPath(super.resourceName, resourceDirectoryRef, resourceFileNameRef);
						String thisResourceDirectory = resourceDirectoryRef.get();
						if (resourceValue.startsWith("/")) {
							new StringBuilder().append("").append("/").toString();
							resourceValue.substring(1);
						}

						if (resourceDirectoryRef.get().length() > 0) {
							String remappedRelativePath = this.remapRelativePath(thisResourceDirectory, resourceValue, true);
							if (!remappedRelativePath.equals(resourceValue)) {
								importAttributeRef.set(remappedRelativePath);
							}
						}
					} else {
						int colonIndex = resourceValue.indexOf(":");
						String schemePrefix = resourceValue.substring(0, colonIndex + 1);
						String pathAfterScheme = resourceValue.substring(colonIndex + 1);
						if (pathAfterScheme.startsWith("/")) {
							schemePrefix = schemePrefix + "/";
							pathAfterScheme = pathAfterScheme.substring(1);
						}

						AtomicReference<String> directoryRef = new AtomicReference<>();
						AtomicReference<String> fileNameRef = new AtomicReference<>();
						String pathSeparator = FileUtils.splitPath(pathAfterScheme, directoryRef, fileNameRef);
						String resourceDirectory = directoryRef.get();
						if (resourceDirectory.length() > 0) {
							String fileName = fileNameRef.get();
							String remappedResourceDirectory = this.remapResourcePath(resourceDirectory);
							if (!remappedResourceDirectory.equals(resourceDirectory)) {
								String newResourceValue = schemePrefix + remappedResourceDirectory + pathSeparator + fileName;
								importAttributeRef.set(newResourceValue);
							}
						}
					}
				}
			}
		} else if (tagName.equalsIgnoreCase("property") && parentTagName != null && parentTagName.equalsIgnoreCase("bean")) {
			AtomicReference<String> nameAttributeRef = element.getAttributeValueRef("name");
			if (nameAttributeRef != null) {
				String propertyName = nameAttributeRef.get();
				if (mappedClassName != null) {
					this.updateBeanPropertyName(mappedClassName, beanClassName, tagName, propertyName, nameAttributeRef);
				}
			}
		} else {
			Enumeration<String> attributeNames = element.getAttributeNames();

			while (attributeNames.hasMoreElements()) {
				String attributeName = attributeNames.nextElement();
				AtomicReference<String> attributeRef = element.getAttributeValueRef(attributeName);
				attributeRef.set(this.remapAttributeValue(attributeRef.get(), attributeName, false));
			}
		}

		outputFragments.add(element);
	}

	@Override
	public void collectAttributeReferences(
		XmlElement element, Map<ClassNode, String> classReferences, Map<FieldNode, String> fieldReferences, Map<MethodNode, String> methodReferences, DualKeyMap<MethodNode, String, Object> methodAccessorReferences
	) {
		String tagName = element.getName();
		String parentTagName = this.getTagNameFromStack(1);
		if (tagName.equalsIgnoreCase("bean") && parentTagName != null && parentTagName.equalsIgnoreCase("beans")) {
			boolean abstractBean = false;
			boolean parentSpecified = false;
			String referencePrefix = "reference in '" + super.fullResourcePath + "' by '" + tagName + "' tag";
			String beanClassName = null;
			AtomicReference<String> classAttributeRef = element.getAttributeValueRef("class");
			List<String> beanNames = null;
			AtomicReference<String> idAttributeRef = element.getAttributeValueRef("id");
			if (idAttributeRef != null) {
				beanNames = new ArrayList<>(1);
				beanNames.add(idAttributeRef.get());
			} else {
				AtomicReference<String> beanNameAttributeRef = element.getAttributeValueRef("name");
				if (beanNameAttributeRef != null) {
					beanNames = this.splitBeanNames(beanNameAttributeRef.get());
				}
			}

			if (classAttributeRef != null) {
				String className = classAttributeRef.get();
				AtomicReference<ClassNode> classNodeRef = new AtomicReference<>();
				beanClassName = this.registerClassName(className, classReferences, classNodeRef, referencePrefix + " 'class' attribute");
				if (!classNodeRef.isNull() && beanNames != null) {
					for (String beanName : beanNames) {
						super.beanClasses.put(beanName, classNodeRef.get());
					}
				}
			}

			if (beanClassName == null) {
				AtomicReference<String> factoryBeanAttributeRef = element.getAttributeValueRef("factory-bean");
				if (factoryBeanAttributeRef != null) {
					String factoryBeanName = factoryBeanAttributeRef.get();
					ClassNode factoryBeanClass = super.beanClasses.get(factoryBeanName);
					if (factoryBeanClass != null && beanNames != null) {
						beanClassName = factoryBeanClass.getBinaryName();

						for (String factoryBeanAlias : beanNames) {
							super.beanClasses.put(factoryBeanAlias, factoryBeanClass);
						}
					}
				}
			}

			if (beanClassName == null) {
				AtomicReference<String> parentAttributeRef = element.getAttributeValueRef("parent");
				if (parentAttributeRef != null && beanNames != null) {
					parentSpecified = true;
					String parentBeanName = parentAttributeRef.get();

					for (String parentBeanAlias : beanNames) {
						super.beanParentNames.put(parentBeanAlias, parentBeanName);
						String currentParentName = parentBeanName;
						ClassNode parentBeanClass = null;

						while (currentParentName != null && parentBeanClass == null) {
							parentBeanClass = super.beanClasses.get(currentParentName);
							if (parentBeanClass != null) {
								beanClassName = parentBeanClass.getBinaryName();
								super.beanClasses.put(parentBeanAlias, parentBeanClass);
							} else {
								currentParentName = super.beanParentNames.get(currentParentName);
							}
						}
					}
				}
			}

			if (beanClassName == null) {
				AtomicReference<String> abstractAttributeRef = element.getAttributeValueRef("abstract");
				if (abstractAttributeRef != null && abstractAttributeRef.get().equalsIgnoreCase("true")) {
					abstractBean = true;
					XmlElement beansElement = this.getTagFromStack(1);
					if (beanNames != null) {
						for (String abstractBeanAlias : beanNames) {
							SpringBeanNode springBeanNode = new SpringBeanNode(element, beansElement);
							super.abstractBeanElements.put(abstractBeanAlias, springBeanNode);
						}
					}
				}
			}

			if (beanClassName == null) {
				if (!abstractBean) {
					if (Options.ZKM_XML_ASSERTS) {
						DebugUtils.assertNotNullObject(beanClassName, new Object[]{super.fullResourcePath, element, 0});
					} else {
						super.logger.error("ERROR:", "Could not identify bean class in : '" + super.fullResourcePath + "' : '" + element + "' (A)");
					}
				}

				return;
			}

			ClassNode workspaceClassNode = ClassHierarchyNode.getWorkspaceClassNode(GlobalUtils.binaryToInternalName(beanClassName));
			if (workspaceClassNode != null) {
				this.registerWorkspaceMethods("<init>", null, null, MaskMethodPattern.MATCH_ALL, null, 0, classReferences, methodReferences, methodAccessorReferences, referencePrefix);
			}

			boolean initMethodRegistered = false;
			boolean destroyMethodRegistered = false;
			Enumeration<String> attributeNames = element.getAttributeNames();

			while (attributeNames.hasMoreElements()) {
				String attributeName = attributeNames.nextElement();
				String attributeValue = element.getAttributeValueRef(attributeName).get();
				String attributeReferenceDescription = referencePrefix + " '" + attributeName + "' attribute";
				if (!attributeName.equalsIgnoreCase("class")) {
					if (attributeName.equalsIgnoreCase("init-method")) {
						MethodSignature initMethodSignature = new MethodSignature(attributeValue, "()V");
						DebugUtils.assertNotNullObject(beanClassName, new Object[]{initMethodSignature, super.fullResourcePath, element, 1});
						this.registerMethodBySignature(beanClassName, initMethodSignature, methodReferences, methodAccessorReferences, attributeReferenceDescription);
						initMethodRegistered = true;
					} else if (attributeName.equalsIgnoreCase("destroy-method")) {
						MethodSignature destroyMethodSignature = new MethodSignature(attributeValue, "()V");
						DebugUtils.assertNotNullObject(beanClassName, new Object[]{destroyMethodSignature, super.fullResourcePath, element, 2});
						this.registerMethodBySignature(beanClassName, destroyMethodSignature, methodReferences, methodAccessorReferences, attributeReferenceDescription);
						destroyMethodRegistered = true;
					} else if (attributeName.equalsIgnoreCase("factory-method")) {
						if (workspaceClassNode != null) {
							this.registerMatchingMethods(workspaceClassNode, attributeValue, (String)null, null, BeanPropertyXmlHandler.SINGLE_ARG_METHOD_PATTERN, 8, methodReferences, methodAccessorReferences, referencePrefix);
						}
					} else if (attributeName.equalsIgnoreCase("parent")) {
						parentSpecified = true;
						if (beanNames != null) {
							for (String parentAttrAlias : beanNames) {
								super.beanParentNames.put(parentAttrAlias, attributeValue);
							}
						}
					}
				}
			}

			if (parentSpecified) {
				String ancestorBeanName = null;
				if (beanNames != null) {
					for (String candidateAlias : beanNames) {
						ancestorBeanName = super.beanParentNames.get(candidateAlias);
						if (ancestorBeanName != null) {
							break;
						}
					}
				}

				SpringBeanNode ancestorBeanNode = super.abstractBeanElements.get(ancestorBeanName);

				while (ancestorBeanName != null && ancestorBeanNode != null) {
					XmlElement beanElement = ancestorBeanNode.getBeanElement();
					if (!initMethodRegistered && beanElement.getAttributeValueRef("default-init-method") != null && beanElement.getAttributeValueRef("default-init-method").get() != null) {
						String defaultInitMethodName = beanElement.getAttributeValueRef("default-init-method").get();
						MethodSignature defaultInitMethodSignature = new MethodSignature(defaultInitMethodName, "()V");
						this.registerMethodBySignature(beanClassName, defaultInitMethodSignature, methodReferences, methodAccessorReferences, "reference in '" + super.fullResourcePath + "' by 'bean' tag default-init-method='" + defaultInitMethodName + "'");
						initMethodRegistered = true;
					}

					if (!destroyMethodRegistered && beanElement.getAttributeValueRef("default-destroy-method") != null && beanElement.getAttributeValueRef("default-destroy-method").get() != null) {
						String defaultDestroyMethodName = beanElement.getAttributeValueRef("default-destroy-method").get();
						MethodSignature defaultDestroyMethodSignature = new MethodSignature(defaultDestroyMethodName, "()V");
						this.registerMethodBySignature(beanClassName, defaultDestroyMethodSignature, methodReferences, methodAccessorReferences, "reference in '" + super.fullResourcePath + "' by 'bean' tag default-destroy-method='" + defaultDestroyMethodName + "'");
						destroyMethodRegistered = true;
					}

					XmlElement abstractBeansElement = ancestorBeanNode.getBeansElement();
					if (!initMethodRegistered && abstractBeansElement.getAttributeValueRef("default-init-method") != null && abstractBeansElement.getAttributeValueRef("default-init-method").get() != null) {
						String beansDefaultInitMethodName = abstractBeansElement.getAttributeValueRef("default-init-method").get();
						MethodSignature beansDefaultInitMethodSignature = new MethodSignature(beansDefaultInitMethodName, "()V");
						this.registerMethodBySignature(beanClassName, beansDefaultInitMethodSignature, methodReferences, methodAccessorReferences, "reference in '" + super.fullResourcePath + "' by 'beans' tag default-init-method='" + beansDefaultInitMethodName + "'");
						if (workspaceClassNode != null) {
							super.fileAdaptor.addDefaultMethodClass(super.fullResourcePath, "beans", "default-init-method", workspaceClassNode);
						}

						initMethodRegistered = true;
					}

					if (!destroyMethodRegistered && abstractBeansElement.getAttributeValueRef("default-destroy-method") != null && abstractBeansElement.getAttributeValueRef("default-destroy-method").get() != null) {
						String beansDefaultDestroyMethodName = abstractBeansElement.getAttributeValueRef("default-destroy-method").get();
						MethodSignature beansDefaultDestroyMethodSignature = new MethodSignature(beansDefaultDestroyMethodName, "()V");
						this.registerMethodBySignature(beanClassName, beansDefaultDestroyMethodSignature, methodReferences, methodAccessorReferences, "reference in '" + super.fullResourcePath + "' by 'beans' tag default-destroy-method='" + beansDefaultDestroyMethodName + "'");
						if (workspaceClassNode != null) {
							super.fileAdaptor.addDefaultMethodClass(super.fullResourcePath, "beans", "default-destroy-method", workspaceClassNode);
						}

						destroyMethodRegistered = true;
					}

					for (XmlElement propertyElement : ancestorBeanNode.getPropertyElements()) {
						AtomicReference<String> propertyNameRef = propertyElement.getAttributeValueRef("name");
						if (propertyNameRef != null) {
							String abstractPropertyName = propertyNameRef.get();
							String abstractPropertyReferenceDescription = "reference in '" + super.fullResourcePath + "' by '" + tagName + "' tag 'name' attribute (B)";
							if (workspaceClassNode != null) {
								this.registerPropertyAccessors(workspaceClassNode, abstractPropertyName, methodReferences, methodAccessorReferences, abstractPropertyReferenceDescription);
							}
						}
					}

					ancestorBeanName = super.beanParentNames.get(ancestorBeanName);
					if (ancestorBeanName != null) {
						ancestorBeanNode = super.abstractBeanElements.get(ancestorBeanName);
					}
				}
			}

			XmlElement enclosingBeansElement = this.getTagFromStack(1);
			if (!initMethodRegistered && enclosingBeansElement.getAttributeValueRef("default-init-method") != null && enclosingBeansElement.getAttributeValueRef("default-init-method").get() != null) {
				String enclosingDefaultInitMethodName = enclosingBeansElement.getAttributeValueRef("default-init-method").get();
				MethodSignature enclosingDefaultInitMethodSignature = new MethodSignature(enclosingDefaultInitMethodName, "()V");
				this.registerMethodBySignature(beanClassName, enclosingDefaultInitMethodSignature, methodReferences, methodAccessorReferences, "reference in '" + super.fullResourcePath + "' by 'beans' tag default-init-method='" + enclosingDefaultInitMethodName + "'");
				if (workspaceClassNode != null) {
					super.fileAdaptor.addDefaultMethodClass(super.fullResourcePath, "beans", "default-init-method", workspaceClassNode);
				}
			}

			if (!destroyMethodRegistered && enclosingBeansElement.getAttributeValueRef("default-destroy-method") != null && enclosingBeansElement.getAttributeValueRef("default-destroy-method").get() != null) {
				String enclosingDefaultDestroyMethodName = enclosingBeansElement.getAttributeValueRef("default-destroy-method").get();
				MethodSignature enclosingDefaultDestroyMethodSignature = new MethodSignature(enclosingDefaultDestroyMethodName, "()V");
				this.registerMethodBySignature(beanClassName, enclosingDefaultDestroyMethodSignature, methodReferences, methodAccessorReferences, "reference in '" + super.fullResourcePath + "' by 'beans' tag default-destroy-method='" + enclosingDefaultDestroyMethodName + "'");
				if (workspaceClassNode != null) {
					super.fileAdaptor.addDefaultMethodClass(super.fullResourcePath, "beans", "default-destroy-method", workspaceClassNode);
				}
			}
		} else if (tagName.equalsIgnoreCase("import") && parentTagName != null && parentTagName.equalsIgnoreCase("beans")) {
			Enumeration<String> importAttributeNames = element.getAttributeNames();

			while (importAttributeNames.hasMoreElements()) {
				String importAttributeName = importAttributeNames.nextElement();
				if (importAttributeName.equalsIgnoreCase("resource")) {
					String importResourceValue = element.getAttributeValueRef(importAttributeName).get();
					this.processImport(importResourceValue);
				}
			}
		} else if (tagName.equalsIgnoreCase("property") && parentTagName != null && parentTagName.equalsIgnoreCase("bean")) {
			AtomicReference<String> nameAttributeRef = element.getAttributeValueRef("name");
			if (nameAttributeRef != null) {
				String propertyName = nameAttributeRef.get();
				String propertyReferenceDescription = "reference in '" + super.fullResourcePath + "' by '" + tagName + "' tag 'name' attribute (A)";
				String enclosingBeanClassName = this.findBeanClassName();
				if (enclosingBeanClassName != null) {
					ClassNode beanClassNode = this.getWorkspaceClassNode(enclosingBeanClassName);
					if (beanClassNode != null) {
						this.registerPropertyAccessors(beanClassNode, propertyName, methodReferences, methodAccessorReferences, propertyReferenceDescription);
					}
				} else {
					XmlElement enclosingElement = this.getTagFromStack(1);
					AtomicReference<String> enclosingAbstractAttributeRef = enclosingElement.getAttributeValueRef("abstract");
					if (enclosingAbstractAttributeRef != null && enclosingAbstractAttributeRef.get().equalsIgnoreCase("true")) {
						AtomicReference<String> enclosingIdAttributeRef = enclosingElement.getAttributeValueRef("id");
						if (enclosingIdAttributeRef != null) {
							SpringBeanNode abstractBeanNode = super.abstractBeanElements.get(enclosingIdAttributeRef.get());
							if (abstractBeanNode != null) {
								abstractBeanNode.addPropertyElement(element);
							}
						}
					}
				}
			}
		} else {
			super.collectAttributeReferences(element, classReferences, fieldReferences, methodReferences, methodAccessorReferences);
		}
	}

	private String findBeanClassName() {
		for (int i = 0; i < super.elementStack.size(); i++) {
			XmlElement element = super.elementStack.peek(i);
			String tagName = element.getName();
			if (tagName.equals("beans") || tagName.equals("bean")) {
				AtomicReference<String> idAttributeRef = element.getAttributeValueRef("id");
				List<String> beanNames = null;
				if (idAttributeRef != null) {
					ClassNode idBeanClass = super.beanClasses.get(idAttributeRef.get());
					if (idBeanClass != null) {
						return idBeanClass.getBinaryName();
					}
				} else {
					AtomicReference<String> nameAttributeRef = element.getAttributeValueRef("name");
					if (nameAttributeRef != null) {
						beanNames = this.splitBeanNames(nameAttributeRef.get());

						for (String beanName : beanNames) {
							ClassNode nameBeanClass = super.beanClasses.get(beanName);
							if (nameBeanClass != null) {
								return nameBeanClass.getBinaryName();
							}
						}
					}
				}

				AtomicReference<String> classAttributeRef = element.getAttributeValueRef("class");
				if (classAttributeRef != null) {
					return classAttributeRef.get();
				}

				AtomicReference<String> factoryBeanAttributeRef = element.getAttributeValueRef("factory-bean");
				if (factoryBeanAttributeRef != null) {
					String factoryBeanName = factoryBeanAttributeRef.get();
					ClassNode factoryBeanClass = super.beanClasses.get(factoryBeanName);
					if (factoryBeanClass != null) {
						return factoryBeanClass.getBinaryName();
					}
				}

				AtomicReference<String> abstractAttributeRef = element.getAttributeValueRef("abstract");
				if ((idAttributeRef != null || beanNames != null && beanNames.size() > 0) && abstractAttributeRef != null && abstractAttributeRef.get().equalsIgnoreCase("true")) {
					if (this.multiMap == null) {
						this.multiMap = new MultiMap<>();

						for (Entry<String, String> beanParentEntry : super.beanParentNames.entrySet()) {
							this.multiMap.put(beanParentEntry.getValue(), beanParentEntry.getKey());
						}
					}

					ArrayList<String> aliasBeanNames = new ArrayList<>();
					if (idAttributeRef != null) {
						String beanId = idAttributeRef.get();
						aliasBeanNames.add(beanId);
					} else {
						aliasBeanNames.addAll(beanNames);
					}

					for (String aliasBeanName : aliasBeanNames) {
						ClassNode aliasBeanClass = this.findAliasBeanClass(aliasBeanName, this.multiMap);
						if (aliasBeanClass != null) {
							return aliasBeanClass.getBinaryName();
						}
					}
				}

				AtomicReference<String> parentAttributeRef = element.getAttributeValueRef("parent");
				if (parentAttributeRef != null) {
					String parentBeanName = parentAttributeRef.get();

					for (ClassNode parentBeanClass = null; parentBeanName != null && parentBeanClass == null; parentBeanName = super.beanParentNames.get(parentBeanName)) {
						parentBeanClass = super.beanClasses.get(parentBeanName);
						if (parentBeanClass != null) {
							return parentBeanClass.getBinaryName();
						}
					}
				}
			}
		}

		return null;
	}

	private ClassNode findAliasBeanClass(String beanName, MultiMap<String, String> referencingBeanNames) {
		List<String> childBeanNames = referencingBeanNames.get(beanName);
		ClassNode resolvedClass = null;
		if (childBeanNames != null) {
			for (String childBeanName : childBeanNames) {
				resolvedClass = super.beanClasses.get(childBeanName);
				if (resolvedClass != null) {
					break;
				}

				resolvedClass = this.findAliasBeanClass(childBeanName, referencingBeanNames);
				if (resolvedClass != null) {
					break;
				}
			}
		}

		return resolvedClass;
	}
}
