package com.zelix.framework.utils;

import com.zelix.framework.constant.ConstantPoolManager;
import com.zelix.framework.instructions.AbstractInsnNode;
import com.zelix.framework.instructions.InsnNode;
import com.zelix.framework.instructions.NewNode;
import com.zelix.framework.instructions.WideConstantPoolInsnNode;
import com.zelix.framework.variable.LocalVariableTable;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.MethodCallWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.repository.ClassProvider;
import com.zelix.workspace.AbstractWorkspace;
import java.util.List;

import static com.zelix.framework.instructions.OpcodeConstants.*;

public class PrimitiveBoxingUtils {
	public static int loadAndBoxByte(
		int varIndex,
		List<AbstractInsnNode> instructions,
		boolean useValueOf,
		LocalVariableTable localVarTable,
		List<TypeWriter> cpEntries,
		ConstantPoolManager cpManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		int pcOffset
	) {
		byte addedSize;
		if (useValueOf) {
			instructions.add(AbstractInsnNode.loadInt(varIndex, localVarTable, pcOffset));
			MethodCallWriter valueOfMethod = cpManager.getOrCreateMethodRef("java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, valueOfMethod));
			addedSize = 2;
		} else {
			ClassInfoWriter wrapperClass = cpManager.getOrCreateClassInfo("java/lang/Byte", cpEntries);
			instructions.add(new NewNode(wrapperClass));
			instructions.add(InsnNode.of(DUP));
			instructions.add(AbstractInsnNode.loadInt(varIndex, localVarTable, pcOffset));
			MethodCallWriter constructorMethod = cpManager.getOrCreateMethodRef("java/lang/Byte", "<init>", "(B)V", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, constructorMethod));
			addedSize = 4;
		}

		return addedSize;
	}

