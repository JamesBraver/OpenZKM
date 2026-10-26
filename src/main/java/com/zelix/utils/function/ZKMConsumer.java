package com.zelix.utils.function;

import java.io.IOException;

public interface ZKMConsumer {
	void accept(Object classes, Object archiveNodes, Object jadResources, Object xmlResources, Object selectedResources, Object runningPathRef);

	void accept(Object classes, Object archiveNodes, Object jadResources, Object xmlResources, Object selectedResources, Object runningPathRef, Object openNestedArchives, Object archiveNodeSet);

	void accept(Object arg, Object choice) throws IOException;

	void accept(Object changeLogFiles, Object changeLogFilesText, Object choice);

	void accept(Object arg);

	void accept();
}
