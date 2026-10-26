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

public class AndroidManifestXmlHandler extends GenericXmlHandler {
	private String packageName;

	public AndroidManifestXmlHandler(String fullResourcePath, AbstractWorkspace classHierarchy, ClassProvider classProvider, AbstractLogger logger) {
		super(fullResourcePath, classHierarchy, classProvider, logger);
	}

	public AndroidManifestXmlHandler(
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
	public void remapAttributeValues(XmlElement element, List<Object> outputFragments) {
		String tagName = element.getName();
		if (tagName.equalsIgnoreCase("manifest")) {
			AtomicReference<String> packageAttributeRef = element.getAttributeValueRef("package");
			if (packageAttributeRef != null) {
				String declaredPackageName = packageAttributeRef.get();
				if (declaredPackageName != null && declaredPackageName.length() > 0) {
					this.packageName = declaredPackageName;
					packageAttributeRef.set(this.remapPackageName(declaredPackageName));
				}
			}
		} else if (!tagName.equalsIgnoreCase("activity")
			&& !tagName.equalsIgnoreCase("provider")
			&& !tagName.equalsIgnoreCase("service")
			&& !tagName.equalsIgnoreCase("receiver")) {
			Enumeration<String> attributeNames = element.getAttributeNames();

			while (attributeNames.hasMoreElements()) {
				String currentAttributeName = attributeNames.nextElement();
				AtomicReference<String> currentAttributeValueRef = element.getAttributeValueRef(currentAttributeName);
				currentAttributeValueRef.set(this.remapAttributeValue(currentAttributeValueRef.get(), currentAttributeName, false));
			}
		} else {
			AtomicReference<String> nameAttributeRef = element.getAttributeValueRef("android:name");
			if (nameAttributeRef != null) {
				String className = nameAttributeRef.get();
				ClassNode classNode = this.getWorkspaceClassNode(className);
				if (classNode == null && this.packageName != null) {
					classNode = this.getWorkspaceClassNode(this.getQualifiedClassName(className));
				}

				if (classNode != null) {
					nameAttributeRef.set(classNode.getBinaryName());
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
		if (tagName.equalsIgnoreCase("manifest")) {
			AtomicReference<String> packageAttributeRef = element.getAttributeValueRef("package");
			if (packageAttributeRef != null) {
				String declaredPackageName = packageAttributeRef.get();
				if (declaredPackageName != null && declaredPackageName.length() > 0) {
					this.packageName = declaredPackageName;
				}
			}
		} else if (!tagName.equalsIgnoreCase("activity")
			&& !tagName.equalsIgnoreCase("provider")
			&& !tagName.equalsIgnoreCase("service")
			&& !tagName.equalsIgnoreCase("receiver")) {
			super.collectAttributeReferences(element, classReferences, fieldReferences, methodReferences, methodAccessorReferences);
		} else {
			String referenceReason = "reference in '" + super.fullResourcePath + "' by '" + tagName + "' tag";
			AtomicReference<String> nameAttributeRef = element.getAttributeValueRef("android:name");
			if (nameAttributeRef != null) {
				String className = nameAttributeRef.get();
				ClassNode classNode = this.getWorkspaceClassNode(className);
				if (classNode == null && this.packageName != null) {
					classNode = this.getWorkspaceClassNode(this.getQualifiedClassName(className));
				}

				if (classNode != null) {
					classReferences.put(classNode, referenceReason);
				}
			}
		}
	}

	private String getQualifiedClassName(String className) {
		return className.startsWith(".") ? this.packageName + className : this.packageName + "." + className;
	}
}
