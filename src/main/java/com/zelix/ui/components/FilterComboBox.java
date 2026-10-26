package com.zelix.ui.components;

import com.zelix.ui.filefilters.AllFiles;
import com.zelix.ui.filefilters.FileFilterBase;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

public class FilterComboBox extends JComboBox<FileFilterBase> implements FileSelectorComponent, ItemListener {
	private DefaultComboBoxModel<FileFilterBase> filterModel;

	public FilterComboBox(int selectedIndex, FileFilterBase[] filters) {
		this.initFilters(selectedIndex, filters);
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.SELECTED) {
			FileFilterBase selectedFilter = (FileFilterBase)this.getSelectedItem();
			this.firePropertyChange("fileFilterChanged", null, selectedFilter);
		}
	}

	public void initFilters(int selectedIndex, FileFilterBase[] filters) {
		this.filterModel = new DefaultComboBoxModel<>();
		if (filters != null && filters.length != 0) {
			for (int i = 0; i < filters.length; i++) {
				this.filterModel.addElement(filters[i]);
			}
		} else {
			this.filterModel.addElement(new AllFiles());
		}

		this.setModel(this.filterModel);
		this.setSelectedIndex(selectedIndex);
		this.addItemListener(this);
		this.setToolTipText("File types to display");
	}
}
