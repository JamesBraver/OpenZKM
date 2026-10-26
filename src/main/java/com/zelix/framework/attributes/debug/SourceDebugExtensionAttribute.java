package com.zelix.framework.attributes.debug;

import com.zelix.framework.AttributesNode;
import com.zelix.framework.StructureNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.utils.StringUtils;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.AtomicBool;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class SourceDebugExtensionAttribute extends AttributesNode implements SimpleReferenceListener {
	private byte[] debugExtension = new byte[this.length];

	public SourceDebugExtensionAttribute(StructureNode parent, int nameIndex, String attributeName, ByteDataBuffer buf, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References) throws IOException {
		super(parent, nameIndex, attributeName, buf, utf8References);
		buf.read(this.debugExtension);
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		visitor.visit(this.attributeNameUtf8, this, this.getParent());
	}

	@Override
	public void writeAttributeBody(DataOutputStream dataOutputStream) throws IOException {
		super.writeAttributeBody(dataOutputStream);
		dataOutputStream.write(this.debugExtension);
	}

	@Override
	public void writeAttributeBodyRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap, ScriptContext scriptContext) throws IOException {
		super.writeAttributeBodyRemapped(dataOutputStream, writerMap, scriptContext);
		dataOutputStream.write(this.debugExtension);
	}

	@Override
	public int getAttributeLength() {
		return this.debugExtension.length;
	}

	@Override
	public void remapDescriptors(int keepInnerClassInfo, int keepGenericsInformation, HashMap<String, String> packageRenameMap, HashMap<String, String> classNameMap) {
		String smapText = new String(this.debugExtension);
		if ((Options.ZKM_TRANSLATE_KOTLIN || Options.translateKotlinMetadata) && smapText.startsWith("SMAP") && smapText.indexOf("Kotlin") > -1) {
			boolean remapPerformed = false;
			StringBuilder smapBuffer = new StringBuilder();
			StringTokenizer lineTokenizer = new StringTokenizer(smapText, "\n");
			AtomicBool descriptorRemapped = new AtomicBool();

			while (lineTokenizer.hasMoreTokens()) {
				String token = lineTokenizer.nextToken();
				String remappedToken = null;
				descriptorRemapped.set(false);
				if (StringUtils.countOccurrences(token, '/') >= 1 || this.getInternalName().equals(token)) {
					remappedToken = TypeWriter.tryRemapDescriptor(token, classNameMap, descriptorRemapped);
				}

				if (smapBuffer.length() > 0) {
					smapBuffer.append("\n");
				}

				if (descriptorRemapped.get() && remappedToken != null && !remappedToken.equals(token)) {
					remapPerformed = true;
					smapBuffer.append(remappedToken);
				} else {
					smapBuffer.append(token);
				}
			}

			if (remapPerformed) {
				this.debugExtension = smapBuffer.toString().getBytes();
			}
		}
	}
}
