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

public class EjbJarXmlHandler extends AbstractFrameworkXmlHandler {
	private String ejbClassName;

	public EjbJarXmlHandler(String fullResourcePath, AbstractWorkspace classHierarchy, ClassProvider classProvider, AbstractLogger logger) {
		super(fullResourcePath, classHierarchy, classProvider, logger);
	}

	public EjbJarXmlHandler(
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
	protected void onEndTag(String tagName, XmlElement element) {
		if (tagName.equals("entity") || tagName.equals("session") || tagName.equals("message-driven")) {
			this.ejbClassName = null;
		}
	}

	@Override
	public void handleCharacterData(String characterData, String tagName, List<Object> outputFragments) {
		if (tagName == null) {
			throw new XMLParseException("Character data outside of tag : '" + StringUtils.escapeJavaString(characterData) + "' (4)");
		} else {
			if (tagName.equals("ejb-class")
				|| tagName.equals("home")
				|| tagName.equals("remote")
				|| tagName.equals("local")
				|| tagName.equals("local-home")
				|| tagName.equals("prim-key-class")
				|| tagName.equals("res-type")
				|| tagName.equals("dependent-class")) {
				String mappedClassName = this.mapClassName(characterData);
				if (tagName.equals("ejb-class")) {
					this.ejbClassName = mappedClassName;
				}

				outputFragments.add(mappedClassName);
			} else if (tagName.equals("method-param")) {
				outputFragments.add(this.mapTypeName(characterData));
			} else if (tagName.equals("small-icon") || tagName.equals("large-icon")) {
				String remappedResourcePath = super.resourcePathTransformer.remapPathWithCache(characterData);
				outputFragments.add(remappedResourcePath);
			} else if (!tagName.equals("field-name") && !tagName.equals("primkey-field")) {
				outputFragments.add(this.remapAttributeValue(characterData, tagName, true));
			} else if ((this.isTagOnStack("entity") || this.isTagOnStack("session") || this.isTagOnStack("message-driven")) && this.ejbClassName != null) {
				outputFragments.add(this.getMappedFieldName(this.ejbClassName, characterData));
			} else {
				outputFragments.add(characterData);
			}
		}
	}

	@Override
	public void recordTextReferences(
		String characterData, String tagName, Map<ClassNode, String> classReferences, Map<FieldNode, String> fieldReferences, Map<MethodNode, String> methodReferences, DualKeyMap<MethodNode, String, Object> methodAccessorReferences
	) {
		if (tagName == null) {
			throw new XMLParseException("Character data outside of tag : '" + StringUtils.escapeJavaString(characterData) + "' (4a)");
		} else {
			String referenceReason = "reference in '" + super.fullResourcePath + "' by '" + tagName + "' tag";
			if (tagName.equals("ejb-class")
				|| tagName.equals("home")
				|| tagName.equals("remote")
				|| tagName.equals("local")
				|| tagName.equals("local-home")
				|| tagName.equals("prim-key-class")
				|| tagName.equals("res-type")
				|| tagName.equals("dependent-class")) {
				String mappedClassName = this.findAndRegisterClassName(characterData, classReferences, referenceReason);
				if (tagName.equals("ejb-class")) {
					this.ejbClassName = mappedClassName;
				}
			} else if (tagName.equals("method-param")) {
				this.registerTypeReference(characterData, classReferences, referenceReason);
			} else if (!tagName.equals("small-icon") && !tagName.equals("large-icon")) {
				if (!tagName.equals("field-name") && !tagName.equals("primkey-field")) {
					this.processPotentialClassName(characterData, classReferences, referenceReason, tagName, true);
				} else if ((this.isTagOnStack("entity") || this.isTagOnStack("session") || this.isTagOnStack("message-driven")) && this.ejbClassName != null) {
					this.registerFieldsInOuterClasses(this.ejbClassName, characterData, fieldReferences, referenceReason);
				}
			}
		}
	}
}
