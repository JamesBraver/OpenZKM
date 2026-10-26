package com.zelix.framework.writer;

import com.zelix.framework.TypeNode;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.writer.base.MemberTypeWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.collections.MultiMap;
import java.io.PrintWriter;

public interface Writer {
	TypeWriter resolve(
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<NameAndTypeWriter, AbstractNamedWriter> nameAndTypeReferences,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		MultiMap<MemberTypeWriter, TypeNode> memberReferences,
		PrintWriter errorWriter
	);

	int getTag();
}
