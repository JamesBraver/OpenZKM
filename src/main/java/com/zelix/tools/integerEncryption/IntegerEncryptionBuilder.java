package com.zelix.tools.integerEncryption;

import com.zelix.exceptions.ProcessingException;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.attributes.code.TryCatchBlockInfo;
import com.zelix.framework.constant.ConstantPoolManager;
import com.zelix.framework.constant.IntegerNode;
import com.zelix.framework.constant.LongNode;
import com.zelix.framework.constant.StringNode;
import com.zelix.framework.encryption.IntegerEntry;
import com.zelix.framework.encryption.IntegerLookup;
import com.zelix.framework.hierarchy.Inheritance;
import com.zelix.framework.instructions.*;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.variable.LocalVariableIndexCounter;
import com.zelix.framework.variable.LocalVariableOperand;
import com.zelix.framework.variable.LocalVariableTable;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.*;
import com.zelix.framework.writer.base.AbstractMethodCallWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.config.Options;
import com.zelix.repository.ClassProvider;
import com.zelix.tools.references.ObfuscateReferencesBuilder;
import com.zelix.utils.ByteUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.dataset.PriorityEntry;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.AbstractWorkspace;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.stream.LongStream;

import static com.zelix.framework.instructions.OpcodeConstants.*;

public class IntegerEncryptionBuilder {
	private Random random = GlobalUtils.newRandom(5023);
	private Iterator<Long> randomLongIterator;
	private IvParameterSpec ivSpec;
	private ClassNode targetClass;
	private FieldTypeWriter threadLocalMapField;
	private Cipher cipher;
	private FieldTypeWriter encryptedIntegersField;
	private AbstractMethodCallWriter indyDecryptMethod;
	private AbstractMethodCallWriter decryptMethod;
	private InvokeDynamicCallWriter indyCallSite;
	private Long globalKey;
	private SecretKeyFactory keyFactory;
	private int xorKey;

	public IntegerEncryptionBuilder() {
		LongStream randomLongStream = this.random.longs(1L, Long.MAX_VALUE);
		this.randomLongIterator = randomLongStream.iterator();
	}

	public static boolean isIndySupported(ClassNode classNode) {
		return Options.ZKM_INTEGER_ENCRYPT_INDY && ObfuscateReferencesBuilder.indySupported(classNode);
	}

	public boolean hasGlobalKey() {
		return this.globalKey != null;
	}

