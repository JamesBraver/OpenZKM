package com.zelix.event;

import com.zelix.framework.StructureNode;

public class ObservableStructure extends ChangeTrackingNotifier implements ChangeListener, ResourceChangedListener {
	public String type;
	public StructureNode node;

	public ObservableStructure(String type, StructureNode node) {
		this.type = type;
		this.node = node;
	}

	@Override
	public void onChanged(INotifier notifier, Object changeCode, Object changedObject, Object unused) {
		this.markChanged();
		this.notify(changeCode, changedObject, unused);
	}

	public StructureNode getNode() {
		return this.node;
	}

	public String getType() {
		return this.type;
	}

	@Override
	public void onChange(INotifier notifier, Object changeKind, Object changedNode, Object changeArg) {
		new ResourceChangeDispatcher(this, notifier, changeKind, changedNode, changeArg);
	}
}
