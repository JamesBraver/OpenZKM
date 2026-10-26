package com.zelix.framework.wrappers.property;

import com.zelix.framework.MemberNode;
import com.zelix.framework.StructureNode;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.IntegerCache;
import java.util.Arrays;
import java.util.Vector;

public class ClassMemberEntry extends ClassComponentEntry<MemberNode> {
	public int memberCount;
	public MemberNode[] members;

	public ClassMemberEntry(String type, StructureNode node, MemberNode[] members, IntegerCache integerCache) {
		super(type, node, integerCache);
		this.setMembers(members);
	}

	public void refreshMembers(MemberNode[] members) {
		this.setMembers(members);
		this.markChanged();
		this.notifySimpleChange();
	}

	public String getNextMemberName() {
		return this.members[super.currentIndex++].getNameDisplay();
	}

	public int findMemberIndex(MemberNode memberNode) {
		if (super.indexMap == null) {
			super.indexMap = GlobalUtils.newHashMap((int)(this.memberCount * 1.5));

			for (int i = 0; i < this.memberCount; i++) {
				super.indexMap.put(this.members[i], super.integerCache.valueOf(i));
			}
		}

		Object index = super.indexMap.get(memberNode);
		return index != null ? (Integer)index : -1;
	}

	public boolean hasMoreMembers() {
		return super.currentIndex < this.memberCount;
	}

	public void setMembers(MemberNode[] members) {
		int declaredMemberCount = members.length;
		Vector<MemberNode> validMembers = new Vector<>(declaredMemberCount);

		for (int i = 0; i < declaredMemberCount; i++) {
			if (members[i].isValid()) {
				validMembers.addElement(members[i]);
			}
		}

		this.memberCount = validMembers.size();
		this.members = new MemberNode[this.memberCount];
		validMembers.copyInto(this.members);
		Arrays.sort(this.members);
		super.indexMap = null;
		this.resetIndex();
	}

	public MemberNode getMember(int index) {
		return this.members[index];
	}
}
