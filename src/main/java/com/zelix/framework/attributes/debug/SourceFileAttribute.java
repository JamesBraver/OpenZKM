package com.zelix.framework.attributes.debug;

import com.zelix.exceptions.ClassParseFailedException;
import com.zelix.framework.AttributesNode;
import com.zelix.framework.StructureNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

public class SourceFileAttribute extends AttributesNode implements SimpleReferenceListener {
	public UTFStringWriter sourceFileName;

	public SourceFileAttribute(StructureNode parent, int nameIndex, String attributeName, ByteDataBuffer buf, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References) throws IOException {
		super(parent, nameIndex, attributeName, buf, utf8References);
		int sourceFileIndex = buf.readUnsignedShort();
		TypeWriter sourceFileWriter = this.getConstantPoolEntry(sourceFileIndex);
		this.setSourceFileName(sourceFileWriter);
		utf8References.put(this.sourceFileName, this);
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		visitor.visit(this.attributeNameUtf8, this, this.getParent());
		this.sourceFileName.accept(visitor, this, this.getParent());
	}

	@Override
	public void writeAttributeBody(DataOutputStream dataOutputStream) throws IOException {
		super.writeAttributeBody(dataOutputStream);
		dataOutputStream.writeShort(this.sourceFileName.getTag());
	}

	public void setSourceFileName(TypeWriter sourceFileNameWriter) {
		if (!(sourceFileNameWriter instanceof UTFStringWriter)) {
			throw new ClassParseFailedException(
				"Class name '"
					+ this.getInternalName()
					+ "' : Invalid SourceFile Attribute : "
					+ (sourceFileNameWriter != null ? sourceFileNameWriter.getTypeTag() + " " + sourceFileNameWriter.getTag() + " " + sourceFileNameWriter.getClass().getName() : "")
			);
		} else {
			this.sourceFileName = (UTFStringWriter)sourceFileNameWriter;
		}
	}

	@Override
	public void updateSimpleNameReference(UTFStringWriter oldReference, UTFStringWriter newReference) {
		if (this.sourceFileName == oldReference) {
			this.sourceFileName = newReference;
		} else {
			super.updateSimpleNameReference(oldReference, newReference);
		}
	}

	@Override
	public void writeAttributeBodyRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap, ScriptContext scriptContext) throws IOException {
		super.writeAttributeBodyRemapped(dataOutputStream, writerMap, scriptContext);
		UTFStringWriter remappedSourceFileName = (UTFStringWriter)writerMap.get(this.sourceFileName);
		if (remappedSourceFileName != null) {
			dataOutputStream.writeShort(remappedSourceFileName.getTag());
		} else {
			dataOutputStream.writeShort(this.sourceFileName.getTag());
		}
	}
}
