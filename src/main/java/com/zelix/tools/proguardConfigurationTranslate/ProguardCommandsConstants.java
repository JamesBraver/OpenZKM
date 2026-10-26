package com.zelix.tools.proguardConfigurationTranslate;

import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.ArraySet;
import java.util.Set;

public interface ProguardCommandsConstants {
	String[] PROGUARD_COMMANDS = new String[]{
		"-adaptclassstrings",
		"-adaptresourcefilecontents",
		"-adaptresourcefilenames",
		"-applymapping",
		"-basedirectory",
		"-classobfuscationdictionary",
		"-dontnote",
		"-dump",
		"-include",
		"-injars",
		"-keepdirectories",
		"-flattenpackagehierarchy",
		"-keeppackagenames",
		"-libraryjars",
		"-obfuscationdictionary",
		"-optimizations",
		"-outjars",
		"-packageobfuscationdictionary",
		"-printconfiguration",
		"-printmapping",
		"-printusage",
		"-printseeds",
		"-renamesourcefileattribute",
		"-repackageclasses",
		"-target"
	};
	String[] PROGUARD_CLI_OPTIONS = new String[]{
		"-adaptclassstrings",
		"-adaptresourcefilecontents",
		"-adaptresourcefilenames",
		"-dontnote",
		"-dump",
		"-keepdirectories",
		"-flattenpackagehierarchy",
		"-keeppackagenames",
		"-printconfiguration",
		"-printmapping",
		"-printusage",
		"-printseeds",
		"-renamesourcefileattribute",
		"-repackageclasses"
	};
	Set<String> PROGUARD_CLI_OPTION_SET = GlobalUtils.newHashSet(new ArraySet<>(PROGUARD_CLI_OPTIONS));
}
