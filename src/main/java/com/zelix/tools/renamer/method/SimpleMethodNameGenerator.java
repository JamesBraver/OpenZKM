package com.zelix.tools.renamer.method;

import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.signature.AbstractMethodKey;
import com.zelix.framework.signature.MethodParameterKey;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.klassmaster.config.Options;
import com.zelix.tools.excludes.DefaultExcludes;
import com.zelix.tools.parameterChanger.ParameterChangerExcludes;
import com.zelix.tools.renamer.MethodRenamer;
import com.zelix.tools.renamer.naming.NameGenerator;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.MethodNameMap;
import com.zelix.utils.collections.MultiMap;
import com.zelix.workspace.ClassHierarchyNode;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SimpleMethodNameGenerator extends AbstractMethodNameGenerator {
	private boolean useAsciiCharactersOnly;
	private final boolean legalIdentifiers;
	private final String newNamesPrefix;
	private final boolean hideStaticMethodNames;
	private final Map<String, NameGenerator> nameGeneratorsByDescriptor;
	private List<String> overrideNames;
	private ClassHierarchyNode lastProcessedClass;

	public SimpleMethodNameGenerator(
		MethodRenamer methodRenamer,
		DefaultExcludes defaultExcludes,
		ParameterChangerExcludes parameterChangerExcludes,
		boolean useAsciiCharactersOnly,
		boolean legalIdentifiers,
		boolean randomizeObfuscation,
		String newNamesPrefix,
		boolean hideStaticMethodNames,
		List<String> overrideNames
	) {
		super(methodRenamer, defaultExcludes, parameterChangerExcludes, randomizeObfuscation);
		this.useAsciiCharactersOnly = useAsciiCharactersOnly;
		this.legalIdentifiers = legalIdentifiers;
		this.newNamesPrefix = newNamesPrefix;
		this.hideStaticMethodNames = hideStaticMethodNames;
		this.nameGeneratorsByDescriptor = GlobalUtils.newHashMap();
		this.overrideNames = overrideNames;
	}

	@Override
	public String generateMethodName(
		ClassHierarchyNode classHierarchyNode,
		String classInternalName,
		MethodNode methodNode,
		Map<MethodSignature, AbstractMethodNode> processedMethodsBySignature,
		MultiMap<String, String> libraryNamesByDescriptor,
		boolean isInterface,
		Map<MethodSignature, MethodNode> localMethodsBySignature,
		Map<MethodParameterKey, AbstractMethodNode> processedMethodsByParamKey,
		Map<MethodParameterKey, AbstractMethodNode> localMethodsByParamKey,
		MethodNameMap<String, AbstractMethodNode> interfaceMethodNames,
		MethodNameMap<String, AbstractMethodNode> classMethodNames,
		Set<String> usedNames,
		boolean aggressiveRenaming,
		boolean parameterObfuscated
	) {
		MethodSignature methodSignature = methodNode.getMethodSignature();
		methodSignature.getFullDescriptor();
		String descriptor;
		if (parameterObfuscated) {
			descriptor = ParameterChangerExcludes.toObjectArrayDescriptor(methodNode);
		} else {
			descriptor = methodSignature.getFullDescriptor();
		}

		ClassNode classNode = classHierarchyNode.getClassNodeIfWorkspace();
		if (this.hideStaticMethodNames) {
			List<String> libraryNames = libraryNamesByDescriptor.get(descriptor);
			if (libraryNames != null) {
				for (int i = 0; i < libraryNames.size(); i++) {
					String libraryName = libraryNames.get(i);
					MethodSignature candidateSignature = new MethodSignature(libraryName, methodSignature.getFullDescriptor());
					MethodSignature objectArraySignature = new MethodSignature(libraryName, descriptor);
					if (!candidateSignature.equals(methodSignature)
						&& !localMethodsBySignature.containsKey(candidateSignature)
						&& !super.hierarchy.isConflicting(candidateSignature)
						&& !super.hierarchy.hasMethodState(classHierarchyNode, (AbstractMethodKey)(super.hierarchy.usesFullSignature() ? candidateSignature : candidateSignature.getParameterKey()))
						&& (
							!parameterObfuscated
								|| !localMethodsBySignature.containsKey(objectArraySignature)
									&& !super.hierarchy.isConflicting(objectArraySignature)
									&& !super.hierarchy.hasMethodState(classHierarchyNode, (AbstractMethodKey)(super.hierarchy.usesFullSignature() ? objectArraySignature : objectArraySignature.getParameterKey()))
						)) {
						AbstractMethodNode existingMethod = processedMethodsBySignature.get(candidateSignature);
						if (existingMethod != null && (ClassNode)classNode.getAnyReferencingClass(existingMethod) == null) {
							if (parameterObfuscated && !methodNode.getNameDisplay().equals(libraryName)) {
								methodNode.setParameterObfuscationMarked(true);
							}

							return libraryName;
						}
					}
				}
			}
		}

		String generatedName = this.resolveMethodName(classHierarchyNode, classInternalName, methodNode, processedMethodsBySignature, isInterface, localMethodsBySignature, processedMethodsByParamKey, localMethodsByParamKey, interfaceMethodNames, classMethodNames, usedNames, aggressiveRenaming, parameterObfuscated);
		libraryNamesByDescriptor.put(descriptor, generatedName);
		return generatedName;
	}

	@Override
	public String getNextName(ClassHierarchyNode classHierarchyNode, String descriptor) {
		if (classHierarchyNode != this.lastProcessedClass) {
			this.nameGeneratorsByDescriptor.clear();
			this.lastProcessedClass = classHierarchyNode;
		}

		NameGenerator nameGenerator = this.nameGeneratorsByDescriptor.get(descriptor);
		if (nameGenerator == null) {
			if (!this.useAsciiCharactersOnly) {
				nameGenerator = new NameGenerator(NameGenerator.UNICODES, NameGenerator.UNICODES, this.overrideNames, super.randomizeObfuscation);
			} else if (this.legalIdentifiers) {
				if (Options.noUnderscoreInMethodNames) {
					nameGenerator = new NameGenerator(NameGenerator.LETTERS, NameGenerator.LETTERS_NUMBERS, this.overrideNames, super.randomizeObfuscation);
				} else {
					nameGenerator = new NameGenerator(NameGenerator.LETTERS_, NameGenerator.LETTERS_NUMBERS_, this.overrideNames, super.randomizeObfuscation);
				}
			} else {
				nameGenerator = new NameGenerator(NameGenerator.NUMBERS, NameGenerator.LETTERS_NUMBERS_, this.overrideNames, super.randomizeObfuscation);
			}

			this.nameGeneratorsByDescriptor.put(descriptor, nameGenerator);
		}

		return nameGenerator.next(this.newNamesPrefix);
	}
}