	public static int loadAndBoxBoolean(
		int varIndex,
		List<AbstractInsnNode> instructions,
		boolean useValueOf,
		LocalVariableTable localVarTable,
		List<TypeWriter> cpEntries,
		ConstantPoolManager cpManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		int pcOffset
	) {
		byte addedSize;
		if (useValueOf) {
			instructions.add(AbstractInsnNode.loadInt(varIndex, localVarTable, pcOffset));
			MethodCallWriter valueOfMethod = cpManager.getOrCreateMethodRef(
				"java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", cpEntries, workspace, classProvider
			);
			instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, valueOfMethod));
			addedSize = 2;
		} else {
			ClassInfoWriter wrapperClass = cpManager.getOrCreateClassInfo("java/lang/Boolean", cpEntries);
			instructions.add(new NewNode(wrapperClass));
			instructions.add(InsnNode.of(DUP));
			instructions.add(AbstractInsnNode.loadInt(varIndex, localVarTable, pcOffset));
			MethodCallWriter constructorMethod = cpManager.getOrCreateMethodRef("java/lang/Boolean", "<init>", "(Z)V", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, constructorMethod));
			addedSize = 4;
		}

		return addedSize;
	}

	public static int boxLong(
		List<AbstractInsnNode> instructions,
		boolean useValueOf,
		LocalVariableTable localVarTable,
		List<TypeWriter> cpEntries,
		ConstantPoolManager cpManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		int pcOffset
	) {
		byte addedSize;
		if (useValueOf) {
			addedSize = 1;
			MethodCallWriter valueOfMethod = cpManager.getOrCreateMethodRef("java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, valueOfMethod));
		} else {
			addedSize = 4;
			ClassInfoWriter wrapperClass = cpManager.getOrCreateClassInfo("java/lang/Long", cpEntries);
			instructions.add(new NewNode(wrapperClass));
			instructions.add(InsnNode.of(DUP_X2));
			instructions.add(InsnNode.of(DUP_X2));
			instructions.add(InsnNode.of(POP));
			MethodCallWriter constructorMethod = cpManager.getOrCreateMethodRef("java/lang/Long", "<init>", "(J)V", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, constructorMethod));
		}

		return addedSize;
	}

	public static int boxDouble(
		List<AbstractInsnNode> instructions,
		boolean useValueOf,
		LocalVariableTable localVarTable,
		List<TypeWriter> cpEntries,
		ConstantPoolManager cpManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		int pcOffset
	) {
		byte addedSize;
		if (useValueOf) {
			addedSize = 1;
			MethodCallWriter valueOfMethod = cpManager.getOrCreateMethodRef("java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, valueOfMethod));
		} else {
			addedSize = 4;
			ClassInfoWriter wrapperClass = cpManager.getOrCreateClassInfo("java/lang/Double", cpEntries);
			instructions.add(new NewNode(wrapperClass));
			instructions.add(InsnNode.of(DUP_X2));
			instructions.add(InsnNode.of(DUP_X2));
			instructions.add(InsnNode.of(POP));
			MethodCallWriter constructorMethod = cpManager.getOrCreateMethodRef("java/lang/Double", "<init>", "(D)V", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, constructorMethod));
		}

		return addedSize;
	}

	public static int loadAndBoxChar(
		int varIndex,
		List<AbstractInsnNode> instructions,
		boolean useValueOf,
		LocalVariableTable localVarTable,
		List<TypeWriter> cpEntries,
		ConstantPoolManager cpManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		int pcOffset
	) {
		byte addedSize;
		if (useValueOf) {
			instructions.add(AbstractInsnNode.loadInt(varIndex, localVarTable, pcOffset));
			MethodCallWriter valueOfMethod = cpManager.getOrCreateMethodRef(
				"java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", cpEntries, workspace, classProvider
			);
			instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, valueOfMethod));
			addedSize = 2;
		} else {
			ClassInfoWriter wrapperClass = cpManager.getOrCreateClassInfo("java/lang/Character", cpEntries);
			instructions.add(new NewNode(wrapperClass));
			instructions.add(InsnNode.of(DUP));
			instructions.add(AbstractInsnNode.loadInt(varIndex, localVarTable, pcOffset));
			MethodCallWriter constructorMethod = cpManager.getOrCreateMethodRef("java/lang/Character", "<init>", "(C)V", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, constructorMethod));
			addedSize = 4;
		}

		return addedSize;
	}

	public static int boxFloat(
		List<AbstractInsnNode> instructions,
		boolean useValueOf,
		LocalVariableTable localVarTable,
		List<TypeWriter> cpEntries,
		ConstantPoolManager cpManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		int pcOffset
	) {
		byte addedSize;
		if (useValueOf) {
			addedSize = 1;
			MethodCallWriter valueOfMethod = cpManager.getOrCreateMethodRef("java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, valueOfMethod));
		} else {
			addedSize = 4;
			ClassInfoWriter wrapperClass = cpManager.getOrCreateClassInfo("java/lang/Boolean", cpEntries);
			instructions.add(new NewNode(wrapperClass));
			instructions.add(InsnNode.of(DUP_X1));
			instructions.add(InsnNode.of(SWAP));
			MethodCallWriter constructorMethod = cpManager.getOrCreateMethodRef("java/lang/Float", "<init>", "(F)V", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, constructorMethod));
		}

		return addedSize;
	}

	public static int loadAndBoxDouble(
		int varIndex,
		List<AbstractInsnNode> instructions,
		boolean useValueOf,
		LocalVariableTable localVarTable,
		List<TypeWriter> cpEntries,
		ConstantPoolManager cpManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		int pcOffset
	) {
		byte addedSize;
		if (useValueOf) {
			addedSize = 2;
			instructions.add(AbstractInsnNode.loadDouble(varIndex, localVarTable, pcOffset));
			MethodCallWriter valueOfMethod = cpManager.getOrCreateMethodRef("java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, valueOfMethod));
		} else {
			ClassInfoWriter wrapperClass = cpManager.getOrCreateClassInfo("java/lang/Double", cpEntries);
			instructions.add(new NewNode(wrapperClass));
			instructions.add(InsnNode.of(DUP));
			instructions.add(AbstractInsnNode.loadDouble(varIndex, localVarTable, pcOffset));
			MethodCallWriter constructorMethod = cpManager.getOrCreateMethodRef("java/lang/Double", "<init>", "(D)V", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, constructorMethod));
			addedSize = 4;
		}

		return addedSize;
	}

	public static int boxBoolean(
		List<AbstractInsnNode> instructions,
		boolean useValueOf,
		LocalVariableTable localVarTable,
		List<TypeWriter> cpEntries,
		ConstantPoolManager cpManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		int pcOffset
	) {
		byte addedSize;
		if (useValueOf) {
			addedSize = 1;
			MethodCallWriter valueOfMethod = cpManager.getOrCreateMethodRef(
				"java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", cpEntries, workspace, classProvider
			);
			instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, valueOfMethod));
		} else {
			addedSize = 4;
			ClassInfoWriter wrapperClass = cpManager.getOrCreateClassInfo("java/lang/Boolean", cpEntries);
			instructions.add(new NewNode(wrapperClass));
			instructions.add(InsnNode.of(DUP_X1));
			instructions.add(InsnNode.of(SWAP));
			MethodCallWriter constructorMethod = cpManager.getOrCreateMethodRef("java/lang/Boolean", "<init>", "(Z)V", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, constructorMethod));
		}

		return addedSize;
	}

	public static int loadAndBoxFloat(
		int varIndex,
		List<AbstractInsnNode> instructions,
		boolean useValueOf,
		LocalVariableTable localVarTable,
		List<TypeWriter> cpEntries,
		ConstantPoolManager cpManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		int pcOffset
	) {
		byte addedSize;
		if (useValueOf) {
			addedSize = 2;
			instructions.add(AbstractInsnNode.loadFloat(varIndex, localVarTable, pcOffset));
			MethodCallWriter valueOfMethod = cpManager.getOrCreateMethodRef("java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, valueOfMethod));
		} else {
			addedSize = 4;
			ClassInfoWriter wrapperClass = cpManager.getOrCreateClassInfo("java/lang/Float", cpEntries);
			instructions.add(new NewNode(wrapperClass));
			instructions.add(InsnNode.of(DUP));
			instructions.add(AbstractInsnNode.loadFloat(varIndex, localVarTable, pcOffset));
			MethodCallWriter constructorMethod = cpManager.getOrCreateMethodRef("java/lang/Float", "<init>", "(F)V", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, constructorMethod));
		}

		return addedSize;
	}

	public static int loadAndBoxInt(
		int varIndex,
		List<AbstractInsnNode> instructions,
		boolean useValueOf,
		LocalVariableTable localVarTable,
		List<TypeWriter> cpEntries,
		ConstantPoolManager cpManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		int pcOffset
	) {
		byte addedSize;
		if (useValueOf) {
			addedSize = 2;
			instructions.add(AbstractInsnNode.loadInt(varIndex, localVarTable, pcOffset));
			MethodCallWriter valueOfMethod = cpManager.getOrCreateMethodRef(
				"java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", cpEntries, workspace, classProvider
			);
			instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, valueOfMethod));
		} else {
			addedSize = 4;
			ClassInfoWriter wrapperClass = cpManager.getOrCreateClassInfo("java/lang/Integer", cpEntries);
			instructions.add(new NewNode(wrapperClass));
			instructions.add(InsnNode.of(DUP));
			instructions.add(AbstractInsnNode.loadInt(varIndex, localVarTable, pcOffset));
			MethodCallWriter constructorMethod = cpManager.getOrCreateMethodRef("java/lang/Integer", "<init>", "(I)V", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, constructorMethod));
		}

		return addedSize;
	}

	public static int loadAndBoxLong(
		int varIndex,
		List<AbstractInsnNode> instructions,
		boolean useValueOf,
		LocalVariableTable localVarTable,
		List<TypeWriter> cpEntries,
		ConstantPoolManager cpManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		int pcOffset
	) {
		byte addedSize;
		if (useValueOf) {
			addedSize = 2;
			instructions.add(AbstractInsnNode.loadLong(varIndex, localVarTable, pcOffset));
			MethodCallWriter valueOfMethod = cpManager.getOrCreateMethodRef("java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, valueOfMethod));
		} else {
			ClassInfoWriter wrapperClass = cpManager.getOrCreateClassInfo("java/lang/Long", cpEntries);
			instructions.add(new NewNode(wrapperClass));
			instructions.add(InsnNode.of(DUP));
			instructions.add(AbstractInsnNode.loadLong(varIndex, localVarTable, pcOffset));
			MethodCallWriter constructorMethod = cpManager.getOrCreateMethodRef("java/lang/Long", "<init>", "(J)V", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, constructorMethod));
			addedSize = 4;
		}

		return addedSize;
	}

	public static int boxInt(
		List<AbstractInsnNode> instructions,
		boolean useValueOf,
		LocalVariableTable localVarTable,
		List<TypeWriter> cpEntries,
		ConstantPoolManager cpManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		int pcOffset
	) {
		byte addedSize;
		if (useValueOf) {
			addedSize = 1;
			MethodCallWriter valueOfMethod = cpManager.getOrCreateMethodRef(
				"java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", cpEntries, workspace, classProvider
			);
			instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, valueOfMethod));
		} else {
			addedSize = 4;
			ClassInfoWriter wrapperClass = cpManager.getOrCreateClassInfo("java/lang/Integer", cpEntries);
			instructions.add(new NewNode(wrapperClass));
			instructions.add(InsnNode.of(DUP_X1));
			instructions.add(InsnNode.of(SWAP));
			MethodCallWriter constructorMethod = cpManager.getOrCreateMethodRef("java/lang/Integer", "<init>", "(I)V", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, constructorMethod));
		}

		return addedSize;
	}

	public static int loadAndBoxShort(
		int varIndex,
		List<AbstractInsnNode> instructions,
		boolean useValueOf,
		LocalVariableTable localVarTable,
		List<TypeWriter> cpEntries,
		ConstantPoolManager cpManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		int pcOffset
	) {
		byte addedSize;
		if (useValueOf) {
			instructions.add(AbstractInsnNode.loadInt(varIndex, localVarTable, pcOffset));
			MethodCallWriter valueOfMethod = cpManager.getOrCreateMethodRef("java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, valueOfMethod));
			addedSize = 2;
		} else {
			ClassInfoWriter wrapperClass = cpManager.getOrCreateClassInfo("java/lang/Short", cpEntries);
			instructions.add(new NewNode(wrapperClass));
			instructions.add(InsnNode.of(DUP));
			instructions.add(AbstractInsnNode.loadInt(varIndex, localVarTable, pcOffset));
			MethodCallWriter constructorMethod = cpManager.getOrCreateMethodRef("java/lang/Short", "<init>", "(S)V", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, constructorMethod));
			addedSize = 4;
		}

		return addedSize;
	}
}
