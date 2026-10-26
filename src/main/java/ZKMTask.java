import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ZKMTask extends Task {
	public static final String MISSING_SCRIPT_MSG = "Missing or empty ZKM Script file name";
	public static final String TRUE = "true";
	public static final String YES = "yes";
	private boolean isVerbose = false;
	private boolean isParseOnly = false;
	public List<Sysproperty> sysproperties = new ArrayList<>();
	private String defaultDirectoryName;
	private String defaultExcludeFileName;
	private String defaultMethodParameterChangesExcludeFileName;
	private String defaultMethodParameterObfuscationExcludeFileName;
	private String defaultTrimExcludeFileName;
	private String logFileName;
	private String scriptFileName;
	private String trimLogFileName;

	public Sysproperty createSysproperty() {
		Sysproperty sysproperty = new Sysproperty(this);
		this.sysproperties.add(sysproperty);
		return sysproperty;
	}

	public void execute() {
		if (this.scriptFileName != null && this.scriptFileName.length() != 0) {
			if (this.sysproperties != null) {
				Properties systemProperties = System.getProperties();

				for (Sysproperty sysproperty : this.sysproperties) {
					systemProperties.put(sysproperty.getKey(), sysproperty.getValue());
				}
			}

			try {
				com.zelix.ZKM.executeWithHashtableProperties(
					this.scriptFileName,
					this.logFileName,
					this.trimLogFileName,
					this.defaultExcludeFileName,
					this.defaultTrimExcludeFileName,
					this.defaultMethodParameterChangesExcludeFileName,
					this.defaultMethodParameterObfuscationExcludeFileName,
					this.defaultDirectoryName,
					this.isVerbose,
					this.isParseOnly,
					this.getProject().getProperties()
				);
			} catch (Exception executionException) {
				executionException.printStackTrace();
				throw new BuildException(executionException.toString());
			}
		} else {
			throw new BuildException("Missing or empty ZKM Script file name");
		}
	}

	public void setDefaultDirectoryName(String defaultDirectoryName) {
		this.defaultDirectoryName = defaultDirectoryName;
	}

	public void setDefaultExcludeFileName(String defaultExcludeFileName) {
		this.defaultExcludeFileName = defaultExcludeFileName;
	}

	public void setDefaultMethodParameterChangesExcludeFileName(String changesExcludeFileName) {
		this.defaultMethodParameterChangesExcludeFileName = changesExcludeFileName;
	}

	public void setDefaultMethodParameterObfuscationExcludeFileName(String obfuscationExcludeFileName) {
		this.defaultMethodParameterObfuscationExcludeFileName = obfuscationExcludeFileName;
	}

	public void setDefaultTrimExcludeFileName(String defaultTrimExcludeFileName) {
		this.defaultTrimExcludeFileName = defaultTrimExcludeFileName;
	}

	public void setIsParseOnly(String parseOnly) {
		if (parseOnly != null && (parseOnly.equalsIgnoreCase("true") || parseOnly.equalsIgnoreCase("yes"))) {
			this.isParseOnly = true;
		}
	}

	public void setIsVerbose(String verbose) {
		if (verbose != null && (verbose.equalsIgnoreCase("true") || verbose.equalsIgnoreCase("yes"))) {
			this.isVerbose = true;
		}
	}

	public void setLogFileName(String logFileName) {
		this.logFileName = logFileName;
	}

	public void setScriptFileName(String scriptFileName) {
		this.scriptFileName = scriptFileName;
	}

	public void setTrimLogFileName(String trimLogFileName) {
		this.trimLogFileName = trimLogFileName;
	}

	public static class Sysproperty {
	   private String key;
	   private String value;
	   public final ZKMTask this$0;

	   public Sysproperty(ZKMTask zKMTask) {
		  this.this$0 = zKMTask;
	   }

	   public String getKey() {
		  return this.key;
	   }

	   public String getValue() {
		  return this.value;
	   }

	   public void setKey(String string) {
		  this.key = string;
	   }

	   public void setValue(String string) {
		  this.value = string;
	   }
	}
}
