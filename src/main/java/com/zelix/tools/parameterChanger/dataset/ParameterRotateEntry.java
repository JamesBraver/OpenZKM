package com.zelix.tools.parameterChanger.dataset;

import com.zelix.framework.constant.ConstantPoolManager;
import com.zelix.framework.hierarchy.Inheritance;
import com.zelix.framework.instructions.AbstractInsnNode;
import com.zelix.framework.instructions.InsnNode;
import com.zelix.framework.instructions.LabelNode;
import com.zelix.framework.instructions.WideConstantPoolInsnNode;
import com.zelix.framework.tree.ArchiveNode;
import com.zelix.framework.variable.LocalVariableOperand;
import com.zelix.framework.variable.LocalVariableTable;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.FieldTypeWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.repository.ClassProvider;
import com.zelix.tools.parameterChanger.ParameterChangerBuilder;
import com.zelix.tools.parameterChanger.salt.SaltEntry;
import com.zelix.tools.parameterChanger.salt.SaltOrder;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.dataset.Triplet;
import com.zelix.workspace.AbstractWorkspace;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.LongStream;

import static com.zelix.framework.instructions.OpcodeConstants.GETSTATIC;
import static com.zelix.framework.instructions.OpcodeConstants.LXOR;

public class ParameterRotateEntry {
	private Map<MethodNode, Long> encryptionKeysByMethod = GlobalUtils.newHashMap(13);
	private Map<MethodNode, LocalVariableOperand> operandsByMethod = GlobalUtils.newHashMap(13);
	private final ParameterChangerBuilder parameterChangerBuilder;
	private final AbstractWorkspace workspace;
	private final ClassProvider classProvider;
	private final Random random;
	private final Iterator<Long> randomLongIterator;
	private final Triplet defaultMpcClassTriplet;
	private final Map<ArchiveNode, Triplet> archiveToMpcClassTripletMap;
	private SaltEntry saltEntry;
	private SaltOrder saltOrder;
	private ChangedMethod changedMethod;
	private ArchiveNode currentArchiveNode;

	public ParameterRotateEntry(
		ParameterChangerBuilder parameterChangerBuilder, Triplet defaultMpcClassTriplet, Map<ArchiveNode, Triplet> archiveToMpcClassTripletMap, AbstractWorkspace workspace, ClassProvider classProvider, Random random
	) {
		this.parameterChangerBuilder = parameterChangerBuilder;
		this.workspace = workspace;
		this.classProvider = classProvider;
		this.random = random;
		LongStream randomLongStream = this.random.longs(1L, 281474976710656L);
		this.randomLongIterator = randomLongStream.iterator();
		this.defaultMpcClassTriplet = defaultMpcClassTriplet;
		this.archiveToMpcClassTripletMap = archiveToMpcClassTripletMap;
	}

	public Long createEncryptionKey(MethodNode methodNode) {
		long encryptionKey = ParameterChangerBuilder.newRandomLong();
		this.encryptionKeysByMethod.put(methodNode, encryptionKey);
		return encryptionKey;
	}

	public Map<MethodNode, LocalVariableOperand> getOperandsByMethod() {
		return this.operandsByMethod;
	}

	public boolean hasEncryptionKey(MethodNode methodNode) {
		return this.encryptionKeysByMethod.containsKey(methodNode);
	}

	public void setCurrentArchiveNode(ArchiveNode archiveNode) {
		this.currentArchiveNode = archiveNode;
	}

	public ParameterChangerBuilder getParameterChangerBuilder() {
		return this.parameterChangerBuilder;
	}

	public void setOperand(MethodNode methodNode, LocalVariableOperand operand) {
		this.operandsByMethod.put(methodNode, operand);
	}

	public Long getEncryptionKey(MethodNode methodNode) {
		return this.encryptionKeysByMethod.get(methodNode);
	}

	public ChangedMethod getChangedMethod() {
		return this.changedMethod;
	}

