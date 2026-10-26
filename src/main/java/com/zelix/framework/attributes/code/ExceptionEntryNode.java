package com.zelix.framework.attributes.code;

import com.zelix.exceptions.ClassParseFailedException;
import com.zelix.framework.ClearingFlag;
import com.zelix.framework.SettingFlag;
import com.zelix.framework.StructureNode;
import com.zelix.framework.analyzer.MethodAnalyzer;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.instructions.LabelNode;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.reference.IGraphElement;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.comparators.ThrowsComparator;
import com.zelix.utils.dataset.ByteDataBuffer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class ExceptionEntryNode extends StructureNode implements ReferenceUpdateListener, IGraphElement {
	private static volatile Comparator<ExceptionEntryNode> throwsComparator;
	private Object[] labels = new Object[3];
	private ClassInfoWriter targetException;
	private boolean labelBased;

	public ExceptionEntryNode(CodeAttribute codeAttribute, ClassInfoWriter targetException, LabelNode startLbl, LabelNode endLbl, LabelNode handlerLbl) {
		super(codeAttribute);
		this.targetException = targetException;
		startLbl.addUsage(SettingFlag.SET_USAGE_EXCEPTION_TRY_START);
		endLbl.addUsage(SettingFlag.SET_USAGE_EXCEPTION_TRY_END);
		handlerLbl.addUsage(SettingFlag.SET_USAGE_EXCEPTION_HANDLER);
		Object[] labelArray = new Object[]{startLbl, endLbl, handlerLbl};
		this.labels = labelArray;
		this.labelBased = true;
	}

	public ExceptionEntryNode(
		StructureNode parent, ByteDataBuffer dataBuffer, MultiMap<Integer, IGraphElement> offsetReferences, MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences
	) throws IOException {
		super(parent);
		int startPC = dataBuffer.readUnsignedShort();
		int endPC = dataBuffer.readUnsignedShort();
		int handlerPC = dataBuffer.readUnsignedShort();
		Integer pcKey = INTEGER_CACHE.valueOf(startPC);
		offsetReferences.put(pcKey, this);
		this.labels[TryCatchBlockPCType.START_PC.ordinal()] = pcKey;
		pcKey = INTEGER_CACHE.valueOf(endPC);
		offsetReferences.put(pcKey, this);
		this.labels[TryCatchBlockPCType.END_PC.ordinal()] = pcKey;
		pcKey = INTEGER_CACHE.valueOf(handlerPC);
		offsetReferences.put(pcKey, this);
		this.labels[TryCatchBlockPCType.HANDLER_PC.ordinal()] = pcKey;
		int catchTypeIndex = dataBuffer.readUnsignedShort();
		this.initTargetException(catchTypeIndex, classReferences);
	}

	public static Comparator<ExceptionEntryNode> getThrowsComparator() {
		if (throwsComparator == null) {
			throwsComparator = new ThrowsComparator();
		}

		return throwsComparator;
	}

	public static ClearingFlag getFlagByType(TryCatchBlockPCType type) {
		switch (type) {
			case START_PC:
				return ClearingFlag.CLEAR_USAGE_EXCEPTION_TRY_START;
			case END_PC:
				return ClearingFlag.CLEAR_USAGE_EXCEPTION_TRY_END;
			case HANDLER_PC:
				return ClearingFlag.CLEAR_USAGE_EXCEPTION_HANDLER;
			default:
				throw new IllegalArgumentException();
		}
	}

	private static boolean isConsecutiveSequence(int start, int end, int[] labelIds) {
		int labelIdIndex = Arrays.binarySearch(labelIds, start);

		for (int labelId = start + 1; labelId < end; labelId++) {
			labelIdIndex++;
			if (labelIdIndex >= labelIds.length || labelId != labelIds[labelIdIndex]) {
				return false;
			}
		}

		return true;
	}

	public boolean hasNoTargetException() {
		return this.targetException == null;
	}

	public String getName(int labelIndex) {
		return ((LabelNode)this.labels[labelIndex]).getLabelName();
	}

	@Override
	public SettingFlag getSettingFlag() {
		return SettingFlag.SET_USAGE_EXCEPTION;
	}

	public void printDetails(PrintWriter writer, StringBuffer indent) {
		writer.println("");
		String catchDescription;
		if (this.targetException != null) {
			catchDescription = "catch " + this.targetException.getInternalName();
		} else {
			catchDescription = "catch ANY (finally)";
		}

		writer.println(indent + catchDescription);
		writer.println(indent + "\tStart offset   : " + this.getName(0));
		writer.println(indent + "\tEnd offset     : " + this.getName(1));
		writer.println(indent + "\tHandler offset : " + this.getName(2));
	}

	@Override
	public void setLabel(Integer pos, LabelNode lbl) {
		int labelCount = this.labels.length;
		lbl.setVisible(true);

		for (int labelIndex = 0; labelIndex < labelCount; labelIndex++) {
			Object labelSlot = this.labels[labelIndex];
			if (labelSlot instanceof Integer && ((Integer)labelSlot).equals(pos)) {
				this.labels[labelIndex] = lbl;
				switch (labelIndex) {
					case 0:
						lbl.addUsage(SettingFlag.SET_USAGE_EXCEPTION_TRY_START);
						break;
					case 1:
						lbl.addUsage(SettingFlag.SET_USAGE_EXCEPTION_TRY_END);
						break;
					case 2:
						lbl.addUsage(SettingFlag.SET_USAGE_EXCEPTION_HANDLER);
				}
			}
		}
	}

	public boolean isOutsideRange(int startOffset, int endOffset) {
		int entryStartOffset = this.getOffset(0);
		int entryEndOffset = this.getOffset(1);
		return entryStartOffset >= endOffset || entryEndOffset <= startOffset;
	}

	private boolean removeLblInGraph(LabelNode lbl, ReferenceGraph<LabelNode, IGraphElement> graph, TryCatchBlockPCType pType) {
		boolean referencedByEntry = false;
		boolean referencedAsPC = false;
		Set<IGraphElement> referencingElements = graph.getReferences(lbl);
		if (referencingElements != null) {
			for (IGraphElement graphElement : referencingElements) {
				if (graphElement instanceof ExceptionEntryNode) {
					referencedByEntry = true;
					if (((ExceptionEntryNode)graphElement).getPCByType(pType) == lbl) {
						referencedAsPC = true;
						break;
					}
				}
			}
		}

		if (!referencedByEntry) {
			lbl.removeFlag(ClearingFlag.CLEAR_USAGE_EXCEPTION);
			lbl.removeFlag(getFlagByType(pType));
		} else if (!referencedAsPC) {
			lbl.removeFlag(getFlagByType(pType));
		}

		return !referencedAsPC;
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		if (this.targetException != null) {
			this.targetException.accept(visitor, this, this);
		}
	}

	public void initTargetException(int typeIndex, MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences) {
		if (typeIndex != 0) {
			TypeWriter catchTypeEntry = this.getConstantPoolEntry(typeIndex);
			if (!(catchTypeEntry instanceof ClassInfoWriter)) {
				throw new ClassParseFailedException(this.getRawSourcePath() + " : Invalid Class index in Exception Entry : " + typeIndex);
			}

			this.targetException = (ClassInfoWriter)catchTypeEntry;
			classReferences.put(this.targetException, this);
		}
	}

	public String getExceptionName() {
		return this.targetException != null ? this.targetException.getInternalName() : "java/lang/Throwable";
	}

	@Override
	public void updateClassReference(ClassInfoWriter oldReference, ClassInfoWriter newReference) {
		if (this.targetException != null && this.targetException == oldReference) {
			this.targetException = newReference;
		}
	}

	@Override
	public String getMnemonics() {
		return "ExceptionEntry";
	}

	public void writeRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> remappedConstants) throws IOException {
		dataOutputStream.writeShort(this.getOffset(0));
		dataOutputStream.writeShort(this.getOffset(1));
		dataOutputStream.writeShort(this.getOffset(2));
		if (this.targetException != null) {
			TypeWriter remappedCatchType = remappedConstants.get(this.targetException);
			if (remappedCatchType != null) {
				dataOutputStream.writeShort(remappedCatchType.getTag());
			} else {
				dataOutputStream.writeShort(this.targetException.getTag());
			}
		} else {
			dataOutputStream.writeShort(0);
		}
	}

	public void write(DataOutputStream dataOutputStream) throws IOException {
		dataOutputStream.writeShort(this.getOffset(0));
		dataOutputStream.writeShort(this.getOffset(1));
		dataOutputStream.writeShort(this.getOffset(2));
		dataOutputStream.writeShort(this.targetException == null ? 0 : this.targetException.getTag());
	}

	public int getStartLabelId() {
		return ((LabelNode)this.labels[0]).getLabelID();
	}

	public int getOffset(int labelIndex) {
		return ((LabelNode)this.labels[labelIndex]).getOffset();
	}

	public boolean isLabelBased() {
		return this.labelBased;
	}

	public int getEndLabelId() {
		return ((LabelNode)this.labels[1]).getLabelID();
	}

	public boolean isInsideRange(int startOffset, int endOffset) {
		int entryStartOffset = this.getOffset(0);
		int entryEndOffset = this.getOffset(1);
		return entryStartOffset >= startOffset && entryEndOffset <= endOffset;
	}

	public boolean removeInGraph(HashSet<LabelNode> removedLabels, int[] labelIds, ReferenceGraph<LabelNode, IGraphElement> graph, MethodAnalyzer methodAnalyzer) {
		LabelNode start = (LabelNode)this.labels[0];
		LabelNode end = (LabelNode)this.labels[1];
		LabelNode handler = (LabelNode)this.labels[2];
		if (removedLabels.contains(handler) || removedLabels.contains(start) && isConsecutiveSequence(start.getLabelID(), end.getLabelID(), labelIds)) {
			graph.removeReference(start, this);
			graph.removeReference(end, this);
			graph.removeReference(handler, this);
			if (removedLabels.contains(start)) {
				this.removeLblInGraph(start, graph, TryCatchBlockPCType.START_PC);
			}

			if (removedLabels.contains(end)) {
				this.removeLblInGraph(end, graph, TryCatchBlockPCType.END_PC);
			}

			this.removeLblInGraph(handler, graph, TryCatchBlockPCType.HANDLER_PC);
			return true;
		} else {
			return false;
		}
	}

	public LabelNode getPCByType(TryCatchBlockPCType type) {
		return (LabelNode)this.labels[type.ordinal()];
	}

	@Override
	public void registerToGraph(ReferenceGraph<LabelNode, IGraphElement> graph) {
		for (int i = 0; i < this.labels.length; i++) {
			graph.addReference((LabelNode)this.labels[i], this);
		}
	}

	public ClassInfoWriter getTargetException() {
		return this.targetException;
	}

	public boolean partiallyOverlapsRange(int startOffset, int endOffset) {
		int entryStartOffset = this.getOffset(0);
		int entryEndOffset = this.getOffset(1);
		return entryStartOffset > startOffset && entryStartOffset < endOffset && entryEndOffset > endOffset || entryStartOffset < startOffset && entryEndOffset > startOffset && entryEndOffset < endOffset;
	}
}
