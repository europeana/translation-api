package eu.europeana.api.translation.client.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class TranslationClientConfiguration {

    private static final Logger LOGGER = LogManager.getLogger(TranslationClientConfiguration.class);

    public static final String PROPERTIES_FILE = "/translation-api-client.properties";
    public static final String TRANSLATION_API_URL = "translation.api.endpoint";

    Properties properties;

    public TranslationClientConfiguration() {
        loadProperties(PROPERTIES_FILE);
    }

    public TranslationClientConfiguration(Properties properties) {
        this.properties = properties;
    }

    private Properties loadProperties(String propertiesFile) {
        properties = new Properties();
        try (InputStream stream = getClass().getResourceAsStream(propertiesFile)) {
            properties.load(stream);
        } catch (IOException e) {
            LOGGER.error("Error loading the properties file {}", PROPERTIES_FILE);
        }
        return properties;
    }

    public String getTranslationApiUrl() {
        return getProperty(TRANSLATION_API_URL);
    }

    private String getProperty(String propertyName) {
        return properties.getProperty(propertyName);
    }
}
