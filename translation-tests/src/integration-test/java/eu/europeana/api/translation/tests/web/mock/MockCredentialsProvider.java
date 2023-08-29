package eu.europeana.api.translation.tests.web.mock;

import java.io.IOException;
import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;

public class MockCredentialsProvider implements CredentialsProvider {

  MockGoogleCredentials credentials; 
  @Override
  public Credentials getCredentials() throws IOException {
    if (credentials == null) {
      credentials = new MockGoogleCredentials(); 
    }
    return credentials;
  }
}
