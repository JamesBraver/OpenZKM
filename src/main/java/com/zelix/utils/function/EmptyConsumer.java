package com.zelix.utils.function;

public class EmptyConsumer implements ZKMConsumer {
	private static EmptyConsumer INSTANCE = new DefaultConsumer();

	public static EmptyConsumer create() {
		return INSTANCE;
	}

	@Override
	public void accept(Object classes, Object archiveNodes, Object jadResources, Object xmlResources, Object selectedResources, Object runningPathRef) {
	}

	@Override
	public void accept(Object classes, Object archiveNodes, Object jadResources, Object xmlResources, Object selectedResources, Object runningPathRef, Object openNestedArchives, Object archiveNodeSet) {
	}

	@Override
	public void accept(Object arg, Object choice) {
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
