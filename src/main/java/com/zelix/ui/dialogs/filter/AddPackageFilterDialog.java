package com.zelix.ui.dialogs.filter;

import com.zelix.HelpManager;
import com.zelix.ui.components.panels.filter.ContainingPackagePanel;
import com.zelix.ui.dataset.ParameterModel;
import com.zelix.ui.dialogs.base.BaseAddFilterDialog;
import com.zelix.utils.function.ZKMConsumer;
import javax.swing.JFrame;

public class AddPackageFilterDialog extends BaseAddFilterDialog {
	public ContainingPackagePanel packagePanel;

	public AddPackageFilterDialog(JFrame parent, String title, ParameterModel parameterModel, int targetType, ZKMConsumer resultConsumer) {
		super(parent, title, parameterModel, targetType, resultConsumer);
	}

	@Override
	protected final void openHelpGUI() {
		HelpManager.showHelp("027");
	}

	@Override
	public void initTabs() {
		this.packagePanel = new ContainingPackagePanel(this, super.parameterModel, false, super.targetType);
		super.tabbedPane.addTab("Package", this.packagePanel);
	}
}
