package com.zelix.tools.parameterChanger.dataset;

import com.zelix.framework.hierarchy.Inheritance;
import com.zelix.framework.instructions.AbstractInsnNode;
import com.zelix.framework.instructions.WideConstantPoolInsnNode;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.FieldTypeWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.repository.ClassProvider;
import com.zelix.tools.parameterChanger.ParameterChangerBuilder;
import com.zelix.tools.parameterChanger.salt.SaltEntry;
import com.zelix.workspace.AbstractWorkspace;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.zelix.framework.instructions.OpcodeConstants.PUTSTATIC;

public class ChangedMethod {
	private final List<ChangedMethod> dependentMethods = new ArrayList<>();
	private final List<ChangedMethod> falseDependents = new ArrayList<>();
	private final MethodNode methodNode;
	private final boolean usesStaticSaltField;
	private final boolean sharesSaltField;
	private FieldTypeWriter saltFieldRef;
	private ChangedMethod falseInitDependency;
	private long randomSaltValue;
	private List<AbstractInsnNode> decryptionInstructions;
	private int maxStack;
	private SaltEntry keyEntry;
	private long valueMidBits;
	private ChangedMethod initDependency;
	private long methodSalt;
	private long saltSeed;
	private long methodValue;
	private SaltEntry valueEntry;
	private Long previousSaltValue;

	public ChangedMethod(MethodNode methodNode) {
		this(methodNode, false, false);
	}

	public ChangedMethod(MethodNode methodNode, boolean usesStaticSaltField, boolean sharesSaltField) {
		this.methodNode = methodNode;
		this.usesStaticSaltField = usesStaticSaltField;
		this.sharesSaltField = sharesSaltField;
	}

	public void setMethodValue(long methodValue) {
		this.previousSaltValue = this.methodValue;
		this.methodValue = methodValue;
	}

	public ChangedMethod getInitDependency() {
		return this.initDependency;
	}

	public int getMaxStack() {
		return this.maxStack;
	}

	public SaltEntry getValueEntry() {
		return this.valueEntry;
	}

	public boolean hasFalseDependents() {
		return !this.falseDependents.isEmpty();
	}

	public void setDecryptionInstructions(List<AbstractInsnNode> decryptionInstructions) {
		this.decryptionInstructions = decryptionInstructions;
	}

	public boolean usesStaticSaltField() {
		return this.usesStaticSaltField;
	}

	public long getKeySaltValue() {
		return this.keyEntry.getSaltValue();
	}

	public ChangedMethod getFalseInitDependency() {
		return this.falseInitDependency;
	}

	public long getMethodSalt() {
		return this.methodSalt;
	}

	public FieldTypeWriter getSaltFieldRef() {
		return this.saltFieldRef;
	}

	public long getMethodValue() {
		return this.methodValue;
	}

	public SaltEntry getKeyEntry() {
		return this.keyEntry;
	}

	public void setSaltSeed(long saltSeed) {
		this.saltSeed = saltSeed;
	}

	public List<ChangedMethod> getDependentMethods() {
		return new ArrayList<>(this.dependentMethods);
	}

	public List<AbstractInsnNode> getDecryptionInstructions() {
		return Collections.unmodifiableList(this.decryptionInstructions);
	}

	public boolean hasInitDependency() {
		return this.initDependency != null;
	}

	public void buildSaltField(ClassNode classNode, AbstractWorkspace workspace, ClassProvider classProvider, List<TypeWriter> typeWriters, Inheritance inheritance, ParameterChangerBuilder parameterChangerBuilder) {
		this.methodSalt = this.valueMidBits;
		ChangedMethod sharedInitDependency = null;
		if (!this.methodNode.isClinit() && this.initDependency != null && this.initDependency.sharesSaltField) {
			this.methodSalt = this.valueMidBits ^ this.initDependency.valueMidBits;
			sharedInitDependency = this.initDependency;
		}

		this.maxStack = parameterChangerBuilder.buildSaltInitCode(classNode, typeWriters, this, sharedInitDependency, workspace, classProvider);
		if (this.usesStaticSaltField) {
			boolean ownerIsInterface = classNode.isInterface();
			FieldNode saltField = classNode.createUniqueField("J", ownerIsInterface ? 4 : 1, true, inheritance, workspace, 5);
			this.saltFieldRef = classNode.getConstantPoolManager().getOrCreateFieldRef(classNode.getInternalName(), saltField.getNameDisplay(), saltField.getDescriptor(), typeWriters, saltField);
			this.decryptionInstructions.add(new WideConstantPoolInsnNode(PUTSTATIC, this.saltFieldRef));
		}
	}

	public long getRandomSaltValue() {
		return this.randomSaltValue;
	}

	public void addDependentMethod(ChangedMethod dependentMethod) {
		this.dependentMethods.add(dependentMethod);
		dependentMethod.initDependency = this;
	}

	public boolean hasFalseInitDependency() {
		return this.falseInitDependency != null;
	}

	public void addFalseDependentMethod(ChangedMethod falseDependentMethod) {
		this.falseDependents.add(falseDependentMethod);
		falseDependentMethod.falseInitDependency = this;
	}

	public long getSaltSeed() {
		return this.saltSeed;
	}

	public boolean sharesSaltField() {
		return this.sharesSaltField;
	}

	public void setSaltEntries(SaltEntry keyEntry, SaltEntry valueEntry, long methodValue, long randomSaltValue) {
		this.keyEntry = keyEntry;
		this.valueEntry = valueEntry;
		this.methodValue = methodValue;
		this.randomSaltValue = randomSaltValue;
		this.valueMidBits = this.valueEntry.getMidBits();
	}

	public List<ChangedMethod> getFalseDependents() {
		return new ArrayList<>(this.falseDependents);
	}

	public MethodNode getMethodNode() {
		return this.methodNode;
	}
}
