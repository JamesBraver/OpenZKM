package com.zelix.repository;

import com.zelix.event.ChangeListener;
import com.zelix.event.INotifier;
import com.zelix.framework.wrappers.LibraryClassNode;
import com.zelix.klassmaster.config.Options;
import com.zelix.utils.GlobalUtils;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VersionedClasspathCache implements ChangeListener {
	private boolean isScannerInvalid = false;
	private ClasspathManager classpathManager;
	private final boolean defaultSearchSystem;
	private ClasspathScanner classpathScanner;
	private Map<String, LibraryClassNode> classCache;

	public VersionedClasspathCache(ClasspathManager classpathManager, boolean defaultSearchSystem) {
		this.classpathManager = classpathManager;
		this.defaultSearchSystem = defaultSearchSystem;
		classpathManager.subscribe(this);
		this.reinitializeCache();
	}

	public static boolean setScannerInvalid(VersionedClasspathCache cache, boolean invalid) {
		return cache.isScannerInvalid = invalid;
	}

	public static ClasspathScanner getClasspathScanner(VersionedClasspathCache cache) {
		return cache.classpathScanner;
	}

	public static ClasspathManager getClasspathManager(VersionedClasspathCache cache) {
		return cache.classpathManager;
	}

	public static void reinitializeCache(VersionedClasspathCache cache) {
		cache.reinitializeCache();
	}

	public static Map<String, LibraryClassNode> getClassCacheMap(VersionedClasspathCache cache) {
		return cache.classCache;
	}

	private void reinitializeCache() {
		if (!this.isScannerInvalid && this.classpathScanner != null) {
			this.classpathScanner.close();
		}

		this.classpathScanner = new ClasspathScanner(this.classpathManager.getRawClasspath(), this.defaultSearchSystem);
		this.isScannerInvalid = false;
		this.classCache = Options.ZKM_USE_PARALLEL ? new ConcurrentHashMap<>() : GlobalUtils.newHashMap();
	}

	public void close() {
		if (this.classpathScanner != null) {
			this.classpathScanner.close();
		}

		if (this.classpathManager != null) {
			this.classpathManager.closeJrtFileSystem();
		}

		this.classCache = null;
	}

	public Integer getReleaseVersion() {
		return this.classpathManager.getReleaseVersion();
	}

	@Override
	public void onChange(INotifier notifier, Object changeKind, Object changedNode, Object changeArg) {
		this.reinitializeCache();
	}
}
