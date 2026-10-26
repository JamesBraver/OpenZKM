package com.zelix.tools.proguardConfigurationTranslate.parser;

import com.zelix.tools.proguardConfigurationTranslate.ProguardNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.ProguardNodeBase;
import com.zelix.tools.proguardConfigurationTranslate.javaccInterface.AnnotationPatternConsumer;
import com.zelix.tools.proguardConfigurationTranslate.javaccInterface.ModifierConsumer;
import com.zelix.tools.proguardConfigurationTranslate.javaccInterface.NameConsumer;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.WildcardMaskPatternUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MemberSpecificationNode extends ProguardNodeBase implements AnnotationPatternConsumer, ModifierConsumer, NameConsumer {
	public static final Map<String, String> VISIBILITY_NEGATIONS = GlobalUtils.newHashMap();
	public static final Set<String> NEGATED_VISIBILITIES = GlobalUtils.newHashSet();
	private boolean isFieldSpec = false;
	private boolean isMethodSpec = false;
	private List<String> modifiers = new ArrayList<>();
	private List<String> parameterTypes;
	private String typePattern;
	private String annotationPattern;
	private String namePattern;

	public MemberSpecificationNode(int commandId) {
		super(commandId);
	}

	public void markAsMethodSpec() {
		this.isMethodSpec = true;
	}

	@Override
	public void setName(String name) {
		this.namePattern = name;
	}

	public void markAsFieldSpec() {
		this.isFieldSpec = true;
	}

	@Override
	public void setAnnotationPattern(String annotationPattern) {
		this.annotationPattern = annotationPattern;
	}

	@Override
	public void addModifier(String modifier) {
		this.modifiers.add(modifier);
	}

	private List<String> expandVisibilityModifiers(List<String> modifiers) {
		List<String> expandedModifiers = modifiers;
		HashSet<String> specifiedVisibilities = GlobalUtils.newHashSet();
		HashSet<String> specifiedNegations = GlobalUtils.newHashSet();
		HashSet<String> otherModifiers = GlobalUtils.newHashSet();

		for (String modifier : modifiers) {
			if (VISIBILITY_NEGATIONS.containsKey(modifier)) {
				specifiedVisibilities.add(modifier);
			} else if (NEGATED_VISIBILITIES.contains(modifier)) {
				specifiedNegations.add(modifier);
			} else {
				otherModifiers.add(modifier);
			}
		}

		if (specifiedVisibilities.size() > 1) {
			expandedModifiers = new ArrayList<>();

			for (String visibility : VISIBILITY_NEGATIONS.keySet()) {
				if (!specifiedVisibilities.contains(visibility)) {
					String negatedVisibility = VISIBILITY_NEGATIONS.get(visibility);
					if (!specifiedNegations.contains(negatedVisibility)) {
						expandedModifiers.add(negatedVisibility);
					}
				}
			}

			for (String negation : specifiedNegations) {
				expandedModifiers.add(negation);
			}

			for (String otherModifier : otherModifiers) {
				expandedModifiers.add(otherModifier);
			}
		}

		return expandedModifiers;
	}

	public void setParameterTypes(List<String> parameterTypes) {
		this.parameterTypes = parameterTypes;
	}

	public void setTypePattern(String typePattern) {
		this.typePattern = typePattern;
	}

	@Override
	public final void parseArguments(ProguardNode parent, ProguardScriptTranslator scriptTranslator) {
		for (int i = 0; i < this.getChildrenCount(); i++) {
			this.getChild(i).parseArguments(this, scriptTranslator);
		}

		ClassSpecification classSpecification = (ClassSpecification)this.getParent();
		StringBuilder specBuilder = new StringBuilder();
		if (this.annotationPattern != null) {
			specBuilder.append('@');
			specBuilder.append(WildcardMaskPatternUtils.toClassNamePattern(this.annotationPattern));
			specBuilder.append(' ');
		}

		this.modifiers = this.expandVisibilityModifiers(this.modifiers);

		for (String modifier : this.modifiers) {
			specBuilder.append(modifier);
			specBuilder.append(' ');
		}

		if (this.typePattern != null && this.typePattern.indexOf("*") == -1 && this.typePattern.indexOf("?") == -1 && this.typePattern.indexOf("%") == -1 && this.typePattern.indexOf(60) == -1) {
			specBuilder.append(this.typePattern);
			specBuilder.append(' ');
		}

		if (this.isFieldSpec) {
			specBuilder.append(WildcardMaskPatternUtils.toFieldNamePattern(this.namePattern));
		} else {
			specBuilder.append(WildcardMaskPatternUtils.toMethodNamePattern(this.namePattern));
		}

		if (this.isFieldSpec) {
			classSpecification.addFieldSpec(specBuilder.toString());
		}

		if (this.isMethodSpec) {
			specBuilder.append('(');

			for (int parameterIndex = 0; parameterIndex < this.parameterTypes.size(); parameterIndex++) {
				specBuilder.append(WildcardMaskPatternUtils.toParameterTypePattern(this.parameterTypes.get(parameterIndex)));
				if (parameterIndex < this.parameterTypes.size() - 1) {
					specBuilder.append(',');
				}
			}

			specBuilder.append(')');
			classSpecification.addMethodSpec(specBuilder.toString());
		}
	}

	static {
		VISIBILITY_NEGATIONS.put("public", "!public");
		VISIBILITY_NEGATIONS.put("protected", "!protected");
		VISIBILITY_NEGATIONS.put("package", "!package");
		VISIBILITY_NEGATIONS.put("private", "!private");
		NEGATED_VISIBILITIES.add("!public");
		NEGATED_VISIBILITIES.add("!protected");
		NEGATED_VISIBILITIES.add("!package");
		NEGATED_VISIBILITIES.add("!private");
	}
}
