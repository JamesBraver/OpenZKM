package com.zelix.framework.analyzer.dataset;

import com.zelix.framework.analyzer.AnalyzingNode;
import java.util.BitSet;

public class FlagData {
	public final AnalyzingNode node;
	public final BitSet bitSet;

	public FlagData(AnalyzingNode node, BitSet bitSet) {
		this.node = node;
		this.bitSet = bitSet;
	}
}
