package com.zelix.framework.attributes;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.StructureNode;
import com.zelix.framework.analyzer.StringConcatAnalysis;
import com.zelix.framework.attributes.base.AbstractAttributeNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.constant.IntegerNode;
import com.zelix.framework.constant.LongNode;
import com.zelix.framework.constant.StringNode;
import com.zelix.framework.listener.IntegerConstants;
import com.zelix.framework.listener.LongConstants;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.listener.StringConstantUpdateListener;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.ResolvedMethodHandleWriter;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BootstrapMethodsAttribute extends AbstractAttributeNode {
	public BootstrapMethodEntry[] bootstrapMethods;

	public BootstrapMethodsAttribute(
		StructureNode parent,
		int nameIndex,
		String attributeName,
		ByteDataBuffer buf,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<StringNode, StringConstantUpdateListener> stringReferences,
		MultiMap<IntegerNode, IntegerConstants> integerReferences,
		MultiMap<LongNode, LongConstants> longReferences,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		PrintWriter printWriter
	) throws IOException {
		super(parent, nameIndex, attributeName, buf, utf8References);
		byte[] rawAttributeBytes = new byte[this.length];
		buf.read(rawAttributeBytes);
		ByteDataBuffer bodyBuffer = ByteDataBuffer.fromBytes(rawAttributeBytes, false);

		try {
			if (this.length >= 2) {
				int entryCount = bodyBuffer.readUnsignedShort();
				this.bootstrapMethods = new BootstrapMethodEntry[entryCount];

				for (int entryIndex = 0; entryIndex < entryCount; entryIndex++) {
					this.bootstrapMethods[entryIndex] = new BootstrapMethodEntry(this, bodyBuffer, entryIndex, stringReferences, integerReferences, longReferences, classReferences, printWriter);
					if (!this.bootstrapMethods[entryIndex].isValid()) {
						super.isParsedSuccessfully = false;
						super.rawData = rawAttributeBytes;
						printWriter.println("ERROR: " + this.getSourcePath() + " : " + this.getAttributeName() + " (C) " + this.bootstrapMethods[entryIndex].getErrorMessage());
					}
				}
			} else {
				super.isParsedSuccessfully = false;
				super.rawData = rawAttributeBytes;
				printWriter.println("ERROR: " + this.getSourcePath() + " : " + this.getAttributeName() + " : length=" + this.length);
			}
		} catch (IOException ioException) {
			super.isParsedSuccessfully = false;
			super.rawData = rawAttributeBytes;
			printWriter.println("ERROR: " + this.getSourcePath() + " : " + this.getAttributeName() + " is possibly corrupt : " + ioException);
		} finally {
			bodyBuffer.close();
		}
	}

	public BootstrapMethodsAttribute(AbstractClassNode parent, UTFStringWriter nameUtf8) {
		super(parent, nameUtf8, 2);
		this.bootstrapMethods = new BootstrapMethodEntry[0];
	}

	public void collectMethodHandleRefs(Set<MethodNode> methodReferences) {
		if (super.isParsedSuccessfully) {
			BootstrapMethodEntry[] entries = this.bootstrapMethods;
			int entryCount = entries.length;

			for (int i = 0; i < entryCount; i++) {
				entries[i].collectMethodHandleRefs(methodReferences);
			}
		}
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		visitor.visit(this.attributeNameUtf8, this, this.getParent());

		for (int i = 0; i < this.bootstrapMethods.length; i++) {
			this.bootstrapMethods[i].acceptVisitor(visitor);
		}
	}

	public void reindexEntries() {
		for (int entryIndex = 0; entryIndex < this.bootstrapMethods.length; entryIndex++) {
			this.bootstrapMethods[entryIndex].setEntryIndex(entryIndex);
		}
	}

	@Override
	public void writeAttributeBody(DataOutputStream dataOutputStream) throws IOException {
		super.writeAttributeBody(dataOutputStream);
		if (super.isParsedSuccessfully) {
			dataOutputStream.writeShort(this.bootstrapMethods.length);
			BootstrapMethodEntry[] entries = this.bootstrapMethods;
			int entryCount = entries.length;

			for (int i = 0; i < entryCount; i++) {
				entries[i].writeToStream(dataOutputStream);
			}
		} else {
			dataOutputStream.write(super.rawData);
		}
	}

	public BootstrapMethodEntry getEntry(int index) {
		if (index >= 0 && index < this.bootstrapMethods.length) {
			return this.bootstrapMethods[index];
		} else {
			throw new IllegalArgumentException(
				"Index out of bounds in '"
					+ this.getAttributesNode().getAttributeName()
					+ "' attribute in class '"
					+ this.getSourcePath()
					+ "' : "
					+ index
					+ ">"
					+ (this.bootstrapMethods.length - 1)
			);
		}
	}

	@Override
	public void writeAttributeBodyRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap, ScriptContext scriptContext) throws IOException {
		super.writeAttributeBodyRemapped(dataOutputStream, writerMap, scriptContext);
		if (super.isParsedSuccessfully) {
			dataOutputStream.writeShort(this.bootstrapMethods.length);
			BootstrapMethodEntry[] entries = this.bootstrapMethods;
			int entryCount = entries.length;

			for (int i = 0; i < entryCount; i++) {
				entries[i].writeToStreamRemapped(dataOutputStream, writerMap, scriptContext);
			}
		} else {
			dataOutputStream.write(super.rawData);
		}
	}

	public void filterEntries(Set<BootstrapMethodEntry> retainedEntries) {
		ArrayList<BootstrapMethodEntry> filteredEntries = new ArrayList<>(this.bootstrapMethods.length);

		for (BootstrapMethodEntry entry : this.bootstrapMethods) {
			if (retainedEntries.contains(entry)) {
				filteredEntries.add(entry);
			}
		}

		if (filteredEntries.size() < this.bootstrapMethods.length) {
			this.bootstrapMethods = filteredEntries.toArray(new BootstrapMethodEntry[filteredEntries.size()]);
			this.length = this.getAttributeLength();
		}
	}

	public BootstrapMethodEntry addEntry(ResolvedMethodHandleWriter bootstrapMethodHandle, TypeWriter[] bootstrapArguments) {
		BootstrapMethodEntry[] newEntries = new BootstrapMethodEntry[this.bootstrapMethods.length + 1];
		System.arraycopy(this.bootstrapMethods, 0, newEntries, 0, this.bootstrapMethods.length);
		BootstrapMethodEntry newEntry = new BootstrapMethodEntry(this, bootstrapMethodHandle, bootstrapArguments, this.bootstrapMethods.length);
		newEntries[this.bootstrapMethods.length] = newEntry;
		this.bootstrapMethods = newEntries;
		this.length = this.getAttributeLength();
		return newEntry;
	}

	@Override
	public int getAttributeLength() {
		int attributeLength = 2;

		for (BootstrapMethodEntry entry : this.bootstrapMethods) {
			attributeLength += entry.getEntrySizeInBytes();
		}

		this.length = attributeLength;
		return attributeLength;
	}

	public void processRecordObjectMethods() {
		if (super.isParsedSuccessfully) {
			AbstractClassNode classNode = this.getClassNode();
			if (classNode.getSuperclassName().equals("java/lang/Record")) {
				for (BootstrapMethodEntry entry : this.bootstrapMethods) {
					entry.updateRecordComponentNames(classNode);
				}
			}
		}
	}

	public boolean isEmpty() {
		return this.bootstrapMethods.length == 0;
	}

	public StringConcatAnalysis analyzeStringConcatFactories() {
		int entryCount = this.bootstrapMethods.length;
		StringConcatAnalysis analysis = new StringConcatAnalysis((ClassNode)this.getClassNode(), this);

		for (int entryIndex = 0; entryIndex < entryCount; entryIndex++) {
			BootstrapMethodEntry entry = this.bootstrapMethods[entryIndex];
			if (entry.isBootstrapMethodStatic()
				&& entry.matchesMethodHandle(
					"java/lang/invoke/StringConcatFactory",
					"makeConcatWithConstants",
					"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;"
				)) {
				TypeWriter[] arguments = entry.getBootstrapArguments();
				TypeWriter recipeArgument = arguments[0];
				if (recipeArgument instanceof StringNode) {
					String recipe = ((StringNode)recipeArgument).getDisplayValue();
					AtomicInt argumentCount = new AtomicInt();
					List<String> recipeParts = StringConcatAnalysis.parseRecipe(recipe, argumentCount);
					if (recipeParts != null) {
						boolean encryptableStringFound = false;
						Iterator<String> partIterator = recipeParts.iterator();

						while (partIterator.hasNext()) {
							if (partIterator.next().length() >= 2) {
								encryptableStringFound = true;
								break;
							}
						}

						boolean nonStringArgumentFound = false;
						if (arguments.length > 1) {
							for (int argumentIndex = 1; argumentIndex < arguments.length; argumentIndex++) {
								if (!(arguments[argumentIndex] instanceof StringNode)) {
									nonStringArgumentFound = true;
									break;
								}

								if (!encryptableStringFound && ((StringNode)arguments[argumentIndex]).getValueAsString().length() >= 2) {
									encryptableStringFound = true;
								}
							}
						}

						if (encryptableStringFound && !nonStringArgumentFound) {
							analysis.addAnalysisResult(entry, recipeParts, argumentCount.get());
						}
					}
				}
			}
		}

		if (!analysis.isEmpty()) {
			analysis.prepareTransformations();
		}

		return analysis;
	}
}
