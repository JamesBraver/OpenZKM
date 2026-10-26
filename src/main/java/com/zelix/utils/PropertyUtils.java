package com.zelix.utils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Properties;

public class PropertyUtils {
	public static void loadSystemProperties() {
		try {
			Map<String, String> env = System.getenv();
			if (GlobalUtils.findHashedProperty(
					env, "1959b123224b9911f43aabc5015503e06cfb8cb223f861b863a0f371d47e95315e2fb40c4b7881ea46e13f51de29a1f4033898ee2338f7311fd58e583912e51a", "false"
				)
				.equals("true")) {
				for (Entry<String, String> entry : env.entrySet()) {
					System.getProperty(entry.getKey());
				}
			}
		} catch (SecurityException ignored) {
		}

		File propsFile = new File("ZKM_System.properties");
		if (propsFile.exists() && !propsFile.isDirectory()) {
			try {
				try (FileReader fileReader = new FileReader(propsFile)) {
					Properties properties = new Properties();
					properties.load(fileReader);

					for (String propertyName : properties.stringPropertyNames()) {
						System.setProperty(propertyName, properties.getProperty(propertyName));
					}
				}
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
		}
	}
}
