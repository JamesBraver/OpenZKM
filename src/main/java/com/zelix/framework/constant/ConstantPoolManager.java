package com.zelix.framework.constant;

import com.zelix.enums.TypeTags;
import com.zelix.exceptions.ClassParseFailedException;
import com.zelix.exceptions.ClasspathException;
import com.zelix.exceptions.FileException;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.TypeNode;
import com.zelix.framework.attributes.BootstrapMethodsAttribute;
import com.zelix.framework.code.InstructionListNode;
import com.zelix.framework.hierarchy.ClassHierarchy;
import com.zelix.framework.listener.*;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.LibraryClassNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.*;
import com.zelix.framework.writer.base.*;
import com.zelix.framework.writer.reflect.ReflectionMethodPattern;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.constant.CopyrightConstants;
import com.zelix.logging.AbstractLogger;
import com.zelix.repository.ClassProvider;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.PathRemapper;
import com.zelix.utils.StringUtils;
import com.zelix.utils.collections.CountingSet;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.collections.readOnly.ReadOnlyMap;
import com.zelix.utils.dataset.AtomicBool;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.ClassHierarchyNode;
import com.zelix.workspace.PhantomClassManager;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.Map.Entry;

public class ConstantPoolManager extends AbstractConstantPoolWriter {
	public static final char[] WATERMARK_CHARS = new char[]{'Z', 'K', 'M'};
	private static final Map<String, String> PRIMITIVE_VALUE_METHODS = GlobalUtils.newHashMap(17);
	private static final Map<String, String> PRIMITIVE_VALUE_DESCRIPTORS = GlobalUtils.newHashMap(17);
	public static final String WATERMARK_STRING;
	private ArrayList<FloatNode> floatNodes = new ArrayList<>();
	private ArrayList<DoubleNode> doubleNodes = new ArrayList<>();
	private ArrayList<ResolvedMethodHandleWriter> methodHandleNodes = new ArrayList<>();
	private ArrayList<MethodDescriptorWriter> methodTypeNodes = new ArrayList<>();
	private List<ModuleNameWriter> moduleNodes = new ArrayList<>();
	private List<PackageNameWriter> packageNodes = new ArrayList<>();
	private ArrayList<TypeDataWriter> typeDataWriters = new ArrayList<>();
	private int initialPoolSize;

	public ConstantPoolManager(
		ByteDataBuffer dataBuffer,
		PrintWriter logWriter,
		ClassNode classNode,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<NameAndTypeWriter, AbstractNamedWriter> nameAndTypeReferences,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		MultiMap<MemberTypeWriter, TypeNode> memberReferences,
		MultiMap<InvokeDynamicCallWriter, IDynamicCallUpdatable> dynamicCallReferences
	) throws IOException {
		super(classNode);
		int constantPoolCount = dataBuffer.readUnsignedShort();
		this.initialPoolSize = constantPoolCount;
		this.constantPoolEntries = new TypeWriter[constantPoolCount];
		ArrayList<TypeWriter> pendingClassConstants = new ArrayList<>();
		ArrayList<TypeWriter> pendingNameAndTypeConstants = new ArrayList<>();
		ArrayList<TypeWriter> pendingMemberAndComplexConstants = new ArrayList<>();
		this.utf8StringConstants = new ArrayList<>(Math.max(5, (int)(constantPoolCount * 1.5)));
		this.nameAndTypeConstants = new ArrayList<>(Math.max(5, (int)(constantPoolCount * 0.2)));
		this.classInfoConstants = new ArrayList<>(Math.max(5, (int)(constantPoolCount * 0.09)));
		super.classInfoByNameCache = GlobalUtils.newHashMap(Math.max(5, (int)(constantPoolCount * 0.09)));
		this.stringConstants = new ArrayList<>(Math.max(5, (int)(constantPoolCount * 0.05)));
		super.integerConstants = new ArrayList<>(Math.max(5, (int)(constantPoolCount * 0.01)));
		super.longConstants = new ArrayList<>(Math.max(5, (int)(constantPoolCount * 0.01)));
		this.methodRefConstants = new ArrayList<>(Math.max(5, (int)(constantPoolCount * 0.15)));
		super.interfaceMethodRefConstants = new ArrayList<>(Math.max(5, (int)(constantPoolCount * 0.15)));
		this.fieldRefConstants = new ArrayList<>(Math.max(5, (int)(constantPoolCount * 0.15)));
		this.constantPoolEntries[0] = NullTypeWriter.getNullSlotPlaceholder(this, 1);
		int entryIndex = 1;

		while (entryIndex < constantPoolCount) {
			TypeWriter entry = TypeWriter.readConstantPoolManagerEntry(entryIndex, dataBuffer, this);
			this.constantPoolEntries[entryIndex++] = entry;
			if (entry.getSlotSize() == 2) {
				this.constantPoolEntries[entryIndex++] = NullTypeWriter.getNullSlotPlaceholder(this, 1);
			}

			TypeTags typeTag = entry.getTypeTag();
			switch (typeTag) {
				case CLASS:
					pendingClassConstants.add(entry);
					break;
				case NAME_AND_TYPE:
					pendingNameAndTypeConstants.add(entry);
					break;
				case METHODREF:
					pendingMemberAndComplexConstants.add(entry);
					break;
				case INTERFACE_METHODREF:
					pendingMemberAndComplexConstants.add(entry);
					break;
				case FIELDREF:
					pendingMemberAndComplexConstants.add(entry);
					break;
				case UTF8:
					this.utf8StringConstants.add((UTFStringWriter)entry);
					break;
				case STRING:
					pendingMemberAndComplexConstants.add(entry);
					break;
				case INTEGER:
					super.integerConstants.add((IntegerNode)entry);
					break;
				case LONG:
					super.longConstants.add((LongNode)entry);
					break;
				case FLOAT:
					this.floatNodes.add((FloatNode)entry);
					break;
				case DOUBLE:
					this.doubleNodes.add((DoubleNode)entry);
					break;
				case METHOD_HANDLE:
					pendingMemberAndComplexConstants.add(entry);
					break;
				case METHOD_TYPE:
					pendingMemberAndComplexConstants.add(entry);
					break;
				case CONSTANT_DYNAMIC:
					pendingMemberAndComplexConstants.add(entry);
					break;
				case INVOKE_DYNAMIC:
					pendingMemberAndComplexConstants.add(entry);
					break;
				case MODULE:
					pendingMemberAndComplexConstants.add(entry);
					break;
				case PACKAGE:
					pendingMemberAndComplexConstants.add(entry);
			}
		}

		this.resolvePendingConstantRefs(pendingClassConstants, utf8References, nameAndTypeReferences, classReferences, memberReferences, logWriter);
		this.resolvePendingConstantRefs(pendingNameAndTypeConstants, utf8References, nameAndTypeReferences, classReferences, memberReferences, logWriter);
		this.resolvePendingConstantRefs(pendingMemberAndComplexConstants, utf8References, nameAndTypeReferences, classReferences, memberReferences, logWriter);
	}

	public static boolean isJavaIdentifier(String identifier) {
		if (identifier != null && identifier.length() != 0) {
			if (!Character.isJavaIdentifierStart(identifier.charAt(0))) {
				return false;
			} else {
				int length = identifier.length();

				for (int charIndex = 1; charIndex < length; charIndex++) {
					char character = identifier.charAt(charIndex);
					if (character != '/' && !Character.isJavaIdentifierPart(character)) {
						return false;
					}
				}

				return true;
			}
		} else {
			return false;
		}
	}

	@Override
	public void remapTargetClassName(String newClassName) {
		String ownerInternalName = this.getOwnerInternalName();
		HashMap<String, ClassInfoWriter> classInfoByName = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(super.classInfoByNameCache.size()));

		for (ClassInfoWriter classInfo : this.classInfoConstants) {
			if (classInfo.getInternalName().equals(ownerInternalName)) {
				classInfo.setName(newClassName);
			}

			classInfoByName.put(classInfo.getInternalName(), classInfo);
		}

