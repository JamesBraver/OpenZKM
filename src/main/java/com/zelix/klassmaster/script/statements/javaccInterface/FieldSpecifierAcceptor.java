package com.zelix.klassmaster.script.statements.javaccInterface;

import com.zelix.klassmaster.script.statements.specifier.ComplexFieldSpecifier;

public interface FieldSpecifierAcceptor {
	void setFieldSpecifier(ComplexFieldSpecifier fieldSpecifier);

	void setFieldTypeDescriptor(String fieldTypeDescriptor);
}
