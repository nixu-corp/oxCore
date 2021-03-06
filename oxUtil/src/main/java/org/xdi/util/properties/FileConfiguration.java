/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.util.properties;


import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.xdi.util.ArrayHelper;

/**
 * @author Yuriy Movchan Date: 03.29.2011
 */
public class FileConfiguration {

	private static final Logger log = Logger.getLogger(FileConfiguration.class);

	private String fileName;
	private boolean isResource;
	private boolean loaded;

	protected PropertiesConfiguration propertiesConfiguration;
	protected String configurationFolderPrefix = "";

	protected Properties properties;

	private final ReentrantLock reloadLock = new ReentrantLock();
	private boolean isReload = false;

	public FileConfiguration(String fileName) {
		this(fileName, false);
	}

	public FileConfiguration(String fileName, boolean isResource) {
		this.fileName = fileName;
		this.isResource = isResource;
		this.loaded = false;

		if (isResource) {
			loadResourceProperties();
		} else {
			initProperties();
			loadProperties();
		}
	}

	protected void initProperties() {
		String tomcatHome = System.getProperty("catalina.home");
		if (tomcatHome != null) {
			log.debug("Setting the tomcat home directory");
			setConfigurationFolderPrefix(System.getProperty("catalina.home") + File.separator + "conf" + File.separator);
		}
		log.debug(String.format("Loading '%s' configuration file from tomcat config folder", this.fileName));
	}

	protected void loadProperties() {
		try {
			this.propertiesConfiguration = new PropertiesConfiguration(configurationFolderPrefix + this.fileName);
			this.loaded = true;
		} catch (ConfigurationException ex) {
			log.debug(String.format("Failed to load '%s' configuration file from tomcat config folder", this.fileName));
		}
	}

	protected void loadResourceProperties() {
		log.debug(String.format("Loading '%s' configuration file from resources", this.fileName));
		try {
			this.propertiesConfiguration = new PropertiesConfiguration(this.fileName);
			this.loaded = true;
		} catch (ConfigurationException ex) {
			log.debug(String.format("Failed to load '%s' configuration file from resources", this.fileName));
		}
	}

	public void reloadProperties() {
		this.isReload = true;

		reloadLock.lock(); // block until condition holds
		try {
			if (!this.isReload) {
				return;
			}

			this.properties = null;
			this.loaded = false;

			if (this.isResource) {
				loadResourceProperties();
			} else {
				loadProperties();
			}
		} finally {
            reloadLock.unlock(); // first unlock, for some reason findbug reported this?
            this.isReload = false;
		}
	}

	public String getFileName() {
		return fileName;
	}

	public boolean isLoaded() {
		return loaded;
	}

	public String getConfigurationFolderPrefix() {
		return configurationFolderPrefix;
	}

	public void setConfigurationFolderPrefix(String configurationFolderPrefix) {
		this.configurationFolderPrefix = configurationFolderPrefix;
	}

	public void saveProperties() {
		try {
			this.propertiesConfiguration.save();
		} catch (ConfigurationException ex) {
			log.debug(String.format("Failed to save '%s' configuration file to tomcat config folder", this.fileName));
		}
	}

	public Properties getProperties() {
		if (properties == null) {
			properties = new Properties();

			Iterator<?> keys = this.propertiesConfiguration.getKeys();
			while (keys.hasNext()) {
				String key = (String) keys.next();
				properties.put(key, getString(key));
			}
		}

		return properties;
	}

	public Properties getPropertiesByPrefix(String propertiesPrefix) {
		Properties properties = new Properties();

		Iterator<?> keys = this.propertiesConfiguration.getKeys();
		while (keys.hasNext()) {
			String key = (String) keys.next();

			if (key.startsWith(propertiesPrefix)) {
				properties.put(key.substring(propertiesPrefix.length()), getString(key));
			}
		}

		return properties;
	}

	public String getString(String key) {
		String values[] = this.propertiesConfiguration.getStringArray(key);
		if (values.length > 0) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < values.length - 1; i++) {
				sb.append(values[i]).append(',');
			}
			sb.append(values[values.length - 1]);

			return sb.toString();
		}

		return null;
	}

	public String getString(String key, String defaultValue) {
		try {
			return getString(key);
		} catch (NoSuchElementException ex) {
			return defaultValue;
		}
	}

	public String[] getStringArray(String key) {
		String result[] = this.propertiesConfiguration.getStringArray(key);
		if (ArrayHelper.isNotEmpty(result) && (result.length == 1) && (result[0].trim().length() == 0)) {
			result = new String[0];
		}

		return result;
	}

	public String[] getStringArray(String key, String[] defaultValue) {
		try {
			return getStringArray(key);
		} catch (NoSuchElementException ex) {
			return defaultValue;
		}
	}

	public List<String> getStringList(String key) {
		String values[] = getStringArray(key);
		List<String> result = new ArrayList<String>(values.length);
		for (String value : values) {
			result.add(value);
		}

		return result;
	}

	public int getInt(String key) {
		return this.propertiesConfiguration.getInt(key);
	}

	public int getInt(String key, int defaultValue) {
		try {
			return getInt(key);
		} catch (NoSuchElementException ex) {
			return defaultValue;
		}
	}

	public boolean getBoolean(String key) {
		return this.propertiesConfiguration.getBoolean(key);
	}

	public boolean getBoolean(String key, boolean defaultValue) {
		try {
			return getBoolean(key);
		} catch (NoSuchElementException ex) {
			return defaultValue;
		}
	}

	public int getCountItems(String pattern) {
		int i = 0;
		while (this.propertiesConfiguration.containsKey(String.format(pattern, ++i)))
			;

		return i - 1;
	}

	public void setString(String key, String value) {
		this.propertiesConfiguration.setProperty(key, value);
	}

}