		super.classInfoByNameCache = classInfoByName;
	}

	public ResolvedMethodHandleWriter getOrCreateMethodHandle(MethodHandleKind methodHandleType, MemberTypeWriter memberWriter, List<TypeWriter> cpEntries) {
		for (ResolvedMethodHandleWriter methodHandle : this.methodHandleNodes) {
			if (methodHandle.getKind() == methodHandleType && methodHandle.matches(methodHandleType, memberWriter)) {
				return methodHandle;
			}
		}

		for (TypeWriter entry : cpEntries) {
			if (entry instanceof ResolvedMethodHandleWriter) {
				ResolvedMethodHandleWriter candidate = (ResolvedMethodHandleWriter)entry;
				if (candidate.getKind() == methodHandleType && candidate.matches(methodHandleType, memberWriter)) {
					return candidate;
				}
			}
		}

		ResolvedMethodHandleWriter newMethodHandle = new ResolvedMethodHandleWriter(0, this, methodHandleType, memberWriter);
		cpEntries.add(newMethodHandle);
		return newMethodHandle;
	}

	public int updateUtf8StringValue(String oldValue, String newValue) {
		int updateCount = 0;

		for (StringNode stringNode : this.stringConstants) {
			if (stringNode.getDisplayValue().equals(oldValue)) {
				stringNode.getUtf8StringWriter().setValue(newValue);
				updateCount++;
			}
		}

		return updateCount;
	}

	public void remapDescriptorsInStrings(HashMap<String, String> classNameMappings) {
		for (int utf8Index = 0; utf8Index < super.unreferencedUtf8Strings.size(); utf8Index++) {
			UTFStringWriter unreferencedUtf8 = super.unreferencedUtf8Strings.get(utf8Index);
			String utf8Value = unreferencedUtf8.getValue();
			String remappedUtf8Value = TypeWriter.remapDescriptor(utf8Value, classNameMappings);
			if (!remappedUtf8Value.equals(utf8Value)) {
				unreferencedUtf8.setValue(remappedUtf8Value);
			}
		}

		Iterator<StringNode> stringIterator = this.stringConstants.iterator();

		while (stringIterator.hasNext()) {
			UTFStringWriter utf8String = stringIterator.next().getUtf8StringWriter();
			String stringValue = utf8String.getValue();
			String remappedStringValue = TypeWriter.remapDescriptor(stringValue, classNameMappings);
			if (!remappedStringValue.equals(stringValue)) {
				utf8String.setValue(remappedStringValue);
			}
		}
	}

	public InterfaceMethodCallWriter getOrCreateInterfaceMethod(
		String ownerClassName, String methodName, String descriptor, List<TypeWriter> cpEntries, AbstractWorkspace workspace, ClassProvider classProvider
	) {
		InterfaceMethodCallWriter interfaceMethodRef = this.findInterfaceMethodRef(ownerClassName, methodName, descriptor);
		if (interfaceMethodRef != null) {
			return interfaceMethodRef;
		} else {
			interfaceMethodRef = (InterfaceMethodCallWriter)this.findMemberRefAmongNewEntries(ownerClassName, methodName, descriptor, cpEntries);
			if (interfaceMethodRef != null) {
				return interfaceMethodRef;
			} else {
				ClassInfoWriter classInfo = this.getOrCreateClassInfo(ownerClassName, cpEntries);
				NameAndTypeWriter nameAndType = this.createNameAndType(methodName, descriptor, cpEntries);
				InterfaceMethodCallWriter newInterfaceMethodRef = new InterfaceMethodCallWriter(0, this, classInfo, nameAndType, workspace, classProvider);
				cpEntries.add(newInterfaceMethodRef);
				return newInterfaceMethodRef;
			}
		}
	}

	public IntegerNode getOrCreateInteger(int value, List<TypeWriter> cpEntries, boolean reuseExisting, boolean needIntObf) {
		if (reuseExisting) {
			for (int integerIndex = 0; integerIndex < super.integerConstants.size(); integerIndex++) {
				IntegerNode integerNode = super.integerConstants.get(integerIndex);
				if (!integerNode.isNeedIntObf() && integerNode.isEqual(value)) {
					return integerNode;
				}
			}

			for (TypeWriter entry : cpEntries) {
				if (entry instanceof IntegerNode) {
					IntegerNode candidate = (IntegerNode)entry;
					if (!candidate.isNeedIntObf() && candidate.isEqual(value)) {
						return candidate;
					}
				}
			}
		}

		IntegerNode newIntegerNode;
		if (needIntObf) {
			newIntegerNode = new IntegerNode(0, this, value, true);
		} else {
			newIntegerNode = new IntegerNode(0, this, value, false);
		}

		cpEntries.add(newIntegerNode);
		return newIntegerNode;
	}

	public ConstantWriter[] getAllConstantWriters() {
		ConstantWriter[] writers = new ConstantWriter[super.integerConstants.size()
			+ super.longConstants.size()
			+ this.floatNodes.size()
			+ this.doubleNodes.size()
			+ this.stringConstants.size()];
		int writerIndex = 0;

		for (int stringIndex = 0; stringIndex < this.stringConstants.size(); stringIndex++) {
			writers[writerIndex++] = this.stringConstants.get(stringIndex);
		}

		for (int integerIndex = 0; integerIndex < super.integerConstants.size(); integerIndex++) {
			writers[writerIndex++] = super.integerConstants.get(integerIndex);
		}

		for (int longIndex = 0; longIndex < super.longConstants.size(); longIndex++) {
			writers[writerIndex++] = super.longConstants.get(longIndex);
		}

		for (int floatIndex = 0; floatIndex < this.floatNodes.size(); floatIndex++) {
			writers[writerIndex++] = this.floatNodes.get(floatIndex);
		}

		for (int doubleIndex = 0; doubleIndex < this.doubleNodes.size(); doubleIndex++) {
			writers[writerIndex++] = this.doubleNodes.get(doubleIndex);
		}

		return writers;
	}

	public void collectReferencedClasses(CountingSet<ClassNode> referencedClasses) {
		String ownerInternalName = this.ownerClassNode.getInternalName();
		int size = this.classInfoConstants.size();

		for (int i = 0; i < size; i++) {
			String className = this.classInfoConstants.get(i).getInternalName();
			if (className.startsWith("[")) {
				className = AbstractClassNode.getTypeName(className);
			}

			ClassNode classNode;
			if (className != null && !className.equals(ownerInternalName) && (classNode = ClassHierarchyNode.getWorkspaceClassNode(className)) != null) {
				referencedClasses.add(classNode);
			}
		}
	}

	public void resolveExternalMembers(Set<MethodNode> bootstrapTargets, AbstractWorkspace workspace, ClassProvider classProvider, PhantomClassManager phantomClassManager) {
		int fieldRefCount = this.fieldRefConstants.size();

		for (int fieldRefIndex = 0; fieldRefIndex < fieldRefCount; fieldRefIndex++) {
			this.fieldRefConstants.get(fieldRefIndex).resolveTargetMember(workspace, classProvider, phantomClassManager);
		}

		int methodRefCount = this.methodRefConstants.size();

		for (int methodRefIndex = 0; methodRefIndex < methodRefCount; methodRefIndex++) {
			this.methodRefConstants.get(methodRefIndex).resolveTargetMember(workspace, classProvider, phantomClassManager);
		}

		int interfaceMethodRefCount = super.interfaceMethodRefConstants.size();

		for (int interfaceMethodRefIndex = 0; interfaceMethodRefIndex < interfaceMethodRefCount; interfaceMethodRefIndex++) {
			super.interfaceMethodRefConstants.get(interfaceMethodRefIndex).resolveTargetMember(workspace, classProvider, phantomClassManager);
		}

		int dynamicCallCount = super.dynamicCallConstants.size();

		for (int dynamicCallIndex = 0; dynamicCallIndex < dynamicCallCount; dynamicCallIndex++) {
			super.dynamicCallConstants.get(dynamicCallIndex).resolveBootstrapTargets(bootstrapTargets, workspace, classProvider, phantomClassManager);
		}

		int dynamicValueCount = super.dynamicValueConstants.size();

		for (int dynamicValueIndex = 0; dynamicValueIndex < dynamicValueCount; dynamicValueIndex++) {
			super.dynamicValueConstants.get(dynamicValueIndex).resolveReferencedClass(workspace, classProvider, phantomClassManager);
		}
	}

	public int updateIntegerConstantValue(int oldValue, int newValue) {
		int updateCount = 0;

		for (IntegerNode integerNode : super.integerConstants) {
			if (integerNode.getValue() == oldValue) {
				integerNode.setValue(newValue);
				updateCount++;
			}
		}

		return updateCount;
	}

	public void remapStringConstantPool(HashMap<String, String> classNameMappings, HashSet<StringNode> processedStringNodes, Set<String> resourceFilePaths, HashMap<String, String> packageMappings) {
		ReadOnlyMap<String, String> readOnlyPackageMappings = new ReadOnlyMap<>(packageMappings);
		int stringCount = this.stringConstants.size();

		for (int stringIndex = 0; stringIndex < stringCount; stringIndex++) {
			StringNode stringNode = this.stringConstants.get(stringIndex);
			if (!processedStringNodes.contains(stringNode)) {
				UTFStringWriter utf8String = stringNode.getUtf8StringWriter();
				String stringValue = utf8String.getValue();
				int valueLength = stringValue.length();
				AtomicBool descriptorRemapped = new AtomicBool();
				String remappedValue = TypeWriter.tryRemapDescriptor(stringValue, classNameMappings, descriptorRemapped);
				if (!remappedValue.equals(stringValue)) {
					utf8String.setValue(remappedValue);
				} else if (!descriptorRemapped.get()) {
					String resourcePath;
					if (stringValue.startsWith("/") && valueLength > 0) {
						resourcePath = stringValue.substring(1);
					} else {
						resourcePath = stringValue;
					}

					if (resourceFilePaths.contains(resourcePath)) {
						String remappedDirectoryPath = PathRemapper.remapDirectoryOfPath(stringValue, readOnlyPackageMappings);
						if (!remappedDirectoryPath.equals(stringValue)) {
							utf8String.setValue(remappedDirectoryPath);
						}
					} else {
						int firstSlashIndex = stringValue.indexOf("/");
						int lastSlashIndex = stringValue.lastIndexOf("/");
						int dotIndex = stringValue.indexOf(".");
						if (firstSlashIndex > 0 && dotIndex > 0 && lastSlashIndex < dotIndex && dotIndex < valueLength - 1) {
							ArrayList<String> absolutePathMatches = new ArrayList<>();
							ArrayList<String> suffixPathMatches = new ArrayList<>();
							String absolutePackagePath = this.getOwnerPackageNameInternal() + (stringValue.charAt(stringValue.length() - 1) == '/' ? "" : '/') + stringValue;

							for (String resourceFilePath : resourceFilePaths) {
								if (resourceFilePath.endsWith(absolutePackagePath)) {
									absolutePathMatches.add(resourceFilePath);
								}

								if (resourceFilePath.endsWith(stringValue)) {
									suffixPathMatches.add(resourceFilePath);
								}
							}

							String matchedResourcePath;
							if (absolutePathMatches.size() == 1) {
								matchedResourcePath = absolutePathMatches.get(0);
							} else {
								if (suffixPathMatches.size() != 1) {
									continue;
								}

								matchedResourcePath = suffixPathMatches.get(0);
							}

							if (matchedResourcePath != null) {
								String remappedRelativePath = PathRemapper.computeMappedRelativePath(matchedResourcePath, stringValue, readOnlyPackageMappings);
								if (!remappedRelativePath.equals(stringValue)) {
									utf8String.setValue(remappedRelativePath);
								}
							}
						} else if (dotIndex == -1) {
							if (firstSlashIndex > 0 && lastSlashIndex < valueLength - 1 && StringUtils.countOccurrences(stringValue, '/') >= 2) {
								if (readOnlyPackageMappings.containsKey(stringValue)) {
									String mappedPath = readOnlyPackageMappings.get(stringValue);
									if (mappedPath != null && !stringValue.equals(mappedPath)) {
										utf8String.setValue(mappedPath);
									}
								} else if (!Options.dontRemapInternalPathNames) {
									String remappedPath = this.remapPathName(stringValue, readOnlyPackageMappings, "/");
									if (remappedPath != null && !stringValue.equals(remappedPath)) {
										utf8String.setValue(remappedPath);
									}
								}
							}
						} else if (firstSlashIndex == -1 && dotIndex > 0 && stringValue.lastIndexOf(".") < valueLength - 1 && StringUtils.countOccurrences(stringValue, '.') >= 2) {
							String slashSeparatedPath = stringValue.replace('.', '/');
							if (readOnlyPackageMappings.containsKey(slashSeparatedPath)) {
								String mappedSlashPath = readOnlyPackageMappings.get(slashSeparatedPath);
								if (mappedSlashPath != null && !slashSeparatedPath.equals(mappedSlashPath) && !Options.dontRemapBinaryNames) {
									String mappedDottedPath = mappedSlashPath.replace('/', '.');
									utf8String.setValue(mappedDottedPath);
								}
							} else if (Options.remapBinaryPathNames) {
								String remappedDottedPath = this.remapPathName(stringValue, readOnlyPackageMappings, ".");
								if (remappedDottedPath != null && !stringValue.equals(remappedDottedPath)) {
									utf8String.setValue(remappedDottedPath);
								}
							}
						}
					}
				}
			}
		}
	}

	public int calculateOptimizedSize() {
		HashSet<String> distinctUtf8Strings = GlobalUtils.newHashSet();
		HashSet<String> distinctNameAndTypes = GlobalUtils.newHashSet();
		HashSet<String> distinctStrings = GlobalUtils.newHashSet();

		for (UTFStringWriter utf8String : this.utf8StringConstants) {
			distinctUtf8Strings.add(utf8String.getAsString());
		}

		for (NameAndTypeWriter nameAndType : this.nameAndTypeConstants) {
			distinctNameAndTypes.add(nameAndType.getAsString());
		}

		for (StringNode stringNode : this.stringConstants) {
			distinctStrings.add(stringNode.getAsString());
		}

		int optimizedSize = this.getConstantPoolSize()
			- this.utf8StringConstants.size()
			+ distinctUtf8Strings.size()
			- this.nameAndTypeConstants.size()
			+ distinctNameAndTypes.size()
			- this.stringConstants.size()
			+ distinctStrings.size();
		if (optimizedSize > 65535) {
		}

		return optimizedSize;
	}

	@Override
	public synchronized int appendEntriesToPool(List<TypeWriter> entries) {
		int newPoolSize = super.appendEntriesToPool(entries);
		int entryCount = entries.size();

		for (int entryIndex = 0; entryIndex < entryCount; entryIndex++) {
			this.addConstantToTypeLists(entries.get(entryIndex));
		}

		return newPoolSize;
	}

	private void processDuplicateEntries(MultiMap<String, TypeWriter> duplicatesByKey, List<TypeWriter> cpEntries, TypeWriter[] constantPoolEntries, Map<TypeWriter, TypeWriter> mapping) {
		Enumeration<String> keyEnumeration = duplicatesByKey.keys();

		while (keyEnumeration.hasMoreElements()) {
			String key = keyEnumeration.nextElement();
			List<TypeWriter> duplicates = duplicatesByKey.get(key);
			int duplicateCount = duplicates.size();
			ArrayList<TypeWriter> duplicateEntries = null;
			if (duplicateCount > 1) {
				duplicateEntries = new ArrayList<>(duplicateCount - 1);
			}

			TypeWriter firstEntry = duplicates.get(0);
			cpEntries.add(firstEntry);

			for (int duplicateIndex = 1; duplicateIndex < duplicateCount; duplicateIndex++) {
				TypeWriter duplicate = duplicates.get(duplicateIndex);
				int tag = duplicate.getTag();
				constantPoolEntries[tag] = null;
				mapping.put(duplicate, firstEntry);
				if (duplicate.isDoubleSlotTag()) {
					constantPoolEntries[tag + 1] = null;
				}

				duplicateEntries.add(duplicate);
			}
		}
	}

	public void reindexBootstrapMethods() {
		if (super.dynamicCallConstants.size() > 0) {
			super.dynamicCallConstants.get(0).notifyParentToReindex();
		}
	}

	public FieldTypeWriter getOrCreateFieldRef(
		String ownerClassName, String fieldName, String descriptor, List<TypeWriter> cpEntries, AbstractWorkspace workspace, ClassProvider classProvider, boolean resolveNow
	) {
		FieldTypeWriter fieldRef = this.findFieldRef(ownerClassName, fieldName, descriptor);
		if (fieldRef != null) {
			return fieldRef;
		} else {
			fieldRef = (FieldTypeWriter)this.findMemberRefAmongNewEntries(ownerClassName, fieldName, descriptor, cpEntries);
			if (fieldRef != null) {
				return fieldRef;
			} else {
				ClassInfoWriter classInfo = this.getOrCreateClassInfo(ownerClassName, cpEntries);
				NameAndTypeWriter nameAndType = this.createNameAndType(fieldName, descriptor, cpEntries);
				FieldTypeWriter newFieldRef = new FieldTypeWriter(0, this, classInfo, nameAndType, workspace, classProvider, resolveNow);
				cpEntries.add(newFieldRef);
				return newFieldRef;
			}
		}
	}

	public StringNode createString(String value, List<TypeWriter> cpEntries) {
		return this.createStringNode(value, cpEntries, true);
	}

	public LongNode getOrCreateLong(long value, List<TypeWriter> cpEntries) {
		return this.getOrCreateLongInternal(value, cpEntries, true, false);
	}

	private void resolvePendingConstantRefs(
		ArrayList<TypeWriter> pendingConstants,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<NameAndTypeWriter, AbstractNamedWriter> nameAndTypeReferences,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		MultiMap<MemberTypeWriter, TypeNode> memberReferences,
		PrintWriter logWriter
	) {
		int passCount = 0;

		boolean retryRequired;
		do {
			retryRequired = false;
			ListIterator<TypeWriter> pendingIterator = pendingConstants.listIterator();

			while (pendingIterator.hasNext()) {
				Writer pendingConstant = (Writer)pendingIterator.next();
				TypeWriter resolvedConstant = pendingConstant.resolve(utf8References, nameAndTypeReferences, classReferences, memberReferences, logWriter);
				if (resolvedConstant == null) {
					retryRequired = true;
				} else {
					pendingIterator.remove();
					this.constantPoolEntries[pendingConstant.getTag()] = resolvedConstant;
					if (resolvedConstant instanceof TypeDataWriter) {
						this.typeDataWriters.add((TypeDataWriter)resolvedConstant);
					} else {
						this.addConstantToTypeLists(resolvedConstant);
					}
				}
			}

			if (++passCount > 3) {
				throw new ClassParseFailedException(this.getOwnerSourcePath() + " :  Unknown problem validating constant pool (B).");
			}
		} while (retryRequired);
	}

	@Override
	public <T extends ReferenceUpdateListener> void duplicateAndRemapConstants(
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8Refs,
		MultiMap<NameAndTypeWriter, AbstractNamedWriter> nameTypeRefs,
		MultiMap<StringNode, StringConstantUpdateListener> stringRefs,
		MultiMap<IntegerNode, IntegerConstants> intRefs,
		MultiMap<LongNode, LongConstants> longRefs,
		MultiMap<ClassInfoWriter, T> classRefs,
		MultiMap<MemberTypeWriter, TypeNode> memberRefs,
		MultiMap<InvokeDynamicCallWriter, IDynamicCallUpdatable> dynamicCallRefs
	) {
		super.duplicateAndRemapConstants(utf8Refs, nameTypeRefs, stringRefs, intRefs, longRefs, classRefs, memberRefs, dynamicCallRefs);
		HashMap<String, ClassInfoWriter> classInfoByName = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(this.classInfoConstants.size()));

		for (ClassInfoWriter classInfo : this.classInfoConstants) {
			classInfoByName.put(classInfo.getInternalName(), classInfo);
		}

		super.classInfoByNameCache = classInfoByName;
	}

	@Override
	public void remapConstantNames(HashMap<String, String> classNameMappings, AbstractLogger logger) {
		HashMap<String, ClassInfoWriter> classInfoByName = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(super.classInfoByNameCache.size()));

		for (ClassInfoWriter classInfo : this.classInfoConstants) {
			classInfo.remapName(classNameMappings);
			classInfoByName.put(classInfo.getInternalName(), classInfo);
		}

		super.classInfoByNameCache = classInfoByName;
		int entryCount = this.nameAndTypeConstants.size();

		for (int nameAndTypeIndex = 0; nameAndTypeIndex < entryCount; nameAndTypeIndex++) {
			if (this.nameAndTypeConstants.get(nameAndTypeIndex) != null) {
				this.nameAndTypeConstants.get(nameAndTypeIndex).remapDescriptor(classNameMappings);
			}
		}

		entryCount = this.methodTypeNodes.size();

		for (int methodTypeIndex = 0; methodTypeIndex < entryCount; methodTypeIndex++) {
			if (this.methodTypeNodes.get(methodTypeIndex) != null) {
				this.methodTypeNodes.get(methodTypeIndex).remapDescriptor(classNameMappings);
			}
		}

		HashSet<LibraryClassNode> visitedClasses = GlobalUtils.newHashSet(13);
		Iterator<MethodCallWriter> methodRefIterator = this.methodRefConstants.iterator();

		while (methodRefIterator.hasNext()) {
			AbstractMethodNode methodRefTarget = (AbstractMethodNode)methodRefIterator.next().getTargetMember();
			this.remapDynamicMethod(methodRefTarget, visitedClasses, classNameMappings, logger);
		}

		Iterator<InterfaceMethodCallWriter> interfaceMethodRefIterator = super.interfaceMethodRefConstants.iterator();

		while (interfaceMethodRefIterator.hasNext()) {
			AbstractMethodNode interfaceMethodRefTarget = (AbstractMethodNode)interfaceMethodRefIterator.next().getTargetMember();
			this.remapDynamicMethod(interfaceMethodRefTarget, visitedClasses, classNameMappings, logger);
		}

		Iterator<InvokeDynamicCallWriter> dynamicCallIterator = super.dynamicCallConstants.iterator();

		while (dynamicCallIterator.hasNext()) {
			AbstractMethodNode dynamicCallTarget = dynamicCallIterator.next().getResolvedBootstrapTarget();
			this.remapDynamicMethod(dynamicCallTarget, visitedClasses, classNameMappings, logger);
		}
	}

	public MethodCallWriter getOrCreateMethodRef(String ownerClassName, String methodName, String descriptor, List<TypeWriter> cpEntries, AbstractWorkspace workspace, ClassProvider classProvider) {
		MethodCallWriter methodRef = this.findMethodRef(ownerClassName, methodName, descriptor);
		if (methodRef != null) {
			return methodRef;
		} else {
			methodRef = (MethodCallWriter)this.findMemberRefAmongNewEntries(ownerClassName, methodName, descriptor, cpEntries);
			if (methodRef != null) {
				return methodRef;
			} else {
				ClassInfoWriter classInfo = this.getOrCreateClassInfo(ownerClassName, cpEntries);
				NameAndTypeWriter nameAndType = this.createNameAndType(methodName, descriptor, cpEntries);
				MethodCallWriter newMethodRef = new MethodCallWriter(0, this, classInfo, nameAndType, workspace, classProvider);
				cpEntries.add(newMethodRef);
				return newMethodRef;
			}
		}
	}

	public IntegerNode createInteger(int value, List<TypeWriter> cpEntries) {
		return this.createIntegerNode(value, cpEntries, true);
	}

	public List<ClassInfoWriter> findSubClasses(ClassHierarchy classHierarchy, String superClassName) {
		ArrayList<ClassInfoWriter> subClasses = new ArrayList<>();

		for (ClassInfoWriter classInfo : this.classInfoConstants) {
			if (!classInfo.isArray()) {
				String className = classInfo.getInternalName();

				try {
					if (className.equals(superClassName) || classHierarchy.isClassInstanceOf(className, superClassName)) {
						subClasses.add(classInfo);
					}
				} catch (FileException ignored) {
				}
			}
		}

		return subClasses;
	}

	@Override
	public boolean shouldObfuscateConstants() {
		return true;
	}

	private void remapDynamicMethod(AbstractMethodNode method, Set<LibraryClassNode> visitedClasses, HashMap<String, String> classNameMappings, AbstractLogger logger) {
		if (method != null && method.isDynamicallyGenerated()) {
			LibraryClassNode libraryClassNode = (LibraryClassNode)method.getClassNode();
			if (visitedClasses.add(libraryClassNode)) {
				libraryClassNode.remapConstantsAndDescriptors(classNameMappings, logger);
			}
		}
	}

	private void clearUnusedEntries(ConstantPoolVisitor constantPoolVisitor, TypeWriter[] constantPoolEntries) {
		for (int entryIndex = 0; entryIndex < constantPoolEntries.length; entryIndex++) {
			if (constantPoolEntries[entryIndex] != null && !(constantPoolEntries[entryIndex] instanceof NullTypeWriter)) {
				TypeWriter entry = constantPoolEntries[entryIndex];
				if (!constantPoolVisitor.contains(entry)) {
					constantPoolEntries[entryIndex] = null;
					if (entry.isDoubleSlotTag()) {
						TypeWriter secondSlotEntry = constantPoolEntries[++entryIndex];
						constantPoolEntries[entryIndex] = null;
					}
				}
			}
		}
	}

	public synchronized void removeUnusedConstants(Set<TypeWriter> unusedConstants) {
		Iterator<StringNode> stringIterator = this.stringConstants.iterator();

		while (stringIterator.hasNext()) {
			StringNode stringNode = stringIterator.next();
			if (unusedConstants.contains(stringNode)) {
				stringIterator.remove();
			}
		}

		Iterator<UTFStringWriter> utf8StringIterator = this.utf8StringConstants.iterator();

		while (utf8StringIterator.hasNext()) {
			UTFStringWriter utf8String = utf8StringIterator.next();
			if (unusedConstants.contains(utf8String)) {
				utf8StringIterator.remove();
			}
		}

		Iterator<IntegerNode> integerIterator = super.integerConstants.iterator();

		while (integerIterator.hasNext()) {
			IntegerNode integerNode = integerIterator.next();
			if (unusedConstants.contains(integerNode)) {
				integerIterator.remove();
			}
		}

		Iterator<LongNode> longIterator = super.longConstants.iterator();

		while (longIterator.hasNext()) {
			LongNode longNode = longIterator.next();
			if (unusedConstants.contains(longNode)) {
				longIterator.remove();
			}
		}

		int entryCount = this.constantPoolEntries.length;
		ArrayList<TypeWriter> retainedEntries = new ArrayList<>(entryCount);

		for (int entryIndex = 0; entryIndex < entryCount; entryIndex++) {
			TypeWriter entry = this.constantPoolEntries[entryIndex];
			if (!(entry instanceof NullTypeWriter) && unusedConstants.contains(entry)) {
				if (entry.getSlotSize() == 2) {
					entryIndex++;
				}
			} else {
				retainedEntries.add(entry);
			}
		}

		this.constantPoolEntries = retainedEntries.toArray(new TypeWriter[retainedEntries.size()]);
		this.updateConstantPoolIndices();
		this.initialPoolSize = this.constantPoolEntries.length;
	}

	public void collectExternalDependencies(HashSet<ClassNode> classReferences, HashSet<FieldNode> fieldReferences, HashSet<MethodNode> methodReferences) {
		int classInfoCount = this.classInfoConstants.size();

		for (int classInfoIndex = 0; classInfoIndex < classInfoCount; classInfoIndex++) {
			String className = AbstractClassNode.getTypeName(this.classInfoConstants.get(classInfoIndex).getInternalName());
			if (className != null) {
				ClassNode classNode = ClassHierarchyNode.getWorkspaceClassNode(className);
				if (classNode != null) {
					classReferences.add(classNode);
				}
			}
		}

		int fieldRefCount = this.fieldRefConstants.size();

		for (int fieldRefIndex = 0; fieldRefIndex < fieldRefCount; fieldRefIndex++) {
			FieldTypeWriter fieldRef = this.fieldRefConstants.get(fieldRefIndex);
			AbstractFieldNode targetField = (AbstractFieldNode)fieldRef.getTargetMember();
			if (targetField != null && targetField.isWorkspaceNode()) {
				fieldReferences.add((FieldNode)targetField);
			}

			ClassNode fieldTypeClassNode = fieldRef.getFieldTypeClassNode();
			if (fieldTypeClassNode != null) {
				classReferences.add(fieldTypeClassNode);
			}
		}

		int methodRefCount = this.methodRefConstants.size();

		for (int methodRefIndex = 0; methodRefIndex < methodRefCount; methodRefIndex++) {
			MethodCallWriter methodRef = this.methodRefConstants.get(methodRefIndex);
			AbstractMethodNode targetMethod = (AbstractMethodNode)methodRef.getTargetMember();
			if (targetMethod != null && targetMethod.isWorkspaceNode()) {
				methodReferences.add((MethodNode)targetMethod);
			}

			List<ClassNode> referencedClasses = methodRef.getReferencedWorkspaceClasses();

			for (int referencedClassIndex = 0; referencedClassIndex < referencedClasses.size(); referencedClassIndex++) {
				classReferences.add(referencedClasses.get(referencedClassIndex));
			}
		}

		int interfaceMethodRefCount = super.interfaceMethodRefConstants.size();

		for (int interfaceMethodRefIndex = 0; interfaceMethodRefIndex < interfaceMethodRefCount; interfaceMethodRefIndex++) {
			InterfaceMethodCallWriter interfaceMethodRef = super.interfaceMethodRefConstants.get(interfaceMethodRefIndex);
			AbstractMethodNode interfaceTargetMethod = (AbstractMethodNode)interfaceMethodRef.getTargetMember();
			if (interfaceTargetMethod != null && interfaceTargetMethod.isWorkspaceNode()) {
				methodReferences.add((MethodNode)interfaceTargetMethod);
			}

			List<ClassNode> interfaceReferencedClasses = interfaceMethodRef.getReferencedWorkspaceClasses();

			for (int interfaceReferencedClassIndex = 0; interfaceReferencedClassIndex < interfaceReferencedClasses.size(); interfaceReferencedClassIndex++) {
				classReferences.add(interfaceReferencedClasses.get(interfaceReferencedClassIndex));
			}
		}
	}

	public void filterUnusedConstants(ConstantPoolVisitor constantPoolVisitor) {
		int sourceCount = this.classInfoConstants.size();
		HashMap<String, ClassInfoWriter> classInfoByName = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(sourceCount));
		ArrayList<ClassInfoWriter> keptClassInfos = new ArrayList<>(sourceCount);
		Set<ClassInfoWriter> usedClassInfos = constantPoolVisitor.getClassInfos();

		for (int classInfoIndex = 0; classInfoIndex < sourceCount; classInfoIndex++) {
			ClassInfoWriter classInfo = this.classInfoConstants.get(classInfoIndex);
			if (usedClassInfos.contains(classInfo)) {
				keptClassInfos.add(classInfo);
				classInfoByName.put(classInfo.getInternalName(), classInfo);
			}
		}

		super.classInfoByNameCache = classInfoByName;
		this.classInfoConstants = keptClassInfos;
		sourceCount = this.utf8StringConstants.size();
		Set<UTFStringWriter> usedUtfStrings = constantPoolVisitor.getUtfStrings();
		ArrayList<UTFStringWriter> keptUtfStrings = new ArrayList<>(sourceCount);

		for (int utf8Index = 0; utf8Index < sourceCount; utf8Index++) {
			UTFStringWriter utf8String = this.utf8StringConstants.get(utf8Index);
			if (usedUtfStrings.contains(utf8String)) {
				keptUtfStrings.add(utf8String);
			}
		}

		this.utf8StringConstants = keptUtfStrings;
		sourceCount = this.nameAndTypeConstants.size();
		Set<NameAndTypeWriter> usedNameAndTypes = constantPoolVisitor.getNameAndTypes();
		ArrayList<NameAndTypeWriter> keptNameAndTypes = new ArrayList<>(sourceCount);

		for (int nameAndTypeIndex = 0; nameAndTypeIndex < sourceCount; nameAndTypeIndex++) {
			NameAndTypeWriter nameAndType = this.nameAndTypeConstants.get(nameAndTypeIndex);
			if (usedNameAndTypes.contains(nameAndType)) {
				keptNameAndTypes.add(nameAndType);
			}
		}

		this.nameAndTypeConstants = keptNameAndTypes;
		sourceCount = this.fieldRefConstants.size();
		Set<MemberTypeWriter> usedMemberReferences = constantPoolVisitor.getMemberReferences();
		ArrayList<FieldTypeWriter> keptFieldRefs = new ArrayList<>(sourceCount);

		for (int fieldRefIndex = 0; fieldRefIndex < sourceCount; fieldRefIndex++) {
			FieldTypeWriter fieldRef = this.fieldRefConstants.get(fieldRefIndex);
			if (usedMemberReferences.contains(fieldRef)) {
				keptFieldRefs.add(fieldRef);
			}
		}

		this.fieldRefConstants = keptFieldRefs;
		sourceCount = this.methodRefConstants.size();
		ArrayList<MethodCallWriter> keptMethodRefs = new ArrayList<>(sourceCount);

		for (int methodRefIndex = 0; methodRefIndex < sourceCount; methodRefIndex++) {
			MethodCallWriter methodRef = this.methodRefConstants.get(methodRefIndex);
			if (usedMemberReferences.contains(methodRef)) {
				keptMethodRefs.add(methodRef);
			}
		}

		this.methodRefConstants = keptMethodRefs;
		sourceCount = super.interfaceMethodRefConstants.size();
		ArrayList<InterfaceMethodCallWriter> keptInterfaceMethodRefs = new ArrayList<>(sourceCount);

		for (int interfaceMethodRefIndex = 0; interfaceMethodRefIndex < sourceCount; interfaceMethodRefIndex++) {
			InterfaceMethodCallWriter interfaceMethodRef = super.interfaceMethodRefConstants.get(interfaceMethodRefIndex);
			if (usedMemberReferences.contains(interfaceMethodRef)) {
				keptInterfaceMethodRefs.add(interfaceMethodRef);
			}
		}

		super.interfaceMethodRefConstants = keptInterfaceMethodRefs;
		sourceCount = this.stringConstants.size();
		Set<ConstantWriter> usedNumericConstants = constantPoolVisitor.getNumericConstants();
		ArrayList<StringNode> keptStrings = new ArrayList<>(sourceCount);

		for (int stringIndex = 0; stringIndex < sourceCount; stringIndex++) {
			StringNode stringNode = this.stringConstants.get(stringIndex);
			if (usedNumericConstants.contains(stringNode)) {
				keptStrings.add(stringNode);
			}
		}

		this.stringConstants = keptStrings;
		sourceCount = super.integerConstants.size();
		ArrayList<IntegerNode> keptIntegers = new ArrayList<>(sourceCount);

		for (int integerIndex = 0; integerIndex < sourceCount; integerIndex++) {
			IntegerNode integerNode = super.integerConstants.get(integerIndex);
			if (usedNumericConstants.contains(integerNode)) {
				keptIntegers.add(integerNode);
			}
		}

		super.integerConstants = keptIntegers;
		sourceCount = super.longConstants.size();
		ArrayList<LongNode> keptLongs = new ArrayList<>(sourceCount);

		for (int longIndex = 0; longIndex < sourceCount; longIndex++) {
			LongNode longNode = super.longConstants.get(longIndex);
			if (usedNumericConstants.contains(longNode)) {
				keptLongs.add(longNode);
			}
		}

		super.longConstants = keptLongs;
		sourceCount = this.floatNodes.size();
		ArrayList<FloatNode> keptFloats = new ArrayList<>(sourceCount);

		for (int floatIndex = 0; floatIndex < sourceCount; floatIndex++) {
			FloatNode floatNode = this.floatNodes.get(floatIndex);
			if (usedNumericConstants.contains(floatNode)) {
				keptFloats.add(floatNode);
			}
		}

		this.floatNodes = keptFloats;
		sourceCount = this.doubleNodes.size();
		ArrayList<DoubleNode> keptDoubles = new ArrayList<>(sourceCount);

		for (int doubleIndex = 0; doubleIndex < sourceCount; doubleIndex++) {
			DoubleNode doubleNode = this.doubleNodes.get(doubleIndex);
			if (usedNumericConstants.contains(doubleNode)) {
				keptDoubles.add(doubleNode);
			}
		}

		this.doubleNodes = keptDoubles;
		sourceCount = super.dynamicValueConstants.size();
		Set<DynamicConstantWriter> usedDynamicConstants = constantPoolVisitor.getDynamicConstants();
		ArrayList<DynamicConstantWriter> keptDynamicValues = new ArrayList<>(sourceCount);

		for (int dynamicValueIndex = 0; dynamicValueIndex < sourceCount; dynamicValueIndex++) {
			DynamicConstantWriter dynamicValue = super.dynamicValueConstants.get(dynamicValueIndex);
			if (usedDynamicConstants.contains(dynamicValue)) {
				keptDynamicValues.add(dynamicValue);
			}
		}

		super.dynamicValueConstants = keptDynamicValues;
		sourceCount = super.dynamicCallConstants.size();
		Set<InvokeDynamicCallWriter> usedDynamicCalls = constantPoolVisitor.getDynamicCalls();
		ArrayList<InvokeDynamicCallWriter> keptDynamicCalls = new ArrayList<>(sourceCount);

		for (int dynamicCallIndex = 0; dynamicCallIndex < sourceCount; dynamicCallIndex++) {
			InvokeDynamicCallWriter dynamicCall = super.dynamicCallConstants.get(dynamicCallIndex);
			if (usedDynamicCalls.contains(dynamicCall)) {
				keptDynamicCalls.add(dynamicCall);
			}
		}

		super.dynamicCallConstants = keptDynamicCalls;
		sourceCount = this.methodTypeNodes.size();
		Set<MethodDescriptorWriter> usedMethodTypes = constantPoolVisitor.getMethodTypes();
		ArrayList<MethodDescriptorWriter> keptMethodTypes = new ArrayList<>(sourceCount);

		for (int methodTypeIndex = 0; methodTypeIndex < sourceCount; methodTypeIndex++) {
			MethodDescriptorWriter methodType = this.methodTypeNodes.get(methodTypeIndex);
			if (usedMethodTypes.contains(methodType)) {
				keptMethodTypes.add(methodType);
			}
		}

		this.methodTypeNodes = keptMethodTypes;
		sourceCount = this.methodHandleNodes.size();
		Set<ResolvedMethodHandleWriter> usedMethodHandles = constantPoolVisitor.getMethodHandles();
		ArrayList<ResolvedMethodHandleWriter> keptMethodHandles = new ArrayList<>(sourceCount);

		for (int methodHandleIndex = 0; methodHandleIndex < sourceCount; methodHandleIndex++) {
			ResolvedMethodHandleWriter methodHandle = this.methodHandleNodes.get(methodHandleIndex);
			if (usedMethodHandles.contains(methodHandle)) {
				keptMethodHandles.add(methodHandle);
			}
		}

		this.methodHandleNodes = keptMethodHandles;
		int entryCount = this.constantPoolEntries.length;
		ArrayList<TypeWriter> keptEntries = new ArrayList<>(sourceCount);

		for (int entryIndex = 0; entryIndex < entryCount; entryIndex++) {
			TypeWriter entry = this.constantPoolEntries[entryIndex];
			if (!(entry instanceof NullTypeWriter) && !constantPoolVisitor.contains(entry)) {
				if (entry.getSlotSize() == 2) {
					entryIndex++;
				}
			} else {
				keptEntries.add(entry);
			}
		}

		this.constantPoolEntries = keptEntries.toArray(new TypeWriter[keptEntries.size()]);
		this.updateConstantPoolIndices();
		this.initialPoolSize = this.constantPoolEntries.length;
	}

	public void updateBootstrapMethodRefs(BootstrapMethodsAttribute bootstrapMethods) {
		for (InvokeDynamicCallWriter dynamicCall : super.dynamicCallConstants) {
			dynamicCall.setBootstrapMethodEntry(bootstrapMethods.getEntry(dynamicCall.getBootstrapMethodIndex()));
		}

		for (DynamicConstantWriter dynamicValue : super.dynamicValueConstants) {
			dynamicValue.setBootstrapMethodEntry(bootstrapMethods.getEntry(dynamicValue.getBootstrapMethodIndex()));
		}
	}

	public RemappedConstantPoolWriter optimizeAndBuildNewPool(ConstantPoolVisitor constantPoolVisitor, Set<InstructionListNode> changedMethods) {
		HashMap<TypeWriter, TypeWriter> remapMap = GlobalUtils.newHashMap(
			GlobalUtils.getInitialCapacity(Math.max(7, (int)((this.getConstantPoolSize() - this.initialPoolSize) * 0.7)))
		);
		TypeWriter[] entryArray = (TypeWriter[])this.constantPoolEntries.clone();
		int initialCapacity = Math.max(15, (int)(this.initialPoolSize * 0.7));
		MultiMap<String, TypeWriter> utf8StringsByValue = new MultiMap<>(initialCapacity, 1);
		ArrayList<TypeWriter> keptConstants = new ArrayList<>(Math.max(5, (int)(this.initialPoolSize * 0.7)));

		for (int utf8Index = 0; utf8Index < this.utf8StringConstants.size(); utf8Index++) {
			UTFStringWriter utf8String = this.utf8StringConstants.get(utf8Index);
			if (constantPoolVisitor.containsUtfString(utf8String)) {
				String utf8Value = utf8String.getAsString();
				utf8StringsByValue.put(utf8Value, utf8String);
			} else {
				entryArray[utf8String.getTag()] = null;
			}
		}

		this.processDuplicateEntries(utf8StringsByValue, keptConstants, entryArray, remapMap);
		MultiMap<String, TypeWriter> nameAndTypesByValue = new MultiMap<>(Math.max(10, (this.getConstantPoolSize() - this.initialPoolSize) / 4), 1);

		for (int nameAndTypeIndex = 0; nameAndTypeIndex < this.nameAndTypeConstants.size(); nameAndTypeIndex++) {
			NameAndTypeWriter nameAndType = this.nameAndTypeConstants.get(nameAndTypeIndex);
			if (constantPoolVisitor.containsNameAndType(nameAndType)) {
				String nameAndTypeValue = nameAndType.getAsString();
				nameAndTypesByValue.put(nameAndTypeValue, nameAndType);
			} else {
				entryArray[nameAndType.getTag()] = null;
			}
		}

		this.processDuplicateEntries(nameAndTypesByValue, keptConstants, entryArray, remapMap);
		MultiMap<String, TypeWriter> stringsByValue = new MultiMap<>(Math.max(10, (this.getConstantPoolSize() - this.initialPoolSize) / 4), 1);

		for (int stringIndex = 0; stringIndex < this.stringConstants.size(); stringIndex++) {
			StringNode stringNode = this.stringConstants.get(stringIndex);
			if (constantPoolVisitor.containsStringLiteral(stringNode)) {
				String stringValue = stringNode.getAsString();
				stringsByValue.put(stringValue, stringNode);
			} else {
				entryArray[stringNode.getTag()] = null;
			}
		}

		this.processDuplicateEntries(stringsByValue, keptConstants, entryArray, remapMap);
		MultiMap<String, TypeWriter> dynamicCallsByValue = new MultiMap<>(Math.max(10, (this.getConstantPoolSize() - this.initialPoolSize) / 4), 1);

		for (int dynamicCallIndex = 0; dynamicCallIndex < super.dynamicCallConstants.size(); dynamicCallIndex++) {
			InvokeDynamicCallWriter dynamicCall = super.dynamicCallConstants.get(dynamicCallIndex);
			if (constantPoolVisitor.containsDynamicCall(dynamicCall)) {
				String dynamicCallValue = dynamicCall.getAsString();
				dynamicCallsByValue.put(dynamicCallValue, dynamicCall);
			} else {
				entryArray[dynamicCall.getTag()] = null;
			}
		}

		this.processDuplicateEntries(dynamicCallsByValue, keptConstants, entryArray, remapMap);
		MultiMap<String, TypeWriter> integersByValue = new MultiMap<>(Math.max(10, (this.getConstantPoolSize() - this.initialPoolSize) / 4), 1);

		for (int integerIndex = 0; integerIndex < super.integerConstants.size(); integerIndex++) {
			IntegerNode integerNode = super.integerConstants.get(integerIndex);
			if (constantPoolVisitor.containsInteger(integerNode)) {
				integersByValue.put(integerNode.getAsString(), integerNode);
			} else {
				entryArray[integerNode.getTag()] = null;
			}
		}

		this.processDuplicateEntries(integersByValue, keptConstants, entryArray, remapMap);
		MultiMap<String, TypeWriter> longsByValue = new MultiMap<>(Math.max(10, (this.getConstantPoolSize() - this.initialPoolSize) / 4), 1);

		for (int longIndex = 0; longIndex < super.longConstants.size(); longIndex++) {
			LongNode longNode = super.longConstants.get(longIndex);
			if (constantPoolVisitor.containsLong(longNode)) {
				longsByValue.put(longNode.getAsString(), longNode);
			} else {
				int longTag = longNode.getTag();
				entryArray[longTag] = null;
				entryArray[longTag + 1] = null;
			}
		}

		this.processDuplicateEntries(longsByValue, keptConstants, entryArray, remapMap);
		this.clearUnusedEntries(constantPoolVisitor, entryArray);
		int entryCount = entryArray.length;
		ArrayList<TypeWriter> orderedEntries = new ArrayList<>(entryCount);
		boolean watermarkWritten = Options.ZKM_DONT_ADD_WATERMARK;
		HashSet<TypeWriter> addedEntries = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(entryCount));
		orderedEntries.add(entryArray[0]);
		addedEntries.add(entryArray[0]);
		ReferenceGraph<TypeWriter, TypeWriter> referenceGraph = new ReferenceGraph<>(remapMap.size());

		for (Entry<TypeWriter, TypeWriter> remapEntry : remapMap.entrySet()) {
			referenceGraph.addReference(remapEntry.getValue(), remapEntry.getKey());
		}

		Random random = null;
		List<TypeWriter> ldcReferencedConstants = constantPoolVisitor.getLdcReferencedConstants();
		if (!Options.sortConstantPoolEntries) {
			random = GlobalUtils.newRandom(1428);
			GlobalUtils.shuffle(ldcReferencedConstants, random);
		} else {
			Collections.sort(ldcReferencedConstants);
		}

		for (TypeWriter ldcConstant : ldcReferencedConstants) {
			TypeWriter mappedConstant = GlobalUtils.getMappedOrDefault(ldcConstant, remapMap);
			if (addedEntries.add(mappedConstant)) {
				orderedEntries.add(mappedConstant);
				if (orderedEntries.size() > 255) {
					changedMethods.addAll(constantPoolVisitor.getReferencingInstructions(mappedConstant));
					Set<TypeWriter> ldcReferences = referenceGraph.getReferences(mappedConstant);
					if (ldcReferences != null) {
						for (TypeWriter ldcReference : ldcReferences) {
							changedMethods.addAll(constantPoolVisitor.getReferencingInstructions(ldcReference));
						}
					}
				}
			}
		}

		List<TypeWriter> ldcWReferencedConstants = constantPoolVisitor.getLdcWReferencedConstants();
		if (!Options.sortConstantPoolEntries) {
			GlobalUtils.shuffle(ldcWReferencedConstants, random);
		} else {
			Collections.sort(ldcWReferencedConstants);
		}

		for (TypeWriter ldcWConstant : ldcWReferencedConstants) {
			TypeWriter mappedLdcWConstant = GlobalUtils.getMappedOrDefault(ldcWConstant, remapMap);
			if (addedEntries.add(mappedLdcWConstant)) {
				orderedEntries.add(mappedLdcWConstant);
				if (orderedEntries.size() <= 255) {
					changedMethods.addAll(constantPoolVisitor.getReferencingInstructions(mappedLdcWConstant));
					Set<TypeWriter> ldcWReferences = referenceGraph.getReferences(mappedLdcWConstant);
					if (ldcWReferences != null) {
						for (TypeWriter ldcWReference : ldcWReferences) {
							changedMethods.addAll(constantPoolVisitor.getReferencingInstructions(ldcWReference));
						}
					}
				}
			}
		}

		ArrayList<TypeWriter> shuffledEntries = new ArrayList<>();
		ArrayList<TypeWriter> doubleSlotEntries = new ArrayList<>();

		for (int entryIndex = 0; entryIndex < entryCount; entryIndex++) {
			TypeWriter entry = entryArray[entryIndex];
			if (entry != null) {
				if (!addedEntries.contains(entry)) {
					if (!Options.sortConstantPoolEntries) {
						if (entry.getSlotSize() == 2) {
							doubleSlotEntries.add(entry);
							entryIndex++;
						} else {
							shuffledEntries.add(entry);
						}
					} else {
						orderedEntries.add(entry);
						addedEntries.add(entry);
						if (entry.getSlotSize() == 2) {
							NullTypeWriter secondSlotPlaceholder = (NullTypeWriter)entryArray[++entryIndex];
							orderedEntries.add(secondSlotPlaceholder);
							addedEntries.add(secondSlotPlaceholder);
						}
					}
				}
			} else if (!watermarkWritten) {
				orderedEntries.add(new UTFStringWriter(entryIndex, this, WATERMARK_STRING));
				watermarkWritten = true;
			}
		}

		if (shuffledEntries.size() > 0) {
			GlobalUtils.shuffle(shuffledEntries, random);

			for (TypeWriter shuffledEntry : shuffledEntries) {
				orderedEntries.add(shuffledEntry);
				addedEntries.add(shuffledEntry);
			}
		}

		if (doubleSlotEntries.size() > 0) {
			GlobalUtils.shuffle(doubleSlotEntries, random);
			NullTypeWriter nullSlotPlaceholder = this.nullSlotPlaceholders[0];

			for (TypeWriter doubleSlotEntry : doubleSlotEntries) {
				orderedEntries.add(doubleSlotEntry);
				addedEntries.add(doubleSlotEntry);
				orderedEntries.add(nullSlotPlaceholder);
				addedEntries.add(nullSlotPlaceholder);
			}
		}

		entryArray = orderedEntries.toArray(new TypeWriter[orderedEntries.size()]);
		return new RemappedConstantPoolWriter(this, entryArray, remapMap);
	}

	public void clearStringObfuscation() {
		Iterator<StringNode> stringIterator = this.stringConstants.iterator();

		while (stringIterator.hasNext()) {
			stringIterator.next().setNeedStringObf(false);
		}
	}

	public void resolveMethodTargets(Set<ClassNode> reflectionClasses, Map<AbstractMethodCallWriter, ReflectionMethodPattern> reflectionPatterns, AbstractWorkspace workspace, AbstractLogger logger) {
		for (int methodRefIndex = 0; methodRefIndex < this.methodRefConstants.size(); methodRefIndex++) {
			AbstractMethodCallWriter methodRef = this.methodRefConstants.get(methodRefIndex);

			try {
				ReflectionMethodPattern matchedPattern;
				if ((matchedPattern = methodRef.findPatternWithInheritance(workspace)) != null) {
					reflectionPatterns.put(methodRef, matchedPattern);
					reflectionClasses.add((ClassNode)this.ownerClassNode);
				}
			} catch (ClasspathException methodRefClasspathException) {
				logger.warn(
					"CLASSPATH WARNING:",
					"Reference to method '"
						+ methodRef.toDisplayString()
						+ "' couldn't be checked because couldn't find '"
						+ methodRefClasspathException.getClassName()
						+ "'. Ensure appropriate classpath is set BEFORE classes are loaded. (1)"
				);
			} catch (FileException methodRefFileException) {
				logger.warn("CLASSPATH WARNING:", "Reference to method '" + methodRef.toDisplayString() + "' couldn't be checked. (1) : " + methodRefFileException.getMessage());
			}
		}

		for (int interfaceMethodRefIndex = 0; interfaceMethodRefIndex < super.interfaceMethodRefConstants.size(); interfaceMethodRefIndex++) {
			AbstractMethodCallWriter interfaceMethodRef = super.interfaceMethodRefConstants.get(interfaceMethodRefIndex);

			try {
				ReflectionMethodPattern interfaceMatchedPattern;
				if ((interfaceMatchedPattern = interfaceMethodRef.findPatternWithInheritance(workspace)) != null) {
					reflectionPatterns.put(interfaceMethodRef, interfaceMatchedPattern);
					reflectionClasses.add((ClassNode)this.ownerClassNode);
				}
			} catch (ClasspathException interfaceMethodRefClasspathException) {
				logger.warn(
					"CLASSPATH WARNING:",
					"Reference to method '"
						+ interfaceMethodRef.toDisplayString()
						+ "' couldn't be checked because couldn't find '"
						+ interfaceMethodRefClasspathException.getClassName()
						+ "'. Ensure appropriate classpath is set BEFORE classes are loaded. (2)"
				);
			} catch (FileException interfaceMethodRefFileException) {
				logger.warn("CLASSPATH WARNING:", "Reference to method '" + interfaceMethodRef.toDisplayString() + "' couldn't be checked. (2) : " + interfaceMethodRefFileException.getMessage());
			}
		}
	}

	private String remapPathName(String pathName, ReadOnlyMap<String, String> packageMappings, String separator) {
		StringTokenizer tokenizer = new StringTokenizer(pathName, separator);
		int tokenIndex = 0;

		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (!isJavaIdentifier(token)) {
				return null;
			}

			if (tokenIndex++ == 0 && token.length() < 2) {
				return null;
			}
		}

		AtomicInt depth = new AtomicInt();
		String remappedPath = PathRemapper.remapPathAndSetDepth(pathName.replace(separator, "/"), packageMappings, depth).replace("/", separator);
		return !pathName.equals(remappedPath) && depth.get() < 2 ? null : remappedPath;
	}

	@Override
	public void remapPackageNames(HashMap<String, String> packageMappings) {
		Iterator<PackageNameWriter> packageIterator = this.packageNodes.iterator();

		while (packageIterator.hasNext()) {
			packageIterator.next().remapName(packageMappings);
		}
	}

	public MethodCallWriter getOrCreateUnboxingMethod(String descriptor, List<TypeWriter> cpEntries, AbstractWorkspace workspace, ClassProvider classProvider) {
		switch (descriptor.charAt(0)) {
			case 'B':
			case 'C':
			case 'S':
				descriptor = "I";
			default:
				String wrapperName = TypeWriter.getWrapperName(descriptor);
				String valueMethodName = PRIMITIVE_VALUE_METHODS.get(descriptor);
				String valueMethodDescriptor = PRIMITIVE_VALUE_DESCRIPTORS.get(descriptor);
				return this.getOrCreateMethodRef(wrapperName, valueMethodName, valueMethodDescriptor, cpEntries, workspace, classProvider);
		}
	}

	private void addConstantToTypeLists(TypeWriter constant) {
		TypeTags typeTag = constant.getTypeTag();
		switch (typeTag) {
			case CLASS:
				this.classInfoConstants.add((ClassInfoWriter)constant);
				ClassInfoWriter previousClassInfo = super.classInfoByNameCache.put(((ClassInfoWriter)constant).getInternalName(), (ClassInfoWriter)constant);
				break;
			case NAME_AND_TYPE:
				this.nameAndTypeConstants.add((NameAndTypeWriter)constant);
				break;
			case METHODREF:
				this.methodRefConstants.add((MethodCallWriter)constant);
				break;
			case INTERFACE_METHODREF:
				super.interfaceMethodRefConstants.add((InterfaceMethodCallWriter)constant);
				break;
			case FIELDREF:
				this.fieldRefConstants.add((FieldTypeWriter)constant);
				break;
			case UTF8:
				this.utf8StringConstants.add((UTFStringWriter)constant);
				break;
			case STRING:
				this.stringConstants.add((StringNode)constant);
				break;
			case INTEGER:
				super.integerConstants.add((IntegerNode)constant);
				break;
			case LONG:
				super.longConstants.add((LongNode)constant);
				break;
			case FLOAT:
				this.floatNodes.add((FloatNode)constant);
				break;
			case DOUBLE:
				this.doubleNodes.add((DoubleNode)constant);
				break;
			case METHOD_HANDLE:
				this.methodHandleNodes.add((ResolvedMethodHandleWriter)constant);
				break;
			case METHOD_TYPE:
				this.methodTypeNodes.add((MethodDescriptorWriter)constant);
				break;
			case CONSTANT_DYNAMIC:
				super.dynamicValueConstants.add((DynamicConstantWriter)constant);
				break;
			case INVOKE_DYNAMIC:
				super.dynamicCallConstants.add((InvokeDynamicCallWriter)constant);
				break;
			case MODULE:
				this.moduleNodes.add((ModuleNameWriter)constant);
				break;
			case PACKAGE:
				this.packageNodes.add((PackageNameWriter)constant);
		}
	}

	public StringNode createStringNode(String value, List<TypeWriter> cpEntries, boolean needStringObf) {
		UTFStringWriter utf8String = new UTFStringWriter(0, this, value);
		StringNode stringNode = new StringNode(0, this, utf8String, needStringObf);
		cpEntries.add(utf8String);
		cpEntries.add(stringNode);
		return stringNode;
	}

	public LongNode getOrCreateLongInternal(long value, List<TypeWriter> cpEntries, boolean reuseExisting, boolean needLongObf) {
		if (reuseExisting) {
			for (int longIndex = 0; longIndex < super.longConstants.size(); longIndex++) {
				LongNode longNode = super.longConstants.get(longIndex);
				if (!longNode.isNeedLongObf() && longNode.isEqual(value)) {
					return longNode;
				}
			}

			for (TypeWriter entry : cpEntries) {
				if (entry instanceof LongNode) {
					LongNode candidate = (LongNode)entry;
					if (!candidate.isNeedLongObf() && candidate.isEqual(value)) {
						return candidate;
					}
				}
			}
		}

		LongNode newLongNode;
		if (needLongObf) {
			newLongNode = new LongNode(0, this, value, true);
		} else {
			newLongNode = new LongNode(0, this, value, false);
		}

		cpEntries.add(newLongNode);
		return newLongNode;
	}

	public IntegerNode createIntegerNode(int value, List<TypeWriter> cpEntries, boolean needIntObf) {
		return this.getOrCreateInteger(value, cpEntries, needIntObf, false);
	}

	static {
		StringBuilder watermarkBuilder = new StringBuilder();

		for (char watermarkChar : WATERMARK_CHARS) {
			watermarkBuilder.append(watermarkChar);
		}

		if (false) {
			watermarkBuilder.append("27.0.0");
			watermarkBuilder.append('E');
		}

		watermarkBuilder.append(CopyrightConstants.VERSION);
		WATERMARK_STRING = watermarkBuilder.toString();
		PRIMITIVE_VALUE_METHODS.put("B", "byteValue");
		PRIMITIVE_VALUE_METHODS.put("C", "charValue");
		PRIMITIVE_VALUE_METHODS.put("D", "doubleValue");
		PRIMITIVE_VALUE_METHODS.put("F", "floatValue");
		PRIMITIVE_VALUE_METHODS.put("I", "intValue");
		PRIMITIVE_VALUE_METHODS.put("J", "longValue");
		PRIMITIVE_VALUE_METHODS.put("S", "shortValue");
		PRIMITIVE_VALUE_METHODS.put("Z", "booleanValue");
		PRIMITIVE_VALUE_DESCRIPTORS.put("B", "()B");
		PRIMITIVE_VALUE_DESCRIPTORS.put("C", "()C");
		PRIMITIVE_VALUE_DESCRIPTORS.put("D", "()D");
		PRIMITIVE_VALUE_DESCRIPTORS.put("F", "()F");
		PRIMITIVE_VALUE_DESCRIPTORS.put("I", "()I");
		PRIMITIVE_VALUE_DESCRIPTORS.put("J", "()J");
		PRIMITIVE_VALUE_DESCRIPTORS.put("S", "()S");
		PRIMITIVE_VALUE_DESCRIPTORS.put("Z", "()Z");
	}
}
