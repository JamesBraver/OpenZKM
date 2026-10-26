package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.base.UnsupportedCommand;

public class AdaptClassStrings extends UnsupportedCommand {
	public AdaptClassStrings(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-adaptclassstrings";
	}
}
