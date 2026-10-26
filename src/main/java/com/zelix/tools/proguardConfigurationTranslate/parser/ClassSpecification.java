package com.zelix.tools.proguardConfigurationTranslate.parser;

import com.zelix.tools.proguardConfigurationTranslate.ProguardNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.BasePatternCommand;
import com.zelix.tools.proguardConfigurationTranslate.base.ProguardNodeBase;
import com.zelix.tools.proguardConfigurationTranslate.javaccInterface.AnnotationPatternConsumer;
import com.zelix.tools.proguardConfigurationTranslate.javaccInterface.ClassPatternConsumer;
import com.zelix.tools.proguardConfigurationTranslate.javaccInterface.ModifierConsumer;
import com.zelix.utils.WildcardMaskPatternUtils;
import java.util.ArrayList;
import java.util.List;

public class ClassSpecification extends ProguardNodeBase implements AnnotationPatternConsumer, ModifierConsumer, ClassPatternConsumer {
	private List<String> classModifiers = new ArrayList<>();
	private List<ClassPattern> classPatterns = new ArrayList<>();
	private List<String> fieldSpecs = new ArrayList<>();
	private List<String> methodSpecs = new ArrayList<>();
	private String extendsAnnotation;
	private String classAnnotation;
	private String implementsAnnotation;
	private String implementsClassName;
	private String extendsClassName;

	public ClassSpecification(int commandId) {
		super(commandId);
	}

	public String getClassModifiers() {
		return this.buildClassModifiers();
	}

	public boolean hasMethodSpecs() {
		return !this.methodSpecs.isEmpty();
	}

	public List<String> getMethodSpecs() {
		return this.methodSpecs;
	}

	public void setExtendsClassName(String className, ProguardScriptTranslator translator) {
		if (this.extendsClassName != null) {
			translator.logWarning("More than one 'extends' clause in class specification : '" + className + "' will replace '" + this.extendsClassName + "'");
		}

		this.extendsClassName = className;
	}

	private String buildClassAnnotation() {
		StringBuilder annotationBuilder = new StringBuilder();
		annotationBuilder.append('@');
		annotationBuilder.append(WildcardMaskPatternUtils.toClassNamePattern(this.classAnnotation));
		annotationBuilder.append(' ');
		return annotationBuilder.toString();
	}

	public List<ClassPattern> getClassPatterns() {
		return this.classPatterns;
	}

	@Override
	public void setAnnotationPattern(String annotationPattern) {
		this.classAnnotation = annotationPattern;
	}

	public boolean hasExtendsOrImplements() {
		return this.extendsClassName != null || this.implementsClassName != null;
	}

	public String getExtendsImplementsClause() {
		return this.buildExtendsImplements();
	}

	public boolean hasFieldSpecs() {
		return !this.fieldSpecs.isEmpty();
	}

	@Override
	public void addModifier(String modifier) {
		this.classModifiers.add(modifier);
	}

	public void setImplementsAnnotation(String annotationPattern, ProguardScriptTranslator translator) {
		if (this.implementsAnnotation != null) {
			translator.logWarning("More than one 'implements' clause in class specification : '" + annotationPattern + "' will replace '" + this.implementsAnnotation + "'");
		}

		this.implementsAnnotation = annotationPattern;
	}

	public void setImplementsClassName(String className) {
		this.implementsClassName = className;
	}

	public void setExtendsAnnotation(String annotationPattern, ProguardScriptTranslator translator) {
		if (this.extendsAnnotation != null) {
			translator.logWarning("More than one 'extends' clause annotation in class specification : '" + annotationPattern + "' will replace '" + this.extendsAnnotation + "'");
		}

		this.extendsAnnotation = annotationPattern;
	}

	public List<String> getFieldSpecs() {
		return this.fieldSpecs;
	}

	public String getClassAnnotation() {
		return this.buildClassAnnotation();
	}

	@Override
	public final void parseArguments(ProguardNode parent, ProguardScriptTranslator scriptTranslator) {
		for (int i = 0; i < this.getChildrenCount(); i++) {
			this.getChild(i).parseArguments(this, scriptTranslator);
		}

		BasePatternCommand classPatternCommand = (BasePatternCommand)this.getParent();
		if (this.classAnnotation != null) {
			classPatternCommand.setAnnotationClause(this.buildClassAnnotation());
		}

		if (this.classModifiers.size() > 0) {
			classPatternCommand.setModifiersClause(this.buildClassModifiers());
		}

		if (this.extendsClassName != null || this.implementsClassName != null) {
			classPatternCommand.setInheritanceClause(this.buildExtendsImplements());
		}

		for (String fieldSpec : this.fieldSpecs) {
			classPatternCommand.addMemberSpec(fieldSpec);
		}

		for (String methodSpec : this.methodSpecs) {
			classPatternCommand.addMemberSpec(methodSpec);
		}

		for (ClassPattern classPattern : this.classPatterns) {
			if (classPattern.isNegated() && !classPattern.isInList()) {
				scriptTranslator.logWarning(
					"ProGuard negated class specification '!"
						+ classPattern.getPattern()
						+ "' is not supported outside of a comma separated list in '"
						+ classPatternCommand.getProguardCommandName()
						+ "' at line "
						+ classPatternCommand.getLineNumber()
						+ "."
				);
			} else {
				String classNamePattern = WildcardMaskPatternUtils.toClassNamePattern(classPattern.getPattern());
				classPattern.setPattern(classNamePattern);
				classPatternCommand.addClassSpec(classPattern);
			}
		}
	}

	private String buildExtendsImplements() {
		StringBuilder clauseBuilder = new StringBuilder();
		if (this.extendsClassName != null) {
			clauseBuilder.append(" extends ");
			if (this.extendsAnnotation != null) {
				clauseBuilder.append("@");
				clauseBuilder.append(WildcardMaskPatternUtils.toClassNamePattern(this.extendsAnnotation));
				clauseBuilder.append(' ');
			}

			clauseBuilder.append(WildcardMaskPatternUtils.toClassNamePattern(this.extendsClassName));
		}

		if (this.implementsClassName != null) {
			clauseBuilder.append(" implements ");
			if (this.implementsAnnotation != null) {
				clauseBuilder.append("@");
				clauseBuilder.append(WildcardMaskPatternUtils.toClassNamePattern(this.implementsAnnotation));
				clauseBuilder.append(' ');
			}

			clauseBuilder.append(WildcardMaskPatternUtils.toClassNamePattern(this.implementsClassName));
		}

		return clauseBuilder.toString();
	}

	public boolean hasClassModifiers() {
		return !this.classModifiers.isEmpty();
	}

	private String buildClassModifiers() {
		StringBuilder modifierBuilder = new StringBuilder();

		for (String modifier : this.classModifiers) {
			modifierBuilder.append(modifier);
			modifierBuilder.append(' ');
		}

		return modifierBuilder.toString();
	}

	public void addFieldSpec(String fieldSpec) {
		this.fieldSpecs.add(fieldSpec);
	}

	public boolean hasClassAnnotation() {
		return this.classAnnotation != null && this.classAnnotation.length() > 0;
	}

	public void addMethodSpec(String methodSpec) {
		this.methodSpecs.add(methodSpec);
	}

	@Override
	public void addClassPattern(ClassPattern classPattern) {
		this.classPatterns.add(classPattern);
	}
}
