package com.zelix;

import com.zelix.klassmaster.AbstractBootstrap;
import com.zelix.utils.FileUtils;
import com.zelix.utils.references.AtomicReference;
import java.io.File;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class ZKM_PG_Task extends Task {
	private StringBuilder config = new StringBuilder();
	private File configurationFile;

	public void addConfiguredInjar(AntPathElement injar) {
		if (this.config.length() > 0) {
			this.config.append(' ');
		}

		this.config.append("-injars");
		this.config.append(' ');
		this.config.append(injar.getPath());
	}

	public void addConfiguredLibraryjar(AntPathElement libraryJar) {
		if (this.config.length() > 0) {
			this.config.append(' ');
		}

		this.config.append("-libraryjars");
		this.config.append(' ');
		this.config.append(libraryJar.getPath());
	}

	public void addConfiguredOutjar(AntPathElement outJar) {
		if (this.config.length() > 0) {
			this.config.append(' ');
		}

		this.config.append("-outjars");
		this.config.append(' ');
		this.config.append(outJar.getPath());
	}

	public void addText(String text) {
		if (this.config.length() > 0) {
			this.config.append(' ');
		}

		this.config.append(this.getProject().replaceProperties(text));
	}

	public void execute() {
		if (this.configurationFile == null && this.config.length() == 0) {
			throw new BuildException("Missing or empty ProGuard configuration");
		} else {
			try {
				String proguardConfigPath;
				if (this.config.length() > 0) {
					String configText;
					if (this.configurationFile == null) {
						configText = this.config.toString();
					} else {
						StringBuilder configBuffer = new StringBuilder();
						configBuffer.append(this.config.toString());
						configBuffer.append(FileUtils.LINE_SEPARATOR);
						configBuffer.append(FileUtils.readToString(this.configurationFile));
						configText = configBuffer.toString();
					}

					AtomicReference<String> errorMessage = new AtomicReference<>();
					proguardConfigPath = FileUtils.writeToTempFile(configText, errorMessage).getAbsolutePath();
					if (!errorMessage.isNull()) {
						throw new BuildException(errorMessage.get());
					}
				} else {
					proguardConfigPath = this.configurationFile.getAbsolutePath();
				}

				AbstractBootstrap.executeProguardStyle(proguardConfigPath, this.getProject().getProperties());
			} catch (Exception exception) {
				exception.printStackTrace();
				throw new BuildException(exception.toString());
			}
		}
	}

	public void setAllowaccessmodification(boolean allowAccessModification) {
		if (allowAccessModification) {
			if (this.config.length() > 0) {
				this.config.append(' ');
			}

			this.config.append("-allowaccessmodification");
		}
	}

	public void setAndroid(boolean android) {
		if (!android) {
			if (this.config.length() > 0) {
				this.config.append(' ');
			}

			this.config.append("-android");
		}
	}

	public void setApplymapping(File applyMappingFile) {
		if (this.config.length() > 0) {
			this.config.append(' ');
		}

		this.config.append("-applymapping");
		this.config.append(' ');
		this.config.append('"');
		this.config.append(applyMappingFile.getAbsolutePath());
		this.config.append('"');
	}

	public void setClassobfuscationdictionary(File classDictionaryFile) {
		if (this.config.length() > 0) {
			this.config.append(' ');
		}

		this.config.append("-classobfuscationdictionary");
		this.config.append(' ');
		this.config.append('"');
		this.config.append(classDictionaryFile.getAbsolutePath());
		this.config.append('"');
	}

	public void setConfiguration(File configurationFile) {
		this.configurationFile = configurationFile;
	}

	public void setDefaultpackage(String defaultPackage) {
		this.setRepackageclasses(defaultPackage);
	}

	public void setDump(File dumpFile) {
		if (this.config.length() > 0) {
			this.config.append(' ');
		}

		this.config.append("-dump");
		this.config.append(' ');
		this.config.append('"');
		this.config.append(dumpFile.getAbsolutePath());
		this.config.append('"');
	}

	public void setFlattenpackagehierarchy(String flattenPackageHierarchy) {
		if (this.config.length() > 0) {
			this.config.append(' ');
		}

		this.config.append("-flattenpackagehierarchy");
		if (flattenPackageHierarchy != null) {
			this.config.append(' ');
			this.config.append('"');
			this.config.append(flattenPackageHierarchy);
			this.config.append('"');
		}
	}

	public void setForceprocessing(boolean forceProcessing) {
		if (forceProcessing) {
			if (this.config.length() > 0) {
				this.config.append(' ');
			}

			this.config.append("-forceprocessing");
		}
	}

	public void setIgnorewarnings(boolean ignoreWarnings) {
		if (ignoreWarnings) {
			if (this.config.length() > 0) {
				this.config.append(' ');
			}

			this.config.append("-ignorewarnings");
		}
	}

	public void setKeepparameternames(boolean keepParameterNames) {
		if (keepParameterNames) {
			if (this.config.length() > 0) {
				this.config.append(' ');
			}

			this.config.append("-keepparameternames");
		}
	}

	public void setMergeinterfacesaggressively(boolean mergeInterfacesAggressively) {
		if (mergeInterfacesAggressively) {
			if (this.config.length() > 0) {
				this.config.append(' ');
			}

			this.config.append("-mergeinterfacesaggressively");
		}
	}

	public void setMicroedition(boolean microEdition) {
		if (!microEdition) {
			if (this.config.length() > 0) {
				this.config.append(' ');
			}

			this.config.append("-microedition");
		}
	}

	public void setNote(boolean note) {
		if (!note) {
			if (this.config.length() > 0) {
				this.config.append(' ');
			}

			this.config.append("-dontnote");
		}
	}

	public void setObfuscate(boolean obfuscate) {
		if (!obfuscate) {
			if (this.config.length() > 0) {
				this.config.append(' ');
			}

			this.config.append("-dontobfuscate");
		}
	}

	public void setObfuscationdictionary(File obfuscationDictionaryFile) {
		if (this.config.length() > 0) {
			this.config.append(' ');
		}

		this.config.append("-obfuscationdictionary");
		this.config.append(' ');
		this.config.append('"');
		this.config.append(obfuscationDictionaryFile.getAbsolutePath());
		this.config.append('"');
	}

	public void setOptimizationpasses(int optimizationPasses) {
		if (this.config.length() > 0) {
			this.config.append(' ');
		}

		this.config.append("-optimizationpasses");
		this.config.append(' ');
		this.config.append(optimizationPasses);
	}

	public void setOptimize(boolean optimize) {
		if (!optimize) {
			if (this.config.length() > 0) {
				this.config.append(' ');
			}

			this.config.append("-dontoptimize");
		}
	}

	public void setOverloadaggressively(boolean overloadAggressively) {
		if (overloadAggressively) {
			if (this.config.length() > 0) {
				this.config.append(' ');
			}

			this.config.append("-overloadaggressively");
		}
	}

	public void setPackageobfuscationdictionary(File packageDictionaryFile) {
		if (this.config.length() > 0) {
			this.config.append(' ');
		}

		this.config.append("-packageobfuscationdictionary");
		this.config.append(' ');
		this.config.append('"');
		this.config.append(packageDictionaryFile.getAbsolutePath());
		this.config.append('"');
	}

	public void setPreverify(boolean preverify) {
		if (!preverify) {
			if (this.config.length() > 0) {
				this.config.append(' ');
			}

			this.config.append("-dontpreverify");
		}
	}

	public void setPrintconfiguration(File printConfigurationFile) {
		if (this.config.length() > 0) {
			this.config.append(' ');
		}

		this.config.append("-printconfiguration");
		this.config.append(' ');
		this.config.append('"');
		this.config.append(printConfigurationFile.getAbsolutePath());
		this.config.append('"');
	}

	public void setPrintmapping(File printMappingFile) {
		if (this.config.length() > 0) {
			this.config.append(' ');
		}

		this.config.append("-printmapping");
		this.config.append(' ');
		this.config.append('"');
		this.config.append(printMappingFile.getAbsolutePath());
		this.config.append('"');
	}

	public void setPrintseeds(File printSeedsFile) {
		if (this.config.length() > 0) {
			this.config.append(' ');
		}

		this.config.append("-printseeds");
		this.config.append(' ');
		this.config.append('"');
		this.config.append(printSeedsFile.getAbsolutePath());
		this.config.append('"');
	}

	public void setPrintusage(File printUsageFile) {
		if (this.config.length() > 0) {
			this.config.append(' ');
		}

		this.config.append("-printusage");
		this.config.append(' ');
		this.config.append('"');
		this.config.append(printUsageFile.getAbsolutePath());
		this.config.append('"');
	}

	public void setRenamesourcefileattribute(String renameSourceFileAttribute) {
		if (this.config.length() > 0) {
			this.config.append(' ');
		}

		this.config.append("-renamesourcefileattribute");
		if (renameSourceFileAttribute != null) {
			this.config.append(' ');
			this.config.append('"');
			this.config.append(renameSourceFileAttribute);
			this.config.append('"');
		}
	}

	public void setRepackageclasses(String repackageClasses) {
		if (this.config.length() > 0) {
			this.config.append(' ');
		}

		this.config.append("-repackageclasses");
		if (repackageClasses != null) {
			this.config.append(' ');
			this.config.append('"');
			this.config.append(repackageClasses);
			this.config.append('"');
		}
	}

	public void setShrink(boolean shrink) {
		if (!shrink) {
			if (this.config.length() > 0) {
				this.config.append(' ');
			}

			this.config.append("-dontshrink");
		}
	}

	public void setSkipnonpubliclibraryclasses(boolean skipNonPublicLibraryClasses) {
		if (this.config.length() > 0) {
			this.config.append(' ');
		}

		if (skipNonPublicLibraryClasses) {
			this.config.append("-skipnonpubliclibraryclasses");
		} else {
			this.config.append("-dontskipnonpubliclibraryclasses");
		}
	}

	public void setSkipnonpubliclibraryclassmembers(boolean skipNonPublicLibraryMembers) {
		if (!skipNonPublicLibraryMembers) {
			if (this.config.length() > 0) {
				this.config.append(' ');
			}

			this.config.append("-dontskipnonpubliclibraryclasses");
		}
	}

	public void setTarget(String target) {
		if (this.config.length() > 0) {
			this.config.append(' ');
		}

		this.config.append("-target");
		this.config.append(' ');
		this.config.append(target);
	}

	public void setUsemixedcaseclassnames(boolean useMixedCaseClassNames) {
		if (!useMixedCaseClassNames) {
			if (this.config.length() > 0) {
				this.config.append(' ');
			}

			this.config.append("-dontusemixedcaseclassnames");
		}
	}

	public void setUseuniqueclassmembernames(boolean useUniqueClassMemberNames) {
		if (useUniqueClassMemberNames) {
			if (this.config.length() > 0) {
				this.config.append(' ');
			}

			this.config.append("-useuniqueclassmembernames");
		}
	}

	public void setVerbose(boolean verbose) {
		if (verbose) {
			if (this.config.length() > 0) {
				this.config.append(' ');
			}

			this.config.append("-verbose");
		}
	}

	public void setWarn(boolean warn) {
		if (!warn) {
			if (this.config.length() > 0) {
				this.config.append(' ');
			}

			this.config.append("-dontwarn");
		}
	}
}
