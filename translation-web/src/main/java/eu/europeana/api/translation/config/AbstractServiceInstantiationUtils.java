package eu.europeana.api.translation.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.api.commons.definitions.utils.LoggingUtils;
import eu.europeana.api.translation.config.services.DetectServiceCfg;
import eu.europeana.api.translation.service.LanguageDetectionService;
import eu.europeana.api.translation.service.exception.LangDetectionServiceConfigurationException;
import eu.europeana.api.translation.service.exception.TranslationServiceConfigurationException;
import eu.europeana.api.translation.service.google.GoogleLangDetectService;
import eu.europeana.api.translation.service.google.GoogleTranslationServiceClientWrapper;
import eu.europeana.api.translation.service.threshold.ThresholdsConfiguration;

/**
 * Class containing utility methods for service instantiation
 */
public abstract class AbstractServiceInstantiationUtils {

  private static final Logger LOG = LogManager.getLogger(AbstractServiceInstantiationUtils.class);

  public static final char SLASH = '/';
  
  abstract TranslationConfig getTranslationConfig();
  abstract GoogleTranslationServiceClientWrapper getGoogleTranslationServiceClientWrapper() throws IOException;

  /**
   * Creates a new instance of googleTranslationClientWrapper. Use it carefully when creating beans,
   * so that they are singletons in the end
   * 
   * @param translationConfig translation configuration
   * @return new instance of GoogleTranslationServiceClientWrapper
   * @throws IOException if configuration cannot be read
   */
  public static GoogleTranslationServiceClientWrapper createGoogleTranslationClientWrapperInstance(
      TranslationConfig translationConfig) throws IOException {
    return new GoogleTranslationServiceClientWrapper(
        translationConfig.getGoogleTranslateProjectId(), translationConfig.useGoogleHttpClient());
  }

  
  /**
   * Creates a new instance of google language detection service
   * 
   * @param translationConfig translation configuration
   * @param googleTranslationServiceClientWrapper wrapper for google client, mainly used for mocking
   *        service implementation
   * @return new instance of GoogleLangDetectService
   * @throws IOException if configuration cannot be read
   */
  public static GoogleLangDetectService createGoogleDetectServiceInstance(
      TranslationConfig translationConfig,
      GoogleTranslationServiceClientWrapper googleTranslationServiceClientWrapper)
      throws IOException {

    GoogleTranslationServiceClientWrapper clientWrapper =
        (googleTranslationServiceClientWrapper == null)
            ? createGoogleTranslationClientWrapperInstance(translationConfig)
            : googleTranslationServiceClientWrapper;

    return new GoogleLangDetectService(translationConfig.getGoogleTranslateProjectId(),
        clientWrapper);
  }

  LanguageDetectionService createServiceInstance(DetectServiceCfg serviceCfg)
      throws LangDetectionServiceConfigurationException {

    LanguageDetectionService service;

    try {
      Class<?> clazz = Class.forName(serviceCfg.getClassname());
      if (GoogleLangDetectService.class.equals(clazz)) {
        // for google we need to call specific factory method
        service = createGoogleDetectServiceInstance(getTranslationConfig(),
            getGoogleTranslationServiceClientWrapper());
      } else {
        // instantiate service with default constructor
        service = (LanguageDetectionService) clazz.getDeclaredConstructor().newInstance();
      }
      
      //set service ID
      if(StringUtils.isNotEmpty(serviceCfg.getId())) {
        service.setServiceId(serviceCfg.getId());
      }else {
        service.setServiceId(clazz.getSimpleName().toUpperCase(Locale.ENGLISH));
      }
      
        
      if (StringUtils.isNotEmpty(serviceCfg.getConfigFilePath())) {
        service.setThresholdsConf(loadLanguageDetectionThresholds(serviceCfg));
      }
    } catch (ClassNotFoundException | IOException | InstantiationException | IllegalAccessException
        | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
        | SecurityException e) {
      throw new LangDetectionServiceConfigurationException(
          "Cannot instantiate service for class: " + serviceCfg.getClassname(), e);
    }

    return service;
  }

