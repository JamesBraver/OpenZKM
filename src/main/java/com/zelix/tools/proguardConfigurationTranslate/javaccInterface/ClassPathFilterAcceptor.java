package com.zelix.tools.proguardConfigurationTranslate.javaccInterface;

public interface ClassPathFilterAcceptor {
	boolean addFileFilter(String filter);

	boolean addArchiveFilter(String filter);
}
