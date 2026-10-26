package com.zelix.framework.attributes.base;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.StructureNode;
import com.zelix.framework.annotation.AnnotationNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.repository.ClassProvider;
import com.zelix.tools.trim.TrimExcludes;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.PhantomClassManager;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class AbstractAnnotationsAttributeNode extends AbstractOwnedAttributeNode {
	public AnnotationNode[] annotations;

	public AbstractAnnotationsAttributeNode(
		StructureNode parent, int nameIndex, String attributeName, ByteDataBuffer buf, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References, PrintWriter printWriter, String logAttributeName
	) throws IOException {
		super(parent, nameIndex, attributeName, buf, utf8References);
		byte[] attributeBytes = new byte[this.length];
		buf.read(attributeBytes);
		ByteDataBuffer dataBuffer = ByteDataBuffer.fromBytes(attributeBytes, false);

		try {
			if (this.length >= 2) {
				int annotationCount = dataBuffer.readUnsignedShort();
				this.annotations = new AnnotationNode[annotationCount];

				for (int i = 0; i < annotationCount; i++) {
					this.annotations[i] = AnnotationNode.readAnnotationNode(this, dataBuffer, utf8References);
					if (!this.annotations[i].isValid()) {
						super.isParsedSuccessfully = false;
						super.rawData = attributeBytes;
						printWriter.println("ERROR: " + this.getRawSourcePath() + " : " + logAttributeName + " (C) " + this.annotations[i].getErrorMsg());
					}
				}
			} else {
				super.isParsedSuccessfully = false;
				super.rawData = attributeBytes;
				printWriter.println("ERROR: " + this.getRawSourcePath() + " : " + logAttributeName + " (D) : length=" + this.length);
			}
		} catch (IOException ioException) {
			super.isParsedSuccessfully = false;
			super.rawData = attributeBytes;
			printWriter.println("ERROR: " + this.getRawSourcePath() + " : " + logAttributeName + " is possibly corrupt : " + ioException);
		} finally {
			dataBuffer.close();
		}
	}

	@Override
	public void resolveAnnotations(AbstractWorkspace workspace, ClassProvider classProvider, PhantomClassManager phantomClassManager, ScriptContext scriptContext) {
		if (super.isParsedSuccessfully) {
			for (int i = 0; i < this.annotations.length; i++) {
				this.annotations[i].resolveTypeAndParams(classProvider, phantomClassManager, scriptContext);
			}
		}
	}

	@Override
	public void updateConstants() {
		if (super.isParsedSuccessfully) {
			for (int i = 0; i < this.annotations.length; i++) {
				this.annotations[i].updateConstants();
			}
		}
	}

	public AnnotationNode getAnnotationByType(String typeName, boolean useOriginalTypeName) {
		if (super.isParsedSuccessfully) {
			for (int i = 0; i < this.annotations.length; i++) {
				String annotationTypeName;
				if (useOriginalTypeName) {
					annotationTypeName = this.annotations[i].getOriginalTypeName();
				} else {
					annotationTypeName = this.annotations[i].getTypeInternalName();
				}

				if (typeName.equals(annotationTypeName)) {
					return this.annotations[i];
				}
			}
		}

		return null;
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		visitor.visit(this.attributeNameUtf8, this, this.getParent());
		if (super.isParsedSuccessfully) {
			for (int i = 0; i < this.annotations.length; i++) {
				this.annotations[i].acceptVisitor(visitor);
			}
		}
	}

	public int getAnnotationCount() {
		return this.annotations.length;
	}

	@Override
	public boolean trimExcludedAnnotations(TrimExcludes trimExcludes, ScriptContext scriptContext, PrintWriter printWriter) {
		if (super.isParsedSuccessfully) {
			boolean annotationsRemoved = false;
			boolean nothingRetained = true;
			ArrayList<AnnotationNode> retainedAnnotations = new ArrayList<>();

			for (int i = 0; i < this.annotations.length; i++) {
				AnnotationNode annotation = this.annotations[i];
				AbstractClassNode typeClass = annotation.getTypeClass();
				if (trimExcludes.isAnnotationTypeExcluded(typeClass)) {
					retainedAnnotations.add(annotation);
					nothingRetained = false;
					String message = "Retaining element in '"
						+ this.getAttributeName()
						+ "' attribute in "
						+ (this.isOwnedByClass() ? "" : "'" + this.getName() + "' in ")
						+ "class '"
						+ this.getSourcePath()
						+ "' because of annotation attribute exclusion for class '"
						+ typeClass.getSourceName()
						+ "'.";
					if (scriptContext.isVerbose()) {
						scriptContext.getWriter().println("\t" + message);
					}

					printWriter.println(message);
				} else {
					annotationsRemoved = true;
				}

				if (annotationsRemoved) {
					AnnotationNode[] annotationArray = new AnnotationNode[retainedAnnotations.size()];
					this.annotations = retainedAnnotations.toArray(annotationArray);
				}
			}

			return nothingRetained;
		} else {
			return false;
		}
	}

	@Override
	public void syncNames() {
		if (super.isParsedSuccessfully) {
			for (int i = 0; i < this.annotations.length; i++) {
				this.annotations[i].syncName();
			}
		}
	}

	public String[] getAnnotationTypeNames() {
		if (!super.isParsedSuccessfully) {
			return new String[0];
		} else {
			String[] typeNames = new String[this.annotations.length];

			for (int i = 0; i < this.annotations.length; i++) {
				typeNames[i] = this.annotations[i].getTypeInternalName();
			}

			return typeNames;
		}
	}

	@Override
	public void writeAttributeBody(DataOutputStream dataOutputStream) throws IOException {
		super.writeAttributeBody(dataOutputStream);
		if (super.isParsedSuccessfully) {
			dataOutputStream.writeShort(this.annotations.length);

			for (int i = 0; i < this.annotations.length; i++) {
				this.annotations[i].write(dataOutputStream);
			}
		} else {
			dataOutputStream.write(super.rawData);
		}
	}

	@Override
	public void collectClassReferences(Set<AbstractClassNode> classNodes) {
		if (super.isParsedSuccessfully) {
			for (int i = 0; i < this.annotations.length; i++) {
				this.annotations[i].collectClassReferences(classNodes);
			}
		}
	}

	@Override
	public void writeAttributeBodyRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap, ScriptContext scriptContext) throws IOException {
		super.writeAttributeBodyRemapped(dataOutputStream, writerMap, scriptContext);
		if (super.isParsedSuccessfully) {
			dataOutputStream.writeShort(this.annotations.length);

			for (int i = 0; i < this.annotations.length; i++) {
				this.annotations[i].writeRemapped(dataOutputStream, writerMap, scriptContext);
			}
		} else {
			dataOutputStream.write(super.rawData);
		}
	}

	@Override
	public int getAttributeLength() {
		if (!super.isParsedSuccessfully) {
			return super.rawData.length;
		} else {
			int attributeLength = 2;

			for (AnnotationNode annotation : this.annotations) {
				attributeLength += annotation.getSize();
			}

			this.length = attributeLength;
			return attributeLength;
		}
	}

	@Override
	public void remapDescriptors(int keepInnerClassInfo, int keepGenericsInformation, HashMap<String, String> packageRenameMap, HashMap<String, String> classNameMap) {
		if (super.isParsedSuccessfully) {
			for (int i = 0; i < this.annotations.length; i++) {
				this.annotations[i].remapDescriptors(packageRenameMap, classNameMap);
			}
		}
	}

	@Override
	public void collectAllReferences(Set<ClassNode> classReferences, Set<ClassNode> signatureClassReferences, Set<FieldNode> fieldReferences, Set<MethodNode> methodReferences) {
		if (super.isParsedSuccessfully) {
			for (int i = 0; i < this.annotations.length; i++) {
				this.annotations[i].collectWorkspaceRefs(classReferences, signatureClassReferences, fieldReferences, methodReferences);
			}
		}
	}
}
