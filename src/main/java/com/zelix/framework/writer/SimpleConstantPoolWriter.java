package com.zelix.framework.writer;

import com.zelix.enums.TypeTags;
import com.zelix.exceptions.ClassParseFailedException;
import com.zelix.exceptions.LookupException;
import com.zelix.framework.TypeNode;
import com.zelix.framework.constant.IntegerNode;
import com.zelix.framework.constant.StringNode;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.wrappers.LibraryClassNode;
import com.zelix.framework.writer.base.AbstractConstantPoolWriter;
import com.zelix.framework.writer.base.MemberTypeWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.logging.AbstractLogger;
import com.zelix.repository.ClassProvider;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.PhantomClassManager;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

public class SimpleConstantPoolWriter extends AbstractConstantPoolWriter {
	public SimpleConstantPoolWriter(
		ByteDataBuffer dataBuffer,
		LibraryClassNode ownerClassNode,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<NameAndTypeWriter, AbstractNamedWriter> nameAndTypeReferences,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		MultiMap<MemberTypeWriter, TypeNode> memberReferences
	) throws IOException {
		super(ownerClassNode);
		int constantPoolCount = dataBuffer.readUnsignedShort();
		ArrayList<TypeWriter> rawClassEntries = new ArrayList<>();
		ArrayList<TypeWriter> nameAndTypeEntries = new ArrayList<>();
		ArrayList<TypeWriter> otherEntries = new ArrayList<>();
		this.constantPoolEntries = new TypeWriter[constantPoolCount];
		this.utf8StringConstants = new ArrayList<>(Math.max(5, (int)(constantPoolCount * 1.5)));
		this.nameAndTypeConstants = new ArrayList<>(Math.max(5, (int)(constantPoolCount * 0.2)));
		this.classInfoConstants = new ArrayList<>(Math.max(5, (int)(constantPoolCount * 0.09)));
		super.classInfoByNameCache = GlobalUtils.newHashMap(Math.max(5, (int)(constantPoolCount * 0.09)));
		this.stringConstants = new ArrayList<>(Math.max(5, (int)(constantPoolCount * 0.05)));
		super.integerConstants = new ArrayList<>(Math.max(5, (int)(constantPoolCount * 0.01)));
		this.methodRefConstants = new ArrayList<>(Math.max(5, (int)(constantPoolCount * 0.15)));
		super.interfaceMethodRefConstants = new ArrayList<>(Math.max(5, (int)(constantPoolCount * 0.15)));
		this.fieldRefConstants = new ArrayList<>(Math.max(5, (int)(constantPoolCount * 0.15)));
		this.constantPoolEntries[0] = NullTypeWriter.getNullSlotPlaceholder(this, 1);
		int entryIndex = 1;

		while (entryIndex < constantPoolCount) {
			TypeWriter entry = TypeWriter.readConstantPoolEntry(entryIndex, dataBuffer, this);
			if (entry.isResolvable()) {
				if (entry instanceof RawClassInfoWriter) {
					rawClassEntries.add(entry);
				} else if (entry instanceof UnresolvedNameAndTypeEntry) {
					nameAndTypeEntries.add(entry);
				} else {
					otherEntries.add(entry);
				}
			}

			this.constantPoolEntries[entryIndex++] = entry;
			if (entry.getSlotSize() == 2) {
				this.constantPoolEntries[entryIndex++] = NullTypeWriter.getNullSlotPlaceholder(this, 1);
			}
		}

		this.resolveEntries(rawClassEntries, utf8References, nameAndTypeReferences, classReferences, memberReferences);
		this.resolveEntries(nameAndTypeEntries, utf8References, nameAndTypeReferences, classReferences, memberReferences);
		this.resolveEntries(otherEntries, utf8References, nameAndTypeReferences, classReferences, memberReferences);
	}

	@Override
	public void remapTargetClassName(String newClassName) {
		String ownerInternalName = this.getOwnerInternalName();

		for (TypeWriter entry : this.constantPoolEntries) {
			if (entry instanceof ClassInfoWriter) {
				ClassInfoWriter classInfoWriter = (ClassInfoWriter)entry;
				if (classInfoWriter.getInternalName().equals(ownerInternalName)) {
					classInfoWriter.setName(newClassName);
				}
			}
		}
	}

