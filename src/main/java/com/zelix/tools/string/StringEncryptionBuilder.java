package com.zelix.tools.string;

import com.zelix.exceptions.ProcessingException;
import com.zelix.framework.attributes.code.TryCatchBlockInfo;
import com.zelix.framework.code.JumpType;
import com.zelix.framework.constant.ConstantPoolManager;
import com.zelix.framework.constant.StringNode;
import com.zelix.framework.encryption.StringEntry;
import com.zelix.framework.encryption.StringLookup;
import com.zelix.framework.hierarchy.Inheritance;
import com.zelix.framework.instructions.*;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.variable.LocalVariableIndexCounter;
import com.zelix.framework.variable.LocalVariableOperand;
import com.zelix.framework.variable.LocalVariableTable;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.writer.*;
import com.zelix.framework.writer.base.AbstractMethodCallWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.config.Options;
import com.zelix.repository.ClassProvider;
import com.zelix.tools.references.ObfuscateReferencesBuilder;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.dataset.PriorityEntry;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.AbstractWorkspace;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.util.*;
import java.util.Map.Entry;

import static com.zelix.framework.instructions.OpcodeConstants.*;

public class StringEncryptionBuilder {
	private static final int XOR_MASK_LIMIT_1 = Options.smallXorMask ? 127 : 255;
	private static final int XOR_MASK_LIMIT_2 = Options.smallXorMask ? 127 : 255;
	private Random random = GlobalUtils.newRandom(1028);
	private final boolean useComplexLoopLogic;
	private final boolean useDoubleArrayStorage;
	private Iterator<Long> longKeyIterator;
	private AbstractMethodCallWriter desUtf8DecodeMethod;
	private MethodCallWriter decoderMethodRef;
	private IvParameterSpec ivParameterSpec;
	private MethodCallWriter encoderMethodRef;
	private int[] xorKeyArray;
	private FieldTypeWriter threadLocalMapField;
	private int xorCipherKey;
	private Cipher desCipherInstance;
	private InvokeDynamicCallWriter indyCallSiteRef;
	private FieldTypeWriter encryptedStringsArrayField;
	private MethodCallWriter stringInternMethodRef;
	private MethodCallWriter stringToCharArrayMethod;
	private MethodCallWriter stringConstructorMethod;
	private ClassNode targetClassNode;
	private boolean useSeparateEncodingMethods;
	private int[] lookUpXorTable;
	private SecretKeyFactory secretKeyFactory;
	private AbstractMethodCallWriter decryptorMethodRef;

	public StringEncryptionBuilder(boolean useComplexLoopLogic, boolean useDoubleArrayStorage) {
		this.useComplexLoopLogic = useComplexLoopLogic;
		this.useDoubleArrayStorage = useDoubleArrayStorage;
		this.longKeyIterator = this.random.longs(1L, Long.MAX_VALUE).iterator();
	}

	public static int[] generateRandomIntArray(Random random, int length, int bound) {
		int[] randomInts = new int[length];

		for (int i = 0; i < length; i++) {
			int boundedRandomInt = random.nextInt() % bound;
			if (boundedRandomInt < 0) {
				boundedRandomInt += bound;
			}

			randomInts[i] = ++boundedRandomInt;
		}

		return randomInts;
	}

	public static boolean isDesIndyEncryptionEnabled(ClassNode classNode) {
		return Options.ZKM_STRING_ENCRYPT_DES && Options.ZKM_STRING_ENCRYPT_INDY && ObfuscateReferencesBuilder.indySupported(classNode);
	}

	public static String simpleXorEncode(String plainText, int[] keyArray) {
		int keyLength = keyArray.length;
		char[] charArray = plainText.toCharArray();
		int charCount = charArray.length;

		for (int i = 0; i < charCount; i++) {
			charArray[i] ^= (char)keyArray[i % keyLength];
		}

		return new String(charArray);
	}

	public static byte[] longToByteArray(long value) {
		return new byte[]{
			(byte)(value >>> 56),
			(byte)(value << 8 >>> 56),
			(byte)(value << 16 >>> 56),
			(byte)(value << 24 >>> 56),
			(byte)(value << 32 >>> 56),
			(byte)(value << 40 >>> 56),
			(byte)(value << 48 >>> 56),
			(byte)(value << 56 >>> 56)
		};
	}

	private static void updateXorKeyArray(int index, int[] keyArray, int[] lookUpTable) {
		int lookUpOffset = lookUpTable[index];

		for (int i = 0; i < keyArray.length; i++) {
			keyArray[i] = (keyArray[i] + lookUpOffset) % 256;
		}
	}

	public static String complexXorEncode(String plainText, int[] keyArray, boolean useOriginalChar) {
		int[] keyArrayCopy = new int[keyArray.length];
		System.arraycopy(keyArray, 0, keyArrayCopy, 0, keyArray.length);
		int keyLength = keyArrayCopy.length;
		char[] charArray = plainText.toCharArray();
		int charCount = charArray.length;

		for (int i = 0; i < charCount; i++) {
			int keyIndex = i % keyLength;
			char originalChar = charArray[i];
			charArray[i] ^= (char)keyArrayCopy[keyIndex];
			keyArrayCopy[keyIndex] = ((keyArrayCopy[keyIndex] >>> 3 | keyArrayCopy[keyIndex] << 5) ^ (useOriginalChar ? originalChar : charArray[i])) & 0xFF;
		}

		return new String(charArray);
	}

	public void injectStringInitCode(
		LocalVariableTable localVariableTable,
		List<AbstractInsnNode> instructions,
		LocalVariableIndexCounter localVariableIndexCounter,
		Set<TypeWriter> usedEntries,
		List<TypeWriter> cpEntries,
		FieldTypeWriter stringField,
		FieldTypeWriter encryptedStringsArrayField,
		int arrayVarIndex,
		AtomicReference<String>[] stringRefs,
		ReferenceGraph<AtomicReference<String>, StringNode> referenceGraph,
		Map<AtomicReference<String>, StringEntry> stringEntryMap,
		JumpType jumpType,
		LabelNode labelNode,
		AtomicInt atomicInt,
		MultiMap<LabelNode, PriorityEntry<LabelNode>> multiMap,
		boolean useXorKeyMask,
		Long desKey,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		int stringRefCount = stringRefs.length;
		HashMap<StringNode, Integer> maskMap = GlobalUtils.newHashMap();
		Map<StringNode, Integer> chunkLengthByStringNode = this.encryptAndMapStrings(
			stringRefs, referenceGraph, stringEntryMap, usedEntries, useXorKeyMask, desKey, maskMap, cpEntries, this.targetClassNode.getConstantPoolManager()
		);
		ConstantPoolManager cpManager = this.targetClassNode.getConstantPoolManager();
		int charOffset = localVariableIndexCounter.getAndIncrease();
		int chunkLength = localVariableIndexCounter.getAndIncrease();
		int escapedString = localVariableIndexCounter.getAndIncrease();
		int arrayWriteIndex = localVariableIndexCounter.getAndIncrease();
		int escapedStringLength = localVariableIndexCounter.getAndIncrease();
		int resolvedArrayVarIndex = arrayVarIndex == -1 ? localVariableIndexCounter.getAndIncrease() : arrayVarIndex;
		AbstractInsnNode.encryptIntConstant(stringRefCount, instructions, cpManager, cpEntries);
		ClassInfoWriter stringClassInfo = cpManager.getOrCreateClassInfo("java/lang/String", cpEntries);
		instructions.add(new WideConstantPoolInsnNode(ANEWARRAY, stringClassInfo));
		instructions.add(AbstractInsnNode.storeObject(resolvedArrayVarIndex, localVariableTable, 1));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(AbstractInsnNode.storeInt(arrayWriteIndex, localVariableTable, 1));

		for (Entry<StringNode, Integer> entry : chunkLengthByStringNode.entrySet()) {
			LabelNode loopStartLabel = new LabelNode(true, 1);
			LabelNode loopEndLabel = new LabelNode(true, 1);
			StringNode stringNode = entry.getKey();
			instructions.add(new WideConstantPoolInsnNode(LDC_W, stringNode));
			instructions.add(InsnNode.of(DUP));
			instructions.add(AbstractInsnNode.storeObject(escapedString, localVariableTable, 1));
			instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, cpManager.getOrCreateMethodRef("java/lang/String", "length", "()I", cpEntries, workspace, classProvider)));
			instructions.add(AbstractInsnNode.storeInt(escapedStringLength, localVariableTable, 1));
			AbstractInsnNode.encryptIntConstant(entry.getValue(), instructions, cpManager, cpEntries);
			instructions.add(AbstractInsnNode.storeInt(chunkLength, localVariableTable, 1));
			instructions.add(InsnNode.of(ICONST_M1));
			instructions.add(AbstractInsnNode.storeInt(charOffset, localVariableTable, 1));
			instructions.add(loopStartLabel);
			if (useXorKeyMask) {
				instructions.add(AbstractInsnNode.newInt(maskMap.get(stringNode)));
			}

			instructions.add(AbstractInsnNode.IINC(charOffset, 1, localVariableTable, 1));
			instructions.add(AbstractInsnNode.loadObject(escapedString, localVariableTable, 1));
			instructions.add(AbstractInsnNode.loadInt(charOffset, localVariableTable, 1));
			instructions.add(InsnNode.of(DUP));
			instructions.add(AbstractInsnNode.loadInt(chunkLength, localVariableTable, 1));
			instructions.add(InsnNode.of(IADD));
			instructions.add(
				new WideConstantPoolInsnNode(INVOKEVIRTUAL, cpManager.getOrCreateMethodRef("java/lang/String", "substring", "(II)Ljava/lang/String;", cpEntries, workspace, classProvider))
			);
			if (jumpType == JumpType.JSR) {
				instructions.add(new JSRNode(labelNode));
			} else if (jumpType == JumpType.GOTO) {
				int returnSiteIndex = atomicInt.increaseAndGet();
				instructions.add(AbstractInsnNode.newInt(returnSiteIndex));
				instructions.add(new GotoNode(labelNode));
				LabelNode returnSiteLabel = new LabelNode(true, 1);
				instructions.add(returnSiteLabel);
				multiMap.put(labelNode, new PriorityEntry<>(returnSiteIndex, returnSiteLabel));
			}