	private void generateDecryptionCode(
		LocalVariableTable localVariableTable,
		ArrayList<AbstractInsnNode> instructions,
		FieldTypeWriter encryptedLongsField,
		List<TypeWriter> cpEntries,
		ConstantPoolManager constantPoolManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		LabelNode alreadyDecryptedLabel = new LabelNode(true, 1);
		instructions.add(AbstractInsnNode.loadInt(0, localVariableTable, 11));
		instructions.add(AbstractInsnNode.loadLong(1, localVariableTable, 11));
		instructions.add(AbstractInsnNode.ldcLong(32767L, constantPoolManager, cpEntries));
		instructions.add(InsnNode.of(LAND));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(IXOR));
		instructions.add(AbstractInsnNode.ldcInt(this.xorKey, constantPoolManager, cpEntries));
		instructions.add(InsnNode.of(IXOR));
		instructions.add(AbstractInsnNode.storeInt(3, localVariableTable, 11));
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, this.encryptedIntegersField));
		instructions.add(AbstractInsnNode.loadInt(3, localVariableTable, 11));
		instructions.add(InsnNode.of(AALOAD));
		instructions.add(new JumpInsnNode(IFNONNULL, alreadyDecryptedLabel));
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, this.encryptedIntegersField));
		instructions.add(AbstractInsnNode.loadInt(3, localVariableTable, 11));
		if (this.targetClass.isJava5OrNewer()) {
			instructions.add(new WideConstantPoolInsnNode(GETSTATIC, encryptedLongsField));
			instructions.add(AbstractInsnNode.loadInt(3, localVariableTable, 11));
			instructions.add(InsnNode.of(LALOAD));
			instructions.add(AbstractInsnNode.loadLong(1, localVariableTable, 11));
			instructions.add(InsnNode.of(LXOR));
			instructions.add(InsnNode.of(L2I));
			MethodCallWriter integerValueOfMethod = constantPoolManager.getOrCreateMethodRef("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, integerValueOfMethod));
		} else {
			ClassInfoWriter integerClass = constantPoolManager.getOrCreateClassInfo("java/lang/Integer", cpEntries);
			instructions.add(new NewNode(integerClass));
			instructions.add(InsnNode.of(DUP));
			instructions.add(new WideConstantPoolInsnNode(GETSTATIC, encryptedLongsField));
			instructions.add(AbstractInsnNode.loadInt(3, localVariableTable, 11));
			instructions.add(InsnNode.of(LALOAD));
			instructions.add(AbstractInsnNode.loadLong(1, localVariableTable, 11));
			instructions.add(InsnNode.of(LXOR));
			instructions.add(InsnNode.of(L2I));
			MethodCallWriter integerConstructorMethod = constantPoolManager.getOrCreateMethodRef("java/lang/Integer", "<init>", "(I)V", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, integerConstructorMethod));
		}

		instructions.add(InsnNode.of(AASTORE));
		instructions.add(alreadyDecryptedLabel);
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, this.encryptedIntegersField));
		instructions.add(AbstractInsnNode.loadInt(3, localVariableTable, 11));
		instructions.add(InsnNode.of(AALOAD));
		MethodCallWriter integerIntValueMethod = constantPoolManager.getOrCreateMethodRef("java/lang/Integer", "intValue", "()I", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, integerIntValueMethod));
		instructions.add(InsnNode.of(IRETURN));
	}

	private void encryptLongToBytesAndBack(
		LocalVariableTable localVariableTable,
		List<AbstractInsnNode> instructions,
		List<TypeWriter> cpEntries,
		int cipherLocalIndex,
		LocalVariableIndexCounter localVariableIndexCounter,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		int longLocalIndex = localVariableIndexCounter.getAndIncrease();
		localVariableIndexCounter.increaseAndGet();
		int bytesLocalIndex = localVariableIndexCounter.getAndIncrease();
		ConstantPoolManager constantPoolManager = this.targetClass.getConstantPoolManager();
		instructions.add(AbstractInsnNode.storeLong(longLocalIndex, localVariableTable, 11));
		AbstractInsnNode.encryptIntConstant(8, instructions, constantPoolManager, cpEntries);
		instructions.add(new NewArrayNode(8));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(AbstractInsnNode.loadLong(longLocalIndex, localVariableTable, 11));
		AbstractInsnNode.encryptIntConstant(56, instructions, constantPoolManager, cpEntries);
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_1));
		instructions.add(AbstractInsnNode.loadLong(longLocalIndex, localVariableTable, 11));
		AbstractInsnNode.encryptIntConstant(48, instructions, constantPoolManager, cpEntries);
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_2));
		instructions.add(AbstractInsnNode.loadLong(longLocalIndex, localVariableTable, 11));
		AbstractInsnNode.encryptIntConstant(40, instructions, constantPoolManager, cpEntries);
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_3));
		instructions.add(AbstractInsnNode.loadLong(longLocalIndex, localVariableTable, 11));
		AbstractInsnNode.encryptIntConstant(32, instructions, constantPoolManager, cpEntries);
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_4));
		instructions.add(AbstractInsnNode.loadLong(longLocalIndex, localVariableTable, 11));
		AbstractInsnNode.encryptIntConstant(24, instructions, constantPoolManager, cpEntries);
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_5));
		instructions.add(AbstractInsnNode.loadLong(longLocalIndex, localVariableTable, 11));
		AbstractInsnNode.encryptIntConstant(16, instructions, constantPoolManager, cpEntries);
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		AbstractInsnNode.encryptIntConstant(6, instructions, constantPoolManager, cpEntries);
		instructions.add(AbstractInsnNode.loadLong(longLocalIndex, localVariableTable, 11));
		AbstractInsnNode.encryptIntConstant(8, instructions, constantPoolManager, cpEntries);
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		AbstractInsnNode.encryptIntConstant(7, instructions, constantPoolManager, cpEntries);
		instructions.add(AbstractInsnNode.loadLong(longLocalIndex, localVariableTable, 11));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(AbstractInsnNode.loadObject(cipherLocalIndex, localVariableTable, 11));
		instructions.add(InsnNode.of(SWAP));
		MethodCallWriter cipherDoFinalMethod = constantPoolManager.getOrCreateMethodRef("javax/crypto/Cipher", "doFinal", "([B)[B", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, cipherDoFinalMethod));
		instructions.add(AbstractInsnNode.storeObject(bytesLocalIndex, localVariableTable, 11));
		instructions.add(AbstractInsnNode.loadObject(bytesLocalIndex, localVariableTable, 11));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(InsnNode.of(BALOAD));
		instructions.add(InsnNode.of(I2L));
		AbstractInsnNode.appendLongConst(255L, instructions, constantPoolManager, cpEntries);
		instructions.add(InsnNode.of(LAND));
		AbstractInsnNode.encryptIntConstant(56, instructions, constantPoolManager, cpEntries);
		instructions.add(InsnNode.of(LSHL));
		instructions.add(AbstractInsnNode.loadObject(bytesLocalIndex, localVariableTable, 11));
		instructions.add(InsnNode.of(ICONST_1));
		instructions.add(InsnNode.of(BALOAD));
		instructions.add(InsnNode.of(I2L));
		AbstractInsnNode.appendLongConst(255L, instructions, constantPoolManager, cpEntries);
		instructions.add(InsnNode.of(LAND));
		AbstractInsnNode.encryptIntConstant(48, instructions, constantPoolManager, cpEntries);
		instructions.add(InsnNode.of(LSHL));
		instructions.add(InsnNode.of(LOR));
		instructions.add(AbstractInsnNode.loadObject(bytesLocalIndex, localVariableTable, 11));
		instructions.add(InsnNode.of(ICONST_2));
		instructions.add(InsnNode.of(BALOAD));
		instructions.add(InsnNode.of(I2L));
		AbstractInsnNode.appendLongConst(255L, instructions, constantPoolManager, cpEntries);
		instructions.add(InsnNode.of(LAND));
		AbstractInsnNode.encryptIntConstant(40, instructions, constantPoolManager, cpEntries);
		instructions.add(InsnNode.of(LSHL));
		instructions.add(InsnNode.of(LOR));
		instructions.add(AbstractInsnNode.loadObject(bytesLocalIndex, localVariableTable, 11));
		instructions.add(InsnNode.of(ICONST_3));
		instructions.add(InsnNode.of(BALOAD));
		instructions.add(InsnNode.of(I2L));
		AbstractInsnNode.appendLongConst(255L, instructions, constantPoolManager, cpEntries);
		instructions.add(InsnNode.of(LAND));
		AbstractInsnNode.encryptIntConstant(32, instructions, constantPoolManager, cpEntries);
		instructions.add(InsnNode.of(LSHL));
		instructions.add(InsnNode.of(LOR));
		instructions.add(AbstractInsnNode.loadObject(bytesLocalIndex, localVariableTable, 11));
		instructions.add(InsnNode.of(ICONST_4));
		instructions.add(InsnNode.of(BALOAD));
		instructions.add(InsnNode.of(I2L));
		AbstractInsnNode.appendLongConst(255L, instructions, constantPoolManager, cpEntries);
		instructions.add(InsnNode.of(LAND));
		AbstractInsnNode.encryptIntConstant(24, instructions, constantPoolManager, cpEntries);
		instructions.add(InsnNode.of(LSHL));
		instructions.add(InsnNode.of(LOR));
		instructions.add(AbstractInsnNode.loadObject(bytesLocalIndex, localVariableTable, 11));
		instructions.add(InsnNode.of(ICONST_5));
		instructions.add(InsnNode.of(BALOAD));
		instructions.add(InsnNode.of(I2L));
		AbstractInsnNode.appendLongConst(255L, instructions, constantPoolManager, cpEntries);
		instructions.add(InsnNode.of(LAND));
		AbstractInsnNode.encryptIntConstant(16, instructions, constantPoolManager, cpEntries);
		instructions.add(InsnNode.of(LSHL));
		instructions.add(InsnNode.of(LOR));
		instructions.add(AbstractInsnNode.loadObject(bytesLocalIndex, localVariableTable, 11));
		AbstractInsnNode.encryptIntConstant(6, instructions, constantPoolManager, cpEntries);
		instructions.add(InsnNode.of(BALOAD));
		instructions.add(InsnNode.of(I2L));
		AbstractInsnNode.appendLongConst(255L, instructions, constantPoolManager, cpEntries);
		instructions.add(InsnNode.of(LAND));
		AbstractInsnNode.encryptIntConstant(8, instructions, constantPoolManager, cpEntries);
		instructions.add(InsnNode.of(LSHL));
		instructions.add(InsnNode.of(LOR));
		instructions.add(AbstractInsnNode.loadObject(bytesLocalIndex, localVariableTable, 11));
		AbstractInsnNode.encryptIntConstant(7, instructions, constantPoolManager, cpEntries);
		instructions.add(InsnNode.of(BALOAD));
		instructions.add(InsnNode.of(I2L));
		AbstractInsnNode.appendLongConst(255L, instructions, constantPoolManager, cpEntries);
		instructions.add(InsnNode.of(LAND));
		instructions.add(InsnNode.of(LOR));
	}

	public void generateInitEncryptedStringsAndLongsCode(
		LocalVariableTable localVariableTable,
		List<AbstractInsnNode> instructions,
		LocalVariableIndexCounter localVariableIndexCounter,
		Set<TypeWriter> unusedConstants,
		List<TypeWriter> cpEntries,
		FieldTypeWriter encryptedLongsField,
		FieldTypeWriter encryptedIntegersArrayField,
		int longsArrayLocalIndex,
		AtomicReference<Integer>[] integerReferences,
		ReferenceGraph<AtomicReference<Integer>, IntegerNode> referenceGraph,
		Map<AtomicReference<Integer>, IntegerEntry> integerEntries,
		LabelNode initialSwitchLabel,
		AtomicInt switchCaseCounter,
		MultiMap<LabelNode, PriorityEntry<LabelNode>> switchEntryMap,
		Long key,
		boolean useDes,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		int referenceCount = integerReferences.length;
		List<StringNode> encryptedValueStrings = this.packEncryptedValuesToStrings(integerReferences, referenceGraph, integerEntries, unusedConstants, key, useDes, cpEntries, this.targetClass.getConstantPoolManager());
		ConstantPoolManager constantPoolManager = this.targetClass.getConstantPoolManager();
		int charIndex = localVariableIndexCounter.getAndIncrease();
		int longsIndex = localVariableIndexCounter.getAndIncrease();
		int stringLocalIndex = localVariableIndexCounter.getAndIncrease();
		int stringLengthLocalIndex = localVariableIndexCounter.getAndIncrease();
		int effectiveLongsArrayLocalIndex = longsArrayLocalIndex == -1 ? localVariableIndexCounter.getAndIncrease() : longsArrayLocalIndex;
		int bytesLocalIndex = localVariableIndexCounter.getAndIncrease();
		AbstractInsnNode.encryptIntConstant(referenceCount, instructions, constantPoolManager, cpEntries);
		instructions.add(new NewArrayNode(11));
		instructions.add(AbstractInsnNode.storeObject(effectiveLongsArrayLocalIndex, localVariableTable, 11));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(AbstractInsnNode.storeInt(longsIndex, localVariableTable, 11));

		for (StringNode encryptedValueString : encryptedValueStrings) {
			LabelNode substringLoopLabel = new LabelNode(true, 1);
			instructions.add(new WideConstantPoolInsnNode(LDC_W, encryptedValueString));
			instructions.add(InsnNode.of(DUP));
			instructions.add(AbstractInsnNode.storeObject(stringLocalIndex, localVariableTable, 11));
			MethodCallWriter stringLengthMethod = constantPoolManager.getOrCreateMethodRef("java/lang/String", "length", "()I", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, stringLengthMethod));
			instructions.add(AbstractInsnNode.storeInt(stringLengthLocalIndex, localVariableTable, 11));
			instructions.add(InsnNode.of(ICONST_0));
			instructions.add(AbstractInsnNode.storeInt(charIndex, localVariableTable, 11));
			instructions.add(substringLoopLabel);
			instructions.add(AbstractInsnNode.loadObject(stringLocalIndex, localVariableTable, 11));
			instructions.add(AbstractInsnNode.loadInt(charIndex, localVariableTable, 11));
			instructions.add(AbstractInsnNode.IINC(charIndex, 8, localVariableTable, 11));
			instructions.add(AbstractInsnNode.loadInt(charIndex, localVariableTable, 11));
			MethodCallWriter stringSubstringMethod = constantPoolManager.getOrCreateMethodRef("java/lang/String", "substring", "(II)Ljava/lang/String;", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, stringSubstringMethod));
			StringNode charsetString = constantPoolManager.createStringNode("ISO-8859-1", cpEntries, false);
			instructions.add(new WideConstantPoolInsnNode(LDC_W, charsetString));
			MethodCallWriter stringGetBytesMethod = constantPoolManager.getOrCreateMethodRef("java/lang/String", "getBytes", "(Ljava/lang/String;)[B", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, stringGetBytesMethod));
			instructions.add(AbstractInsnNode.storeObject(bytesLocalIndex, localVariableTable, 11));
			instructions.add(AbstractInsnNode.loadObject(effectiveLongsArrayLocalIndex, localVariableTable, 11));
			instructions.add(AbstractInsnNode.loadInt(longsIndex, localVariableTable, 11));
			instructions.add(AbstractInsnNode.IINC(longsIndex, 1, localVariableTable, 11));
			instructions.add(AbstractInsnNode.loadObject(bytesLocalIndex, localVariableTable, 11));
			instructions.add(InsnNode.of(ICONST_0));
			instructions.add(InsnNode.of(BALOAD));
			instructions.add(InsnNode.of(I2L));
			AbstractInsnNode.appendLongConst(255L, instructions, constantPoolManager, cpEntries);
			instructions.add(InsnNode.of(LAND));
			instructions.add(AbstractInsnNode.newInt(56));
			instructions.add(InsnNode.of(LSHL));
			instructions.add(AbstractInsnNode.loadObject(bytesLocalIndex, localVariableTable, 11));
			instructions.add(InsnNode.of(ICONST_1));
			instructions.add(InsnNode.of(BALOAD));
			instructions.add(InsnNode.of(I2L));
			AbstractInsnNode.appendLongConst(255L, instructions, constantPoolManager, cpEntries);
			instructions.add(InsnNode.of(LAND));
			instructions.add(AbstractInsnNode.newInt(48));
			instructions.add(InsnNode.of(LSHL));
			instructions.add(InsnNode.of(LOR));
			instructions.add(AbstractInsnNode.loadObject(bytesLocalIndex, localVariableTable, 11));
			instructions.add(InsnNode.of(ICONST_2));
			instructions.add(InsnNode.of(BALOAD));
			instructions.add(InsnNode.of(I2L));
			AbstractInsnNode.appendLongConst(255L, instructions, constantPoolManager, cpEntries);
			instructions.add(InsnNode.of(LAND));
			instructions.add(AbstractInsnNode.newInt(40));
			instructions.add(InsnNode.of(LSHL));
			instructions.add(InsnNode.of(LOR));
			instructions.add(AbstractInsnNode.loadObject(bytesLocalIndex, localVariableTable, 11));
			instructions.add(InsnNode.of(ICONST_3));
			instructions.add(InsnNode.of(BALOAD));
			instructions.add(InsnNode.of(I2L));
			AbstractInsnNode.appendLongConst(255L, instructions, constantPoolManager, cpEntries);
			instructions.add(InsnNode.of(LAND));
			instructions.add(AbstractInsnNode.newInt(32));
			instructions.add(InsnNode.of(LSHL));
			instructions.add(InsnNode.of(LOR));
			instructions.add(AbstractInsnNode.loadObject(bytesLocalIndex, localVariableTable, 11));
			instructions.add(InsnNode.of(ICONST_4));
			instructions.add(InsnNode.of(BALOAD));
			instructions.add(InsnNode.of(I2L));
			AbstractInsnNode.appendLongConst(255L, instructions, constantPoolManager, cpEntries);
			instructions.add(InsnNode.of(LAND));
			instructions.add(AbstractInsnNode.newInt(24));
			instructions.add(InsnNode.of(LSHL));
			instructions.add(InsnNode.of(LOR));
			instructions.add(AbstractInsnNode.loadObject(bytesLocalIndex, localVariableTable, 11));
			instructions.add(InsnNode.of(ICONST_5));
			instructions.add(InsnNode.of(BALOAD));
			instructions.add(InsnNode.of(I2L));
			AbstractInsnNode.appendLongConst(255L, instructions, constantPoolManager, cpEntries);
			instructions.add(InsnNode.of(LAND));
			instructions.add(AbstractInsnNode.newInt(16));
			instructions.add(InsnNode.of(LSHL));
			instructions.add(InsnNode.of(LOR));
			instructions.add(AbstractInsnNode.loadObject(bytesLocalIndex, localVariableTable, 11));
			instructions.add(AbstractInsnNode.newInt(6));
			instructions.add(InsnNode.of(BALOAD));
			instructions.add(InsnNode.of(I2L));
			AbstractInsnNode.appendLongConst(255L, instructions, constantPoolManager, cpEntries);
			instructions.add(InsnNode.of(LAND));
			instructions.add(AbstractInsnNode.newInt(8));
			instructions.add(InsnNode.of(LSHL));
			instructions.add(InsnNode.of(LOR));
			instructions.add(AbstractInsnNode.loadObject(bytesLocalIndex, localVariableTable, 11));
			instructions.add(AbstractInsnNode.newInt(7));
			instructions.add(InsnNode.of(BALOAD));
			instructions.add(InsnNode.of(I2L));
			AbstractInsnNode.appendLongConst(255L, instructions, constantPoolManager, cpEntries);
			instructions.add(InsnNode.of(LAND));
			instructions.add(InsnNode.of(LOR));
			int switchCaseValue = switchCaseCounter.increaseAndGet();
			instructions.add(AbstractInsnNode.newInt(switchCaseValue));
			instructions.add(new GotoNode(initialSwitchLabel));
			LabelNode switchCaseLabel = new LabelNode(true, 1);
			instructions.add(switchCaseLabel);
			switchEntryMap.put(initialSwitchLabel, new PriorityEntry<>(switchCaseValue, switchCaseLabel));
			instructions.add(InsnNode.of(LASTORE));
			instructions.add(AbstractInsnNode.loadInt(charIndex, localVariableTable, 11));
			instructions.add(AbstractInsnNode.loadInt(stringLengthLocalIndex, localVariableTable, 11));
			instructions.add(new JumpInsnNode(IF_ICMPLT, substringLoopLabel));
		}

		if (encryptedLongsField != null) {
			instructions.add(AbstractInsnNode.loadObject(effectiveLongsArrayLocalIndex, localVariableTable, 11));
			instructions.add(new WideConstantPoolInsnNode(PUTSTATIC, encryptedLongsField));
			AbstractInsnNode.encryptIntConstant(referenceCount, instructions, constantPoolManager, cpEntries);
			ClassInfoWriter integerClass = constantPoolManager.getOrCreateClassInfo("java/lang/Integer", cpEntries);
			instructions.add(new WideConstantPoolInsnNode(ANEWARRAY, integerClass));
			instructions.add(new WideConstantPoolInsnNode(PUTSTATIC, this.encryptedIntegersField));
		}
	}

	public long getGlobalKey() {
		return this.globalKey;
	}

	private void generateIndyBootstrapCode(
		LocalVariableTable localVariableTable,
		ArrayList<AbstractInsnNode> instructions,
		AbstractMethodCallWriter decryptMethod,
		List<TypeWriter> cpEntries,
		ConstantPoolManager constantPoolManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		instructions.add(AbstractInsnNode.loadObject(3, localVariableTable, 11));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(InsnNode.of(AALOAD));
		ClassInfoWriter integerClass = constantPoolManager.getOrCreateClassInfo("java/lang/Integer", cpEntries);
		instructions.add(new WideConstantPoolInsnNode(CHECKCAST, integerClass));
		MethodCallWriter integerIntValueMethod = constantPoolManager.getOrCreateMethodRef("java/lang/Integer", "intValue", "()I", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, integerIntValueMethod));
		instructions.add(AbstractInsnNode.storeInt(4, localVariableTable, 11));
		instructions.add(AbstractInsnNode.loadObject(3, localVariableTable, 11));
		instructions.add(InsnNode.of(ICONST_1));
		instructions.add(InsnNode.of(AALOAD));
		ClassInfoWriter longClass = constantPoolManager.getOrCreateClassInfo("java/lang/Long", cpEntries);
		instructions.add(new WideConstantPoolInsnNode(CHECKCAST, longClass));
		MethodCallWriter longValueMethod = constantPoolManager.getOrCreateMethodRef("java/lang/Long", "longValue", "()J", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, longValueMethod));
		instructions.add(AbstractInsnNode.storeLong(5, localVariableTable, 11));
		instructions.add(AbstractInsnNode.loadInt(4, localVariableTable, 11));
		instructions.add(AbstractInsnNode.loadLong(5, localVariableTable, 11));
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, decryptMethod));
		instructions.add(AbstractInsnNode.storeInt(7, localVariableTable, 11));
		Integer innerClassOrdinal = this.targetClass.hasInnerClassOrdinal() ? this.targetClass.getInnerClassOrdinal() : null;
		AbstractFieldNode integerTypeField = classProvider.getClassNode("java/lang/Integer", innerClassOrdinal).findField("TYPE", "Ljava/lang/Class;");
		FieldTypeWriter integerTypeFieldRef = constantPoolManager.getOrCreateFieldRef("java/lang/Integer", "TYPE", "Ljava/lang/Class;", cpEntries, integerTypeField);
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, integerTypeFieldRef));
		instructions.add(AbstractInsnNode.loadInt(7, localVariableTable, 11));
		MethodCallWriter integerValueOfMethod = constantPoolManager.getOrCreateMethodRef("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, integerValueOfMethod));
		MethodCallWriter methodHandlesConstantMethod = constantPoolManager.getOrCreateMethodRef(
			"java/lang/invoke/MethodHandles", "constant", "(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;", cpEntries, workspace, classProvider
		);
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, methodHandlesConstantMethod));
		instructions.add(AbstractInsnNode.storeObject(8, localVariableTable, 11));
		instructions.add(AbstractInsnNode.loadObject(1, localVariableTable, 11));
		instructions.add(AbstractInsnNode.loadObject(8, localVariableTable, 11));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(InsnNode.of(ICONST_2));
		ClassInfoWriter classArrayElementClass = constantPoolManager.getOrCreateClassInfo("java/lang/Class", cpEntries);
		instructions.add(new WideConstantPoolInsnNode(ANEWARRAY, classArrayElementClass));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, integerTypeFieldRef));
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_1));
		AbstractFieldNode longTypeField = classProvider.getClassNode("java/lang/Long", innerClassOrdinal).findField("TYPE", "Ljava/lang/Class;");
		FieldTypeWriter longTypeFieldRef = constantPoolManager.getOrCreateFieldRef("java/lang/Long", "TYPE", "Ljava/lang/Class;", cpEntries, longTypeField);
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, longTypeFieldRef));
		instructions.add(InsnNode.of(AASTORE));
		MethodCallWriter dropArgumentsMethod = constantPoolManager.getOrCreateMethodRef(
			"java/lang/invoke/MethodHandles", "dropArguments", "(Ljava/lang/invoke/MethodHandle;I[Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;", cpEntries, workspace, classProvider
		);
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, dropArgumentsMethod));
		MethodCallWriter setTargetMethod = constantPoolManager.getOrCreateMethodRef("java/lang/invoke/MutableCallSite", "setTarget", "(Ljava/lang/invoke/MethodHandle;)V", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, setTargetMethod));
		instructions.add(AbstractInsnNode.loadInt(7, localVariableTable, 11));
		instructions.add(InsnNode.of(IRETURN));
	}

	private void generateXorCode(
		LocalVariableTable localVariableTable,
		List<AbstractInsnNode> instructions,
		List<TypeWriter> cpEntries,
		Long key,
		int keyIndex,
		LocalVariableIndexCounter localVariableIndexCounter,
		ConstantPoolManager constantPoolManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		instructions.add(AbstractInsnNode.loadLong(keyIndex, localVariableTable, 11));
		instructions.add(InsnNode.of(LXOR));
	}

	private void generateDesDecryptionCode(
		LocalVariableTable localVariableTable,
		ArrayList<AbstractInsnNode> instructions,
		boolean usesLongArray,
		FieldTypeWriter encryptedLongsField,
		TryCatchBlockInfo[] tryCatchBlockInfos,
		List<TypeWriter> cpEntries,
		ConstantPoolManager constantPoolManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		LabelNode cacheHitLabel = new LabelNode(true, 1);
		LabelNode afterTryLabel = new LabelNode(true, 1);
		LabelNode alreadyDecryptedLabel = new LabelNode(true, 1);
		LabelNode tryStartLabel = new LabelNode(true, 384);
		LabelNode tryEndLabel = new LabelNode(true, 640);
		LabelNode catchHandlerLabel = new LabelNode(true, 1152);
		ClassInfoWriter exceptionClass = constantPoolManager.getOrCreateClassInfo("java/lang/Exception", cpEntries);
		tryCatchBlockInfos[0] = new TryCatchBlockInfo(exceptionClass, tryStartLabel, tryEndLabel, catchHandlerLabel);
		instructions.add(AbstractInsnNode.loadInt(0, localVariableTable, 11));
		instructions.add(AbstractInsnNode.loadLong(1, localVariableTable, 11));
		instructions.add(AbstractInsnNode.ldcLong(32767L, constantPoolManager, cpEntries));
		instructions.add(InsnNode.of(LAND));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(IXOR));
		instructions.add(AbstractInsnNode.ldcInt(this.xorKey, constantPoolManager, cpEntries));
		instructions.add(InsnNode.of(IXOR));
		instructions.add(AbstractInsnNode.storeInt(3, localVariableTable, 11));
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, this.encryptedIntegersField));
		instructions.add(AbstractInsnNode.loadInt(3, localVariableTable, 11));
		instructions.add(InsnNode.of(AALOAD));
		instructions.add(new JumpInsnNode(IFNONNULL, alreadyDecryptedLabel));
		instructions.add(AbstractInsnNode.newInt(8));
		instructions.add(new NewArrayNode(8));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(AbstractInsnNode.loadLong(1, localVariableTable, 11));
		instructions.add(AbstractInsnNode.newInt(56));
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_1));
		instructions.add(AbstractInsnNode.loadLong(1, localVariableTable, 11));
		instructions.add(AbstractInsnNode.newInt(48));
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_2));
		instructions.add(AbstractInsnNode.loadLong(1, localVariableTable, 11));
		instructions.add(AbstractInsnNode.newInt(40));
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_3));
		instructions.add(AbstractInsnNode.loadLong(1, localVariableTable, 11));
		instructions.add(AbstractInsnNode.newInt(32));
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_4));
		instructions.add(AbstractInsnNode.loadLong(1, localVariableTable, 11));
		instructions.add(AbstractInsnNode.newInt(24));
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_5));
		instructions.add(AbstractInsnNode.loadLong(1, localVariableTable, 11));
		instructions.add(AbstractInsnNode.newInt(16));
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.newInt(6));
		instructions.add(AbstractInsnNode.loadLong(1, localVariableTable, 11));
		instructions.add(AbstractInsnNode.newInt(8));
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.newInt(7));
		instructions.add(AbstractInsnNode.loadLong(1, localVariableTable, 11));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(AbstractInsnNode.storeObject(4, localVariableTable, 11));
		if (usesLongArray) {
			instructions.add(new WideConstantPoolInsnNode(GETSTATIC, encryptedLongsField));
			instructions.add(AbstractInsnNode.loadInt(3, localVariableTable, 11));
			instructions.add(InsnNode.of(LALOAD));
		} else {
			instructions.add(new WideConstantPoolInsnNode(GETSTATIC, encryptedLongsField));
		}

		instructions.add(AbstractInsnNode.storeLong(5, localVariableTable, 11));
		instructions.add(AbstractInsnNode.newInt(8));
		instructions.add(new NewArrayNode(8));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(AbstractInsnNode.loadLong(5, localVariableTable, 11));
		instructions.add(AbstractInsnNode.newInt(56));
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_1));
		instructions.add(AbstractInsnNode.loadLong(5, localVariableTable, 11));
		instructions.add(AbstractInsnNode.newInt(48));
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_2));
		instructions.add(AbstractInsnNode.loadLong(5, localVariableTable, 11));
		instructions.add(AbstractInsnNode.newInt(40));
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_3));
		instructions.add(AbstractInsnNode.loadLong(5, localVariableTable, 11));
		instructions.add(AbstractInsnNode.newInt(32));
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_4));
		instructions.add(AbstractInsnNode.loadLong(5, localVariableTable, 11));
		instructions.add(AbstractInsnNode.newInt(24));
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_5));
		instructions.add(AbstractInsnNode.loadLong(5, localVariableTable, 11));
		instructions.add(AbstractInsnNode.newInt(16));
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.newInt(6));
		instructions.add(AbstractInsnNode.loadLong(5, localVariableTable, 11));
		instructions.add(AbstractInsnNode.newInt(8));
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.newInt(7));
		instructions.add(AbstractInsnNode.loadLong(5, localVariableTable, 11));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(AbstractInsnNode.storeObject(7, localVariableTable, 11));
		MethodCallWriter currentThreadMethod = constantPoolManager.getOrCreateMethodRef("java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", cpEntries, workspace, classProvider);
		if (this.targetClass.isJava5OrNewer()) {
			instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, currentThreadMethod));
			if (this.targetClass.isJava19OrNewer()) {
				MethodCallWriter threadIdMethod = constantPoolManager.getOrCreateMethodRef("java/lang/Thread", "threadId", "()J", cpEntries, workspace, classProvider);
				instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, threadIdMethod));
			} else {
				MethodCallWriter threadGetIdMethod = constantPoolManager.getOrCreateMethodRef("java/lang/Thread", "getId", "()J", cpEntries, workspace, classProvider);
				instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, threadGetIdMethod));
			}

			MethodCallWriter longValueOfMethod = constantPoolManager.getOrCreateMethodRef("java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, longValueOfMethod));
		} else {
			MethodCallWriter identityHashCodeMethod = constantPoolManager.getOrCreateMethodRef("java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I", cpEntries, workspace, classProvider);
			ClassInfoWriter longClass = constantPoolManager.getOrCreateClassInfo("java/lang/Long", cpEntries);
			instructions.add(new NewNode(longClass));
			instructions.add(InsnNode.of(DUP));
			instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, currentThreadMethod));
			instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, identityHashCodeMethod));
			instructions.add(InsnNode.of(I2L));
			MethodCallWriter longConstructorMethod = constantPoolManager.getOrCreateMethodRef("java/lang/Long", "<init>", "(J)V", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, longConstructorMethod));
		}

		instructions.add(AbstractInsnNode.storeObject(8, localVariableTable, 11));
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, this.threadLocalMapField));
		instructions.add(AbstractInsnNode.loadObject(8, localVariableTable, 11));
		InterfaceMethodCallWriter mapGetMethod = constantPoolManager.getOrCreateInterfaceMethod("java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", cpEntries, workspace, classProvider);
		instructions.add(new InvokeInterfaceNode(mapGetMethod));
		ClassInfoWriter objectArrayClass = constantPoolManager.getOrCreateClassInfo("[Ljava/lang/Object;", cpEntries);
		instructions.add(new WideConstantPoolInsnNode(CHECKCAST, objectArrayClass));
		instructions.add(AbstractInsnNode.storeObject(9, localVariableTable, 11));
		instructions.add(tryStartLabel);
		instructions.add(AbstractInsnNode.loadObject(9, localVariableTable, 11));
		instructions.add(new JumpInsnNode(IFNONNULL, cacheHitLabel));
		instructions.add(InsnNode.of(ICONST_3));
		ClassInfoWriter objectArrayElementClass = constantPoolManager.getOrCreateClassInfo("java/lang/Object", cpEntries);
		instructions.add(new WideConstantPoolInsnNode(ANEWARRAY, objectArrayElementClass));
		instructions.add(AbstractInsnNode.storeObject(9, localVariableTable, 11));
		instructions.add(AbstractInsnNode.loadObject(9, localVariableTable, 11));
		instructions.add(InsnNode.of(ICONST_0));
		StringNode cipherTransformationString = constantPoolManager.createStringNode("DES/CBC/NoPadding", cpEntries, false);
		instructions.add(new WideConstantPoolInsnNode(LDC_W, cipherTransformationString));
		MethodCallWriter cipherGetInstanceMethod = constantPoolManager.getOrCreateMethodRef("javax/crypto/Cipher", "getInstance", "(Ljava/lang/String;)Ljavax/crypto/Cipher;", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, cipherGetInstanceMethod));
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(AbstractInsnNode.loadObject(9, localVariableTable, 11));
		instructions.add(InsnNode.of(ICONST_1));
		StringNode algorithmString = constantPoolManager.createStringNode("DES", cpEntries, false);
		instructions.add(new WideConstantPoolInsnNode(LDC_W, algorithmString));
		MethodCallWriter keyFactoryGetInstanceMethod = constantPoolManager.getOrCreateMethodRef(
			"javax/crypto/SecretKeyFactory", "getInstance", "(Ljava/lang/String;)Ljavax/crypto/SecretKeyFactory;", cpEntries, workspace, classProvider
		);
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, keyFactoryGetInstanceMethod));
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(AbstractInsnNode.loadObject(9, localVariableTable, 11));
		instructions.add(InsnNode.of(ICONST_2));
		ClassInfoWriter ivParameterSpecClass = constantPoolManager.getOrCreateClassInfo("javax/crypto/spec/IvParameterSpec", cpEntries);
		instructions.add(new NewNode(ivParameterSpecClass));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.newInt(8));
		instructions.add(new NewArrayNode(8));
		MethodCallWriter ivParameterSpecConstructorMethod = constantPoolManager.getOrCreateMethodRef("javax/crypto/spec/IvParameterSpec", "<init>", "([B)V", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, ivParameterSpecConstructorMethod));
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, this.threadLocalMapField));
		instructions.add(AbstractInsnNode.loadObject(8, localVariableTable, 11));
		instructions.add(AbstractInsnNode.loadObject(9, localVariableTable, 11));
		InterfaceMethodCallWriter mapPutMethod = constantPoolManager.getOrCreateInterfaceMethod(
			"java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", cpEntries, workspace, classProvider
		);
		instructions.add(new InvokeInterfaceNode(mapPutMethod));
		instructions.add(InsnNode.of(POP));
		instructions.add(cacheHitLabel);
		ClassInfoWriter desKeySpecClass = constantPoolManager.getOrCreateClassInfo("javax/crypto/spec/DESKeySpec", cpEntries);
		instructions.add(new NewNode(desKeySpecClass));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.loadObject(4, localVariableTable, 11));
		MethodCallWriter desKeySpecConstructorMethod = constantPoolManager.getOrCreateMethodRef("javax/crypto/spec/DESKeySpec", "<init>", "([B)V", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, desKeySpecConstructorMethod));
		instructions.add(AbstractInsnNode.storeObject(11, localVariableTable, 11));
		instructions.add(AbstractInsnNode.loadObject(9, localVariableTable, 11));
		instructions.add(InsnNode.of(ICONST_1));
		instructions.add(InsnNode.of(AALOAD));
		ClassInfoWriter secretKeyFactoryClass = constantPoolManager.getOrCreateClassInfo("javax/crypto/SecretKeyFactory", cpEntries);
		instructions.add(new WideConstantPoolInsnNode(CHECKCAST, secretKeyFactoryClass));
		instructions.add(AbstractInsnNode.loadObject(11, localVariableTable, 11));
		MethodCallWriter generateSecretMethod = constantPoolManager.getOrCreateMethodRef(
			"javax/crypto/SecretKeyFactory", "generateSecret", "(Ljava/security/spec/KeySpec;)Ljavax/crypto/SecretKey;", cpEntries, workspace, classProvider
		);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, generateSecretMethod));
		instructions.add(AbstractInsnNode.storeObject(12, localVariableTable, 11));
		instructions.add(AbstractInsnNode.loadObject(9, localVariableTable, 11));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(InsnNode.of(AALOAD));
		ClassInfoWriter cipherClass = constantPoolManager.getOrCreateClassInfo("javax/crypto/Cipher", cpEntries);
		instructions.add(new WideConstantPoolInsnNode(CHECKCAST, cipherClass));
		instructions.add(AbstractInsnNode.storeObject(13, localVariableTable, 11));
		instructions.add(AbstractInsnNode.loadObject(13, localVariableTable, 11));
		instructions.add(AbstractInsnNode.newInt(2));
		instructions.add(AbstractInsnNode.loadObject(12, localVariableTable, 11));
		instructions.add(AbstractInsnNode.loadObject(9, localVariableTable, 11));
		instructions.add(InsnNode.of(ICONST_2));
		instructions.add(InsnNode.of(AALOAD));
		instructions.add(new WideConstantPoolInsnNode(CHECKCAST, ivParameterSpecClass));
		MethodCallWriter cipherInitMethod = constantPoolManager.getOrCreateMethodRef(
			"javax/crypto/Cipher", "init", "(ILjava/security/Key;Ljava/security/spec/AlgorithmParameterSpec;)V", cpEntries, workspace, classProvider
		);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, cipherInitMethod));
		instructions.add(AbstractInsnNode.loadObject(13, localVariableTable, 11));
		instructions.add(AbstractInsnNode.loadObject(7, localVariableTable, 11));
		MethodCallWriter cipherDoFinalMethod = constantPoolManager.getOrCreateMethodRef("javax/crypto/Cipher", "doFinal", "([B)[B", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, cipherDoFinalMethod));
		instructions.add(AbstractInsnNode.storeObject(10, localVariableTable, 11));
		instructions.add(tryEndLabel);
		instructions.add(new GotoNode(afterTryLabel));
		instructions.add(catchHandlerLabel);
		instructions.add(AbstractInsnNode.storeObject(11, localVariableTable, 11));
		ClassInfoWriter runtimeExceptionClass = constantPoolManager.getOrCreateClassInfo("java/lang/RuntimeException", cpEntries);
		instructions.add(new NewNode(runtimeExceptionClass));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.ldcString(constantPoolManager.getOwnerInternalName(), constantPoolManager, cpEntries, false));
		instructions.add(AbstractInsnNode.loadObject(11, localVariableTable, 11));
		MethodCallWriter runtimeExceptionConstructorMethod = constantPoolManager.getOrCreateMethodRef("java/lang/RuntimeException", "<init>", "(Ljava/lang/String;Ljava/lang/Throwable;)V", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, runtimeExceptionConstructorMethod));
		instructions.add(InsnNode.of(ATHROW));
		instructions.add(afterTryLabel);
		instructions.add(AbstractInsnNode.loadObject(10, localVariableTable, 11));
		instructions.add(InsnNode.of(ICONST_4));
		instructions.add(InsnNode.of(BALOAD));
		instructions.add(AbstractInsnNode.newInt(255));
		instructions.add(InsnNode.of(IAND));
		instructions.add(AbstractInsnNode.newInt(24));
		instructions.add(InsnNode.of(ISHL));
		instructions.add(AbstractInsnNode.loadObject(10, localVariableTable, 11));
		instructions.add(InsnNode.of(ICONST_5));
		instructions.add(InsnNode.of(BALOAD));
		instructions.add(AbstractInsnNode.newInt(255));
		instructions.add(InsnNode.of(IAND));
		instructions.add(AbstractInsnNode.newInt(16));
		instructions.add(InsnNode.of(ISHL));
		instructions.add(InsnNode.of(IOR));
		instructions.add(AbstractInsnNode.loadObject(10, localVariableTable, 11));
		instructions.add(AbstractInsnNode.newInt(6));
		instructions.add(InsnNode.of(BALOAD));
		instructions.add(AbstractInsnNode.newInt(255));
		instructions.add(InsnNode.of(IAND));
		instructions.add(AbstractInsnNode.newInt(8));
		instructions.add(InsnNode.of(ISHL));
		instructions.add(InsnNode.of(IOR));
		instructions.add(AbstractInsnNode.loadObject(10, localVariableTable, 11));
		instructions.add(AbstractInsnNode.newInt(7));
		instructions.add(InsnNode.of(BALOAD));
		instructions.add(AbstractInsnNode.newInt(255));
		instructions.add(InsnNode.of(IAND));
		instructions.add(InsnNode.of(IOR));
		instructions.add(AbstractInsnNode.storeInt(11, localVariableTable, 11));
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, this.encryptedIntegersField));
		instructions.add(AbstractInsnNode.loadInt(3, localVariableTable, 11));
		if (this.targetClass.isJava5OrNewer()) {
			instructions.add(AbstractInsnNode.loadInt(11, localVariableTable, 11));
			MethodCallWriter integerValueOfMethod = constantPoolManager.getOrCreateMethodRef("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, integerValueOfMethod));
		} else {
			ClassInfoWriter integerClass = constantPoolManager.getOrCreateClassInfo("java/lang/Integer", cpEntries);
			instructions.add(new NewNode(integerClass));
			instructions.add(InsnNode.of(DUP));
			instructions.add(AbstractInsnNode.loadInt(11, localVariableTable, 11));
			MethodCallWriter integerConstructorMethod = constantPoolManager.getOrCreateMethodRef("java/lang/Integer", "<init>", "(I)V", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, integerConstructorMethod));
		}

		instructions.add(InsnNode.of(AASTORE));
		instructions.add(alreadyDecryptedLabel);
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, this.encryptedIntegersField));
		instructions.add(AbstractInsnNode.loadInt(3, localVariableTable, 11));
		instructions.add(InsnNode.of(AALOAD));
		MethodCallWriter integerIntValueMethod = constantPoolManager.getOrCreateMethodRef("java/lang/Integer", "intValue", "()I", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, integerIntValueMethod));
		instructions.add(InsnNode.of(IRETURN));
	}

	public void generateDesInitCode(
		LocalVariableTable localVariableTable,
		List<AbstractInsnNode> instructions,
		int cipherLocalIndex,
		Long encryptionKey,
		LocalVariableOperand keyOperand,
		LocalVariableIndexCounter localVariableIndexCounter,
		List<TypeWriter> cpEntries,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		ArrayList<AbstractInsnNode> instructionBuffer = new ArrayList<>();
		ConstantPoolManager constantPoolManager = this.targetClass.getConstantPoolManager();
		StringNode cipherTransformationString = constantPoolManager.createStringNode("DES/CBC/NoPadding", cpEntries, false);
		instructionBuffer.add(new WideConstantPoolInsnNode(LDC_W, cipherTransformationString));
		MethodCallWriter cipherGetInstanceMethod = constantPoolManager.getOrCreateMethodRef("javax/crypto/Cipher", "getInstance", "(Ljava/lang/String;)Ljavax/crypto/Cipher;", cpEntries, workspace, classProvider);
		instructionBuffer.add(new WideConstantPoolInsnNode(INVOKESTATIC, cipherGetInstanceMethod));
		instructionBuffer.add(InsnNode.of(DUP));
		instructionBuffer.add(AbstractInsnNode.storeObject(cipherLocalIndex, localVariableTable, 11));
		instructionBuffer.add(AbstractInsnNode.newInt(2));
		StringNode algorithmString = constantPoolManager.createStringNode("DES", cpEntries, false);
		instructionBuffer.add(new WideConstantPoolInsnNode(LDC_W, algorithmString));
		MethodCallWriter keyFactoryGetInstanceMethod = constantPoolManager.getOrCreateMethodRef(
			"javax/crypto/SecretKeyFactory", "getInstance", "(Ljava/lang/String;)Ljavax/crypto/SecretKeyFactory;", cpEntries, workspace, classProvider
		);
		instructionBuffer.add(new WideConstantPoolInsnNode(INVOKESTATIC, keyFactoryGetInstanceMethod));
		instructionBuffer.add(AbstractInsnNode.newInt(8));
		instructionBuffer.add(new NewArrayNode(8));
		LabelNode loopStartLabel = new LabelNode(true, 1);
		LabelNode loopEndLabel = new LabelNode(true, 1);
		int byteIndex = localVariableIndexCounter.getAndIncrease();
		instructionBuffer.add(InsnNode.of(DUP));
		instructionBuffer.add(InsnNode.of(ICONST_0));
		instructionBuffer.add(AbstractInsnNode.loadLong(keyOperand.getIndex(), localVariableTable, 11));
		instructionBuffer.add(AbstractInsnNode.newInt(56));
		instructionBuffer.add(InsnNode.of(LUSHR));
		instructionBuffer.add(InsnNode.of(L2I));
		instructionBuffer.add(InsnNode.of(I2B));
		instructionBuffer.add(InsnNode.of(BASTORE));
		instructionBuffer.add(InsnNode.of(ICONST_1));
		instructionBuffer.add(AbstractInsnNode.storeInt(byteIndex, localVariableTable, 11));
		instructionBuffer.add(loopStartLabel);
		instructionBuffer.add(AbstractInsnNode.loadInt(byteIndex, localVariableTable, 11));
		instructionBuffer.add(AbstractInsnNode.newInt(8));
		instructionBuffer.add(new JumpInsnNode(IF_ICMPGE, loopEndLabel));
		instructionBuffer.add(InsnNode.of(DUP));
		instructionBuffer.add(AbstractInsnNode.loadInt(byteIndex, localVariableTable, 11));
		instructionBuffer.add(AbstractInsnNode.loadLong(keyOperand.getIndex(), localVariableTable, 11));
		instructionBuffer.add(AbstractInsnNode.loadInt(byteIndex, localVariableTable, 11));
		instructionBuffer.add(AbstractInsnNode.newInt(8));
		instructionBuffer.add(InsnNode.of(IMUL));
		instructionBuffer.add(InsnNode.of(LSHL));
		instructionBuffer.add(AbstractInsnNode.newInt(56));
		instructionBuffer.add(InsnNode.of(LUSHR));
		instructionBuffer.add(InsnNode.of(L2I));
		instructionBuffer.add(InsnNode.of(I2B));
		instructionBuffer.add(InsnNode.of(BASTORE));
		instructionBuffer.add(AbstractInsnNode.IINC(byteIndex, 1, localVariableTable, 11));
		instructionBuffer.add(new GotoNode(loopStartLabel));
		instructionBuffer.add(loopEndLabel);
		ClassInfoWriter desKeySpecClass = constantPoolManager.getOrCreateClassInfo("javax/crypto/spec/DESKeySpec", cpEntries);
		instructionBuffer.add(new NewNode(desKeySpecClass));
		instructionBuffer.add(InsnNode.of(DUP_X1));
		instructionBuffer.add(InsnNode.of(SWAP));
		MethodCallWriter desKeySpecConstructorMethod = constantPoolManager.getOrCreateMethodRef("javax/crypto/spec/DESKeySpec", "<init>", "([B)V", cpEntries, workspace, classProvider);
		instructionBuffer.add(new WideConstantPoolInsnNode(INVOKESPECIAL, desKeySpecConstructorMethod));
		MethodCallWriter generateSecretMethod = constantPoolManager.getOrCreateMethodRef(
			"javax/crypto/SecretKeyFactory", "generateSecret", "(Ljava/security/spec/KeySpec;)Ljavax/crypto/SecretKey;", cpEntries, workspace, classProvider
		);
		instructionBuffer.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, generateSecretMethod));
		ClassInfoWriter ivParameterSpecClass = constantPoolManager.getOrCreateClassInfo("javax/crypto/spec/IvParameterSpec", cpEntries);
		instructionBuffer.add(new NewNode(ivParameterSpecClass));
		instructionBuffer.add(InsnNode.of(DUP));
		instructionBuffer.add(AbstractInsnNode.newInt(8));
		instructionBuffer.add(new NewArrayNode(8));
		MethodCallWriter ivParameterSpecConstructorMethod = constantPoolManager.getOrCreateMethodRef("javax/crypto/spec/IvParameterSpec", "<init>", "([B)V", cpEntries, workspace, classProvider);
		instructionBuffer.add(new WideConstantPoolInsnNode(INVOKESPECIAL, ivParameterSpecConstructorMethod));
		MethodCallWriter cipherInitMethod = constantPoolManager.getOrCreateMethodRef(
			"javax/crypto/Cipher", "init", "(ILjava/security/Key;Ljava/security/spec/AlgorithmParameterSpec;)V", cpEntries, workspace, classProvider
		);
		instructionBuffer.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, cipherInitMethod));
		instructions.addAll(instructionBuffer);
	}

	public FieldTypeWriter getThreadLocalMapField() {
		return this.threadLocalMapField;
	}

	private void generateIndyCallSiteCode(
		LocalVariableTable localVariableTable,
		ArrayList<AbstractInsnNode> instructions,
		AbstractMethodCallWriter bootstrapMethod,
		TryCatchBlockInfo[] tryCatchBlockInfos,
		List<TypeWriter> cpEntries,
		ConstantPoolManager constantPoolManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		LabelNode tryStartLabel = new LabelNode(true, 128);
		LabelNode tryEndLabel = new LabelNode(true, 128);
		LabelNode catchHandlerLabel = new LabelNode(true, 128);
		LabelNode afterTryLabel = new LabelNode(true, 1);
		ClassInfoWriter exceptionClass = constantPoolManager.getOrCreateClassInfo("java/lang/Exception", cpEntries);
		tryCatchBlockInfos[0] = new TryCatchBlockInfo(exceptionClass, tryStartLabel, tryEndLabel, catchHandlerLabel);
		ClassInfoWriter mutableCallSiteClass = constantPoolManager.getOrCreateClassInfo("java/lang/invoke/MutableCallSite", cpEntries);
		instructions.add(new NewNode(mutableCallSiteClass));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.loadObject(2, localVariableTable, 11));
		MethodCallWriter callSiteConstructorMethod = constantPoolManager.getOrCreateMethodRef("java/lang/invoke/MutableCallSite", "<init>", "(Ljava/lang/invoke/MethodType;)V", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, callSiteConstructorMethod));
		instructions.add(AbstractInsnNode.storeObject(3, localVariableTable, 11));
		instructions.add(tryStartLabel);
		instructions.add(AbstractInsnNode.loadObject(3, localVariableTable, 11));
		ResolvedMethodHandleWriter bootstrapMethodHandle = constantPoolManager.getOrCreateMethodHandle(MethodHandleKind.REF_INVOKE_STATIC, bootstrapMethod, cpEntries);
		instructions.add(new WideConstantPoolInsnNode(LDC_W, bootstrapMethodHandle));
		instructions.add(AbstractInsnNode.ldcClass("[Ljava/lang/Object;", constantPoolManager, cpEntries));
		instructions.add(AbstractInsnNode.loadObject(2, localVariableTable, 11));
		MethodCallWriter parameterCountMethod = constantPoolManager.getOrCreateMethodRef("java/lang/invoke/MethodType", "parameterCount", "()I", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, parameterCountMethod));
		MethodCallWriter asCollectorMethod = constantPoolManager.getOrCreateMethodRef(
			"java/lang/invoke/MethodHandle", "asCollector", "(Ljava/lang/Class;I)Ljava/lang/invoke/MethodHandle;", cpEntries, workspace, classProvider
		);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, asCollectorMethod));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(InsnNode.of(ICONST_3));
		ClassInfoWriter objectArrayElementClass = constantPoolManager.getOrCreateClassInfo("java/lang/Object", cpEntries);
		instructions.add(new WideConstantPoolInsnNode(ANEWARRAY, objectArrayElementClass));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(AbstractInsnNode.loadObject(0, localVariableTable, 11));
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.newInt(1));
		instructions.add(AbstractInsnNode.loadObject(3, localVariableTable, 11));
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.newInt(2));
		instructions.add(AbstractInsnNode.loadObject(1, localVariableTable, 11));
		instructions.add(InsnNode.of(AASTORE));
		MethodCallWriter insertArgumentsMethod = constantPoolManager.getOrCreateMethodRef(
			"java/lang/invoke/MethodHandles",
			"insertArguments",
			"(Ljava/lang/invoke/MethodHandle;I[Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;",
			cpEntries,
			workspace,
			classProvider
		);
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, insertArgumentsMethod));
		instructions.add(AbstractInsnNode.loadObject(2, localVariableTable, 11));
		MethodCallWriter explicitCastArgumentsMethod = constantPoolManager.getOrCreateMethodRef(
			"java/lang/invoke/MethodHandles",
			"explicitCastArguments",
			"(Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;",
			cpEntries,
			workspace,
			classProvider
		);
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, explicitCastArgumentsMethod));
		MethodCallWriter setTargetMethod = constantPoolManager.getOrCreateMethodRef("java/lang/invoke/MutableCallSite", "setTarget", "(Ljava/lang/invoke/MethodHandle;)V", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, setTargetMethod));
		instructions.add(tryEndLabel);
		instructions.add(new GotoNode(afterTryLabel));
		instructions.add(catchHandlerLabel);
		instructions.add(AbstractInsnNode.storeObject(4, localVariableTable, 11));
		ClassInfoWriter runtimeExceptionClass = constantPoolManager.getOrCreateClassInfo("java/lang/RuntimeException", cpEntries);
		instructions.add(new NewNode(runtimeExceptionClass));
		instructions.add(InsnNode.of(DUP));
		ClassInfoWriter stringBuilderClass = constantPoolManager.getOrCreateClassInfo("java/lang/StringBuilder", cpEntries);
		instructions.add(new NewNode(stringBuilderClass));
		instructions.add(InsnNode.of(DUP));
		MethodCallWriter stringBuilderConstructorMethod = constantPoolManager.getOrCreateMethodRef("java/lang/StringBuilder", "<init>", "()V", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, stringBuilderConstructorMethod));
		instructions.add(AbstractInsnNode.ldcString(constantPoolManager.getOwnerInternalName(), constantPoolManager, cpEntries, false));
		MethodCallWriter stringBuilderAppendMethod = constantPoolManager.getOrCreateMethodRef("java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, stringBuilderAppendMethod));
		StringNode nameSeparatorString = constantPoolManager.createStringNode(" : ", cpEntries, false);
		instructions.add(new WideConstantPoolInsnNode(LDC_W, nameSeparatorString));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, stringBuilderAppendMethod));
		instructions.add(AbstractInsnNode.loadObject(1, localVariableTable, 11));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, stringBuilderAppendMethod));
		StringNode typeSeparatorString = constantPoolManager.createStringNode(" : ", cpEntries, false);
		instructions.add(new WideConstantPoolInsnNode(LDC_W, typeSeparatorString));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, stringBuilderAppendMethod));
		instructions.add(AbstractInsnNode.loadObject(2, localVariableTable, 11));
		MethodCallWriter methodTypeToStringMethod = constantPoolManager.getOrCreateMethodRef("java/lang/invoke/MethodType", "toString", "()Ljava/lang/String;", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, methodTypeToStringMethod));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, stringBuilderAppendMethod));
		MethodCallWriter stringBuilderToStringMethod = constantPoolManager.getOrCreateMethodRef("java/lang/StringBuilder", "toString", "()Ljava/lang/String;", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, stringBuilderToStringMethod));
		instructions.add(AbstractInsnNode.loadObject(4, localVariableTable, 11));
		MethodCallWriter runtimeExceptionConstructorMethod = constantPoolManager.getOrCreateMethodRef("java/lang/RuntimeException", "<init>", "(Ljava/lang/String;Ljava/lang/Throwable;)V", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, runtimeExceptionConstructorMethod));
		instructions.add(InsnNode.of(ATHROW));
		instructions.add(afterTryLabel);
		instructions.add(AbstractInsnNode.loadObject(3, localVariableTable, 11));
		instructions.add(InsnNode.of(ARETURN));
	}

	public void initThreadLocalMapField(List<AbstractInsnNode> instructions, List<TypeWriter> cpEntries, ConstantPoolManager constantPoolManager, AbstractWorkspace workspace, ClassProvider classProvider) {
		ClassInfoWriter hashMapClass = constantPoolManager.getOrCreateClassInfo("java/util/HashMap", cpEntries);
		instructions.add(new NewNode(hashMapClass));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.newInt(13));
		MethodCallWriter hashMapConstructorMethod = constantPoolManager.getOrCreateMethodRef("java/util/HashMap", "<init>", "(I)V", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, hashMapConstructorMethod));
		instructions.add(new WideConstantPoolInsnNode(PUTSTATIC, this.threadLocalMapField));
	}

	public long packIntWithRandomPrefix(int value) {
		byte[] valueBytes = ByteUtils.unpackInt(value);
		byte[] prefixBytes = new byte[4];
		this.random.nextBytes(prefixBytes);
		return (prefixBytes[0] & 255L) << 56
			| (prefixBytes[1] & 255L) << 48
			| (prefixBytes[2] & 255L) << 40
			| (prefixBytes[3] & 255L) << 32
			| (valueBytes[0] & 255L) << 24
			| (valueBytes[1] & 255L) << 16
			| (valueBytes[2] & 255L) << 8
			| valueBytes[3] & 255L;
	}

	public AbstractMethodCallWriter getDecryptMethod(ClassNode classNode) {
		return this.indyDecryptMethod != null ? this.indyDecryptMethod : this.decryptMethod;
	}

	public InvokeDynamicCallWriter getIndyCallSite(ClassNode classNode) {
		return this.indyCallSite;
	}

	private void resetForClass(ClassNode classNode) {
		this.targetClass = classNode;
		this.xorKey = 0;
		this.globalKey = null;
		this.indyCallSite = null;
		this.decryptMethod = null;
		this.indyDecryptMethod = null;
		this.encryptedIntegersField = null;
		this.threadLocalMapField = null;
	}

	public void setupEncryption(
		ClassNode classNode,
		Inheritance inheritance,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		List<TypeWriter> cpEntries,
		FieldTypeWriter encryptedLongsArrayField,
		boolean hasParameterChanger,
		boolean usesLongArray,
		boolean useIndy,
		boolean useDesEncryption
	) {
		this.resetForClass(classNode);
		ConstantPoolManager constantPoolManager = this.targetClass.getConstantPoolManager();
		if (useDesEncryption) {
			if (encryptedLongsArrayField != null && usesLongArray) {
				this.xorKey = this.random.nextInt(32766) + 1;
				FieldNode desIntegersArrayFieldNode = this.targetClass.createUniqueField("[Ljava/lang/Integer;", this.targetClass.isInterface() ? 4 : 1, true, inheritance, workspace, 11);
				this.encryptedIntegersField = constantPoolManager.getOrCreateFieldRef(
					this.targetClass.getInternalName(), desIntegersArrayFieldNode.getNameDisplay(), desIntegersArrayFieldNode.getDescriptor(), cpEntries, workspace, classProvider, true
				);
				FieldNode cacheMapFieldNode = this.targetClass.createUniqueField("Ljava/util/Map;", this.targetClass.isInterface() ? 4 : 1, true, inheritance, workspace, 11);
				this.threadLocalMapField = constantPoolManager.getOrCreateFieldRef(this.targetClass.getInternalName(), cacheMapFieldNode.getNameDisplay(), cacheMapFieldNode.getDescriptor(), cpEntries, workspace, classProvider, true);
				TryCatchBlockInfo[] desDecryptTryCatchBlocks = new TryCatchBlockInfo[1];
				LocalVariableTable desDecryptLocalVariables = new LocalVariableTable(true, "(IJ)I", 14);
				ArrayList<AbstractInsnNode> desDecryptInstructions = new ArrayList<>();
				this.generateDesDecryptionCode(desDecryptLocalVariables, desDecryptInstructions, usesLongArray, encryptedLongsArrayField, desDecryptTryCatchBlocks, cpEntries, constantPoolManager, workspace, classProvider);
				MethodNode desDecryptMethodNode = this.targetClass.createUniqueMethod("(IJ)I", desDecryptInstructions, 6, 14, 1, desDecryptLocalVariables, desDecryptTryCatchBlocks, "Integer Constant Encryption", cpEntries, inheritance, workspace, 11);
				this.indyDecryptMethod = this.targetClass.getOrCreateAnyMethodRef(desDecryptMethodNode, cpEntries);
				if (useIndy) {
					LocalVariableTable bootstrapLocalVariables = new LocalVariableTable(
						true, "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/invoke/MutableCallSite;Ljava/lang/String;[Ljava/lang/Object;)I", 9
					);
					ArrayList<AbstractInsnNode> bootstrapInstructions = new ArrayList<>();
					this.generateIndyBootstrapCode(bootstrapLocalVariables, bootstrapInstructions, this.indyDecryptMethod, cpEntries, constantPoolManager, workspace, classProvider);
					MethodNode bootstrapMethodNode = this.targetClass
						.createUniqueMethod(
							"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/invoke/MutableCallSite;Ljava/lang/String;[Ljava/lang/Object;)I",
							bootstrapInstructions,
							7,
							9,
							1,
							bootstrapLocalVariables,
							new TryCatchBlockInfo[0],
							"Integer Constant Encryption",
							cpEntries,
							inheritance,
							workspace,
							11
						);
					AbstractMethodCallWriter bootstrapMethod = this.targetClass.getOrCreateAnyMethodRef(bootstrapMethodNode, cpEntries);
					TryCatchBlockInfo[] callSiteTryCatchBlocks = new TryCatchBlockInfo[1];
					LocalVariableTable callSiteLocalVariables = new LocalVariableTable(
						true, "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", 5
					);
					ArrayList<AbstractInsnNode> callSiteInstructions = new ArrayList<>();
					this.generateIndyCallSiteCode(callSiteLocalVariables, callSiteInstructions, bootstrapMethod, callSiteTryCatchBlocks, cpEntries, constantPoolManager, workspace, classProvider);
					MethodNode callSiteMethodNode = this.targetClass
						.createUniqueMethod(
							"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
							callSiteInstructions,
							7,
							5,
							1,
							callSiteLocalVariables,
							callSiteTryCatchBlocks,
							"Integer Constant Encryption",
							cpEntries,
							inheritance,
							workspace,
							11
						);
					AbstractMethodCallWriter callSiteBootstrapMethod = this.targetClass.getOrCreateAnyMethodRef(callSiteMethodNode, cpEntries);
					String callSiteName = String.valueOf((char)(97 + this.random.nextInt(26)));
					this.indyCallSite = this.targetClass.createInvokeDynamicCall(callSiteName, "(IJ)I", callSiteBootstrapMethod, cpEntries, constantPoolManager);
				}
			}
		} else if (encryptedLongsArrayField != null) {
			if (usesLongArray) {
				this.xorKey = this.random.nextInt(32766) + 1;
				this.globalKey = this.randomLongIterator.next();
				FieldNode plainIntegersArrayFieldNode = this.targetClass.createUniqueField("[Ljava/lang/Integer;", this.targetClass.isInterface() ? 4 : 1, true, inheritance, workspace, 11);
				this.encryptedIntegersField = constantPoolManager.getOrCreateFieldRef(
					this.targetClass.getInternalName(), plainIntegersArrayFieldNode.getNameDisplay(), plainIntegersArrayFieldNode.getDescriptor(), cpEntries, workspace, classProvider, true
				);
				TryCatchBlockInfo[] plainDecryptTryCatchBlocks = new TryCatchBlockInfo[0];
				LocalVariableTable plainDecryptLocalVariables = new LocalVariableTable(true, "(IJ)I", 4);
				ArrayList<AbstractInsnNode> plainDecryptInstructions = new ArrayList<>();
				this.generateDecryptionCode(plainDecryptLocalVariables, plainDecryptInstructions, encryptedLongsArrayField, cpEntries, constantPoolManager, workspace, classProvider);
				MethodNode plainDecryptMethodNode = this.targetClass.createUniqueMethod("(IJ)I", plainDecryptInstructions, 8, 4, 1, plainDecryptLocalVariables, plainDecryptTryCatchBlocks, "Integer Constant Encryption", cpEntries, inheritance, workspace, 11);
				this.decryptMethod = this.targetClass.getOrCreateAnyMethodRef(plainDecryptMethodNode, cpEntries);
			} else {
				this.globalKey = this.randomLongIterator.next();
			}
		} else if (usesLongArray) {
			if (!hasParameterChanger) {
				this.globalKey = this.randomLongIterator.next();
			}
		} else if (!hasParameterChanger) {
			this.globalKey = this.randomLongIterator.next();
		}
	}

	public void generateStoreEncryptedLongCode(
		LocalVariableTable localVariableTable,
		List<AbstractInsnNode> instructions,
		FieldTypeWriter encryptedLongsField,
		LongNode encryptedLongNode,
		IntegerEntry integerEntry,
		LabelNode initialSwitchLabel,
		AtomicInt switchCaseCounter,
		MultiMap<LabelNode, PriorityEntry<LabelNode>> switchEntryMap
	) {
		instructions.add(new WideConstantPoolInsnNode(LDC2_W, encryptedLongNode));
		int switchCaseValue = switchCaseCounter.increaseAndGet();
		instructions.add(AbstractInsnNode.newInt(switchCaseValue));
		instructions.add(new GotoNode(initialSwitchLabel));
		LabelNode switchCaseLabel = new LabelNode(true, 1);
		instructions.add(switchCaseLabel);
		switchEntryMap.put(initialSwitchLabel, new PriorityEntry<>(switchCaseValue, switchCaseLabel));
		if (integerEntry.hasArrayField()) {
			instructions.add(new WideConstantPoolInsnNode(PUTSTATIC, encryptedLongsField));
		} else if (integerEntry.hasLocalArrayIndex()) {
			instructions.add(AbstractInsnNode.storeLong(integerEntry.getLocalArrayIndex(), localVariableTable, 11));
		}
	}

	private List<StringNode> packEncryptedValuesToStrings(
		AtomicReference<Integer>[] integerReferences,
		ReferenceGraph<AtomicReference<Integer>, IntegerNode> referenceGraph,
		Map<AtomicReference<Integer>, IntegerEntry> integerEntryMap,
		Set<TypeWriter> constantsToRemove,
		Long encryptionKey,
		boolean useDesEncryption,
		List<TypeWriter> cpEntries,
		ConstantPoolManager constantPoolManager
	) {
		ArrayList<StringNode> packedStrings = new ArrayList<>();
		int referenceCount = integerReferences.length;
		int referenceIndex = 0;

		while (referenceIndex < referenceCount) {
			StringBuilder chunkBuilder = new StringBuilder(referenceCount * 4);
			int chunkLength = 0;

			while (referenceIndex < referenceCount) {
				AtomicReference<Integer> integerReference = integerReferences[referenceIndex];
				IntegerEntry integerEntry = integerEntryMap.get(integerReference);

				for (IntegerNode referencingConstant : referenceGraph.getReferences(integerReference)) {
					constantsToRemove.add(referencingConstant);
				}

				IntegerLookup lookupType = integerEntry.getLookupType();
				long packedValue = this.packIntWithRandomPrefix(integerReference.get());
				short lookupIndex = 0;
				if (lookupType.equals(IntegerLookup.LOOKUP_METHOD_AND_INDEX)) {
					long randomKey = this.randomLongIterator.next();
					integerEntry.setEncryptionKey(randomKey);
					lookupIndex = (short)(referenceIndex ^ this.xorKey);
					packedValue ^= randomKey;
				} else if (lookupType.equals(IntegerLookup.LOOKUP_METHOD_AND_INDEX_DES) || lookupType.equals(IntegerLookup.INDY_ENTRY_AND_INDEX)) {
					long desRandomKey = this.randomLongIterator.next();
					integerEntry.setEncryptionKey(desRandomKey);
					lookupIndex = (short)(referenceIndex ^ this.xorKey);
					packedValue = this.combineWithKey(packedValue, desRandomKey);
				}

				long encryptedLong;
				if (encryptionKey != null) {
					if (useDesEncryption) {
						encryptedLong = this.combineWithKey(packedValue, encryptionKey);
					} else {
						encryptedLong = packedValue ^ encryptionKey;
					}
				} else {
					encryptedLong = packedValue ^ this.globalKey;
				}

				byte[] encryptedBytes = ByteUtils.unpackLong(encryptedLong);

				String encryptedString;
				try {
					encryptedString = new String(encryptedBytes, "ISO-8859-1");
				} catch (UnsupportedEncodingException exception) {
					throw new ProcessingException(exception.getMessage(), exception);
				}

				int utf8Length = GlobalUtils.getUTF8Length(encryptedString);
				if (chunkLength > 0 && chunkLength + utf8Length > 32768) {
					break;
				}

				chunkLength += utf8Length;
				referenceIndex++;
				chunkBuilder.append(encryptedString);
				if (lookupType.equals(IntegerLookup.LOOKUP_METHOD_AND_INDEX)
					|| lookupType.equals(IntegerLookup.LOOKUP_METHOD_AND_INDEX_DES)
					|| lookupType.equals(IntegerLookup.INDY_ENTRY_AND_INDEX)) {
					integerEntry.setIndex(lookupIndex);
				}

				if (packedStrings.size() == 0 && referenceCount > 3 && referenceIndex == referenceCount - 2) {
					break;
				}
			}

			StringNode packedString = constantPoolManager.createString(chunkBuilder.toString(), cpEntries);
			packedStrings.add(packedString);
		}

		return packedStrings;
	}

	private void generateXorVariantCode(
		LocalVariableTable localVariableTable,
		List<AbstractInsnNode> instructions,
		List<TypeWriter> cpEntries,
		Long encryptionKey,
		LocalVariableOperand keyOperand,
		LocalVariableIndexCounter localVariableIndexCounter,
		Integer cipherLocalIndex,
		Long globalKey,
		Integer longLocalIndex,
		ConstantPoolManager constantPoolManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		if (cipherLocalIndex != null) {
			this.encryptLongToBytesAndBack(localVariableTable, instructions, cpEntries, cipherLocalIndex, localVariableIndexCounter, workspace, classProvider);
		} else if (keyOperand != null) {
			this.generateXorCode(localVariableTable, instructions, cpEntries, encryptionKey, keyOperand.getIndex(), localVariableIndexCounter, constantPoolManager, workspace, classProvider);
		} else {
			this.generateXorCode(localVariableTable, instructions, cpEntries, globalKey, longLocalIndex, localVariableIndexCounter, constantPoolManager, workspace, classProvider);
		}
	}

	public void generateEncryptedValueSwitch(
		LocalVariableTable localVariableTable,
		LabelNode initialSwitchLabel,
		Long key,
		LocalVariableOperand keyOperand,
		LocalVariableIndexCounter localVariableIndexCounter,
		List<AbstractInsnNode> instructions,
		List<TypeWriter> cpEntries,
		MultiMap<LabelNode, PriorityEntry<LabelNode>> switchEntryMap,
		Integer cipherIndex,
		Integer keyIndex,
		ConstantPoolManager constantPoolManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		ArrayList<AbstractInsnNode> instructionBuffer = new ArrayList<>();
		LabelNode afterSwitchLabel = new LabelNode(true, 1);
		instructionBuffer.add(new GotoNode(afterSwitchLabel));
		instructionBuffer.add(initialSwitchLabel);
		List<PriorityEntry<LabelNode>> switchEntries = switchEntryMap.get(initialSwitchLabel);
		if (switchEntries.size() > 1) {
			Collections.sort(switchEntries);
			LabelNode defaultLabel = switchEntries.get(0).getValue();
			LabelNode[] targetLabels = new LabelNode[switchEntries.size() - 1];

			for (int entryIndex = 1; entryIndex < switchEntries.size(); entryIndex++) {
				targetLabels[entryIndex - 1] = switchEntries.get(entryIndex).getValue();
			}

			instructionBuffer.add(InsnNode.of(DUP_X2));
			instructionBuffer.add(InsnNode.of(POP));
			this.generateXorVariantCode(localVariableTable, instructionBuffer, cpEntries, key, keyOperand, localVariableIndexCounter, cipherIndex, this.globalKey, keyIndex, constantPoolManager, workspace, classProvider);
			instructionBuffer.add(InsnNode.of(DUP2_X1));
			instructionBuffer.add(InsnNode.of(POP2));
			instructionBuffer.add(new TableSwitchNode(defaultLabel, 0, targetLabels.length - 1, targetLabels));
		} else {
			LabelNode targetLabel = switchEntries.get(0).getValue();
			instructionBuffer.add(InsnNode.of(DUP_X2));
			instructionBuffer.add(InsnNode.of(POP));
			this.generateXorVariantCode(localVariableTable, instructionBuffer, cpEntries, key, keyOperand, localVariableIndexCounter, cipherIndex, this.globalKey, keyIndex, constantPoolManager, workspace, classProvider);
			instructionBuffer.add(InsnNode.of(DUP2_X1));
			instructionBuffer.add(InsnNode.of(POP2));
			instructionBuffer.add(InsnNode.of(POP));
			instructionBuffer.add(new GotoNode(targetLabel));
		}

		instructionBuffer.add(afterSwitchLabel);
		instructions.addAll(instructionBuffer);
	}

	public long combineWithKey(long value, long encryptionKey) {
		return this.desCrypt(value, encryptionKey, false);
	}

	public void generateStoreEncryptedIntegerCode(
		LocalVariableTable localVariableTable, List<AbstractInsnNode> instructions, FieldTypeWriter targetField, IntegerEntry integerEntry, Long encryptionKey, LocalVariableOperand keyOperand, List<TypeWriter> cpEntries
	) {
		IntegerLookup lookupType = integerEntry.getLookupType();
		switch (lookupType) {
			case FIELD_ARRAY_AND_INDEX:
			default:
				break;
			case LOCAL_ARRAY_AND_INDEX:
				if (integerEntry.hasIndex()) {
					instructions.add(InsnNode.of(DUP));
					instructions.add(AbstractInsnNode.newInt(integerEntry.getIndex()));
					instructions.add(InsnNode.of(LALOAD));
				} else {
					instructions.add(InsnNode.of(DUP2));
				}

				instructions.add(InsnNode.of(L2I));
				break;
			case LOOKUP_METHOD_AND_INDEX:
				long lookupEncryptionKey = integerEntry.getEncryptionKey();
				ConstantPoolManager lookupConstantPoolManager = this.targetClass.getConstantPoolManager();
				int lookupIndex = integerEntry.getIndex();
				if (encryptionKey != null && keyOperand != null) {
					AbstractInsnNode.encryptIntConstant(lookupIndex ^ (int)(lookupEncryptionKey & 32767L), instructions, lookupConstantPoolManager, cpEntries);
					instructions.add(AbstractInsnNode.loadLong(keyOperand.getIndex(), localVariableTable, 11));
					long lookupXoredKey = encryptionKey ^ lookupEncryptionKey;
					instructions.add(AbstractInsnNode.ldcLong(lookupXoredKey, lookupConstantPoolManager, cpEntries));
					instructions.add(InsnNode.of(LXOR));
				} else {
					AbstractInsnNode.encryptIntConstant(lookupIndex ^ (int)(lookupEncryptionKey & 32767L), instructions, lookupConstantPoolManager, cpEntries);
					AbstractInsnNode.appendLongConst(lookupEncryptionKey, instructions, lookupConstantPoolManager, cpEntries);
				}

				instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, this.decryptMethod));
				break;
			case LOOKUP_METHOD_AND_INDEX_DES:
				long desEncryptionKey = integerEntry.getEncryptionKey();
				ConstantPoolManager desConstantPoolManager = this.targetClass.getConstantPoolManager();
				int desIndex = integerEntry.getIndex();
				if (encryptionKey != null && keyOperand != null) {
					AbstractInsnNode.encryptIntConstant(desIndex ^ (int)(desEncryptionKey & 32767L), instructions, desConstantPoolManager, cpEntries);
					instructions.add(AbstractInsnNode.loadLong(keyOperand.getIndex(), localVariableTable, 11));
					long desXoredKey = encryptionKey ^ desEncryptionKey;
					instructions.add(AbstractInsnNode.ldcLong(desXoredKey, desConstantPoolManager, cpEntries));
					instructions.add(InsnNode.of(LXOR));
				}

				instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, integerEntry.getMethodCallWriter()));
				break;
			case INDY_ENTRY_AND_INDEX:
				long indyEncryptionKey = integerEntry.getEncryptionKey();
				ConstantPoolManager indyConstantPoolManager = this.targetClass.getConstantPoolManager();
				int indyIndex = integerEntry.getIndex();
				if (encryptionKey != null && keyOperand != null) {
					AbstractInsnNode.encryptIntConstant(indyIndex ^ (int)(indyEncryptionKey & 32767L), instructions, indyConstantPoolManager, cpEntries);
					instructions.add(AbstractInsnNode.loadLong(keyOperand.getIndex(), localVariableTable, 11));
					long indyXoredKey = encryptionKey ^ indyEncryptionKey;
					instructions.add(AbstractInsnNode.ldcLong(indyXoredKey, indyConstantPoolManager, cpEntries));
					instructions.add(InsnNode.of(LXOR));
				}

				instructions.add(new InvokeDynamicNode(integerEntry.getIndyCallWriter()));
		}

		instructions.add(new WideConstantPoolInsnNode(PUTSTATIC, targetField));
	}

	public FieldTypeWriter getEncryptedIntegersField(ClassNode classNode) {
		return this.encryptedIntegersField;
	}

	public long desCrypt(long value, long encryptionKey, boolean decrypt) {
		byte[] keyBytes = ByteUtils.unpackLong(encryptionKey);

		try {
			DESKeySpec desKeySpec = new DESKeySpec(keyBytes);
			if (this.keyFactory == null) {
				this.keyFactory = SecretKeyFactory.getInstance("DES");
			}

			if (this.cipher == null) {
				this.cipher = Cipher.getInstance("DES/CBC/NoPadding");
				this.ivSpec = new IvParameterSpec(new byte[8]);
			}

			SecretKey secretKey = this.keyFactory.generateSecret(desKeySpec);
			this.cipher.init(decrypt ? 2 : 1, secretKey, this.ivSpec);
			byte[] valueBytes = ByteUtils.unpackLong(value);
			return ByteUtils.packLong(this.cipher.doFinal(valueBytes));
		} catch (Exception exception) {
			throw new ProcessingException(exception.toString(), exception);
		}
	}
}
