package com.zelix.tools.longEncryption;

import com.zelix.exceptions.ProcessingException;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.attributes.code.TryCatchBlockInfo;
import com.zelix.framework.constant.ConstantPoolManager;
import com.zelix.framework.constant.LongNode;
import com.zelix.framework.constant.StringNode;
import com.zelix.framework.encryption.LongEntry;
import com.zelix.framework.encryption.LongLookup;
import com.zelix.framework.hierarchy.Inheritance;
import com.zelix.framework.instructions.AbstractInsnNode;
import com.zelix.framework.instructions.GotoNode;
import com.zelix.framework.instructions.InsnNode;
import com.zelix.framework.instructions.InvokeDynamicNode;
import com.zelix.framework.instructions.InvokeInterfaceNode;
import com.zelix.framework.instructions.JumpInsnNode;
import com.zelix.framework.instructions.LabelNode;
import com.zelix.framework.instructions.NewArrayNode;
import com.zelix.framework.instructions.NewNode;
import com.zelix.framework.instructions.TableSwitchNode;
import com.zelix.framework.instructions.WideConstantPoolInsnNode;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.variable.LocalVariableIndexCounter;
import com.zelix.framework.variable.LocalVariableOperand;
import com.zelix.framework.variable.LocalVariableTable;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.FieldTypeWriter;
import com.zelix.framework.writer.InterfaceMethodCallWriter;
import com.zelix.framework.writer.InvokeDynamicCallWriter;
import com.zelix.framework.writer.MethodCallWriter;
import com.zelix.framework.writer.MethodHandleKind;
import com.zelix.framework.writer.ResolvedMethodHandleWriter;
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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.LongStream;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import static com.zelix.framework.instructions.OpcodeConstants.*;

public class LongEncryptionBuilder {
	private Random random = GlobalUtils.newRandom(5023);
	private Iterator<Long> randomLongIterator;
	private FieldTypeWriter threadLocalMapField;
	private SecretKeyFactory secretKeyFactory;
	private InvokeDynamicCallWriter indyCallSiteWriter;
	private int xorKey;
	private ClassNode currentClassNode;
	private Cipher cipher;
	private IvParameterSpec ivParameterSpec;
	private Long globalXorKey;
	private AbstractMethodCallWriter indyDecryptMethodWriter;
	private AbstractMethodCallWriter decryptionMethodWriter;
	private FieldTypeWriter encryptedLongsArrayField;

	public LongEncryptionBuilder() {
		LongStream randomLongStream = this.random.longs(1L, Long.MAX_VALUE);
		this.randomLongIterator = randomLongStream.iterator();
	}

	public static boolean isIndySupported(ClassNode classNode) {
		return Options.ZKM_LONG_ENCRYPT_INDY && ObfuscateReferencesBuilder.indySupported(classNode);
	}

	public void buildStoreEncryptedLong(
		LocalVariableTable localVariableTable,
		List<AbstractInsnNode> instructions,
		FieldTypeWriter encodedLongsField,
		LongNode longNode,
		LongEntry longEntry,
		LabelNode switchLabel,
		AtomicInt caseIdCounter,
		MultiMap<LabelNode, PriorityEntry<LabelNode>> switchCaseMap
	) {
		instructions.add(new WideConstantPoolInsnNode(LDC2_W, longNode));
		int caseId = caseIdCounter.increaseAndGet();
		instructions.add(AbstractInsnNode.newInt(caseId));
		instructions.add(new GotoNode(switchLabel));
		LabelNode caseLabel = new LabelNode(true, 1);
		instructions.add(caseLabel);
		switchCaseMap.put(switchLabel, new PriorityEntry<>(caseId, caseLabel));
		if (longEntry.hasArrayField()) {
			instructions.add(new WideConstantPoolInsnNode(PUTSTATIC, encodedLongsField));
		} else if (longEntry.hasLocalArrayIndex()) {
			instructions.add(AbstractInsnNode.storeLong(longEntry.getLocalArrayIndex(), localVariableTable, 12));
		}
	}

	public void buildStoreWithLookup(
		LocalVariableTable localVariableTable, List<AbstractInsnNode> instructions, FieldTypeWriter targetField, LongEntry longEntry, Long longKey, LocalVariableOperand longKeyOperand, List<TypeWriter> cpEntries
	) {
		LongLookup lookupType = longEntry.getLookupType();
		switch (lookupType) {
			case FIELD_ARRAY_AND_INDEX:
			default:
				break;
			case LOCAL_ARRAY_AND_INDEX:
				if (longEntry.hasIndex()) {
					instructions.add(InsnNode.of(DUP));
					instructions.add(AbstractInsnNode.newInt(longEntry.getIndex()));
					instructions.add(InsnNode.of(LALOAD));
				} else {
					instructions.add(InsnNode.of(DUP2));
				}
				break;
			case LOOKUP_METHOD_AND_INDEX:
				long encryptionKey = longEntry.getEncryptionKey();
				ConstantPoolManager cpManager = this.currentClassNode.getConstantPoolManager();
				int entryIndex = longEntry.getIndex();
				if (longKey != null && longKeyOperand != null) {
					AbstractInsnNode.encryptIntConstant(entryIndex ^ (int)(encryptionKey & 32767L), instructions, cpManager, cpEntries);
					instructions.add(AbstractInsnNode.loadLong(longKeyOperand.getIndex(), localVariableTable, 12));
					long encryptedLongKey = longKey ^ encryptionKey;
					instructions.add(AbstractInsnNode.ldcLong(encryptedLongKey, cpManager, cpEntries));
					instructions.add(InsnNode.of(LXOR));
				} else {
					AbstractInsnNode.encryptIntConstant(entryIndex ^ (int)(encryptionKey & 32767L), instructions, cpManager, cpEntries);
					AbstractInsnNode.appendLongConst(encryptionKey, instructions, cpManager, cpEntries);
				}

				instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, this.decryptionMethodWriter));
				break;
			case LOOKUP_METHOD_AND_INDEX_DES:
				long desEncryptionKey = longEntry.getEncryptionKey();
				ConstantPoolManager desCpManager = this.currentClassNode.getConstantPoolManager();
				int desEntryIndex = longEntry.getIndex();
				if (longKey != null && longKeyOperand != null) {
					AbstractInsnNode.encryptIntConstant(desEntryIndex ^ (int)(desEncryptionKey & 32767L), instructions, desCpManager, cpEntries);
					instructions.add(AbstractInsnNode.loadLong(longKeyOperand.getIndex(), localVariableTable, 12));
					long desEncryptedLongKey = longKey ^ desEncryptionKey;
					instructions.add(AbstractInsnNode.ldcLong(desEncryptedLongKey, desCpManager, cpEntries));
					instructions.add(InsnNode.of(LXOR));
				}

				instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, longEntry.getMethodCallWriter()));
				break;
			case INDY_ENTRY_AND_INDEX:
				long indyEncryptionKey = longEntry.getEncryptionKey();
				ConstantPoolManager indyCpManager = this.currentClassNode.getConstantPoolManager();
				int indyEntryIndex = longEntry.getIndex();
				if (longKey != null && longKeyOperand != null) {
					AbstractInsnNode.encryptIntConstant(indyEntryIndex ^ (int)(indyEncryptionKey & 32767L), instructions, indyCpManager, cpEntries);
					instructions.add(AbstractInsnNode.loadLong(longKeyOperand.getIndex(), localVariableTable, 12));
					long indyEncryptedLongKey = longKey ^ indyEncryptionKey;
					instructions.add(AbstractInsnNode.ldcLong(indyEncryptedLongKey, indyCpManager, cpEntries));
					instructions.add(InsnNode.of(LXOR));
				}

				instructions.add(new InvokeDynamicNode(longEntry.getIndyCallWriter()));
		}

		instructions.add(new WideConstantPoolInsnNode(PUTSTATIC, targetField));
	}

	private void resetBuilder(ClassNode classNode) {
		this.currentClassNode = classNode;
		this.xorKey = 0;
		this.globalXorKey = null;
		this.indyCallSiteWriter = null;
		this.decryptionMethodWriter = null;
		this.indyDecryptMethodWriter = null;
		this.encryptedLongsArrayField = null;
		this.threadLocalMapField = null;
	}

	public void buildIndyEncryptionCall(
		ClassNode classNode,
		Inheritance inheritance,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		List<TypeWriter> cpEntries,
		FieldTypeWriter encodedLongsField,
		boolean hasParameterChanger,
		boolean isArrayField,
		boolean useIndyCallSite,
		boolean useDes
	) {
		this.resetBuilder(classNode);
		ConstantPoolManager cpManager = this.currentClassNode.getConstantPoolManager();
		if (useDes) {
			if (encodedLongsField != null && isArrayField) {
				this.xorKey = this.random.nextInt(32766) + 1;
				FieldNode desEncryptedLongsArrayNode = this.currentClassNode.createUniqueField("[Ljava/lang/Long;", this.currentClassNode.isInterface() ? 4 : 1, true, inheritance, workspace, 12);
				this.encryptedLongsArrayField = cpManager.getOrCreateFieldRef(
					this.currentClassNode.getInternalName(), desEncryptedLongsArrayNode.getNameDisplay(), desEncryptedLongsArrayNode.getDescriptor(), cpEntries, workspace, classProvider, true
				);
				FieldNode desThreadLocalMapNode = this.currentClassNode.createUniqueField("Ljava/util/Map;", this.currentClassNode.isInterface() ? 4 : 1, true, inheritance, workspace, 12);
				this.threadLocalMapField = cpManager.getOrCreateFieldRef(
					this.currentClassNode.getInternalName(), desThreadLocalMapNode.getNameDisplay(), desThreadLocalMapNode.getDescriptor(), cpEntries, workspace, classProvider, true
				);
				TryCatchBlockInfo[] desDecryptTryCatchBlocks = new TryCatchBlockInfo[1];
				LocalVariableTable desDecryptLocalVariables = new LocalVariableTable(true, "(IJ)J", 14);
				ArrayList<AbstractInsnNode> desDecryptInstructions = new ArrayList<>();
				this.buildDesDecryptionWrapper(desDecryptLocalVariables, desDecryptInstructions, isArrayField, encodedLongsField, desDecryptTryCatchBlocks, cpEntries, cpManager, workspace, classProvider);
				MethodNode desDecryptMethodNode = this.currentClassNode.createUniqueMethod("(IJ)J", desDecryptInstructions, 6, 14, 1, desDecryptLocalVariables, desDecryptTryCatchBlocks, "Long Constant Encryption", cpEntries, inheritance, workspace, 12);
				this.indyDecryptMethodWriter = this.currentClassNode.getOrCreateAnyMethodRef(desDecryptMethodNode, cpEntries);
				if (useIndyCallSite) {
					LocalVariableTable callSiteSetupLocalVariables = new LocalVariableTable(
						true, "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/invoke/MutableCallSite;Ljava/lang/String;[Ljava/lang/Object;)J", 10
					);
					ArrayList<AbstractInsnNode> callSiteSetupInstructions = new ArrayList<>();
					this.buildIndyCallSiteSetup(callSiteSetupLocalVariables, callSiteSetupInstructions, this.indyDecryptMethodWriter, cpEntries, cpManager, workspace, classProvider);
					MethodNode callSiteSetupMethodNode = this.currentClassNode
						.createUniqueMethod(
							"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/invoke/MutableCallSite;Ljava/lang/String;[Ljava/lang/Object;)J",
							callSiteSetupInstructions,
							7,
							10,
							1,
							callSiteSetupLocalVariables,
							new TryCatchBlockInfo[0],
							"Long Constant Encryption",
							cpEntries,
							inheritance,
							workspace,
							12
						);
					AbstractMethodCallWriter callSiteSetupWriter = this.currentClassNode.getOrCreateAnyMethodRef(callSiteSetupMethodNode, cpEntries);
					TryCatchBlockInfo[] bootstrapTryCatchBlocks = new TryCatchBlockInfo[1];
					LocalVariableTable bootstrapLocalVariables = new LocalVariableTable(
						true, "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", 5
					);
					ArrayList<AbstractInsnNode> bootstrapInstructions = new ArrayList<>();
					this.buildIndyBootstrapMethod(bootstrapLocalVariables, bootstrapInstructions, callSiteSetupWriter, bootstrapTryCatchBlocks, cpEntries, cpManager, workspace, classProvider);
					MethodNode bootstrapMethodNode = this.currentClassNode
						.createUniqueMethod(
							"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
							bootstrapInstructions,
							7,
							5,
							1,
							bootstrapLocalVariables,
							bootstrapTryCatchBlocks,
							"Long Constant Encryption",
							cpEntries,
							inheritance,
							workspace,
							12
						);
					AbstractMethodCallWriter bootstrapWriter = this.currentClassNode.getOrCreateAnyMethodRef(bootstrapMethodNode, cpEntries);
					String callSiteName = String.valueOf((char)(97 + this.random.nextInt(26)));
					this.indyCallSiteWriter = this.currentClassNode.createInvokeDynamicCall(callSiteName, "(IJ)J", bootstrapWriter, cpEntries, cpManager);
				}
			}
		} else if (encodedLongsField != null) {
			if (isArrayField) {
				this.xorKey = this.random.nextInt(32766) + 1;
				this.globalXorKey = this.randomLongIterator.next();
				FieldNode encryptedLongsArrayNode = this.currentClassNode.createUniqueField("[Ljava/lang/Long;", this.currentClassNode.isInterface() ? 4 : 1, true, inheritance, workspace, 12);
				this.encryptedLongsArrayField = cpManager.getOrCreateFieldRef(
					this.currentClassNode.getInternalName(), encryptedLongsArrayNode.getNameDisplay(), encryptedLongsArrayNode.getDescriptor(), cpEntries, workspace, classProvider, true
				);
				TryCatchBlockInfo[] decryptTryCatchBlocks = new TryCatchBlockInfo[0];
				LocalVariableTable decryptLocalVariables = new LocalVariableTable(true, "(IJ)J", 4);
				ArrayList<AbstractInsnNode> decryptInstructions = new ArrayList<>();
				this.buildDecryptionWrapper(decryptLocalVariables, decryptInstructions, isArrayField, encodedLongsField, decryptTryCatchBlocks, cpEntries, cpManager, workspace, classProvider);
				MethodNode decryptMethodNode = this.currentClassNode.createUniqueMethod("(IJ)J", decryptInstructions, 8, 4, 1, decryptLocalVariables, decryptTryCatchBlocks, "Long Constant Encryption", cpEntries, inheritance, workspace, 12);
				this.decryptionMethodWriter = this.currentClassNode.getOrCreateAnyMethodRef(decryptMethodNode, cpEntries);
			} else {
				this.globalXorKey = this.randomLongIterator.next();
			}
		} else if (isArrayField) {
			if (!hasParameterChanger) {
				this.globalXorKey = this.randomLongIterator.next();
			}
		} else if (!hasParameterChanger) {
			this.globalXorKey = this.randomLongIterator.next();
		}
	}

	public boolean hasGlobalKey() {
		return this.globalXorKey != null;
	}

	public long getGlobalKey() {
		return this.globalXorKey;
	}

	public void buildInitEncryptedLongs(
		LocalVariableTable localVariableTable,
		List<AbstractInsnNode> instructions,
		LocalVariableIndexCounter localVariableIndexCounter,
		Set<TypeWriter> usedConstants,
		List<TypeWriter> cpEntries,
		FieldTypeWriter encryptedLongsField,
		FieldTypeWriter longsCacheField,
		int arrayVarIndex,
		AtomicReference<Long>[] longReferences,
		ReferenceGraph<AtomicReference<Long>, LongNode> referenceGraph,
		Map<AtomicReference<Long>, LongEntry> longEntries,
		LabelNode switchLabel,
		AtomicInt caseIdCounter,
		MultiMap<LabelNode, PriorityEntry<LabelNode>> switchCaseMap,
		Long longKey,
		boolean useDes,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		int longReferenceCount = longReferences.length;
		List<StringNode> encodedStrings = this.convertToEncodedStringList(longReferences, referenceGraph, longEntries, usedConstants, longKey, useDes, cpEntries, this.currentClassNode.getConstantPoolManager());
		ConstantPoolManager cpManager = this.currentClassNode.getConstantPoolManager();
		int charOffsetSlot = localVariableIndexCounter.getAndIncrease();
		int arrayIndexSlot = localVariableIndexCounter.getAndIncrease();
		int stringSlot = localVariableIndexCounter.getAndIncrease();
		int stringLengthSlot = localVariableIndexCounter.getAndIncrease();
		int arraySlot = arrayVarIndex == -1 ? localVariableIndexCounter.getAndIncrease() : arrayVarIndex;
		int byteArraySlot = localVariableIndexCounter.getAndIncrease();
		AbstractInsnNode.encryptIntConstant(longReferenceCount, instructions, cpManager, cpEntries);
		instructions.add(new NewArrayNode(11));
		instructions.add(AbstractInsnNode.storeObject(arraySlot, localVariableTable, 12));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(AbstractInsnNode.storeInt(arrayIndexSlot, localVariableTable, 12));

		for (StringNode encodedString : encodedStrings) {
			LabelNode stringLoopLabel = new LabelNode(true, 1);
			instructions.add(new WideConstantPoolInsnNode(LDC_W, encodedString));
			instructions.add(InsnNode.of(DUP));
			instructions.add(AbstractInsnNode.storeObject(stringSlot, localVariableTable, 12));
			MethodCallWriter lengthWriter = cpManager.getOrCreateMethodRef("java/lang/String", "length", "()I", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, lengthWriter));
			instructions.add(AbstractInsnNode.storeInt(stringLengthSlot, localVariableTable, 12));
			instructions.add(InsnNode.of(ICONST_0));
			instructions.add(AbstractInsnNode.storeInt(charOffsetSlot, localVariableTable, 12));
			instructions.add(stringLoopLabel);
			instructions.add(AbstractInsnNode.loadObject(stringSlot, localVariableTable, 12));
			instructions.add(AbstractInsnNode.loadInt(charOffsetSlot, localVariableTable, 12));
			instructions.add(AbstractInsnNode.IINC(charOffsetSlot, 8, localVariableTable, 12));
			instructions.add(AbstractInsnNode.loadInt(charOffsetSlot, localVariableTable, 12));
			MethodCallWriter substringWriter = cpManager.getOrCreateMethodRef("java/lang/String", "substring", "(II)Ljava/lang/String;", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, substringWriter));
			StringNode charsetName = cpManager.createStringNode("ISO-8859-1", cpEntries, false);
			instructions.add(new WideConstantPoolInsnNode(LDC_W, charsetName));
			MethodCallWriter getBytesWriter = cpManager.getOrCreateMethodRef("java/lang/String", "getBytes", "(Ljava/lang/String;)[B", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, getBytesWriter));
			instructions.add(AbstractInsnNode.storeObject(byteArraySlot, localVariableTable, 12));
			instructions.add(AbstractInsnNode.loadObject(arraySlot, localVariableTable, 12));
			instructions.add(AbstractInsnNode.loadInt(arrayIndexSlot, localVariableTable, 12));
			instructions.add(AbstractInsnNode.IINC(arrayIndexSlot, 1, localVariableTable, 12));
			instructions.add(AbstractInsnNode.loadObject(byteArraySlot, localVariableTable, 12));
			instructions.add(InsnNode.of(ICONST_0));
			instructions.add(InsnNode.of(BALOAD));
			instructions.add(InsnNode.of(I2L));
			AbstractInsnNode.appendLongConst(255L, instructions, cpManager, cpEntries);
			instructions.add(InsnNode.of(LAND));
			instructions.add(AbstractInsnNode.newInt(56));
			instructions.add(InsnNode.of(LSHL));
			instructions.add(AbstractInsnNode.loadObject(byteArraySlot, localVariableTable, 12));
			instructions.add(InsnNode.of(ICONST_1));
			instructions.add(InsnNode.of(BALOAD));
			instructions.add(InsnNode.of(I2L));
			AbstractInsnNode.appendLongConst(255L, instructions, cpManager, cpEntries);
			instructions.add(InsnNode.of(LAND));
			instructions.add(AbstractInsnNode.newInt(48));
			instructions.add(InsnNode.of(LSHL));
			instructions.add(InsnNode.of(LOR));
			instructions.add(AbstractInsnNode.loadObject(byteArraySlot, localVariableTable, 12));
			instructions.add(InsnNode.of(ICONST_2));
			instructions.add(InsnNode.of(BALOAD));
			instructions.add(InsnNode.of(I2L));
			AbstractInsnNode.appendLongConst(255L, instructions, cpManager, cpEntries);
			instructions.add(InsnNode.of(LAND));
			instructions.add(AbstractInsnNode.newInt(40));
			instructions.add(InsnNode.of(LSHL));
			instructions.add(InsnNode.of(LOR));
			instructions.add(AbstractInsnNode.loadObject(byteArraySlot, localVariableTable, 12));
			instructions.add(InsnNode.of(ICONST_3));
			instructions.add(InsnNode.of(BALOAD));
			instructions.add(InsnNode.of(I2L));
			AbstractInsnNode.appendLongConst(255L, instructions, cpManager, cpEntries);
			instructions.add(InsnNode.of(LAND));
			instructions.add(AbstractInsnNode.newInt(32));
			instructions.add(InsnNode.of(LSHL));
			instructions.add(InsnNode.of(LOR));
			instructions.add(AbstractInsnNode.loadObject(byteArraySlot, localVariableTable, 12));
			instructions.add(InsnNode.of(ICONST_4));
			instructions.add(InsnNode.of(BALOAD));
			instructions.add(InsnNode.of(I2L));
			AbstractInsnNode.appendLongConst(255L, instructions, cpManager, cpEntries);
			instructions.add(InsnNode.of(LAND));
			instructions.add(AbstractInsnNode.newInt(24));
			instructions.add(InsnNode.of(LSHL));
			instructions.add(InsnNode.of(LOR));
			instructions.add(AbstractInsnNode.loadObject(byteArraySlot, localVariableTable, 12));
			instructions.add(InsnNode.of(ICONST_5));
			instructions.add(InsnNode.of(BALOAD));
			instructions.add(InsnNode.of(I2L));
			AbstractInsnNode.appendLongConst(255L, instructions, cpManager, cpEntries);
			instructions.add(InsnNode.of(LAND));
			instructions.add(AbstractInsnNode.newInt(16));
			instructions.add(InsnNode.of(LSHL));
			instructions.add(InsnNode.of(LOR));
			instructions.add(AbstractInsnNode.loadObject(byteArraySlot, localVariableTable, 12));
			instructions.add(AbstractInsnNode.newInt(6));
			instructions.add(InsnNode.of(BALOAD));
			instructions.add(InsnNode.of(I2L));
			AbstractInsnNode.appendLongConst(255L, instructions, cpManager, cpEntries);
			instructions.add(InsnNode.of(LAND));
			instructions.add(AbstractInsnNode.newInt(8));
			instructions.add(InsnNode.of(LSHL));
			instructions.add(InsnNode.of(LOR));
			instructions.add(AbstractInsnNode.loadObject(byteArraySlot, localVariableTable, 12));
			instructions.add(AbstractInsnNode.newInt(7));
			instructions.add(InsnNode.of(BALOAD));
			instructions.add(InsnNode.of(I2L));
			AbstractInsnNode.appendLongConst(255L, instructions, cpManager, cpEntries);
			instructions.add(InsnNode.of(LAND));
			instructions.add(InsnNode.of(LOR));
			int caseId = caseIdCounter.increaseAndGet();
			instructions.add(AbstractInsnNode.newInt(caseId));
			instructions.add(new GotoNode(switchLabel));
			LabelNode caseLabel = new LabelNode(true, 1);
			instructions.add(caseLabel);
			switchCaseMap.put(switchLabel, new PriorityEntry<>(caseId, caseLabel));
			instructions.add(InsnNode.of(LASTORE));
			instructions.add(AbstractInsnNode.loadInt(charOffsetSlot, localVariableTable, 12));
			instructions.add(AbstractInsnNode.loadInt(stringLengthSlot, localVariableTable, 12));
			instructions.add(new JumpInsnNode(IF_ICMPLT, stringLoopLabel));
		}

		if (encryptedLongsField != null) {
			instructions.add(AbstractInsnNode.loadObject(arraySlot, localVariableTable, 12));
			instructions.add(new WideConstantPoolInsnNode(PUTSTATIC, encryptedLongsField));
			AbstractInsnNode.encryptIntConstant(longReferenceCount, instructions, cpManager, cpEntries);
			ClassInfoWriter longClassInfo = cpManager.getOrCreateClassInfo("java/lang/Long", cpEntries);
			instructions.add(new WideConstantPoolInsnNode(ANEWARRAY, longClassInfo));
			instructions.add(new WideConstantPoolInsnNode(PUTSTATIC, this.encryptedLongsArrayField));
		}
	}

	private void buildDecryptionWrapper(
		LocalVariableTable localVariableTable,
		ArrayList<AbstractInsnNode> instructions,
		boolean isArrayField,
		FieldTypeWriter encryptedLongsField,
		TryCatchBlockInfo[] tryCatchBlocks,
		List<TypeWriter> cpEntries,
		ConstantPoolManager cpManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		LabelNode alreadyInitializedLabel = new LabelNode(true, 1);
		instructions.add(AbstractInsnNode.loadInt(0, localVariableTable, 12));
		instructions.add(AbstractInsnNode.loadLong(1, localVariableTable, 12));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(IXOR));
		instructions.add(AbstractInsnNode.ldcInt(this.xorKey, cpManager, cpEntries));
		instructions.add(InsnNode.of(IXOR));
		instructions.add(AbstractInsnNode.ldcInt(32767, cpManager, cpEntries));
		instructions.add(InsnNode.of(IAND));
		instructions.add(AbstractInsnNode.storeInt(3, localVariableTable, 12));
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, this.encryptedLongsArrayField));
		instructions.add(AbstractInsnNode.loadInt(3, localVariableTable, 12));
		instructions.add(InsnNode.of(AALOAD));
		instructions.add(new JumpInsnNode(IFNONNULL, alreadyInitializedLabel));
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, this.encryptedLongsArrayField));
		instructions.add(AbstractInsnNode.loadInt(3, localVariableTable, 12));
		if (this.currentClassNode.isJava5OrNewer()) {
			instructions.add(new WideConstantPoolInsnNode(GETSTATIC, encryptedLongsField));
			instructions.add(AbstractInsnNode.loadInt(3, localVariableTable, 12));
			instructions.add(InsnNode.of(LALOAD));
			instructions.add(AbstractInsnNode.loadLong(1, localVariableTable, 12));
			instructions.add(InsnNode.of(LXOR));
			MethodCallWriter longValueOfWriter = cpManager.getOrCreateMethodRef("java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, longValueOfWriter));
		} else {
			ClassInfoWriter longClassInfo = cpManager.getOrCreateClassInfo("java/lang/Long", cpEntries);
			instructions.add(new NewNode(longClassInfo));
			instructions.add(InsnNode.of(DUP));
			instructions.add(new WideConstantPoolInsnNode(GETSTATIC, encryptedLongsField));
			instructions.add(AbstractInsnNode.loadInt(3, localVariableTable, 12));
			instructions.add(InsnNode.of(LALOAD));
			instructions.add(AbstractInsnNode.loadLong(1, localVariableTable, 12));
			instructions.add(InsnNode.of(LXOR));
			MethodCallWriter longInitWriter = cpManager.getOrCreateMethodRef("java/lang/Long", "<init>", "(J)V", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, longInitWriter));
		}

		instructions.add(InsnNode.of(AASTORE));
		instructions.add(alreadyInitializedLabel);
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, this.encryptedLongsArrayField));
		instructions.add(AbstractInsnNode.loadInt(3, localVariableTable, 12));
		instructions.add(InsnNode.of(AALOAD));
		MethodCallWriter longValueWriter = cpManager.getOrCreateMethodRef("java/lang/Long", "longValue", "()J", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, longValueWriter));
		instructions.add(InsnNode.of(LRETURN));
	}

	public void buildInitThreadLocalMap(List<AbstractInsnNode> instructions, List<TypeWriter> cpEntries, ConstantPoolManager cpManager, AbstractWorkspace workspace, ClassProvider classProvider) {
		ClassInfoWriter hashMapClassInfo = cpManager.getOrCreateClassInfo("java/util/HashMap", cpEntries);
		instructions.add(new NewNode(hashMapClassInfo));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.newInt(13));
		MethodCallWriter hashMapInitWriter = cpManager.getOrCreateMethodRef("java/util/HashMap", "<init>", "(I)V", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, hashMapInitWriter));
		instructions.add(new WideConstantPoolInsnNode(PUTSTATIC, this.threadLocalMapField));
	}

	private void buildIndyBootstrapMethod(
		LocalVariableTable localVariableTable,
		ArrayList<AbstractInsnNode> instructions,
		AbstractMethodCallWriter targetMethodWriter,
		TryCatchBlockInfo[] tryCatchBlocks,
		List<TypeWriter> cpEntries,
		ConstantPoolManager cpManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		LabelNode tryStartLabel = new LabelNode(true, 128);
		LabelNode tryEndLabel = new LabelNode(true, 128);
		LabelNode catchHandlerLabel = new LabelNode(true, 128);
		LabelNode endLabel = new LabelNode(true, 1);
		ClassInfoWriter exceptionClassInfo = cpManager.getOrCreateClassInfo("java/lang/Exception", cpEntries);
		tryCatchBlocks[0] = new TryCatchBlockInfo(exceptionClassInfo, tryStartLabel, tryEndLabel, catchHandlerLabel);
		ClassInfoWriter mutableCallSiteClassInfo = cpManager.getOrCreateClassInfo("java/lang/invoke/MutableCallSite", cpEntries);
		instructions.add(new NewNode(mutableCallSiteClassInfo));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.loadObject(2, localVariableTable, 12));
		MethodCallWriter callSiteInitWriter = cpManager.getOrCreateMethodRef("java/lang/invoke/MutableCallSite", "<init>", "(Ljava/lang/invoke/MethodType;)V", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, callSiteInitWriter));
		instructions.add(AbstractInsnNode.storeObject(3, localVariableTable, 12));
		instructions.add(tryStartLabel);
		instructions.add(AbstractInsnNode.loadObject(3, localVariableTable, 12));
		ResolvedMethodHandleWriter targetMethodHandleWriter = cpManager.getOrCreateMethodHandle(MethodHandleKind.REF_INVOKE_STATIC, targetMethodWriter, cpEntries);
		instructions.add(new WideConstantPoolInsnNode(LDC_W, targetMethodHandleWriter));
		instructions.add(AbstractInsnNode.ldcClass("[Ljava/lang/Object;", cpManager, cpEntries));
		instructions.add(AbstractInsnNode.loadObject(2, localVariableTable, 12));
		MethodCallWriter parameterCountWriter = cpManager.getOrCreateMethodRef("java/lang/invoke/MethodType", "parameterCount", "()I", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, parameterCountWriter));
		MethodCallWriter asCollectorWriter = cpManager.getOrCreateMethodRef(
			"java/lang/invoke/MethodHandle", "asCollector", "(Ljava/lang/Class;I)Ljava/lang/invoke/MethodHandle;", cpEntries, workspace, classProvider
		);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, asCollectorWriter));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(InsnNode.of(ICONST_3));
		ClassInfoWriter objectClassInfo = cpManager.getOrCreateClassInfo("java/lang/Object", cpEntries);
		instructions.add(new WideConstantPoolInsnNode(ANEWARRAY, objectClassInfo));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(AbstractInsnNode.loadObject(0, localVariableTable, 12));
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.newInt(1));
		instructions.add(AbstractInsnNode.loadObject(3, localVariableTable, 12));
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.newInt(2));
		instructions.add(AbstractInsnNode.loadObject(1, localVariableTable, 12));
		instructions.add(InsnNode.of(AASTORE));
		MethodCallWriter insertArgumentsWriter = cpManager.getOrCreateMethodRef(
			"java/lang/invoke/MethodHandles",
			"insertArguments",
			"(Ljava/lang/invoke/MethodHandle;I[Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;",
			cpEntries,
			workspace,
			classProvider
		);
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, insertArgumentsWriter));
		instructions.add(AbstractInsnNode.loadObject(2, localVariableTable, 12));
		MethodCallWriter explicitCastArgumentsWriter = cpManager.getOrCreateMethodRef(
			"java/lang/invoke/MethodHandles",
			"explicitCastArguments",
			"(Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;",
			cpEntries,
			workspace,
			classProvider
		);
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, explicitCastArgumentsWriter));
		MethodCallWriter callSiteSetTargetWriter = cpManager.getOrCreateMethodRef("java/lang/invoke/MutableCallSite", "setTarget", "(Ljava/lang/invoke/MethodHandle;)V", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, callSiteSetTargetWriter));
		instructions.add(tryEndLabel);
		instructions.add(new GotoNode(endLabel));
		instructions.add(catchHandlerLabel);
		instructions.add(AbstractInsnNode.storeObject(4, localVariableTable, 12));
		ClassInfoWriter runtimeExceptionClassInfo = cpManager.getOrCreateClassInfo("java/lang/RuntimeException", cpEntries);
		instructions.add(new NewNode(runtimeExceptionClassInfo));
		instructions.add(InsnNode.of(DUP));
		ClassInfoWriter stringBuilderClassInfo = cpManager.getOrCreateClassInfo("java/lang/StringBuilder", cpEntries);
		instructions.add(new NewNode(stringBuilderClassInfo));
		instructions.add(InsnNode.of(DUP));
		MethodCallWriter stringBuilderInitWriter = cpManager.getOrCreateMethodRef("java/lang/StringBuilder", "<init>", "()V", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, stringBuilderInitWriter));
		instructions.add(AbstractInsnNode.ldcString(cpManager.getOwnerInternalName(), cpManager, cpEntries, false));
		MethodCallWriter appendWriter = cpManager.getOrCreateMethodRef("java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, appendWriter));
		StringNode ownerSeparator = cpManager.createStringNode(" : ", cpEntries, false);
		instructions.add(new WideConstantPoolInsnNode(LDC_W, ownerSeparator));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, appendWriter));
		instructions.add(AbstractInsnNode.loadObject(1, localVariableTable, 12));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, appendWriter));
		StringNode nameSeparator = cpManager.createStringNode(" : ", cpEntries, false);
		instructions.add(new WideConstantPoolInsnNode(LDC_W, nameSeparator));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, appendWriter));
		instructions.add(AbstractInsnNode.loadObject(2, localVariableTable, 12));
		MethodCallWriter methodTypeToStringWriter = cpManager.getOrCreateMethodRef("java/lang/invoke/MethodType", "toString", "()Ljava/lang/String;", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, methodTypeToStringWriter));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, appendWriter));
		MethodCallWriter stringBuilderToStringWriter = cpManager.getOrCreateMethodRef("java/lang/StringBuilder", "toString", "()Ljava/lang/String;", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, stringBuilderToStringWriter));
		instructions.add(AbstractInsnNode.loadObject(4, localVariableTable, 12));
		MethodCallWriter runtimeExceptionInitWriter = cpManager.getOrCreateMethodRef("java/lang/RuntimeException", "<init>", "(Ljava/lang/String;Ljava/lang/Throwable;)V", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, runtimeExceptionInitWriter));
		instructions.add(InsnNode.of(ATHROW));
		instructions.add(endLabel);
		instructions.add(AbstractInsnNode.loadObject(3, localVariableTable, 12));
		instructions.add(InsnNode.of(ARETURN));
	}

	public long combineWithKey(long value, long key) {
		return this.desTransform(value, key, false);
	}

	private void buildIndyCallSiteSetup(
		LocalVariableTable localVariableTable,
		ArrayList<AbstractInsnNode> instructions,
		AbstractMethodCallWriter decryptMethodWriter,
		List<TypeWriter> cpEntries,
		ConstantPoolManager cpManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		instructions.add(AbstractInsnNode.loadObject(3, localVariableTable, 12));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(InsnNode.of(AALOAD));
		ClassInfoWriter integerClassInfo = cpManager.getOrCreateClassInfo("java/lang/Integer", cpEntries);
		instructions.add(new WideConstantPoolInsnNode(CHECKCAST, integerClassInfo));
		MethodCallWriter intValueWriter = cpManager.getOrCreateMethodRef("java/lang/Integer", "intValue", "()I", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, intValueWriter));
		instructions.add(AbstractInsnNode.storeInt(4, localVariableTable, 12));
		instructions.add(AbstractInsnNode.loadObject(3, localVariableTable, 12));
		instructions.add(InsnNode.of(ICONST_1));
		instructions.add(InsnNode.of(AALOAD));
		ClassInfoWriter longClassInfo = cpManager.getOrCreateClassInfo("java/lang/Long", cpEntries);
		instructions.add(new WideConstantPoolInsnNode(CHECKCAST, longClassInfo));
		MethodCallWriter longValueWriter = cpManager.getOrCreateMethodRef("java/lang/Long", "longValue", "()J", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, longValueWriter));
		instructions.add(AbstractInsnNode.storeLong(5, localVariableTable, 12));
		instructions.add(AbstractInsnNode.loadInt(4, localVariableTable, 12));
		instructions.add(AbstractInsnNode.loadLong(5, localVariableTable, 12));
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, decryptMethodWriter));
		instructions.add(AbstractInsnNode.storeLong(7, localVariableTable, 12));
		Integer innerClassOrdinal = this.currentClassNode.hasInnerClassOrdinal() ? this.currentClassNode.getInnerClassOrdinal() : null;
		AbstractFieldNode longTypeField = classProvider.getClassNode("java/lang/Long", innerClassOrdinal).findField("TYPE", "Ljava/lang/Class;");
		FieldTypeWriter longTypeFieldRef = cpManager.getOrCreateFieldRef("java/lang/Long", "TYPE", "Ljava/lang/Class;", cpEntries, longTypeField);
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, longTypeFieldRef));
		instructions.add(AbstractInsnNode.loadLong(7, localVariableTable, 12));
		MethodCallWriter longValueOfWriter = cpManager.getOrCreateMethodRef("java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, longValueOfWriter));
		MethodCallWriter constantWriter = cpManager.getOrCreateMethodRef(
			"java/lang/invoke/MethodHandles", "constant", "(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;", cpEntries, workspace, classProvider
		);
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, constantWriter));
		instructions.add(AbstractInsnNode.storeObject(9, localVariableTable, 12));
		instructions.add(AbstractInsnNode.loadObject(1, localVariableTable, 12));
		instructions.add(AbstractInsnNode.loadObject(9, localVariableTable, 12));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(InsnNode.of(ICONST_2));
		ClassInfoWriter classClassInfo = cpManager.getOrCreateClassInfo("java/lang/Class", cpEntries);
		instructions.add(new WideConstantPoolInsnNode(ANEWARRAY, classClassInfo));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_0));
		AbstractFieldNode integerTypeField = classProvider.getClassNode("java/lang/Integer", innerClassOrdinal).findField("TYPE", "Ljava/lang/Class;");
		FieldTypeWriter integerTypeFieldRef = cpManager.getOrCreateFieldRef("java/lang/Integer", "TYPE", "Ljava/lang/Class;", cpEntries, integerTypeField);
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, integerTypeFieldRef));
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_1));
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, longTypeFieldRef));
		instructions.add(InsnNode.of(AASTORE));
		MethodCallWriter dropArgumentsWriter = cpManager.getOrCreateMethodRef(
			"java/lang/invoke/MethodHandles", "dropArguments", "(Ljava/lang/invoke/MethodHandle;I[Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;", cpEntries, workspace, classProvider
		);
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, dropArgumentsWriter));
		MethodCallWriter callSiteSetTargetWriter = cpManager.getOrCreateMethodRef("java/lang/invoke/MutableCallSite", "setTarget", "(Ljava/lang/invoke/MethodHandle;)V", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, callSiteSetTargetWriter));
		instructions.add(AbstractInsnNode.loadLong(7, localVariableTable, 12));
		instructions.add(InsnNode.of(LRETURN));
	}

	public void buildEncryptedValueSwitch(
		LocalVariableTable localVariableTable,
		LabelNode switchLabel,
		Long longKey,
		LocalVariableOperand longKeyOperand,
		LocalVariableIndexCounter localVariableIndexCounter,
		List<AbstractInsnNode> instructions,
		List<TypeWriter> cpEntries,
		MultiMap<LabelNode, PriorityEntry<LabelNode>> switchCaseMap,
		Integer cipherVarIndex,
		Integer xorKeyVarIndex,
		ConstantPoolManager cpManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		ArrayList<AbstractInsnNode> switchInstructions = new ArrayList<>();
		LabelNode afterSwitchLabel = new LabelNode(true, 1);
		switchInstructions.add(new GotoNode(afterSwitchLabel));
		switchInstructions.add(switchLabel);
		List<PriorityEntry<LabelNode>> caseEntries = switchCaseMap.get(switchLabel);
		if (caseEntries.size() > 1) {
			Collections.sort(caseEntries);
			LabelNode defaultLabel = caseEntries.get(0).getValue();
			LabelNode[] caseLabels = new LabelNode[caseEntries.size() - 1];

			for (int caseIndex = 1; caseIndex < caseEntries.size(); caseIndex++) {
				caseLabels[caseIndex - 1] = caseEntries.get(caseIndex).getValue();
			}

			switchInstructions.add(InsnNode.of(DUP_X2));
			switchInstructions.add(InsnNode.of(POP));
			this.dispatchEncryption(localVariableTable, switchInstructions, cpEntries, longKey, longKeyOperand, localVariableIndexCounter, cipherVarIndex, this.globalXorKey, xorKeyVarIndex, cpManager, workspace, classProvider);
			switchInstructions.add(InsnNode.of(DUP2_X1));
			switchInstructions.add(InsnNode.of(POP2));
			switchInstructions.add(new TableSwitchNode(defaultLabel, 0, caseLabels.length - 1, caseLabels));
		} else {
			LabelNode singleTargetLabel = caseEntries.get(0).getValue();
			switchInstructions.add(InsnNode.of(DUP_X2));
			switchInstructions.add(InsnNode.of(POP));
			this.dispatchEncryption(localVariableTable, switchInstructions, cpEntries, longKey, longKeyOperand, localVariableIndexCounter, cipherVarIndex, this.globalXorKey, xorKeyVarIndex, cpManager, workspace, classProvider);
			switchInstructions.add(InsnNode.of(DUP2_X1));
			switchInstructions.add(InsnNode.of(POP2));
			switchInstructions.add(InsnNode.of(POP));
			switchInstructions.add(new GotoNode(singleTargetLabel));
		}

		switchInstructions.add(afterSwitchLabel);
		instructions.addAll(switchInstructions);
	}

	private List<StringNode> convertToEncodedStringList(
		AtomicReference<Long>[] longReferences,
		ReferenceGraph<AtomicReference<Long>, LongNode> referenceGraph,
		Map<AtomicReference<Long>, LongEntry> longEntries,
		Set<TypeWriter> usedConstants,
		Long longKey,
		boolean useDes,
		List<TypeWriter> cpEntries,
		ConstantPoolManager cpManager
	) {
		ArrayList<StringNode> encodedStrings = new ArrayList<>();
		int longReferenceCount = longReferences.length;
		int referenceIndex = 0;

		while (referenceIndex < longReferenceCount) {
			StringBuilder stringBuffer = new StringBuilder(longReferenceCount * 4);
			int accumulatedLength = 0;

			while (referenceIndex < longReferenceCount) {
				AtomicReference<Long> longReference = longReferences[referenceIndex];
				LongEntry longEntry = longEntries.get(longReference);

				for (LongNode longNode : referenceGraph.getReferences(longReference)) {
					usedConstants.add(longNode);
				}

				LongLookup lookupType = longEntry.getLookupType();
				long encryptedValue = longReference.get();
				short encodedIndex = 0;
				if (lookupType.equals(LongLookup.LOOKUP_METHOD_AND_INDEX)) {
					long encryptionKey = this.randomLongIterator.next();
					longEntry.setEncryptionKey(encryptionKey);
					encodedIndex = (short)(referenceIndex ^ this.xorKey);
					encryptedValue ^= encryptionKey;
				} else if (lookupType.equals(LongLookup.LOOKUP_METHOD_AND_INDEX_DES) || lookupType.equals(LongLookup.INDY_ENTRY_AND_INDEX)) {
					long desEncryptionKey = this.randomLongIterator.next();
					longEntry.setEncryptionKey(desEncryptionKey);
					encodedIndex = (short)(referenceIndex ^ this.xorKey);
					encryptedValue = this.combineWithKey(encryptedValue, desEncryptionKey);
				}

				long maskedValue;
				if (longKey != null) {
					if (useDes) {
						maskedValue = this.combineWithKey(encryptedValue, longKey);
					} else {
						maskedValue = encryptedValue ^ longKey;
					}
				} else {
					maskedValue = encryptedValue ^ this.globalXorKey;
				}

				byte[] maskedValueBytes = ByteUtils.unpackLong(maskedValue);

				String decodedString;
				try {
					decodedString = new String(maskedValueBytes, "ISO-8859-1");
				} catch (UnsupportedEncodingException encodingException) {
					throw new ProcessingException(encodingException.getMessage(), encodingException);
				}

				int utf8Length = GlobalUtils.getUTF8Length(decodedString);
				if (accumulatedLength > 0 && accumulatedLength + utf8Length > 32768) {
					break;
				}

				accumulatedLength += utf8Length;
				referenceIndex++;
				stringBuffer.append(decodedString);
				if (lookupType.equals(LongLookup.LOOKUP_METHOD_AND_INDEX)
					|| lookupType.equals(LongLookup.LOOKUP_METHOD_AND_INDEX_DES)
					|| lookupType.equals(LongLookup.INDY_ENTRY_AND_INDEX)) {
					longEntry.setIndex(encodedIndex);
				}

				if (encodedStrings.size() == 0 && longReferenceCount > 3 && referenceIndex == longReferenceCount - 2) {
					break;
				}
			}

			StringNode encodedStringNode = cpManager.createString(stringBuffer.toString(), cpEntries);
			encodedStrings.add(encodedStringNode);
		}

		return encodedStrings;
	}

	private void dispatchEncryption(
		LocalVariableTable localVariableTable,
		List<AbstractInsnNode> instructions,
		List<TypeWriter> cpEntries,
		Long longKey,
		LocalVariableOperand longKeyOperand,
		LocalVariableIndexCounter localVariableIndexCounter,
		Integer cipherSlotIndex,
		Long globalXorKey,
		Integer globalKeySlotIndex,
		ConstantPoolManager cpManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		if (cipherSlotIndex != null) {
			this.encryptAndStoreLong(localVariableTable, instructions, cpEntries, cipherSlotIndex, localVariableIndexCounter, workspace, classProvider);
		} else if (longKeyOperand != null) {
			this.buildXorWithKey(localVariableTable, instructions, cpEntries, longKey, longKeyOperand.getIndex(), localVariableIndexCounter, cpManager, workspace, classProvider);
		} else {
			this.buildXorWithKey(localVariableTable, instructions, cpEntries, globalXorKey, globalKeySlotIndex, localVariableIndexCounter, cpManager, workspace, classProvider);
		}
	}

	public InvokeDynamicCallWriter getIndyCallSiteWriter(ClassNode classNode) {
		return this.indyCallSiteWriter;
	}

	public FieldTypeWriter getEncryptedLongsArrayField(ClassNode classNode) {
		return this.encryptedLongsArrayField;
	}

	private void buildDesDecryptionWrapper(
		LocalVariableTable localVariableTable,
		ArrayList<AbstractInsnNode> instructions,
		boolean isArrayField,
		FieldTypeWriter encryptedLongsField,
		TryCatchBlockInfo[] tryCatchBlocks,
		List<TypeWriter> cpEntries,
		ConstantPoolManager cpManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		LabelNode cacheHitLabel = new LabelNode(true, 1);
		LabelNode afterCatchLabel = new LabelNode(true, 1);
		LabelNode arrayEntryNonNullLabel = new LabelNode(true, 1);
		LabelNode tryStartLabel = new LabelNode(true, 384);
		LabelNode tryEndLabel = new LabelNode(true, 640);
		LabelNode catchHandlerLabel = new LabelNode(true, 1152);
		ClassInfoWriter exceptionClassInfo = cpManager.getOrCreateClassInfo("java/lang/Exception", cpEntries);
		tryCatchBlocks[0] = new TryCatchBlockInfo(exceptionClassInfo, tryStartLabel, tryEndLabel, catchHandlerLabel);
		instructions.add(AbstractInsnNode.loadInt(0, localVariableTable, 12));
		instructions.add(AbstractInsnNode.loadLong(1, localVariableTable, 12));
		instructions.add(AbstractInsnNode.ldcLong(32767L, cpManager, cpEntries));
		instructions.add(InsnNode.of(LAND));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(IXOR));
		instructions.add(AbstractInsnNode.ldcInt(this.xorKey, cpManager, cpEntries));
		instructions.add(InsnNode.of(IXOR));
		instructions.add(AbstractInsnNode.storeInt(3, localVariableTable, 12));
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, this.encryptedLongsArrayField));
		instructions.add(AbstractInsnNode.loadInt(3, localVariableTable, 12));
		instructions.add(InsnNode.of(AALOAD));
		instructions.add(new JumpInsnNode(IFNONNULL, arrayEntryNonNullLabel));
		instructions.add(AbstractInsnNode.newInt(8));
		instructions.add(new NewArrayNode(8));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(AbstractInsnNode.loadLong(1, localVariableTable, 12));
		instructions.add(AbstractInsnNode.newInt(56));
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_1));
		instructions.add(AbstractInsnNode.loadLong(1, localVariableTable, 12));
		instructions.add(AbstractInsnNode.newInt(48));
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_2));
		instructions.add(AbstractInsnNode.loadLong(1, localVariableTable, 12));
		instructions.add(AbstractInsnNode.newInt(40));
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_3));
		instructions.add(AbstractInsnNode.loadLong(1, localVariableTable, 12));
		instructions.add(AbstractInsnNode.newInt(32));
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_4));
		instructions.add(AbstractInsnNode.loadLong(1, localVariableTable, 12));
		instructions.add(AbstractInsnNode.newInt(24));
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_5));
		instructions.add(AbstractInsnNode.loadLong(1, localVariableTable, 12));
		instructions.add(AbstractInsnNode.newInt(16));
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.newInt(6));
		instructions.add(AbstractInsnNode.loadLong(1, localVariableTable, 12));
		instructions.add(AbstractInsnNode.newInt(8));
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.newInt(7));
		instructions.add(AbstractInsnNode.loadLong(1, localVariableTable, 12));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(AbstractInsnNode.storeObject(4, localVariableTable, 12));
		if (isArrayField) {
			instructions.add(new WideConstantPoolInsnNode(GETSTATIC, encryptedLongsField));
			instructions.add(AbstractInsnNode.loadInt(3, localVariableTable, 12));
			instructions.add(InsnNode.of(LALOAD));
		} else {
			instructions.add(new WideConstantPoolInsnNode(GETSTATIC, encryptedLongsField));
		}

		instructions.add(AbstractInsnNode.storeLong(5, localVariableTable, 12));
		instructions.add(AbstractInsnNode.newInt(8));
		instructions.add(new NewArrayNode(8));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(AbstractInsnNode.loadLong(5, localVariableTable, 12));
		instructions.add(AbstractInsnNode.newInt(56));
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_1));
		instructions.add(AbstractInsnNode.loadLong(5, localVariableTable, 12));
		instructions.add(AbstractInsnNode.newInt(48));
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_2));
		instructions.add(AbstractInsnNode.loadLong(5, localVariableTable, 12));
		instructions.add(AbstractInsnNode.newInt(40));
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_3));
		instructions.add(AbstractInsnNode.loadLong(5, localVariableTable, 12));
		instructions.add(AbstractInsnNode.newInt(32));
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_4));
		instructions.add(AbstractInsnNode.loadLong(5, localVariableTable, 12));
		instructions.add(AbstractInsnNode.newInt(24));
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_5));
		instructions.add(AbstractInsnNode.loadLong(5, localVariableTable, 12));
		instructions.add(AbstractInsnNode.newInt(16));
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.newInt(6));
		instructions.add(AbstractInsnNode.loadLong(5, localVariableTable, 12));
		instructions.add(AbstractInsnNode.newInt(8));
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.newInt(7));
		instructions.add(AbstractInsnNode.loadLong(5, localVariableTable, 12));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(AbstractInsnNode.storeObject(7, localVariableTable, 12));
		MethodCallWriter currentThreadWriter = cpManager.getOrCreateMethodRef("java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", cpEntries, workspace, classProvider);
		if (this.currentClassNode.isJava5OrNewer()) {
			instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, currentThreadWriter));
			if (this.currentClassNode.isJava19OrNewer()) {
				MethodCallWriter threadIdWriter = cpManager.getOrCreateMethodRef("java/lang/Thread", "threadId", "()J", cpEntries, workspace, classProvider);
				instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, threadIdWriter));
			} else {
				MethodCallWriter threadGetIdWriter = cpManager.getOrCreateMethodRef("java/lang/Thread", "getId", "()J", cpEntries, workspace, classProvider);
				instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, threadGetIdWriter));
			}

			MethodCallWriter mapKeyLongValueOfWriter = cpManager.getOrCreateMethodRef("java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, mapKeyLongValueOfWriter));
		} else {
			ClassInfoWriter mapKeyLongClassInfo = cpManager.getOrCreateClassInfo("java/lang/Long", cpEntries);
			instructions.add(new NewNode(mapKeyLongClassInfo));
			instructions.add(InsnNode.of(DUP));
			instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, currentThreadWriter));
			MethodCallWriter identityHashCodeWriter = cpManager.getOrCreateMethodRef("java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, identityHashCodeWriter));
			instructions.add(InsnNode.of(I2L));
			MethodCallWriter mapKeyLongInitWriter = cpManager.getOrCreateMethodRef("java/lang/Long", "<init>", "(J)V", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, mapKeyLongInitWriter));
		}

		instructions.add(AbstractInsnNode.storeObject(8, localVariableTable, 12));
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, this.threadLocalMapField));
		instructions.add(AbstractInsnNode.loadObject(8, localVariableTable, 12));
		InterfaceMethodCallWriter mapGetWriter = cpManager.getOrCreateInterfaceMethod("java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", cpEntries, workspace, classProvider);
		instructions.add(new InvokeInterfaceNode(mapGetWriter));
		ClassInfoWriter objectArrayClassInfo = cpManager.getOrCreateClassInfo("[Ljava/lang/Object;", cpEntries);
		instructions.add(new WideConstantPoolInsnNode(CHECKCAST, objectArrayClassInfo));
		instructions.add(AbstractInsnNode.storeObject(9, localVariableTable, 12));
		instructions.add(tryStartLabel);
		instructions.add(AbstractInsnNode.loadObject(9, localVariableTable, 12));
		instructions.add(new JumpInsnNode(IFNONNULL, cacheHitLabel));
		instructions.add(InsnNode.of(ICONST_3));
		ClassInfoWriter objectClassInfo = cpManager.getOrCreateClassInfo("java/lang/Object", cpEntries);
		instructions.add(new WideConstantPoolInsnNode(ANEWARRAY, objectClassInfo));
		instructions.add(AbstractInsnNode.storeObject(9, localVariableTable, 12));
		instructions.add(AbstractInsnNode.loadObject(9, localVariableTable, 12));
		instructions.add(InsnNode.of(ICONST_0));
		StringNode cipherTransformation = cpManager.createStringNode("DES/CBC/NoPadding", cpEntries, false);
		instructions.add(new WideConstantPoolInsnNode(LDC_W, cipherTransformation));
		MethodCallWriter cipherGetInstanceWriter = cpManager.getOrCreateMethodRef("javax/crypto/Cipher", "getInstance", "(Ljava/lang/String;)Ljavax/crypto/Cipher;", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, cipherGetInstanceWriter));
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(AbstractInsnNode.loadObject(9, localVariableTable, 12));
		instructions.add(InsnNode.of(ICONST_1));
		StringNode cipherAlgorithm = cpManager.createStringNode("DES", cpEntries, false);
		instructions.add(new WideConstantPoolInsnNode(LDC_W, cipherAlgorithm));
		MethodCallWriter secretKeyFactoryGetInstanceWriter = cpManager.getOrCreateMethodRef(
			"javax/crypto/SecretKeyFactory", "getInstance", "(Ljava/lang/String;)Ljavax/crypto/SecretKeyFactory;", cpEntries, workspace, classProvider
		);
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, secretKeyFactoryGetInstanceWriter));
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(AbstractInsnNode.loadObject(9, localVariableTable, 12));
		instructions.add(InsnNode.of(ICONST_2));
		ClassInfoWriter ivParameterSpecClassInfo = cpManager.getOrCreateClassInfo("javax/crypto/spec/IvParameterSpec", cpEntries);
		instructions.add(new NewNode(ivParameterSpecClassInfo));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.newInt(8));
		instructions.add(new NewArrayNode(8));
		MethodCallWriter ivParameterSpecInitWriter = cpManager.getOrCreateMethodRef("javax/crypto/spec/IvParameterSpec", "<init>", "([B)V", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, ivParameterSpecInitWriter));
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, this.threadLocalMapField));
		instructions.add(AbstractInsnNode.loadObject(8, localVariableTable, 12));
		instructions.add(AbstractInsnNode.loadObject(9, localVariableTable, 12));
		InterfaceMethodCallWriter mapPutWriter = cpManager.getOrCreateInterfaceMethod(
			"java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", cpEntries, workspace, classProvider
		);
		instructions.add(new InvokeInterfaceNode(mapPutWriter));
		instructions.add(InsnNode.of(POP));
		instructions.add(cacheHitLabel);
		ClassInfoWriter desKeySpecClassInfo = cpManager.getOrCreateClassInfo("javax/crypto/spec/DESKeySpec", cpEntries);
		instructions.add(new NewNode(desKeySpecClassInfo));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.loadObject(4, localVariableTable, 12));
		MethodCallWriter desKeySpecInitWriter = cpManager.getOrCreateMethodRef("javax/crypto/spec/DESKeySpec", "<init>", "([B)V", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, desKeySpecInitWriter));
		instructions.add(AbstractInsnNode.storeObject(11, localVariableTable, 12));
		instructions.add(AbstractInsnNode.loadObject(9, localVariableTable, 12));
		instructions.add(InsnNode.of(ICONST_1));
		instructions.add(InsnNode.of(AALOAD));
		ClassInfoWriter secretKeyFactoryClassInfo = cpManager.getOrCreateClassInfo("javax/crypto/SecretKeyFactory", cpEntries);
		instructions.add(new WideConstantPoolInsnNode(CHECKCAST, secretKeyFactoryClassInfo));
		instructions.add(AbstractInsnNode.loadObject(11, localVariableTable, 12));
		MethodCallWriter generateSecretWriter = cpManager.getOrCreateMethodRef(
			"javax/crypto/SecretKeyFactory", "generateSecret", "(Ljava/security/spec/KeySpec;)Ljavax/crypto/SecretKey;", cpEntries, workspace, classProvider
		);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, generateSecretWriter));
		instructions.add(AbstractInsnNode.storeObject(12, localVariableTable, 12));
		instructions.add(AbstractInsnNode.loadObject(9, localVariableTable, 12));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(InsnNode.of(AALOAD));
		ClassInfoWriter cipherClassInfo = cpManager.getOrCreateClassInfo("javax/crypto/Cipher", cpEntries);
		instructions.add(new WideConstantPoolInsnNode(CHECKCAST, cipherClassInfo));
		instructions.add(AbstractInsnNode.storeObject(13, localVariableTable, 12));
		instructions.add(AbstractInsnNode.loadObject(13, localVariableTable, 12));
		instructions.add(AbstractInsnNode.newInt(2));
		instructions.add(AbstractInsnNode.loadObject(12, localVariableTable, 12));
		instructions.add(AbstractInsnNode.loadObject(9, localVariableTable, 12));
		instructions.add(InsnNode.of(ICONST_2));
		instructions.add(InsnNode.of(AALOAD));
		instructions.add(new WideConstantPoolInsnNode(CHECKCAST, ivParameterSpecClassInfo));
		MethodCallWriter cipherInitWriter = cpManager.getOrCreateMethodRef(
			"javax/crypto/Cipher", "init", "(ILjava/security/Key;Ljava/security/spec/AlgorithmParameterSpec;)V", cpEntries, workspace, classProvider
		);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, cipherInitWriter));
		instructions.add(AbstractInsnNode.loadObject(13, localVariableTable, 12));
		instructions.add(AbstractInsnNode.loadObject(7, localVariableTable, 12));
		MethodCallWriter cipherDoFinalWriter = cpManager.getOrCreateMethodRef("javax/crypto/Cipher", "doFinal", "([B)[B", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, cipherDoFinalWriter));
		instructions.add(AbstractInsnNode.storeObject(10, localVariableTable, 12));
		instructions.add(tryEndLabel);
		instructions.add(new GotoNode(afterCatchLabel));
		instructions.add(catchHandlerLabel);
		instructions.add(AbstractInsnNode.storeObject(11, localVariableTable, 12));
		ClassInfoWriter runtimeExceptionClassInfo = cpManager.getOrCreateClassInfo("java/lang/RuntimeException", cpEntries);
		instructions.add(new NewNode(runtimeExceptionClassInfo));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.ldcString(cpManager.getOwnerInternalName(), cpManager, cpEntries, false));
		instructions.add(AbstractInsnNode.loadObject(11, localVariableTable, 12));
		MethodCallWriter runtimeExceptionInitWriter = cpManager.getOrCreateMethodRef("java/lang/RuntimeException", "<init>", "(Ljava/lang/String;Ljava/lang/Throwable;)V", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, runtimeExceptionInitWriter));
		instructions.add(InsnNode.of(ATHROW));
		instructions.add(afterCatchLabel);
		instructions.add(AbstractInsnNode.loadObject(10, localVariableTable, 12));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(InsnNode.of(BALOAD));
		instructions.add(InsnNode.of(I2L));
		AbstractInsnNode.appendLongConst(255L, instructions, cpManager, cpEntries);
		instructions.add(InsnNode.of(LAND));
		instructions.add(AbstractInsnNode.newInt(56));
		instructions.add(InsnNode.of(LSHL));
		instructions.add(AbstractInsnNode.loadObject(10, localVariableTable, 12));
		instructions.add(InsnNode.of(ICONST_1));
		instructions.add(InsnNode.of(BALOAD));
		instructions.add(InsnNode.of(I2L));
		AbstractInsnNode.appendLongConst(255L, instructions, cpManager, cpEntries);
		instructions.add(InsnNode.of(LAND));
		instructions.add(AbstractInsnNode.newInt(48));
		instructions.add(InsnNode.of(LSHL));
		instructions.add(InsnNode.of(LOR));
		instructions.add(AbstractInsnNode.loadObject(10, localVariableTable, 12));
		instructions.add(InsnNode.of(ICONST_2));
		instructions.add(InsnNode.of(BALOAD));
		instructions.add(InsnNode.of(I2L));
		AbstractInsnNode.appendLongConst(255L, instructions, cpManager, cpEntries);
		instructions.add(InsnNode.of(LAND));
		instructions.add(AbstractInsnNode.newInt(40));
		instructions.add(InsnNode.of(LSHL));
		instructions.add(InsnNode.of(LOR));
		instructions.add(AbstractInsnNode.loadObject(10, localVariableTable, 12));
		instructions.add(InsnNode.of(ICONST_3));
		instructions.add(InsnNode.of(BALOAD));
		instructions.add(InsnNode.of(I2L));
		AbstractInsnNode.appendLongConst(255L, instructions, cpManager, cpEntries);
		instructions.add(InsnNode.of(LAND));
		instructions.add(AbstractInsnNode.newInt(32));
		instructions.add(InsnNode.of(LSHL));
		instructions.add(InsnNode.of(LOR));
		instructions.add(AbstractInsnNode.loadObject(10, localVariableTable, 12));
		instructions.add(InsnNode.of(ICONST_4));
		instructions.add(InsnNode.of(BALOAD));
		instructions.add(InsnNode.of(I2L));
		AbstractInsnNode.appendLongConst(255L, instructions, cpManager, cpEntries);
		instructions.add(InsnNode.of(LAND));
		instructions.add(AbstractInsnNode.newInt(24));
		instructions.add(InsnNode.of(LSHL));
		instructions.add(InsnNode.of(LOR));
		instructions.add(AbstractInsnNode.loadObject(10, localVariableTable, 12));
		instructions.add(InsnNode.of(ICONST_5));
		instructions.add(InsnNode.of(BALOAD));
		instructions.add(InsnNode.of(I2L));
		AbstractInsnNode.appendLongConst(255L, instructions, cpManager, cpEntries);
		instructions.add(InsnNode.of(LAND));
		instructions.add(AbstractInsnNode.newInt(16));
		instructions.add(InsnNode.of(LSHL));
		instructions.add(InsnNode.of(LOR));
		instructions.add(AbstractInsnNode.loadObject(10, localVariableTable, 12));
		instructions.add(AbstractInsnNode.newInt(6));
		instructions.add(InsnNode.of(BALOAD));
		instructions.add(InsnNode.of(I2L));
		AbstractInsnNode.appendLongConst(255L, instructions, cpManager, cpEntries);
		instructions.add(InsnNode.of(LAND));
		instructions.add(AbstractInsnNode.newInt(8));
		instructions.add(InsnNode.of(LSHL));
		instructions.add(InsnNode.of(LOR));
		instructions.add(AbstractInsnNode.loadObject(10, localVariableTable, 12));
		instructions.add(AbstractInsnNode.newInt(7));
		instructions.add(InsnNode.of(BALOAD));
		instructions.add(InsnNode.of(I2L));
		AbstractInsnNode.appendLongConst(255L, instructions, cpManager, cpEntries);
		instructions.add(InsnNode.of(LAND));
		instructions.add(InsnNode.of(LOR));
		instructions.add(AbstractInsnNode.storeLong(11, localVariableTable, 12));
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, this.encryptedLongsArrayField));
		instructions.add(AbstractInsnNode.loadInt(3, localVariableTable, 12));
		if (this.currentClassNode.isJava5OrNewer()) {
			instructions.add(AbstractInsnNode.loadLong(11, localVariableTable, 12));
			MethodCallWriter resultLongValueOfWriter = cpManager.getOrCreateMethodRef("java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, resultLongValueOfWriter));
		} else {
			ClassInfoWriter resultLongClassInfo = cpManager.getOrCreateClassInfo("java/lang/Long", cpEntries);
			instructions.add(new NewNode(resultLongClassInfo));
			instructions.add(InsnNode.of(DUP));
			instructions.add(AbstractInsnNode.loadLong(11, localVariableTable, 12));
			MethodCallWriter resultLongInitWriter = cpManager.getOrCreateMethodRef("java/lang/Long", "<init>", "(J)V", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, resultLongInitWriter));
		}

		instructions.add(InsnNode.of(AASTORE));
		instructions.add(arrayEntryNonNullLabel);
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, this.encryptedLongsArrayField));
		instructions.add(AbstractInsnNode.loadInt(3, localVariableTable, 12));
		instructions.add(InsnNode.of(AALOAD));
		MethodCallWriter longValueWriter = cpManager.getOrCreateMethodRef("java/lang/Long", "longValue", "()J", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, longValueWriter));
		instructions.add(InsnNode.of(LRETURN));
	}

	public AbstractMethodCallWriter getDecryptionMethodWriter(ClassNode classNode) {
		return this.indyDecryptMethodWriter != null ? this.indyDecryptMethodWriter : this.decryptionMethodWriter;
	}

	public long desTransform(long value, long key, boolean decrypt) {
		byte[] keyBytes = ByteUtils.unpackLong(key);

		try {
			DESKeySpec desKeySpec = new DESKeySpec(keyBytes);
			if (this.secretKeyFactory == null) {
				this.secretKeyFactory = SecretKeyFactory.getInstance("DES");
			}

			if (this.cipher == null) {
				this.cipher = Cipher.getInstance("DES/CBC/NoPadding");
				this.ivParameterSpec = new IvParameterSpec(new byte[8]);
			}

			SecretKey secretKey = this.secretKeyFactory.generateSecret(desKeySpec);
			this.cipher.init(decrypt ? 2 : 1, secretKey, this.ivParameterSpec);
			byte[] valueBytes = ByteUtils.unpackLong(value);
			return ByteUtils.packLong(this.cipher.doFinal(valueBytes));
		} catch (Exception exception) {
			throw new ProcessingException(exception.toString(), exception);
		}
	}

	public FieldTypeWriter getThreadLocalMapField() {
		return this.threadLocalMapField;
	}

	private void encryptAndStoreLong(
		LocalVariableTable localVariableTable,
		List<AbstractInsnNode> instructions,
		List<TypeWriter> cpEntries,
		int cipherSlotIndex,
		LocalVariableIndexCounter localVariableIndexCounter,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		int longValueSlot = localVariableIndexCounter.getAndIncrease();
		localVariableIndexCounter.increaseAndGet();
		int encryptedValueSlot = localVariableIndexCounter.getAndIncrease();
		ConstantPoolManager cpManager = this.currentClassNode.getConstantPoolManager();
		instructions.add(AbstractInsnNode.storeLong(longValueSlot, localVariableTable, 12));
		AbstractInsnNode.encryptIntConstant(8, instructions, cpManager, cpEntries);
		instructions.add(new NewArrayNode(8));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(AbstractInsnNode.loadLong(longValueSlot, localVariableTable, 12));
		AbstractInsnNode.encryptIntConstant(56, instructions, cpManager, cpEntries);
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_1));
		instructions.add(AbstractInsnNode.loadLong(longValueSlot, localVariableTable, 12));
		AbstractInsnNode.encryptIntConstant(48, instructions, cpManager, cpEntries);
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_2));
		instructions.add(AbstractInsnNode.loadLong(longValueSlot, localVariableTable, 12));
		AbstractInsnNode.encryptIntConstant(40, instructions, cpManager, cpEntries);
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_3));
		instructions.add(AbstractInsnNode.loadLong(longValueSlot, localVariableTable, 12));
		AbstractInsnNode.encryptIntConstant(32, instructions, cpManager, cpEntries);
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_4));
		instructions.add(AbstractInsnNode.loadLong(longValueSlot, localVariableTable, 12));
		AbstractInsnNode.encryptIntConstant(24, instructions, cpManager, cpEntries);
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_5));
		instructions.add(AbstractInsnNode.loadLong(longValueSlot, localVariableTable, 12));
		AbstractInsnNode.encryptIntConstant(16, instructions, cpManager, cpEntries);
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		AbstractInsnNode.encryptIntConstant(6, instructions, cpManager, cpEntries);
		instructions.add(AbstractInsnNode.loadLong(longValueSlot, localVariableTable, 12));
		AbstractInsnNode.encryptIntConstant(8, instructions, cpManager, cpEntries);
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(DUP));
		AbstractInsnNode.encryptIntConstant(7, instructions, cpManager, cpEntries);
		instructions.add(AbstractInsnNode.loadLong(longValueSlot, localVariableTable, 12));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(AbstractInsnNode.loadObject(cipherSlotIndex, localVariableTable, 12));
		instructions.add(InsnNode.of(SWAP));
		MethodCallWriter cipherDoFinalWriter = cpManager.getOrCreateMethodRef("javax/crypto/Cipher", "doFinal", "([B)[B", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, cipherDoFinalWriter));
		instructions.add(AbstractInsnNode.storeObject(encryptedValueSlot, localVariableTable, 12));
		instructions.add(AbstractInsnNode.loadObject(encryptedValueSlot, localVariableTable, 12));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(InsnNode.of(BALOAD));
		instructions.add(InsnNode.of(I2L));
		AbstractInsnNode.appendLongConst(255L, instructions, cpManager, cpEntries);
		instructions.add(InsnNode.of(LAND));
		AbstractInsnNode.encryptIntConstant(56, instructions, cpManager, cpEntries);
		instructions.add(InsnNode.of(LSHL));
		instructions.add(AbstractInsnNode.loadObject(encryptedValueSlot, localVariableTable, 12));
		instructions.add(InsnNode.of(ICONST_1));
		instructions.add(InsnNode.of(BALOAD));
		instructions.add(InsnNode.of(I2L));
		AbstractInsnNode.appendLongConst(255L, instructions, cpManager, cpEntries);
		instructions.add(InsnNode.of(LAND));
		AbstractInsnNode.encryptIntConstant(48, instructions, cpManager, cpEntries);
		instructions.add(InsnNode.of(LSHL));
		instructions.add(InsnNode.of(LOR));
		instructions.add(AbstractInsnNode.loadObject(encryptedValueSlot, localVariableTable, 12));
		instructions.add(InsnNode.of(ICONST_2));
		instructions.add(InsnNode.of(BALOAD));
		instructions.add(InsnNode.of(I2L));
		AbstractInsnNode.appendLongConst(255L, instructions, cpManager, cpEntries);
		instructions.add(InsnNode.of(LAND));
		AbstractInsnNode.encryptIntConstant(40, instructions, cpManager, cpEntries);
		instructions.add(InsnNode.of(LSHL));
		instructions.add(InsnNode.of(LOR));
		instructions.add(AbstractInsnNode.loadObject(encryptedValueSlot, localVariableTable, 12));
		instructions.add(InsnNode.of(ICONST_3));
		instructions.add(InsnNode.of(BALOAD));
		instructions.add(InsnNode.of(I2L));
		AbstractInsnNode.appendLongConst(255L, instructions, cpManager, cpEntries);
		instructions.add(InsnNode.of(LAND));
		AbstractInsnNode.encryptIntConstant(32, instructions, cpManager, cpEntries);
		instructions.add(InsnNode.of(LSHL));
		instructions.add(InsnNode.of(LOR));
		instructions.add(AbstractInsnNode.loadObject(encryptedValueSlot, localVariableTable, 12));
		instructions.add(InsnNode.of(ICONST_4));
		instructions.add(InsnNode.of(BALOAD));
		instructions.add(InsnNode.of(I2L));
		AbstractInsnNode.appendLongConst(255L, instructions, cpManager, cpEntries);
		instructions.add(InsnNode.of(LAND));
		AbstractInsnNode.encryptIntConstant(24, instructions, cpManager, cpEntries);
		instructions.add(InsnNode.of(LSHL));
		instructions.add(InsnNode.of(LOR));
		instructions.add(AbstractInsnNode.loadObject(encryptedValueSlot, localVariableTable, 12));
		instructions.add(InsnNode.of(ICONST_5));
		instructions.add(InsnNode.of(BALOAD));
		instructions.add(InsnNode.of(I2L));
		AbstractInsnNode.appendLongConst(255L, instructions, cpManager, cpEntries);
		instructions.add(InsnNode.of(LAND));
		AbstractInsnNode.encryptIntConstant(16, instructions, cpManager, cpEntries);
		instructions.add(InsnNode.of(LSHL));
		instructions.add(InsnNode.of(LOR));
		instructions.add(AbstractInsnNode.loadObject(encryptedValueSlot, localVariableTable, 12));
		AbstractInsnNode.encryptIntConstant(6, instructions, cpManager, cpEntries);
		instructions.add(InsnNode.of(BALOAD));
		instructions.add(InsnNode.of(I2L));
		AbstractInsnNode.appendLongConst(255L, instructions, cpManager, cpEntries);
		instructions.add(InsnNode.of(LAND));
		AbstractInsnNode.encryptIntConstant(8, instructions, cpManager, cpEntries);
		instructions.add(InsnNode.of(LSHL));
		instructions.add(InsnNode.of(LOR));
		instructions.add(AbstractInsnNode.loadObject(encryptedValueSlot, localVariableTable, 12));
		AbstractInsnNode.encryptIntConstant(7, instructions, cpManager, cpEntries);
		instructions.add(InsnNode.of(BALOAD));
		instructions.add(InsnNode.of(I2L));
		AbstractInsnNode.appendLongConst(255L, instructions, cpManager, cpEntries);
		instructions.add(InsnNode.of(LAND));
		instructions.add(InsnNode.of(LOR));
	}

	private void buildXorWithKey(
		LocalVariableTable localVariableTable,
		List<AbstractInsnNode> instructions,
		List<TypeWriter> cpEntries,
		Long xorKey,
		int xorKeyVarIndex,
		LocalVariableIndexCounter localVariableIndexCounter,
		ConstantPoolManager cpManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		instructions.add(AbstractInsnNode.loadLong(xorKeyVarIndex, localVariableTable, 12));
		instructions.add(InsnNode.of(LXOR));
	}

	public void buildDesInitCode(
		LocalVariableTable localVariableTable,
		List<AbstractInsnNode> instructions,
		int cipherVarIndex,
		Long longKey,
		LocalVariableOperand longKeyOperand,
		LocalVariableIndexCounter localVariableIndexCounter,
		List<TypeWriter> cpEntries,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		ArrayList<AbstractInsnNode> initInstructions = new ArrayList<>();
		ConstantPoolManager cpManager = this.currentClassNode.getConstantPoolManager();
		StringNode cipherTransformation = cpManager.createStringNode("DES/CBC/NoPadding", cpEntries, false);
		initInstructions.add(new WideConstantPoolInsnNode(LDC_W, cipherTransformation));
		MethodCallWriter cipherGetInstanceWriter = cpManager.getOrCreateMethodRef("javax/crypto/Cipher", "getInstance", "(Ljava/lang/String;)Ljavax/crypto/Cipher;", cpEntries, workspace, classProvider);
		initInstructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, cipherGetInstanceWriter));
		initInstructions.add(InsnNode.of(DUP));
		initInstructions.add(AbstractInsnNode.storeObject(cipherVarIndex, localVariableTable, 12));
		initInstructions.add(AbstractInsnNode.newInt(2));
		StringNode cipherAlgorithm = cpManager.createStringNode("DES", cpEntries, false);
		initInstructions.add(new WideConstantPoolInsnNode(LDC_W, cipherAlgorithm));
		MethodCallWriter secretKeyFactoryGetInstanceWriter = cpManager.getOrCreateMethodRef(
			"javax/crypto/SecretKeyFactory", "getInstance", "(Ljava/lang/String;)Ljavax/crypto/SecretKeyFactory;", cpEntries, workspace, classProvider
		);
		initInstructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, secretKeyFactoryGetInstanceWriter));
		initInstructions.add(AbstractInsnNode.newInt(8));
		initInstructions.add(new NewArrayNode(8));
		LabelNode loopStartLabel = new LabelNode(true, 1);
		LabelNode loopEndLabel = new LabelNode(true, 1);
		int byteIndexSlot = localVariableIndexCounter.getAndIncrease();
		initInstructions.add(InsnNode.of(DUP));
		initInstructions.add(InsnNode.of(ICONST_0));
		initInstructions.add(AbstractInsnNode.loadLong(longKeyOperand.getIndex(), localVariableTable, 12));
		initInstructions.add(AbstractInsnNode.newInt(56));
		initInstructions.add(InsnNode.of(LUSHR));
		initInstructions.add(InsnNode.of(L2I));
		initInstructions.add(InsnNode.of(I2B));
		initInstructions.add(InsnNode.of(BASTORE));
		initInstructions.add(InsnNode.of(ICONST_1));
		initInstructions.add(AbstractInsnNode.storeInt(byteIndexSlot, localVariableTable, 12));
		initInstructions.add(loopStartLabel);
		initInstructions.add(AbstractInsnNode.loadInt(byteIndexSlot, localVariableTable, 12));
		initInstructions.add(AbstractInsnNode.newInt(8));
		initInstructions.add(new JumpInsnNode(IF_ICMPGE, loopEndLabel));
		initInstructions.add(InsnNode.of(DUP));
		initInstructions.add(AbstractInsnNode.loadInt(byteIndexSlot, localVariableTable, 12));
		initInstructions.add(AbstractInsnNode.loadLong(longKeyOperand.getIndex(), localVariableTable, 12));
		initInstructions.add(AbstractInsnNode.loadInt(byteIndexSlot, localVariableTable, 12));
		initInstructions.add(AbstractInsnNode.newInt(8));
		initInstructions.add(InsnNode.of(IMUL));
		initInstructions.add(InsnNode.of(LSHL));
		initInstructions.add(AbstractInsnNode.newInt(56));
		initInstructions.add(InsnNode.of(LUSHR));
		initInstructions.add(InsnNode.of(L2I));
		initInstructions.add(InsnNode.of(I2B));
		initInstructions.add(InsnNode.of(BASTORE));
		initInstructions.add(AbstractInsnNode.IINC(byteIndexSlot, 1, localVariableTable, 12));
		initInstructions.add(new GotoNode(loopStartLabel));
		initInstructions.add(loopEndLabel);
		ClassInfoWriter desKeySpecClassInfo = cpManager.getOrCreateClassInfo("javax/crypto/spec/DESKeySpec", cpEntries);
		initInstructions.add(new NewNode(desKeySpecClassInfo));
		initInstructions.add(InsnNode.of(DUP_X1));
		initInstructions.add(InsnNode.of(SWAP));
		MethodCallWriter desKeySpecInitWriter = cpManager.getOrCreateMethodRef("javax/crypto/spec/DESKeySpec", "<init>", "([B)V", cpEntries, workspace, classProvider);
		initInstructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, desKeySpecInitWriter));
		MethodCallWriter generateSecretWriter = cpManager.getOrCreateMethodRef(
			"javax/crypto/SecretKeyFactory", "generateSecret", "(Ljava/security/spec/KeySpec;)Ljavax/crypto/SecretKey;", cpEntries, workspace, classProvider
		);
		initInstructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, generateSecretWriter));
		ClassInfoWriter ivParameterSpecClassInfo = cpManager.getOrCreateClassInfo("javax/crypto/spec/IvParameterSpec", cpEntries);
		initInstructions.add(new NewNode(ivParameterSpecClassInfo));
		initInstructions.add(InsnNode.of(DUP));
		initInstructions.add(AbstractInsnNode.newInt(8));
		initInstructions.add(new NewArrayNode(8));
		MethodCallWriter ivParameterSpecInitWriter = cpManager.getOrCreateMethodRef("javax/crypto/spec/IvParameterSpec", "<init>", "([B)V", cpEntries, workspace, classProvider);
		initInstructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, ivParameterSpecInitWriter));
		MethodCallWriter cipherInitWriter = cpManager.getOrCreateMethodRef(
			"javax/crypto/Cipher", "init", "(ILjava/security/Key;Ljava/security/spec/AlgorithmParameterSpec;)V", cpEntries, workspace, classProvider
		);
		initInstructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, cipherInitWriter));
		instructions.addAll(initInstructions);
	}
}
