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
import com.zelix.tools.parameterChanger.ParameterPattern;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractParameterAnnotationsAttributeNode extends AbstractOwnedAttributeNode {
	public AnnotationNode[][] parameterAnnotations;
	public AnnotationNode[][] originalParameterAnnotations;

	public AbstractParameterAnnotationsAttributeNode(
		StructureNode parent, int nameIndex, String attributeName, ByteDataBuffer buf, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References, PrintWriter printWriter, String attributeLabel
	) throws IOException {
		super(parent, nameIndex, attributeName, buf, utf8References);
		byte[] attributeBytes = new byte[this.length];
		buf.read(attributeBytes);
		ByteDataBuffer attributeBuffer = ByteDataBuffer.fromBytes(attributeBytes, false);

		try {
			if (this.length < 1) {
				super.isParsedSuccessfully = false;
				printWriter.println("ERROR: " + this.getRawSourcePath() + " : " + attributeLabel + " (B) : length=" + this.length);
				super.rawData = attributeBytes;
			} else {
				int parameterCount = attributeBuffer.readUnsignedByte();
				this.parameterAnnotations = new AnnotationNode[parameterCount][];

				for (int parameterIndex = 0; parameterIndex < parameterCount; parameterIndex++) {
					int annotationCount = attributeBuffer.readUnsignedShort();
					this.parameterAnnotations[parameterIndex] = new AnnotationNode[annotationCount];

					for (int annotationIndex = 0; annotationIndex < annotationCount; annotationIndex++) {
						this.parameterAnnotations[parameterIndex][annotationIndex] = AnnotationNode.readAnnotationNode(this, attributeBuffer, utf8References);
						if (!this.parameterAnnotations[parameterIndex][annotationIndex].isValid()) {
							super.isParsedSuccessfully = false;
							printWriter.println("ERROR: " + this.getRawSourcePath() + " : " + attributeLabel + " (A) " + this.parameterAnnotations[parameterIndex][annotationIndex].getErrorMsg());
							super.rawData = attributeBytes;
							return;
						}
					}
				}
			}
		} catch (IOException ioException) {
			super.isParsedSuccessfully = false;
			printWriter.println("ERROR: " + this.getRawSourcePath() + " : " + attributeLabel + " is possibly corrupt : " + ioException);
			super.rawData = attributeBytes;
		} finally {
			attributeBuffer.close();
		}
	}

	public String[] getOriginalAnnotationTypes(int parameterIndex) {
		if (!super.isParsedSuccessfully) {
			return new String[0];
		} else if (this.originalParameterAnnotations == null) {
			return this.getAnnotationTypes(parameterIndex);
		} else if (this.originalParameterAnnotations.length <= parameterIndex) {
			return new String[0];
		} else {
			String[] annotationTypes = new String[this.originalParameterAnnotations[parameterIndex].length];

			for (int i = 0; i < this.originalParameterAnnotations[parameterIndex].length; i++) {
				annotationTypes[i] = this.originalParameterAnnotations[parameterIndex][i].getTypeInternalName();
			}

			return annotationTypes;
		}
	}

	@Override
	public void resolveAnnotations(AbstractWorkspace workspace, ClassProvider classProvider, PhantomClassManager phantomClassManager, ScriptContext scriptContext) {
		if (super.isParsedSuccessfully) {
			for (int parameterIndex = 0; parameterIndex < this.parameterAnnotations.length; parameterIndex++) {
				for (int annotationIndex = 0; annotationIndex < this.parameterAnnotations[parameterIndex].length; annotationIndex++) {
					this.parameterAnnotations[parameterIndex][annotationIndex].resolveTypeAndParams(classProvider, phantomClassManager, scriptContext);
				}
			}
		}
	}

	public void updateParameterAnnotations(ParameterPattern parameterPattern) {
		int parameterCount = parameterPattern.getParameterCount();
		AnnotationNode[][] newParameterAnnotations = new AnnotationNode[parameterCount][];
		int extraParameterCount = parameterPattern.getExtraParameterCount();
		int oldParameterIndex = 0;
		int nextExtraParameterIndex = 0;
		nextExtraParameterIndex++;
		int extraParameterIndex = parameterPattern.getExtraParameterIndex(0);

		for (int parameterIndex = 0; parameterIndex < parameterCount; parameterIndex++) {
			if (parameterIndex == extraParameterIndex) {
				newParameterAnnotations[parameterIndex] = new AnnotationNode[0];
				if (nextExtraParameterIndex < extraParameterCount) {
					extraParameterIndex = parameterPattern.getExtraParameterIndex(nextExtraParameterIndex++);
				}
			} else if (oldParameterIndex < this.parameterAnnotations.length) {
				newParameterAnnotations[parameterIndex] = this.parameterAnnotations[oldParameterIndex++];
			} else {
				newParameterAnnotations[parameterIndex] = new AnnotationNode[0];
			}
		}

		if (this.originalParameterAnnotations == null) {
			this.originalParameterAnnotations = this.parameterAnnotations;
		}

		this.parameterAnnotations = newParameterAnnotations;
		this.getAttributeLength();
	}

	@Override
	public void updateConstants() {
		if (super.isParsedSuccessfully) {
			for (int parameterIndex = 0; parameterIndex < this.parameterAnnotations.length; parameterIndex++) {
				for (int annotationIndex = 0; annotationIndex < this.parameterAnnotations[parameterIndex].length; annotationIndex++) {
					this.parameterAnnotations[parameterIndex][annotationIndex].updateConstants();
				}
			}
		}
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		visitor.visit(this.attributeNameUtf8, this, this.getParent());
		if (super.isParsedSuccessfully) {
			for (int parameterIndex = 0; parameterIndex < this.parameterAnnotations.length; parameterIndex++) {
				for (int annotationIndex = 0; annotationIndex < this.parameterAnnotations[parameterIndex].length; annotationIndex++) {
					this.parameterAnnotations[parameterIndex][annotationIndex].acceptVisitor(visitor);
				}
			}
		}
	}

	@Override
	public boolean trimExcludedAnnotations(TrimExcludes trimExcludes, ScriptContext scriptContext, PrintWriter printWriter) {
		if (!super.isParsedSuccessfully) {
			return true;
		} else {
			PrintWriter contextWriter = scriptContext.getWriter();
			boolean annotationsExcluded = false;
			boolean allExcluded = true;
			ArrayList<List<AnnotationNode>> retainedAnnotationsByParameter = new ArrayList<>();

			for (int parameterIndex = 0; parameterIndex < this.parameterAnnotations.length; parameterIndex++) {
				ArrayList<AnnotationNode> retainedAnnotations = new ArrayList<>();
				retainedAnnotationsByParameter.add(retainedAnnotations);

				for (int annotationIndex = 0; annotationIndex < this.parameterAnnotations[parameterIndex].length; annotationIndex++) {
					AnnotationNode annotation = this.parameterAnnotations[parameterIndex][annotationIndex];
					AbstractClassNode typeClass = annotation.getTypeClass();
					if (trimExcludes.isAnnotationTypeExcluded(typeClass)) {
						allExcluded = false;
						retainedAnnotations.add(annotation);
						String retentionMessage = "Retaining element in '"
							+ this.getAttributeName()
							+ "' attribute in "
							+ (this.isOwnedByClass() ? "" : "'" + this.getName() + "' in ")
							+ "class '"
							+ this.getSourcePath()
							+ "' because of annotation attribute exclusion for class '"
							+ typeClass.getSourceName()
							+ "'.";
						if (scriptContext.isVerbose()) {
							contextWriter.println("\t" + retentionMessage);
						}

						printWriter.println(retentionMessage);
					} else {
						annotationsExcluded = true;
					}
				}
			}

			if (annotationsExcluded) {
				int parameterCount = retainedAnnotationsByParameter.size();
				AnnotationNode[][] rebuiltParameterAnnotations = new AnnotationNode[parameterCount][];

				for (int i = 0; i < parameterCount; i++) {
					List<AnnotationNode> retainedAnnotationList = retainedAnnotationsByParameter.get(i);
					AnnotationNode[] annotationArray = new AnnotationNode[retainedAnnotationList.size()];
					rebuiltParameterAnnotations[i] = retainedAnnotationList.toArray(annotationArray);
				}

				this.parameterAnnotations = rebuiltParameterAnnotations;
			}

			return allExcluded;
		}
	}

	@Override
	public void syncNames() {
		if (super.isParsedSuccessfully) {
			for (int parameterIndex = 0; parameterIndex < this.parameterAnnotations.length; parameterIndex++) {
				for (int annotationIndex = 0; annotationIndex < this.parameterAnnotations[parameterIndex].length; annotationIndex++) {
					this.parameterAnnotations[parameterIndex][annotationIndex].syncName();
				}
			}
		}
	}

	@Override
	public void writeAttributeBody(DataOutputStream dataOutputStream) throws IOException {
		super.writeAttributeBody(dataOutputStream);
		if (super.isParsedSuccessfully) {
			dataOutputStream.writeByte(this.parameterAnnotations.length);

			for (int parameterIndex = 0; parameterIndex < this.parameterAnnotations.length; parameterIndex++) {
				dataOutputStream.writeShort(this.parameterAnnotations[parameterIndex].length);

				for (int annotationIndex = 0; annotationIndex < this.parameterAnnotations[parameterIndex].length; annotationIndex++) {
					this.parameterAnnotations[parameterIndex][annotationIndex].write(dataOutputStream);
				}
			}
		} else {
			dataOutputStream.write(super.rawData);
		}
	}

	public String[] getAnnotationTypes(int parameterIndex) {
		if (super.isParsedSuccessfully && this.parameterAnnotations.length > parameterIndex) {
			String[] annotationTypes = new String[this.parameterAnnotations[parameterIndex].length];

			for (int i = 0; i < this.parameterAnnotations[parameterIndex].length; i++) {
				annotationTypes[i] = this.parameterAnnotations[parameterIndex][i].getTypeInternalName();
			}

			return annotationTypes;
		} else {
			return new String[0];
		}
	}

	@Override
	public void collectClassReferences(Set<AbstractClassNode> classNodes) {
		if (super.isParsedSuccessfully) {
			for (int parameterIndex = 0; parameterIndex < this.parameterAnnotations.length; parameterIndex++) {
				for (int annotationIndex = 0; annotationIndex < this.parameterAnnotations[parameterIndex].length; annotationIndex++) {
					this.parameterAnnotations[parameterIndex][annotationIndex].collectClassReferences(classNodes);
				}
			}
		}
	}

	@Override
	public void writeAttributeBodyRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap, ScriptContext scriptContext) throws IOException {
		super.writeAttributeBodyRemapped(dataOutputStream, writerMap, scriptContext);
		if (super.isParsedSuccessfully) {
			dataOutputStream.writeByte(this.parameterAnnotations.length);

			for (int parameterIndex = 0; parameterIndex < this.parameterAnnotations.length; parameterIndex++) {
				dataOutputStream.writeShort(this.parameterAnnotations[parameterIndex].length);

				for (int annotationIndex = 0; annotationIndex < this.parameterAnnotations[parameterIndex].length; annotationIndex++) {
					this.parameterAnnotations[parameterIndex][annotationIndex].writeRemapped(dataOutputStream, writerMap, scriptContext);
				}
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
			int attributeLength = 1;
			int parameterCount = this.parameterAnnotations.length;

			for (int parameterIndex = 0; parameterIndex < parameterCount; parameterIndex++) {
				attributeLength += 2;
				int annotationCount = this.parameterAnnotations[parameterIndex].length;

				for (int annotationIndex = 0; annotationIndex < annotationCount; annotationIndex++) {
					attributeLength += this.parameterAnnotations[parameterIndex][annotationIndex].getSize();
				}
			}

			this.length = attributeLength;
			return this.length;
		}
	}

	@Override
	public void remapDescriptors(int keepInnerClassInfo, int keepGenericsInformation, HashMap<String, String> packageRenameMap, HashMap<String, String> classNameMap) {
		if (super.isParsedSuccessfully) {
			for (int parameterIndex = 0; parameterIndex < this.parameterAnnotations.length; parameterIndex++) {
				for (int annotationIndex = 0; annotationIndex < this.parameterAnnotations[parameterIndex].length; annotationIndex++) {
					this.parameterAnnotations[parameterIndex][annotationIndex].remapDescriptors(packageRenameMap, classNameMap);
				}
			}
		}
	}

	@Override
	public void collectAllReferences(Set<ClassNode> classReferences, Set<ClassNode> signatureClassReferences, Set<FieldNode> fieldReferences, Set<MethodNode> methodReferences) {
		if (super.isParsedSuccessfully) {
			for (int parameterIndex = 0; parameterIndex < this.parameterAnnotations.length; parameterIndex++) {
				for (int annotationIndex = 0; annotationIndex < this.parameterAnnotations[parameterIndex].length; annotationIndex++) {
					this.parameterAnnotations[parameterIndex][annotationIndex].collectWorkspaceRefs(classReferences, signatureClassReferences, fieldReferences, methodReferences);
				}
			}
		}
	}

	public AnnotationNode getParameterAnnotation(String typeInternalName) {
		if (super.isParsedSuccessfully) {
			for (int parameterIndex = 0; parameterIndex < this.parameterAnnotations.length; parameterIndex++) {
				for (int annotationIndex = 0; annotationIndex < this.parameterAnnotations[parameterIndex].length; annotationIndex++) {
					if (typeInternalName.equals(this.parameterAnnotations[parameterIndex][annotationIndex].getTypeInternalName())) {
						return this.parameterAnnotations[parameterIndex][annotationIndex];
					}
				}
			}
		}

		return null;
	}
}