	public List<AbstractInsnNode> buildDecryptionPrologue(MethodNode methodNode, List<TypeWriter> constantPoolEntries, Inheritance inheritance, int variableFlags) {
		ClassNode methodOwnerClass = methodNode.getMethodOwner();
		this.changedMethod.buildSaltField(methodOwnerClass, this.workspace, this.classProvider, constantPoolEntries, inheritance, this.parameterChangerBuilder);
		ArrayList<AbstractInsnNode> prologueInstructions = new ArrayList<>(this.changedMethod.getDecryptionInstructions());
		ConstantPoolManager constantPoolManager = methodOwnerClass.getConstantPoolManager();
		Long encryptionKey = this.createEncryptionKey(methodNode);
		FieldTypeWriter saltFieldRef = this.changedMethod.getSaltFieldRef();
		long methodSalt = this.changedMethod.getMethodSalt();
		long encodedMethodSalt = encryptionKey ^ methodSalt;
		int saltLocalIndex = methodNode.getMaxLocalsCompat();
		methodNode.setMaxLocals(saltLocalIndex + 2);
		prologueInstructions.add(new WideConstantPoolInsnNode(GETSTATIC, saltFieldRef));
		prologueInstructions.add(AbstractInsnNode.ldcLong(encodedMethodSalt, constantPoolManager, constantPoolEntries));
		prologueInstructions.add(InsnNode.of(LXOR));
		LocalVariableTable localVariableTable = methodNode.getLocalVariableTable();
		prologueInstructions.add(AbstractInsnNode.storeLong(saltLocalIndex, localVariableTable, variableFlags));
		this.setOperand(methodNode, localVariableTable.getVariable(saltLocalIndex));
		prologueInstructions.add(new LabelNode(8192));
		return prologueInstructions;
	}

	public LocalVariableOperand getOperand(MethodNode methodNode) {
		return this.operandsByMethod.get(methodNode);
	}

	public long getSaltValue(int saltIndex) {
		return this.saltEntry.getPayloadAt(saltIndex);
	}

	public List<AbstractInsnNode> buildSaltXorPrologue(LocalVariableTable localVariableTable, int methodIndex, AtomicInt bytecodeSize, List<TypeWriter> constantPoolEntries, ConstantPoolManager constantPoolManager, int variableFlags) {
		ClassNode ownerClass = (ClassNode)constantPoolManager.getOwnerClassNode();
		int[] bitPermutation = this.parameterChangerBuilder.getBitPermutation();
		long randomSaltValue = this.changedMethod.getRandomSaltValue();
		Long methodSaltKey = ParameterChangerBuilder.permuteWithRandomIndex(randomSaltValue, bitPermutation);
		Long orderSaltKey = ParameterChangerBuilder.permuteWithRandomIndex(this.saltOrder.getRotationKey(), bitPermutation);
		Long valueKey = this.randomLongIterator.next();
		this.saltEntry.advanceChain(valueKey);
		ArrayList<AbstractInsnNode> prologueInstructions = new ArrayList<>();
		AtomicInt bytecodeSizeAccumulator = new AtomicInt();
		List<AbstractInsnNode> decryptionCallInstructions = this.parameterChangerBuilder.buildParamDecryptionCall(ownerClass, methodSaltKey, orderSaltKey, valueKey, bytecodeSizeAccumulator, constantPoolEntries, constantPoolManager, this.workspace, this.classProvider);
		int totalBytecodeSize = 0 + bytecodeSizeAccumulator.get();
		prologueInstructions.addAll(decryptionCallInstructions);
		prologueInstructions.add(AbstractInsnNode.loadLong(0, localVariableTable, variableFlags));
		prologueInstructions.add(InsnNode.of(LXOR));
		prologueInstructions.add(AbstractInsnNode.storeLong(0, localVariableTable, variableFlags));
		prologueInstructions.add(new LabelNode(8192));
		totalBytecodeSize += 3;
		bytecodeSize.set(totalBytecodeSize);
		return prologueInstructions;
	}

	public void initChangedMethod(MethodNode methodNode, int saltOrderIndex) {
		Triplet mpcClassTriplet;
		if (this.currentArchiveNode != null) {
			mpcClassTriplet = this.archiveToMpcClassTripletMap.get(this.currentArchiveNode);
		} else {
			mpcClassTriplet = this.defaultMpcClassTriplet;
		}

		this.changedMethod = new ChangedMethod(methodNode, true, false);
		this.saltOrder = this.parameterChangerBuilder.getSaltOrder(saltOrderIndex);
		this.changedMethod.setSaltEntries(this.saltOrder.getKeyEntry(), this.saltOrder.getValueEntry(), this.saltOrder.getMethodValue(), this.saltOrder.getMethodKey());
		this.changedMethod.setSaltSeed(this.randomLongIterator.next());
		ClassNode methodOwnerClass = methodNode.getMethodOwner();
		this.parameterChangerBuilder.putMpcClassTriplet(methodOwnerClass, mpcClassTriplet);
		this.parameterChangerBuilder.registerChangedMethod(methodNode, this.changedMethod);
		this.parameterChangerBuilder.addStringEncryptionClass(methodOwnerClass);
		SaltEntry keyEntry = this.changedMethod.getKeyEntry();
		this.saltEntry = new SaltEntry(keyEntry, this.parameterChangerBuilder.getBitPermutation());
	}

	public Map<MethodNode, Long> getEncryptionKeysByMethod() {
		return Collections.unmodifiableMap(this.encryptionKeysByMethod);
	}
}
