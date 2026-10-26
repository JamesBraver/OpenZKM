package com.zelix.workspace.resources.xml;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.signature.FieldSignature;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.klassmaster.config.Options;
import com.zelix.logging.AbstractLogger;
import com.zelix.repository.ClassProvider;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.PathRemapper;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.readOnly.ReadOnlyMap;
import com.zelix.utils.collections.readOnly.ReadOnlyRegistryView;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.resources.xml.pattern.MaskMethodPattern;
import com.zelix.workspace.resources.xml.pattern.MethodPattern;
import java.util.Map;

public class BeanPropertyXmlHandler extends GenericXmlHandler {
	public static final MethodPattern SINGLE_ARG_METHOD_PATTERN = new MaskMethodPattern("(*)*");
	public static final MethodPattern SETTER_PATTERN = new MaskMethodPattern("(*)V");
	public static final MethodPattern GETTER_PATTERN = new MaskMethodPattern("()*");
	public static final MethodPattern BOOLEAN_GETTER_PATTERN = new MaskMethodPattern("()Z");

	public BeanPropertyXmlHandler(String fullResourcePath, AbstractWorkspace classHierarchy, ClassProvider classProvider, AbstractLogger logger) {
		super(fullResourcePath, classHierarchy, classProvider, logger);
	}

	public BeanPropertyXmlHandler(
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

	public void updateBeanPropertyName(String className, String mappedClassName, String tagName, String propertyName, AtomicReference<String> propertyNameRef) {
		String agreedPropertyName = null;
		String setterName = AbstractFrameworkXmlHandler.formatAccessorName("set", propertyName);
		String mappedSetterName = this.findMappedMethodName(className, setterName, SETTER_PATTERN, 1);
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
							+ mappedClassName
							+ "' (1)"
					);
			} else {
				agreedPropertyName = setterFieldName;
				propertyNameRef.set(setterFieldName);
			}
		}

		String getterName = AbstractFrameworkXmlHandler.formatAccessorName("get", propertyName);
		String mappedGetterName = this.findMappedMethodName(className, getterName, GETTER_PATTERN, 1);
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
							+ mappedClassName
							+ "' (2)"
					);
			} else {
				if (agreedPropertyName == null) {
					agreedPropertyName = getterFieldName;
				} else {
					DebugUtils.assertTrue(
						agreedPropertyName.equals(getterFieldName),
						new String[]{
							"Could not retain bean format for property '"
								+ propertyName
								+ "' in  XML file '"
								+ super.fullResourcePath
								+ "' : Clashing new values '"
								+ agreedPropertyName
								+ "' '"
								+ getterFieldName
								+ "' : '"
								+ mappedClassName
								+ "' (C)"
						}
					);
				}

				propertyNameRef.set(getterFieldName);
			}
		}

		String isAccessorName = AbstractFrameworkXmlHandler.formatAccessorName("is", propertyName);
		MethodPattern isAccessorPattern = Options.requireBooleanIsAccessor ? BOOLEAN_GETTER_PATTERN : GETTER_PATTERN;
		String mappedIsAccessorName = this.findMappedMethodName(className, isAccessorName, isAccessorPattern, 1);
		if (mappedIsAccessorName != null) {
			String isAccessorFieldName = this.getAccessorFieldName("is", mappedIsAccessorName, propertyName);
			if (isAccessorFieldName == null) {
				super.logger
					.error(
						"ERROR:",
						"Could not retain bean format for property '"
							+ propertyName
							+ "' in  XML file '"
							+ super.fullResourcePath
							+ "' : '"
							+ isAccessorName
							+ "' '"
							+ mappedIsAccessorName
							+ "' : '"
							+ mappedClassName
							+ "' (3)"
					);
			} else {
				if (agreedPropertyName != null) {
					DebugUtils.assertTrue(
						agreedPropertyName.equals(isAccessorFieldName),
						new String[]{
							"Could not retain bean format for property '"
								+ propertyName
								+ "' in  XML file '"
								+ super.fullResourcePath
								+ "' : Clashing new values '"
								+ agreedPropertyName
								+ "' '"
								+ isAccessorFieldName
								+ "' : '"
								+ mappedClassName
								+ "' (D)"
						}
					);
				}

				propertyNameRef.set(isAccessorFieldName);
			}
		}
	}

	public void registerPropertyAccessors(ClassNode classNode, String propertyName, Map<MethodNode, String> methodReferences, DualKeyMap<MethodNode, String, Object> methodAccessorReferences, String reason) {
		Object beanPropertyToken = new Object();
		String setterName = AbstractFrameworkXmlHandler.formatAccessorName("set", propertyName);
		this.registerMatchingMethods(classNode, setterName, "set", beanPropertyToken, SETTER_PATTERN, methodReferences, methodAccessorReferences, reason);
		String getterName = AbstractFrameworkXmlHandler.formatAccessorName("get", propertyName);
		this.registerMatchingMethods(classNode, getterName, "get", beanPropertyToken, GETTER_PATTERN, methodReferences, methodAccessorReferences, reason);
		String isAccessorName = AbstractFrameworkXmlHandler.formatAccessorName("is", propertyName);
		MethodPattern isAccessorPattern = Options.requireBooleanIsAccessor ? BOOLEAN_GETTER_PATTERN : GETTER_PATTERN;
		this.registerMatchingMethods(classNode, isAccessorName, "is", beanPropertyToken, isAccessorPattern, methodReferences, methodAccessorReferences, reason);
	}
}
