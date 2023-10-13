package eu.europeana.api.translation.service.google;

import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.tasks.v2.stub.CloudTasksStubSettings;
import com.google.cloud.translate.v3.TranslationServiceClient;
import com.google.cloud.translate.v3.TranslationServiceSettings;

public class GoogleTranslationServiceClientWrapper {

  private static final String MOCK_CLIENT_PROJ_ID = "google-test";

  private final Logger logger = LogManager.getLogger(getClass());
  
  /**Client that will be used to send requests. This client only needs to be created
   * once, and can be reused for multiple requests. After completing all of your requests, call
   * the "close" method on the client to safely clean up any remaining background resources.
   */
  private TranslationServiceClient client;
  private boolean closed;
  
  public GoogleTranslationServiceClientWrapper(String projectId, boolean useHttpClient) throws IOException {
    initClient(projectId, useHttpClient);
  }
  
  private void initClient(String projectId, boolean useHttpClient) throws IOException {
    // allow service mocking
    final boolean skipInitialization = isMockService(projectId);
    if(skipInitialization) {
      return;
    }
    
    // gRPC doesn't like communication via the socks proxy (throws an error) and also doesn't
    // support the
    // socksNonProxyHosts settings, so this is to tell it to by-pass the configured proxy
    if (useHttpClient) {
      TranslationServiceSettings translationServiceSettings =
          TranslationServiceSettings.newHttpJsonBuilder().build();
      
      logger.info("GoogleTranslationService initialised, projectId = {}", projectId);
      this.client=TranslationServiceClient.create(translationServiceSettings);
    } else {
      TransportChannelProvider transportChannelProvider = CloudTasksStubSettings
          .defaultGrpcTransportProviderBuilder()
          .setChannelConfigurator(
              managedChannelBuilder -> managedChannelBuilder.proxyDetector(socketAddress -> null))
          .build();

      TranslationServiceSettings tss;
      tss = TranslationServiceSettings.newBuilder()
          .setTransportChannelProvider(transportChannelProvider).build();
      
      logger.info("GoogleTranslationService initialised, projectId = {}", projectId);
      this.client=TranslationServiceClient.create(tss);
    }  
    this.closed=false;
  }

  private boolean isMockService(String projectId) {
    return MOCK_CLIENT_PROJ_ID.equals(projectId);
  }

  public TranslationServiceClient getClient() {
    return client;
  }
  
  //only for the tests (mocked client)
  public void setClient(TranslationServiceClient client) {
    this.client=client;
  }
  
  public void close() {
    if (!this.closed && this.client!=null) {
      if(logger.isDebugEnabled()) {
        logger.debug("Shutting down GoogleTranslationService client.");
      }
      try {
        this.client.close();
        this.closed=true;
      } catch (RuntimeException e) {
        if(logger.isInfoEnabled()) {
          logger.info("Unexpected error occured when closing translation service.", e);
        }
      }
    }
  }
  
}
