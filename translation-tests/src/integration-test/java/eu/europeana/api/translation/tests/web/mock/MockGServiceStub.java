package eu.europeana.api.translation.tests.web.mock;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import com.google.api.core.AbstractApiFuture;
import com.google.api.core.ApiFuture;
import com.google.api.gax.rpc.ApiCallContext;
import com.google.api.gax.rpc.ClientContext;
import com.google.api.gax.rpc.UnaryCallable;
import com.google.cloud.translate.v3.TranslateTextRequest;
import com.google.cloud.translate.v3.TranslateTextResponse;
import com.google.cloud.translate.v3.TranslateTextResponse.Builder;
import com.google.cloud.translate.v3.stub.HttpJsonTranslationServiceStub;
import com.google.cloud.translate.v3.stub.TranslationServiceStubSettings;
import com.google.protobuf.util.JsonFormat;
import static eu.europeana.api.translation.tests.IntegrationTestUtils.*;

public class MockGServiceStub extends HttpJsonTranslationServiceStub{

  static TranslationServiceStubSettings settings = buildSettings();
  public static final Map<String, String> TRANSLATION_RESPONSE_MAP = initTranslationMap();
  
  private static Map<String, String> initTranslationMap() {
    try {
      return Map.of(
          loadFile(TRANSLATION_GOOGLE_REQUEST).replaceAll("\r", "\n").trim(), loadFile(TRANSLATION_GOOGLE_RESPONSE)
      );
    } catch (IOException e) {
      throw new RuntimeException("Test initialization exception!", e);
    }
  }
  
  public MockGServiceStub() throws IOException{
    super(buildSettings(), ClientContext.create(settings));
  }

  private static TranslationServiceStubSettings buildSettings() {
    try {
      final MockCredentialsProvider credentialsProvider = new MockCredentialsProvider();
      settings = TranslationServiceStubSettings
          .newHttpJsonBuilder()
          .setEndpoint("http://localhost:8080/google/translate")
          .setCredentialsProvider(credentialsProvider)
          .build();
      return settings;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  @Override
  public UnaryCallable<TranslateTextRequest, TranslateTextResponse> translateTextCallable() {
    
    return new UnaryCallable<TranslateTextRequest, TranslateTextResponse>() {
      
      HttpJsonFuture futureCall;
      
      @Override
      public ApiFuture<TranslateTextResponse> futureCall(TranslateTextRequest request, ApiCallContext thisCallContext) {
        return buildResponse(request);
      }

      public TranslateTextResponse call(TranslateTextRequest request) {
        try {
          return buildResponse(request).get();
        } catch (InterruptedException | ExecutionException e) {
          throw new RuntimeException(e);
        }
      }
      
      private ApiFuture<TranslateTextResponse> buildResponse(TranslateTextRequest request) {
        TranslateTextResponse resp ;
        try {
          final Builder responseBuilder = TranslateTextResponse.newBuilder();
          String response = getResponse(request);
          JsonFormat.parser().ignoringUnknownFields().merge(response, responseBuilder);
          resp = responseBuilder.build();          
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        
        futureCall = new HttpJsonFuture(request, resp); 
        return futureCall;
      }

      private String getResponse(TranslateTextRequest request) {
        String req = request.toString().trim();
        String response = TRANSLATION_RESPONSE_MAP.getOrDefault(req, null);
        if(response != null) {
          return response;
        }
        for (Map.Entry<String, String> entry : TRANSLATION_RESPONSE_MAP.entrySet()) {
          String key = entry.getKey();
          final String key1 = key.replaceAll(" ", "").replaceAll("\n", "");
          final String req1 = req.replaceAll(" ", "").replaceAll("\n", "");
          if(key1.equals(req1)) {
            response = entry.getValue();
          }
          
        }
        return response;
      }
      
      
    };
   
  }
  
  class HttpJsonFuture extends AbstractApiFuture<TranslateTextResponse> {
    TranslateTextRequest req;
    TranslateTextResponse resp;
    
    private HttpJsonFuture(TranslateTextRequest req, TranslateTextResponse resp) {
      this.req = req;
      this.resp = resp;
    }
    
    @Override
    public boolean isDone() {
      return resp != null;
    }

    @Override
    public TranslateTextResponse get() throws InterruptedException, ExecutionException {
      return resp;
    }
    
    @Override
    protected void interruptTask() {
//      call.cancel("HttpJsonFuture was cancelled", null);
      System.out.println("Call to interrupt req: " + req);
    }

    @Override
    public boolean set(TranslateTextResponse value) {
      return super.set(value);
    }

    @Override
    public boolean setException(Throwable throwable) {
      return super.setException(throwable);
    }
  }

}
