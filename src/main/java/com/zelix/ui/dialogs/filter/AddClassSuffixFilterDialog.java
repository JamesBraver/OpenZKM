package com.zelix.ui.dialogs.filter;

import com.zelix.HelpManager;
import com.zelix.ui.components.panels.filter.ContainingClassSuffixPanel;
import com.zelix.ui.components.panels.filter.ContainingPackagePanel;
import com.zelix.ui.dataset.ParameterModel;
import com.zelix.ui.dialogs.base.BaseAddFilterDialog;
import com.zelix.utils.function.ZKMConsumer;
import javax.swing.JFrame;

public class AddClassSuffixFilterDialog extends BaseAddFilterDialog {
	public ContainingClassSuffixPanel superClassPanel;
	public ContainingPackagePanel packagePanel;

	public AddClassSuffixFilterDialog(JFrame parent, String title, ParameterModel parameterModel, int targetType, ZKMConsumer resultConsumer) {
		super(parent, title, parameterModel, targetType, resultConsumer);
	}

	@Override
	protected final void openHelpGUI() {
		HelpManager.showHelp("031");
	}

	@Override
	public void initTabs() {
		this.packagePanel = new ContainingPackagePanel(this, super.parameterModel, false, super.targetType);
		this.superClassPanel = new ContainingClassSuffixPanel(this, super.parameterModel, super.targetType);
		super.tabbedPane.addTab("Containing package", null, this.packagePanel, HelpManager.getToolTip("CONTAINING_PACKAGE_PART_OF_PARAM"));
		super.tabbedPane.addTab("Class", null, this.superClassPanel, HelpManager.getToolTip("CLASS_SUFFIX_PART_OF_PARAM"));
		super.tabbedPane.setSelectedComponent(this.superClassPanel);
	}
}
