package eu.europeana.api.translation.tests.web.mock;

import com.google.cloud.translate.v3.TranslationServiceClient;
import com.google.cloud.translate.v3.stub.TranslationServiceStub;

public class MockGClient extends TranslationServiceClient {

  public MockGClient(TranslationServiceStub stub){
    super(stub);
  }

}
