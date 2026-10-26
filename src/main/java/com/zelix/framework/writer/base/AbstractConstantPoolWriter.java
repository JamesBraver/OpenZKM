package com.zelix.framework.writer.base;

import com.zelix.enums.TypeTags;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.TypeNode;
import com.zelix.framework.constant.ConstantPool;
import com.zelix.framework.constant.IntegerNode;
import com.zelix.framework.constant.LongNode;
import com.zelix.framework.constant.StringNode;
import com.zelix.framework.listener.*;
import com.zelix.framework.writer.*;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.logging.AbstractLogger;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.ArraySet;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.collections.readOnly.ReadOnlyList;
import com.zelix.workspace.AbstractWorkspace;

import java.util.*;
import java.util.Map.Entry;

public abstract class AbstractConstantPoolWriter implements ConstantPool {
	public final NullTypeWriter[] nullSlotPlaceholders = new NullTypeWriter[2];
	public ArrayList<UTFStringWriter> unreferencedUtf8Strings = new ArrayList<>();
	public ArrayList<LongNode> longConstants = new ArrayList<>();
	public ArrayList<InvokeDynamicCallWriter> dynamicCallConstants = new ArrayList<>();
	public ArrayList<DynamicConstantWriter> dynamicValueConstants = new ArrayList<>();
	public AbstractClassNode ownerClassNode;
	public ArrayList<FieldTypeWriter> fieldRefConstants;
	public ArrayList<MethodCallWriter> methodRefConstants;
	public ArrayList<ClassInfoWriter> classInfoConstants;
	public ArrayList<IntegerNode> integerConstants;
	public ArrayList<UTFStringWriter> utf8StringConstants;
	public ArrayList<StringNode> stringConstants;
	public ArrayList<NameAndTypeWriter> nameAndTypeConstants;
	public TypeWriter[] constantPoolEntries;
	public ArrayList<InterfaceMethodCallWriter> interfaceMethodRefConstants;
	public Map<String, ClassInfoWriter> classInfoByNameCache;

	public AbstractConstantPoolWriter(AbstractClassNode ownerClassNode) {
		this.ownerClassNode = ownerClassNode;
	}

	public abstract void remapTargetClassName(String newClassName);

	public String getOwnerClassNameBinary() {
		return this.ownerClassNode.getClassNameBinary();
	}

	public final String getOwnerRawSourcePath() {
		return this.ownerClassNode.getRawSourcePath();
	}

	protected InterfaceMethodCallWriter findInterfaceMethodRef(String owner, String name, String desc) {
		for (InterfaceMethodCallWriter methodRef : this.interfaceMethodRefConstants) {
			if (methodRef.isTarget(owner, name, desc)) {
				return methodRef;
			}
		}

		return null;
	}

	protected FieldTypeWriter findFieldRef(String owner, String name, String desc) {
		for (int i = 0; i < this.fieldRefConstants.size(); i++) {
			FieldTypeWriter fieldRef = this.fieldRefConstants.get(i);
			if (fieldRef.isTarget(owner, name, desc)) {
				return fieldRef;
			}
		}

		return null;
	}

	public final MethodCallWriter getOrCreateClassMethodRef(AbstractMethodNode methodNode, List<TypeWriter> entries) {
		return (MethodCallWriter)this.getOrCreateMethodRefInternal(methodNode.getInternalName(), methodNode.getName(), methodNode.getDescriptor(), entries, methodNode);
	}

	@Override
	public final TypeWriter getConstantPoolEntry(int index) {
		return index > 0 && index < this.constantPoolEntries.length ? this.constantPoolEntries[index] : null;
	}

	public UTFStringWriter createUtf8String(String value, List<TypeWriter> entries) {
		UTFStringWriter utf8StringWriter = new UTFStringWriter(0, this, value);
		entries.add(utf8StringWriter);
		return utf8StringWriter;
	}

