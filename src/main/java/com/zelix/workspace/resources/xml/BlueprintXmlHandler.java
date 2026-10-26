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
import com.zelix.utils.dataset.AtomicBool;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.ClassHierarchyNode;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class BlueprintXmlHandler extends BeanPropertyXmlHandler {
	private final Map<String, ClassNode> beanIdToClassNode = GlobalUtils.newHashMap();
	private final Map<String, DeferredAttributeReference> deferredMethodNameReferences = GlobalUtils.newHashMap();

	public BlueprintXmlHandler(String fullResourcePath, AbstractWorkspace classHierarchy, ClassProvider classProvider, AbstractLogger logger) {
		super(fullResourcePath, classHierarchy, classProvider, logger);
	}

	public BlueprintXmlHandler(
		String fullResourcePath,
		ReadOnlyMap<String, String> classNameMapping,
		ReadOnlyRegistryView<AbstractClassNode, FieldSignature, FieldSignature> fieldMappingRegistry,
		ReadOnlyRegistryView<AbstractClassNode, MethodSignature, MethodSignature> methodMappingRegistry,
		PathRemapper resourcePathTransformer,
		AbstractWorkspace classHierarchy,
		ClassProvider classProvider,
		AbstractLogger logger
	) {
		super(fullResourcePath, classNameMapping, fieldMappingRegistry, methodMappingRegistry, resourcePathTransformer, classHierarchy, classProvider, logger);
	}

	@Override
	public void initialize() {
		for (Entry<String, DeferredAttributeReference> entry : this.deferredMethodNameReferences.entrySet()) {
			String methodName = entry.getKey();
			DeferredAttributeReference reference = entry.getValue();
			ClassNode ownerClassNode = this.beanIdToClassNode.get(reference.getBeanReference());
			if (ownerClassNode != null) {
				AtomicReference<String> errorMessage = new AtomicReference<>();
				AtomicBool ambiguous = new AtomicBool();
				String mappedMethodName = this.resolveMappedMethodName(ownerClassNode.getBinaryName(), methodName, 0, errorMessage, ambiguous);
				if (mappedMethodName != null) {
					if (!methodName.equals(mappedMethodName)) {
						reference.getAttributeValueRef().set(mappedMethodName);
					}
				} else if (ambiguous.get()) {
					super.logger
						.error(
							"ERROR:",
							"Could not update method name \""
								+ methodName
								+ "\" in  XML file \""
								+ super.fullResourcePath
								+ "\" : \""
								+ reference.getReferenceReason()
								+ "\" : \""
								+ errorMessage.get()
								+ "\""
						);
				}
			}
		}
	}

	@Override
	public void resolveDeferredReferences(
		Map<FieldNode, String> fieldReferences, Map<MethodNode, String> methodReferences, DualKeyMap<MethodNode, String, Object> methodAccessorReferences
	) {
		for (Entry<String, DeferredAttributeReference> entry : this.deferredMethodNameReferences.entrySet()) {
			String methodName = entry.getKey();
			DeferredAttributeReference reference = entry.getValue();
			ClassNode ownerClassNode = this.beanIdToClassNode.get(reference.getBeanReference());
			if (ownerClassNode != null) {
				this.registerMatchingMethods(
					ownerClassNode,
					methodName,
					null,
					null,
					BeanPropertyXmlHandler.SINGLE_ARG_METHOD_PATTERN,
					0,
					methodReferences,
					methodAccessorReferences,
					reference.getReferenceReason()
				);
			}
		}
	}

	@Override
	public void remapAttributeValues(XmlElement element, List<Object> outputFragments) {
		String tagName = element.getName();
		String parentTagName = this.getTagNameFromStack(1);
		ClassNode classNode = null;
		String binaryClassName = null;
		if (tagName.equalsIgnoreCase("bean")) {
			AtomicReference<String> classNameRef = element.getAttributeValueRef("class");
			if (classNameRef != null) {
				String className = classNameRef.get();
				binaryClassName = this.mapClassName(className);
				if (!className.equals(binaryClassName)) {
					classNameRef.set(binaryClassName);
				}

				if (binaryClassName != null) {
					classNode = ClassHierarchyNode.getWorkspaceClassNode(GlobalUtils.binaryToInternalName(binaryClassName));
				}
			}

			AtomicReference<String> idAttributeRef = element.getAttributeValueRef("id");
			if (idAttributeRef != null) {
				String beanId = idAttributeRef.get();
				if (classNode != null) {
					this.beanIdToClassNode.put(beanId, classNode);
					binaryClassName = classNode.getBinaryName();
				}
			}

			String factoryBeanReference = null;
			AtomicReference<String> factoryRefAttribute = element.getAttributeValueRef("factory-ref");
			if (factoryRefAttribute != null) {
				factoryBeanReference = factoryRefAttribute.get();
				if (classNode == null) {
					classNode = this.beanIdToClassNode.get(factoryBeanReference);
					if (classNode != null) {
						binaryClassName = classNode.getBinaryName();
					}
				}
			}

			Enumeration<String> beanAttributeNames = element.getAttributeNames();

			while (beanAttributeNames.hasMoreElements()) {
				String beanAttributeName = beanAttributeNames.nextElement();
				AtomicReference<String> beanAttributeValueRef = element.getAttributeValueRef(beanAttributeName);
				String existingMethodName = beanAttributeValueRef.get();
				if (!beanAttributeName.equalsIgnoreCase("class") && !beanAttributeName.equalsIgnoreCase("id") && !beanAttributeName.equalsIgnoreCase("factory-ref")) {
					if (beanAttributeName.equalsIgnoreCase("init-method")) {
						MethodSignature initMethodSignature = new MethodSignature(existingMethodName, "()V");
						String mappedInitMethodName = this.getMappedMethodName(binaryClassName, initMethodSignature, new AtomicBool());
						if (!existingMethodName.equals(mappedInitMethodName)) {
							beanAttributeValueRef.set(mappedInitMethodName);
						}
					} else if (beanAttributeName.equalsIgnoreCase("destroy-method")) {
						MethodSignature destroyMethodSignature = new MethodSignature(existingMethodName, "()V");
						String mappedDestroyMethodName = this.getMappedMethodName(binaryClassName, destroyMethodSignature, new AtomicBool());
						if (!existingMethodName.equals(mappedDestroyMethodName)) {
							beanAttributeValueRef.set(mappedDestroyMethodName);
						}
					} else if (beanAttributeName.equalsIgnoreCase("factory-method")) {
						if (binaryClassName != null) {
							AtomicReference<String> errorMessage = new AtomicReference<>();
							AtomicBool ambiguous = new AtomicBool();
							String mappedFactoryMethodName = this.resolveMappedMethodName(binaryClassName, existingMethodName, 0, errorMessage, ambiguous);
							if (mappedFactoryMethodName != null) {
								if (!existingMethodName.equals(mappedFactoryMethodName)) {
									beanAttributeValueRef.set(mappedFactoryMethodName);
								}
							} else if (ambiguous.get()) {
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
											+ existingMethodName
											+ "' : \""
											+ errorMessage.get()
											+ "\""
									);
							}
						} else if (factoryBeanReference != null) {
							String referenceReason = "reference in '" + super.fullResourcePath + "' by 'factory-method' attribute in '" + tagName + "' tag";
							this.deferredMethodNameReferences
								.put(existingMethodName, new DeferredAttributeReference(factoryBeanReference, beanAttributeValueRef, referenceReason));
						}
					}
				}
			}
		} else if (tagName.equalsIgnoreCase("property") && parentTagName != null && parentTagName.equalsIgnoreCase("bean")) {
			AtomicReference<String> nameAttributeRef = element.getAttributeValueRef("name");
			if (nameAttributeRef != null) {
				String propertyName = nameAttributeRef.get();
				String enclosingBeanClassName = this.findEnclosingBeanClassName();
				if (enclosingBeanClassName != null) {
					binaryClassName = this.mapClassName(enclosingBeanClassName);
					this.updateBeanPropertyName(binaryClassName, enclosingBeanClassName, tagName, propertyName, nameAttributeRef);
				}
			}
		} else if (tagName.equalsIgnoreCase("registration-listener")) {
			String referenceReason = "reference in '" + super.fullResourcePath + "' by '" + tagName + "' tag";
			AtomicReference<String> refAttribute = element.getAttributeValueRef("ref");
			if (refAttribute != null) {
				String beanReference = refAttribute.get();
				ClassNode listenerClassNode = this.beanIdToClassNode.get(beanReference);
				AtomicReference<String> registrationMethodRef = element.getAttributeValueRef("registration-method");
				if (registrationMethodRef != null) {
					String registrationMethodName = registrationMethodRef.get();
					if (listenerClassNode != null) {
						this.updateMethodAttribute(element, "registration-method", listenerClassNode.getBinaryName());
					} else {
						this.deferredMethodNameReferences
							.put(registrationMethodName, new DeferredAttributeReference(beanReference, registrationMethodRef, referenceReason));
					}
				}

				AtomicReference<String> unregistrationMethodRef = element.getAttributeValueRef("unregistration-method");
				if (unregistrationMethodRef != null) {
					String unregistrationMethodName = unregistrationMethodRef.get();
					if (listenerClassNode != null) {
						this.updateMethodAttribute(element, "unregistration-method", listenerClassNode.getBinaryName());
					} else {
						this.deferredMethodNameReferences
							.put(unregistrationMethodName, new DeferredAttributeReference(beanReference, unregistrationMethodRef, referenceReason));
					}
				}
			}
		} else if (tagName.equalsIgnoreCase("reference-listener")) {
			String referenceReason = "reference in '" + super.fullResourcePath + "' by '" + tagName + "' tag";
			AtomicReference<String> refAttribute = element.getAttributeValueRef("ref");
			if (refAttribute != null) {
				String beanReference = refAttribute.get();
				ClassNode listenerClassNode = this.beanIdToClassNode.get(beanReference);
				AtomicReference<String> bindMethodRef = element.getAttributeValueRef("bind-method");
				if (bindMethodRef != null) {
					String bindMethodName = bindMethodRef.get();
					if (listenerClassNode != null) {
						this.updateMethodAttribute(element, "bind-method", listenerClassNode.getBinaryName());
					} else {
						this.deferredMethodNameReferences.put(bindMethodName, new DeferredAttributeReference(beanReference, bindMethodRef, referenceReason));
					}
				}

				AtomicReference<String> unbindMethodRef = element.getAttributeValueRef("unbind-method");
				if (unbindMethodRef != null) {
					String unbindMethodName = unbindMethodRef.get();
					if (listenerClassNode != null) {
						this.updateMethodAttribute(element, "unbind-method", listenerClassNode.getBinaryName());
					} else {
						this.deferredMethodNameReferences
							.put(unbindMethodName, new DeferredAttributeReference(beanReference, unbindMethodRef, referenceReason));
					}
				}
			}
		} else {
			Enumeration<String> attributeNames = element.getAttributeNames();

			while (attributeNames.hasMoreElements()) {
				String currentAttributeName = attributeNames.nextElement();
				AtomicReference<String> currentAttributeValueRef = element.getAttributeValueRef(currentAttributeName);
				currentAttributeValueRef.set(this.remapAttributeValue(currentAttributeValueRef.get(), currentAttributeName, false));
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
		if (tagName.equalsIgnoreCase("bean")) {
			String referenceReason = "reference in '" + super.fullResourcePath + "' by '" + tagName + "' tag";
			String binaryClassName = null;
			ClassNode classNode = null;
			AtomicReference<String> classNameRef = element.getAttributeValueRef("class");
			if (classNameRef != null) {
				String className = classNameRef.get();
				binaryClassName = this.findAndRegisterClassName(className, classReferences, referenceReason + " 'class' attribute");
				if (binaryClassName != null) {
					classNode = ClassHierarchyNode.getWorkspaceClassNode(GlobalUtils.binaryToInternalName(binaryClassName));
				}
			}

			AtomicReference<String> idAttributeRef = element.getAttributeValueRef("id");
			if (idAttributeRef != null) {
				String beanId = idAttributeRef.get();
				if (classNode != null) {
					this.beanIdToClassNode.put(beanId, classNode);
				}
			}

			String factoryBeanReference = null;
			AtomicReference<String> factoryRefAttribute = element.getAttributeValueRef("factory-ref");
			if (factoryRefAttribute != null) {
				factoryBeanReference = factoryRefAttribute.get();
				if (classNode == null) {
					classNode = this.beanIdToClassNode.get(factoryBeanReference);
					if (classNode != null) {
						binaryClassName = classNode.getBinaryName();
					}
				}
			}

			Enumeration<String> attributeNames = element.getAttributeNames();

			while (attributeNames.hasMoreElements()) {
				String currentAttributeName = attributeNames.nextElement();
				String existingMethodName = element.getAttributeValueRef(currentAttributeName).get();
				String attributeReferenceDescription = referenceReason + " '" + currentAttributeName + "' attribute";
				if (!currentAttributeName.equalsIgnoreCase("class") && !currentAttributeName.equalsIgnoreCase("id") && !currentAttributeName.equalsIgnoreCase("factory-ref")) {
					if (currentAttributeName.equalsIgnoreCase("init-method")) {
						MethodSignature initMethodSignature = new MethodSignature(existingMethodName, "()V");
						this.registerMethodBySignature(binaryClassName, initMethodSignature, methodReferences, methodAccessorReferences, attributeReferenceDescription);
					} else if (currentAttributeName.equalsIgnoreCase("destroy-method")) {
						MethodSignature destroyMethodSignature = new MethodSignature(existingMethodName, "()V");
						this.registerMethodBySignature(binaryClassName, destroyMethodSignature, methodReferences, methodAccessorReferences, attributeReferenceDescription);
					} else if (currentAttributeName.equalsIgnoreCase("factory-method")) {
						if (classNode != null) {
							this.registerMatchingMethods(classNode, existingMethodName, (String)null, null, BeanPropertyXmlHandler.SINGLE_ARG_METHOD_PATTERN, 0, methodReferences, methodAccessorReferences, referenceReason);
						} else if (factoryBeanReference != null) {
							this.deferredMethodNameReferences.put(existingMethodName, new DeferredAttributeReference(factoryBeanReference, referenceReason));
						}
					}
				}
			}
		} else if (tagName.equalsIgnoreCase("registration-listener")) {
			String referenceReason = "reference in '" + super.fullResourcePath + "' by '" + tagName + "' tag";
			AtomicReference<String> refAttribute = element.getAttributeValueRef("ref");
			if (refAttribute != null) {
				String beanReference = refAttribute.get();
				ClassNode listenerClassNode = this.beanIdToClassNode.get(beanReference);
				AtomicReference<String> registrationMethodRef = element.getAttributeValueRef("registration-method");
				if (registrationMethodRef != null) {
					String registrationMethodName = registrationMethodRef.get();
					if (listenerClassNode != null) {
						this.registerMatchingMethods(
							listenerClassNode,
							registrationMethodName,
							null,
							null,
							BeanPropertyXmlHandler.SINGLE_ARG_METHOD_PATTERN,
							0,
							methodReferences,
							methodAccessorReferences,
							referenceReason
						);
					} else {
						this.deferredMethodNameReferences.put(registrationMethodName, new DeferredAttributeReference(beanReference, referenceReason));
					}
				}

				AtomicReference<String> unregistrationMethodRef = element.getAttributeValueRef("unregistration-method");
				if (unregistrationMethodRef != null) {
					String unregistrationMethodName = unregistrationMethodRef.get();
					if (listenerClassNode != null) {
						this.registerMatchingMethods(
							listenerClassNode,
							unregistrationMethodName,
							null,
							null,
							BeanPropertyXmlHandler.SINGLE_ARG_METHOD_PATTERN,
							0,
							methodReferences,
							methodAccessorReferences,
							referenceReason
						);
					} else {
						this.deferredMethodNameReferences.put(unregistrationMethodName, new DeferredAttributeReference(beanReference, referenceReason));
					}
				}
			}
		} else if (tagName.equalsIgnoreCase("reference-listener")) {
			String referenceReason = "reference in '" + super.fullResourcePath + "' by '" + tagName + "' tag";
			AtomicReference<String> refAttribute = element.getAttributeValueRef("ref");
			if (refAttribute != null) {
				String beanReference = refAttribute.get();
				ClassNode listenerClassNode = this.beanIdToClassNode.get(beanReference);
				AtomicReference<String> bindMethodRef = element.getAttributeValueRef("bind-method");
				if (bindMethodRef != null) {
					String bindMethodName = bindMethodRef.get();
					if (listenerClassNode != null) {
						this.registerMatchingMethods(
							listenerClassNode, bindMethodName, null, null, BeanPropertyXmlHandler.SINGLE_ARG_METHOD_PATTERN, 0, methodReferences, methodAccessorReferences, referenceReason
						);
					} else {
						this.deferredMethodNameReferences.put(bindMethodName, new DeferredAttributeReference(beanReference, referenceReason));
					}
				}

				AtomicReference<String> unbindMethodRef = element.getAttributeValueRef("unbind-method");
				if (unbindMethodRef != null) {
					String unbindMethodName = unbindMethodRef.get();
					if (listenerClassNode != null) {
						this.registerMatchingMethods(
							listenerClassNode, unbindMethodName, null, null, BeanPropertyXmlHandler.SINGLE_ARG_METHOD_PATTERN, 0, methodReferences, methodAccessorReferences, referenceReason
						);
					} else {
						this.deferredMethodNameReferences.put(unbindMethodName, new DeferredAttributeReference(beanReference, referenceReason));
					}
				}
			}
		} else if (tagName.equalsIgnoreCase("service") || tagName.equalsIgnoreCase("reference")) {
			AtomicReference<String> interfaceValueRef = element.getAttributeValueRef("interface");
			if (interfaceValueRef != null) {
				String interfaceName = interfaceValueRef.get();
				String interfaceReferenceDescription = "reference in '" + super.fullResourcePath + "' by '" + tagName + "' tag";
				this.findAndRegisterClassName(interfaceName, classReferences, interfaceReferenceDescription + " 'class' attribute");
			}
		} else if (tagName.equalsIgnoreCase("property") && parentTagName != null && parentTagName.equalsIgnoreCase("bean")) {
			AtomicReference<String> nameAttributeRef = element.getAttributeValueRef("name");
			if (nameAttributeRef != null) {
				String propertyName = nameAttributeRef.get();
				String propertyReferenceDescription = "reference in '" + super.fullResourcePath + "' by '" + tagName + "' tag 'name' attribute";
				String enclosingBeanClassName = this.findEnclosingBeanClassName();
				if (enclosingBeanClassName != null) {
					ClassNode enclosingBeanClassNode = this.getWorkspaceClassNode(enclosingBeanClassName);
					if (enclosingBeanClassNode != null) {
						this.registerPropertyAccessors(enclosingBeanClassNode, propertyName, methodReferences, methodAccessorReferences, propertyReferenceDescription);
					}
				}
			}
		} else if (tagName.equalsIgnoreCase("argument")) {
			String argumentReferenceDescription = "reference in '" + super.fullResourcePath + "' by '" + tagName + "' tag 'argument' attribute";
			String enclosingBeanClassName = this.findEnclosingBeanClassName();
			if (enclosingBeanClassName != null) {
				ClassNode enclosingBeanClassNode = this.getWorkspaceClassNode(enclosingBeanClassName);
				if (enclosingBeanClassNode != null) {
					this.registerMatchingMethods(
						enclosingBeanClassNode, "<init>", null, null, BeanPropertyXmlHandler.SINGLE_ARG_METHOD_PATTERN, 0, methodReferences, methodAccessorReferences, argumentReferenceDescription
					);
				}
			}
		} else {
			super.collectAttributeReferences(element, classReferences, fieldReferences, methodReferences, methodAccessorReferences);
		}
	}

	@Override
	public boolean supportsHierarchy() {
		return false;
	}

	private void updateMethodAttribute(XmlElement element, String attributeName, String className) {
		AtomicReference<String> attributeValueRef = element.getAttributeValueRef(attributeName);
		String existingMethodName = attributeValueRef.get();
		AtomicReference<String> errorMessage = new AtomicReference<>();
		AtomicBool ambiguous = new AtomicBool();
		String mappedMethodName = this.resolveMappedMethodName(className, existingMethodName, 0, errorMessage, ambiguous);
		if (mappedMethodName != null) {
			if (!attributeValueRef.get().equals(mappedMethodName)) {
				attributeValueRef.set(mappedMethodName);
			}
		} else if (ambiguous.get()) {
			super.logger
				.error(
					"ERROR:",
					"Could not update '"
						+ attributeName
						+ "' attribute of tag '"
						+ element.getName()
						+ "' in  XML file '"
						+ super.fullResourcePath
						+ "' : existing value is '"
						+ existingMethodName
						+ "' : \""
						+ errorMessage.get()
						+ "\""
				);
		}
	}

	private String findEnclosingBeanClassName() {
		for (int i = 0; i < super.elementStack.size(); i++) {
			XmlElement element = super.elementStack.peek(i);
			AtomicReference<String> classNameRef;
			if (element.getName().equals("bean") && (classNameRef = element.getAttributeValueRef("class")) != null) {
				return classNameRef.get();
			}
		}

		return null;
	}
}
