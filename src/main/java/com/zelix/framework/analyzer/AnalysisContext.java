package com.zelix.framework.analyzer;

import com.zelix.framework.analyzer.dataset.MethodAnalysisResult;
import com.zelix.framework.analyzer.reference.Reference;
import com.zelix.framework.code.InstructionListNode;
import com.zelix.framework.hierarchy.TypeResolver;
import com.zelix.framework.signature.FieldID;
import com.zelix.framework.signature.MethodID;
import com.zelix.framework.writer.base.AbstractMethodCallWriter;
import com.zelix.framework.writer.reflect.ReflectionMethodPattern;
import com.zelix.tools.excludes.ExcludeElement;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.CountingSet;
import com.zelix.utils.collections.HierarchicalMultiMap;
import com.zelix.utils.dataset.AtomicBool;
import com.zelix.workspace.AbstractWorkspace;
import java.util.HashSet;
import java.util.Map;

public class AnalysisContext {
	public final MethodAnalyzer methodAnalyzer;
	public final CountingSet<ExcludeElement> traceCountsByElement;
	private AtomicBool privateCallChain;
	public HashSet<Reference> currentReferences;
	public final boolean isArrayParameter;
	public final AnalysisType analysisType;
	public final HierarchicalMultiMap<FieldID, InstructionListNode, Integer> fieldReadsByField;
	public final HierarchicalMultiMap<FieldID, InstructionListNode, Integer> fieldWritesByField;
	public final HierarchicalMultiMap<MethodID, InstructionListNode, Integer> methodCallsByMethod;
	public final TypeResolver typeResolver;
	public final AbstractWorkspace workspace;
	public final Map<AbstractMethodCallWriter, ReflectionMethodPattern> reflectionPatterns;
	public final HashSet<MethodID> enteredReferencedMethods;
	public final boolean isStaticMethod;
	public final Map<MethodID, MethodAnalysisResult> methodAnalysisResults;
	public final Map<FieldID, HashSet<Reference>> fieldReferencesCache;
	public final Map<ExcludeElement, HashSet<Reference>> stackSourceCache;
	public final Map<ExcludeElement, HashSet<Reference>> localVarSourceCache;
	public int nestingLevel;
	public boolean propertyNameTrace;
	public Map<InstructionListNode, MethodAnalyzer> methodAnalyzerCache;

	public AnalysisContext(MethodAnalyzer methodAnalyzer, AnalysisContext parentContext, AnalysisType analysisType, int nestingLevel, boolean propertyNameTrace) {
		this.methodAnalyzer = methodAnalyzer;
		this.traceCountsByElement = new CountingSet<>();
		this.privateCallChain = new AtomicBool(true);
		this.currentReferences = parentContext.currentReferences;
		this.isArrayParameter = parentContext.isArrayParameter;
		this.analysisType = analysisType;
		this.fieldReadsByField = parentContext.fieldReadsByField;
		this.fieldWritesByField = parentContext.fieldWritesByField;
		this.methodCallsByMethod = parentContext.methodCallsByMethod;
		this.typeResolver = parentContext.typeResolver;
		this.workspace = parentContext.workspace;
		this.reflectionPatterns = parentContext.reflectionPatterns;
		this.enteredReferencedMethods = GlobalUtils.clone(parentContext.enteredReferencedMethods);
		this.isStaticMethod = parentContext.isStaticMethod;
		this.methodAnalysisResults = parentContext.methodAnalysisResults;
		this.fieldReferencesCache = parentContext.fieldReferencesCache;
		this.stackSourceCache = parentContext.stackSourceCache;
		this.localVarSourceCache = parentContext.localVarSourceCache;
		this.nestingLevel = nestingLevel;
		this.propertyNameTrace = propertyNameTrace;
		this.methodAnalyzerCache = parentContext.methodAnalyzerCache;
		this.privateCallChain = parentContext.privateCallChain;
	}

	public AnalysisContext(MethodAnalyzer methodAnalyzer, AnalysisContext parentContext, boolean isStaticMethod) {
		this.methodAnalyzer = methodAnalyzer;
		this.traceCountsByElement = new CountingSet<>();
		this.privateCallChain = new AtomicBool(true);
		this.currentReferences = parentContext.currentReferences;
		this.isArrayParameter = parentContext.isArrayParameter;
		this.analysisType = parentContext.analysisType;
		this.fieldReadsByField = parentContext.fieldReadsByField;
		this.fieldWritesByField = parentContext.fieldWritesByField;
		this.methodCallsByMethod = parentContext.methodCallsByMethod;
		this.typeResolver = parentContext.typeResolver;
		this.workspace = parentContext.workspace;
		this.reflectionPatterns = parentContext.reflectionPatterns;
		this.enteredReferencedMethods = GlobalUtils.clone(parentContext.enteredReferencedMethods);
		this.isStaticMethod = isStaticMethod;
		this.methodAnalysisResults = parentContext.methodAnalysisResults;
		this.fieldReferencesCache = parentContext.fieldReferencesCache;
		this.stackSourceCache = parentContext.stackSourceCache;
		this.localVarSourceCache = parentContext.localVarSourceCache;
		this.nestingLevel = parentContext.nestingLevel + 1;
		this.methodAnalyzerCache = parentContext.methodAnalyzerCache;
		this.privateCallChain = parentContext.privateCallChain;
	}

