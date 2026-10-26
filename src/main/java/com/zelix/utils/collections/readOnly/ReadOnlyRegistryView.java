package com.zelix.utils.collections.readOnly;

import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.DualKeyMap;
import java.util.Enumeration;
import java.util.Map;

public class ReadOnlyRegistryView<I, K, V> {
	private DualKeyMap<I, K, V> wrappedRegistry;

	public ReadOnlyRegistryView(DualKeyMap<I, K, V> wrappedRegistry) {
		if (wrappedRegistry == null) {
			throw new IllegalArgumentException("Null argument not allowed");
		} else {
			this.wrappedRegistry = wrappedRegistry;
		}
	}

	public final V get(I groupKey, K innerKey) {
		return this.wrappedRegistry.get(groupKey, innerKey);
	}

	@Override
	public final ReadOnlyRegistryView<I, K, V> clone() {
		return new ReadOnlyRegistryView<>(GlobalUtils.clone(this.wrappedRegistry));
	}

	public final int getGroupCount() {
		return this.wrappedRegistry.getGroupCount();
	}

	public final synchronized Enumeration<I> getGroupKeys() {
		return this.wrappedRegistry.getGroupKeys();
	}

	public ReadOnlyMap<K, V> getReadOnlyGroup(I groupKey) {
		Map<K, V> wrappedGroup = this.wrappedRegistry.getGroup(groupKey);
		return wrappedGroup != null ? new ReadOnlyMap<>(wrappedGroup) : null;
	}
}
