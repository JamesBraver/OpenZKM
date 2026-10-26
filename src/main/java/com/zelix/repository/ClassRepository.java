package com.zelix.repository;

import com.zelix.exceptions.ClasspathException;
import com.zelix.exceptions.FileException;
import com.zelix.exceptions.ProcessingException;
import com.zelix.framework.tree.ClasspathResource;
import com.zelix.framework.wrappers.LibraryClassNode;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.PhantomClassManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ClassRepository {
	private final VersionedClasspathCache mainCache;
	private final boolean defaultSearchSystem;
	private List<VersionedClasspathCache> versionedCaches;

	public ClassRepository(ClasspathManager classpathManager, boolean defaultSearchSystem) {
		this.mainCache = new VersionedClasspathCache(classpathManager, defaultSearchSystem);
		classpathManager.setClassRepository(this);
		this.defaultSearchSystem = defaultSearchSystem;
	}

	public static LibraryClassNode loadClassFromResource(ClasspathResource classpathResource) {
		String resourceName = classpathResource.getResourceName();
		int classSuffixIndex = resourceName.lastIndexOf(".class");
		if (classSuffixIndex == -1) {
			throw new FileException("\"" + resourceName + "\" doesn't end with \".class\"");
		} else {
			String internalName = resourceName.substring(0, classSuffixIndex);

			try {
				AtomicInt streamSizeRef = new AtomicInt(0);
				InputStream inputStream = classpathResource.getInputStream(streamSizeRef);
				if (inputStream != null) {
					ByteDataBuffer classDataBuffer = ByteDataBuffer.fromStream(inputStream, streamSizeRef.get());
					return new LibraryClassNode(classDataBuffer, classpathResource);
				} else {
					throw new ClasspathException(
						internalName, "Class '" + GlobalUtils.internalToBinaryName(internalName) + "' not found. Check the classpath option and reopen classes. (3a)"
					);
				}
			} catch (IOException ioException) {
				throw new FileException(internalName, "'" + internalName + "' : File error (D) : '" + ioException + "'");
			} catch (ClasspathException classpathException) {
				throw classpathException;
			} catch (ProcessingException processingException) {
				throw new FileException(processingException.getMessage());
			}
		}
	}

	public void addVersionedClasspath(ClasspathManager classpathManager) {
		if (this.versionedCaches == null) {
			this.versionedCaches = new ArrayList<>();
		}

		classpathManager.setClassRepository(this);
		VersionedClasspathCache cache = new VersionedClasspathCache(classpathManager, this.defaultSearchSystem);
		this.versionedCaches.add(cache);
	}

	public LibraryClassNode loadClass(String internalName, boolean throwIfNotFound, String context) {
		return this.loadClassInternalNoVersion(internalName, throwIfNotFound, context, false);
	}

	public LibraryClassNode findClass(String internalName) {
		try {
			return this.loadClass(internalName, false, null);
		} catch (FileException ignored) {
			return null;
		}
	}

	public void close() {
		this.mainCache.close();
		if (this.versionedCaches != null) {
			for (VersionedClasspathCache cache : this.versionedCaches) {
				cache.close();
			}
		}
	}

	public ClasspathScanner getMainClasspathScanner() {
		VersionedClasspathCache.setScannerInvalid(this.mainCache, true);
		return VersionedClasspathCache.getClasspathScanner(this.mainCache);
	}

	public LibraryClassNode findClass(String internalName, boolean searchSystem) {
		try {
			return this.loadClassInternalNoVersion(internalName, false, null, searchSystem);
		} catch (FileException ignored) {
			return null;
		}
	}

	public void removeFromCaches(String internalName, LibraryClassNode libraryClassNode) {
		VersionedClasspathCache.getClassCacheMap(this.mainCache).remove(internalName);
		if (this.versionedCaches != null) {
			for (VersionedClasspathCache cache : this.versionedCaches) {
				VersionedClasspathCache.getClassCacheMap(cache).remove(internalName);
			}
		}
	}

	public void addToCaches(String internalName, LibraryClassNode libraryClassNode) {
		VersionedClasspathCache.getClassCacheMap(this.mainCache).put(internalName, libraryClassNode);
		if (this.versionedCaches != null) {
			for (VersionedClasspathCache cache : this.versionedCaches) {
				VersionedClasspathCache.getClassCacheMap(cache).put(internalName, libraryClassNode);
			}
		}
	}

	public LibraryClassNode loadClassInternal(
		String internalName, Integer version, boolean throwIfNotFound, String context, boolean searchSystem, PhantomClassManager phantomClassManager
	) {
		if (internalName.startsWith("[")) {
			internalName = "java/lang/Object";
		}

		VersionedClasspathCache selectedCache = this.mainCache;
		if (version != null && this.versionedCaches != null) {
			for (VersionedClasspathCache cache : this.versionedCaches) {
				if (cache.getReleaseVersion() <= version) {
					selectedCache = cache;
				}
			}
		}

		LibraryClassNode cachedClass = VersionedClasspathCache.getClassCacheMap(selectedCache).get(internalName);
		if (cachedClass != null) {
			return cachedClass;
		} else {
			StringBuilder contextBuffer = new StringBuilder();
			if (context != null) {
				contextBuffer.append(" while ");
				contextBuffer.append(context);
				if (version != null) {
					contextBuffer.append(" (multi-release version ");
					contextBuffer.append(version);
					contextBuffer.append(")");
				}
			}

			ClasspathResource resource = VersionedClasspathCache.getClasspathScanner(selectedCache).findResource(internalName, searchSystem);
			Object classInputStream = null;
			int streamSize = 0;
			if (resource == null) {
				if (VersionedClasspathCache.getClasspathManager(selectedCache).isJrtInitialized()) {
					try {
						AtomicReference<Path> resourcePathRef = new AtomicReference<>();
						byte[] classBytes = VersionedClasspathCache.getClasspathManager(selectedCache).readFromJrtFileSystem(internalName, resourcePathRef);
						if (classBytes != null) {
							resource = new ClasspathResource(resourcePathRef.get(), VersionedClasspathCache.getClasspathManager(selectedCache));
							classInputStream = new ByteArrayInputStream(classBytes);
							streamSize = classBytes.length;
						}
					} catch (IOException ioExceptionJrtRead) {
						throw new FileException(internalName, "'" + internalName + "' : File error (A) : '" + ioExceptionJrtRead + "'");
					}
				}
			} else {
				AtomicInt streamSizeRef = new AtomicInt(0);

				try {
					classInputStream = resource.getInputStream(streamSizeRef);
					streamSize = streamSizeRef.get();
				} catch (IOException ioExceptionResourceRead) {
					throw new FileException(internalName, "'" + internalName + "' : File error (B) : '" + ioExceptionResourceRead + "'");
				}
			}

			if (classInputStream == null) {
				AtomicReference<String> ignoreRuleRef = new AtomicReference<>();
				if (phantomClassManager != null && phantomClassManager.isClassIgnored(internalName, ignoreRuleRef)) {
					phantomClassManager.getScriptContext()
						.logMessageInternal(
							"Class '"
								+ GlobalUtils.internalToBinaryName(internalName)
								+ "' could not be found but it has been specified as able to be ignored. : '"
								+ ignoreRuleRef.get()
								+ "'",
							true
						);
					return null;
				} else if (throwIfNotFound) {
					throw new ClasspathException(
						internalName,
						"Class '" + GlobalUtils.internalToBinaryName(internalName) + "' not found" + contextBuffer + ". Check the classpath option and reopen classes. (1a)"
					);
				} else {
					return null;
				}
			} else {
				ByteDataBuffer classDataBuffer;
				try {
					classDataBuffer = ByteDataBuffer.fromStream((InputStream)classInputStream, streamSize);
				} catch (IOException ioExceptionBufferRead) {
					throw new FileException(internalName, "'" + internalName + "' : File error (C) : " + ioExceptionBufferRead + "'");
				}

				synchronized (VersionedClasspathCache.getClassCacheMap(selectedCache)) {
					cachedClass = VersionedClasspathCache.getClassCacheMap(selectedCache).get(internalName);
					if (cachedClass != null) {
						return cachedClass;
					} else {
						LibraryClassNode classNode;
						try {
							classNode = new LibraryClassNode(classDataBuffer, resource);
							if (version != null) {
								classNode.setInnerClassOrdinal(version);
							}
						} catch (ProcessingException processingException) {
							throw new FileException(internalName, processingException.getMessage());
						}

						if (!internalName.equals(classNode.getInternalName())) {
							String resourceName = resource.getResourceName();
							String containerPath = resource.getContainerPath();
							if (containerPath != null) {
								throw new FileException(
									internalName,
									"File '"
										+ resourceName
										+ "' in '"
										+ containerPath
										+ "' contains the class '"
										+ classNode.getBinaryName()
										+ "' rather than '"
										+ GlobalUtils.internalToBinaryName(internalName)
										+ "' as expected."
								);
							} else {
								throw new FileException(
									internalName,
									"File '"
										+ resourceName
										+ "' contains the class '"
										+ classNode.getBinaryName()
										+ "' rather than '"
										+ GlobalUtils.internalToBinaryName(internalName)
										+ "' as expected."
								);
							}
						} else {
							VersionedClasspathCache.getClassCacheMap(selectedCache).put(internalName, classNode);
							return classNode;
						}
					}
				}
			}
		}
	}

	public LibraryClassNode loadClassInternalNoSystem(String internalName, Integer version, boolean throwIfNotFound, String context, PhantomClassManager phantomClassManager) {
		return this.loadClassInternal(internalName, version, throwIfNotFound, context, false, phantomClassManager);
	}

	public LibraryClassNode loadClassFromArchive(String internalName) {
		ClasspathResource resource = VersionedClasspathCache.getClasspathScanner(this.mainCache).findResourceInSelfJar(internalName);
		if (resource != null) {
			try {
				return loadClassFromResource(resource);
			} catch (FileException ignored) {
			}
		}

		return null;
	}

	public LibraryClassNode loadClassInternalWithSystem(String internalName, Integer version, boolean throwIfNotFound, String context, boolean searchSystem) {
		return this.loadClassInternal(internalName, version, throwIfNotFound, context, searchSystem, null);
	}

	public LibraryClassNode loadClassOrThrow(String internalName) {
		return this.loadClass(internalName, true, null);
	}

	public void reinitializeCaches() {
		VersionedClasspathCache.reinitializeCache(this.mainCache);
		if (this.versionedCaches != null) {
			for (VersionedClasspathCache cache : this.versionedCaches) {
				VersionedClasspathCache.reinitializeCache(cache);
			}
		}
	}

	public LibraryClassNode loadClassInternalNoVersion(String internalName, boolean throwIfNotFound, String context, boolean searchSystem) {
		return this.loadClassInternal(internalName, null, throwIfNotFound, context, searchSystem, null);
	}

	public LibraryClassNode loadClass(String internalName, boolean throwIfNotFound) {
		return this.loadClass(internalName, throwIfNotFound, null);
	}
}
