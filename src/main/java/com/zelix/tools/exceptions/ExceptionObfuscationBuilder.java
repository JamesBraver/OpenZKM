package com.zelix.tools.exceptions;

import com.zelix.framework.attributes.code.CodeAttribute;
import com.zelix.framework.attributes.code.ExceptionEntryNode;
import com.zelix.framework.attributes.code.TryCatchBlockInfo;
import com.zelix.framework.code.InstructionBlock;
import com.zelix.framework.code.InstructionListNode;
import com.zelix.framework.code.MethodMapping;
import com.zelix.framework.hierarchy.ClassHierarchy;
import com.zelix.framework.hierarchy.Inheritance;
import com.zelix.framework.hierarchy.TypeResolver;
import com.zelix.framework.instructions.AbstractInsnNode;
import com.zelix.framework.instructions.InsnNode;
import com.zelix.framework.instructions.WideConstantPoolInsnNode;
import com.zelix.framework.variable.LocalVariableTable;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.base.AbstractMethodCallWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.utils.ClassMetrics;
import com.zelix.utils.collections.HierarchicalMultiMap;
import com.zelix.utils.collections.KeyValuePair;
import com.zelix.utils.dataset.MemberContext;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.PhantomClassManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;

import static com.zelix.framework.instructions.OpcodeConstants.*;

public class ExceptionObfuscationBuilder {
	public static void obfuscate(
		ClassNode classNode,
		Set<MethodNode> generatedMethods,
		HierarchicalMultiMap<MemberContext, InstructionListNode, WideConstantPoolInsnNode> memberObfuscationRefs,
		Map<MethodNode, ?> decryptionLocalVarByMethod,
		MethodMapping dummyHandlersByMethod,
		List<TypeWriter> cpEntries,
		TypeResolver typeResolver,
		ClassHierarchy classHierarchy,
		ClassMetrics classMetrics,
		Inheritance inheritance,
		AbstractWorkspace workspace,
		ScriptContext scriptContext
	) {
		PhantomClassManager phantomClassManager = scriptContext == null ? null : scriptContext.getPhantomClassManager();
		ArrayList<WideConstantPoolInsnNode> helperCallInstructions = new ArrayList<>();
		WideConstantPoolInsnNode helperCallInstruction = null;
		MemberContext exceptionHelperContext = null;
		if (classMetrics.isVeryLargeApplication() && !Options.dontCreateExceptionHelper) {
			String exceptionTypeDescriptor = null;

			for (Object entryObj : dummyHandlersByMethod.entrySet()) {
				Entry<?, ?> entry = (Entry<?, ?>)entryObj;
				KeyValuePair<?, ?> pair = (KeyValuePair<?, ?>)entry.getValue();

				for (Object excObj : (List)pair.getValue()) {
					String exceptionName = ((ExceptionEntryNode)excObj).getExceptionName();
					if (exceptionTypeDescriptor == null) {
						exceptionTypeDescriptor = 'L' + exceptionName + ';';
					} else if (phantomClassManager != null && (phantomClassManager.isInIgnoredClasses(exceptionName) || phantomClassManager.isInIgnoredClasses(exceptionTypeDescriptor.substring(1, exceptionTypeDescriptor.length() - 1)))) {
						exceptionTypeDescriptor = "Ljava/lang/Throwable;";
					} else {
						exceptionTypeDescriptor = typeResolver.getCommonSupertype(exceptionTypeDescriptor, 'L' + exceptionName + ';', "Exception Obfuscation");
					}
				}
			}

			String helperMethodDescriptor = '(' + exceptionTypeDescriptor + ')' + exceptionTypeDescriptor;
			LocalVariableTable localVariableTable = new LocalVariableTable(true, helperMethodDescriptor, 5);
			ArrayList<AbstractInsnNode> helperMethodInstructions = new ArrayList<>();
			helperMethodInstructions.add(AbstractInsnNode.loadObject(0, localVariableTable, 8));
			helperMethodInstructions.add(InsnNode.of(ARETURN));
			TryCatchBlockInfo[] tryCatchBlocks = new TryCatchBlockInfo[0];
			MethodNode exceptionHelperMethod = classNode.createUniqueMethod(helperMethodDescriptor, helperMethodInstructions, 1, 1, 1, localVariableTable, tryCatchBlocks, "Exception Obfuscation", cpEntries, inheritance, workspace, 8);
			generatedMethods.add(exceptionHelperMethod);
			AbstractMethodCallWriter methodRefWriter = classNode.getOrCreateAnyMethodRef(exceptionHelperMethod, cpEntries);
			helperCallInstruction = new WideConstantPoolInsnNode(INVOKESTATIC, methodRefWriter);
			helperCallInstructions.add(helperCallInstruction);
			exceptionHelperContext = new MemberContext(exceptionHelperMethod, classNode);
		}

		for (Object handlerEntryObj : dummyHandlersByMethod.entrySet()) {
			Entry<?, ?> handlerEntry = (Entry<?, ?>)handlerEntryObj;
			InstructionListNode instructionListNode = (InstructionListNode)handlerEntry.getKey();
			KeyValuePair<?, ?> handlerPair = (KeyValuePair<?, ?>)handlerEntry.getValue();
			List<?> rawBlocks = (List<?>)handlerPair.getKey();
			ArrayList<InstructionBlock> instructionBlocks = new ArrayList<>(rawBlocks.size());

			for (Object blockObj : rawBlocks) {
				instructionBlocks.add((InstructionBlock)blockObj);
			}

			if (helperCallInstructions.size() > 0) {
				for (InstructionBlock instructionBlock : instructionBlocks) {
					instructionBlock.insertBeforeOpcode(ATHROW, helperCallInstructions);
				}
			}

			List<?> rawExceptionEntries = (List<?>)handlerPair.getValue();
			ArrayList<ExceptionEntryNode> exceptionEntries = new ArrayList<>(rawExceptionEntries.size());

			for (Object exceptionEntryObj : rawExceptionEntries) {
				exceptionEntries.add((ExceptionEntryNode)exceptionEntryObj);
			}

			instructionListNode.applyPatchesWithoutStackCheck(instructionBlocks, "Exception Obfuscation");
			((CodeAttribute)instructionListNode.getParent()).reorderExceptionHandlers(true, exceptionEntries, typeResolver, classHierarchy, scriptContext);
			instructionListNode.setModified(true);
			if (memberObfuscationRefs != null && exceptionHelperContext != null && helperCallInstruction != null && decryptionLocalVarByMethod != null && decryptionLocalVarByMethod.containsKey(instructionListNode.getMethodNode())) {
				memberObfuscationRefs.put(exceptionHelperContext, instructionListNode, helperCallInstruction);
			}
		}
	}
}
