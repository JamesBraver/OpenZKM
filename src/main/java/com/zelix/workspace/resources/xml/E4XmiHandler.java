package com.zelix.workspace.resources.xml;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.signature.FieldSignature;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.logging.AbstractLogger;
import com.zelix.repository.ClassProvider;
import com.zelix.utils.PathRemapper;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.readOnly.ReadOnlyMap;
import com.zelix.utils.collections.readOnly.ReadOnlyRegistryView;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.AbstractWorkspace;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

public class E4XmiHandler extends GenericXmlHandler {
	public E4XmiHandler(
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

	public E4XmiHandler(String fullResourcePath, AbstractWorkspace classHierarchy, ClassProvider classProvider, AbstractLogger logger) {
		super(fullResourcePath, classHierarchy, classProvider, logger);
	}

	@Override
	public void remapAttributeValues(XmlElement element, List<Object> outputFragments) {
		element.getName();
		XmlElement parentElement = this.getTagFromStack(0);
		if (parentElement != null) {
			parentElement.getName();
		}

		Enumeration<String> attributeNames = element.getAttributeNames();

		while (attributeNames.hasMoreElements()) {
			String attributeName = attributeNames.nextElement();
			AtomicReference<String> attributeRef = element.getAttributeValueRef(attributeName);
			if (attributeRef != null) {
				String attributeValue = attributeRef.get();
				if (attributeValue.startsWith("bundleclass://")) {
					int classSeparatorIndex = attributeValue.lastIndexOf("/");
					if (classSeparatorIndex > -1 && classSeparatorIndex < attributeValue.length() - 1) {
						String className = attributeValue.substring(classSeparatorIndex + 1);
						ClassNode classNode = this.getWorkspaceClassNode(className);
						if (classNode != null && classNode.isNameChanged()) {
							String bundlePrefix = attributeValue.substring(0, classSeparatorIndex + 1);
							String bundleClassValue = bundlePrefix + classNode.getBinaryName();
							attributeRef.set(bundleClassValue);
						}
					}
				} else if (attributeValue.startsWith("platform:/plugin/")) {
					int pluginSeparatorIndex = attributeValue.indexOf("/", 17);
					if (pluginSeparatorIndex > -1 && pluginSeparatorIndex < attributeValue.length() - 1) {
						attributeValue.substring(0, pluginSeparatorIndex + 1);
						String pluginPath = attributeValue.substring(pluginSeparatorIndex + 1);
						int packageSeparatorIndex = pluginPath.lastIndexOf("/");
						if (packageSeparatorIndex > -1) {
							String packagePath = pluginPath.substring(0, packageSeparatorIndex);
							String fileNamePart = pluginPath.substring(packageSeparatorIndex);
							String remappedPackagePath = this.remapResourcePath(packagePath);
							if (!packagePath.equals(remappedPackagePath)) {
								String pluginClassValue = attributeValue.substring(0, pluginSeparatorIndex + 1) + remappedPackagePath + fileNamePart;
								attributeRef.set(pluginClassValue);
							}
						}
					}
				}
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
		if (parentElement != null) {
			parentElement.getName();
		}

		Enumeration<String> attributeNames = element.getAttributeNames();

		while (attributeNames.hasMoreElements()) {
			String attributeName = attributeNames.nextElement();
			AtomicReference<String> attributeRef = element.getAttributeValueRef(attributeName);
			if (attributeRef != null) {
				String attributeValue = attributeRef.get();
				if (attributeValue.startsWith("bundleclass://")) {
					int classSeparatorIndex = attributeValue.lastIndexOf("/");
					if (classSeparatorIndex > -1 && classSeparatorIndex < attributeValue.length()) {
						String className = attributeValue.substring(classSeparatorIndex + 1);
						ClassNode classNode = this.getWorkspaceClassNode(className);
						if (classNode != null) {
							String referenceContext = "reference in '" + super.fullResourcePath + "' by '" + tagName + "' tag '" + attributeName + "' attribute with value '" + attributeValue + "'";
							classReferences.put(classNode, referenceContext);
						}
					}
				}
			}
		}
	}
}
