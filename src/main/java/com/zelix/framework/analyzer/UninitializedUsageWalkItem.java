package com.zelix.framework.analyzer;

import java.util.BitSet;

public class UninitializedUsageWalkItem {
	public final AnalyzingNode node;
	public final int fromIndex;
	public final BitSet usageMask;

	public UninitializedUsageWalkItem(AnalyzingNode node, int fromIndex, BitSet usageMask) {
		this.node = node;
		this.fromIndex = fromIndex;
		this.usageMask = usageMask;
	}
}
