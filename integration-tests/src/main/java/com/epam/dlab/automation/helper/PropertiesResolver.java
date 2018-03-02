/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

package com.epam.dlab.automation.helper;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Properties;

public class PropertiesResolver {

    private static final Logger LOGGER = LogManager.getLogger(PropertiesResolver.class);
    public static final boolean DEV_MODE;
    public static final String CONFIG_FILE_NAME = "application.properties";
    public static String NOTEBOOK_SCENARIO_FILES_LOCATION_PROPERTY_TEMPLATE = "scenario.%s.files.location";
    public static String NOTEBOOK_TEST_TEMPLATES_LOCATION = "%s.test.templates.location";
    public static String NOTEBOOK_CONFIGURATION_FILE_TEMPLATE = "%s/%s-notebook.json";

    //keys from application.properties(dev-application.properties)
    private final static String CONF_FILE_LOCATION_PROPERTY = "conf.file.location";
    private static String KEYS_DIRECTORY_LOCATION_PROPERTY = "keys.directory.location";
    private static String NOTEBOOK_TEST_DATA_COPY_SCRIPT = "notebook.test.data.copy.script";
    private static String NOTEBOOK_TEST_LIB_LOCATION = "notebook.test.lib.location";

    private static String SCENARIO_JUPYTER_FILES_LOCATION_PROPERTY = "scenario.jupyter.files.location";
    private static String SCENARIO_RSTUDIO_FILES_LOCATION_PROPERTY = "scenario.rstudio.files.location";
    private static String SCENARIO_ZEPPELIN_FILES_LOCATION_PROPERTY = "scenario.zeppelin.files.location";
    private static String SCENARIO_TENSOR_FILES_LOCATION_PROPERTY = "scenario.tensor.files.location";
    private static String SCENARIO_DEEPLEARNING_FILES_LOCATION_PROPERTY = "scenario.deeplearning.files.location";

    private static String JUPYTER_TEST_TEMPLATES_LOCATION_PROPERTY = "jupyter.test.templates.location";
    private static String RSTUDIO_TEST_TEMPLATES_LOCATION_PROPERTY = "rstudio.test.templates.location";
    private static String ZEPPELIN_TEST_TEMPLATES_LOCATION_PROPERTY = "zeppelin.test.templates.location";
    private static String TENSOR_TEST_TEMPLATES_LOCATION_PROPERTY = "tensor.test.templates.location";
    private static String DEEPLEARNING_TEST_TEMPLATES_LOCATION_PROPERTY = "deeplearning.test.templates.location";

    private static String CLUSTER_CONFIG_FILE_LOCATION_PROPERTY = "ec2.config.files.location";
    private static String AZURE_CONFIG_FILE_LOCATION_PROPERTY = "azure.config.files.location";
    private static String GCP_CONFIG_FILE_LOCATION_PROPERTY = "gcp.config.files.location";

    public static String getJupyterTestTemplatesLocationProperty() {
        return JUPYTER_TEST_TEMPLATES_LOCATION_PROPERTY;
    }

    public static String getRstudioTestTemplatesLocationProperty() {
        return RSTUDIO_TEST_TEMPLATES_LOCATION_PROPERTY;
    }

    public static String getZeppelinTestTemplatesLocationProperty() {
        return ZEPPELIN_TEST_TEMPLATES_LOCATION_PROPERTY;
    }

    public static String getTensorTestTemplatesLocationProperty() {
        return TENSOR_TEST_TEMPLATES_LOCATION_PROPERTY;
    }

    public static String getDeeplearningTestTemplatesLocationProperty() {
        return DEEPLEARNING_TEST_TEMPLATES_LOCATION_PROPERTY;
    }

    private static Properties properties = new Properties();

    static {
        DEV_MODE = System.getProperty("run.mode", "remote").equalsIgnoreCase("dev");
        loadApplicationProperties();
    }

	private static String getProperty(String propertyName, boolean isOptional) {
		String s = System.getProperty(propertyName, "");
		if (s.isEmpty() && !isOptional) {
        	throw new IllegalArgumentException("Missed required JVM argument -D" + propertyName);
        }
        return s;
	}
	
	public static void overlapProperty(Properties props, String propertyName, boolean isOptional) {
		String argName = StringUtils.replaceChars(propertyName, '_', '.').toLowerCase();
		String s = System.getProperty(argName, "");
		if (!s.isEmpty()) {
            props.setProperty(propertyName, s);
        }
		if(!isOptional && props.getProperty(propertyName, "").isEmpty()) {
        	throw new IllegalArgumentException("Missed required argument -D" + argName + " or property " + propertyName);
        }
	}


    private static String getConfRootPath() {
    	return getProperty("conf.root.path", false);
    }