			instructions.add(AbstractInsnNode.loadObject(resolvedArrayVarIndex, localVariableTable, 1));
			instructions.add(InsnNode.of(SWAP));
			instructions.add(AbstractInsnNode.loadInt(arrayWriteIndex, localVariableTable, 1));
			instructions.add(AbstractInsnNode.IINC(arrayWriteIndex, 1, localVariableTable, 1));
			instructions.add(InsnNode.of(SWAP));
			instructions.add(InsnNode.of(AASTORE));
			instructions.add(AbstractInsnNode.loadInt(charOffset, localVariableTable, 1));
			instructions.add(AbstractInsnNode.loadInt(chunkLength, localVariableTable, 1));
			instructions.add(InsnNode.of(IADD));
			instructions.add(InsnNode.of(DUP));
			instructions.add(AbstractInsnNode.storeInt(charOffset, localVariableTable, 1));
			instructions.add(AbstractInsnNode.loadInt(escapedStringLength, localVariableTable, 1));
			instructions.add(new JumpInsnNode(IF_ICMPGE, loopEndLabel));
			instructions.add(AbstractInsnNode.loadObject(escapedString, localVariableTable, 1));
			instructions.add(AbstractInsnNode.loadInt(charOffset, localVariableTable, 1));
			instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, cpManager.getOrCreateMethodRef("java/lang/String", "charAt", "(I)C", cpEntries, workspace, classProvider)));
			instructions.add(AbstractInsnNode.storeInt(chunkLength, localVariableTable, 1));
			instructions.add(new GotoNode(loopStartLabel));
			instructions.add(loopEndLabel);
		}

		if (stringField != null) {
			instructions.add(AbstractInsnNode.loadObject(resolvedArrayVarIndex, localVariableTable, 1));
			instructions.add(new WideConstantPoolInsnNode(PUTSTATIC, stringField));
			if (this.useDoubleArrayStorage) {
				AbstractInsnNode.encryptIntConstant(stringRefCount, instructions, cpManager, cpEntries);
				instructions.add(new WideConstantPoolInsnNode(ANEWARRAY, stringClassInfo));
				instructions.add(new WideConstantPoolInsnNode(PUTSTATIC, encryptedStringsArrayField));
			}
		}
	}

	public FieldTypeWriter getThreadLocalMapField() {
		return this.threadLocalMapField;
	}

	private void generateComplexXorDecoderCode(
		LocalVariableTable localVariableTable,
		List<AbstractInsnNode> instructions,
		List<TypeWriter> cpEntries,
		int loopVarIndex,
		MethodCallWriter toCharArrayMethodRef,
		MethodCallWriter stringConstructorMethodRef,
		MethodCallWriter internMethodRef,
		boolean standaloneMethod,
		boolean forceIntern,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		LabelNode loopIterationLabel = new LabelNode(true, 1);
		LabelNode caseLabel0 = new LabelNode(true, 1);
		LabelNode caseLabel1 = new LabelNode(true, 1);
		LabelNode caseLabel2 = new LabelNode(true, 1);
		LabelNode caseLabel3 = new LabelNode(true, 1);
		LabelNode caseLabel4 = new LabelNode(true, 1);
		LabelNode caseLabel5 = new LabelNode(true, 1);
		LabelNode switchDefaultLabel = new LabelNode(true, 1);
		LabelNode caseJoinLabel = new LabelNode(true, 1);
		LabelNode loopConditionLabel = new LabelNode(true, 1);
		LabelNode switchDispatchLabel = new LabelNode(true, 1);
		LabelNode[] caseLabels = new LabelNode[]{caseLabel0, caseLabel1, caseLabel2, caseLabel3, caseLabel4, caseLabel5};
		if (standaloneMethod) {
			instructions.add(AbstractInsnNode.loadInt(0, localVariableTable, 1));
			instructions.add(AbstractInsnNode.loadObject(1, localVariableTable, 1));
		} else {
			instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, toCharArrayMethodRef));
		}

		instructions.add(InsnNode.of(DUP_X1));
		instructions.add(InsnNode.of(ARRAYLENGTH));
		instructions.add(InsnNode.of(DUP_X2));
		instructions.add(InsnNode.of(POP));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(AbstractInsnNode.storeInt(loopVarIndex, localVariableTable, 1));
		if (this.useComplexLoopLogic) {
			instructions.add(InsnNode.of(DUP2_X1));
			instructions.add(InsnNode.of(POP2));
			instructions.add(InsnNode.of(DUP_X2));
			instructions.add(InsnNode.of(ICONST_1));
			instructions.add(new JumpInsnNode(IF_ICMPGT, loopConditionLabel));
		} else {
			instructions.add(new GotoNode(loopConditionLabel));
		}

		instructions.add(loopIterationLabel);
		instructions.add(InsnNode.of(DUP2));
		instructions.add(InsnNode.of(SWAP));
		instructions.add(AbstractInsnNode.loadInt(loopVarIndex, localVariableTable, 1));
		instructions.add(switchDispatchLabel);
		instructions.add(InsnNode.of(DUP2_X1));
		instructions.add(InsnNode.of(CALOAD));
		instructions.add(InsnNode.of(SWAP));
		instructions.add(AbstractInsnNode.loadInt(loopVarIndex, localVariableTable, 1));
		instructions.add(AbstractInsnNode.newInt(7));
		instructions.add(InsnNode.of(IREM));
		instructions.add(new TableSwitchNode(switchDefaultLabel, 0, 5, caseLabels));

		for (int i = 0; i < 6; i++) {
			instructions.add(caseLabels[i]);
			instructions.add(AbstractInsnNode.newInt(this.xorKeyArray[i]));
			instructions.add(new GotoNode(caseJoinLabel));
		}

		instructions.add(switchDefaultLabel);
		instructions.add(AbstractInsnNode.newInt(this.xorKeyArray[6]));
		instructions.add(caseJoinLabel);
		instructions.add(InsnNode.of(IXOR));
		instructions.add(InsnNode.of(IXOR));
		instructions.add(InsnNode.of(I2C));
		instructions.add(InsnNode.of(CASTORE));
		instructions.add(AbstractInsnNode.IINC(loopVarIndex, 1, localVariableTable, 1));
		if (this.useComplexLoopLogic) {
			instructions.add(InsnNode.of(DUP));
			instructions.add(new JumpInsnNode(IFNE, loopConditionLabel));
			instructions.add(InsnNode.of(DUP2));
			instructions.add(InsnNode.of(DUP_X1));
			instructions.add(new GotoNode(switchDispatchLabel));
		}

		instructions.add(loopConditionLabel);
		instructions.add(InsnNode.of(DUP2_X1));
		instructions.add(InsnNode.of(POP2));
		instructions.add(InsnNode.of(DUP_X2));
		instructions.add(AbstractInsnNode.loadInt(loopVarIndex, localVariableTable, 1));
		instructions.add(new JumpInsnNode(IF_ICMPGT, loopIterationLabel));
		instructions.add(InsnNode.of(POP));
		instructions.add(new NewNode(this.targetClassNode.getConstantPoolManager().getOrCreateClassInfo("java/lang/String", cpEntries)));
		instructions.add(InsnNode.of(DUP_X1));
		instructions.add(InsnNode.of(SWAP));
		instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, stringConstructorMethodRef));
		if (internMethodRef != null && (forceIntern || !this.useDoubleArrayStorage) && Options.ZKM_STRING_ENCRYPT_INTERN) {
			instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, internMethodRef));
		}

		if (standaloneMethod) {
			instructions.add(InsnNode.of(ARETURN));
		} else {
			instructions.add(InsnNode.of(SWAP));
			instructions.add(InsnNode.of(POP));
		}
	}

	public FieldTypeWriter getEncryptedStringsArrayField(ClassNode classNode) {
		return this.encryptedStringsArrayField;
	}

	public void injectDecryptionStubCall(
		LocalVariableTable localVariableTable,
		LabelNode stubLabel,
		LocalVariableIndexCounter localVariableIndexCounter,
		List<AbstractInsnNode> instructions,
		List<TypeWriter> cpEntries,
		JumpType jumpType,
		MultiMap<LabelNode, PriorityEntry<LabelNode>> returnSiteMap,
		Integer cipherVarIndex,
		boolean useXorKeyMask,
		ConstantPoolManager cpManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		ArrayList<AbstractInsnNode> stubInsns = new ArrayList<>();
		LabelNode stubEndLabel = new LabelNode(true, 1);
		stubInsns.add(new GotoNode(stubEndLabel));
		stubInsns.add(stubLabel);
		if (jumpType == JumpType.JSR) {
			int retAddressVarIndex = localVariableIndexCounter.getAndIncrease();
			stubInsns.add(AbstractInsnNode.storeObject(retAddressVarIndex, localVariableTable, 1));
			this.injectStringDecodeCall(localVariableTable, stubInsns, cpEntries, localVariableIndexCounter, cipherVarIndex, useXorKeyMask, workspace, classProvider);
			stubInsns.add(AbstractInsnNode.loadRetAddress(retAddressVarIndex, localVariableTable, 1));
		} else if (jumpType == JumpType.GOTO) {
			List<PriorityEntry<LabelNode>> returnSites = returnSiteMap.get(stubLabel);
			if (returnSites.size() > 1) {
				Collections.sort(returnSites);
				LabelNode defaultReturnLabel = returnSites.get(0).getValue();
				LabelNode[] returnLabels = new LabelNode[returnSites.size() - 1];

				for (int i = 1; i < returnSites.size(); i++) {
					returnLabels[i - 1] = returnSites.get(i).getValue();
				}

				if (useXorKeyMask) {
					stubInsns.add(InsnNode.of(DUP_X2));
					stubInsns.add(InsnNode.of(POP));
				} else {
					stubInsns.add(InsnNode.of(SWAP));
				}

				this.injectStringDecodeCall(localVariableTable, stubInsns, cpEntries, localVariableIndexCounter, cipherVarIndex, useXorKeyMask, workspace, classProvider);
				stubInsns.add(InsnNode.of(SWAP));
				stubInsns.add(new TableSwitchNode(defaultReturnLabel, 0, returnLabels.length - 1, returnLabels));
			} else {
				LabelNode returnLabel = returnSites.get(0).getValue();
				if (useXorKeyMask) {
					stubInsns.add(InsnNode.of(DUP_X2));
					stubInsns.add(InsnNode.of(POP));
				} else {
					stubInsns.add(InsnNode.of(SWAP));
				}

				this.injectStringDecodeCall(localVariableTable, stubInsns, cpEntries, localVariableIndexCounter, cipherVarIndex, useXorKeyMask, workspace, classProvider);
				stubInsns.add(InsnNode.of(SWAP));
				stubInsns.add(InsnNode.of(POP));
				stubInsns.add(new GotoNode(returnLabel));
			}
		}

		stubInsns.add(stubEndLabel);
		instructions.addAll(stubInsns);
	}

	public AbstractMethodCallWriter getDecryptorMethodRef(ClassNode classNode) {
		return this.decryptorMethodRef;
	}

	private Map<StringNode, Integer> encryptAndMapStrings(
		AtomicReference<String>[] stringRefs,
		ReferenceGraph<AtomicReference<String>, StringNode> referenceGraph,
		Map<AtomicReference<String>, StringEntry> stringEntryMap,
		Set<TypeWriter> usedEntries,
		boolean useXorKeyMask,
		Long desKey,
		Map<StringNode, Integer> maskMap,
		List<TypeWriter> cpEntries,
		ConstantPoolManager cpManager
	) {
		LinkedHashMap<StringNode, Integer> chunkLengthByStringNode = new LinkedHashMap<>();
		int stringRefCount = stringRefs.length;
		int stringRefIndex = 0;

		while (stringRefIndex < stringRefCount) {
			StringBuilder escapedStringBuilder = new StringBuilder();
			int firstChunkLength = -1;
			int groupUtf8Length = 0;
			Integer xorMask = null;
			int[] maskedKeyArray = null;
			if (useXorKeyMask) {
				xorMask = this.generateMaskInt(this.random, XOR_MASK_LIMIT_2);
				maskedKeyArray = this.generateXorMaskedKeyArray(xorMask);
			}

			while (stringRefIndex < stringRefCount) {
				AtomicReference<String> stringRef = stringRefs[stringRefIndex];
				StringEntry stringEntry = stringEntryMap.get(stringRef);

				for (StringNode stringNode : referenceGraph.getReferences(stringRef)) {
					usedEntries.add(stringNode);
					usedEntries.add(stringNode.getUtf8StringWriter());
				}

				StringLookup lookupType = stringEntry.getLookupType();
				String plainText = stringRef.get();
				int maskedIndex = 0;
				if (lookupType.equals(StringLookup.LOOKUP_METHOD_AND_INDEX)) {
					int[] randomKeyArray = generateRandomIntArray(this.random, 2, 255);
					plainText = complexXorEncode(plainText, randomKeyArray, true);
					updateXorKeyArray(plainText.charAt(0) & 255, randomKeyArray, this.lookUpXorTable);
					stringEntry.setLookupKey((short)(randomKeyArray[0] | randomKeyArray[1] << 8));
					maskedIndex = (short)(stringRefIndex ^ this.xorCipherKey);
				} else if (lookupType.equals(StringLookup.LOOKUP_METHOD_AND_INDEX_DES) || lookupType.equals(StringLookup.INDY_ENTRY_AND_INDEX)) {
					long entryDesKey = this.longKeyIterator.next();
					stringEntry.setEncryptionKey(entryDesKey);
					maskedIndex = (short)(stringRefIndex ^ this.xorCipherKey);
					plainText = this.desEncryptString(plainText, entryDesKey);
				}

				String encryptedString;
				if (desKey != null) {
					encryptedString = this.desEncryptString(plainText, desKey);
				} else if (useXorKeyMask) {
					encryptedString = simpleXorEncode(plainText, maskedKeyArray);
				} else {
					encryptedString = simpleXorEncode(plainText, this.xorKeyArray);
				}

				int encryptedLength = encryptedString.length();
				StringBuilder chunkBuilder = new StringBuilder(encryptedLength + 1);
				if (firstChunkLength == -1) {
					firstChunkLength = encryptedLength;
				} else {
					chunkBuilder.append((char)encryptedLength);
				}

				chunkBuilder.append(encryptedString);
				int chunkUtf8Length = GlobalUtils.getUTF8Length(chunkBuilder.toString());
				if (chunkUtf8Length > 65535) {
					String offendingString = stringRef.get();
					DebugUtils.assertTrue(
						false,
						new String[]{
							"String too large in class '"
								+ this.targetClassNode.getSourcePath()
								+ "' : "
								+ offendingString.length()
								+ " : "
								+ chunkUtf8Length
								+ " : "
								+ GlobalUtils.getUTF8Length(offendingString)
								+ " : '"
								+ GlobalUtils.escapeString(offendingString.substring(0, 50))
								+ "'"
						}
					);
				}

				if (groupUtf8Length > 0 && groupUtf8Length + chunkUtf8Length > 32768) {
					break;
				}

				groupUtf8Length += chunkUtf8Length;
				stringRefIndex++;
				escapedStringBuilder.append((CharSequence)chunkBuilder);
				if (lookupType.equals(StringLookup.LOOKUP_METHOD_AND_INDEX)
					|| lookupType.equals(StringLookup.LOOKUP_METHOD_AND_INDEX_DES)
					|| lookupType.equals(StringLookup.INDY_ENTRY_AND_INDEX)) {
					stringEntry.setIndex(maskedIndex);
				}

				if (chunkLengthByStringNode.size() == 0 && stringRefCount > 3 && stringRefIndex == stringRefCount - 2) {
					break;
				}
			}

			StringNode escapedStringNode = cpManager.createString(escapedStringBuilder.toString(), cpEntries);
			chunkLengthByStringNode.put(escapedStringNode, firstChunkLength);
			if (xorMask != null) {
				maskMap.put(escapedStringNode, xorMask);
			}
		}

		return chunkLengthByStringNode;
	}

	private void generateUtf8DecodeCode(
		LocalVariableTable localVariableTable,
		ArrayList<AbstractInsnNode> instructions,
		List<TypeWriter> cpEntries,
		ConstantPoolManager cpManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		LabelNode loopStartLabel = new LabelNode(true, 1);
		LabelNode twoByteSequenceLabel = new LabelNode(true, 1);
		LabelNode threeByteSequenceLabel = new LabelNode(true, 1);
		LabelNode loopContinueLabel = new LabelNode(true, 1);
		LabelNode loopExitLabel = new LabelNode(true, 1);
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(AbstractInsnNode.storeInt(1, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadObject(0, localVariableTable, 1));
		instructions.add(InsnNode.of(ARRAYLENGTH));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.storeInt(2, localVariableTable, 1));
		instructions.add(new NewArrayNode(5));
		instructions.add(AbstractInsnNode.storeObject(3, localVariableTable, 1));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(AbstractInsnNode.storeInt(4, localVariableTable, 1));
		instructions.add(loopStartLabel);
		instructions.add(AbstractInsnNode.loadInt(4, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadInt(2, localVariableTable, 1));
		instructions.add(new JumpInsnNode(IF_ICMPGE, loopExitLabel));
		instructions.add(AbstractInsnNode.newInt(255));
		instructions.add(AbstractInsnNode.loadObject(0, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadInt(4, localVariableTable, 1));
		instructions.add(InsnNode.of(BALOAD));
		instructions.add(InsnNode.of(IAND));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.storeInt(5, localVariableTable, 1));
		instructions.add(AbstractInsnNode.newInt(192));
		instructions.add(new JumpInsnNode(IF_ICMPGE, twoByteSequenceLabel));
		instructions.add(AbstractInsnNode.loadObject(3, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadInt(1, localVariableTable, 1));
		instructions.add(AbstractInsnNode.IINC(1, 1, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadInt(5, localVariableTable, 1));
		instructions.add(InsnNode.of(I2C));
		instructions.add(InsnNode.of(CASTORE));
		instructions.add(new GotoNode(loopContinueLabel));
		instructions.add(twoByteSequenceLabel);
		instructions.add(AbstractInsnNode.loadInt(5, localVariableTable, 1));
		instructions.add(AbstractInsnNode.newInt(224));
		instructions.add(new JumpInsnNode(IF_ICMPGE, threeByteSequenceLabel));
		instructions.add(AbstractInsnNode.loadInt(5, localVariableTable, 1));
		instructions.add(AbstractInsnNode.newInt(31));
		instructions.add(InsnNode.of(IAND));
		instructions.add(InsnNode.of(I2C));
		instructions.add(AbstractInsnNode.newInt(6));
		instructions.add(InsnNode.of(ISHL));
		instructions.add(InsnNode.of(I2C));
		instructions.add(AbstractInsnNode.storeInt(6, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadObject(0, localVariableTable, 1));
		instructions.add(AbstractInsnNode.IINC(4, 1, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadInt(4, localVariableTable, 1));
		instructions.add(InsnNode.of(BALOAD));
		instructions.add(AbstractInsnNode.storeInt(5, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadInt(6, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadInt(5, localVariableTable, 1));
		instructions.add(AbstractInsnNode.newInt(63));
		instructions.add(InsnNode.of(IAND));
		instructions.add(InsnNode.of(I2C));
		instructions.add(InsnNode.of(IOR));
		instructions.add(InsnNode.of(I2C));
		instructions.add(AbstractInsnNode.storeInt(6, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadObject(3, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadInt(1, localVariableTable, 1));
		instructions.add(AbstractInsnNode.IINC(1, 1, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadInt(6, localVariableTable, 1));
		instructions.add(InsnNode.of(CASTORE));
		instructions.add(new GotoNode(loopContinueLabel));
		instructions.add(threeByteSequenceLabel);
		instructions.add(AbstractInsnNode.loadInt(4, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadInt(2, localVariableTable, 1));
		instructions.add(InsnNode.of(ICONST_2));
		instructions.add(InsnNode.of(ISUB));
		instructions.add(new JumpInsnNode(IF_ICMPGE, loopContinueLabel));
		instructions.add(AbstractInsnNode.loadInt(5, localVariableTable, 1));
		instructions.add(AbstractInsnNode.newInt(15));
		instructions.add(InsnNode.of(IAND));
		instructions.add(InsnNode.of(I2C));
		instructions.add(AbstractInsnNode.newInt(12));
		instructions.add(InsnNode.of(ISHL));
		instructions.add(InsnNode.of(I2C));
		instructions.add(AbstractInsnNode.storeInt(6, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadObject(0, localVariableTable, 1));
		instructions.add(AbstractInsnNode.IINC(4, 1, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadInt(4, localVariableTable, 1));
		instructions.add(InsnNode.of(BALOAD));
		instructions.add(AbstractInsnNode.storeInt(5, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadInt(6, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadInt(5, localVariableTable, 1));
		instructions.add(AbstractInsnNode.newInt(63));
		instructions.add(InsnNode.of(IAND));
		instructions.add(InsnNode.of(I2C));
		instructions.add(AbstractInsnNode.newInt(6));
		instructions.add(InsnNode.of(ISHL));
		instructions.add(InsnNode.of(IOR));
		instructions.add(InsnNode.of(I2C));
		instructions.add(AbstractInsnNode.storeInt(6, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadObject(0, localVariableTable, 1));
		instructions.add(AbstractInsnNode.IINC(4, 1, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadInt(4, localVariableTable, 1));
		instructions.add(InsnNode.of(BALOAD));
		instructions.add(AbstractInsnNode.storeInt(5, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadInt(6, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadInt(5, localVariableTable, 1));
		instructions.add(AbstractInsnNode.newInt(63));
		instructions.add(InsnNode.of(IAND));
		instructions.add(InsnNode.of(I2C));
		instructions.add(InsnNode.of(IOR));
		instructions.add(InsnNode.of(I2C));
		instructions.add(AbstractInsnNode.storeInt(6, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadObject(3, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadInt(1, localVariableTable, 1));
		instructions.add(AbstractInsnNode.IINC(1, 1, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadInt(6, localVariableTable, 1));
		instructions.add(InsnNode.of(CASTORE));
		instructions.add(loopContinueLabel);
		instructions.add(AbstractInsnNode.IINC(4, 1, localVariableTable, 1));
		instructions.add(new GotoNode(loopStartLabel));
		instructions.add(loopExitLabel);
		instructions.add(new NewNode(cpManager.getOrCreateClassInfo("java/lang/String", cpEntries)));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.loadObject(3, localVariableTable, 1));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(AbstractInsnNode.loadInt(1, localVariableTable, 1));
		instructions.add(
			new WideConstantPoolInsnNode(INVOKESPECIAL, cpManager.getOrCreateMethodRef("java/lang/String", "<init>", "([CII)V", cpEntries, workspace, classProvider))
		);
		instructions.add(InsnNode.of(ARETURN));
	}

	private void injectDesDecryptCode(
		LocalVariableTable localVariableTable,
		List<AbstractInsnNode> instructions,
		List<TypeWriter> cpEntries,
		Integer cipherVarIndex,
		int bytesVarIndex,
		MethodCallWriter internMethodRef,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		ConstantPoolManager cpManager = this.targetClassNode.getConstantPoolManager();
		instructions.add(new WideConstantPoolInsnNode(LDC_W, cpManager.createStringNode("ISO-8859-1", cpEntries, false)));
		instructions.add(
			new WideConstantPoolInsnNode(
				INVOKEVIRTUAL, cpManager.getOrCreateMethodRef("java/lang/String", "getBytes", "(Ljava/lang/String;)[B", cpEntries, workspace, classProvider)
			)
		);
		instructions.add(AbstractInsnNode.loadObject(cipherVarIndex, localVariableTable, 1));
		instructions.add(InsnNode.of(SWAP));
		instructions.add(
			new WideConstantPoolInsnNode(INVOKEVIRTUAL, cpManager.getOrCreateMethodRef("javax/crypto/Cipher", "doFinal", "([B)[B", cpEntries, workspace, classProvider))
		);
		instructions.add(AbstractInsnNode.storeObject(bytesVarIndex, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadObject(bytesVarIndex, localVariableTable, 1));
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, this.desUtf8DecodeMethod));
		if (internMethodRef != null && Options.ZKM_STRING_ENCRYPT_INTERN) {
			instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, internMethodRef));
		}
	}

	public String desEncryptString(String plainText, long desKey) {
		byte[] keyBytes = longToByteArray(desKey);

		try {
			DESKeySpec keySpec = new DESKeySpec(keyBytes);
			if (this.secretKeyFactory == null) {
				this.secretKeyFactory = SecretKeyFactory.getInstance("DES");
			}

			if (this.desCipherInstance == null) {
				this.desCipherInstance = Cipher.getInstance("DES/CBC/PKCS5Padding");
				this.ivParameterSpec = new IvParameterSpec(new byte[8]);
			}

			this.desCipherInstance.init(1, this.secretKeyFactory.generateSecret(keySpec), this.ivParameterSpec);
			return new String(this.desCipherInstance.doFinal(GlobalUtils.toUTF8(plainText)), "ISO-8859-1");
		} catch (Exception encryptionException) {
			throw new ProcessingException(encryptionException.toString(), encryptionException);
		}
	}

	public int[] getXorKeyArray() {
		return this.xorKeyArray.clone();
	}

	private void initializeContext(ClassNode classNode) {
		this.targetClassNode = classNode;
		this.xorKeyArray = null;
		this.xorCipherKey = 0;
		this.lookUpXorTable = null;
		this.indyCallSiteRef = null;
		this.encoderMethodRef = null;
		this.decoderMethodRef = null;
		this.stringToCharArrayMethod = null;
		this.stringConstructorMethod = null;
		this.stringInternMethodRef = null;
		this.desUtf8DecodeMethod = null;
		this.decryptorMethodRef = null;
		this.encryptedStringsArrayField = null;
		this.threadLocalMapField = null;
	}

	private void generateLookupXorDecoderCode(
		LocalVariableTable localVariableTable,
		List<AbstractInsnNode> instructions,
		boolean hasExtraParameter,
		FieldTypeWriter encryptedStringsArrayField,
		FieldTypeWriter stringField,
		int xorCipherKey,
		int[] lookUpXorTable,
		List<TypeWriter> cpEntries,
		ConstantPoolManager cpManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		int maskSlot;
		if (hasExtraParameter) {
			maskSlot = 2;
		} else {
			maskSlot = 1;
		}

		int indexSlot = maskSlot + 1;
		int charsSlot = indexSlot + 1;
		int lookupSlot = charsSlot + 1;
		int evenKeySlot = lookupSlot + 1;
		int oddKeySlot = evenKeySlot + 1;
		int loopIndexSlot = oddKeySlot + 1;
		int paritySlot = loopIndexSlot + 1;
		LabelNode caseLabel0 = new LabelNode(true, 1);
		LabelNode caseLabel1 = new LabelNode(true, 1);
		LabelNode caseLabel2 = new LabelNode(true, 1);
		LabelNode caseLabel3 = new LabelNode(true, 1);
		LabelNode caseLabel4 = new LabelNode(true, 1);
		LabelNode caseLabel5 = new LabelNode(true, 1);
		LabelNode caseLabel6 = new LabelNode(true, 1);
		LabelNode caseLabel7 = new LabelNode(true, 1);
		LabelNode caseLabel8 = new LabelNode(true, 1);
		LabelNode caseLabel9 = new LabelNode(true, 1);
		LabelNode caseLabel10 = new LabelNode(true, 1);
		LabelNode caseLabel11 = new LabelNode(true, 1);
		LabelNode caseLabel12 = new LabelNode(true, 1);
		LabelNode caseLabel13 = new LabelNode(true, 1);
		LabelNode caseLabel14 = new LabelNode(true, 1);
		LabelNode caseLabel15 = new LabelNode(true, 1);
		LabelNode caseLabel16 = new LabelNode(true, 1);
		LabelNode caseLabel17 = new LabelNode(true, 1);
		LabelNode caseLabel18 = new LabelNode(true, 1);
		LabelNode caseLabel19 = new LabelNode(true, 1);
		LabelNode caseLabel20 = new LabelNode(true, 1);
		LabelNode caseLabel21 = new LabelNode(true, 1);
		LabelNode caseLabel22 = new LabelNode(true, 1);
		LabelNode caseLabel23 = new LabelNode(true, 1);
		LabelNode caseLabel24 = new LabelNode(true, 1);
		LabelNode caseLabel25 = new LabelNode(true, 1);
		LabelNode caseLabel26 = new LabelNode(true, 1);
		LabelNode caseLabel27 = new LabelNode(true, 1);
		LabelNode caseLabel28 = new LabelNode(true, 1);
		LabelNode caseLabel29 = new LabelNode(true, 1);
		LabelNode caseLabel30 = new LabelNode(true, 1);
		LabelNode caseLabel31 = new LabelNode(true, 1);
		LabelNode caseLabel32 = new LabelNode(true, 1);
		LabelNode caseLabel33 = new LabelNode(true, 1);
		LabelNode caseLabel34 = new LabelNode(true, 1);
		LabelNode caseLabel35 = new LabelNode(true, 1);
		LabelNode caseLabel36 = new LabelNode(true, 1);
		LabelNode caseLabel37 = new LabelNode(true, 1);
		LabelNode caseLabel38 = new LabelNode(true, 1);
		LabelNode caseLabel39 = new LabelNode(true, 1);
		LabelNode caseLabel40 = new LabelNode(true, 1);
		LabelNode caseLabel41 = new LabelNode(true, 1);
		LabelNode caseLabel42 = new LabelNode(true, 1);
		LabelNode caseLabel43 = new LabelNode(true, 1);
		LabelNode caseLabel44 = new LabelNode(true, 1);
		LabelNode caseLabel45 = new LabelNode(true, 1);
		LabelNode caseLabel46 = new LabelNode(true, 1);
		LabelNode caseLabel47 = new LabelNode(true, 1);
		LabelNode caseLabel48 = new LabelNode(true, 1);
		LabelNode caseLabel49 = new LabelNode(true, 1);
		LabelNode caseLabel50 = new LabelNode(true, 1);
		LabelNode caseLabel51 = new LabelNode(true, 1);
		LabelNode caseLabel52 = new LabelNode(true, 1);
		LabelNode caseLabel53 = new LabelNode(true, 1);
		LabelNode caseLabel54 = new LabelNode(true, 1);
		LabelNode caseLabel55 = new LabelNode(true, 1);
		LabelNode caseLabel56 = new LabelNode(true, 1);
		LabelNode caseLabel57 = new LabelNode(true, 1);
		LabelNode caseLabel58 = new LabelNode(true, 1);
		LabelNode caseLabel59 = new LabelNode(true, 1);
		LabelNode caseLabel60 = new LabelNode(true, 1);
		LabelNode caseLabel61 = new LabelNode(true, 1);
		LabelNode caseLabel62 = new LabelNode(true, 1);
		LabelNode caseLabel63 = new LabelNode(true, 1);
		LabelNode caseLabel64 = new LabelNode(true, 1);
		LabelNode caseLabel65 = new LabelNode(true, 1);
		LabelNode caseLabel66 = new LabelNode(true, 1);
		LabelNode caseLabel67 = new LabelNode(true, 1);
		LabelNode caseLabel68 = new LabelNode(true, 1);
		LabelNode caseLabel69 = new LabelNode(true, 1);
		LabelNode caseLabel70 = new LabelNode(true, 1);
		LabelNode caseLabel71 = new LabelNode(true, 1);
		LabelNode caseLabel72 = new LabelNode(true, 1);
		LabelNode caseLabel73 = new LabelNode(true, 1);
		LabelNode caseLabel74 = new LabelNode(true, 1);
		LabelNode caseLabel75 = new LabelNode(true, 1);
		LabelNode caseLabel76 = new LabelNode(true, 1);
		LabelNode caseLabel77 = new LabelNode(true, 1);
		LabelNode caseLabel78 = new LabelNode(true, 1);
		LabelNode caseLabel79 = new LabelNode(true, 1);
		LabelNode caseLabel80 = new LabelNode(true, 1);
		LabelNode caseLabel81 = new LabelNode(true, 1);
		LabelNode caseLabel82 = new LabelNode(true, 1);
		LabelNode caseLabel83 = new LabelNode(true, 1);
		LabelNode caseLabel84 = new LabelNode(true, 1);
		LabelNode caseLabel85 = new LabelNode(true, 1);
		LabelNode caseLabel86 = new LabelNode(true, 1);
		LabelNode caseLabel87 = new LabelNode(true, 1);
		LabelNode caseLabel88 = new LabelNode(true, 1);
		LabelNode caseLabel89 = new LabelNode(true, 1);
		LabelNode caseLabel90 = new LabelNode(true, 1);
		LabelNode caseLabel91 = new LabelNode(true, 1);
		LabelNode caseLabel92 = new LabelNode(true, 1);
		LabelNode caseLabel93 = new LabelNode(true, 1);
		LabelNode caseLabel94 = new LabelNode(true, 1);
		LabelNode caseLabel95 = new LabelNode(true, 1);
		LabelNode caseLabel96 = new LabelNode(true, 1);
		LabelNode caseLabel97 = new LabelNode(true, 1);
		LabelNode caseLabel98 = new LabelNode(true, 1);
		LabelNode caseLabel99 = new LabelNode(true, 1);
		LabelNode caseLabel100 = new LabelNode(true, 1);
		LabelNode caseLabel101 = new LabelNode(true, 1);
		LabelNode caseLabel102 = new LabelNode(true, 1);
		LabelNode caseLabel103 = new LabelNode(true, 1);
		LabelNode caseLabel104 = new LabelNode(true, 1);
		LabelNode caseLabel105 = new LabelNode(true, 1);
		LabelNode caseLabel106 = new LabelNode(true, 1);
		LabelNode caseLabel107 = new LabelNode(true, 1);
		LabelNode caseLabel108 = new LabelNode(true, 1);
		LabelNode caseLabel109 = new LabelNode(true, 1);
		LabelNode caseLabel110 = new LabelNode(true, 1);
		LabelNode caseLabel111 = new LabelNode(true, 1);
		LabelNode caseLabel112 = new LabelNode(true, 1);
		LabelNode caseLabel113 = new LabelNode(true, 1);
		LabelNode caseLabel114 = new LabelNode(true, 1);
		LabelNode caseLabel115 = new LabelNode(true, 1);
		LabelNode caseLabel116 = new LabelNode(true, 1);
		LabelNode caseLabel117 = new LabelNode(true, 1);
		LabelNode caseLabel118 = new LabelNode(true, 1);
		LabelNode caseLabel119 = new LabelNode(true, 1);
		LabelNode caseLabel120 = new LabelNode(true, 1);
		LabelNode caseLabel121 = new LabelNode(true, 1);
		LabelNode caseLabel122 = new LabelNode(true, 1);
		LabelNode caseLabel123 = new LabelNode(true, 1);
		LabelNode caseLabel124 = new LabelNode(true, 1);
		LabelNode caseLabel125 = new LabelNode(true, 1);
		LabelNode caseLabel126 = new LabelNode(true, 1);
		LabelNode caseLabel127 = new LabelNode(true, 1);
		LabelNode caseLabel128 = new LabelNode(true, 1);
		LabelNode caseLabel129 = new LabelNode(true, 1);
		LabelNode caseLabel130 = new LabelNode(true, 1);
		LabelNode caseLabel131 = new LabelNode(true, 1);
		LabelNode caseLabel132 = new LabelNode(true, 1);
		LabelNode caseLabel133 = new LabelNode(true, 1);
		LabelNode caseLabel134 = new LabelNode(true, 1);
		LabelNode caseLabel135 = new LabelNode(true, 1);
		LabelNode caseLabel136 = new LabelNode(true, 1);
		LabelNode caseLabel137 = new LabelNode(true, 1);
		LabelNode caseLabel138 = new LabelNode(true, 1);
		LabelNode caseLabel139 = new LabelNode(true, 1);
		LabelNode caseLabel140 = new LabelNode(true, 1);
		LabelNode caseLabel141 = new LabelNode(true, 1);
		LabelNode caseLabel142 = new LabelNode(true, 1);
		LabelNode caseLabel143 = new LabelNode(true, 1);
		LabelNode caseLabel144 = new LabelNode(true, 1);
		LabelNode caseLabel145 = new LabelNode(true, 1);
		LabelNode caseLabel146 = new LabelNode(true, 1);
		LabelNode caseLabel147 = new LabelNode(true, 1);
		LabelNode caseLabel148 = new LabelNode(true, 1);
		LabelNode caseLabel149 = new LabelNode(true, 1);
		LabelNode caseLabel150 = new LabelNode(true, 1);
		LabelNode caseLabel151 = new LabelNode(true, 1);
		LabelNode caseLabel152 = new LabelNode(true, 1);
		LabelNode caseLabel153 = new LabelNode(true, 1);
		LabelNode caseLabel154 = new LabelNode(true, 1);
		LabelNode caseLabel155 = new LabelNode(true, 1);
		LabelNode caseLabel156 = new LabelNode(true, 1);
		LabelNode caseLabel157 = new LabelNode(true, 1);
		LabelNode caseLabel158 = new LabelNode(true, 1);
		LabelNode caseLabel159 = new LabelNode(true, 1);
		LabelNode caseLabel160 = new LabelNode(true, 1);
		LabelNode caseLabel161 = new LabelNode(true, 1);
		LabelNode caseLabel162 = new LabelNode(true, 1);
		LabelNode caseLabel163 = new LabelNode(true, 1);
		LabelNode caseLabel164 = new LabelNode(true, 1);
		LabelNode caseLabel165 = new LabelNode(true, 1);
		LabelNode caseLabel166 = new LabelNode(true, 1);
		LabelNode caseLabel167 = new LabelNode(true, 1);
		LabelNode caseLabel168 = new LabelNode(true, 1);
		LabelNode caseLabel169 = new LabelNode(true, 1);
		LabelNode caseLabel170 = new LabelNode(true, 1);
		LabelNode caseLabel171 = new LabelNode(true, 1);
		LabelNode caseLabel172 = new LabelNode(true, 1);
		LabelNode caseLabel173 = new LabelNode(true, 1);
		LabelNode caseLabel174 = new LabelNode(true, 1);
		LabelNode caseLabel175 = new LabelNode(true, 1);
		LabelNode caseLabel176 = new LabelNode(true, 1);
		LabelNode caseLabel177 = new LabelNode(true, 1);
		LabelNode caseLabel178 = new LabelNode(true, 1);
		LabelNode caseLabel179 = new LabelNode(true, 1);
		LabelNode caseLabel180 = new LabelNode(true, 1);
		LabelNode caseLabel181 = new LabelNode(true, 1);
		LabelNode caseLabel182 = new LabelNode(true, 1);
		LabelNode caseLabel183 = new LabelNode(true, 1);
		LabelNode caseLabel184 = new LabelNode(true, 1);
		LabelNode caseLabel185 = new LabelNode(true, 1);
		LabelNode caseLabel186 = new LabelNode(true, 1);
		LabelNode caseLabel187 = new LabelNode(true, 1);
		LabelNode caseLabel188 = new LabelNode(true, 1);
		LabelNode caseLabel189 = new LabelNode(true, 1);
		LabelNode caseLabel190 = new LabelNode(true, 1);
		LabelNode caseLabel191 = new LabelNode(true, 1);
		LabelNode caseLabel192 = new LabelNode(true, 1);
		LabelNode caseLabel193 = new LabelNode(true, 1);
		LabelNode caseLabel194 = new LabelNode(true, 1);
		LabelNode caseLabel195 = new LabelNode(true, 1);
		LabelNode caseLabel196 = new LabelNode(true, 1);
		LabelNode caseLabel197 = new LabelNode(true, 1);
		LabelNode caseLabel198 = new LabelNode(true, 1);
		LabelNode caseLabel199 = new LabelNode(true, 1);
		LabelNode caseLabel200 = new LabelNode(true, 1);
		LabelNode caseLabel201 = new LabelNode(true, 1);
		LabelNode caseLabel202 = new LabelNode(true, 1);
		LabelNode caseLabel203 = new LabelNode(true, 1);
		LabelNode caseLabel204 = new LabelNode(true, 1);
		LabelNode caseLabel205 = new LabelNode(true, 1);
		LabelNode caseLabel206 = new LabelNode(true, 1);
		LabelNode caseLabel207 = new LabelNode(true, 1);
		LabelNode caseLabel208 = new LabelNode(true, 1);
		LabelNode caseLabel209 = new LabelNode(true, 1);
		LabelNode caseLabel210 = new LabelNode(true, 1);
		LabelNode caseLabel211 = new LabelNode(true, 1);
		LabelNode caseLabel212 = new LabelNode(true, 1);
		LabelNode caseLabel213 = new LabelNode(true, 1);
		LabelNode caseLabel214 = new LabelNode(true, 1);
		LabelNode caseLabel215 = new LabelNode(true, 1);
		LabelNode caseLabel216 = new LabelNode(true, 1);
		LabelNode caseLabel217 = new LabelNode(true, 1);
		LabelNode caseLabel218 = new LabelNode(true, 1);
		LabelNode caseLabel219 = new LabelNode(true, 1);
		LabelNode caseLabel220 = new LabelNode(true, 1);
		LabelNode caseLabel221 = new LabelNode(true, 1);
		LabelNode caseLabel222 = new LabelNode(true, 1);
		LabelNode caseLabel223 = new LabelNode(true, 1);
		LabelNode caseLabel224 = new LabelNode(true, 1);
		LabelNode caseLabel225 = new LabelNode(true, 1);
		LabelNode caseLabel226 = new LabelNode(true, 1);
		LabelNode caseLabel227 = new LabelNode(true, 1);
		LabelNode caseLabel228 = new LabelNode(true, 1);
		LabelNode caseLabel229 = new LabelNode(true, 1);
		LabelNode caseLabel230 = new LabelNode(true, 1);
		LabelNode caseLabel231 = new LabelNode(true, 1);
		LabelNode caseLabel232 = new LabelNode(true, 1);
		LabelNode caseLabel233 = new LabelNode(true, 1);
		LabelNode caseLabel234 = new LabelNode(true, 1);
		LabelNode caseLabel235 = new LabelNode(true, 1);
		LabelNode caseLabel236 = new LabelNode(true, 1);
		LabelNode caseLabel237 = new LabelNode(true, 1);
		LabelNode caseLabel238 = new LabelNode(true, 1);
		LabelNode caseLabel239 = new LabelNode(true, 1);
		LabelNode caseLabel240 = new LabelNode(true, 1);
		LabelNode caseLabel241 = new LabelNode(true, 1);
		LabelNode caseLabel242 = new LabelNode(true, 1);
		LabelNode caseLabel243 = new LabelNode(true, 1);
		LabelNode caseLabel244 = new LabelNode(true, 1);
		LabelNode caseLabel245 = new LabelNode(true, 1);
		LabelNode caseLabel246 = new LabelNode(true, 1);
		LabelNode caseLabel247 = new LabelNode(true, 1);
		LabelNode caseLabel248 = new LabelNode(true, 1);
		LabelNode caseLabel249 = new LabelNode(true, 1);
		LabelNode caseLabel250 = new LabelNode(true, 1);
		LabelNode caseLabel251 = new LabelNode(true, 1);
		LabelNode caseLabel252 = new LabelNode(true, 1);
		LabelNode caseLabel253 = new LabelNode(true, 1);
		LabelNode caseLabel254 = new LabelNode(true, 1);
		LabelNode defaultCaseLabel = new LabelNode(true, 1);
		LabelNode switchEndLabel = new LabelNode(true, 1);
		LabelNode evenKeyAdjustedLabel = new LabelNode(true, 1);
		LabelNode oddKeyAdjustedLabel = new LabelNode(true, 1);
		LabelNode loopBodyLabel = new LabelNode(true, 1);
		LabelNode oddBranchLabel = new LabelNode(true, 1);
		LabelNode branchEndLabel = new LabelNode(true, 1);
		LabelNode loopConditionLabel = new LabelNode(true, 1);
		LabelNode alreadyDecryptedLabel = new LabelNode(true, 1);
		instructions.add(AbstractInsnNode.loadInt(0, localVariableTable, 1));
		if (hasExtraParameter) {
			instructions.add(AbstractInsnNode.loadInt(maskSlot, localVariableTable, 1));
			instructions.add(InsnNode.of(IXOR));
		}

		instructions.add(AbstractInsnNode.newInt(xorCipherKey));
		instructions.add(InsnNode.of(IXOR));
		instructions.add(AbstractInsnNode.ldcInt(65535, cpManager, cpEntries));
		instructions.add(InsnNode.of(IAND));
		instructions.add(AbstractInsnNode.storeInt(indexSlot, localVariableTable, 1));
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, encryptedStringsArrayField));
		instructions.add(AbstractInsnNode.loadInt(indexSlot, localVariableTable, 1));
		instructions.add(InsnNode.of(AALOAD));
		instructions.add(new JumpInsnNode(IFNONNULL, alreadyDecryptedLabel));
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, stringField));
		instructions.add(AbstractInsnNode.loadInt(indexSlot, localVariableTable, 1));
		instructions.add(InsnNode.of(AALOAD));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, cpManager.getOrCreateMethodRef("java/lang/String", "toCharArray", "()[C", cpEntries, workspace, classProvider)));
		instructions.add(AbstractInsnNode.storeObject(charsSlot, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadObject(charsSlot, localVariableTable, 1));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(InsnNode.of(CALOAD));
		instructions.add(AbstractInsnNode.newInt(255));
		instructions.add(InsnNode.of(IAND));
		LabelNode[] caseLabels = new LabelNode[]{
			caseLabel0,
			caseLabel1,
			caseLabel2,
			caseLabel3,
			caseLabel4,
			caseLabel5,
			caseLabel6,
			caseLabel7,
			caseLabel8,
			caseLabel9,
			caseLabel10,
			caseLabel11,
			caseLabel12,
			caseLabel13,
			caseLabel14,
			caseLabel15,
			caseLabel16,
			caseLabel17,
			caseLabel18,
			caseLabel19,
			caseLabel20,
			caseLabel21,
			caseLabel22,
			caseLabel23,
			caseLabel24,
			caseLabel25,
			caseLabel26,
			caseLabel27,
			caseLabel28,
			caseLabel29,
			caseLabel30,
			caseLabel31,
			caseLabel32,
			caseLabel33,
			caseLabel34,
			caseLabel35,
			caseLabel36,
			caseLabel37,
			caseLabel38,
			caseLabel39,
			caseLabel40,
			caseLabel41,
			caseLabel42,
			caseLabel43,
			caseLabel44,
			caseLabel45,
			caseLabel46,
			caseLabel47,
			caseLabel48,
			caseLabel49,
			caseLabel50,
			caseLabel51,
			caseLabel52,
			caseLabel53,
			caseLabel54,
			caseLabel55,
			caseLabel56,
			caseLabel57,
			caseLabel58,
			caseLabel59,
			caseLabel60,
			caseLabel61,
			caseLabel62,
			caseLabel63,
			caseLabel64,
			caseLabel65,
			caseLabel66,
			caseLabel67,
			caseLabel68,
			caseLabel69,
			caseLabel70,
			caseLabel71,
			caseLabel72,
			caseLabel73,
			caseLabel74,
			caseLabel75,
			caseLabel76,
			caseLabel77,
			caseLabel78,
			caseLabel79,
			caseLabel80,
			caseLabel81,
			caseLabel82,
			caseLabel83,
			caseLabel84,
			caseLabel85,
			caseLabel86,
			caseLabel87,
			caseLabel88,
			caseLabel89,
			caseLabel90,
			caseLabel91,
			caseLabel92,
			caseLabel93,
			caseLabel94,
			caseLabel95,
			caseLabel96,
			caseLabel97,
			caseLabel98,
			caseLabel99,
			caseLabel100,
			caseLabel101,
			caseLabel102,
			caseLabel103,
			caseLabel104,
			caseLabel105,
			caseLabel106,
			caseLabel107,
			caseLabel108,
			caseLabel109,
			caseLabel110,
			caseLabel111,
			caseLabel112,
			caseLabel113,
			caseLabel114,
			caseLabel115,
			caseLabel116,
			caseLabel117,
			caseLabel118,
			caseLabel119,
			caseLabel120,
			caseLabel121,
			caseLabel122,
			caseLabel123,
			caseLabel124,
			caseLabel125,
			caseLabel126,
			caseLabel127,
			caseLabel128,
			caseLabel129,
			caseLabel130,
			caseLabel131,
			caseLabel132,
			caseLabel133,
			caseLabel134,
			caseLabel135,
			caseLabel136,
			caseLabel137,
			caseLabel138,
			caseLabel139,
			caseLabel140,
			caseLabel141,
			caseLabel142,
			caseLabel143,
			caseLabel144,
			caseLabel145,
			caseLabel146,
			caseLabel147,
			caseLabel148,
			caseLabel149,
			caseLabel150,
			caseLabel151,
			caseLabel152,
			caseLabel153,
			caseLabel154,
			caseLabel155,
			caseLabel156,
			caseLabel157,
			caseLabel158,
			caseLabel159,
			caseLabel160,
			caseLabel161,
			caseLabel162,
			caseLabel163,
			caseLabel164,
			caseLabel165,
			caseLabel166,
			caseLabel167,
			caseLabel168,
			caseLabel169,
			caseLabel170,
			caseLabel171,
			caseLabel172,
			caseLabel173,
			caseLabel174,
			caseLabel175,
			caseLabel176,
			caseLabel177,
			caseLabel178,
			caseLabel179,
			caseLabel180,
			caseLabel181,
			caseLabel182,
			caseLabel183,
			caseLabel184,
			caseLabel185,
			caseLabel186,
			caseLabel187,
			caseLabel188,
			caseLabel189,
			caseLabel190,
			caseLabel191,
			caseLabel192,
			caseLabel193,
			caseLabel194,
			caseLabel195,
			caseLabel196,
			caseLabel197,
			caseLabel198,
			caseLabel199,
			caseLabel200,
			caseLabel201,
			caseLabel202,
			caseLabel203,
			caseLabel204,
			caseLabel205,
			caseLabel206,
			caseLabel207,
			caseLabel208,
			caseLabel209,
			caseLabel210,
			caseLabel211,
			caseLabel212,
			caseLabel213,
			caseLabel214,
			caseLabel215,
			caseLabel216,
			caseLabel217,
			caseLabel218,
			caseLabel219,
			caseLabel220,
			caseLabel221,
			caseLabel222,
			caseLabel223,
			caseLabel224,
			caseLabel225,
			caseLabel226,
			caseLabel227,
			caseLabel228,
			caseLabel229,
			caseLabel230,
			caseLabel231,
			caseLabel232,
			caseLabel233,
			caseLabel234,
			caseLabel235,
			caseLabel236,
			caseLabel237,
			caseLabel238,
			caseLabel239,
			caseLabel240,
			caseLabel241,
			caseLabel242,
			caseLabel243,
			caseLabel244,
			caseLabel245,
			caseLabel246,
			caseLabel247,
			caseLabel248,
			caseLabel249,
			caseLabel250,
			caseLabel251,
			caseLabel252,
			caseLabel253,
			caseLabel254
		};
		instructions.add(new TableSwitchNode(defaultCaseLabel, 0, caseLabels.length - 1, caseLabels));

		for (int i = 0; i < caseLabels.length; i += 1) {
			instructions.add(caseLabels[i]);
			instructions.add(AbstractInsnNode.newInt(lookUpXorTable[i]));
			instructions.add(new GotoNode(switchEndLabel));
		}

		instructions.add(defaultCaseLabel);
		instructions.add(AbstractInsnNode.newInt(lookUpXorTable[255]));
		instructions.add(switchEndLabel);
		instructions.add(AbstractInsnNode.storeInt(lookupSlot, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadInt(1, localVariableTable, 1));
		if (hasExtraParameter) {
			instructions.add(AbstractInsnNode.loadInt(maskSlot, localVariableTable, 1));
			instructions.add(InsnNode.of(IXOR));
			instructions.add(InsnNode.of(DUP));
			instructions.add(AbstractInsnNode.storeInt(1, localVariableTable, 1));
		}

		instructions.add(AbstractInsnNode.newInt(255));
		instructions.add(InsnNode.of(IAND));
		instructions.add(AbstractInsnNode.loadInt(lookupSlot, localVariableTable, 1));
		instructions.add(InsnNode.of(ISUB));
		instructions.add(AbstractInsnNode.storeInt(evenKeySlot, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadInt(evenKeySlot, localVariableTable, 1));
		instructions.add(new JumpInsnNode(IFGE, evenKeyAdjustedLabel));
		instructions.add(AbstractInsnNode.IINC(evenKeySlot, 256, localVariableTable, 1));
		instructions.add(evenKeyAdjustedLabel);
		instructions.add(AbstractInsnNode.loadInt(1, localVariableTable, 1));
		instructions.add(AbstractInsnNode.ldcInt(65535, cpManager, cpEntries));
		instructions.add(InsnNode.of(IAND));
		instructions.add(AbstractInsnNode.newInt(8));
		instructions.add(InsnNode.of(IUSHR));
		instructions.add(AbstractInsnNode.loadInt(lookupSlot, localVariableTable, 1));
		instructions.add(InsnNode.of(ISUB));
		instructions.add(AbstractInsnNode.storeInt(oddKeySlot, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadInt(oddKeySlot, localVariableTable, 1));
		instructions.add(new JumpInsnNode(IFGE, oddKeyAdjustedLabel));
		instructions.add(AbstractInsnNode.IINC(oddKeySlot, 256, localVariableTable, 1));
		instructions.add(oddKeyAdjustedLabel);
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(AbstractInsnNode.storeInt(loopIndexSlot, localVariableTable, 1));
		instructions.add(new GotoNode(loopConditionLabel));
		instructions.add(loopBodyLabel);
		instructions.add(AbstractInsnNode.loadInt(loopIndexSlot, localVariableTable, 1));
		instructions.add(InsnNode.of(ICONST_2));
		instructions.add(InsnNode.of(IREM));
		instructions.add(AbstractInsnNode.storeInt(paritySlot, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadObject(charsSlot, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadInt(loopIndexSlot, localVariableTable, 1));
		instructions.add(InsnNode.of(DUP2));
		instructions.add(InsnNode.of(CALOAD));
		instructions.add(AbstractInsnNode.loadInt(paritySlot, localVariableTable, 1));
		instructions.add(new JumpInsnNode(IFNE, oddBranchLabel));
		instructions.add(AbstractInsnNode.loadInt(evenKeySlot, localVariableTable, 1));
		instructions.add(InsnNode.of(IXOR));
		instructions.add(InsnNode.of(I2C));
		instructions.add(InsnNode.of(CASTORE));
		instructions.add(AbstractInsnNode.loadInt(evenKeySlot, localVariableTable, 1));
		instructions.add(AbstractInsnNode.newInt(3));
		instructions.add(InsnNode.of(IUSHR));
		instructions.add(AbstractInsnNode.loadInt(evenKeySlot, localVariableTable, 1));
		instructions.add(AbstractInsnNode.newInt(5));
		instructions.add(InsnNode.of(ISHL));
		instructions.add(InsnNode.of(IOR));
		instructions.add(AbstractInsnNode.loadObject(charsSlot, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadInt(loopIndexSlot, localVariableTable, 1));
		instructions.add(InsnNode.of(CALOAD));
		instructions.add(InsnNode.of(IXOR));
		instructions.add(AbstractInsnNode.newInt(255));
		instructions.add(InsnNode.of(IAND));
		instructions.add(AbstractInsnNode.storeInt(evenKeySlot, localVariableTable, 1));
		instructions.add(new GotoNode(branchEndLabel));
		instructions.add(oddBranchLabel);
		instructions.add(AbstractInsnNode.loadInt(oddKeySlot, localVariableTable, 1));
		instructions.add(InsnNode.of(IXOR));
		instructions.add(InsnNode.of(I2C));
		instructions.add(InsnNode.of(CASTORE));
		instructions.add(AbstractInsnNode.loadInt(oddKeySlot, localVariableTable, 1));
		instructions.add(AbstractInsnNode.newInt(3));
		instructions.add(InsnNode.of(IUSHR));
		instructions.add(AbstractInsnNode.loadInt(oddKeySlot, localVariableTable, 1));
		instructions.add(AbstractInsnNode.newInt(5));
		instructions.add(InsnNode.of(ISHL));
		instructions.add(InsnNode.of(IOR));
		instructions.add(AbstractInsnNode.loadObject(charsSlot, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadInt(loopIndexSlot, localVariableTable, 1));
		instructions.add(InsnNode.of(CALOAD));
		instructions.add(InsnNode.of(IXOR));
		instructions.add(AbstractInsnNode.newInt(255));
		instructions.add(InsnNode.of(IAND));
		instructions.add(AbstractInsnNode.storeInt(oddKeySlot, localVariableTable, 1));
		instructions.add(branchEndLabel);
		instructions.add(AbstractInsnNode.IINC(loopIndexSlot, 1, localVariableTable, 1));
		instructions.add(loopConditionLabel);
		instructions.add(AbstractInsnNode.loadInt(loopIndexSlot, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadObject(charsSlot, localVariableTable, 1));
		instructions.add(InsnNode.of(ARRAYLENGTH));
		instructions.add(new JumpInsnNode(IF_ICMPLT, loopBodyLabel));
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, encryptedStringsArrayField));
		instructions.add(AbstractInsnNode.loadInt(indexSlot, localVariableTable, 1));
		instructions.add(new NewNode(cpManager.getOrCreateClassInfo("java/lang/String", cpEntries)));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.loadObject(charsSlot, localVariableTable, 1));
		instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, cpManager.getOrCreateMethodRef("java/lang/String", "<init>", "([C)V", cpEntries, workspace, classProvider)));
		if (this.stringInternMethodRef != null && Options.ZKM_STRING_ENCRYPT_INTERN) {
			instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, this.stringInternMethodRef));
		}

		instructions.add(InsnNode.of(AASTORE));
		instructions.add(alreadyDecryptedLabel);
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, encryptedStringsArrayField));
		instructions.add(AbstractInsnNode.loadInt(indexSlot, localVariableTable, 1));
		instructions.add(InsnNode.of(AALOAD));
		instructions.add(InsnNode.of(ARETURN));
	}

	public void injectThreadLocalMapInit(
		List<AbstractInsnNode> instructions,
		List<TypeWriter> cpEntries,
		ConstantPoolManager cpManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		instructions.add(new NewNode(cpManager.getOrCreateClassInfo("java/util/HashMap", cpEntries)));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.newInt(13));
		instructions.add(
			new WideConstantPoolInsnNode(INVOKESPECIAL, cpManager.getOrCreateMethodRef("java/util/HashMap", "<init>", "(I)V", cpEntries, workspace, classProvider))
		);
		instructions.add(new WideConstantPoolInsnNode(PUTSTATIC, this.threadLocalMapField));
	}

	public int generateMaskInt(Random random, int bound) {
		int boundedRandomInt = random.nextInt() % bound;
		if (boundedRandomInt < 0) {
			boundedRandomInt += bound;
		}

		return boundedRandomInt + 1;
	}

	private void generateSimpleXorEncoderCode(LocalVariableTable localVariableTable, List<AbstractInsnNode> instructions, MethodCallWriter toCharArrayMethodRef, boolean useComplexLoopLogic) {
		LabelNode exitLabel = new LabelNode(true, 1);
		LabelNode emptyArrayLabel = new LabelNode(true, 1);
		instructions.add(AbstractInsnNode.loadObject(0, localVariableTable, 1));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, toCharArrayMethodRef));
		instructions.add(InsnNode.of(DUP));
		if (useComplexLoopLogic) {
			instructions.add(emptyArrayLabel);
		}

		instructions.add(InsnNode.of(ARRAYLENGTH));
		if (useComplexLoopLogic) {
			instructions.add(InsnNode.of(DUP_X1));
		}

		instructions.add(AbstractInsnNode.newInt(2));
		instructions.add(new JumpInsnNode(IF_ICMPGE, exitLabel));
		if (useComplexLoopLogic) {
			instructions.add(InsnNode.of(DUP_X1));
			instructions.add(InsnNode.of(SWAP));
			instructions.add(new JumpInsnNode(IFEQ, emptyArrayLabel));
		} else {
			instructions.add(InsnNode.of(DUP));
		}

		instructions.add(InsnNode.of(ICONST_0));
		if (useComplexLoopLogic) {
			instructions.add(InsnNode.of(DUP_X2));
		}

		instructions.add(InsnNode.of(DUP2));
		instructions.add(InsnNode.of(CALOAD));
		instructions.add(AbstractInsnNode.newInt(this.xorKeyArray[6]));
		instructions.add(InsnNode.of(IXOR));
		instructions.add(InsnNode.of(I2C));
		instructions.add(InsnNode.of(CASTORE));
		instructions.add(exitLabel);
		instructions.add(InsnNode.of(ARETURN));
	}

	private void generateIndyBootstrapMethodCode(
		LocalVariableTable localVariableTable,
		ArrayList<AbstractInsnNode> instructions,
		AbstractMethodCallWriter decryptorMethodRef,
		List<TypeWriter> cpEntries,
		ConstantPoolManager cpManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		instructions.add(AbstractInsnNode.loadObject(3, localVariableTable, 1));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(InsnNode.of(AALOAD));
		instructions.add(new WideConstantPoolInsnNode(CHECKCAST, cpManager.getOrCreateClassInfo("java/lang/Integer", cpEntries)));
		instructions.add(
			new WideConstantPoolInsnNode(INVOKEVIRTUAL, cpManager.getOrCreateMethodRef("java/lang/Integer", "intValue", "()I", cpEntries, workspace, classProvider))
		);
		instructions.add(AbstractInsnNode.storeInt(4, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadObject(3, localVariableTable, 1));
		instructions.add(InsnNode.of(ICONST_1));
		instructions.add(InsnNode.of(AALOAD));
		instructions.add(new WideConstantPoolInsnNode(CHECKCAST, cpManager.getOrCreateClassInfo("java/lang/Long", cpEntries)));
		instructions.add(
			new WideConstantPoolInsnNode(INVOKEVIRTUAL, cpManager.getOrCreateMethodRef("java/lang/Long", "longValue", "()J", cpEntries, workspace, classProvider))
		);
		instructions.add(AbstractInsnNode.storeLong(5, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadInt(4, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadLong(5, localVariableTable, 1));
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, decryptorMethodRef));
		instructions.add(AbstractInsnNode.storeObject(7, localVariableTable, 1));
		instructions.add(new WideConstantPoolInsnNode(LDC_W, cpManager.getOrCreateClassInfo("java/lang/String", cpEntries)));
		instructions.add(AbstractInsnNode.loadObject(7, localVariableTable, 1));
		instructions.add(
			new WideConstantPoolInsnNode(
				INVOKESTATIC,
				cpManager.getOrCreateMethodRef(
					"java/lang/invoke/MethodHandles",
					"constant",
					"(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;",
					cpEntries,
					workspace,
					classProvider
				)
			)
		);
		instructions.add(AbstractInsnNode.storeObject(8, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadObject(1, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadObject(8, localVariableTable, 1));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(InsnNode.of(ICONST_2));
		instructions.add(new WideConstantPoolInsnNode(ANEWARRAY, cpManager.getOrCreateClassInfo("java/lang/Class", cpEntries)));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_0));
		Integer innerClassOrdinal = this.targetClassNode.hasInnerClassOrdinal() ? this.targetClassNode.getInnerClassOrdinal() : null;
		instructions.add(
			new WideConstantPoolInsnNode(
				GETSTATIC,
				cpManager.getOrCreateFieldRef(
					"java/lang/Integer", "TYPE", "Ljava/lang/Class;", cpEntries, classProvider.getClassNode("java/lang/Integer", innerClassOrdinal).findField("TYPE", "Ljava/lang/Class;")
				)
			)
		);
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_1));
		instructions.add(
			new WideConstantPoolInsnNode(
				GETSTATIC,
				cpManager.getOrCreateFieldRef(
					"java/lang/Long", "TYPE", "Ljava/lang/Class;", cpEntries, classProvider.getClassNode("java/lang/Long", innerClassOrdinal).findField("TYPE", "Ljava/lang/Class;")
				)
			)
		);
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(
			new WideConstantPoolInsnNode(
				INVOKESTATIC,
				cpManager.getOrCreateMethodRef(
					"java/lang/invoke/MethodHandles",
					"dropArguments",
					"(Ljava/lang/invoke/MethodHandle;I[Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;",
					cpEntries,
					workspace,
					classProvider
				)
			)
		);
		instructions.add(
			new WideConstantPoolInsnNode(
				INVOKEVIRTUAL,
				cpManager.getOrCreateMethodRef(
					"java/lang/invoke/MutableCallSite", "setTarget", "(Ljava/lang/invoke/MethodHandle;)V", cpEntries, workspace, classProvider
				)
			)
		);
		instructions.add(AbstractInsnNode.loadObject(7, localVariableTable, 1));
		instructions.add(InsnNode.of(ARETURN));
	}

	public void injectStringLoadAndDecode(
		LocalVariableTable localVariableTable,
		List<AbstractInsnNode> instructions,
		FieldTypeWriter stringField,
		StringNode stringNode,
		StringEntry stringEntry,
		JumpType jumpType,
		LabelNode labelNode,
		AtomicInt atomicInt,
		MultiMap<LabelNode, PriorityEntry<LabelNode>> multiMap,
		boolean useXorKeyMask,
		AtomicReference<Integer> maskRef
	) {
		if (useXorKeyMask) {
			int xorMask = this.generateMaskInt(this.random, XOR_MASK_LIMIT_2);
			maskRef.set(xorMask);
			instructions.add(AbstractInsnNode.newInt(xorMask));
		}

		if (stringNode.getTag() > 255) {
			instructions.add(new WideConstantPoolInsnNode(LDC_W, stringNode));
		} else {
			instructions.add(new LdcNode(stringNode));
		}

		if (jumpType == JumpType.JSR) {
			instructions.add(new JSRNode(labelNode));
		} else if (jumpType == JumpType.GOTO) {
			int returnSiteIndex = atomicInt.increaseAndGet();
			instructions.add(AbstractInsnNode.newInt(returnSiteIndex));
			instructions.add(new GotoNode(labelNode));
			LabelNode returnSiteLabel = new LabelNode(true, 1);
			instructions.add(returnSiteLabel);
			multiMap.put(labelNode, new PriorityEntry<>(returnSiteIndex, returnSiteLabel));
		} else {
			instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, this.encoderMethodRef));
			instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, this.decoderMethodRef));
		}

		if (stringEntry.hasArrayField()) {
			instructions.add(new WideConstantPoolInsnNode(PUTSTATIC, stringField));
		} else if (stringEntry.hasLocalArrayIndex()) {
			instructions.add(AbstractInsnNode.storeObject(stringEntry.getLocalArrayIndex(), localVariableTable, 1));
		}
	}

	public void injectDesCipherInitCode(
		LocalVariableTable localVariableTable,
		List<AbstractInsnNode> instructions,
		int cipherVarIndex,
		Long desKey,
		LocalVariableOperand keyOperand,
		LocalVariableIndexCounter localVariableIndexCounter,
		List<TypeWriter> cpEntries,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		ArrayList<AbstractInsnNode> cipherInitInsns = new ArrayList<>();
		ConstantPoolManager cpManager = this.targetClassNode.getConstantPoolManager();
		cipherInitInsns.add(new WideConstantPoolInsnNode(LDC_W, cpManager.createStringNode("DES/CBC/PKCS5Padding", cpEntries, false)));
		cipherInitInsns.add(
			new WideConstantPoolInsnNode(
				INVOKESTATIC,
				cpManager.getOrCreateMethodRef("javax/crypto/Cipher", "getInstance", "(Ljava/lang/String;)Ljavax/crypto/Cipher;", cpEntries, workspace, classProvider)
			)
		);
		cipherInitInsns.add(InsnNode.of(DUP));
		cipherInitInsns.add(AbstractInsnNode.storeObject(cipherVarIndex, localVariableTable, 1));
		cipherInitInsns.add(AbstractInsnNode.newInt(2));
		cipherInitInsns.add(new WideConstantPoolInsnNode(LDC_W, cpManager.createStringNode("DES", cpEntries, false)));
		cipherInitInsns.add(
			new WideConstantPoolInsnNode(
				INVOKESTATIC,
				cpManager.getOrCreateMethodRef(
					"javax/crypto/SecretKeyFactory", "getInstance", "(Ljava/lang/String;)Ljavax/crypto/SecretKeyFactory;", cpEntries, workspace, classProvider
				)
			)
		);
		cipherInitInsns.add(AbstractInsnNode.newInt(8));
		cipherInitInsns.add(new NewArrayNode(8));
		LabelNode keyLoopStartLabel = new LabelNode(true, 1);
		LabelNode keyLoopEndLabel = new LabelNode(true, 1);
		int localVariableIndex = localVariableIndexCounter.getAndIncrease();
		cipherInitInsns.add(InsnNode.of(DUP));
		cipherInitInsns.add(InsnNode.of(ICONST_0));
		cipherInitInsns.add(AbstractInsnNode.loadLong(keyOperand.getIndex(), localVariableTable, 1));
		cipherInitInsns.add(AbstractInsnNode.newInt(56));
		cipherInitInsns.add(InsnNode.of(LUSHR));
		cipherInitInsns.add(InsnNode.of(L2I));
		cipherInitInsns.add(InsnNode.of(I2B));
		cipherInitInsns.add(InsnNode.of(BASTORE));
		cipherInitInsns.add(InsnNode.of(ICONST_1));
		cipherInitInsns.add(AbstractInsnNode.storeInt(localVariableIndex, localVariableTable, 1));
		cipherInitInsns.add(keyLoopStartLabel);
		cipherInitInsns.add(AbstractInsnNode.loadInt(localVariableIndex, localVariableTable, 1));
		cipherInitInsns.add(AbstractInsnNode.newInt(8));
		cipherInitInsns.add(new JumpInsnNode(IF_ICMPGE, keyLoopEndLabel));
		cipherInitInsns.add(InsnNode.of(DUP));
		cipherInitInsns.add(AbstractInsnNode.loadInt(localVariableIndex, localVariableTable, 1));
		cipherInitInsns.add(AbstractInsnNode.loadLong(keyOperand.getIndex(), localVariableTable, 1));
		cipherInitInsns.add(AbstractInsnNode.loadInt(localVariableIndex, localVariableTable, 1));
		cipherInitInsns.add(AbstractInsnNode.newInt(8));
		cipherInitInsns.add(InsnNode.of(IMUL));
		cipherInitInsns.add(InsnNode.of(LSHL));
		cipherInitInsns.add(AbstractInsnNode.newInt(56));
		cipherInitInsns.add(InsnNode.of(LUSHR));
		cipherInitInsns.add(InsnNode.of(L2I));
		cipherInitInsns.add(InsnNode.of(I2B));
		cipherInitInsns.add(InsnNode.of(BASTORE));
		cipherInitInsns.add(AbstractInsnNode.IINC(localVariableIndex, 1, localVariableTable, 1));
		cipherInitInsns.add(new GotoNode(keyLoopStartLabel));
		cipherInitInsns.add(keyLoopEndLabel);
		cipherInitInsns.add(new NewNode(cpManager.getOrCreateClassInfo("javax/crypto/spec/DESKeySpec", cpEntries)));
		cipherInitInsns.add(InsnNode.of(DUP_X1));
		cipherInitInsns.add(InsnNode.of(SWAP));
		cipherInitInsns.add(
			new WideConstantPoolInsnNode(
				INVOKESPECIAL, cpManager.getOrCreateMethodRef("javax/crypto/spec/DESKeySpec", "<init>", "([B)V", cpEntries, workspace, classProvider)
			)
		);
		cipherInitInsns.add(
			new WideConstantPoolInsnNode(
				INVOKEVIRTUAL,
				cpManager.getOrCreateMethodRef(
					"javax/crypto/SecretKeyFactory", "generateSecret", "(Ljava/security/spec/KeySpec;)Ljavax/crypto/SecretKey;", cpEntries, workspace, classProvider
				)
			)
		);
		cipherInitInsns.add(new NewNode(cpManager.getOrCreateClassInfo("javax/crypto/spec/IvParameterSpec", cpEntries)));
		cipherInitInsns.add(InsnNode.of(DUP));
		cipherInitInsns.add(AbstractInsnNode.newInt(8));
		cipherInitInsns.add(new NewArrayNode(8));
		cipherInitInsns.add(
			new WideConstantPoolInsnNode(
				INVOKESPECIAL, cpManager.getOrCreateMethodRef("javax/crypto/spec/IvParameterSpec", "<init>", "([B)V", cpEntries, workspace, classProvider)
			)
		);
		cipherInitInsns.add(
			new WideConstantPoolInsnNode(
				INVOKEVIRTUAL,
				cpManager.getOrCreateMethodRef(
					"javax/crypto/Cipher", "init", "(ILjava/security/Key;Ljava/security/spec/AlgorithmParameterSpec;)V", cpEntries, workspace, classProvider
				)
			)
		);
		instructions.addAll(cipherInitInsns);
	}

	private void generateSimpleXorDecoderCode(
		LocalVariableTable localVariableTable,
		List<AbstractInsnNode> instructions,
		List<TypeWriter> cpEntries,
		int loopVarIndex,
		MethodCallWriter toCharArrayMethodRef,
		MethodCallWriter stringConstructorMethodRef,
		MethodCallWriter internMethodRef,
		boolean standaloneMethod,
		boolean forceIntern,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		LabelNode loopIterationLabel = new LabelNode(true, 1);
		LabelNode caseLabel0 = new LabelNode(true, 1);
		LabelNode caseLabel1 = new LabelNode(true, 1);
		LabelNode caseLabel2 = new LabelNode(true, 1);
		LabelNode caseLabel3 = new LabelNode(true, 1);
		LabelNode caseLabel4 = new LabelNode(true, 1);
		LabelNode caseLabel5 = new LabelNode(true, 1);
		LabelNode switchDefaultLabel = new LabelNode(true, 1);
		LabelNode caseJoinLabel = new LabelNode(true, 1);
		LabelNode loopConditionLabel = new LabelNode(true, 1);
		LabelNode switchDispatchLabel = new LabelNode(true, 1);
		LabelNode[] caseLabels = new LabelNode[]{caseLabel0, caseLabel1, caseLabel2, caseLabel3, caseLabel4, caseLabel5};
		if (standaloneMethod) {
			instructions.add(AbstractInsnNode.loadObject(0, localVariableTable, 1));
		} else {
			instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, toCharArrayMethodRef));
		}

		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ARRAYLENGTH));
		instructions.add(InsnNode.of(SWAP));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(AbstractInsnNode.storeInt(loopVarIndex, localVariableTable, 1));
		if (this.useComplexLoopLogic) {
			instructions.add(InsnNode.of(SWAP));
			instructions.add(InsnNode.of(DUP_X1));
			instructions.add(InsnNode.of(ICONST_1));
			instructions.add(new JumpInsnNode(IF_ICMPGT, loopConditionLabel));
		} else {
			instructions.add(new GotoNode(loopConditionLabel));
		}

		instructions.add(loopIterationLabel);
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.loadInt(loopVarIndex, localVariableTable, 1));
		instructions.add(switchDispatchLabel);
		instructions.add(InsnNode.of(DUP2));
		instructions.add(InsnNode.of(CALOAD));
		instructions.add(AbstractInsnNode.loadInt(loopVarIndex, localVariableTable, 1));
		instructions.add(AbstractInsnNode.newInt(7));
		instructions.add(InsnNode.of(IREM));
		instructions.add(new TableSwitchNode(switchDefaultLabel, 0, 5, caseLabels));

		for (int i = 0; i < 6; i++) {
			instructions.add(caseLabels[i]);
			instructions.add(AbstractInsnNode.newInt(this.xorKeyArray[i]));
			instructions.add(new GotoNode(caseJoinLabel));
		}

		instructions.add(switchDefaultLabel);
		instructions.add(AbstractInsnNode.newInt(this.xorKeyArray[6]));
		instructions.add(caseJoinLabel);
		instructions.add(InsnNode.of(IXOR));
		instructions.add(InsnNode.of(I2C));
		instructions.add(InsnNode.of(CASTORE));
		instructions.add(AbstractInsnNode.IINC(loopVarIndex, 1, localVariableTable, 1));
		if (this.useComplexLoopLogic) {
			instructions.add(InsnNode.of(SWAP));
			instructions.add(InsnNode.of(DUP_X1));
			instructions.add(new JumpInsnNode(IFNE, loopConditionLabel));
			instructions.add(InsnNode.of(DUP2));
			instructions.add(InsnNode.of(SWAP));
			instructions.add(new GotoNode(switchDispatchLabel));
		}

		instructions.add(loopConditionLabel);
		instructions.add(InsnNode.of(SWAP));
		instructions.add(InsnNode.of(DUP_X1));
		instructions.add(AbstractInsnNode.loadInt(loopVarIndex, localVariableTable, 1));
		instructions.add(new JumpInsnNode(IF_ICMPGT, loopIterationLabel));
		instructions.add(new NewNode(this.targetClassNode.getConstantPoolManager().getOrCreateClassInfo("java/lang/String", cpEntries)));
		instructions.add(InsnNode.of(DUP_X1));
		instructions.add(InsnNode.of(SWAP));
		instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, stringConstructorMethodRef));
		if (internMethodRef != null && (forceIntern || !this.useDoubleArrayStorage) && Options.ZKM_STRING_ENCRYPT_INTERN) {
			instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, internMethodRef));
		}

		if (standaloneMethod) {
			instructions.add(InsnNode.of(ARETURN));
		} else {
			instructions.add(InsnNode.of(SWAP));
			instructions.add(InsnNode.of(POP));
		}
	}

	private void injectStringDecodeCall(
		LocalVariableTable localVariableTable,
		List<AbstractInsnNode> instructions,
		List<TypeWriter> cpEntries,
		LocalVariableIndexCounter localVariableIndexCounter,
		Integer cipherVarIndex,
		boolean useXorKeyMask,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		if (this.useSeparateEncodingMethods) {
			instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, this.encoderMethodRef));
			instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, this.decoderMethodRef));
		} else if (cipherVarIndex != null) {
			this.injectDesDecryptCode(localVariableTable, instructions, cpEntries, cipherVarIndex, localVariableIndexCounter.getAndIncrease(), this.stringInternMethodRef, workspace, classProvider);
		} else if (useXorKeyMask) {
			this.generateComplexXorDecoderCode(
				localVariableTable,
				instructions,
				cpEntries,
				localVariableIndexCounter.getAndIncrease(),
				this.stringToCharArrayMethod,
				this.stringConstructorMethod,
				this.stringInternMethodRef,
				false,
				true,
				workspace,
				classProvider
			);
		} else {
			this.generateSimpleXorDecoderCode(
				localVariableTable,
				instructions,
				cpEntries,
				localVariableIndexCounter.getAndIncrease(),
				this.stringToCharArrayMethod,
				this.stringConstructorMethod,
				this.stringInternMethodRef,
				false,
				true,
				workspace,
				classProvider
			);
		}
	}

	public int[] generateXorMaskedKeyArray(int keyLength) {
		int[] maskedKeyArray = new int[this.xorKeyArray.length];

		for (int i = 0; i < this.xorKeyArray.length; i++) {
			maskedKeyArray[i] = this.xorKeyArray[i] ^ keyLength;
		}

		return maskedKeyArray;
	}

	public void injectDynamicFieldWrite(
		LocalVariableTable localVariableTable,
		List<AbstractInsnNode> instructions,
		FieldTypeWriter stringField,
		StringEntry stringEntry,
		Long desKey,
		LocalVariableOperand keyOperand,
		List<TypeWriter> cpEntries
	) {
		switch (stringEntry.getLookupType()) {
			case FIELD_ARRAY_AND_INDEX:
			case LOCAL_ARRAY_AND_INDEX:
				if (stringEntry.hasIndex()) {
					instructions.add(InsnNode.of(DUP));
					instructions.add(AbstractInsnNode.newInt(stringEntry.getIndex()));
					instructions.add(InsnNode.of(AALOAD));
				} else {
					instructions.add(InsnNode.of(DUP));
				}
				break;
			case LOOKUP_METHOD_AND_INDEX:
				AbstractMethodCallWriter decryptMethodRef = stringEntry.getMethodCallWriter();
				if (desKey != null && keyOperand != null) {
					ConstantPoolManager cpManagerLookup = this.targetClassNode.getConstantPoolManager();
					instructions.add(AbstractInsnNode.ldcInt(stringEntry.getIndex() ^ desKey.intValue(), cpManagerLookup, cpEntries));
					instructions.add(AbstractInsnNode.ldcInt(stringEntry.getLookupKey() ^ desKey.intValue(), cpManagerLookup, cpEntries));
					instructions.add(AbstractInsnNode.loadLong(keyOperand.getIndex(), localVariableTable, 1));
					instructions.add(InsnNode.of(L2I));
				} else {
					instructions.add(AbstractInsnNode.newInt(stringEntry.getIndex()));
					instructions.add(AbstractInsnNode.newInt(stringEntry.getLookupKey()));
				}

				instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, decryptMethodRef));
				break;
			case LOOKUP_METHOD_AND_INDEX_DES:
				ConstantPoolManager cpManagerDes = this.targetClassNode.getConstantPoolManager();
				int desStringIndex = stringEntry.getIndex();
				long desEncryptionKey = stringEntry.getEncryptionKey();
				if (desKey != null && keyOperand != null) {
					AbstractInsnNode.encryptIntConstant(desStringIndex ^ (int)(desEncryptionKey & 32767L), instructions, cpManagerDes, cpEntries);
					AbstractInsnNode.appendLongConst(desEncryptionKey ^ desKey, instructions, cpManagerDes, cpEntries);
					instructions.add(AbstractInsnNode.loadLong(keyOperand.getIndex(), localVariableTable, 1));
					instructions.add(InsnNode.of(LXOR));
				} else {
					AbstractInsnNode.encryptIntConstant(desStringIndex ^ (int)desEncryptionKey, instructions, cpManagerDes, cpEntries);
					AbstractInsnNode.appendLongConst(desEncryptionKey, instructions, cpManagerDes, cpEntries);
				}

				instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, stringEntry.getMethodCallWriter()));
				break;
			case INDY_ENTRY_AND_INDEX:
				ConstantPoolManager cpManagerIndy = this.targetClassNode.getConstantPoolManager();
				int indyStringIndex = stringEntry.getIndex();
				long indyEncryptionKey = stringEntry.getEncryptionKey();
				if (desKey != null && keyOperand != null) {
					AbstractInsnNode.encryptIntConstant(indyStringIndex ^ (int)(indyEncryptionKey & 32767L), instructions, cpManagerIndy, cpEntries);
					AbstractInsnNode.appendLongConst(indyEncryptionKey ^ desKey, instructions, cpManagerIndy, cpEntries);
					instructions.add(AbstractInsnNode.loadLong(keyOperand.getIndex(), localVariableTable, 1));
					instructions.add(InsnNode.of(LXOR));
				} else {
					AbstractInsnNode.encryptIntConstant(indyStringIndex ^ (int)indyEncryptionKey, instructions, cpManagerIndy, cpEntries);
					AbstractInsnNode.appendLongConst(indyEncryptionKey, instructions, cpManagerIndy, cpEntries);
				}

				instructions.add(new InvokeDynamicNode(stringEntry.getIndyCallWriter()));
		}

		instructions.add(new WideConstantPoolInsnNode(PUTSTATIC, stringField));
	}

	private void generateThreadLocalDecryptorCode(
		LocalVariableTable localVariableTable,
		ArrayList<AbstractInsnNode> instructions,
		FieldTypeWriter threadLocalMapField,
		FieldTypeWriter encryptedStringsArrayField,
		FieldTypeWriter stringField,
		TryCatchBlockInfo[] tryCatchBlocks,
		List<TypeWriter> cpEntries,
		ConstantPoolManager cpManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		LabelNode cacheMergeLabel = new LabelNode(true, 1);
		LabelNode keyLoopStartLabel = new LabelNode(true, 1);
		LabelNode keyLoopEndLabel = new LabelNode(true, 1);
		LabelNode exitLabel = new LabelNode(true, 1);
		instructions.add(AbstractInsnNode.loadInt(0, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadLong(1, localVariableTable, 1));
		instructions.add(AbstractInsnNode.ldcLong(32767L, cpManager, cpEntries));
		instructions.add(InsnNode.of(LAND));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(IXOR));
		instructions.add(AbstractInsnNode.newInt(this.xorCipherKey));
		instructions.add(InsnNode.of(IXOR));
		instructions.add(AbstractInsnNode.storeInt(5, localVariableTable, 1));
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, encryptedStringsArrayField));
		instructions.add(AbstractInsnNode.loadInt(5, localVariableTable, 1));
		instructions.add(InsnNode.of(AALOAD));
		instructions.add(new JumpInsnNode(IFNONNULL, exitLabel));
		LabelNode tryStartLabel = new LabelNode(true, 128);
		LabelNode tryEndLabel = new LabelNode(true, 128);
		LabelNode catchHandlerLabel = new LabelNode(true, 128);
		tryCatchBlocks[0] = new TryCatchBlockInfo(cpManager.getOrCreateClassInfo("java/lang/Exception", cpEntries), tryStartLabel, tryEndLabel, catchHandlerLabel);
		instructions.add(tryStartLabel);
		MethodCallWriter currentThreadMethodRef = cpManager.getOrCreateMethodRef(
			"java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", cpEntries, workspace, classProvider
		);
		if (this.targetClassNode.isJava5OrNewer()) {
			instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, currentThreadMethodRef));
			if (this.targetClassNode.isJava19OrNewer()) {
				instructions.add(
					new WideConstantPoolInsnNode(INVOKEVIRTUAL, cpManager.getOrCreateMethodRef("java/lang/Thread", "threadId", "()J", cpEntries, workspace, classProvider))
				);
			} else {
				instructions.add(
					new WideConstantPoolInsnNode(INVOKEVIRTUAL, cpManager.getOrCreateMethodRef("java/lang/Thread", "getId", "()J", cpEntries, workspace, classProvider))
				);
			}

			instructions.add(
				new WideConstantPoolInsnNode(
					INVOKESTATIC, cpManager.getOrCreateMethodRef("java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", cpEntries, workspace, classProvider)
				)
			);
		} else {
			instructions.add(new NewNode(cpManager.getOrCreateClassInfo("java/lang/Long", cpEntries)));
			instructions.add(InsnNode.of(DUP));
			instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, currentThreadMethodRef));
			instructions.add(
				new WideConstantPoolInsnNode(
					INVOKESTATIC, cpManager.getOrCreateMethodRef("java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I", cpEntries, workspace, classProvider)
				)
			);
			instructions.add(InsnNode.of(I2L));
			instructions.add(
				new WideConstantPoolInsnNode(INVOKESPECIAL, cpManager.getOrCreateMethodRef("java/lang/Long", "<init>", "(J)V", cpEntries, workspace, classProvider))
			);
		}

		instructions.add(AbstractInsnNode.storeObject(3, localVariableTable, 1));
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, threadLocalMapField));
		instructions.add(AbstractInsnNode.loadObject(3, localVariableTable, 1));
		instructions.add(
			new InvokeInterfaceNode(
				cpManager.getOrCreateInterfaceMethod("java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", cpEntries, workspace, classProvider)
			)
		);
		instructions.add(new WideConstantPoolInsnNode(CHECKCAST, cpManager.getOrCreateClassInfo("[Ljava/lang/Object;", cpEntries)));
		instructions.add(AbstractInsnNode.storeObject(4, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadObject(4, localVariableTable, 1));
		instructions.add(new JumpInsnNode(IFNONNULL, cacheMergeLabel));
		instructions.add(InsnNode.of(ICONST_3));
		instructions.add(new WideConstantPoolInsnNode(ANEWARRAY, cpManager.getOrCreateClassInfo("java/lang/Object", cpEntries)));
		instructions.add(AbstractInsnNode.storeObject(4, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadObject(4, localVariableTable, 1));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(new WideConstantPoolInsnNode(LDC_W, cpManager.createStringNode("DES/CBC/PKCS5Padding", cpEntries, false)));
		instructions.add(
			new WideConstantPoolInsnNode(
				INVOKESTATIC,
				cpManager.getOrCreateMethodRef(
					"javax/crypto/Cipher", "getInstance", "(Ljava/lang/String;)Ljavax/crypto/Cipher;", cpEntries, workspace, classProvider
				)
			)
		);
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(AbstractInsnNode.loadObject(4, localVariableTable, 1));
		instructions.add(InsnNode.of(ICONST_1));
		instructions.add(new WideConstantPoolInsnNode(LDC_W, cpManager.createStringNode("DES", cpEntries, false)));
		instructions.add(
			new WideConstantPoolInsnNode(
				INVOKESTATIC,
				cpManager.getOrCreateMethodRef(
					"javax/crypto/SecretKeyFactory", "getInstance", "(Ljava/lang/String;)Ljavax/crypto/SecretKeyFactory;", cpEntries, workspace, classProvider
				)
			)
		);
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(AbstractInsnNode.loadObject(4, localVariableTable, 1));
		instructions.add(InsnNode.of(ICONST_2));
		ClassInfoWriter ivSpecClassInfo = cpManager.getOrCreateClassInfo("javax/crypto/spec/IvParameterSpec", cpEntries);
		instructions.add(new NewNode(ivSpecClassInfo));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.newInt(8));
		instructions.add(new NewArrayNode(8));
		instructions.add(
			new WideConstantPoolInsnNode(
				INVOKESPECIAL, cpManager.getOrCreateMethodRef("javax/crypto/spec/IvParameterSpec", "<init>", "([B)V", cpEntries, workspace, classProvider)
			)
		);
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, threadLocalMapField));
		instructions.add(AbstractInsnNode.loadObject(3, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadObject(4, localVariableTable, 1));
		instructions.add(
			new InvokeInterfaceNode(
				cpManager.getOrCreateInterfaceMethod(
					"java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", cpEntries, workspace, classProvider
				)
			)
		);
		instructions.add(InsnNode.of(POP));
		instructions.add(tryEndLabel);
		instructions.add(new GotoNode(cacheMergeLabel));
		instructions.add(catchHandlerLabel);
		instructions.add(AbstractInsnNode.storeObject(9, localVariableTable, 1));
		instructions.add(new NewNode(cpManager.getOrCreateClassInfo("java/lang/RuntimeException", cpEntries)));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.ldcString(cpManager.getOwnerInternalName(), cpManager, cpEntries, false));
		instructions.add(AbstractInsnNode.loadObject(9, localVariableTable, 1));
		instructions.add(
			new WideConstantPoolInsnNode(
				INVOKESPECIAL,
				cpManager.getOrCreateMethodRef(
					"java/lang/RuntimeException", "<init>", "(Ljava/lang/String;Ljava/lang/Throwable;)V", cpEntries, workspace, classProvider
				)
			)
		);
		instructions.add(InsnNode.of(ATHROW));
		instructions.add(cacheMergeLabel);
		instructions.add(AbstractInsnNode.newInt(8));
		instructions.add(new NewArrayNode(8));
		instructions.add(AbstractInsnNode.storeObject(6, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadObject(6, localVariableTable, 1));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(AbstractInsnNode.loadLong(1, localVariableTable, 1));
		instructions.add(AbstractInsnNode.newInt(56));
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(InsnNode.of(ICONST_1));
		instructions.add(AbstractInsnNode.storeInt(7, localVariableTable, 1));
		instructions.add(keyLoopStartLabel);
		instructions.add(AbstractInsnNode.loadInt(7, localVariableTable, 1));
		instructions.add(AbstractInsnNode.newInt(8));
		instructions.add(new JumpInsnNode(IF_ICMPGE, keyLoopEndLabel));
		instructions.add(AbstractInsnNode.loadObject(6, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadInt(7, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadLong(1, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadInt(7, localVariableTable, 1));
		instructions.add(AbstractInsnNode.newInt(8));
		instructions.add(InsnNode.of(IMUL));
		instructions.add(InsnNode.of(LSHL));
		instructions.add(AbstractInsnNode.newInt(56));
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2B));
		instructions.add(InsnNode.of(BASTORE));
		instructions.add(AbstractInsnNode.IINC(7, 1, localVariableTable, 1));
		instructions.add(new GotoNode(keyLoopStartLabel));
		instructions.add(keyLoopEndLabel);
		instructions.add(new NewNode(cpManager.getOrCreateClassInfo("javax/crypto/spec/DESKeySpec", cpEntries)));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.loadObject(6, localVariableTable, 1));
		instructions.add(
			new WideConstantPoolInsnNode(
				INVOKESPECIAL, cpManager.getOrCreateMethodRef("javax/crypto/spec/DESKeySpec", "<init>", "([B)V", cpEntries, workspace, classProvider)
			)
		);
		instructions.add(AbstractInsnNode.storeObject(7, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadObject(4, localVariableTable, 1));
		instructions.add(InsnNode.of(ICONST_1));
		instructions.add(InsnNode.of(AALOAD));
		instructions.add(new WideConstantPoolInsnNode(CHECKCAST, cpManager.getOrCreateClassInfo("javax/crypto/SecretKeyFactory", cpEntries)));
		instructions.add(AbstractInsnNode.loadObject(7, localVariableTable, 1));
		instructions.add(
			new WideConstantPoolInsnNode(
				INVOKEVIRTUAL,
				cpManager.getOrCreateMethodRef(
					"javax/crypto/SecretKeyFactory", "generateSecret", "(Ljava/security/spec/KeySpec;)Ljavax/crypto/SecretKey;", cpEntries, workspace, classProvider
				)
			)
		);
		instructions.add(AbstractInsnNode.storeObject(8, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadObject(4, localVariableTable, 1));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(InsnNode.of(AALOAD));
		ClassInfoWriter cipherClassInfo = cpManager.getOrCreateClassInfo("javax/crypto/Cipher", cpEntries);
		instructions.add(new WideConstantPoolInsnNode(CHECKCAST, cipherClassInfo));
		instructions.add(AbstractInsnNode.newInt(2));
		instructions.add(AbstractInsnNode.loadObject(8, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadObject(4, localVariableTable, 1));
		instructions.add(InsnNode.of(ICONST_2));
		instructions.add(InsnNode.of(AALOAD));
		instructions.add(new WideConstantPoolInsnNode(CHECKCAST, ivSpecClassInfo));
		instructions.add(
			new WideConstantPoolInsnNode(
				INVOKEVIRTUAL,
				cpManager.getOrCreateMethodRef(
					"javax/crypto/Cipher", "init", "(ILjava/security/Key;Ljava/security/spec/AlgorithmParameterSpec;)V", cpEntries, workspace, classProvider
				)
			)
		);
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, stringField));
		instructions.add(AbstractInsnNode.loadInt(5, localVariableTable, 1));
		instructions.add(InsnNode.of(AALOAD));
		instructions.add(new WideConstantPoolInsnNode(LDC_W, cpManager.createStringNode("ISO-8859-1", cpEntries, false)));
		instructions.add(
			new WideConstantPoolInsnNode(
				INVOKEVIRTUAL, cpManager.getOrCreateMethodRef("java/lang/String", "getBytes", "(Ljava/lang/String;)[B", cpEntries, workspace, classProvider)
			)
		);
		instructions.add(AbstractInsnNode.storeObject(9, localVariableTable, 1));
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, encryptedStringsArrayField));
		instructions.add(AbstractInsnNode.loadInt(5, localVariableTable, 1));
		instructions.add(AbstractInsnNode.loadObject(4, localVariableTable, 1));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(InsnNode.of(AALOAD));
		instructions.add(new WideConstantPoolInsnNode(CHECKCAST, cipherClassInfo));
		instructions.add(AbstractInsnNode.loadObject(9, localVariableTable, 1));
		instructions.add(
			new WideConstantPoolInsnNode(
				INVOKEVIRTUAL, cpManager.getOrCreateMethodRef("javax/crypto/Cipher", "doFinal", "([B)[B", cpEntries, workspace, classProvider)
			)
		);
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, this.desUtf8DecodeMethod));
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(exitLabel);
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, encryptedStringsArrayField));
		instructions.add(AbstractInsnNode.loadInt(5, localVariableTable, 1));
		instructions.add(InsnNode.of(AALOAD));
		instructions.add(InsnNode.of(ARETURN));
	}

	private void generateIndyCallSiteBootstrap(
		LocalVariableTable localVariableTable,
		ArrayList<AbstractInsnNode> instructions,
		AbstractMethodCallWriter bootstrapMethodRef,
		FieldTypeWriter threadLocalMapField,
		TryCatchBlockInfo[] tryCatchBlocks,
		List<TypeWriter> cpEntries,
		ConstantPoolManager cpManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		LabelNode tryStartLabel = new LabelNode(true, 128);
		LabelNode tryEndLabel = new LabelNode(true, 128);
		LabelNode catchHandlerLabel = new LabelNode(true, 128);
		LabelNode exitLabel = new LabelNode(true, 1);
		tryCatchBlocks[0] = new TryCatchBlockInfo(cpManager.getOrCreateClassInfo("java/lang/Exception", cpEntries), tryStartLabel, tryEndLabel, catchHandlerLabel);
		instructions.add(new NewNode(cpManager.getOrCreateClassInfo("java/lang/invoke/MutableCallSite", cpEntries)));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.loadObject(2, localVariableTable, 1));
		instructions.add(
			new WideConstantPoolInsnNode(
				INVOKESPECIAL,
				cpManager.getOrCreateMethodRef(
					"java/lang/invoke/MutableCallSite", "<init>", "(Ljava/lang/invoke/MethodType;)V", cpEntries, workspace, classProvider
				)
			)
		);
		instructions.add(AbstractInsnNode.storeObject(3, localVariableTable, 1));
		instructions.add(tryStartLabel);
		instructions.add(AbstractInsnNode.loadObject(3, localVariableTable, 1));
		instructions.add(new WideConstantPoolInsnNode(LDC_W, cpManager.getOrCreateMethodHandle(MethodHandleKind.REF_INVOKE_STATIC, bootstrapMethodRef, cpEntries)));
		instructions.add(AbstractInsnNode.ldcClass("[Ljava/lang/Object;", cpManager, cpEntries));
		instructions.add(AbstractInsnNode.loadObject(2, localVariableTable, 1));
		instructions.add(
			new WideConstantPoolInsnNode(
				INVOKEVIRTUAL, cpManager.getOrCreateMethodRef("java/lang/invoke/MethodType", "parameterCount", "()I", cpEntries, workspace, classProvider)
			)
		);
		instructions.add(
			new WideConstantPoolInsnNode(
				INVOKEVIRTUAL,
				cpManager.getOrCreateMethodRef(
					"java/lang/invoke/MethodHandle", "asCollector", "(Ljava/lang/Class;I)Ljava/lang/invoke/MethodHandle;", cpEntries, workspace, classProvider
				)
			)
		);
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(InsnNode.of(ICONST_3));
		instructions.add(new WideConstantPoolInsnNode(ANEWARRAY, cpManager.getOrCreateClassInfo("java/lang/Object", cpEntries)));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(AbstractInsnNode.loadObject(0, localVariableTable, 1));
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.newInt(1));
		instructions.add(AbstractInsnNode.loadObject(3, localVariableTable, 1));
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.newInt(2));
		instructions.add(AbstractInsnNode.loadObject(1, localVariableTable, 1));
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(
			new WideConstantPoolInsnNode(
				INVOKESTATIC,
				cpManager.getOrCreateMethodRef(
					"java/lang/invoke/MethodHandles",
					"insertArguments",
					"(Ljava/lang/invoke/MethodHandle;I[Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;",
					cpEntries,
					workspace,
					classProvider
				)
			)
		);
		instructions.add(AbstractInsnNode.loadObject(2, localVariableTable, 1));
		instructions.add(
			new WideConstantPoolInsnNode(
				INVOKESTATIC,
				cpManager.getOrCreateMethodRef(
					"java/lang/invoke/MethodHandles",
					"explicitCastArguments",
					"(Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;",
					cpEntries,
					workspace,
					classProvider
				)
			)
		);
		instructions.add(
			new WideConstantPoolInsnNode(
				INVOKEVIRTUAL,
				cpManager.getOrCreateMethodRef(
					"java/lang/invoke/MutableCallSite", "setTarget", "(Ljava/lang/invoke/MethodHandle;)V", cpEntries, workspace, classProvider
				)
			)
		);
		instructions.add(tryEndLabel);
		instructions.add(new GotoNode(exitLabel));
		instructions.add(catchHandlerLabel);
		instructions.add(AbstractInsnNode.storeObject(4, localVariableTable, 1));
		instructions.add(new NewNode(cpManager.getOrCreateClassInfo("java/lang/RuntimeException", cpEntries)));
		instructions.add(InsnNode.of(DUP));
		instructions.add(new NewNode(cpManager.getOrCreateClassInfo("java/lang/StringBuilder", cpEntries)));
		instructions.add(InsnNode.of(DUP));
		instructions.add(
			new WideConstantPoolInsnNode(
				INVOKESPECIAL, cpManager.getOrCreateMethodRef("java/lang/StringBuilder", "<init>", "()V", cpEntries, workspace, classProvider)
			)
		);
		instructions.add(AbstractInsnNode.ldcString(cpManager.getOwnerInternalName(), cpManager, cpEntries, false));
		MethodCallWriter stringBuilderAppendMethodRef = cpManager.getOrCreateMethodRef(
			"java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", cpEntries, workspace, classProvider
		);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, stringBuilderAppendMethodRef));
		instructions.add(new WideConstantPoolInsnNode(LDC_W, cpManager.createStringNode(" : ", cpEntries, false)));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, stringBuilderAppendMethodRef));
		instructions.add(AbstractInsnNode.loadObject(1, localVariableTable, 1));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, stringBuilderAppendMethodRef));
		instructions.add(new WideConstantPoolInsnNode(LDC_W, cpManager.createStringNode(" : ", cpEntries, false)));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, stringBuilderAppendMethodRef));
		instructions.add(AbstractInsnNode.loadObject(2, localVariableTable, 1));
		instructions.add(
			new WideConstantPoolInsnNode(
				INVOKEVIRTUAL, cpManager.getOrCreateMethodRef("java/lang/invoke/MethodType", "toString", "()Ljava/lang/String;", cpEntries, workspace, classProvider)
			)
		);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, stringBuilderAppendMethodRef));
		instructions.add(
			new WideConstantPoolInsnNode(
				INVOKEVIRTUAL, cpManager.getOrCreateMethodRef("java/lang/StringBuilder", "toString", "()Ljava/lang/String;", cpEntries, workspace, classProvider)
			)
		);
		instructions.add(AbstractInsnNode.loadObject(4, localVariableTable, 1));
		instructions.add(
			new WideConstantPoolInsnNode(
				INVOKESPECIAL,
				cpManager.getOrCreateMethodRef(
					"java/lang/RuntimeException", "<init>", "(Ljava/lang/String;Ljava/lang/Throwable;)V", cpEntries, workspace, classProvider
				)
			)
		);
		instructions.add(InsnNode.of(ATHROW));
		instructions.add(exitLabel);
		instructions.add(AbstractInsnNode.loadObject(3, localVariableTable, 1));
		instructions.add(InsnNode.of(ARETURN));
	}

	public InvokeDynamicCallWriter getIndyCallSiteRef(ClassNode classNode) {
		return this.indyCallSiteRef;
	}

	public void setupEncryptionSchema(
		ClassNode classNode,
		Inheritance inheritance,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		List<TypeWriter> cpEntries,
		boolean useSeparateEncodingMethods,
		boolean internStrings,
		boolean useEnhancedStorage,
		boolean useXorKeyMask,
		FieldTypeWriter stringField,
		boolean hasExtraParameter,
		boolean useIndyBootstrap,
		boolean useDesEncryption
	) {
		this.initializeContext(classNode);
		ConstantPoolManager cpManager = this.targetClassNode.getConstantPoolManager();
		this.stringInternMethodRef = null;
		if (internStrings) {
			this.stringInternMethodRef = cpManager.getOrCreateMethodRef("java/lang/String", "intern", "()Ljava/lang/String;", cpEntries, workspace, classProvider);
		}

		this.useSeparateEncodingMethods = useSeparateEncodingMethods;
		if (useDesEncryption) {
			if (this.targetClassNode.hasStringDecodeMethod()) {
				this.desUtf8DecodeMethod = this.targetClassNode.getStringDecodeMethod();
			} else {
				LocalVariableTable base64DecodeLocals = new LocalVariableTable(true, "([B)Ljava/lang/String;", 7);
				ArrayList<AbstractInsnNode> base64DecodeInsns = new ArrayList<>();
				this.generateUtf8DecodeCode(base64DecodeLocals, base64DecodeInsns, cpEntries, cpManager, workspace, classProvider);
				this.desUtf8DecodeMethod = this.targetClassNode
					.getOrCreateAnyMethodRef(
						this.targetClassNode
							.createUniqueMethod(
								"([B)Ljava/lang/String;", base64DecodeInsns, 5, 7, 1, base64DecodeLocals, new TryCatchBlockInfo[0], "String Encryption", cpEntries, inheritance, workspace, 1
							),
						cpEntries
					);
				this.targetClassNode.setStringDecodeMethod(this.desUtf8DecodeMethod);
			}

			if (useEnhancedStorage) {
				this.xorCipherKey = this.random.nextInt(32766) + 1;
				FieldNode desEncryptedStringsFieldNode = this.targetClassNode
					.createUniqueField("[Ljava/lang/String;", this.targetClassNode.isInterface() ? 4 : 1, true, inheritance, workspace, 1);
				this.encryptedStringsArrayField = cpManager.getOrCreateFieldRef(
					this.targetClassNode.getInternalName(),
					desEncryptedStringsFieldNode.getNameDisplay(),
					desEncryptedStringsFieldNode.getDescriptor(),
					cpEntries,
					workspace,
					classProvider,
					true
				);
				FieldNode threadLocalMapFieldNode = this.targetClassNode
					.createUniqueField("Ljava/util/Map;", this.targetClassNode.isInterface() ? 4 : 1, true, inheritance, workspace, 1);
				this.threadLocalMapField = cpManager.getOrCreateFieldRef(
					this.targetClassNode.getInternalName(), threadLocalMapFieldNode.getNameDisplay(), threadLocalMapFieldNode.getDescriptor(), cpEntries, workspace, classProvider, true
				);
				TryCatchBlockInfo[] desDecryptorTryCatch = new TryCatchBlockInfo[]{null};
				LocalVariableTable desDecryptorLocals = new LocalVariableTable(true, "(IJ)Ljava/lang/String;", 10);
				ArrayList<AbstractInsnNode> desDecryptorInsns = new ArrayList<>();
				this.generateThreadLocalDecryptorCode(
					desDecryptorLocals,
					desDecryptorInsns,
					this.threadLocalMapField,
					this.encryptedStringsArrayField,
					stringField,
					desDecryptorTryCatch,
					cpEntries,
					cpManager,
					workspace,
					classProvider
				);
				this.decryptorMethodRef = this.targetClassNode
					.getOrCreateAnyMethodRef(
						this.targetClassNode
							.createUniqueMethod("(IJ)Ljava/lang/String;", desDecryptorInsns, 6, 10, 1, desDecryptorLocals, desDecryptorTryCatch, "String Encryption", cpEntries, inheritance, workspace, 1),
						cpEntries
					);
				if (useIndyBootstrap) {
					LocalVariableTable indyBootstrapLocals = new LocalVariableTable(
						true, "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/invoke/MutableCallSite;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;", 9
					);
					ArrayList<AbstractInsnNode> indyBootstrapInsns = new ArrayList<>();
					this.generateIndyBootstrapMethodCode(indyBootstrapLocals, indyBootstrapInsns, this.decryptorMethodRef, cpEntries, cpManager, workspace, classProvider);
					AbstractMethodCallWriter indyBootstrapMethodRef = this.targetClassNode
						.getOrCreateAnyMethodRef(
							this.targetClassNode
								.createUniqueMethod(
									"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/invoke/MutableCallSite;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;",
									indyBootstrapInsns,
									7,
									9,
									1,
									indyBootstrapLocals,
									new TryCatchBlockInfo[0],
									"String Encryption",
									cpEntries,
									inheritance,
									workspace,
									1
								),
							cpEntries
						);
					TryCatchBlockInfo[] callSiteTryCatch = new TryCatchBlockInfo[]{null};
					LocalVariableTable callSiteBootstrapLocals = new LocalVariableTable(
						true, "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", 5
					);
					ArrayList<AbstractInsnNode> callSiteBootstrapInsns = new ArrayList<>();
					this.generateIndyCallSiteBootstrap(
						callSiteBootstrapLocals, callSiteBootstrapInsns, indyBootstrapMethodRef, this.threadLocalMapField, callSiteTryCatch, cpEntries, cpManager, workspace, classProvider
					);
					this.indyCallSiteRef = this.targetClassNode
						.createInvokeDynamicCall(
							String.valueOf((char)(97 + this.random.nextInt(26))),
							"(IJ)Ljava/lang/String;",
							this.targetClassNode
								.getOrCreateAnyMethodRef(
									this.targetClassNode
										.createUniqueMethod(
											"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
											callSiteBootstrapInsns,
											7,
											5,
											1,
											callSiteBootstrapLocals,
											callSiteTryCatch,
											"String Encryption",
											cpEntries,
											inheritance,
											workspace,
											1
										),
									cpEntries
								),
							cpEntries,
							cpManager
						);
				}
			}
		} else {
			this.xorKeyArray = generateRandomIntArray(this.random, 7, XOR_MASK_LIMIT_1);
			this.stringToCharArrayMethod = cpManager.getOrCreateMethodRef("java/lang/String", "toCharArray", "()[C", cpEntries, workspace, classProvider);
			this.stringConstructorMethod = cpManager.getOrCreateMethodRef("java/lang/String", "<init>", "([C)V", cpEntries, workspace, classProvider);
			if (this.useSeparateEncodingMethods) {
				LocalVariableTable encoderLocals = new LocalVariableTable(true, "(Ljava/lang/String;)[C", 1);
				ArrayList<AbstractInsnNode> encoderInsns = new ArrayList<>();
				this.generateSimpleXorEncoderCode(encoderLocals, encoderInsns, this.stringToCharArrayMethod, this.useComplexLoopLogic);
				this.encoderMethodRef = this.targetClassNode
					.getOrCreateClassMethodRef(
						this.targetClassNode
							.createUniqueMethod(
								"(Ljava/lang/String;)[C", encoderInsns, 6, 1, 1, encoderLocals, new TryCatchBlockInfo[0], "String Encryption", cpEntries, inheritance, workspace, 1
							),
						cpEntries
					);
				String decoderDescriptor = useXorKeyMask ? "(I[C)Ljava/lang/String;" : "([C)Ljava/lang/String;";
				LocalVariableTable decoderLocals = new LocalVariableTable(true, decoderDescriptor, 2);
				ArrayList<AbstractInsnNode> decoderInsns = new ArrayList<>();
				if (useXorKeyMask) {
					this.generateComplexXorDecoderCode(
						decoderLocals,
						decoderInsns,
						cpEntries,
						1,
						this.stringToCharArrayMethod,
						this.stringConstructorMethod,
						this.stringInternMethodRef,
						true,
						false,
						workspace,
						classProvider
					);
				} else {
					this.generateSimpleXorDecoderCode(
						decoderLocals,
						decoderInsns,
						cpEntries,
						1,
						this.stringToCharArrayMethod,
						this.stringConstructorMethod,
						this.stringInternMethodRef,
						true,
						false,
						workspace,
						classProvider
					);
				}

				this.decoderMethodRef = this.targetClassNode
					.getOrCreateClassMethodRef(
						this.targetClassNode
							.createUniqueMethod(decoderDescriptor, decoderInsns, 7, 2, 1, decoderLocals, new TryCatchBlockInfo[0], "String Encryption", cpEntries, inheritance, workspace, 1),
						cpEntries
					);
			}

			if (useEnhancedStorage) {
				FieldNode xorEncryptedStringsFieldNode = this.targetClassNode
					.createUniqueField("[Ljava/lang/String;", this.targetClassNode.isInterface() ? 4 : 1, true, inheritance, workspace, 1);
				this.encryptedStringsArrayField = cpManager.getOrCreateFieldRef(
					this.targetClassNode.getInternalName(), xorEncryptedStringsFieldNode.getNameDisplay(), xorEncryptedStringsFieldNode.getDescriptor(), cpEntries, workspace, classProvider, true
				);
				String threadLocalDecryptorDescriptor = hasExtraParameter ? "(III)Ljava/lang/String;" : "(II)Ljava/lang/String;";
				int decryptorLocalCapacity = hasExtraParameter ? 10 : 9;
				LocalVariableTable threadLocalDecryptorLocals = new LocalVariableTable(true, threadLocalDecryptorDescriptor, decryptorLocalCapacity);
				ArrayList<AbstractInsnNode> threadLocalDecryptorInsns = new ArrayList<>();
				int[] xorCipherKeyInts = generateRandomIntArray(this.random, 2, 255);
				this.xorCipherKey = (short)(xorCipherKeyInts[1] << 8 | xorCipherKeyInts[0]);
				this.lookUpXorTable = this.generateShuffledLookupTable();
				this.generateLookupXorDecoderCode(
					threadLocalDecryptorLocals,
					threadLocalDecryptorInsns,
					hasExtraParameter,
					this.encryptedStringsArrayField,
					stringField,
					this.xorCipherKey,
					this.lookUpXorTable,
					cpEntries,
					cpManager,
					workspace,
					classProvider
				);
				this.decryptorMethodRef = this.targetClassNode
					.getOrCreateAnyMethodRef(
						this.targetClassNode
							.createUniqueMethod(threadLocalDecryptorDescriptor, threadLocalDecryptorInsns, 5, decryptorLocalCapacity, 1, threadLocalDecryptorLocals, new TryCatchBlockInfo[0], "String Encryption", cpEntries, inheritance, workspace, 1),
						cpEntries
					);
			}
		}
	}

	public int[] generateShuffledLookupTable() {
		int[] lookUpTable = new int[]{
			0,
			1,
			2,
			3,
			4,
			5,
			6,
			7,
			8,
			9,
			10,
			11,
			12,
			13,
			14,
			15,
			16,
			17,
			18,
			19,
			20,
			21,
			22,
			23,
			24,
			25,
			26,
			27,
			28,
			29,
			30,
			31,
			32,
			33,
			34,
			35,
			36,
			37,
			38,
			39,
			40,
			41,
			42,
			43,
			44,
			45,
			46,
			47,
			48,
			49,
			50,
			51,
			52,
			53,
			54,
			55,
			56,
			57,
			58,
			59,
			60,
			61,
			62,
			63,
			64,
			65,
			66,
			67,
			68,
			69,
			70,
			71,
			72,
			73,
			74,
			75,
			76,
			77,
			78,
			79,
			80,
			81,
			82,
			83,
			84,
			85,
			86,
			87,
			88,
			89,
			90,
			91,
			92,
			93,
			94,
			95,
			96,
			97,
			98,
			99,
			100,
			101,
			102,
			103,
			104,
			105,
			106,
			107,
			108,
			109,
			110,
			111,
			112,
			113,
			114,
			115,
			116,
			117,
			118,
			119,
			120,
			121,
			122,
			123,
			124,
			125,
			126,
			127,
			128,
			129,
			130,
			131,
			132,
			133,
			134,
			135,
			136,
			137,
			138,
			139,
			140,
			141,
			142,
			143,
			144,
			145,
			146,
			147,
			148,
			149,
			150,
			151,
			152,
			153,
			154,
			155,
			156,
			157,
			158,
			159,
			160,
			161,
			162,
			163,
			164,
			165,
			166,
			167,
			168,
			169,
			170,
			171,
			172,
			173,
			174,
			175,
			176,
			177,
			178,
			179,
			180,
			181,
			182,
			183,
			184,
			185,
			186,
			187,
			188,
			189,
			190,
			191,
			192,
			193,
			194,
			195,
			196,
			197,
			198,
			199,
			200,
			201,
			202,
			203,
			204,
			205,
			206,
			207,
			208,
			209,
			210,
			211,
			212,
			213,
			214,
			215,
			216,
			217,
			218,
			219,
			220,
			221,
			222,
			223,
			224,
			225,
			226,
			227,
			228,
			229,
			230,
			231,
			232,
			233,
			234,
			235,
			236,
			237,
			238,
			239,
			240,
			241,
			242,
			243,
			244,
			245,
			246,
			247,
			248,
			249,
			250,
			251,
			252,
			253,
			254,
			255
		};
		GlobalUtils.shuffle(lookUpTable, this.random);
		return lookUpTable;
	}
}
