package eu.europeana.api.translation.web.service;

import java.io.IOException;
import javax.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.tasks.v2.stub.CloudTasksStubSettings;
import com.google.cloud.translate.v3.TranslationServiceClient;
import com.google.cloud.translate.v3.TranslationServiceSettings;

public class GoogleTranslationServiceClientWrapper {

  private final Logger logger = LogManager.getLogger(getClass());
  
  /**Client that will be used to send requests. This client only needs to be created
   * once, and can be reused for multiple requests. After completing all of your requests, call
   * the "close" method on the client to safely clean up any remaining background resources.
   */
  private TranslationServiceClient client;
  
  public GoogleTranslationServiceClientWrapper(String projectId, boolean useHttpClient) {
    initClient(projectId, useHttpClient);
  }
  
  private void initClient(String projectId, boolean useHttpClient) {
    // allow service mocking
    final boolean initClientConnection = !"google-test".equals(projectId);
    if(! initClientConnection) {
      return;
    }
    
    try {
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
    } catch (IOException e) {
      throw new RuntimeException("Cannot instantiate Google TranslationServiceClient!", e);
    }    
  }

  public TranslationServiceClient getClient() {
    return client;
  }
  
  @PreDestroy
  public void close() {
    if (this.client != null) {
      if(logger.isDebugEnabled()) {
        logger.debug("Shutting down GoogleTranslationService client.");
      }
      try {
        this.client.close();
      } catch (RuntimeException e) {
        if(logger.isInfoEnabled()) {
          logger.info("Unexpected error occured when closing translation service.", e);
        }
      }
    }
  }
  
}