    private static void loadApplicationProperties() {
        InputStream input = null;

        try {
            input = PropertiesResolver.class.getClassLoader().getResourceAsStream(CONFIG_FILE_NAME);

            // load a properties file
            properties.load(input);
            String rootPath = getConfRootPath();
            for (String key : properties.keySet().toArray(new String[0])) {
            	String path = StringUtils.replace(properties.getProperty(key), "${CONF_ROOT_PATH}", rootPath);
            	path = Paths.get(path).toAbsolutePath().toString();
            	properties.setProperty(key, path);
            }
            overlapProperty(properties, CONF_FILE_LOCATION_PROPERTY, false);

            // get the property value and print it out
            LOGGER.info(properties.getProperty(CONF_FILE_LOCATION_PROPERTY));
            LOGGER.info(properties.getProperty(KEYS_DIRECTORY_LOCATION_PROPERTY));
            LOGGER.info(properties.getProperty(NOTEBOOK_TEST_DATA_COPY_SCRIPT));
            LOGGER.info(properties.getProperty(NOTEBOOK_TEST_LIB_LOCATION));
            LOGGER.info(properties.getProperty(SCENARIO_JUPYTER_FILES_LOCATION_PROPERTY));
            LOGGER.info(properties.getProperty(SCENARIO_RSTUDIO_FILES_LOCATION_PROPERTY));
            LOGGER.info(properties.getProperty(SCENARIO_ZEPPELIN_FILES_LOCATION_PROPERTY));
            LOGGER.info(properties.getProperty(SCENARIO_TENSOR_FILES_LOCATION_PROPERTY));
            LOGGER.info(properties.getProperty(SCENARIO_DEEPLEARNING_FILES_LOCATION_PROPERTY));
            LOGGER.info(properties.getProperty(JUPYTER_TEST_TEMPLATES_LOCATION_PROPERTY));
            LOGGER.info(properties.getProperty(RSTUDIO_TEST_TEMPLATES_LOCATION_PROPERTY));
            LOGGER.info(properties.getProperty(ZEPPELIN_TEST_TEMPLATES_LOCATION_PROPERTY));
            LOGGER.info(properties.getProperty(TENSOR_TEST_TEMPLATES_LOCATION_PROPERTY));
            LOGGER.info(properties.getProperty(DEEPLEARNING_TEST_TEMPLATES_LOCATION_PROPERTY));
            LOGGER.info(properties.getProperty(CLUSTER_CONFIG_FILE_LOCATION_PROPERTY));

        } catch (IOException ex) {
            LOGGER.error(ex);
            LOGGER.error("Application configuration file could not be found by the path: {}", CONFIG_FILE_NAME);
            System.exit(0);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    LOGGER.error(e);
                    LOGGER.error("Application configuration file could not be found by the path: {}", CONFIG_FILE_NAME);
                }
            }
        }
    }


    public static String getConfFileLocation() {
        return properties.getProperty(CONF_FILE_LOCATION_PROPERTY);
    }

    public static String getKeysLocation() {
        return properties.getProperty(KEYS_DIRECTORY_LOCATION_PROPERTY);
    }

    public static String getNotebookTestDataCopyScriptLocation() {
        return properties.getProperty(NOTEBOOK_TEST_DATA_COPY_SCRIPT);
    }

    public static String getNotebookTestLibLocation() {
        return properties.getProperty(NOTEBOOK_TEST_LIB_LOCATION);
    }

    public static String getScenarioJupyterFilesLocation() {
        return properties.getProperty(SCENARIO_JUPYTER_FILES_LOCATION_PROPERTY);
    }

    public static String getScenarioRstudioFilesLocation() {
        return properties.getProperty(SCENARIO_RSTUDIO_FILES_LOCATION_PROPERTY);
    }

    public static String getScenarioZeppelinFilesLocation() {
        return properties.getProperty(SCENARIO_ZEPPELIN_FILES_LOCATION_PROPERTY);
    }

    public static String getScenarioTensorFilesLocation() {
        return properties.getProperty(SCENARIO_TENSOR_FILES_LOCATION_PROPERTY);
    }

    public static String getScenarioDeeplearningFilesLocation() {
        return properties.getProperty(SCENARIO_DEEPLEARNING_FILES_LOCATION_PROPERTY);
    }

    public static String getClusterEC2ConfFileLocation() {
        return properties.getProperty(CLUSTER_CONFIG_FILE_LOCATION_PROPERTY );
    }

    public static String getClusterAzureConfFileLocation() {
        return properties.getProperty(AZURE_CONFIG_FILE_LOCATION_PROPERTY );
    }

    public static String getClusterGcpConfFileLocation() {
        return properties.getProperty(GCP_CONFIG_FILE_LOCATION_PROPERTY);
    }

    public static String getPropertyByName(String propertyName) {
        return properties.getProperty(propertyName);
    }
}
