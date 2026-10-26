package com.zelix.repository;

import com.zelix.utils.function.ZKMConsumer;

public class ClassLoadingStatus implements ZKMConsumer {
	private boolean success = true;
	private String errorMessage;

	public void setStatus(boolean success, String errorMessage) {
		this.success = success;
		this.errorMessage = errorMessage;
	}

	@Override
	public void accept(Object classes, Object archiveNodes, Object jadResources, Object xmlResources, Object selectedResources, Object runningPathRef) {
	}

	public String getErrorMessage() {
		return this.errorMessage;
	}

	@Override
	public void accept(Object classes, Object archiveNodes, Object jadResources, Object xmlResources, Object selectedResources, Object runningPathRef, Object openNestedArchives, Object archiveNodeSet) {
	}

	@Override
	public void accept(Object arg, Object choice) {
	}

	public boolean isSuccess() {
		return this.success;
	}

	@Override
	public void accept(Object changeLogFiles, Object changeLogFilesText, Object choice) {
	}

	@Override
	public void accept(Object arg) {
	}

	@Override
	public void accept() {
	}
}
