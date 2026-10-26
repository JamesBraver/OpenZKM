package com.zelix.framework.writer.base;

import com.zelix.enums.SymbolType;
import com.zelix.exceptions.MissingMemberException;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.LibraryClassNode;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.NameAndTypeWriter;
import com.zelix.framework.writer.reflect.MethodKind;
import com.zelix.framework.writer.reflect.ReflectionLookup;
import com.zelix.framework.writer.reflect.ReflectionMethodPattern;
import com.zelix.framework.writer.reflect.ReflectionRewriteSnippet;
import com.zelix.framework.writer.reflect.RewriteScope;
import com.zelix.framework.writer.reflect.snippets.AtomicFieldUpdaterNewUpdaterSnippet;
import com.zelix.framework.writer.reflect.snippets.ClassForNameSnippet;
import com.zelix.framework.writer.reflect.snippets.ClassForNameWithClassLoaderSnippet;
import com.zelix.framework.writer.reflect.snippets.ClassGetFieldSnippet;
import com.zelix.framework.writer.reflect.snippets.ClassGetMethodSnippet;
import com.zelix.framework.writer.reflect.snippets.ClassLoaderLoadClassSnippet;
import com.zelix.framework.writer.reflect.snippets.LookupFindConstructorSnippet;
import com.zelix.framework.writer.reflect.snippets.LookupFindFieldSnippet;
import com.zelix.framework.writer.reflect.snippets.LookupFindMethodSnippet;
import com.zelix.klassmaster.config.Options;
import com.zelix.repository.ClassProvider;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.PhantomClassManager;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractMethodCallWriter extends MemberTypeWriter {
	private static ReflectionRewriteSnippet CLASS_FOR_NAME = new ClassForNameSnippet();
	private static ReflectionRewriteSnippet LOAD_CLASS = new ClassLoaderLoadClassSnippet();
	private static ReflectionRewriteSnippet CLASS_FOR_NAME_WITH_LOADER = new ClassForNameWithClassLoaderSnippet();
	private static ReflectionRewriteSnippet GET_METHOD = new ClassGetMethodSnippet();
	private static ReflectionRewriteSnippet FIND_METHOD = new LookupFindMethodSnippet();
	private static ReflectionRewriteSnippet FIND_CONSTRUCTOR_SNIPPET = new LookupFindConstructorSnippet();
	private static ReflectionRewriteSnippet GET_FIELD_SNIPPET = new ClassGetFieldSnippet();
	private static ReflectionRewriteSnippet FIND_FIELD_SNIPPET = new LookupFindFieldSnippet();
	private static ReflectionRewriteSnippet NEW_UPDATER_SNIPPET = new AtomicFieldUpdaterNewUpdaterSnippet();
	private static ReflectionMethodPattern LAMBDA_INTERFACE_METHOD_NAME_PATTERN = new ReflectionMethodPattern(
		0, MethodKind.INSTANCE, SymbolType.FUNCTIONAL_INTERFACE_METHOD_TYPE, false, "getFunctionalInterfaceMethodName()Ljava/lang/String;"
	);
	private static ReflectionMethodPattern GET_NAME_PATTERN = new ReflectionMethodPattern(
		0, MethodKind.INSTANCE, SymbolType.OBJECT_NAME_TYPE, false, "getName()Ljava/lang/String;"
	);
	private static ReflectionMethodPattern CLASS_FOR_NAME_PATTERN = new ReflectionMethodPattern(
		1, MethodKind.STATIC, SymbolType.CLASS_TYPE, false, "forName(Ljava/lang/String;)Ljava/lang/Class;", CLASS_FOR_NAME, RewriteScope.ALL_CLASSES
	);
	private static ReflectionMethodPattern CLASS_FOR_NAME_MODULE_PATTERN = new ReflectionMethodPattern(
		2,
		MethodKind.STATIC,
		SymbolType.CLASS_TYPE,
		false,
		"forName(Ljava/lang/Module;Ljava/lang/String;)Ljava/lang/Class;",
		CLASS_FOR_NAME,
		RewriteScope.ALL_CLASSES
	);
	private static ReflectionMethodPattern CLASS_FOR_NAME_LOADER_PATTERN = new ReflectionMethodPattern(
		3,
		MethodKind.STATIC,
		SymbolType.CLASS_TYPE,
		false,
		"forName(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;",
		CLASS_FOR_NAME_WITH_LOADER,
		RewriteScope.ALL_CLASSES
	);
	private static ReflectionMethodPattern CLASS_GET_FIELD_PATTERN = new ReflectionMethodPattern(
		1,
		MethodKind.INSTANCE,
		SymbolType.FIELD_TYPE,
		true,
		"getField(Ljava/lang/String;)Ljava/lang/reflect/Field;",
		GET_FIELD_SNIPPET,
		RewriteScope.ALL_PUBLIC_FIELDS
	);
	private static ReflectionMethodPattern CLASS_GET_METHOD_PATTERN = new ReflectionMethodPattern(
		2,
		MethodKind.INSTANCE,
		SymbolType.METHOD_TYPE,
		true,
		"getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;",
		GET_METHOD,
		RewriteScope.ALL_PUBLIC_METHODS
	);
	private static ReflectionMethodPattern LOADER_LOAD_CLASS_PATTERN = new ReflectionMethodPattern(
		1, MethodKind.INSTANCE, SymbolType.CLASS_TYPE, false, "loadClass(Ljava/lang/String;)Ljava/lang/Class;", CLASS_FOR_NAME, RewriteScope.ALL_CLASSES
	);
	private static ReflectionMethodPattern LOADER_LOAD_CLASS_URL_PATTERN = new ReflectionMethodPattern(
		2,
		MethodKind.INSTANCE,
		SymbolType.CLASS_TYPE,
		false,
		"loadClass(Ljava/net/URL;Ljava/lang/String;)Ljava/lang/Class;",
		CLASS_FOR_NAME,
		RewriteScope.ALL_CLASSES
	);
	private static ReflectionMethodPattern LOADER_LOAD_CLASS_NAME_PATTERN = new ReflectionMethodPattern(
		2, MethodKind.INSTANCE, SymbolType.CLASS_TYPE, false, "loadClass(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Class;"
	);
	private static ReflectionMethodPattern LOADER_LOAD_CLASS_RESOLVE_PATTERN = new ReflectionMethodPattern(
		2, MethodKind.INSTANCE, SymbolType.CLASS_TYPE, false, "loadClass(Ljava/lang/String;Z)Ljava/lang/Class;", LOAD_CLASS, RewriteScope.ALL_CLASSES
	);
	private static ReflectionMethodPattern RMI_LOAD_CLASS_PATTERN = new ReflectionMethodPattern(
		1, MethodKind.STATIC, SymbolType.CLASS_TYPE, false, "loadClass(Ljava/lang/String;)Ljava/lang/Class;", CLASS_FOR_NAME, RewriteScope.ALL_CLASSES
	);
	private static ReflectionMethodPattern RMI_LOAD_CLASS_URL_PATTERN = new ReflectionMethodPattern(
		2, MethodKind.STATIC, SymbolType.CLASS_TYPE, false, "loadClass(Ljava/net/URL;Ljava/lang/String;)Ljava/lang/Class;", CLASS_FOR_NAME, RewriteScope.ALL_CLASSES
	);
	private static ReflectionMethodPattern RMI_LOAD_CLASS_NAME_PATTERN = new ReflectionMethodPattern(
		2,
		MethodKind.STATIC,
		SymbolType.CLASS_TYPE,
		false,
		"loadClass(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Class;",
		CLASS_FOR_NAME,
		RewriteScope.ALL_CLASSES
	);
	private static ReflectionMethodPattern RMI_LOAD_CLASS_LOADER_PATTERN = new ReflectionMethodPattern(
		2,
		MethodKind.STATIC,
		SymbolType.CLASS_TYPE,
		false,
		"loadClass(Ljava/lang/String;Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/Class;",
		LOAD_CLASS,
		RewriteScope.ALL_CLASSES
	);
	private static ReflectionMethodPattern CLASS_GET_DECLARED_FIELD_PATTERN = new ReflectionMethodPattern(
		1,
		MethodKind.INSTANCE,
		SymbolType.FIELD_TYPE,
		true,
		"getDeclaredField(Ljava/lang/String;)Ljava/lang/reflect/Field;",
		GET_FIELD_SNIPPET,
		RewriteScope.ALL_FIELDS_OF_CLASS
	);
	private static ReflectionMethodPattern CLASS_GET_DECLARED_METHOD_PATTERN = new ReflectionMethodPattern(
		2,
		MethodKind.INSTANCE,
		SymbolType.METHOD_TYPE,
		true,
		"getDeclaredMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;",
		GET_METHOD,
		RewriteScope.ALL_METHODS_OF_CLASS
	);
	private static ReflectionMethodPattern LOADER_DEFINE_CLASS_PATTERN = new ReflectionMethodPattern(
		4, MethodKind.INSTANCE, SymbolType.CLASS_TYPE, false, "defineClass(Ljava/lang/String;[BII)Ljava/lang/Class;"
	);
	private static ReflectionMethodPattern LOADER_DEFINE_CLASS_DOMAIN_PATTERN = new ReflectionMethodPattern(
		5, MethodKind.INSTANCE, SymbolType.CLASS_TYPE, false, "defineClass(Ljava/lang/String;[BIILjava/security/ProtectionDomain;)Ljava/lang/Class;"
	);
	private static ReflectionMethodPattern LOADER_FIND_CLASS_PATTERN = new ReflectionMethodPattern(
		1, MethodKind.INSTANCE, SymbolType.CLASS_TYPE, false, "findClass(Ljava/lang/String;)Ljava/lang/Class;", CLASS_FOR_NAME, RewriteScope.ALL_CLASSES
	);
	private static ReflectionMethodPattern LOADER_FIND_SYSTEM_CLASS_PATTERN = new ReflectionMethodPattern(
		1, MethodKind.INSTANCE, SymbolType.CLASS_TYPE, false, "findSystemClass(Ljava/lang/String;)Ljava/lang/Class;", CLASS_FOR_NAME, RewriteScope.ALL_CLASSES
	);
	private static ReflectionMethodPattern LOADER_FIND_LOADED_CLASS_PATTERN = new ReflectionMethodPattern(
		1, MethodKind.INSTANCE, SymbolType.CLASS_TYPE, false, "findLoadedClass(Ljava/lang/String;)Ljava/lang/Class;", CLASS_FOR_NAME, RewriteScope.ALL_CLASSES
	);
	private static ReflectionMethodPattern EVENT_SET_DESC_INIT_PATTERN_1 = new ReflectionMethodPattern(
		4, MethodKind.CONSTRUCTOR, SymbolType.OBJECT_TYPE, false, "<init>(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/String;)V"
	);
	private static ReflectionMethodPattern EVENT_SET_DESC_INIT_PATTERN_2 = new ReflectionMethodPattern(
		6,
		MethodKind.CONSTRUCTOR,
		SymbolType.OBJECT_TYPE,
		false,
		"<init>(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;[Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"
	);
	private static ReflectionMethodPattern EVENT_SET_DESC_INIT_PATTERN_3 = new ReflectionMethodPattern(
		7,
		MethodKind.CONSTRUCTOR,
		SymbolType.OBJECT_TYPE,
		false,
		"<init>(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;[Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"
	);
	private static ReflectionMethodPattern BUNDLE_GET_BUNDLE_PATTERN = new ReflectionMethodPattern(
		1,
		MethodKind.STATIC,
		SymbolType.BUNDLE_TYPE,
		false,
		"getBundle(Ljava/lang/String;)Ljava/util/ResourceBundle;",
		CLASS_FOR_NAME,
		RewriteScope.ALL_LIST_RESOUCE_BUNDLE
	);
	private static ReflectionMethodPattern BUNDLE_GET_BUNDLE_LOCALE_PATTERN = new ReflectionMethodPattern(
		2,
		MethodKind.STATIC,
		SymbolType.BUNDLE_TYPE,
		false,
		"getBundle(Ljava/lang/String;Ljava/util/Locale;)Ljava/util/ResourceBundle;",
		LOAD_CLASS,
		RewriteScope.ALL_LIST_RESOUCE_BUNDLE
	);
	private static ReflectionMethodPattern BUNDLE_GET_BUNDLE_LOADER_PATTERN = new ReflectionMethodPattern(
		3,
		MethodKind.STATIC,
		SymbolType.BUNDLE_TYPE,
		false,
		"getBundle(Ljava/lang/String;Ljava/util/Locale;Ljava/lang/ClassLoader;)Ljava/util/ResourceBundle;",
		CLASS_FOR_NAME_WITH_LOADER,
		RewriteScope.ALL_LIST_RESOUCE_BUNDLE
	);
	private static ReflectionMethodPattern BEANS_INSTANTIATE_PATTERN = new ReflectionMethodPattern(
		2,
		MethodKind.STATIC,
		SymbolType.DEFAULT_CONSTRUCTOR_CALL_TYPE,
		false,
		"instantiate(Ljava/lang/ClassLoader;Ljava/lang/String;)Ljava/lang/Object;",
		CLASS_FOR_NAME,
		RewriteScope.ALL_CLASSES
	);
	private static ReflectionMethodPattern BEANS_INSTANTIATE_CONTEXT_PATTERN = new ReflectionMethodPattern(
		3,
		MethodKind.STATIC,
		SymbolType.DEFAULT_CONSTRUCTOR_CALL_TYPE,
		false,
		"instantiate(Ljava/lang/ClassLoader;Ljava/lang/String;Ljava/beans/beancontext/BeanContext;)Ljava/lang/Object;",
		LOAD_CLASS,
		RewriteScope.ALL_CLASSES
	);
	private static ReflectionMethodPattern BEANS_INSTANTIATE_APPLET_PATTERN = new ReflectionMethodPattern(
		4,
		MethodKind.STATIC,
		SymbolType.DEFAULT_CONSTRUCTOR_CALL_TYPE,
		false,
		"instantiate(Ljava/lang/ClassLoader;Ljava/lang/String;Ljava/beans/beancontext/BeanContext;Ljava/beans/AppletInitializer;)Ljava/lang/Object;",
		CLASS_FOR_NAME_WITH_LOADER,
		RewriteScope.ALL_LIST_RESOUCE_BUNDLE
	);
	private static ReflectionMethodPattern BEAN_CONTEXT_INSTANTIATE_CHILD_PATTERN = new ReflectionMethodPattern(
		1,
		MethodKind.INSTANCE,
		SymbolType.DEFAULT_CONSTRUCTOR_CALL_TYPE,
		false,
		"instantiateChild(Ljava/lang/String;)Ljava/lang/Object;",
		CLASS_FOR_NAME,
		RewriteScope.ALL_CLASSES
	);
	private static ReflectionMethodPattern CLASS_NEW_INSTANCE_PATTERN = new ReflectionMethodPattern(
		0, MethodKind.INSTANCE, SymbolType.DEFAULT_CONSTRUCTOR_CALL_TYPE, true, true, "newInstance()Ljava/lang/Object;"
	);
	private static ReflectionMethodPattern CLASS_GET_CONSTRUCTOR_PATTERN = new ReflectionMethodPattern(
		0, MethodKind.INSTANCE, SymbolType.SPECIFIC_CONSTRUCTOR_CALL_TYPE, true, true, "getConstructor([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;"
	);
	private static ReflectionMethodPattern CLASS_GET_CONSTRUCTORS_PATTERN = new ReflectionMethodPattern(
		1, MethodKind.INSTANCE, SymbolType.ALL_CONSTRUCTORS_CALL_TYPE, true, true, "getConstructors()[Ljava/lang/reflect/Constructor;"
	);
	private static ReflectionMethodPattern PACKAGE_GET_PACKAGE_PATTERN = new ReflectionMethodPattern(
		1, MethodKind.STATIC, SymbolType.PACKAGE_TYPE, false, "getPackage(Ljava/lang/String;)Ljava/lang/Package;"
	);
	private static ReflectionMethodPattern LOADER_GET_PACKAGE_PATTERN = new ReflectionMethodPattern(
		1, MethodKind.INSTANCE, SymbolType.PACKAGE_TYPE, false, "getPackage(Ljava/lang/String;)Ljava/lang/Package;"
	);
	private static ReflectionMethodPattern LOOKUP_FIND_STATIC_PATTERN = new ReflectionMethodPattern(
		3,
		MethodKind.INSTANCE,
		SymbolType.METHOD_HANDLE_TYPE,
		false,
		true,
		"findStatic(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;",
		FIND_METHOD,
		RewriteScope.ALL_STATIC_METHODS_ACCESSIBLE_FROM_CLASS
	);
	private static ReflectionMethodPattern LOOKUP_FIND_VIRTUAL_PATTERN = new ReflectionMethodPattern(
		3,
		MethodKind.INSTANCE,
		SymbolType.METHOD_HANDLE_TYPE,
		false,
		true,
		"findVirtual(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;",
		FIND_METHOD,
		RewriteScope.ALL_VIRTUAL_METHODS_ACCESSIBLE_FROM_CLASS
	);
	private static ReflectionMethodPattern LOOKUP_FIND_SPECIAL_PATTERN = new ReflectionMethodPattern(
		4,
		MethodKind.INSTANCE,
		SymbolType.METHOD_HANDLE_TYPE,
		false,
		true,
		"findSpecial(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;",
		FIND_CONSTRUCTOR_SNIPPET,
		RewriteScope.ALL_VIRTUAL_METHODS_ACCESSIBLE_FROM_CLASS
	);
	private static ReflectionMethodPattern LOOKUP_FIND_CONSTRUCTOR_PATTERN = new ReflectionMethodPattern(
		2,
		MethodKind.INSTANCE,
		SymbolType.METHOD_HANDLE_TYPE,
		false,
		true,
		"findConstructor(Ljava/lang/Class;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;"
	);
	private static ReflectionMethodPattern LOOKUP_FIND_GETTER_PATTERN = new ReflectionMethodPattern(
		3,
		MethodKind.INSTANCE,
		SymbolType.METHOD_HANDLE_TYPE,
		false,
		true,
		"findGetter(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;",
		FIND_FIELD_SNIPPET,
		RewriteScope.ALL_INSTANCE_FIELDS_ACCESSIBLE_FROM_CLASS
	);
	private static ReflectionMethodPattern LOOKUP_FIND_SETTER_PATTERN = new ReflectionMethodPattern(
		3,
		MethodKind.INSTANCE,
		SymbolType.METHOD_HANDLE_TYPE,
		false,
		true,
		"findSetter(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;",
		FIND_FIELD_SNIPPET,
		RewriteScope.ALL_INSTANCE_FIELDS_ACCESSIBLE_FROM_CLASS
	);
	private static ReflectionMethodPattern LOOKUP_FIND_STATIC_GETTER_PATTERN = new ReflectionMethodPattern(
		3,
		MethodKind.INSTANCE,
		SymbolType.METHOD_HANDLE_TYPE,
		false,
		true,
		"findStaticGetter(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;",
		FIND_FIELD_SNIPPET,
		RewriteScope.ALL_STATIC_FIELDS_ACCESSIBLE_FROM_CLASS
	);
	private static ReflectionMethodPattern LOOKUP_FIND_STATIC_SETTER_PATTERN = new ReflectionMethodPattern(
		3,
		MethodKind.INSTANCE,
		SymbolType.METHOD_HANDLE_TYPE,
		false,
		true,
		"findStaticSetter(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;",
		FIND_FIELD_SNIPPET,
		RewriteScope.ALL_STATIC_FIELDS_ACCESSIBLE_FROM_CLASS
	);
	private static ReflectionMethodPattern LOOKUP_FIND_VAR_HANDLE_PATTERN = new ReflectionMethodPattern(
		3,
		MethodKind.INSTANCE,
		SymbolType.METHOD_HANDLE_TYPE,
		false,
		true,
		"findVarHandle(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/VarHandle;",
		FIND_FIELD_SNIPPET,
		RewriteScope.ALL_INSTANCE_FIELDS_ACCESSIBLE_FROM_CLASS
	);
	private static ReflectionMethodPattern LOOKUP_FIND_STATIC_VAR_HANDLE_PATTERN = new ReflectionMethodPattern(
		3,
		MethodKind.INSTANCE,
		SymbolType.METHOD_HANDLE_TYPE,
		false,
		true,
		"findStaticVarHandle(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/VarHandle;",
		FIND_FIELD_SNIPPET,
		RewriteScope.ALL_STATIC_FIELDS_ACCESSIBLE_FROM_CLASS
	);
	private static ReflectionMethodPattern LOOKUP_BIND_PATTERN = new ReflectionMethodPattern(
		3,
		MethodKind.INSTANCE,
		SymbolType.METHOD_HANDLE_TYPE,
		false,
		true,
		"bind(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;",
		FIND_METHOD,
		RewriteScope.ALL_VIRTUAL_METHODS_ACCESSIBLE_FROM_CLASS
	);
	private static ReflectionMethodPattern ATOMIC_REF_UPDATER_PATTERN = new ReflectionMethodPattern(
		3,
		MethodKind.STATIC,
		SymbolType.FIELD_TYPE,
		false,
		true,
		"newUpdater(Ljava/lang/Class;Ljava/lang/Class;Ljava/lang/String;)Ljava/util/concurrent/atomic/AtomicReferenceFieldUpdater;",
		NEW_UPDATER_SNIPPET,
		RewriteScope.ALL_INSTANCE_FIELDS_OF_CLASS
	);
	public static ReflectionMethodPattern ATOMIC_INT_UPDATER_PATTERN = new ReflectionMethodPattern(
		2,
		MethodKind.STATIC,
		SymbolType.FIELD_TYPE,
		false,
		true,
		"newUpdater(Ljava/lang/Class;Ljava/lang/String;)Ljava/util/concurrent/atomic/AtomicIntegerFieldUpdater;",
		GET_FIELD_SNIPPET,
		RewriteScope.ALL_INSTANCE_FIELDS_OF_CLASS
	);
	public static ReflectionMethodPattern ATOMIC_LONG_UPDATER_PATTERN = new ReflectionMethodPattern(
		2,
		MethodKind.STATIC,
		SymbolType.FIELD_TYPE,
		false,
		true,
		"newUpdater(Ljava/lang/Class;Ljava/lang/String;)Ljava/util/concurrent/atomic/AtomicLongFieldUpdater;",
		GET_FIELD_SNIPPET,
		RewriteScope.ALL_INSTANCE_FIELDS_OF_CLASS
	);
	private static ReflectionMethodPattern METHOD_TYPE_PATTERN_1 = new ReflectionMethodPattern(
		1, MethodKind.STATIC, SymbolType.METHOD_TYPE_TYPE, false, false, "methodType(Ljava/lang/Class;)Ljava/lang/invoke/MethodType;"
	);
	private static ReflectionMethodPattern METHOD_TYPE_PATTERN_2 = new ReflectionMethodPattern(
		2, MethodKind.STATIC, SymbolType.METHOD_TYPE_TYPE, false, false, "methodType(Ljava/lang/Class;Ljava/lang/Class;)Ljava/lang/invoke/MethodType;"
	);
	private static ReflectionMethodPattern METHOD_TYPE_PATTERN_3 = new ReflectionMethodPattern(
		2, MethodKind.STATIC, SymbolType.METHOD_TYPE_TYPE, false, false, "methodType(Ljava/lang/Class;[Ljava/lang/Class;)Ljava/lang/invoke/MethodType;"
	);
	private static ReflectionMethodPattern METHOD_TYPE_PATTERN_4 = new ReflectionMethodPattern(
		3,
		MethodKind.STATIC,
		SymbolType.METHOD_TYPE_TYPE,
		false,
		false,
		"methodType(Ljava/lang/Class;Ljava/lang/Class;[Ljava/lang/Class;)Ljava/lang/invoke/MethodType;"
	);
	private static ReflectionMethodPattern METHOD_TYPE_PATTERN_5 = new ReflectionMethodPattern(
		2,
		MethodKind.STATIC,
		SymbolType.METHOD_TYPE_TYPE,
		false,
		false,
		"methodType(Ljava/lang/Class;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodType;",
		null,
		RewriteScope.NONE
	);
	private static DualKeyMap<String, String, ReflectionMethodPattern> REFLECTION_PATTERN_REGISTRY = new DualKeyMap<>(21, 5);
	private static HashSet<String> INHERITANCE_LOOKUP_SIGNATURES = GlobalUtils.newHashSet(31);

	public AbstractMethodCallWriter(
		AbstractMemberRefInfoWriter memberRefInfoWriter, ClassInfoWriter owner, NameAndTypeWriter target, MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences
	) {
		super(memberRefInfoWriter, owner, target, classReferences);
	}

	public AbstractMethodCallWriter(int index, AbstractConstantPoolWriter constantPool, ClassInfoWriter owner, NameAndTypeWriter target, AbstractMethodNode targetMember) {
		super(index, constantPool, owner, target, targetMember);
	}

	public AbstractMethodCallWriter(
		int index, AbstractConstantPoolWriter constantPool, ClassInfoWriter owner, NameAndTypeWriter target, AbstractWorkspace workspace, ClassProvider classProvider
	) {
		super(index, constantPool, owner, target, workspace, classProvider, true);
	}

	public static AbstractMethodNode findMethodInClass(AbstractClassNode classNode, MethodSignature methodSignature, AbstractWorkspace workspace) {
		return (AbstractMethodNode)(classNode.isWorkspaceNode() && !classNode.isInnerClass() ? workspace.getMethodBySignature((ClassNode)classNode, methodSignature) : classNode.findMethod(methodSignature));
	}

	public static AbstractMethodNode findMethodInInterfaces(
		AbstractClassNode classNode,
		MethodSignature methodSignature,
		Integer innerClassOrdinal,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		HashSet<AbstractClassNode> searchedClasses,
		String lookupMessage,
		PhantomClassManager phantomClassManager
	) {
		Integer resolvedInnerClassOrdinal = classNode.hasInnerClassOrdinal() ? classNode.getInnerClassOrdinal() : innerClassOrdinal;
		String[] interfaceNames = classNode.getInterfaceNames();

		for (int i = 0; i < interfaceNames.length; i++) {
			AbstractClassNode interfaceClassNode = classProvider.getClassNode(interfaceNames[i], resolvedInnerClassOrdinal, lookupMessage, phantomClassManager);
			if (interfaceClassNode != null) {
				AbstractMethodNode foundMethod = findMethodInClass(interfaceClassNode, methodSignature, workspace);
				if (foundMethod != null) {
					return foundMethod;
				}

				if (searchedClasses.add(interfaceClassNode)) {
					foundMethod = findMethodInInterfaces(interfaceClassNode, methodSignature, innerClassOrdinal, workspace, classProvider, searchedClasses, lookupMessage, phantomClassManager);
					if (foundMethod != null) {
						return foundMethod;
					}
				}
			}
		}

		return null;
	}

	public AbstractMethodNode resolveTargetMethod(
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		AtomicReference<String> searchedClassName,
		AtomicReference<AbstractClassNode> ownerClass,
		AtomicReference<Set<AbstractClassNode>> searchedClasses,
		PhantomClassManager phantomClassManager
	) {
		String methodName = this.getName();
		String methodDescriptor = this.getDescriptor();
		MethodSignature methodSignature = new MethodSignature(methodName, methodDescriptor);
		AbstractMethodNode resolvedMethod = null;
		String ownerName = this.getOwnerName();
		String currentOwnerName = ownerName;
		AbstractClassNode ownerClassNode = this.getOwnerClassNode();
		Integer ownerInnerClassOrdinal = ownerClassNode.hasInnerClassOrdinal() ? ownerClassNode.getInnerClassOrdinal() : null;
		if (MethodSignature.isHandleMethod(ownerName, methodName, workspace) && this.getOwnerClassNode().isJava7OrNewer()) {
			LibraryClassNode ownerLibraryClassNode = (LibraryClassNode)classProvider.getClassNode(
				ownerName, ownerInnerClassOrdinal, "looking for class '" + ownerName + "' which is referenced in class '" + this.getOwnerRawSourcePath() + "'"
			);
			return ownerLibraryClassNode.getOrCreateStubMethod(methodSignature);
		} else {
			AbstractClassNode searchStartClassNode = null;
			HashSet<AbstractClassNode> visitedClasses = null;
			if (ownerName.startsWith("[")) {
				currentOwnerName = "java/lang/Object";
			}

			String lookupMessage = "looking for method '"
				+ methodSignature.toSignatureString(null)
				+ "' in class '"
				+ GlobalUtils.internalToBinaryName(ownerName)
				+ "' which is referenced in class '"
				+ this.getOwnerRawSourcePath()
				+ "'";
			AbstractClassNode currentClassNode = classProvider.getClassNode(currentOwnerName, ownerInnerClassOrdinal, lookupMessage, phantomClassManager);

			while (resolvedMethod == null && currentClassNode != null) {
				resolvedMethod = findMethodInClass(currentClassNode, methodSignature, workspace);
				if (resolvedMethod == null) {
					if (searchStartClassNode == null) {
						searchStartClassNode = currentClassNode;
						visitedClasses = GlobalUtils.newHashSet();
					}

					visitedClasses.add(currentClassNode);
				}

				currentOwnerName = currentClassNode.getSuperclassName();
				if (currentOwnerName == null) {
					break;
				}

				Integer superclassInnerClassOrdinal = currentClassNode.hasInnerClassOrdinal() ? currentClassNode.getInnerClassOrdinal() : ownerInnerClassOrdinal;
				currentClassNode = classProvider.getClassNode(currentOwnerName, superclassInnerClassOrdinal, lookupMessage, phantomClassManager);
			}

			if (resolvedMethod == null) {
				currentClassNode = searchStartClassNode;

				while (resolvedMethod == null && currentClassNode != null) {
					resolvedMethod = findMethodInInterfaces(currentClassNode, methodSignature, ownerInnerClassOrdinal, workspace, classProvider, visitedClasses, lookupMessage, phantomClassManager);
					if (resolvedMethod == null) {
						currentOwnerName = currentClassNode.getSuperclassName();
						if (currentOwnerName == null) {
							break;
						}

						Integer interfaceSearchInnerClassOrdinal = currentClassNode.hasInnerClassOrdinal() ? currentClassNode.getInnerClassOrdinal() : ownerInnerClassOrdinal;
						currentClassNode = classProvider.getClassNode(currentOwnerName, interfaceSearchInnerClassOrdinal, lookupMessage, phantomClassManager);
					}
				}
			}

			searchedClassName.set(currentOwnerName);
			ownerClass.set(searchStartClassNode);
			searchedClasses.set(visitedClasses);
			return resolvedMethod;
		}
	}

	public ReflectionMethodPattern getPatternFromRegistry() {
		String signatureKey = this.getSignatureKey();
		String ownerName = this.getOwnerName();
		return REFLECTION_PATTERN_REGISTRY.get(signatureKey, ownerName);
	}

	public List<ClassNode> getReferencedWorkspaceClasses() {
		return TypeWriter.getReferencedWorkspaceClasses(this.target.getDescriptor());
	}

	public String getSignatureKey() {
		return this.getName() + this.getDescriptor();
	}

	public final boolean isConstructorCall() {
		return this.target.nameUtf8Entry.getValue().equals("<init>");
	}

	public List<String> getParameterDescriptors() {
		return TypeWriter.parseParameterDescriptors(this.target.getDescriptor());
	}

	public ReflectionMethodPattern findPatternWithInheritance(AbstractWorkspace workspace) {
		String signatureKey = this.getSignatureKey();
		String ownerName = this.getOwnerName();
		Map<String, ReflectionMethodPattern> patternsByOwnerName = REFLECTION_PATTERN_REGISTRY.getGroup(signatureKey);
		if (patternsByOwnerName == null) {
			return null;
		} else if (patternsByOwnerName.containsKey(ownerName)) {
			return patternsByOwnerName.get(ownerName);
		} else if (!INHERITANCE_LOOKUP_SIGNATURES.contains(signatureKey)) {
			return null;
		} else {
			for (String candidateOwnerName : patternsByOwnerName.keySet()) {
				if (!candidateOwnerName.equals("java/rmi/server/LoaderHandler") && !candidateOwnerName.equals("java/beans/beancontext/BeanContext")) {
					if (workspace.isClassInstanceOf(ownerName, candidateOwnerName)) {
						return patternsByOwnerName.get(candidateOwnerName);
					}
				} else if (workspace.isSubclassOf(ownerName, candidateOwnerName)) {
					return patternsByOwnerName.get(candidateOwnerName);
				}
			}

			return null;
		}
	}

	public final String getStackReturnType() {
		return getStackReturnType(this.target);
	}

	public int getReturnStackSize() {
		return TypeWriter.getReturnDescriptor(this.target.getDescriptor()).equals("V") ? 0 : 1;
	}

	public final int getParameterStackSize() {
		List<String> parameterDescriptors = TypeWriter.parseParameterDescriptors(this.target.getDescriptor());
		int parameterCount = parameterDescriptors.size();
		int stackSize = parameterCount;

		for (int i = 0; i < parameterCount; i++) {
			String parameterDescriptor = parameterDescriptors.get(i);
			if (parameterDescriptor.equals("J") || parameterDescriptor.equals("D")) {
				stackSize++;
			}
		}

		return stackSize;
	}

	@Override
	public final void resolveTargetMember(AbstractWorkspace workspace, ClassProvider classProvider, PhantomClassManager phantomClassManager) {
		MethodSignature methodSignature = new MethodSignature(this.getName(), this.getDescriptor());
		AtomicReference<AbstractClassNode> ownerClassRef = new AtomicReference<>();
		AtomicReference<Set<AbstractClassNode>> searchedClassesRef = new AtomicReference<>();
		this.targetMember = this.resolveTargetMethod(workspace, classProvider, new AtomicReference<>(), ownerClassRef, searchedClassesRef, phantomClassManager);
		AbstractClassNode ownerClassNode = ownerClassRef.get();
		Set<AbstractClassNode> searchedClasses = searchedClassesRef.get();
		if (this.targetMember == null) {
			if (ownerClassNode != null) {
				AtomicReference<String> searchedClassNameRef = new AtomicReference<>();
				if ((!Options.ZKM_IGNORE_MISSING_MEMBERS || ownerClassNode.isWorkspaceNode())
					&& (phantomClassManager == null || !phantomClassManager.isMethodIgnored(ownerClassNode, this.getName(), this.getDescriptor(), searchedClassNameRef))) {
					throw new MissingMemberException(
						"Could not find method '"
							+ methodSignature.toFullHumanReadableString()
							+ "' in class '"
							+ ownerClassNode.getSourcePath()
							+ "' ("
							+ ownerClassNode.getBinaryName()
							+ ") or in its hierarchy. Such a reference occurs in class '"
							+ this.getOwnerSourcePath()
							+ "'. Please check the classpath and reopen your classes (A)."
					);
				}

				if (!searchedClassNameRef.isNull() && phantomClassManager != null) {
					phantomClassManager.getScriptContext()
						.logMessageInternal(
							"Method '"
								+ methodSignature.toFullHumanReadableString()
								+ "' in class '"
								+ ownerClassNode.getBinaryName()
								+ "' could not be found but it has been specified as able to be ignored. : '"
								+ searchedClassNameRef.get()
								+ "'",
							true
						);
				}
			}
		} else if (searchedClasses != null && this.targetMember.isStatic()) {
			for (AbstractClassNode searchedClassNode : searchedClasses) {
				if (searchedClassNode.isWorkspaceNode()) {
					((ClassNode)searchedClassNode).addMethodClassReference((AbstractMethodNode)this.targetMember, ownerClassNode);
				}
			}
		}
	}

	public final List<String> getStackParameterTypes() {
		return getStackParameterTypes(this.target);
	}

	static {
		CLASS_FOR_NAME_PATTERN.addParameter(0, ReflectionLookup.CLASS_NAME_PARAM_TYPE, false);
		CLASS_FOR_NAME_MODULE_PATTERN.addParameter(1, ReflectionLookup.CLASS_NAME_PARAM_TYPE, false);
		CLASS_FOR_NAME_LOADER_PATTERN.addParameter(0, ReflectionLookup.CLASS_NAME_PARAM_TYPE, false);
		CLASS_GET_FIELD_PATTERN.addParameter(0, ReflectionLookup.FIELD_NAME_PARAM_TYPE, false);
		CLASS_GET_METHOD_PATTERN.addParameter(0, ReflectionLookup.METHOD_NAME_PARAM_TYPE, false);
		CLASS_GET_METHOD_PATTERN.addParameter(1, ReflectionLookup.CLASS_PARAM_TYPE, true);
		LOADER_LOAD_CLASS_PATTERN.addParameter(0, ReflectionLookup.CLASS_NAME_PARAM_TYPE, false);
		LOADER_LOAD_CLASS_URL_PATTERN.addParameter(1, ReflectionLookup.CLASS_NAME_PARAM_TYPE, false);
		LOADER_LOAD_CLASS_NAME_PATTERN.addParameter(1, ReflectionLookup.CLASS_NAME_PARAM_TYPE, false);
		LOADER_LOAD_CLASS_RESOLVE_PATTERN.addParameter(0, ReflectionLookup.CLASS_NAME_PARAM_TYPE, false);
		RMI_LOAD_CLASS_PATTERN.addParameter(0, ReflectionLookup.CLASS_NAME_PARAM_TYPE, false);
		RMI_LOAD_CLASS_URL_PATTERN.addParameter(1, ReflectionLookup.CLASS_NAME_PARAM_TYPE, false);
		RMI_LOAD_CLASS_NAME_PATTERN.addParameter(1, ReflectionLookup.CLASS_NAME_PARAM_TYPE, false);
		RMI_LOAD_CLASS_LOADER_PATTERN.addParameter(1, ReflectionLookup.CLASS_NAME_PARAM_TYPE, false);
		CLASS_GET_DECLARED_FIELD_PATTERN.addParameter(0, ReflectionLookup.FIELD_NAME_PARAM_TYPE, false);
		CLASS_GET_DECLARED_METHOD_PATTERN.addParameter(0, ReflectionLookup.METHOD_NAME_PARAM_TYPE, false);
		CLASS_GET_DECLARED_METHOD_PATTERN.addParameter(1, ReflectionLookup.CLASS_PARAM_TYPE, true);
		LOADER_DEFINE_CLASS_PATTERN.addParameter(0, ReflectionLookup.CLASS_NAME_PARAM_TYPE, false);
		LOADER_DEFINE_CLASS_DOMAIN_PATTERN.addParameter(0, ReflectionLookup.CLASS_NAME_PARAM_TYPE, false);
		LOADER_FIND_CLASS_PATTERN.addParameter(0, ReflectionLookup.CLASS_NAME_PARAM_TYPE, false);
		LOADER_FIND_SYSTEM_CLASS_PATTERN.addParameter(0, ReflectionLookup.CLASS_NAME_PARAM_TYPE, false);
		LOADER_FIND_LOADED_CLASS_PATTERN.addParameter(0, ReflectionLookup.CLASS_NAME_PARAM_TYPE, false);
		EVENT_SET_DESC_INIT_PATTERN_1.addParameter(0, ReflectionLookup.CLASS_PARAM_TYPE, false);
		EVENT_SET_DESC_INIT_PATTERN_1.addParameter(2, ReflectionLookup.CLASS_PARAM_TYPE, false);
		EVENT_SET_DESC_INIT_PATTERN_1.addParameter(3, ReflectionLookup.METHOD_NAME_PARAM_TYPE, false);
		EVENT_SET_DESC_INIT_PATTERN_2.addParameter(0, ReflectionLookup.CLASS_PARAM_TYPE, false);
		EVENT_SET_DESC_INIT_PATTERN_2.addParameter(2, ReflectionLookup.CLASS_PARAM_TYPE, false);
		EVENT_SET_DESC_INIT_PATTERN_2.addParameter(3, ReflectionLookup.METHOD_NAME_PARAM_TYPE, true);
		EVENT_SET_DESC_INIT_PATTERN_2.addParameter(4, ReflectionLookup.METHOD_NAME_PARAM_TYPE, false);
		EVENT_SET_DESC_INIT_PATTERN_2.addParameter(5, ReflectionLookup.METHOD_NAME_PARAM_TYPE, false);
		EVENT_SET_DESC_INIT_PATTERN_3.addParameter(0, ReflectionLookup.CLASS_PARAM_TYPE, false);
		EVENT_SET_DESC_INIT_PATTERN_3.addParameter(2, ReflectionLookup.CLASS_PARAM_TYPE, false);
		EVENT_SET_DESC_INIT_PATTERN_3.addParameter(3, ReflectionLookup.METHOD_NAME_PARAM_TYPE, true);
		EVENT_SET_DESC_INIT_PATTERN_3.addParameter(4, ReflectionLookup.METHOD_NAME_PARAM_TYPE, false);
		EVENT_SET_DESC_INIT_PATTERN_3.addParameter(5, ReflectionLookup.METHOD_NAME_PARAM_TYPE, false);
		EVENT_SET_DESC_INIT_PATTERN_3.addParameter(6, ReflectionLookup.METHOD_NAME_PARAM_TYPE, false);
		BUNDLE_GET_BUNDLE_PATTERN.addParameter(0, ReflectionLookup.CLASS_OR_PROPERTIES_NAME_PARAM_TYPE, false);
		BUNDLE_GET_BUNDLE_LOCALE_PATTERN.addParameter(0, ReflectionLookup.CLASS_OR_PROPERTIES_NAME_PARAM_TYPE, false);
		BUNDLE_GET_BUNDLE_LOADER_PATTERN.addParameter(0, ReflectionLookup.CLASS_OR_PROPERTIES_NAME_PARAM_TYPE, false);
		BEANS_INSTANTIATE_PATTERN.addParameter(1, ReflectionLookup.CLASS_NAME_PARAM_TYPE, false);
		BEANS_INSTANTIATE_CONTEXT_PATTERN.addParameter(1, ReflectionLookup.CLASS_NAME_PARAM_TYPE, false);
		BEANS_INSTANTIATE_APPLET_PATTERN.addParameter(1, ReflectionLookup.CLASS_NAME_PARAM_TYPE, false);
		BEAN_CONTEXT_INSTANTIATE_CHILD_PATTERN.addParameter(0, ReflectionLookup.CLASS_NAME_PARAM_TYPE, false);
		CLASS_GET_CONSTRUCTOR_PATTERN.addParameter(0, ReflectionLookup.CLASS_PARAM_TYPE, true);
		PACKAGE_GET_PACKAGE_PATTERN.addParameter(0, ReflectionLookup.PACKAGE_NAME_PARAM_TYPE, false);
		LOADER_GET_PACKAGE_PATTERN.addParameter(0, ReflectionLookup.PACKAGE_NAME_PARAM_TYPE, false);
		LOOKUP_FIND_STATIC_PATTERN.addParameter(0, ReflectionLookup.CLASS_PARAM_TYPE, false, true);
		LOOKUP_FIND_STATIC_PATTERN.addParameter(1, ReflectionLookup.METHOD_NAME_PARAM_TYPE, false);
		LOOKUP_FIND_STATIC_PATTERN.addParameter(2, ReflectionLookup.METHOD_TYPE_PARAM_TYPE, false);
		LOOKUP_FIND_VIRTUAL_PATTERN.addParameter(0, ReflectionLookup.CLASS_PARAM_TYPE, false, true);
		LOOKUP_FIND_VIRTUAL_PATTERN.addParameter(1, ReflectionLookup.METHOD_NAME_PARAM_TYPE, false);
		LOOKUP_FIND_VIRTUAL_PATTERN.addParameter(2, ReflectionLookup.METHOD_TYPE_PARAM_TYPE, false);
		LOOKUP_FIND_SPECIAL_PATTERN.addParameter(0, ReflectionLookup.CLASS_PARAM_TYPE, false);
		LOOKUP_FIND_SPECIAL_PATTERN.addParameter(1, ReflectionLookup.METHOD_NAME_PARAM_TYPE, false);
		LOOKUP_FIND_SPECIAL_PATTERN.addParameter(2, ReflectionLookup.METHOD_TYPE_PARAM_TYPE, false);
		LOOKUP_FIND_SPECIAL_PATTERN.addParameter(3, ReflectionLookup.CLASS_PARAM_TYPE, false, true);
		LOOKUP_FIND_CONSTRUCTOR_PATTERN.addParameter(0, ReflectionLookup.CLASS_PARAM_TYPE, false, true);
		LOOKUP_FIND_CONSTRUCTOR_PATTERN.addParameter(1, ReflectionLookup.METHOD_TYPE_PARAM_TYPE, false);
		LOOKUP_FIND_GETTER_PATTERN.addParameter(0, ReflectionLookup.CLASS_PARAM_TYPE, false, true);
		LOOKUP_FIND_GETTER_PATTERN.addParameter(1, ReflectionLookup.FIELD_NAME_PARAM_TYPE, false);
		LOOKUP_FIND_GETTER_PATTERN.addParameter(2, ReflectionLookup.CLASS_PARAM_TYPE, false);
		LOOKUP_FIND_SETTER_PATTERN.addParameter(0, ReflectionLookup.CLASS_PARAM_TYPE, false, true);
		LOOKUP_FIND_SETTER_PATTERN.addParameter(1, ReflectionLookup.FIELD_NAME_PARAM_TYPE, false);
		LOOKUP_FIND_SETTER_PATTERN.addParameter(2, ReflectionLookup.CLASS_PARAM_TYPE, false);
		LOOKUP_FIND_STATIC_GETTER_PATTERN.addParameter(0, ReflectionLookup.CLASS_PARAM_TYPE, false, true);
		LOOKUP_FIND_STATIC_GETTER_PATTERN.addParameter(1, ReflectionLookup.FIELD_NAME_PARAM_TYPE, false);
		LOOKUP_FIND_STATIC_GETTER_PATTERN.addParameter(2, ReflectionLookup.CLASS_PARAM_TYPE, false);
		LOOKUP_FIND_STATIC_SETTER_PATTERN.addParameter(0, ReflectionLookup.CLASS_PARAM_TYPE, false, true);
		LOOKUP_FIND_STATIC_SETTER_PATTERN.addParameter(1, ReflectionLookup.FIELD_NAME_PARAM_TYPE, false);
		LOOKUP_FIND_STATIC_SETTER_PATTERN.addParameter(2, ReflectionLookup.CLASS_PARAM_TYPE, false);
		LOOKUP_FIND_VAR_HANDLE_PATTERN.addParameter(0, ReflectionLookup.CLASS_PARAM_TYPE, false, true);
		LOOKUP_FIND_VAR_HANDLE_PATTERN.addParameter(1, ReflectionLookup.FIELD_NAME_PARAM_TYPE, false);
		LOOKUP_FIND_VAR_HANDLE_PATTERN.addParameter(2, ReflectionLookup.CLASS_PARAM_TYPE, false);
		LOOKUP_FIND_STATIC_VAR_HANDLE_PATTERN.addParameter(0, ReflectionLookup.CLASS_PARAM_TYPE, false, true);
		LOOKUP_FIND_STATIC_VAR_HANDLE_PATTERN.addParameter(1, ReflectionLookup.FIELD_NAME_PARAM_TYPE, false);
		LOOKUP_FIND_STATIC_VAR_HANDLE_PATTERN.addParameter(2, ReflectionLookup.CLASS_PARAM_TYPE, false);
		LOOKUP_BIND_PATTERN.addParameter(0, ReflectionLookup.OBJECT_PARAM_TYPE, false);
		LOOKUP_BIND_PATTERN.addParameter(1, ReflectionLookup.METHOD_NAME_PARAM_TYPE, false);
		LOOKUP_BIND_PATTERN.addParameter(2, ReflectionLookup.METHOD_TYPE_PARAM_TYPE, false);
		ATOMIC_REF_UPDATER_PATTERN.addParameter(0, ReflectionLookup.CLASS_PARAM_TYPE, false, true);
		ATOMIC_REF_UPDATER_PATTERN.addParameter(1, ReflectionLookup.CLASS_PARAM_TYPE, false, false, true);
		ATOMIC_REF_UPDATER_PATTERN.addParameter(2, ReflectionLookup.FIELD_NAME_PARAM_TYPE, false);
		ATOMIC_INT_UPDATER_PATTERN.addParameter(0, ReflectionLookup.CLASS_PARAM_TYPE, false, true);
		ATOMIC_INT_UPDATER_PATTERN.addParameter(1, ReflectionLookup.FIELD_NAME_PARAM_TYPE, false);
		ATOMIC_LONG_UPDATER_PATTERN.addParameter(0, ReflectionLookup.CLASS_PARAM_TYPE, false, true);
		ATOMIC_LONG_UPDATER_PATTERN.addParameter(1, ReflectionLookup.FIELD_NAME_PARAM_TYPE, false);
		METHOD_TYPE_PATTERN_1.addParameter(0, ReflectionLookup.CLASS_PARAM_TYPE, false);
		METHOD_TYPE_PATTERN_2.addParameter(0, ReflectionLookup.CLASS_PARAM_TYPE, false);
		METHOD_TYPE_PATTERN_2.addParameter(1, ReflectionLookup.CLASS_PARAM_TYPE, false);
		METHOD_TYPE_PATTERN_3.addParameter(0, ReflectionLookup.CLASS_PARAM_TYPE, false);
		METHOD_TYPE_PATTERN_3.addParameter(1, ReflectionLookup.CLASS_PARAM_TYPE, true);
		METHOD_TYPE_PATTERN_4.addParameter(0, ReflectionLookup.CLASS_PARAM_TYPE, false);
		METHOD_TYPE_PATTERN_4.addParameter(1, ReflectionLookup.CLASS_PARAM_TYPE, false);
		METHOD_TYPE_PATTERN_4.addParameter(2, ReflectionLookup.CLASS_PARAM_TYPE, true);
		METHOD_TYPE_PATTERN_5.addParameter(0, ReflectionLookup.CLASS_PARAM_TYPE, false);
		METHOD_TYPE_PATTERN_5.addParameter(1, ReflectionLookup.METHOD_TYPE_PARAM_TYPE, false);
		REFLECTION_PATTERN_REGISTRY.put(
			"getFunctionalInterfaceMethodName()Ljava/lang/String;", "java/lang/invoke/SerializedLambda", LAMBDA_INTERFACE_METHOD_NAME_PATTERN
		);
		REFLECTION_PATTERN_REGISTRY.put("getName()Ljava/lang/String;", "java/lang/Class", GET_NAME_PATTERN);
		REFLECTION_PATTERN_REGISTRY.put("getName()Ljava/lang/String;", "java/lang/Package", GET_NAME_PATTERN);
		REFLECTION_PATTERN_REGISTRY.put("getName()Ljava/lang/String;", "java/lang/reflect/Field", GET_NAME_PATTERN);
		REFLECTION_PATTERN_REGISTRY.put("getName()Ljava/lang/String;", "java/lang/reflect/Method", GET_NAME_PATTERN);
		REFLECTION_PATTERN_REGISTRY.put("getName()Ljava/lang/String;", "java/lang/reflect/Constructor", GET_NAME_PATTERN);
		REFLECTION_PATTERN_REGISTRY.put("getName()Ljava/lang/String;", "java/lang/reflect/Member", GET_NAME_PATTERN);
		REFLECTION_PATTERN_REGISTRY.put("forName(Ljava/lang/String;)Ljava/lang/Class;", "java/lang/Class", CLASS_FOR_NAME_PATTERN);
		REFLECTION_PATTERN_REGISTRY.put("forName(Ljava/lang/Module;Ljava/lang/String;)Ljava/lang/Class;", "java/lang/Class", CLASS_FOR_NAME_MODULE_PATTERN);
		REFLECTION_PATTERN_REGISTRY.put("forName(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", "java/lang/Class", CLASS_FOR_NAME_LOADER_PATTERN);
		REFLECTION_PATTERN_REGISTRY.put("getField(Ljava/lang/String;)Ljava/lang/reflect/Field;", "java/lang/Class", CLASS_GET_FIELD_PATTERN);
		REFLECTION_PATTERN_REGISTRY.put("getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", "java/lang/Class", CLASS_GET_METHOD_PATTERN);
		REFLECTION_PATTERN_REGISTRY.put("getDeclaredField(Ljava/lang/String;)Ljava/lang/reflect/Field;", "java/lang/Class", CLASS_GET_DECLARED_FIELD_PATTERN);
		REFLECTION_PATTERN_REGISTRY.put(
			"getDeclaredMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", "java/lang/Class", CLASS_GET_DECLARED_METHOD_PATTERN
		);
		REFLECTION_PATTERN_REGISTRY.put("loadClass(Ljava/lang/String;)Ljava/lang/Class;", "java/lang/ClassLoader", LOADER_LOAD_CLASS_PATTERN);
		REFLECTION_PATTERN_REGISTRY.put("loadClass(Ljava/lang/String;Z)Ljava/lang/Class;", "java/lang/ClassLoader", LOADER_LOAD_CLASS_RESOLVE_PATTERN);
		REFLECTION_PATTERN_REGISTRY.put("defineClass(Ljava/lang/String;[BII)Ljava/lang/Class;", "java/lang/ClassLoader", LOADER_DEFINE_CLASS_PATTERN);
		REFLECTION_PATTERN_REGISTRY.put(
			"defineClass(Ljava/lang/String;[BIILjava/security/ProtectionDomain;)Ljava/lang/Class;", "java/lang/ClassLoader", LOADER_DEFINE_CLASS_DOMAIN_PATTERN
		);
		REFLECTION_PATTERN_REGISTRY.put("findClass(Ljava/lang/String;)Ljava/lang/Class;", "java/lang/ClassLoader", LOADER_FIND_CLASS_PATTERN);
		REFLECTION_PATTERN_REGISTRY.put("findClass(Ljava/lang/String;)Ljava/lang/Class;", "java/lang/invoke/MethodHandles$Lookup", LOADER_FIND_CLASS_PATTERN);
		REFLECTION_PATTERN_REGISTRY.put("findSystemClass(Ljava/lang/String;)Ljava/lang/Class;", "java/lang/ClassLoader", LOADER_FIND_SYSTEM_CLASS_PATTERN);
		REFLECTION_PATTERN_REGISTRY.put("findLoadedClass(Ljava/lang/String;)Ljava/lang/Class;", "java/lang/ClassLoader", LOADER_FIND_LOADED_CLASS_PATTERN);
		REFLECTION_PATTERN_REGISTRY.put(
			"<init>(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/String;)V", "java/beans/EventSetDescriptor", EVENT_SET_DESC_INIT_PATTERN_1
		);
		REFLECTION_PATTERN_REGISTRY.put(
			"<init>(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;[Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
			"java/beans/EventSetDescriptor",
			EVENT_SET_DESC_INIT_PATTERN_2
		);
		REFLECTION_PATTERN_REGISTRY.put(
			"<init>(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;[Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
			"java/beans/EventSetDescriptor",
			EVENT_SET_DESC_INIT_PATTERN_3
		);
		REFLECTION_PATTERN_REGISTRY.put("loadClass(Ljava/lang/String;)Ljava/lang/Class;", "java/rmi/server/RMIClassLoader", RMI_LOAD_CLASS_PATTERN);
		REFLECTION_PATTERN_REGISTRY.put("loadClass(Ljava/net/URL;Ljava/lang/String;)Ljava/lang/Class;", "java/rmi/server/RMIClassLoader", RMI_LOAD_CLASS_URL_PATTERN);
		REFLECTION_PATTERN_REGISTRY.put(
			"loadClass(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Class;", "java/rmi/server/RMIClassLoader", RMI_LOAD_CLASS_NAME_PATTERN
		);
		REFLECTION_PATTERN_REGISTRY.put(
			"loadClass(Ljava/lang/String;Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/Class;", "java/rmi/server/RMIClassLoader", RMI_LOAD_CLASS_LOADER_PATTERN
		);
		REFLECTION_PATTERN_REGISTRY.put("loadClass(Ljava/lang/String;)Ljava/lang/Class;", "java/rmi/server/LoaderHandler", LOADER_LOAD_CLASS_PATTERN);
		REFLECTION_PATTERN_REGISTRY.put(
			"loadClass(Ljava/net/URL;Ljava/lang/String;)Ljava/lang/Class;", "java/rmi/server/LoaderHandler", LOADER_LOAD_CLASS_URL_PATTERN
		);
		REFLECTION_PATTERN_REGISTRY.put("getBundle(Ljava/lang/String;)Ljava/util/ResourceBundle;", "java/util/ResourceBundle", BUNDLE_GET_BUNDLE_PATTERN);
		REFLECTION_PATTERN_REGISTRY.put(
			"getBundle(Ljava/lang/String;Ljava/util/Locale;)Ljava/util/ResourceBundle;", "java/util/ResourceBundle", BUNDLE_GET_BUNDLE_LOCALE_PATTERN
		);
		REFLECTION_PATTERN_REGISTRY.put(
			"getBundle(Ljava/lang/String;Ljava/util/Locale;Ljava/lang/ClassLoader;)Ljava/util/ResourceBundle;",
			"java/util/ResourceBundle",
			BUNDLE_GET_BUNDLE_LOADER_PATTERN
		);
		REFLECTION_PATTERN_REGISTRY.put("instantiate(Ljava/lang/ClassLoader;Ljava/lang/String;)Ljava/lang/Object;", "java/beans/Beans", BEANS_INSTANTIATE_PATTERN);
		REFLECTION_PATTERN_REGISTRY.put(
			"instantiate(Ljava/lang/ClassLoader;Ljava/lang/String;Ljava/beans/beancontext/BeanContext;)Ljava/lang/Object;",
			"java/beans/Beans",
			BEANS_INSTANTIATE_CONTEXT_PATTERN
		);
		REFLECTION_PATTERN_REGISTRY.put(
			"instantiate(Ljava/lang/ClassLoader;Ljava/lang/String;Ljava/beans/beancontext/BeanContext;Ljava/beans/AppletInitializer;)Ljava/lang/Object;",
			"java/beans/Beans",
			BEANS_INSTANTIATE_APPLET_PATTERN
		);
		REFLECTION_PATTERN_REGISTRY.put(
			"instantiateChild(Ljava/lang/String;)Ljava/lang/Object;", "java/beans/beancontext/BeanContext", BEAN_CONTEXT_INSTANTIATE_CHILD_PATTERN
		);
		REFLECTION_PATTERN_REGISTRY.put("newInstance()Ljava/lang/Object;", "java/lang/Class", CLASS_NEW_INSTANCE_PATTERN);
		REFLECTION_PATTERN_REGISTRY.put("getConstructor([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;", "java/lang/Class", CLASS_GET_CONSTRUCTOR_PATTERN);
		REFLECTION_PATTERN_REGISTRY.put("getConstructors()[Ljava/lang/reflect/Constructor;", "java/lang/Class", CLASS_GET_CONSTRUCTORS_PATTERN);
		REFLECTION_PATTERN_REGISTRY.put("getPackage(Ljava/lang/String;)Ljava/lang/Package;", "java/lang/Package", PACKAGE_GET_PACKAGE_PATTERN);
		REFLECTION_PATTERN_REGISTRY.put("getPackage(Ljava/lang/String;)Ljava/lang/Package;", "java/lang/ClassLoader", LOADER_GET_PACKAGE_PATTERN);
		REFLECTION_PATTERN_REGISTRY.put(
			"findStatic(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;",
			"java/lang/invoke/MethodHandles$Lookup",
			LOOKUP_FIND_STATIC_PATTERN
		);
		REFLECTION_PATTERN_REGISTRY.put(
			"findVirtual(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;",
			"java/lang/invoke/MethodHandles$Lookup",
			LOOKUP_FIND_VIRTUAL_PATTERN
		);
		REFLECTION_PATTERN_REGISTRY.put(
			"findSpecial(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;",
			"java/lang/invoke/MethodHandles$Lookup",
			LOOKUP_FIND_SPECIAL_PATTERN
		);
		REFLECTION_PATTERN_REGISTRY.put(
			"findConstructor(Ljava/lang/Class;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;",
			"java/lang/invoke/MethodHandles$Lookup",
			LOOKUP_FIND_CONSTRUCTOR_PATTERN
		);
		REFLECTION_PATTERN_REGISTRY.put(
			"findGetter(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;",
			"java/lang/invoke/MethodHandles$Lookup",
			LOOKUP_FIND_GETTER_PATTERN
		);
		REFLECTION_PATTERN_REGISTRY.put(
			"findSetter(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;",
			"java/lang/invoke/MethodHandles$Lookup",
			LOOKUP_FIND_SETTER_PATTERN
		);
		REFLECTION_PATTERN_REGISTRY.put(
			"findStaticGetter(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;",
			"java/lang/invoke/MethodHandles$Lookup",
			LOOKUP_FIND_STATIC_GETTER_PATTERN
		);
		REFLECTION_PATTERN_REGISTRY.put(
			"findStaticSetter(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;",
			"java/lang/invoke/MethodHandles$Lookup",
			LOOKUP_FIND_STATIC_SETTER_PATTERN
		);
		REFLECTION_PATTERN_REGISTRY.put(
			"findVarHandle(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/VarHandle;",
			"java/lang/invoke/MethodHandles$Lookup",
			LOOKUP_FIND_VAR_HANDLE_PATTERN
		);
		REFLECTION_PATTERN_REGISTRY.put(
			"findStaticVarHandle(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/VarHandle;",
			"java/lang/invoke/MethodHandles$Lookup",
			LOOKUP_FIND_STATIC_VAR_HANDLE_PATTERN
		);
		REFLECTION_PATTERN_REGISTRY.put(
			"bind(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;",
			"java/lang/invoke/MethodHandles$Lookup",
			LOOKUP_BIND_PATTERN
		);
		REFLECTION_PATTERN_REGISTRY.put(
			"newUpdater(Ljava/lang/Class;Ljava/lang/Class;Ljava/lang/String;)Ljava/util/concurrent/atomic/AtomicReferenceFieldUpdater;",
			"java/util/concurrent/atomic/AtomicReferenceFieldUpdater",
			ATOMIC_REF_UPDATER_PATTERN
		);
		REFLECTION_PATTERN_REGISTRY.put(
			"newUpdater(Ljava/lang/Class;Ljava/lang/String;)Ljava/util/concurrent/atomic/AtomicIntegerFieldUpdater;",
			"java/util/concurrent/atomic/AtomicIntegerFieldUpdater",
			ATOMIC_INT_UPDATER_PATTERN
		);
		REFLECTION_PATTERN_REGISTRY.put(
			"newUpdater(Ljava/lang/Class;Ljava/lang/String;)Ljava/util/concurrent/atomic/AtomicLongFieldUpdater;",
			"java/util/concurrent/atomic/AtomicLongFieldUpdater",
			ATOMIC_LONG_UPDATER_PATTERN
		);
		REFLECTION_PATTERN_REGISTRY.put("methodType(Ljava/lang/Class;)Ljava/lang/invoke/MethodType;", "java/lang/invoke/MethodType", METHOD_TYPE_PATTERN_1);
		REFLECTION_PATTERN_REGISTRY.put(
			"methodType(Ljava/lang/Class;Ljava/lang/Class;)Ljava/lang/invoke/MethodType;", "java/lang/invoke/MethodType", METHOD_TYPE_PATTERN_2
		);
		REFLECTION_PATTERN_REGISTRY.put(
			"methodType(Ljava/lang/Class;[Ljava/lang/Class;)Ljava/lang/invoke/MethodType;", "java/lang/invoke/MethodType", METHOD_TYPE_PATTERN_3
		);
		REFLECTION_PATTERN_REGISTRY.put(
			"methodType(Ljava/lang/Class;Ljava/lang/Class;[Ljava/lang/Class;)Ljava/lang/invoke/MethodType;", "java/lang/invoke/MethodType", METHOD_TYPE_PATTERN_4
		);
		REFLECTION_PATTERN_REGISTRY.put(
			"methodType(Ljava/lang/Class;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodType;", "java/lang/invoke/MethodType", METHOD_TYPE_PATTERN_5
		);
		INHERITANCE_LOOKUP_SIGNATURES.add("getPackage(Ljava/lang/String;)Ljava/lang/Package;");
		INHERITANCE_LOOKUP_SIGNATURES.add("<init>(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/String;)V");
		INHERITANCE_LOOKUP_SIGNATURES.add("<init>(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;[Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
		INHERITANCE_LOOKUP_SIGNATURES.add(
			"<init>(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;[Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"
		);
		INHERITANCE_LOOKUP_SIGNATURES.add("defineClass(Ljava/lang/String;[BII)Ljava/lang/Class;");
		INHERITANCE_LOOKUP_SIGNATURES.add("defineClass(Ljava/lang/String;[BIILjava/security/ProtectionDomain;)Ljava/lang/Class;");
		INHERITANCE_LOOKUP_SIGNATURES.add("findLoadedClass(Ljava/lang/String;)Ljava/lang/Class;");
		INHERITANCE_LOOKUP_SIGNATURES.add("findClass(Ljava/lang/String;)Ljava/lang/Class;");
		INHERITANCE_LOOKUP_SIGNATURES.add("findSystemClass(Ljava/lang/String;)Ljava/lang/Class;");
		INHERITANCE_LOOKUP_SIGNATURES.add("loadClass(Ljava/lang/String;)Ljava/lang/Class;");
		INHERITANCE_LOOKUP_SIGNATURES.add("loadClass(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Class;");
		INHERITANCE_LOOKUP_SIGNATURES.add("loadClass(Ljava/lang/String;Z)Ljava/lang/Class;");
		INHERITANCE_LOOKUP_SIGNATURES.add("loadClass(Ljava/net/URL;Ljava/lang/String;)Ljava/lang/Class;");
		INHERITANCE_LOOKUP_SIGNATURES.add("getBundle(Ljava/lang/String;)Ljava/util/ResourceBundle;");
		INHERITANCE_LOOKUP_SIGNATURES.add("getBundle(Ljava/lang/String;Ljava/util/Locale;)Ljava/util/ResourceBundle;");
		INHERITANCE_LOOKUP_SIGNATURES.add("getBundle(Ljava/lang/String;Ljava/util/Locale;Ljava/lang/ClassLoader;)Ljava/util/ResourceBundle;");
		INHERITANCE_LOOKUP_SIGNATURES.add("instantiate(Ljava/lang/ClassLoader;Ljava/lang/String;)Ljava/lang/Object;");
		INHERITANCE_LOOKUP_SIGNATURES.add("instantiate(Ljava/lang/ClassLoader;Ljava/lang/String;Ljava/beans/beancontext/BeanContext;)Ljava/lang/Object;");
		INHERITANCE_LOOKUP_SIGNATURES.add(
			"instantiate(Ljava/lang/ClassLoader;Ljava/lang/String;Ljava/beans/beancontext/BeanContext;Ljava/beans/AppletInitializer;)Ljava/lang/Object;"
		);
		INHERITANCE_LOOKUP_SIGNATURES.add("instantiateChild(Ljava/lang/String;)Ljava/lang/Object;");
	}
}
