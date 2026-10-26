package com.zelix.framework;

import com.zelix.event.AtomicNotifier;
import com.zelix.framework.constant.ConstantPool;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.writer.base.AbstractConstantPoolWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.IntegerCache;

public abstract class StructureNode extends AtomicNotifier implements ConstantPool {
	public static final IntegerCache INTEGER_CACHE = IntegerCache.getInstance();
	private StructureNode parent;

	public StructureNode(StructureNode parent) {
		this.parent = parent;
	}

	public String getBinaryName() {
		return this.parent.getBinaryName();
	}

	public final int getMemberType() {
		if (this.parent instanceof AbstractClassNode) {
			return 1;
		} else if (this.parent instanceof AbstractFieldNode) {
			return 2;
		} else {
			return this.parent instanceof AbstractMethodNode ? 3 : 4;
		}
	}

	public StructureNode getParent() {
		return this.parent;
	}

	public String getClassNameBinary() {
		return this.parent.getClassNameBinary();
	}

	@Override
	public TypeWriter getConstantPoolEntry(int index) {
		return this.parent.getConstantPoolEntry(index);
	}

	public abstract void acceptVisitor(ConstantPoolVisitor visitor);

	public final AbstractClassNode getEquivalentClassNode(AbstractClassNode classNode) {
		AbstractClassNode owningClassNode = this.getClassNode();
		AbstractClassNode equivalentClassNode;
		if (owningClassNode.isInnerClass() && classNode.hasInnerClasses()) {
			equivalentClassNode = classNode.findInnerClassByOrdinal(owningClassNode.getInnerClassOrdinal());
		} else {
			equivalentClassNode = classNode;
		}

		return equivalentClassNode;
	}

	public String getClassName() {
		return this.parent.getClassName();
	}

	public AbstractClassNode getClassNode() {
		StructureNode sn = this;

		while (sn.parent != null) {
			sn = sn.parent;
		}

		return (AbstractClassNode)sn;
	}

	public final ClassNode getEquivalentClassNode(ClassNode classNode) {
		return (ClassNode)this.getEquivalentClassNode(classNode);
	}

	public String toCode() {
		return "";
	}

	public String getSourceName() {
		return this.parent.getSourceName();
	}

	public boolean isClassNode() {
		return false;
	}

	public String getConstructorDisplayName() {
		return this.parent.getConstructorDisplayName();
	}

	public String getSourcePath() {
		return this.parent.getSourcePath();
	}

	public String getRawSourcePath() {
		return this.parent.getRawSourcePath();
	}

	public AbstractConstantPoolWriter getConstantPoolWriter() {
		return this.parent.getConstantPoolWriter();
	}

	public String getPackageName() {
		return this.parent.getPackageName();
	}

	public final AttributesNode getAttributesNode() {
		StructureNode sn = this;

		while (sn.parent != null && !(sn instanceof AttributesNode)) {
			sn = sn.parent;
		}

		return (AttributesNode)sn;
	}

	public String getInternalName() {
		return this.parent.getInternalName();
	}

	public void setParent(StructureNode parent) {
		this.parent = parent;
	}
}
