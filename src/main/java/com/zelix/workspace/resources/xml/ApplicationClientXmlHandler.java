package com.zelix.workspace.resources.xml;

import com.zelix.exceptions.XMLParseException;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.signature.FieldSignature;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.logging.AbstractLogger;
import com.zelix.repository.ClassProvider;
import com.zelix.utils.PathRemapper;
import com.zelix.utils.StringUtils;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.readOnly.ReadOnlyMap;
import com.zelix.utils.collections.readOnly.ReadOnlyRegistryView;
import com.zelix.workspace.AbstractWorkspace;
import java.util.List;
import java.util.Map;

public class ApplicationClientXmlHandler extends AbstractFrameworkXmlHandler {
	public ApplicationClientXmlHandler(
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

	public ApplicationClientXmlHandler(String fullResourcePath, AbstractWorkspace classHierarchy, ClassProvider classProvider, AbstractLogger logger) {
		super(fullResourcePath, classHierarchy, classProvider, logger);
	}

	@Override
	public void handleCharacterData(String characterData, String tagName, List<Object> outputFragments) {
		if (tagName == null) {
			throw new XMLParseException("Character data outside of tag : '" + StringUtils.escapeJavaString(characterData) + "' (3)");
		} else {
			if (tagName.equals("res-type") || tagName.equals("resource-env-ref-type") || tagName.equals("home") || tagName.equals("remote") || tagName.equals("callback-handler")) {
				String mappedClassName = this.mapClassName(characterData);
				outputFragments.add(mappedClassName);
			} else if (!tagName.equals("small-icon") && !tagName.equals("large-icon")) {
				outputFragments.add(this.remapAttributeValue(characterData, tagName, true));
			} else {
				String remappedResourcePath = super.resourcePathTransformer.remapPathWithCache(characterData);
				outputFragments.add(remappedResourcePath);
			}
		}
	}

	@Override
	public void recordTextReferences(
		String characterData, String tagName, Map<ClassNode, String> classReferences, Map<FieldNode, String> fieldReferences, Map<MethodNode, String> methodReferences, DualKeyMap<MethodNode, String, Object> methodAccessorReferences
	) {
		if (tagName == null) {
			throw new XMLParseException("Character data outside of tag : '" + StringUtils.escapeJavaString(characterData) + "' (3a)");
		} else {
			String referenceReason = "reference in '" + super.fullResourcePath + "' by '" + tagName + "' tag";
			if (tagName.equals("res-type") || tagName.equals("resource-env-ref-type") || tagName.equals("home") || tagName.equals("remote") || tagName.equals("callback-handler")) {
				this.findAndRegisterClassName(characterData, classReferences, referenceReason);
			} else if (!tagName.equals("small-icon") && !tagName.equals("large-icon")) {
				this.processPotentialClassName(characterData, classReferences, referenceReason, tagName, true);
			}
		}
	}
}
