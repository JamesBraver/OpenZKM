package com.zelix.ui.components;

import com.zelix.ui.components.cell.AbstractListCellRenderer;
import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.ListModel;

public class ClassList<E extends AbstractListCellRenderer> extends PlatformAdaptiveList<E> {
	public ClassList(ListModel<E> model) {
		super(model);
		this.initCellRenderer();
	}

	private void initCellRenderer() {
		this.setCellRenderer(new ClassList.ClassListCellRenderer(this));
	}

	public static class ClassListCellRenderer extends DefaultListCellRenderer {
		public final ClassList<?> classList;

		public ClassListCellRenderer(ClassList<?> classList) {
			this.classList = classList;
		}

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			((AbstractListCellRenderer)value).initRenderer(this, list, value, index, isSelected, cellHasFocus);
			return this;
		}
	}
}
