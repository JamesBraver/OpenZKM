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
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

public class IBatisSqlMapXmlHandler extends BeanPropertyXmlHandler {
	private Map<String, String> typeAliasMap = GlobalUtils.newHashMap();

	public IBatisSqlMapXmlHandler(
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

	public IBatisSqlMapXmlHandler(String fullResourcePath, AbstractWorkspace classHierarchy, ClassProvider classProvider, AbstractLogger logger) {
		super(fullResourcePath, classHierarchy, classProvider, logger);
	}

	@Override
	public void remapAttributeValues(XmlElement element, List<Object> outputFragments) {
		String tagName = element.getName();
		String parentTagName = null;
		XmlElement parentElement = this.getTagFromStack(0);
		if (parentElement != null) {
			parentTagName = parentElement.getName();
		}

		if (tagName.equalsIgnoreCase("typeAlias")) {
			AtomicReference<String> aliasRef = element.getAttributeValueRef("alias");
			if (aliasRef != null) {
				String aliasName = aliasRef.get();
				if (aliasName != null && aliasName.length() > 0) {
					AtomicReference<String> typeRef = element.getAttributeValueRef("type");
					if (typeRef != null) {
						String typeName = typeRef.get();
						if (typeName != null && typeName.length() > 0) {
							this.typeAliasMap.put(aliasName.trim(), typeName.trim());
							typeRef.set(this.mapClassName(typeName));
						}
					}
				}
			}
		} else if (tagName.equalsIgnoreCase("property") && parentTagName != null && parentTagName.equalsIgnoreCase("result-map")
			|| tagName.equalsIgnoreCase("result") && parentTagName != null && parentTagName.equalsIgnoreCase("resultMap")) {
			AtomicReference<String> classAttributeRef = null;
			String mappedTypeName = null;
			String remappedClassName = null;
			if (parentElement != null) {
				classAttributeRef = parentElement.getAttributeValueRef("class");
			}

			if (classAttributeRef != null) {
				mappedTypeName = GlobalUtils.getMappedOrDefault(classAttributeRef.get(), this.typeAliasMap);
				remappedClassName = this.mapClassName(mappedTypeName);
			}

			if (remappedClassName != null) {
				Enumeration<String> attributeNames = element.getAttributeNames();

				while (attributeNames.hasMoreElements()) {
					String attributeName = attributeNames.nextElement();
					String discardedAttributeValue = element.getAttributeValueRef(attributeName).get();
					if (attributeName.equalsIgnoreCase("name") && tagName.equalsIgnoreCase("property") || attributeName.equalsIgnoreCase("property") && tagName.equalsIgnoreCase("result")) {
						AtomicReference<String> attributeRef = element.getAttributeValueRef(attributeName);
						if (attributeRef != null) {
							String attributeValue = attributeRef.get();
							this.updateBeanPropertyName(remappedClassName, mappedTypeName, tagName, attributeValue, attributeRef);
						}
					}
				}
			}
		} else {
			Enumeration<String> remappedAttributeNames = element.getAttributeNames();

			while (remappedAttributeNames.hasMoreElements()) {
				String remappedAttributeName = remappedAttributeNames.nextElement();
				AtomicReference<String> remappedAttributeRef = element.getAttributeValueRef(remappedAttributeName);
				remappedAttributeRef.set(this.remapAttributeValue(remappedAttributeRef.get(), remappedAttributeName, false));
			}
		}

		outputFragments.add(element);
	}

	@Override
	public void collectAttributeReferences(
		XmlElement element, Map<ClassNode, String> classReferences, Map<FieldNode, String> fieldReferences, Map<MethodNode, String> methodReferences, DualKeyMap<MethodNode, String, Object> methodAccessorReferences
	) {
		String tagName = element.getName();
		XmlElement parentElement = this.getTagFromStack(0);
		String parentTagName = null;
		if (parentElement != null) {
			parentTagName = parentElement.getName();
		}

		if (tagName.equalsIgnoreCase("typeAlias")) {
			AtomicReference<String> aliasRef = element.getAttributeValueRef("alias");
			if (aliasRef != null) {
				String aliasName = aliasRef.get();
				if (aliasName != null && aliasName.length() > 0) {
					AtomicReference<String> typeRef = element.getAttributeValueRef("type");
					if (typeRef != null) {
						String typeName = typeRef.get();
						if (typeName != null && typeName.length() > 0) {
							this.typeAliasMap.put(aliasName.trim(), typeName.trim());
						}
					}
				}
			}
		} else if (tagName.equalsIgnoreCase("property") && parentTagName != null && parentTagName.equalsIgnoreCase("result-map")
			|| tagName.equalsIgnoreCase("result") && parentTagName != null && parentTagName.equalsIgnoreCase("resultMap")) {
			String referenceContext = "reference in '" + super.fullResourcePath + "' by '" + tagName + "' tag";
			String className = null;
			AtomicReference<String> classAttributeRef = null;
			if (parentElement != null) {
				classAttributeRef = parentElement.getAttributeValueRef("class");
			}

			if (classAttributeRef != null) {
				String mappedTypeName = GlobalUtils.getMappedOrDefault(classAttributeRef.get(), this.typeAliasMap);
				className = this.findAndRegisterClassName(mappedTypeName, classReferences, referenceContext + " 'class' attribute");
			}

			if (className != null) {
				Enumeration<String> attributeNames = element.getAttributeNames();

				while (attributeNames.hasMoreElements()) {
					String attributeName = attributeNames.nextElement();
					String discardedAttributeValue = element.getAttributeValueRef(attributeName).get();
					String attributeContext = referenceContext + " '" + attributeName + "' attribute";
					if (attributeName.equalsIgnoreCase("name") && tagName.equalsIgnoreCase("property") || attributeName.equalsIgnoreCase("property") && tagName.equalsIgnoreCase("result")) {
						AtomicReference<String> attributeRef = element.getAttributeValueRef(attributeName);
						if (attributeRef != null) {
							String attributeValue = attributeRef.get();
							ClassNode classNode = this.getWorkspaceClassNode(className);
							if (classNode != null) {
								this.registerPropertyAccessors(classNode, attributeValue, methodReferences, methodAccessorReferences, attributeContext);
							}
						}
					}
				}
			}
		} else {
			super.collectAttributeReferences(element, classReferences, fieldReferences, methodReferences, methodAccessorReferences);
		}
	}
}
