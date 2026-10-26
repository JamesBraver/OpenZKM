package com.zelix.repository;

import com.zelix.event.ChangeListener;
import com.zelix.event.EventRegistry;
import com.zelix.event.INotifier;
import com.zelix.exceptions.ClassResolveException;
import com.zelix.exceptions.ClassResolveNotFoundException;
import com.zelix.exceptions.ProcessingException;
import com.zelix.framework.attributes.UnknownAttribute;
import com.zelix.framework.tree.ClasspathResource;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.ThreeLevelMultiMap;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.utils.enumerations.ArrayEnumeration;
import com.zelix.utils.references.AtomicReference;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.HashMap;

public class ClassResolver implements ChangeListener {
	private static final Boolean BOOLEAN_TRUE = Boolean.TRUE;
	private static final Boolean BOOLEAN_FALSE = Boolean.FALSE;
	private EventRegistry eventRegistry = new EventRegistry();
	private AtomicReference<String> progressMessage = new AtomicReference<>();
	private DualKeyMap<String, String, Boolean> superclassHierarchyCache = new DualKeyMap<>();
	private DualKeyMap<String, String, Boolean> interfaceHierarchyCache = new DualKeyMap<>();
	private ClasspathManager classpathManager;
	private final boolean defaultSearchSystem;
	private ClasspathScanner classpathScanner;
	private HashMap<String, ClassNode> classCache;

	public ClassResolver(ClasspathManager classpathManager, boolean defaultSearchSystem) {
		this.classpathManager = classpathManager;
		this.defaultSearchSystem = defaultSearchSystem;
		this.classpathManager.subscribe(this);
		this.reinitializeCache();
	}

	private void reinitializeCache() {
		if (this.classpathScanner != null) {
			this.classpathScanner.close();
		}

		this.classpathScanner = new ClasspathScanner(this.classpathManager.getRawClasspath(), this.defaultSearchSystem);
		this.classCache = GlobalUtils.newHashMap();
	}