  /**
   * Sets the confidence thresholds for accepting/rejecting a detected language
   * 
   * @param detectServiceCfg the service configuration
   * @return thresholdsConfiguration object
   * @throws LangDetectionServiceConfigurationException when unable to read the configuration
   */
  public ThresholdsConfiguration loadLanguageDetectionThresholds(DetectServiceCfg detectServiceCfg)
      throws LangDetectionServiceConfigurationException {

    String configFileName = detectServiceCfg.getConfigFilePath();
    if (StringUtils.isEmpty(configFileName)) {
      return null;
    }

    InputStream inputStream;
    File languageThresholdsFile = getTranslationConfig().getConfigFile(configFileName);
    if (languageThresholdsFile.exists()) {
      // thresholds config file found in config folder
      try {
        inputStream = new FileInputStream(languageThresholdsFile);
      } catch (FileNotFoundException e) {
        // should actually not happen as the file exists
        throw new LangDetectionServiceConfigurationException(
            "Unexpected error occured when reading configFile: " + configFileName, e);
      }
    } else {
      // load thresholds from resources if available, need to search in the root
      // folder of resources
      String location =
          configFileName.startsWith("" + SLASH) ? configFileName : (SLASH + configFileName);
      inputStream = AbstractServiceInstantiationUtils.class.getResourceAsStream(location);
    }

    if (inputStream == null) {
      throw new LangDetectionServiceConfigurationException(
          "Thresholds configuration file not found, neither in configs nor in classpath: "
              + configFileName);
    }

    try (InputStreamReader rawReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(rawReader)) {

      String configsAsJsonString = readLinesAsString(reader);
      ThresholdsConfiguration thresholdsConf =
          new ObjectMapper().readValue(configsAsJsonString, ThresholdsConfiguration.class);
      thresholdsConf.validateThresholds();

      if (LOG.isInfoEnabled()) {
        LOG.info(
            "Successfully loaded language detection thresholds from config file {}, Values: {}",
            configFileName, thresholdsConf);
      }
      return thresholdsConf;
    } catch (IOException e) {
      throw new LangDetectionServiceConfigurationException(
          "Cannot load language detection thresholds from file: " + languageThresholdsFile, e);
    }
  }

  private String readLinesAsString(BufferedReader reader) {
    return reader.lines().collect(Collectors.joining(System.lineSeparator()));
  }

  /**
   * Load Pangeanic thresholds from config file
   * 
   * @param configFileName the name for the config file
   * @return thresholds as properties
   * @throws TranslationServiceConfigurationException if the configuration file cannot be parsed
   */
  public Properties loadPangeanicTranslationThresholds(String configFileName)
      throws TranslationServiceConfigurationException {

    Properties thresholds = new Properties();

    File languageThresholdsFile = getTranslationConfig().getConfigFile(configFileName);
    if (languageThresholdsFile.exists()) {
      // load thresholds from config file if available
      try (Reader input = Files.newBufferedReader(languageThresholdsFile.toPath())) {
        thresholds.load(input);
        logInfo("Successfully loaded pangeanic thresholds from config file, Values: {}",
            LoggingUtils.sanitizeUserInput(thresholds.toString()));
      } catch (IOException e) {
        throw new TranslationServiceConfigurationException(
            "Cannot load pangeanic language thresholds from config file: " + languageThresholdsFile,
            e);
      }
    } else {
      // load thresholds from resources if available, need to search in the root
      // folder of resources
      try (InputStream input =
          AbstractServiceInstantiationUtils.class.getResourceAsStream("/" + configFileName)) {
        if (input != null) {
          thresholds.load(input);
          logInfo("Successfully loaded pangeanic thresholds from resources, Values: {}",
              LoggingUtils.sanitizeUserInput(thresholds.toString()));
        }
      } catch (IOException e) {
        throw new TranslationServiceConfigurationException(
            "Cannot load pangeanic languae thresholds from file: " + languageThresholdsFile, e);
      }
    }

    // load properties
    if (thresholds.isEmpty()) {
      logInfo("No configurations found for pangeanic language thresholds available.");
    }

    return thresholds;
  }

  private void logInfo(final String message, Object... params) {
    if (LOG.isInfoEnabled()) {
      LOG.info(message, params);
    }
  }

}