	private void resolveEntries(
		List<TypeWriter> unresolvedEntries,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<NameAndTypeWriter, AbstractNamedWriter> nameAndTypeReferences,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		MultiMap<MemberTypeWriter, TypeNode> memberReferences
	) {
		int attemptCount = 0;

		boolean unresolvedFound;
		do {
			unresolvedFound = false;
			ListIterator<TypeWriter> entryIterator = unresolvedEntries.listIterator();

			while (entryIterator.hasNext()) {
				TypeWriter entry = entryIterator.next();
				StringWriter messageBuffer = new StringWriter();
				PrintWriter messageWriter = new PrintWriter(messageBuffer);
				TypeWriter resolvedEntry = ((Writer)entry).resolve(utf8References, nameAndTypeReferences, classReferences, memberReferences, messageWriter);
				if (resolvedEntry == null) {
					unresolvedFound = true;
				} else {
					entryIterator.remove();
					this.constantPoolEntries[entry.getTag()] = resolvedEntry;
					if (!resolvedEntry.isResolutionValid()) {
						throw new LookupException(messageBuffer.toString());
					}

					this.addConstantToTypeLists(resolvedEntry);
				}
			}

			if (++attemptCount > 3) {
				throw new ClassParseFailedException(this.getOwnerSourcePath() + " :  Unknown problem validating constant pool (B).");
			}
		} while (unresolvedFound);
	}

	@Override
	public synchronized int appendEntriesToPool(List<TypeWriter> entries) {
		int poolSize = super.appendEntriesToPool(entries);
		int newEntryCount = entries.size();

		for (int i = 0; i < newEntryCount; i++) {
			this.addConstantToTypeLists(entries.get(i));
		}

		return poolSize;
	}

	@Override
	public final void remapConstantNames(HashMap<String, String> classNameMappings, AbstractLogger logger) {
		for (int i = 1; i < this.constantPoolEntries.length; i++) {
			if (!(this.constantPoolEntries[i] instanceof NullTypeWriter)) {
				if (this.constantPoolEntries[i] instanceof ClassInfoWriter) {
					((ClassInfoWriter)this.constantPoolEntries[i]).remapName(classNameMappings);
				} else if (this.constantPoolEntries[i] instanceof NameAndTypeWriter) {
					((NameAndTypeWriter)this.constantPoolEntries[i]).remapDescriptor(classNameMappings);
				} else if (this.constantPoolEntries[i] instanceof MethodDescriptorWriter) {
					((MethodDescriptorWriter)this.constantPoolEntries[i]).remapDescriptor(classNameMappings);
				}
			}
		}
	}

	public void resolveMemberReferences(AbstractWorkspace workspace, ClassProvider classProvider, PhantomClassManager phantomClassManager) {
		for (int i = 1; i < this.constantPoolEntries.length; i++) {
			if (this.constantPoolEntries[i] instanceof MemberTypeWriter) {
				((MemberTypeWriter)this.constantPoolEntries[i]).resolveTargetMember(workspace, classProvider, phantomClassManager);
			}
		}
	}

	@Override
	public boolean shouldObfuscateConstants() {
		return false;
	}

	private void addConstantToTypeLists(TypeWriter constant) {
		TypeTags typeTag = constant.getTypeTag();
		switch (typeTag) {
			case CLASS:
				this.classInfoConstants.add((ClassInfoWriter)constant);
				super.classInfoByNameCache.put(((ClassInfoWriter)constant).getInternalName(), (ClassInfoWriter)constant);
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
			case LONG:
			case FLOAT:
			case DOUBLE:
			case METHOD_HANDLE:
			case METHOD_TYPE:
			default:
				break;
			case INVOKE_DYNAMIC:
				super.dynamicCallConstants.add((InvokeDynamicCallWriter)constant);
		}
	}

	@Override
	public void remapPackageNames(HashMap<String, String> packageMappings) {
		for (int i = 1; i < this.constantPoolEntries.length; i++) {
			if (this.constantPoolEntries[i] instanceof PackageNameWriter) {
				((PackageNameWriter)this.constantPoolEntries[i]).remapName(packageMappings);
			}
		}
	}
}
