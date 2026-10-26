package com.zelix.klassmaster.script.statements.specifier;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.AccessFlags;
import com.zelix.framework.hierarchy.ClassHierarchy;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.annotation.ComplexAnnotationSpecifier;
import com.zelix.klassmaster.script.statements.base.ScriptCommand;
import com.zelix.klassmaster.script.statements.javaccInterface.AnnotationSpecifierAcceptor;
import com.zelix.klassmaster.script.statements.javaccInterface.FieldSpecifierAcceptor;
import com.zelix.klassmaster.script.statements.javaccInterface.MemberMatcher;
import com.zelix.klassmaster.script.statements.javaccInterface.ThrowsExceptionNameAcceptor;
import com.zelix.klassmaster.script.statements.signature.MethodArgumentSpecifier;
import com.zelix.tools.excludes.ExcludeStatementManager;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.StringUtils;
import com.zelix.utils.references.AtomicReference;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MemberSpecifier extends AbstractScriptNode implements FieldSpecifierAcceptor, ThrowsExceptionNameAcceptor, MemberMatcher, AnnotationSpecifierAcceptor {
	private Map<String, String> accessFlagsMap = GlobalUtils.newHashMap(19);
	private List<String> throwsExceptionNames = new ArrayList<>();
	private String visibilityModifier;
	private ComplexAnnotationSpecifier[] parameterAnnotations;
	private String fieldTypeDescriptor;
	private ComplexAnnotationSpecifier memberAnnotationSpecifier;
	private AccessFlags memberAccessFlags;
	private ScriptContext scriptContext;
	private ComplexMethodSpecifier methodSpecifier;
	private MethodArgumentSpecifier methodArgumentSpecifier;
	private ComplexFieldSpecifier fieldSpecifier;

	public MemberSpecifier(int nodeType) {
		super(nodeType);
	}

	@Override
	public void setFieldSpecifier(ComplexFieldSpecifier fieldSpecifier) {
		this.fieldSpecifier = fieldSpecifier;
	}

	public void setMethodArgumentSpecifier(MethodArgumentSpecifier methodArgumentSpecifier) {
		this.methodArgumentSpecifier = methodArgumentSpecifier;
	}

	@Override
	public void setFieldTypeDescriptor(String fieldTypeDescriptor) {
		this.fieldTypeDescriptor = fieldTypeDescriptor;
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		this.scriptContext = scriptContext;
		int childCount = this.getChildrenCount();

		for (int i = 0; i < childCount; i++) {
			this.getChild(i).parseArguments(this, this.scriptContext);
		}

		this.buildAccessFlags();
	}

	private void buildAccessFlags() {
		byte accessFlagsTargetType;
		if (this.fieldSpecifier != null) {
			accessFlagsTargetType = 3;
		} else {
			accessFlagsTargetType = 4;
		}

		this.memberAccessFlags = RenameFilterParameter.parseAccessFlagsFromMap(this.accessFlagsMap, accessFlagsTargetType);
	}

	@Override
	public void addThrowsExceptionName(String exceptionName) {
		this.throwsExceptionNames.add(exceptionName);
	}

	public void setParameterAnnotations(ComplexAnnotationSpecifier[] parameterAnnotations) {
		this.parameterAnnotations = parameterAnnotations;
	}

	@Override
	public String toRuleString() {
		return this.fieldSpecifier != null
			? RenameFilterParameter.buildFieldAnnotationString(this.memberAnnotationSpecifier, this.memberAccessFlags, this.fieldTypeDescriptor, this.fieldSpecifier)
			: RenameFilterParameter.buildMethodAnnotationString(this.memberAnnotationSpecifier, this.memberAccessFlags, this.methodSpecifier, this.methodArgumentSpecifier, this.parameterAnnotations, this.throwsExceptionNames);
	}

	public void setMethodSpecifier(ComplexMethodSpecifier methodSpecifier) {
		this.methodSpecifier = methodSpecifier;
	}

	public void addAccessFlag(String accessFlag) {
		String commandName = this.findScriptCommand().getCommandName();
		int lineNumber = this.findScriptCommand().getLineNumber();
		if (accessFlag.equals("public") || accessFlag.equals("protected") || accessFlag.equals("private") || accessFlag.equals("package")) {
			if (this.visibilityModifier == null) {
				this.visibilityModifier = accessFlag;
			} else if (!this.visibilityModifier.equals(accessFlag)) {
				this.scriptContext
					.logWarningInternal(
						"\"" + accessFlag + "\" appears after \"" + this.visibilityModifier + "\" in \"" + commandName + "\" statement at line " + lineNumber + ". \"" + accessFlag + "\" ignored.", true
					);
				return;
			}
		}

		if (this.accessFlagsMap.put(accessFlag, accessFlag) != null) {
			this.scriptContext.logWarningInternal("\"" + accessFlag + "\" appears more than once in \"" + commandName + "\" statement at line " + lineNumber + ".", true);
		}
	}

	@Override
	public void setAnnotationSpecifier(ComplexAnnotationSpecifier annotationSpecifier) {
		this.memberAnnotationSpecifier = annotationSpecifier;
	}

	@Override
	public final boolean hasMatchingMember(AbstractClassNode classNode, ClassHierarchy classHierarchy) {
		if (this.fieldSpecifier != null) {
			for (AbstractFieldNode fieldNode : classNode.getFields()) {
				if (RenameFilterParameter.matchesAccessFlags(fieldNode.getAccessFlags(), this.memberAccessFlags)
					&& this.fieldSpecifier.matchesWildcard(ExcludeStatementManager.getNameForMatching(fieldNode))
					&& (this.memberAnnotationSpecifier == null || RenameFilterParameter.fieldHasAnnotation(fieldNode, this.memberAnnotationSpecifier, classHierarchy))
					&& (this.fieldTypeDescriptor == null || this.fieldTypeDescriptor.equals(ExcludeStatementManager.getDescriptorForMatching(fieldNode)))) {
					return true;
				}
			}
		} else {
			for (AbstractMethodNode methodNode : classNode.getMethods()) {
				if (RenameFilterParameter.matchesAccessFlags(methodNode.getAccessFlags(), this.memberAccessFlags)
					&& this.methodSpecifier.matchesWildcard(ExcludeStatementManager.getNameForMatching(methodNode))
					&& RenameFilterParameter.matchesMethodDescriptor(ExcludeStatementManager.getDescriptorForMatching(methodNode), this.methodArgumentSpecifier)
					&& (this.memberAnnotationSpecifier == null || RenameFilterParameter.methodHasAnnotation(methodNode, this.memberAnnotationSpecifier, classHierarchy))
					&& RenameFilterParameter.matchesParameterAnnotations(methodNode, this.parameterAnnotations, this.methodArgumentSpecifier, classHierarchy)) {
					AtomicReference<String> errorMessage = new AtomicReference<>();
					boolean matched = RenameFilterParameter.validateThrowsClause(classHierarchy, methodNode, this.throwsExceptionNames, errorMessage);
					if (!errorMessage.isNull()) {
						String errorText = errorMessage.get();
						ScriptCommand scriptCommand = this.findScriptCommand();
						errorText = StringUtils.replace(
							StringUtils.replace(StringUtils.replace(errorText, "<0>", this.toRuleString()), "<1>", scriptCommand.getCommandName()), "<2>", String.valueOf(scriptCommand.getLineNumber())
						);
						this.scriptContext.logWarning(errorText);
					}

					if (matched) {
						return true;
					}
				}
			}
		}

		return false;
	}
}
