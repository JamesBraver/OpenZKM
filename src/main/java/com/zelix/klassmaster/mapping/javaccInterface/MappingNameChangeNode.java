package com.zelix.klassmaster.mapping.javaccInterface;

import com.zelix.tools.proguardConfigurationTranslate.ProguardTranslateWriter;

public interface MappingNameChangeNode {
	void writeNameChange(String className, ProguardTranslateWriter writer);
}
