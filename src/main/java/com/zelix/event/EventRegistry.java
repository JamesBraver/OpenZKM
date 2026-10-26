package com.zelix.event;

import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.readOnly.ReadOnlyList;
import java.util.Map;

public class EventRegistry {
	private final DualKeyMap<Object, INotifier, ChangeListener> registry;

	public EventRegistry(boolean useConcurrent) {
		this.registry = new DualKeyMap<>(useConcurrent);
	}

	public EventRegistry() {
		this(false);
	}

	public void subscribeDefault(AbstractNotifier notifier, ChangeListener listener) {
		this.subscribe(notifier, listener, "DefaultKey__");
	}

	public void subscribe(INotifier notifier, ChangeListener listener, Object key) {
		notifier.subscribe(listener);
		this.registry.put(key, notifier, listener);
	}

	public void clearAll() {
		for (Object groupKey : new ReadOnlyList<>(this.registry.getGroupKeys())) {
			this.unsubscribeGroup(groupKey);
		}
	}

	public ChangeListener unsubscribe(INotifier notifier, ChangeListener listener, Object key) {
		notifier.unsubscribe(listener);
		return this.registry.remove(key, notifier);
	}

	public void unsubscribeGroup(Object key) {
		Map<INotifier, ChangeListener> group = this.registry.getGroup(key);
		if (group != null) {
			for (INotifier notifier : GlobalUtils.newHashSet(group.keySet())) {
				ChangeListener listener = group.get(notifier);
				this.unsubscribe(notifier, listener, key);
			}
		}
	}
}
