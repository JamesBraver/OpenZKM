package com.zelix;

import com.zelix.exceptions.DebugAssertFailedException;
import com.zelix.utils.dataset.IntPair;
import com.zelix.utils.dataset.IntSize;

import java.io.*;

public class UIConfig implements Serializable {
	public static final int unusedInt1 = 1;
	public static final int unusedInt2 = 2;
	public static final int unusedInt3 = 3;
	private boolean useBytecode = true;
	private int unusedInt = 1;
	private boolean useBuildHelper = true;
	private boolean useCrossPlatformLookAndFeel = true;
	private IntPair windowPosition = new IntPair(0, 0);
	private IntSize windowSize = new IntSize(400, 300);
	private String configFileName;
	private String configDirectory;
	private boolean openNestedArchives;
	private String lastRunningPath;
	private String lastChangeLogs;
	private String lastClasspathPath;
	private String lastSavingPath;
	private String lastMappingPath;
	private String lastScriptPath;
	private String lastClassPath;

	public UIConfig() {
	}

	public UIConfig(String configFileName, String configPath) {
		this.configFileName = configPath;
		this.configDirectory = configFileName;
		File configFile = new File(configFileName, configPath);
		if (configFile.exists() && !configFile.isDirectory() && configFile.canRead()) {
			ObjectInputStream objectInputStream = null;

			try {
				FileInputStream fileInputStream = new FileInputStream(configFile);
				objectInputStream = new ObjectInputStream(fileInputStream);
				UIConfig loadedConfig = (UIConfig)objectInputStream.readObject();
				this.openNestedArchives = loadedConfig.openNestedArchives;
				this.lastRunningPath = loadedConfig.lastRunningPath;
				this.lastChangeLogs = loadedConfig.lastChangeLogs;
				this.lastClasspathPath = loadedConfig.lastClasspathPath;
				this.useBytecode = loadedConfig.useBytecode;
				this.lastSavingPath = loadedConfig.lastSavingPath;
				this.lastMappingPath = loadedConfig.lastMappingPath;
				this.lastScriptPath = loadedConfig.lastScriptPath;
				this.lastClassPath = loadedConfig.lastClassPath;
				this.unusedInt = loadedConfig.unusedInt;
				this.useBuildHelper = loadedConfig.useBuildHelper;
				this.useCrossPlatformLookAndFeel = loadedConfig.useCrossPlatformLookAndFeel;
				this.windowPosition = loadedConfig.windowPosition;
				this.windowSize = loadedConfig.windowSize;
			} catch (IOException ignored) {
			} catch (ClassNotFoundException classNotFoundException) {
			} catch (ClassCastException classCastException) {
			} catch (DebugAssertFailedException debugAssertFailedException) {
				throw debugAssertFailedException;
			} catch (Throwable throwable) {
			} finally {
				if (objectInputStream != null) {
					try {
						objectInputStream.close();
					} catch (IOException closeException) {
					}
				}
			}
		}
	}

	public synchronized int getUnusedInt() {
		return this.unusedInt;
	}

	public synchronized void setOpenNestedArchives(boolean openNestedArchives) {
		this.openNestedArchives = openNestedArchives;
	}

	public synchronized void setUseCrossPlatformLaf(boolean useCrossPlatformLookAndFeel) {
		this.useCrossPlatformLookAndFeel = useCrossPlatformLookAndFeel;
	}

	public synchronized void setUnusedInt(int unusedInt) {
		this.unusedInt = unusedInt;
	}

	public synchronized void setUseBytecode(boolean useBytecode) {
		this.useBytecode = useBytecode;
	}

	public synchronized void setWindowPosition(int x, int y) {
		this.windowPosition = new IntPair(x, y);
	}

	public synchronized void setLastChangeLogs(String lastChangeLogs) {
		this.lastChangeLogs = lastChangeLogs;
	}

	private synchronized void save(String configFileName, String configPath) {
		File configFile = new File(configFileName, configPath);
		if (!configFile.exists() || !configFile.isDirectory() && configFile.canWrite()) {
			ObjectOutputStream objectOutputStream = null;

			try {
				FileOutputStream fileOutputStream = new FileOutputStream(configFile);
				objectOutputStream = new ObjectOutputStream(fileOutputStream);
				objectOutputStream.writeObject(this);
			} catch (IOException ignored) {
			} finally {
				if (objectOutputStream != null) {
					try {
						objectOutputStream.close();
					} catch (IOException closeException) {
					}
				}
			}
		}
	}

	public synchronized String getLastChangeLogs() {
		return this.lastChangeLogs;
	}

	public synchronized void setLastRunningPath(String lastRunningPath) {
		this.lastRunningPath = lastRunningPath;
	}

	public synchronized IntPair getWindowPosition() {
		return this.windowPosition;
	}

	public synchronized void setWindowSize(int width, int height) {
		this.windowSize = new IntSize(width, height);
	}

	public synchronized void setUseBuildHelper(boolean useBuildHelper) {
		this.useBuildHelper = useBuildHelper;
	}

	public synchronized IntSize getWindowSize() {
		return this.windowSize;
	}

	public synchronized void setLastClassPath(String lastClassPath) {
		this.lastClassPath = lastClassPath;
	}

	public synchronized String getLastRunningPath() {
		return this.lastRunningPath;
	}

	public synchronized void setLastClasspathPath(String lastClasspathPath) {
		this.lastClasspathPath = lastClasspathPath;
	}

	public synchronized void setLastSavingPath(String lastSavingPath) {
		this.lastSavingPath = lastSavingPath;
	}

	public synchronized String getLastClassPath() {
		return this.lastClassPath;
	}

	public synchronized String getLastClasspathPath() {
		return this.lastClasspathPath;
	}

	public synchronized void setLastScriptPath(String lastScriptPath) {
		this.lastScriptPath = lastScriptPath;
	}

	public synchronized void setLastMappingPath(String lastMappingPath) {
		this.lastMappingPath = lastMappingPath;
	}

	public synchronized String getLastSavingPath() {
		return this.lastSavingPath;
	}

	public synchronized String getLastScriptPath() {
		return this.lastScriptPath;
	}

	public synchronized boolean getUseBytecode() {
		return this.useBytecode;
	}

	public synchronized boolean getUseBuildHelper() {
		return this.useBuildHelper;
	}

	public synchronized boolean getUseCrossPlatformLaf() {
		return this.useCrossPlatformLookAndFeel;
	}

	public synchronized void saveConfig() {
		this.save(this.configDirectory, this.configFileName);
	}

	public synchronized String getLastMappingPath() {
		return this.lastMappingPath;
	}

	public synchronized boolean getOpenNestedArchives() {
		return this.openNestedArchives;
	}
}
