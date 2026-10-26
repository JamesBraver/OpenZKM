package com.zelix.framework.attributes.base;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.StructureNode;
import com.zelix.framework.annotation.AnnotationTypeNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.instructions.LabelNode;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.reference.IGraphElement;
import com.zelix.framework.reference.ReferenceGraph;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class AbstractTypeAnnotationsAttributeNode extends AbstractOwnedAttributeNode {
	public AnnotationTypeNode[] typeAnnotations;

	public AbstractTypeAnnotationsAttributeNode(
		StructureNode parent,
		int nameIndex,
		String attributeName,
		ByteDataBuffer buf,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<Integer, IGraphElement> elementsByOffset,
		PrintWriter printWriter,
		String attributeLabel
	) throws IOException {
		super(parent, nameIndex, attributeName, buf, utf8References);
		byte[] attributeBytes = new byte[this.length];
		buf.read(attributeBytes);
		ByteDataBuffer dataBuffer = ByteDataBuffer.fromBytes(attributeBytes, false);

		try {
			if (this.length >= 2) {
				int annotationCount = dataBuffer.readUnsignedShort();
				this.typeAnnotations = new AnnotationTypeNode[annotationCount];

				for (int i = 0; i < annotationCount; i++) {
					this.typeAnnotations[i] = new AnnotationTypeNode(this, dataBuffer, utf8References, elementsByOffset);
					if (!this.typeAnnotations[i].isValid()) {
						super.isParsedSuccessfully = false;
						super.rawData = attributeBytes;
						printWriter.println("ERROR: " + this.getRawSourcePath() + " : " + attributeLabel + " (E) " + this.typeAnnotations[i].getErrorMessage());
					}
				}
			} else {
				super.isParsedSuccessfully = false;
				super.rawData = attributeBytes;
				printWriter.println("ERROR: " + this.getRawSourcePath() + " : " + attributeLabel + " (F) : length=" + this.length);
			}
		} catch (IOException ioException) {
			super.isParsedSuccessfully = false;
			super.rawData = attributeBytes;
			printWriter.println("ERROR: " + this.getRawSourcePath() + " : " + attributeLabel + " is possibly corrupt : " + ioException);
		} finally {
			dataBuffer.close();
		}
	}

	@Override
	public void resolveAnnotations(AbstractWorkspace workspace, ClassProvider classProvider, PhantomClassManager phantomClassManager, ScriptContext scriptContext) {
		if (super.isParsedSuccessfully) {
			for (int i = 0; i < this.typeAnnotations.length; i++) {
				this.typeAnnotations[i].resolveReferences(classProvider, phantomClassManager, scriptContext);
			}
		}
	}

	public boolean removeSignatureAnnotations() {
		boolean annotationsRemoved = false;
		ArrayList<AnnotationTypeNode> remainingAnnotations = new ArrayList<>(this.typeAnnotations.length);

		for (AnnotationTypeNode annotation : this.typeAnnotations) {
			if (!annotation.isTypeDependent()) {
				remainingAnnotations.add(annotation);
			} else {
				annotationsRemoved = true;
			}
		}

		if (remainingAnnotations.size() < this.typeAnnotations.length) {
			this.typeAnnotations = remainingAnnotations.toArray(new AnnotationTypeNode[remainingAnnotations.size()]);
		}

		return annotationsRemoved;
	}

	@Override
	public void updateConstants() {
		if (super.isParsedSuccessfully) {
			for (int i = 0; i < this.typeAnnotations.length; i++) {
				this.typeAnnotations[i].updateConstants();
			}
		}
	}

	public boolean isEmpty() {
		return this.typeAnnotations.length == 0;
	}

	public void removeAnnotationsForLabels(HashSet<LabelNode> deadLabels, ReferenceGraph<LabelNode, IGraphElement> graph) {
		ArrayList<AnnotationTypeNode> remainingAnnotations = new ArrayList<>(this.typeAnnotations.length);

		for (AnnotationTypeNode annotation : this.typeAnnotations) {
			if (!annotation.removeInGraph(deadLabels, graph)) {
				remainingAnnotations.add(annotation);
			}
		}

		if (remainingAnnotations.size() < this.typeAnnotations.length) {
			this.typeAnnotations = remainingAnnotations.toArray(new AnnotationTypeNode[remainingAnnotations.size()]);
		}
	}

	public int removeThrowsAnnotations() {
		ArrayList<AnnotationTypeNode> remainingAnnotations = new ArrayList<>(this.typeAnnotations.length);

		for (AnnotationTypeNode annotation : this.typeAnnotations) {
			if (!annotation.isThrowsTarget()) {
				remainingAnnotations.add(annotation);
			}
		}

		if (remainingAnnotations.size() < this.typeAnnotations.length) {
			this.typeAnnotations = remainingAnnotations.toArray(new AnnotationTypeNode[remainingAnnotations.size()]);
		}

		return this.typeAnnotations.length;
	}

	@Override
	public final void acceptVisitor(ConstantPoolVisitor visitor) {
		visitor.visit(this.attributeNameUtf8, this, this.getParent());
		if (super.isParsedSuccessfully) {
			for (int i = 0; i < this.typeAnnotations.length; i++) {
				this.typeAnnotations[i].acceptVisitor(visitor);
			}
		}
	}

	@Override
	public boolean trimExcludedAnnotations(TrimExcludes trimExcludes, ScriptContext scriptContext, PrintWriter printWriter) {
		if (super.isParsedSuccessfully) {
			boolean annotationsRemoved = false;
			boolean nothingRetained = true;
			ArrayList<AnnotationTypeNode> retainedAnnotations = new ArrayList<>();

			for (int i = 0; i < this.typeAnnotations.length; i++) {
				AnnotationTypeNode annotation = this.typeAnnotations[i];
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
						+ annotation.getSourceName()
						+ "'.";
					if (scriptContext.isVerbose()) {
						scriptContext.getWriter().println("\t" + message);
					}

					printWriter.println(message);
				} else {
					annotationsRemoved = true;
				}

				if (annotationsRemoved) {
					AnnotationTypeNode[] annotationArray = new AnnotationTypeNode[retainedAnnotations.size()];
					this.typeAnnotations = retainedAnnotations.toArray(annotationArray);
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
			for (int i = 0; i < this.typeAnnotations.length; i++) {
				this.typeAnnotations[i].syncName();
			}
		}
	}

	@Override
	public void writeAttributeBody(DataOutputStream dataOutputStream) throws IOException {
		super.writeAttributeBody(dataOutputStream);
		if (super.isParsedSuccessfully) {
			dataOutputStream.writeShort(this.typeAnnotations.length);

			for (int i = 0; i < this.typeAnnotations.length; i++) {
				this.typeAnnotations[i].write(dataOutputStream);
			}
		} else {
			dataOutputStream.write(super.rawData);
		}
	}

	@Override
	public void collectClassReferences(Set<AbstractClassNode> classNodes) {
		if (super.isParsedSuccessfully) {
			for (int i = 0; i < this.typeAnnotations.length; i++) {
				this.typeAnnotations[i].collectClassReferences(classNodes);
			}
		}
	}

	public void registerAnnotations(ReferenceGraph<LabelNode, IGraphElement> graph) {
		AnnotationTypeNode[] annotations = this.typeAnnotations;
		int annotationCount = annotations.length;

		for (int i = 0; i < annotationCount; i++) {
			annotations[i].registerToGraph(graph);
		}
	}

	@Override
	public void writeAttributeBodyRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap, ScriptContext scriptContext) throws IOException {
		super.writeAttributeBodyRemapped(dataOutputStream, writerMap, scriptContext);
		if (super.isParsedSuccessfully) {
			dataOutputStream.writeShort(this.typeAnnotations.length);

			for (int i = 0; i < this.typeAnnotations.length; i++) {
				this.typeAnnotations[i].writeRemapped(dataOutputStream, writerMap, scriptContext);
			}
		} else {
			dataOutputStream.write(super.rawData);
		}
	}

	@Override
	public int getAttributeLength() {
		int attributeLength = 2;

		for (AnnotationTypeNode annotation : this.typeAnnotations) {
			attributeLength += annotation.getSize();
		}

		this.length = attributeLength;
		return attributeLength;
	}

	@Override
	public final void remapDescriptors(int keepInnerClassInfo, int keepGenericsInformation, HashMap<String, String> packageRenameMap, HashMap<String, String> classNameMap) {
		if (super.isParsedSuccessfully) {
			for (int i = 0; i < this.typeAnnotations.length; i++) {
				this.typeAnnotations[i].remapDescriptors(packageRenameMap, classNameMap);
			}
		}
	}

	@Override
	public void collectAllReferences(Set<ClassNode> classReferences, Set<ClassNode> signatureClassReferences, Set<FieldNode> fieldReferences, Set<MethodNode> methodReferences) {
		if (super.isParsedSuccessfully) {
			for (int i = 0; i < this.typeAnnotations.length; i++) {
				this.typeAnnotations[i].collectAllReferences(classReferences, signatureClassReferences, fieldReferences, methodReferences);
			}
		}
	}
}
