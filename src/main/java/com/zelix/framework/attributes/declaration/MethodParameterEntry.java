package com.zelix.framework.attributes.declaration;

import com.zelix.framework.StructureNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.AbstractConstantPoolWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class MethodParameterEntry extends StructureNode implements SimpleReferenceListener {
	public boolean isValid = true;
	private int accessFlags;
	private UTFStringWriter nameWriter;
	public String errorMessage;

	public MethodParameterEntry(StructureNode parent, ByteDataBuffer buf, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References) throws IOException {
		super(parent);
		int nameIndex = buf.readUnsignedShort();
		this.accessFlags = buf.readUnsignedShort();
		if (nameIndex != 0) {
			TypeWriter nameEntry = this.getConstantPoolEntry(nameIndex);
			if (nameEntry instanceof UTFStringWriter) {
				this.nameWriter = (UTFStringWriter)nameEntry;
				utf8References.put(this.nameWriter, this);
			} else {
				this.isValid = false;
				this.errorMessage = "Invalid MethodParameters nameEntry '" + TypeWriter.getConstantPoolTagName(nameEntry.getTypeTag()) + "'";
			}
		}
	}

	public MethodParameterEntry(MethodParametersAttribute parent, AbstractConstantPoolWriter constantPoolWriter, List<TypeWriter> cpEntries) {
		super(parent);
		UTFStringWriter newNameWriter = constantPoolWriter.createUtf8String("a", cpEntries);
		this.nameWriter = newNameWriter;
	}

	protected void writeToStream(DataOutputStream dataOutputStream) throws IOException {
		if (this.nameWriter != null) {
			dataOutputStream.writeShort(this.nameWriter.getTag());
		} else {
			dataOutputStream.writeShort(0);
		}

		dataOutputStream.writeShort(this.accessFlags);
	}

	public boolean isValid() {
		return this.isValid;
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		if (this.nameWriter != null) {
			visitor.visit(this.nameWriter, this, this.getParent());
		}
	}

	protected void writeToStreamRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap, ScriptContext scriptContext) throws IOException {
		if (this.nameWriter != null) {
			TypeWriter remappedName = writerMap.get(this.nameWriter);
			if (remappedName != null) {
				dataOutputStream.writeShort(remappedName.getTag());
			} else {
				dataOutputStream.writeShort(this.nameWriter.getTag());
			}
		} else {
			dataOutputStream.writeShort(0);
		}

		dataOutputStream.writeShort(this.accessFlags);
	}

	public String getErrorMessage() {
		return this.errorMessage;
	}

	public void mangleName(int parameterIndex) {
		if (this.nameWriter != null) {
			if (Options.ZKM_DEBUG_SHOW_OLD_NAMES) {
				String mangledName = this.nameWriter.getValue() + '_' + "a";
				this.nameWriter.setValue(mangledName);
			} else {
				this.nameWriter.setValue("a");
			}
		}
	}

	@Override
	public void updateSimpleNameReference(UTFStringWriter oldReference, UTFStringWriter newReference) {
		if (this.nameWriter != null && this.nameWriter == oldReference) {
			this.nameWriter = newReference;
		}
	}

	public int getEntrySizeInBytes() {
		return 4;
	}
}
