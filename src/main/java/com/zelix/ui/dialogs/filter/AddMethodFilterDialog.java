package com.zelix.ui.dialogs.filter;

import com.zelix.HelpManager;
import com.zelix.ui.components.panels.base.ContainingPanel;
import com.zelix.ui.components.panels.filter.ContainingClassPanel;
import com.zelix.ui.components.panels.filter.ContainingMethodPanel;
import com.zelix.ui.components.panels.filter.ContainingPackagePanel;
import com.zelix.ui.dataset.ParameterModel;
import com.zelix.ui.dialogs.base.BaseAddFilterDialog;
import com.zelix.utils.function.ZKMConsumer;
import javax.swing.JFrame;

public class AddMethodFilterDialog extends BaseAddFilterDialog {
	public ContainingPanel classPanel;
	public ContainingMethodPanel methodPanel;
	public ContainingPackagePanel packagePanel;

	public AddMethodFilterDialog(JFrame parent, String title, ParameterModel parameterModel, int targetType, ZKMConsumer resultConsumer) {
		super(parent, title, parameterModel, targetType, resultConsumer);
	}

	@Override
	protected final void openHelpGUI() {
		if (super.targetType == 1) {
			HelpManager.showHelp("030");
		} else if (super.targetType == 2) {
			HelpManager.showHelp("024");
		} else if (super.targetType == 3) {
			HelpManager.showHelp("044");
		}
	}

	@Override
	public void initTabs() {
		this.packagePanel = new ContainingPackagePanel(this, super.parameterModel, super.targetType == 1, super.targetType);
		boolean higherLevelExcludeEnabled = super.targetType != 3;
		int targetType = super.targetType;
		ParameterModel parameterModel = super.parameterModel;
		this.classPanel = new ContainingClassPanel(this, parameterModel, higherLevelExcludeEnabled, targetType);
		this.methodPanel = new ContainingMethodPanel(this, super.parameterModel, super.targetType);
		super.tabbedPane.addTab("Containing package", null, this.packagePanel, HelpManager.getToolTip("CONTAINING_PACKAGE_PART_OF_PARAM"));
		super.tabbedPane.addTab("Containing class", null, this.classPanel, HelpManager.getToolTip("CONTAINING_CLASS_PART_OF_PARAM"));
		super.tabbedPane.addTab("Method", null, this.methodPanel, HelpManager.getToolTip("METHOD_PART_OF_PARAM"));
		super.tabbedPane.setSelectedComponent(this.methodPanel);
	}
}
