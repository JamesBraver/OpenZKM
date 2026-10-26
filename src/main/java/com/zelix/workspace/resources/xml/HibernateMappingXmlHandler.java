package com.zelix.workspace.resources.xml;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.signature.FieldSignature;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.klassmaster.config.Options;
import com.zelix.logging.AbstractLogger;
import com.zelix.repository.ClassProvider;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.PathRemapper;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.readOnly.ReadOnlyMap;
import com.zelix.utils.collections.readOnly.ReadOnlyRegistryView;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.resources.xml.pattern.MethodPattern;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HibernateMappingXmlHandler extends BeanPropertyXmlHandler {
	private static final Set<String> builtinGeneratorNames = GlobalUtils.newHashSet();
	private String currentClassName;
	private String defaultPackage;
	private ClassNode currentClassNode;
	private boolean fieldAccess;

	public HibernateMappingXmlHandler(
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

	public HibernateMappingXmlHandler(String fullResourcePath, AbstractWorkspace classHierarchy, ClassProvider classProvider, AbstractLogger logger) {
		super(fullResourcePath, classHierarchy, classProvider, logger);
	}

	private ClassNode resolveClassAttribute(XmlElement tag, String attributeName) {
		ClassNode classNode = null;
		AtomicReference<String> attributeValueRef = tag.getAttributeValueRef(attributeName);
		if (attributeValueRef != null) {
			String className = attributeValueRef.get();
			classNode = this.getWorkspaceClassNode(className);
			boolean resolvedInDefaultPackage = false;
			if (classNode == null && this.defaultPackage != null) {
				classNode = this.findClassNode(className, this.defaultPackage);
				resolvedInDefaultPackage = classNode != null;
			}

			if (classNode != null) {
				String newAttributeValue;
				if (resolvedInDefaultPackage) {
					newAttributeValue = classNode.getSimpleNameInternal();
				} else {
					newAttributeValue = classNode.getBinaryName();
				}

				attributeValueRef.set(newAttributeValue);
			}
		}

		return classNode;
	}

	private void retainBeanFormatForAttribute(XmlElement tag, String attributeName, ClassNode classNode) {
		AtomicReference<String> attributeValueRef = tag.getAttributeValueRef(attributeName);
		if (attributeValueRef != null) {
			String tagName = tag.getName();
			String propertyName = attributeValueRef.get();
			if (classNode != null) {
				this.retainBeanFormat(tagName, attributeName, propertyName, attributeValueRef, classNode);
			}
		}
	}

	@Override
	public void remapAttributeValues(XmlElement element, List<Object> outputFragments) {
		String tagName = element.getName();
		if (tagName.equalsIgnoreCase("hibernate-mapping")) {
			AtomicReference<String> packageRef = element.getAttributeValueRef("package");
			if (packageRef != null) {
				String packageName = packageRef.get();
				if (packageName != null && packageName.length() > 0) {
					this.defaultPackage = packageName;
					packageRef.set(this.remapPackageName(packageName));
				}
			}

			AtomicReference<String> defaultAccessRef = element.getAttributeValueRef("default-access");
			if (defaultAccessRef != null) {
				String defaultAccess = defaultAccessRef.get();
				if (defaultAccess != null) {
					this.fieldAccess = defaultAccess.equals("field");
				}
			}
		} else if (tagName.equalsIgnoreCase("class")) {
			ClassNode declaredClassNode = this.resolveClassAttribute(element, "name");
			if (declaredClassNode != null) {
				this.currentClassNode = declaredClassNode;
			}

			this.resolveClassAttribute(element, "persister");
			this.resolveClassAttribute(element, "entity-name");
		} else if (tagName.equalsIgnoreCase("id")
			|| tagName.equalsIgnoreCase("property")
			|| tagName.equalsIgnoreCase("many-to-one")
			|| tagName.equalsIgnoreCase("one-to-one")
			|| tagName.equalsIgnoreCase("composite-id")) {
			this.resolveClassAttribute(element, "entity-name");
			AtomicReference<String> nameRef = element.getAttributeValueRef("name");
			if (nameRef != null) {
				String propertyName = nameRef.get();
				if (this.currentClassNode != null) {
					this.retainBeanFormat(tagName, "name", propertyName, nameRef, this.currentClassNode);
				}
			}

			ClassNode referencedClassNode = this.resolveClassAttribute(element, "class");
			this.resolveClassAttribute(element, "type");
			this.resolveClassAttribute(element, "access");
			if ((tagName.equalsIgnoreCase("one-to-one") || tagName.equalsIgnoreCase("many-to-one")) && element.getAttributeValueRef("property-ref") != null) {
				AtomicReference<String> propertyRef = element.getAttributeValueRef("property-ref");
				if (propertyRef != null && !propertyRef.isNull()) {
					String propertyRefName = propertyRef.get();
					if (referencedClassNode != null) {
						this.retainBeanFormat(tagName, "property-ref", propertyRefName, propertyRef, referencedClassNode);
					} else if (element.getAttributeValueRef("name") != null && this.currentClassNode != null) {
						String mappedFieldName = this.getMappedFieldName(this.currentClassNode.getInternalName(), element.getAttributeValueRef("name").get());
						AbstractFieldNode[] fields = this.currentClassNode.findFieldsByName(mappedFieldName);
						if (fields.length == 1) {
							ClassNode propertyTypeClassNode = this.getWorkspaceClassNode(fields[0].getReadableType());
							if (propertyTypeClassNode != null) {
								this.retainBeanFormat(tagName, "property-ref", propertyRefName, propertyRef, propertyTypeClassNode);
							}
						}
					}
				}
			}
		} else if (tagName.equalsIgnoreCase("generator")) {
			this.resolveClassAttribute(element, "class");
			this.resolveClassAttribute(element, "entity-name");
		} else if (tagName.equalsIgnoreCase("one-to-many") || tagName.equalsIgnoreCase("map-key-many-to-many")) {
			this.resolveClassAttribute(element, "class");
			this.resolveClassAttribute(element, "entity-name");
		} else if (tagName.equalsIgnoreCase("many-to-many")) {
			this.resolveClassAttribute(element, "entity-name");
		} else if (!tagName.equalsIgnoreCase("set")
			&& !tagName.equalsIgnoreCase("list")
			&& !tagName.equalsIgnoreCase("map")
			&& !tagName.equalsIgnoreCase("bag")
			&& !tagName.equalsIgnoreCase("array")
			&& !tagName.equalsIgnoreCase("primitive-array")) {
			if (!tagName.equalsIgnoreCase("subclass") && !tagName.equalsIgnoreCase("joined-subclass") && !tagName.equalsIgnoreCase("union-subclass")) {
				Enumeration<String> attributeNames = element.getAttributeNames();

				while (attributeNames.hasMoreElements()) {
					String nextAttributeName = attributeNames.nextElement();
					AtomicReference<String> nextAttributeValueRef = element.getAttributeValueRef(nextAttributeName);
					nextAttributeValueRef.set(this.remapAttributeValue(nextAttributeValueRef.get(), nextAttributeName, false));
				}
			} else {
				this.resolveClassAttribute(element, "name");
				this.resolveClassAttribute(element, "extends");
				this.resolveClassAttribute(element, "entity-name");
				this.resolveClassAttribute(element, "persister");
			}
		} else {
			if (this.currentClassNode != null) {
				this.retainBeanFormatForAttribute(element, "name", this.currentClassNode);
			}

			AtomicReference<String> sortRef = element.getAttributeValueRef("sort");
			if (sortRef != null && !sortRef.isNull() && !sortRef.get().equals("unsorted") && !sortRef.get().equals("natural")) {
				this.resolveClassAttribute(element, "sort");
			}
		}

		outputFragments.add(element);
	}

	@Override
	public void collectAttributeReferences(
		XmlElement element, Map<ClassNode, String> classReferences, Map<FieldNode, String> fieldReferences, Map<MethodNode, String> methodReferences, DualKeyMap<MethodNode, String, Object> methodAccessorReferences
	) {
		String tagName = element.getName();
		if (tagName.equalsIgnoreCase("hibernate-mapping")) {
			AtomicReference<String> packageRef = element.getAttributeValueRef("package");
			if (packageRef != null) {
				String packageName = packageRef.get();
				if (packageName != null && packageName.length() > 0) {
					this.defaultPackage = packageName;
				}
			}

			AtomicReference<String> defaultAccessRef = element.getAttributeValueRef("default-access");
			if (defaultAccessRef != null) {
				String defaultAccess = defaultAccessRef.get();
				if (defaultAccess != null) {
					this.fieldAccess = defaultAccess.equals("field");
				}
			}
		} else if (tagName.equalsIgnoreCase("class")) {
			String nameRefDescription = "reference in '" + super.fullResourcePath + "' by '" + tagName + "' tag 'name' attribute";
			AtomicReference<String> classNameOut = new AtomicReference<>();
			ClassNode declaredClassNode = this.recordClassReference(element, this.defaultPackage, tagName, "name", classNameOut, nameRefDescription, classReferences);
			if (!classNameOut.isNull()) {
				this.currentClassName = classNameOut.get();
			}

			if (declaredClassNode != null) {
				this.currentClassNode = declaredClassNode;
			}

			classNameOut.set(null);
			String persisterRefDescription = "reference in '" + super.fullResourcePath + "' by '" + tagName + "' tag 'persister' attribute";
			this.recordClassReference(element, this.defaultPackage, tagName, "persister", classNameOut, persisterRefDescription, classReferences);
			String classTagEntityNameRefDescription = "reference in '" + super.fullResourcePath + "' by '" + tagName + "' tag 'entity-name' attribute";
			this.recordClassReference(element, this.defaultPackage, tagName, "entity-name", classNameOut, classTagEntityNameRefDescription, classReferences);
		} else if (tagName.equalsIgnoreCase("id")
			|| tagName.equalsIgnoreCase("property")
			|| tagName.equalsIgnoreCase("many-to-one")
			|| tagName.equalsIgnoreCase("one-to-one")
			|| tagName.equalsIgnoreCase("composite-id")) {
			this.registerPropertyReference(element, "name", fieldReferences, methodReferences, methodAccessorReferences);
			ArrayList<ClassNode> referencedClassNodes = new ArrayList<>();
			if (element.getAttributeValueRef("class") != null) {
				String propertyTagClassRefDescription = "reference in '" + super.fullResourcePath + "' by '" + tagName + "' tag 'class' attribute";
				ClassNode classAttributeClassNode = this.recordClassRef(element, this.defaultPackage, tagName, "class", propertyTagClassRefDescription, classReferences);
				if (classAttributeClassNode != null) {
					referencedClassNodes.add(classAttributeClassNode);
				}
			}

			if ((tagName.equalsIgnoreCase("many-to-one") || tagName.equalsIgnoreCase("one-to-one")) && element.getAttributeValueRef("entity-name") != null) {
				String propertyTagEntityNameRefDescription = "reference in '" + super.fullResourcePath + "' by '" + tagName + "' tag 'entity-name' attribute";
				ClassNode entityNameAttributeClassNode = this.recordClassRef(element, this.defaultPackage, tagName, "entity-name", propertyTagEntityNameRefDescription, classReferences);
				if (entityNameAttributeClassNode != null) {
					referencedClassNodes.add(entityNameAttributeClassNode);
				}
			}

			if (!tagName.equalsIgnoreCase("property") && element.getAttributeValueRef("access") != null) {
				String accessRefDescription = "reference in '" + super.fullResourcePath + "' by '" + tagName + "' tag 'access' attribute";
				this.recordClassRef(element, this.defaultPackage, tagName, "access", accessRefDescription, classReferences);
			}

			if (element.getAttributeValueRef("type") != null) {
				String typeRefDescription = "reference in '" + super.fullResourcePath + "' by '" + tagName + "' tag 'type' attribute";
				this.recordClassRef(element, this.defaultPackage, tagName, "type", typeRefDescription, classReferences);
			}

			if ((tagName.equalsIgnoreCase("one-to-one") || tagName.equalsIgnoreCase("many-to-one")) && element.getAttributeValueRef("property-ref") != null) {
				AtomicReference<String> propertyRef = element.getAttributeValueRef("property-ref");
				if (propertyRef != null && !propertyRef.isNull()) {
					String propertyRefName = propertyRef.get();
					String propertyRefDescription = "reference in '" + super.fullResourcePath + "' by '" + tagName + "' tag 'property-ref' attribute";
					if (referencedClassNodes.size() > 0) {
						if (!this.fieldAccess) {
							for (ClassNode classNode : referencedClassNodes) {
								this.registerPropertyAccessors(classNode, propertyRefName, methodReferences, methodAccessorReferences, propertyRefDescription);
							}
						} else {
							for (ClassNode outerClassNode : referencedClassNodes) {
								this.registerFieldsInOuterClasses(outerClassNode, propertyRefName, fieldReferences, propertyRefDescription);
							}
						}
					} else if (element.getAttributeValueRef("name") != null && this.currentClassNode != null) {
						String fieldName = element.getAttributeValueRef("name").get();
						AbstractFieldNode[] fields = this.currentClassNode.findFieldsByName(fieldName);
						if (fields.length == 1) {
							ClassNode propertyTypeClassNode = this.getWorkspaceClassNode(fields[0].getReadableType());
							if (propertyTypeClassNode != null) {
								if (!this.fieldAccess) {
									this.registerPropertyAccessors(propertyTypeClassNode, propertyRefName, methodReferences, methodAccessorReferences, propertyRefDescription);
								} else {
									this.registerFieldsInOuterClasses(propertyTypeClassNode, propertyRefName, fieldReferences, propertyRefDescription);
								}
							}
						}
					}
				}
			}
		} else if (tagName.equalsIgnoreCase("one-to-many") || tagName.equalsIgnoreCase("map-key-many-to-many")) {
			String oneToManyClassRefDescription = "reference in '" + super.fullResourcePath + "' by '" + tagName + "' tag 'class' attribute";
			this.recordClassRef(element, this.defaultPackage, tagName, "class", oneToManyClassRefDescription, classReferences);
			String oneToManyEntityNameRefDescription = "reference in '" + super.fullResourcePath + "' by '" + tagName + "' tag 'entity-name' attribute";
			this.recordClassRef(element, this.defaultPackage, tagName, "entity-name", oneToManyEntityNameRefDescription, classReferences);
		} else if (tagName.equalsIgnoreCase("many-to-many")) {
			AtomicReference<String> entityNameRef = element.getAttributeValueRef("entity-name");
			if (entityNameRef != null && !entityNameRef.isNull()) {
				String manyToManyEntityNameRefDescription = "reference in '" + super.fullResourcePath + "' by '" + tagName + "' tag 'entity-name' attribute";
				this.recordClassRef(element, this.defaultPackage, tagName, "entity-name", manyToManyEntityNameRefDescription, classReferences);
			}
		} else if (tagName.equalsIgnoreCase("generator")) {
			String generatorClassRefDescription = "reference in '" + super.fullResourcePath + "' by '" + tagName + "' tag 'class' attribute";
			AtomicReference<String> classRef = element.getAttributeValueRef("class");
			if (classRef != null) {
				String generatorClassName = classRef.get();
				if (!builtinGeneratorNames.contains(generatorClassName)) {
					this.recordClassRef(element, this.defaultPackage, tagName, "class", generatorClassRefDescription, classReferences);
				}
			}
		} else if (tagName.equalsIgnoreCase("set")
			|| tagName.equalsIgnoreCase("list")
			|| tagName.equalsIgnoreCase("map")
			|| tagName.equalsIgnoreCase("bag")
			|| tagName.equalsIgnoreCase("array")
			|| tagName.equalsIgnoreCase("primitive-array")) {
			this.registerPropertyReference(element, "name", fieldReferences, methodReferences, methodAccessorReferences);
			AtomicReference<String> sortRef = element.getAttributeValueRef("sort");
			if (sortRef != null && !sortRef.isNull() && !sortRef.get().equals("unsorted") && !sortRef.get().equals("natural")) {
				String sortRefDescription = "reference in '" + super.fullResourcePath + "' by '" + tagName + "' tag 'sort' attribute";
				this.recordClassRef(element, this.defaultPackage, tagName, "sort", sortRefDescription, classReferences);
			}
		} else if (!tagName.equalsIgnoreCase("subclass") && !tagName.equalsIgnoreCase("joined-subclass") && !tagName.equalsIgnoreCase("union-subclass")) {
			super.collectAttributeReferences(element, classReferences, fieldReferences, methodReferences, methodAccessorReferences);
		} else {
			this.recordClassAttribute(element, this.defaultPackage, "name", classReferences);
			this.recordClassAttribute(element, this.defaultPackage, "extends", classReferences);
			this.recordClassAttribute(element, this.defaultPackage, "entity-name", classReferences);
			this.recordClassAttribute(element, this.defaultPackage, "persister", classReferences);
		}
	}

	private ClassNode findClassNode(String className, String packageName) {
		ClassNode classNode = this.getWorkspaceClassNode(className);
		if (classNode == null && packageName != null) {
			classNode = this.getWorkspaceClassNode(packageName + "." + className);
		}

		return classNode;
	}

	private ClassNode recordClassAttribute(XmlElement tag, String packageName, String attributeName, Map<ClassNode, String> classReferences) {
		if (tag.getAttributeValueRef(attributeName) != null) {
			String tagName = tag.getName();
			String refDescription = "reference in '" + super.fullResourcePath + "' by '" + tagName + "' tag '" + attributeName + "' attribute";
			return this.recordClassRef(tag, packageName, tagName, attributeName, refDescription, classReferences);
		} else {
			return null;
		}
	}

	private void retainBeanFormat(String tagName, String attributeName, String propertyName, AtomicReference<String> valueRef, ClassNode classNode) {
		String binaryName = classNode.getBinaryName();
		if (!this.fieldAccess) {
			String resolvedFieldName = null;
			String setterName = AbstractFrameworkXmlHandler.formatAccessorName("set", propertyName);
			String mappedSetterName = this.findMappedMethodName(binaryName, setterName, BeanPropertyXmlHandler.SETTER_PATTERN, 1);
			if (mappedSetterName == null && tagName.equalsIgnoreCase("id")) {
				mappedSetterName = this.findMappedMethodName(binaryName, setterName, BeanPropertyXmlHandler.SETTER_PATTERN, 2);
			}

			if (mappedSetterName != null) {
				String setterFieldName = this.getAccessorFieldName("set", mappedSetterName, propertyName);
				if (setterFieldName == null) {
					super.logger
						.error(
							"ERROR:",
							"Could not retain bean format for property '"
								+ propertyName
								+ "' in  XML file '"
								+ super.fullResourcePath
								+ "' : '"
								+ setterName
								+ "' '"
								+ mappedSetterName
								+ "' : '"
								+ classNode.getSourcePath()
								+ "' (4)"
						);
				} else {
					resolvedFieldName = setterFieldName;
					valueRef.set(setterFieldName);
				}
			}

			String getterName = AbstractFrameworkXmlHandler.formatAccessorName("get", propertyName);
			String mappedGetterName = this.findMappedMethodName(binaryName, getterName, BeanPropertyXmlHandler.GETTER_PATTERN, 1);
			if (mappedGetterName != null) {
				String getterFieldName = this.getAccessorFieldName("get", mappedGetterName, propertyName);
				if (getterFieldName == null) {
					super.logger
						.error(
							"ERROR:",
							"Could not retain bean format for property '"
								+ propertyName
								+ "' in  XML file '"
								+ super.fullResourcePath
								+ "' : '"
								+ getterName
								+ "' '"
								+ mappedGetterName
								+ "' : '"
								+ classNode.getSourcePath()
								+ "' (5)"
						);
				} else {
					if (resolvedFieldName == null) {
						resolvedFieldName = getterFieldName;
					} else {
						DebugUtils.assertTrue(
							resolvedFieldName.equals(getterFieldName),
							new String[]{
								"Could not retain bean format for property '"
									+ propertyName
									+ "' in  XML file '"
									+ super.fullResourcePath
									+ "' : Clashing new values '"
									+ resolvedFieldName
									+ "' '"
									+ getterFieldName
									+ "' : '"
									+ classNode.getSourcePath()
									+ "' (A)"
							}
						);
					}

					valueRef.set(getterFieldName);
				}
			}

			String booleanGetterName = AbstractFrameworkXmlHandler.formatAccessorName("is", propertyName);
			MethodPattern booleanAccessorPattern = Options.requireBooleanIsAccessor ? BeanPropertyXmlHandler.BOOLEAN_GETTER_PATTERN : BeanPropertyXmlHandler.GETTER_PATTERN;
			String mappedBooleanGetterName = this.findMappedMethodName(binaryName, booleanGetterName, booleanAccessorPattern, 1);
			if (mappedBooleanGetterName != null) {
				String booleanGetterFieldName = this.getAccessorFieldName("is", mappedBooleanGetterName, propertyName);
				if (booleanGetterFieldName == null) {
					super.logger
						.error(
							"ERROR:",
							"Could not retain bean format for property '"
								+ propertyName
								+ "' in  XML file '"
								+ super.fullResourcePath
								+ "' : '"
								+ booleanGetterName
								+ "' '"
								+ mappedBooleanGetterName
								+ "' : '"
								+ classNode.getSourcePath()
								+ "' (6)"
						);
				} else {
					if (resolvedFieldName != null) {
						DebugUtils.assertTrue(
							resolvedFieldName.equals(booleanGetterFieldName),
							new String[]{
								"Could not retain bean format for property '"
									+ propertyName
									+ "' in  XML file '"
									+ super.fullResourcePath
									+ "' : Clashing new values '"
									+ resolvedFieldName
									+ "' '"
									+ booleanGetterFieldName
									+ "' : '"
									+ classNode.getSourcePath()
									+ "' (B)"
							}
						);
					}

					valueRef.set(booleanGetterFieldName);
				}
			}
		} else {
			String mappedFieldName = this.getMappedFieldName(binaryName, propertyName);
			valueRef.set(mappedFieldName);
		}
	}

	private void registerPropertyReference(XmlElement tag, String attributeName, Map<FieldNode, String> fieldReferences, Map<MethodNode, String> methodReferences, DualKeyMap<MethodNode, String, Object> accessorReferences) {
		AtomicReference<String> attributeValueRef = tag.getAttributeValueRef(attributeName);
		if (attributeValueRef != null) {
			String propertyName = attributeValueRef.get();
			String tagName = tag.getName();
			if (this.currentClassNode != null) {
				String refDescription = "reference in '" + super.fullResourcePath + "' by '" + tagName + "' tag '" + attributeName + "' attribute";
				if (!this.fieldAccess) {
					this.registerPropertyAccessors(this.currentClassNode, propertyName, methodReferences, accessorReferences, refDescription);
				} else {
					this.registerFieldsInOuterClasses(this.currentClassNode, propertyName, fieldReferences, refDescription);
				}
			}
		}
	}

	private ClassNode recordClassRef(XmlElement tag, String packageName, String tagName, String attributeName, String refDesc, Map<ClassNode, String> classReferences) {
		return this.recordClassReference(tag, packageName, tagName, attributeName, null, refDesc, classReferences);
	}

	private ClassNode recordClassReference(XmlElement tag, String packageName, String tagName, String attributeName, AtomicReference<String> valueOut, String refDesc, Map<ClassNode, String> classReferences) {
		AtomicReference<String> attributeValueRef = tag.getAttributeValueRef(attributeName);
		ClassNode classNode = null;
		if (attributeValueRef != null) {
			String className = attributeValueRef.get();
			if (valueOut != null) {
				valueOut.set(className);
			}

			classNode = this.getWorkspaceClassNode(className);
			if (classNode == null && packageName != null) {
				classNode = this.findClassNode(className, packageName);
			}

			if (classNode != null) {
				classReferences.put(classNode, refDesc);
			}
		}

		return classNode;
	}

	static {
		builtinGeneratorNames.add("increment");
		builtinGeneratorNames.add("identity");
		builtinGeneratorNames.add("sequence");
		builtinGeneratorNames.add("hilo");
		builtinGeneratorNames.add("seqhilo");
		builtinGeneratorNames.add("uuid");
		builtinGeneratorNames.add("guid");
		builtinGeneratorNames.add("native");
		builtinGeneratorNames.add("assigned");
		builtinGeneratorNames.add("select");
		builtinGeneratorNames.add("foreign");
		builtinGeneratorNames.add("sequence-identity");
	}
}
