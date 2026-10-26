package com.zelix.ui.components.cell;

import java.awt.Component;
import javax.swing.JList;

public abstract class AbstractListCellRenderer {
	protected Object value;

	public AbstractListCellRenderer(Object value) {
		this.value = value;
	}

	public abstract void initRenderer(Component component, JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus);

	public Object getValue() {
		return this.value;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof AbstractListCellRenderer ? this.value.equals(((AbstractListCellRenderer)obj).value) : false;
	}

	@Override
	public int hashCode() {
		return this.value.hashCode();
	}

	@Override
	public String toString() {
		return this.value.toString();
	}
}
