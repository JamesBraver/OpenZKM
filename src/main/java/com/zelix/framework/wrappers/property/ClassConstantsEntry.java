package com.zelix.framework.wrappers.property;

import com.zelix.framework.StructureNode;
import com.zelix.framework.constant.ConstantNode;
import com.zelix.framework.constant.StringNode;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.writer.base.ConstantWriter;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.IntegerCache;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;

public class ClassConstantsEntry extends ClassComponentEntry<ConstantWriter> {
	private Map<StringNode, StringNode> remappedNodes;
	private ReferenceGraph<StringNode, StringNode> referenceGraph;
	public ConstantWriter[] constants;
	public int constantCount;

	public ClassConstantsEntry(String type, StructureNode node, ConstantWriter[] constants, IntegerCache integerCache) {
		super(type, node, integerCache);
		this.setConstants(constants);
	}

	public void refreshConstants(ConstantWriter[] constants) {
		this.setConstants(constants);
		this.markChanged();
		this.notifySimpleChange();
	}

	public int findConstantIndex(ConstantNode constantNode) {
		if (super.indexMap == null) {
			this.remappedNodes = GlobalUtils.newHashMap();

			for (Entry<StringNode, Set<StringNode>> adjacencyEntry : this.referenceGraph.getAdjacencyEntries()) {
				StringNode remappedNode = adjacencyEntry.getKey();

				for (StringNode originalNode : adjacencyEntry.getValue()) {
					this.remappedNodes.put(originalNode, remappedNode);
				}
			}

			super.indexMap = GlobalUtils.newHashMap((int)(this.constantCount * 1.5));

			for (int i = 0; i < this.constantCount; i++) {
				super.indexMap.put(this.constants[i], super.integerCache.valueOf(i));
			}
		}

		Object index = super.indexMap.get(constantNode instanceof StringNode ? GlobalUtils.getMappedOrDefault((StringNode)constantNode, this.remappedNodes) : constantNode);
		if (index != null) {
			return (Integer)index;
		} else {
			throw new IllegalArgumentException("Entry not found in " + this.getClass().getName());
		}
	}

	public int getConstantCount() {
		return this.constantCount;
	}

	public ConstantWriter getConstant(int index) {
		return this.constants[index];
	}

	public boolean hasMoreConstants() {
		return super.currentIndex < this.constantCount;
	}

	public ReferenceGraph<StringNode, StringNode> getReferenceGraph() {
		return this.referenceGraph;
	}

	public String getNextConstantName() {
		return this.constants[super.currentIndex++].getDisplayValue();
	}

	public void setConstants(ConstantWriter[] constants) {
		this.referenceGraph = new ReferenceGraph<>();
		ArrayList<ConstantWriter> distinctConstants = new ArrayList<>();

		for (ConstantWriter constant : constants) {
			if (constant instanceof StringNode) {
				StringNode stringNode = (StringNode)constant;
				StringNode remappedNode = stringNode.getRemappedNode();
				if (remappedNode != null) {
					this.referenceGraph.addReference(remappedNode, stringNode);
				} else {
					distinctConstants.add(stringNode);
				}
			} else {
				distinctConstants.add(constant);
			}
		}

		this.constantCount = distinctConstants.size();
		this.constants = new ConstantWriter[this.constantCount];
		this.constants = distinctConstants.toArray(this.constants);
		super.indexMap = null;
		this.resetIndex();
	}
}