	public AnalysisContext(
		MethodAnalyzer methodAnalyzer,
		HashSet<Reference> currentReferences,
		boolean isArrayParameter,
		AnalysisType analysisType,
		HierarchicalMultiMap<FieldID, InstructionListNode, Integer> fieldReadsByField,
		HierarchicalMultiMap<FieldID, InstructionListNode, Integer> fieldWritesByField,
		HierarchicalMultiMap<MethodID, InstructionListNode, Integer> methodCallsByMethod,
		TypeResolver typeResolver,
		AbstractWorkspace workspace,
		Map<AbstractMethodCallWriter, ReflectionMethodPattern> reflectionPatterns,
		boolean isStaticMethod,
		Map<MethodID, MethodAnalysisResult> methodAnalysisResults,
		Map<FieldID, HashSet<Reference>> fieldReferencesCache,
		Map<ExcludeElement, HashSet<Reference>> stackSourceCache,
		Map<ExcludeElement, HashSet<Reference>> localVarSourceCache,
		int nestingLevel,
		Map<InstructionListNode, MethodAnalyzer> methodAnalyzerCache
	) {
		this.methodAnalyzer = methodAnalyzer;
		this.traceCountsByElement = new CountingSet<>();
		this.privateCallChain = new AtomicBool(true);
		this.currentReferences = currentReferences;
		this.isArrayParameter = isArrayParameter;
		this.analysisType = analysisType;
		this.fieldReadsByField = fieldReadsByField;
		this.fieldWritesByField = fieldWritesByField;
		this.methodCallsByMethod = methodCallsByMethod;
		this.typeResolver = typeResolver;
		this.workspace = workspace;
		this.reflectionPatterns = reflectionPatterns;
		this.enteredReferencedMethods = GlobalUtils.newHashSet();
		this.isStaticMethod = isStaticMethod;
		this.methodAnalysisResults = methodAnalysisResults;
		this.fieldReferencesCache = fieldReferencesCache;
		this.stackSourceCache = stackSourceCache;
		this.localVarSourceCache = localVarSourceCache;
		this.nestingLevel = nestingLevel;
		this.methodAnalyzerCache = methodAnalyzerCache;
	}

	public AnalysisContext(MethodAnalyzer methodAnalyzer, AnalysisContext parentContext, AnalysisType analysisType) {
		this(methodAnalyzer, parentContext, analysisType, parentContext.nestingLevel + 1);
	}

	public AnalysisContext(
		MethodAnalyzer methodAnalyzer,
		HashSet<Reference> currentReferences,
		boolean isArrayParameter,
		AnalysisType analysisType,
		HierarchicalMultiMap<FieldID, InstructionListNode, Integer> fieldReadsByField,
		HierarchicalMultiMap<FieldID, InstructionListNode, Integer> fieldWritesByField,
		HierarchicalMultiMap<MethodID, InstructionListNode, Integer> methodCallsByMethod,
		TypeResolver typeResolver,
		AbstractWorkspace workspace,
		Map<AbstractMethodCallWriter, ReflectionMethodPattern> reflectionPatterns,
		boolean isStaticMethod,
		Map<MethodID, MethodAnalysisResult> methodAnalysisResults,
		Map<FieldID, HashSet<Reference>> fieldReferencesCache,
		Map<ExcludeElement, HashSet<Reference>> stackSourceCache,
		Map<ExcludeElement, HashSet<Reference>> localVarSourceCache,
		Map<InstructionListNode, MethodAnalyzer> methodAnalyzerCache
	) {
		this(methodAnalyzer, currentReferences, isArrayParameter, analysisType, fieldReadsByField, fieldWritesByField, methodCallsByMethod, typeResolver, workspace, reflectionPatterns, isStaticMethod, methodAnalysisResults, fieldReferencesCache, stackSourceCache, localVarSourceCache, 0, methodAnalyzerCache);
	}

	public AnalysisContext(MethodAnalyzer methodAnalyzer, AnalysisContext parentContext, AnalysisType analysisType, int nestingLevel) {
		this(methodAnalyzer, parentContext, analysisType, nestingLevel, false);
	}

	public boolean isPrivateCallChain() {
		return this.privateCallChain.get();
	}

	public boolean enterReferencedMethod(MethodID methodId) {
		return this.enteredReferencedMethods.add(methodId);
	}

	public int getNestingLevel() {
		return this.nestingLevel;
	}

	public boolean leaveReferencedMethod(MethodID methodId) {
		return this.enteredReferencedMethods.remove(methodId);
	}

	public void setPrivateCallChain(boolean privateCallChain) {
		this.privateCallChain.set(privateCallChain);
	}

	public HashSet<Reference> swapCurrentReferences(HashSet<Reference> newReferences) {
		HashSet<Reference> previousReferences = this.currentReferences;
		this.currentReferences = newReferences;
		return previousReferences;
	}
}