	public ClassNode resolveClass(String internalName) {
		if (internalName.startsWith("[")) {
			internalName = "java/lang/Object";
		}

		ClassNode classNode = this.classCache.get(internalName);
		if (classNode != null) {
			return classNode;
		} else {
			ClasspathResource resource = this.classpathScanner.findResource(internalName, false);
			int streamSize;
			Object classInputStream;
			if (resource == null) {
				if (!this.classpathManager.isJrtInitialized()) {
					throw new ClassResolveNotFoundException(
						internalName, "Class '" + GlobalUtils.internalToBinaryName(internalName) + "' not found. Check the classpath option and reopen classes. (4)"
					);
				}

				try {
					AtomicReference<Path> resourcePathRef = new AtomicReference<>();
					byte[] classBytes = this.classpathManager.readFromJrtFileSystem(internalName, resourcePathRef);
					if (classBytes == null) {
						throw new ClassResolveNotFoundException(
							internalName, "Class '" + GlobalUtils.internalToBinaryName(internalName) + "' not found. Check the classpath option and reopen classes. (3)"
						);
					}

					resource = new ClasspathResource(resourcePathRef.get(), this.classpathManager);
					classInputStream = new ByteArrayInputStream(classBytes);
					streamSize = classBytes.length;
				} catch (IOException ioExceptionJrtRead) {
					throw new ClassResolveNotFoundException(
						internalName, "Class '" + GlobalUtils.internalToBinaryName(internalName) + "' not found. Check the classpath option and reopen classes. (1)"
					);
				}
			} else {
				AtomicInt streamSizeRef = new AtomicInt(0);

				try {
					classInputStream = resource.getInputStream(streamSizeRef);
					streamSize = streamSizeRef.get();
				} catch (IOException ioExceptionResourceRead) {
					throw new ClassResolveNotFoundException(internalName, "'" + internalName + "' : File error (E) : '" + ioExceptionResourceRead + "'");
				}
			}

			try {
				if (classInputStream != null) {
					ByteDataBuffer classDataBuffer = ByteDataBuffer.fromStream((InputStream)classInputStream, streamSize);
					StringWriter loadLogBuffer = new StringWriter();
					PrintWriter loadLogWriter = new PrintWriter(loadLogBuffer);
					StringWriter warningLogBuffer = new StringWriter();
					PrintWriter warningLogWriter = new PrintWriter(warningLogBuffer);
					ThreeLevelMultiMap<String, String, UnknownAttribute, Object> unknownAttributes = new ThreeLevelMultiMap<>(4, 3, 5, 5, false);

					try {
						classNode = new ClassNode(classDataBuffer, resource, this.progressMessage, this.eventRegistry, loadLogWriter, warningLogWriter, unknownAttributes);
					} catch (ProcessingException processingException) {
						throw new ClassResolveException(processingException.getMessage());
					}

					if (!internalName.equals(classNode.getInternalName())) {
						String resourceName = resource.getResourceName();
						String containerPath = resource.getContainerPath();
						if (containerPath != null) {
							throw new ClassResolveException(
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
							throw new ClassResolveException(
								"File '" + resourceName + "' contains the class '" + classNode.getBinaryName() + "' rather than '" + GlobalUtils.internalToBinaryName(internalName) + "' as expected."
							);
						}
					} else {
						this.classCache.put(internalName, classNode);
						return classNode;
					}
				} else {
					throw new ClassResolveException("Class '" + GlobalUtils.internalToBinaryName(internalName) + "' not found. Check the classpath option and reopen classes. (2)");
				}
			} catch (IOException ioExceptionClassLoad) {
				throw new ClassResolveException("'" + GlobalUtils.internalToBinaryName(internalName) + "' : File error (F) : " + ioExceptionClassLoad);
			}
		}
	}

	public final boolean implementsInterface(String className, String interfaceName) {
		if (className.equals("java/lang/Object")) {
			return false;
		} else {
			Boolean cachedResult = this.interfaceHierarchyCache.get(interfaceName, className);
			if (cachedResult != null) {
				return cachedResult;
			} else {
				ClassNode classNode = this.resolveClass(className);
				ArrayEnumeration<String> interfaceNameEnumeration = new ArrayEnumeration<>(classNode.getInterfaceNames());

				while (interfaceNameEnumeration.hasMoreElements()) {
					String superInterfaceName = interfaceNameEnumeration.nextElement();
					if (superInterfaceName.equals(interfaceName)) {
						this.interfaceHierarchyCache.put(interfaceName, className, BOOLEAN_TRUE);
						return true;
					}

					if (this.implementsInterface(superInterfaceName, interfaceName)) {
						this.interfaceHierarchyCache.put(interfaceName, className, BOOLEAN_TRUE);
						return true;
					}
				}

				String superclassName = classNode.getSuperclassName();
				boolean interfaceImplemented = this.implementsInterface(superclassName, interfaceName);
				this.interfaceHierarchyCache.put(interfaceName, className, interfaceImplemented ? BOOLEAN_TRUE : BOOLEAN_FALSE);
				return interfaceImplemented;
			}
		}
	}

	public void close() {
		if (this.classCache != null) {
			this.classCache.clear();
		}

		this.superclassHierarchyCache.clearAll();
		this.interfaceHierarchyCache.clearAll();
		this.eventRegistry.clearAll();
		if (this.classpathScanner != null) {
			this.classpathScanner.close();
		}

		if (this.classpathManager != null) {
			this.classpathManager.closeJrtFileSystem();
		}
	}

	public final boolean hasSuperclass(String className, String superclassName) {
		if (className.equals("java/lang/Object")) {
			return false;
		} else {
			Boolean cachedResult = this.superclassHierarchyCache.get(superclassName, className);
			if (cachedResult != null) {
				return cachedResult;
			} else {
				String directSuperclassName = this.resolveClass(className).getSuperclassName();
				if (directSuperclassName.equals(superclassName)) {
					this.superclassHierarchyCache.put(superclassName, className, BOOLEAN_TRUE);
					return true;
				} else if (directSuperclassName.equals("java/lang/Object")) {
					this.superclassHierarchyCache.put(superclassName, className, BOOLEAN_FALSE);
					return false;
				} else {
					boolean superclassInChain = this.hasSuperclass(directSuperclassName, superclassName);
					this.superclassHierarchyCache.put(superclassName, className, superclassInChain ? BOOLEAN_TRUE : BOOLEAN_FALSE);
					return superclassInChain;
				}
			}
		}
	}

	@Override
	public void onChange(INotifier notifier, Object changeKind, Object changedNode, Object changeArg) {
		this.reinitializeCache();
	}
}