	public final FieldTypeWriter getOrCreateFieldRef(String owner, String name, String desc, List<TypeWriter> entries, AbstractFieldNode fieldNode) {
		FieldTypeWriter existingFieldRef = this.findFieldRef(owner, name, desc);
		if (existingFieldRef != null) {
			return existingFieldRef;
		} else {
			existingFieldRef = (FieldTypeWriter)this.findMemberRefAmongNewEntries(owner, name, desc, entries);
			if (existingFieldRef != null) {
				return existingFieldRef;
			} else {
				ClassInfoWriter classInfo = this.getOrCreateClassInfo(owner, entries);
				NameAndTypeWriter nameAndType = this.createNameAndType(name, desc, entries);
				FieldTypeWriter newFieldRef = new FieldTypeWriter(0, this, classInfo, nameAndType, fieldNode);
				entries.add(newFieldRef);
				return newFieldRef;
			}
		}
	}

	public synchronized int appendEntriesToPool(List<TypeWriter> entries) {
		int appendedEntryCount = entries.size();
		int slotTotal = 0;

		for (int i = 0; i < appendedEntryCount; i++) {
			slotTotal += entries.get(i).getSlotSize();
		}

		int currentIndex = this.constantPoolEntries.length;
		TypeWriter[] newPool = new TypeWriter[currentIndex + slotTotal];
		System.arraycopy(this.constantPoolEntries, 0, newPool, 0, currentIndex);

		for (int sourceEntryIndex = 0; sourceEntryIndex < appendedEntryCount; sourceEntryIndex++) {
			TypeWriter newEntry = entries.get(sourceEntryIndex);
			newEntry.setTag(currentIndex);
			newPool[currentIndex++] = newEntry;
			if (newEntry.getSlotSize() == 2) {
				newPool[currentIndex] = NullTypeWriter.getNullSlotPlaceholder(this, 1);
				currentIndex++;
			}
		}

		this.constantPoolEntries = newPool;
		return this.constantPoolEntries.length;
	}

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
		for (int i = 0; i < this.utf8StringConstants.size(); i++) {
			UTFStringWriter poolUtf8Writer = this.utf8StringConstants.get(i);
			if (!utf8Refs.containsKey(poolUtf8Writer)) {
				this.unreferencedUtf8Strings.add(poolUtf8Writer);
			}
		}

