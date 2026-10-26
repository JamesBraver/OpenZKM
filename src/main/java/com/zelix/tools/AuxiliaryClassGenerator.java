package com.zelix.tools;

import com.zelix.framework.wrappers.ClassNode;

public interface AuxiliaryClassGenerator {
	ClassNode generate(String className, int majorVersion, int minorVersion, AuxiliaryWiringStrategy wiringStrategy);
}
