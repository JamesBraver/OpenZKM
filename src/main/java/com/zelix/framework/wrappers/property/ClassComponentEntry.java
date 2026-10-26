package com.zelix.framework.wrappers.property;

import com.zelix.event.ObservableStructure;
import com.zelix.framework.StructureNode;
import com.zelix.utils.IntegerCache;
import java.util.Map;

public class ClassComponentEntry<E> extends ObservableStructure {
	public IntegerCache integerCache;
	public int currentIndex;
	public Map<E, Integer> indexMap;

	public ClassComponentEntry(String type, StructureNode node, IntegerCache integerCache) {
		super(type, node);
		this.integerCache = integerCache;
	}

	public final void resetIndex() {
		this.currentIndex = 0;
	}
}