		this.deduplicateClassReferences(classRefs);
		this.deduplicateMemberReferences(memberRefs);
		int extraEntryCount = utf8Refs.valueCount()
			- utf8Refs.size()
			+ (nameTypeRefs.valueCount() - nameTypeRefs.size()) * 3
			+ (stringRefs.valueCount() - stringRefs.size()) * 2
			+ intRefs.valueCount()
			- intRefs.size()
			+ (longRefs.valueCount() - longRefs.size()) * 2
			+ (dynamicCallRefs.valueCount() - dynamicCallRefs.size()) * 4;
		if (extraEntryCount > 0) {
			ArrayList<TypeWriter> newEntries = new ArrayList<>(this.constantPoolEntries.length + extraEntryCount);
			newEntries.addAll(new ArraySet<>(this.constantPoolEntries));
			int nextPoolIndex = this.constantPoolEntries.length;
			ReadOnlyList<StringNode> sortedStringNodes = new ReadOnlyList<>(stringRefs.keys());
			Collections.sort(sortedStringNodes);

			for (StringNode stringNode : sortedStringNodes) {
				List<StringConstantUpdateListener> stringListeners = stringRefs.get(stringNode);
				int stringListenerCount = stringListeners.size();

				for (int stringListenerIndex = 1; stringListenerIndex < stringListenerCount; stringListenerIndex++) {
					StringConstantUpdateListener stringListener = stringListeners.get(stringListenerIndex);
					UTFStringWriter stringUtf8Writer = stringNode.getUtf8StringWriter().copy(nextPoolIndex++);
					newEntries.add(stringUtf8Writer);
					this.utf8StringConstants.add(stringUtf8Writer);
					StringNode newStringNode = new StringNode(nextPoolIndex++, this, stringUtf8Writer, stringNode, utf8Refs, true);
					newEntries.add(newStringNode);
					this.stringConstants.add(newStringNode);
					stringListener.updateStringConstant(stringNode, newStringNode);
				}
			}

			ReadOnlyList<IntegerNode> sortedIntegerNodes = new ReadOnlyList<>(intRefs.keys());
			Collections.sort(sortedIntegerNodes);

			for (IntegerNode integerNode : sortedIntegerNodes) {
				List<IntegerConstants> integerListeners = intRefs.get(integerNode);
				int integerListenerCount = integerListeners.size();

				for (int integerListenerIndex = 1; integerListenerIndex < integerListenerCount; integerListenerIndex++) {
					IntegerConstants integerListener = integerListeners.get(integerListenerIndex);
					IntegerNode newIntegerNode = new IntegerNode(nextPoolIndex++, this, integerNode);
					newEntries.add(newIntegerNode);
					this.integerConstants.add(newIntegerNode);
					integerListener.updateIntegerConstant(integerNode, newIntegerNode);
				}
			}

			ReadOnlyList<LongNode> sortedLongNodes = new ReadOnlyList<>(longRefs.keys());
			Collections.sort(sortedLongNodes);
			NullTypeWriter nullSlotPlaceholder = this.nullSlotPlaceholders[0];

			for (LongNode longNode : sortedLongNodes) {
				List<LongConstants> longListeners = longRefs.get(longNode);
				int longListenerCount = longListeners.size();

				for (int longListenerIndex = 1; longListenerIndex < longListenerCount; longListenerIndex++) {
					LongConstants longListener = longListeners.get(longListenerIndex);
					LongNode newLongNode = new LongNode(nextPoolIndex++, this, longNode);
					newEntries.add(newLongNode);
					newEntries.add(nullSlotPlaceholder);
					nextPoolIndex++;
					this.longConstants.add(newLongNode);
					longListener.updateLongConstant(longNode, newLongNode);
				}
			}

			ReadOnlyList<InvokeDynamicCallWriter> sortedDynamicCalls = new ReadOnlyList<>(dynamicCallRefs.keys());
			Collections.sort(sortedDynamicCalls);

			for (InvokeDynamicCallWriter dynamicCall : sortedDynamicCalls) {
				dynamicCall.getBootstrapMethodEntry();
				List<IDynamicCallUpdatable> dynamicListeners = dynamicCallRefs.get(dynamicCall);
				int dynamicListenerCount = dynamicListeners.size();

				for (int dynamicListenerIndex = 1; dynamicListenerIndex < dynamicListenerCount; dynamicListenerIndex++) {
					IDynamicCallUpdatable dynamicListener = dynamicListeners.get(dynamicListenerIndex);
					NameAndTypeWriter dynamicNameAndType = dynamicCall.getTarget();
					UTFStringWriter dynamicNameUtf8 = dynamicNameAndType.getNameEntry().copy(nextPoolIndex++);
					newEntries.add(dynamicNameUtf8);
					this.utf8StringConstants.add(dynamicNameUtf8);
					UTFStringWriter dynamicDescriptorUtf8 = dynamicNameAndType.getDescriptorEntry().copy(nextPoolIndex++);
					newEntries.add(dynamicDescriptorUtf8);
					this.utf8StringConstants.add(dynamicDescriptorUtf8);
					NameAndTypeWriter newDynamicNameAndType = new NameAndTypeWriter(nextPoolIndex++, this, dynamicNameUtf8, dynamicDescriptorUtf8, utf8Refs);
					newEntries.add(newDynamicNameAndType);
					this.nameAndTypeConstants.add(newDynamicNameAndType);
					InvokeDynamicCallWriter newDynamicCall = new InvokeDynamicCallWriter(nextPoolIndex++, newDynamicNameAndType, dynamicCall);
					newEntries.add(newDynamicCall);
					this.dynamicCallConstants.add(newDynamicCall);
					dynamicListener.updateDynamicCall(dynamicCall, newDynamicCall);
				}
			}

			ReadOnlyList<NameAndTypeWriter> sortedNameAndTypes = new ReadOnlyList<>(nameTypeRefs.keys());
			Collections.sort(sortedNameAndTypes);

			for (NameAndTypeWriter nameAndType : sortedNameAndTypes) {
				List<AbstractNamedWriter> nameTypeListeners = nameTypeRefs.get(nameAndType);
				int nameTypeListenerCount = nameTypeListeners.size();

				for (int nameTypeListenerIndex = 1; nameTypeListenerIndex < nameTypeListenerCount; nameTypeListenerIndex++) {
					AbstractNamedWriter nameTypeListener = nameTypeListeners.get(nameTypeListenerIndex);
					UTFStringWriter nameUtf8 = nameAndType.getNameEntry().copy(nextPoolIndex++);
					newEntries.add(nameUtf8);
					this.utf8StringConstants.add(nameUtf8);
					UTFStringWriter descriptorUtf8 = nameAndType.getDescriptorEntry().copy(nextPoolIndex++);
					newEntries.add(descriptorUtf8);
					this.utf8StringConstants.add(descriptorUtf8);
					NameAndTypeWriter newNameAndType = new NameAndTypeWriter(nextPoolIndex++, this, nameUtf8, descriptorUtf8, utf8Refs);
					newEntries.add(newNameAndType);
					this.nameAndTypeConstants.add(newNameAndType);
					nameTypeListener.setTarget(newNameAndType);
				}
			}

			ReadOnlyList<UTFStringWriter> sortedUtf8Writers = new ReadOnlyList<>(utf8Refs.keys());
			Collections.sort(sortedUtf8Writers);

			for (UTFStringWriter utf8Writer : sortedUtf8Writers) {
				List<SimpleReferenceListener> utf8Listeners = utf8Refs.get(utf8Writer);
				int utf8ListenerCount = utf8Listeners.size();

				for (int utf8ListenerIndex = 1; utf8ListenerIndex < utf8ListenerCount; utf8ListenerIndex++) {
					SimpleReferenceListener utf8Listener = utf8Listeners.get(utf8ListenerIndex);
					UTFStringWriter newUtf8Writer = utf8Writer.copy(nextPoolIndex++);
					newEntries.add(newUtf8Writer);
					this.utf8StringConstants.add(newUtf8Writer);
					utf8Listener.updateSimpleNameReference(utf8Writer, newUtf8Writer);
				}
			}

			this.constantPoolEntries = newEntries.toArray(new TypeWriter[newEntries.size()]);
		}
	}

	public abstract void remapConstantNames(HashMap<String, String> classNameMappings, AbstractLogger logger);

	protected MethodCallWriter findMethodRef(String owner, String name, String desc) {
		for (MethodCallWriter methodRef : this.methodRefConstants) {
			if (methodRef.isTarget(owner, name, desc)) {
				return methodRef;
			}
		}

		return null;
	}

	public ClassInfoWriter getOrCreateClassInfo(String internalName, List<TypeWriter> entries) {
		for (ClassInfoWriter classInfo : this.classInfoConstants) {
			if (classInfo.hasInternalName(internalName)) {
				return classInfo;
			}
		}

		for (TypeWriter entry : entries) {
			if (entry.getTypeTag() == TypeTags.CLASS && ((ClassInfoWriter)entry).hasInternalName(internalName)) {
				return (ClassInfoWriter)entry;
			}
		}

		UTFStringWriter utf8StringWriter = new UTFStringWriter(0, this, internalName);
		ClassInfoWriter newClassInfo = new ClassInfoWriter(0, this, utf8StringWriter);
		entries.add(newClassInfo);
		entries.add(utf8StringWriter);
		return newClassInfo;
	}

	public abstract boolean shouldObfuscateConstants();

	public String getOwnerInternalName() {
		return this.ownerClassNode.getInternalName();
	}

	public final String getOwnerSourcePath() {
		return this.ownerClassNode.getSourcePath();
	}

	public String getOwnerPackageNameInternal() {
		return this.ownerClassNode.getPackageNameInternal();
	}

	protected MemberTypeWriter findMemberRefAmongNewEntries(String owner, String name, String desc, List<TypeWriter> entries) {
		for (TypeWriter entry : entries) {
			if (entry instanceof MemberTypeWriter) {
				MemberTypeWriter memberRef = (MemberTypeWriter)entry;
				if (memberRef.isTarget(owner, name, desc)) {
					return memberRef;
				}
			}
		}

		return null;
	}

	public final AbstractClassNode getOwnerClassNode() {
		return this.ownerClassNode;
	}

	public AbstractClassInfoWriter getOwnerThisClassWriter() {
		return this.ownerClassNode.getThisClassWriter();
	}

	public NameAndTypeWriter createNameAndType(String name, String desc, List<TypeWriter> entries) {
		UTFStringWriter nameUtf8 = new UTFStringWriter(0, this, name);
		UTFStringWriter descriptorUtf8 = new UTFStringWriter(0, this, desc);
		NameAndTypeWriter nameAndType = new NameAndTypeWriter(0, this, nameUtf8, descriptorUtf8, null);
		entries.add(nameAndType);
		entries.add(nameUtf8);
		entries.add(descriptorUtf8);
		return nameAndType;
	}

	private AbstractMethodCallWriter getOrCreateMethodRefInternal(String owner, String name, String desc, List<TypeWriter> entries, AbstractMethodNode methodNode) {
		boolean ownerIsInterface = methodNode.getClassNode().isInterface();
		AbstractMethodCallWriter existingMethodRef;
		if (ownerIsInterface) {
			existingMethodRef = this.findInterfaceMethodRef(owner, name, desc);
		} else {
			existingMethodRef = this.findMethodRef(owner, name, desc);
		}

		if (existingMethodRef != null) {
			return existingMethodRef;
		} else {
			existingMethodRef = (AbstractMethodCallWriter)this.findMemberRefAmongNewEntries(owner, name, desc, entries);
			if (existingMethodRef != null) {
				return existingMethodRef;
			} else {
				ClassInfoWriter classInfo = this.getOrCreateClassInfo(owner, entries);
				NameAndTypeWriter nameAndType = this.createNameAndType(name, desc, entries);
				AbstractMethodCallWriter newMethodRef;
				if (ownerIsInterface) {
					newMethodRef = new InterfaceMethodCallWriter(0, this, classInfo, nameAndType, methodNode);
				} else {
					newMethodRef = new MethodCallWriter(0, this, classInfo, nameAndType, methodNode);
				}

				entries.add(newMethodRef);
				return newMethodRef;
			}
		}
	}

	private void deduplicateMemberReferences(MultiMap<MemberTypeWriter, TypeNode> memberReferences) {
		for (MemberTypeWriter methodRef : this.methodRefConstants) {
			if (!memberReferences.containsKey(methodRef)) {
				memberReferences.putAll(methodRef, new ArrayList<>(0));
			}
		}

		for (MemberTypeWriter interfaceMethodRef : this.interfaceMethodRefConstants) {
			if (!memberReferences.containsKey(interfaceMethodRef)) {
				memberReferences.putAll(interfaceMethodRef, new ArrayList<>(0));
			}
		}

		for (MemberTypeWriter fieldRef : this.fieldRefConstants) {
			if (!memberReferences.containsKey(fieldRef)) {
				memberReferences.putAll(fieldRef, new ArrayList<>(0));
			}
		}

		HashSet<String> duplicateKeys = GlobalUtils.newHashSet();
		DualKeyMap<String, MemberTypeWriter, List<TypeNode>> referencesByKey = new DualKeyMap<>(memberReferences.size());

		for (Entry<MemberTypeWriter, List<TypeNode>> entry : memberReferences.entrySet()) {
			String key = entry.getKey().getAsString();
			if (referencesByKey.containsGroup(key)) {
				duplicateKeys.add(key);
			}

			referencesByKey.put(key, entry.getKey(), entry.getValue());
		}

		for (String duplicateKey : duplicateKeys) {
			Map<MemberTypeWriter, List<TypeNode>> referencesByMember = referencesByKey.getGroup(duplicateKey);
			TreeSet<MemberTypeWriter> sortedMembers = new TreeSet<>(referencesByMember.keySet());
			int memberIndex = 0;
			MemberTypeWriter primaryMember = null;

			for (MemberTypeWriter member : sortedMembers) {
				if (memberIndex++ == 0) {
					primaryMember = member;
				} else {
					for (TypeNode typeNode : referencesByMember.get(member)) {
						typeNode.updateMemberReference(member, primaryMember);
					}
				}
			}
		}

		referencesByKey.clearAll();
		duplicateKeys.clear();
	}

	public final void updateConstantPoolIndices() {
		int entryCount = this.constantPoolEntries.length;

		for (int i = 0; i < entryCount; i++) {
			this.constantPoolEntries[i].setTag(i);
		}
	}

	public final InterfaceMethodCallWriter getOrCreateInterfaceMethodRef(AbstractMethodNode methodNode, List<TypeWriter> entries) {
		return (InterfaceMethodCallWriter)this.getOrCreateMethodRefInternal(methodNode.getInternalName(), methodNode.getName(), methodNode.getDescriptor(), entries, methodNode);
	}

	public final void resolveFieldReferences(ScriptContext ctx, AbstractWorkspace workspace) {
		int fieldRefCount = this.fieldRefConstants.size();

		for (int i = 0; i < fieldRefCount; i++) {
			this.fieldRefConstants.get(i).updateTargetFromMember(ctx, workspace);
		}
	}

	public final FieldTypeWriter getOrCreateFieldRef(AbstractFieldNode fieldNode, List<TypeWriter> entries) {
		return this.getOrCreateFieldRef(fieldNode.getInternalName(), fieldNode.getNameDisplay(), fieldNode.getDescriptor(), entries, fieldNode);
	}

	public final void resolveMethodAndDynamicRefs(ScriptContext ctx, AbstractWorkspace workspace) {
		int refCount = this.methodRefConstants.size();

		for (int i = 0; i < refCount; i++) {
			this.methodRefConstants.get(i).updateTargetFromMember(ctx, workspace);
		}

		refCount = this.interfaceMethodRefConstants.size();

		for (int interfaceMethodIndex = 0; interfaceMethodIndex < refCount; interfaceMethodIndex++) {
			this.interfaceMethodRefConstants.get(interfaceMethodIndex).updateTargetFromMember(ctx, workspace);
		}

		refCount = this.dynamicCallConstants.size();

		for (int dynamicCallIndex = 0; dynamicCallIndex < refCount; dynamicCallIndex++) {
			this.dynamicCallConstants.get(dynamicCallIndex).remapCallSiteName(ctx);
		}
	}

	public abstract void remapPackageNames(HashMap<String, String> packageMappings);

	private <T extends ReferenceUpdateListener> void deduplicateClassReferences(MultiMap<ClassInfoWriter, T> classReferences) {
		for (ClassInfoWriter classInfoWriter : this.classInfoConstants) {
			if (!classReferences.containsKey(classInfoWriter)) {
				classReferences.putAll(classInfoWriter, new ArrayList<>(0));
			}
		}

		HashSet<String> duplicateKeys = GlobalUtils.newHashSet();
		DualKeyMap<String, ClassInfoWriter, List<T>> referencesByKey = new DualKeyMap<>(classReferences.size());

		for (Entry<ClassInfoWriter, List<T>> entry : classReferences.entrySet()) {
			String key = entry.getKey().getInternalName();
			if (referencesByKey.containsGroup(key)) {
				duplicateKeys.add(key);
			}

			referencesByKey.put(key, entry.getKey(), entry.getValue());
		}

		for (String duplicateKey : duplicateKeys) {
			Map<ClassInfoWriter, List<T>> referencesByClassInfo = referencesByKey.getGroup(duplicateKey);
			TreeSet<ClassInfoWriter> sortedClassInfos = new TreeSet<>(referencesByClassInfo.keySet());
			int classInfoIndex = 0;
			ClassInfoWriter primaryClassInfo = null;

			for (ClassInfoWriter classInfo : sortedClassInfos) {
				if (classInfoIndex++ == 0) {
					primaryClassInfo = classInfo;
				} else {
					for (ReferenceUpdateListener listener : referencesByClassInfo.get(classInfo)) {
						listener.updateClassReference(classInfo, primaryClassInfo);
					}
				}
			}
		}

		referencesByKey.clearAll();
		duplicateKeys.clear();
	}

	public final AbstractMethodCallWriter getOrCreateAnyMethodRef(AbstractMethodNode methodNode, List<TypeWriter> entries) {
		return this.getOrCreateMethodRefInternal(methodNode.getInternalName(), methodNode.getName(), methodNode.getDescriptor(), entries, methodNode);
	}

	public final int getConstantPoolSize() {
		return this.constantPoolEntries.length;
	}
}
