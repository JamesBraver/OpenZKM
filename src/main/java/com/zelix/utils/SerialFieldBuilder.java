package com.zelix.utils;

import com.zelix.exceptions.ProcessingException;
import com.zelix.framework.attributes.declaration.ConstantValueAttribute;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.utils.comparators.FieldNodeComparator;
import com.zelix.utils.enumerations.ArrayEnumeration;
import com.zelix.workspace.AbstractWorkspace;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Vector;

public class SerialFieldBuilder implements Serializable {
	public static final String SERIAL_VER_ALGORITHM = System.getProperty("SERIAL_VER_ALGORITHM", "1.5");
	private static final Comparator<FieldNode> fieldComp = new FieldNodeComparator();
	private static final Comparator<MethodNode> methodComp = new MethodNodeComparator();

	public static long computeSerialVersionUid(ClassNode classNode, AbstractWorkspace workspace) {
		String iName = classNode.getInternalName();
		if (!workspace.isClassInstanceOf(iName, "java/lang/Enum")
			&& !workspace.isClassInstanceOf(iName, "java/lang/reflect/Proxy")
			&& !iName.equals("java/lang/Enum")
			&& (
				workspace.isSubclassOf(iName, "java/io/Serializable")
					|| workspace.isSubclassOf(iName, "java/io/Externalizable")
					|| iName.equals("java/io/Externalizable")
					|| iName.equals("java/io/Serializable")
			)) {
			FieldNode serialVersionUidField = workspace.getFieldByNameAndDescriptor(iName, "serialVersionUID", "J");
			if (serialVersionUidField != null) {
				ConstantValueAttribute constantValueAttribute = serialVersionUidField.getConstantValueAttribute();
				if (constantValueAttribute != null) {
					return constantValueAttribute.getLongValue();
				}
			}

			ByteArrayOutputStream digestBuffer = new ByteArrayOutputStream(512);
			long serialVersionUid = 0L;

			try {
				MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
				DigestOutputStream digestStream = new DigestOutputStream(digestBuffer, sha1);
				DataOutputStream dataStream = new DataOutputStream(digestStream);
				dataStream.writeUTF(classNode.getBinaryName());
				int flags = classNode.getInnerClassAccessFlags();
				if (flags == -1) {
					flags = classNode.getAccessFlags();
				}

				if (!SERIAL_VER_ALGORITHM.equals("1.4")) {
					flags &= 1553;
				}

				if (classNode.isInterface()) {
					if (classNode.getMethodCount() > 0) {
						flags |= 1024;
					} else {
						flags &= -1025;
					}
				}

				dataStream.writeInt(flags);
				String[] interfaceNames = classNode.getInterfaceNames();
				Arrays.sort(interfaceNames);

				for (int interfaceIndex = 0; interfaceIndex < interfaceNames.length; interfaceIndex++) {
					String interfaceName = interfaceNames[interfaceIndex].replace('/', '.');
					dataStream.writeUTF(interfaceName);
				}

				ArrayEnumeration<FieldNode> fieldEnumeration = classNode.getFieldEnumeration();
				fieldEnumeration.sort(fieldComp);

				while (fieldEnumeration.hasMoreElements()) {
					FieldNode field = fieldEnumeration.nextElement();
					if (!field.isPrivate() || !field.isStatic() && !field.isTransient()) {
						dataStream.writeUTF(field.getNameDisplay());
						int fieldAccessFlags = field.getAccessFlags();
						if (!SERIAL_VER_ALGORITHM.equals("1.4")) {
							fieldAccessFlags &= 223;
						}

						dataStream.writeInt(fieldAccessFlags);
						dataStream.writeUTF(field.getDescriptor());
					}
				}

				if (workspace.containsMethodSignature(MethodSignature.STATIC_INITIALIZER, classNode)) {
					dataStream.writeUTF("<clinit>");
					dataStream.writeInt(8);
					dataStream.writeUTF("()V");
				}

				ArrayEnumeration<MethodNode> methodEnumeration = classNode.getMethodEnumeration();
				methodEnumeration.sort(methodComp);
				Vector<MethodNode> constructorMethods = new Vector<>();
				Vector<MethodNode> regularMethods = new Vector<>();

				while (methodEnumeration.hasMoreElements()) {
					MethodNode method = methodEnumeration.nextElement();
					if (!method.isPrivate()) {
						if (method.isConstructor()) {
							constructorMethods.addElement(method);
						} else if (!method.isClinit()) {
							regularMethods.addElement(method);
						}
					}
				}

				for (int constructorIndex = 0; constructorIndex < constructorMethods.size(); constructorIndex++) {
					MethodNode constructorMethod = constructorMethods.elementAt(constructorIndex);
					dataStream.writeUTF(constructorMethod.getName());
					int constructorMethodAccessFlags = constructorMethod.getAccessFlags();
					if (!SERIAL_VER_ALGORITHM.equals("1.4")) {
						constructorMethodAccessFlags &= 3391;
					}

					dataStream.writeInt(constructorMethodAccessFlags);
					dataStream.writeUTF(constructorMethod.getDescriptor().replace('/', '.'));
				}

				for (int methodIndex = 0; methodIndex < regularMethods.size(); methodIndex++) {
					MethodNode regularMethod = regularMethods.elementAt(methodIndex);
					dataStream.writeUTF(regularMethod.getName());
					int regularMethodAccessFlags = regularMethod.getAccessFlags();
					if (!SERIAL_VER_ALGORITHM.equals("1.4")) {
						regularMethodAccessFlags &= 3391;
					}

					dataStream.writeInt(regularMethodAccessFlags);
					dataStream.writeUTF(regularMethod.getDescriptor().replace('/', '.'));
				}

				dataStream.flush();
				byte[] digestBytes = sha1.digest();

				for (int byteIndex = 0; byteIndex < Math.min(8, digestBytes.length); byteIndex++) {
					serialVersionUid += (long)(digestBytes[byteIndex] & 255) << byteIndex * 8;
				}
			} catch (IOException ignored) {
				serialVersionUid = 0L;
			} catch (NoSuchAlgorithmException exception) {
				throw new ProcessingException(exception.toString());
			}

			return serialVersionUid;
		} else {
			return 0L;
		}
	}

	public static class MethodNodeComparator implements Comparator<MethodNode> {
		@Override
		public int compare(MethodNode o1, MethodNode o2) {
			return this.compareBySignature(o1, o2);
		}

		public int compareBySignature(MethodNode method1, MethodNode method2) {
			String signature1 = method1.getName() + method1.getDescriptor();
			String signature2 = method2.getName() + method2.getDescriptor();
			return signature1.compareTo(signature2);
		}
	}
}
