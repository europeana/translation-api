package eu.europeana.api.translation.tests.web.mock;

import static eu.europeana.api.translation.tests.IntegrationTestUtils.LANG_DETECT_GOOGLE_REQUEST;
import static eu.europeana.api.translation.tests.IntegrationTestUtils.LANG_DETECT_GOOGLE_RESPONSE;
import static eu.europeana.api.translation.tests.IntegrationTestUtils.TRANSLATION_GOOGLE_REQUEST;
import static eu.europeana.api.translation.tests.IntegrationTestUtils.*;
import static eu.europeana.api.translation.tests.IntegrationTestUtils.loadFile;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import com.google.api.core.AbstractApiFuture;
import com.google.api.core.ApiFuture;
import com.google.api.gax.rpc.ApiCallContext;
import com.google.api.gax.rpc.ClientContext;
import com.google.api.gax.rpc.UnaryCallable;
import com.google.cloud.translate.v3.DetectLanguageRequest;
import com.google.cloud.translate.v3.DetectLanguageResponse;
import com.google.cloud.translate.v3.TranslateTextRequest;
import com.google.cloud.translate.v3.TranslateTextResponse;
import com.google.cloud.translate.v3.stub.HttpJsonTranslationServiceStub;
import com.google.cloud.translate.v3.stub.TranslationServiceStubSettings;
import com.google.protobuf.util.JsonFormat;

public class MockGServiceStub extends HttpJsonTranslationServiceStub{

  static TranslationServiceStubSettings settings = buildSettings();
  public static final Map<String, String> TRANSLATION_RESPONSE_MAP = initTranslationMap();
  public static final Map<String, String> LANG_DETECT_RESPONSE_MAP = initLangDetectMap();
  
  private static Map<String, String> initTranslationMap() {
    try {
      return Map.of(
          loadFile(TRANSLATION_GOOGLE_REQUEST).replaceAll("\r", "\n").trim(), loadFile(TRANSLATION_GOOGLE_RESPONSE),
          loadFile(TRANSLATION_GOOGLE_REQUEST_NO_SRC_LANG).replaceAll("\r", "\n").trim(), loadFile(TRANSLATION_GOOGLE_RESPONSE_NO_SRC_LANG)
      );
    } catch (IOException e) {
      throw new RuntimeException("Test initialization exception (translation map)!", e);
    }
  }

  private static Map<String, String> initLangDetectMap() {
    try {
      return Map.of(
          loadFile(LANG_DETECT_GOOGLE_REQUEST).replaceAll("\r", "\n").trim(), loadFile(LANG_DETECT_GOOGLE_RESPONSE)
      );
    } catch (IOException e) {
      throw new RuntimeException("Test initialization exception (lang detect map)!", e);
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
      
      HttpJsonFutureTranslate futureCall;
      
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
          final com.google.cloud.translate.v3.TranslateTextResponse.Builder responseBuilder = TranslateTextResponse.newBuilder();
          String response = getResponse(request);
          JsonFormat.parser().ignoringUnknownFields().merge(response, responseBuilder);
          resp = responseBuilder.build();          
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        
        futureCall = new HttpJsonFutureTranslate(request, resp); 
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
  
  @Override
  public UnaryCallable<DetectLanguageRequest, DetectLanguageResponse> detectLanguageCallable() {
    
    return new UnaryCallable<DetectLanguageRequest, DetectLanguageResponse>() {
      
      HttpJsonFutureDetect futureCall;
      
      @Override
      public ApiFuture<DetectLanguageResponse> futureCall(DetectLanguageRequest request, ApiCallContext thisCallContext) {
        return buildResponse(request);
      }

      public DetectLanguageResponse call(DetectLanguageRequest request) {
        try {
          return buildResponse(request).get();
        } catch (InterruptedException | ExecutionException e) {
          throw new RuntimeException(e);
        }
      }
      
      private ApiFuture<DetectLanguageResponse> buildResponse(DetectLanguageRequest request) {
        DetectLanguageResponse resp ;
        try {
          final com.google.cloud.translate.v3.DetectLanguageResponse.Builder responseBuilder = DetectLanguageResponse.newBuilder();
          String response = getResponse(request);
          JsonFormat.parser().ignoringUnknownFields().merge(response, responseBuilder);
          resp = responseBuilder.build();          
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        
        futureCall = new HttpJsonFutureDetect(request, resp); 
        return futureCall;
      }

      private String getResponse(DetectLanguageRequest request) {
        String req = request.toString().trim();
        String response = LANG_DETECT_RESPONSE_MAP.getOrDefault(req, null);
        if(response != null) {
          return response;
        }
        for (Map.Entry<String, String> entry : LANG_DETECT_RESPONSE_MAP.entrySet()) {
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
  
  
  class HttpJsonFutureTranslate extends AbstractApiFuture<TranslateTextResponse> {
    TranslateTextRequest req;
    TranslateTextResponse resp;
    
    private HttpJsonFutureTranslate(TranslateTextRequest req, TranslateTextResponse resp) {
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

  class HttpJsonFutureDetect extends AbstractApiFuture<DetectLanguageResponse> {
    DetectLanguageRequest req;
    DetectLanguageResponse resp;
    
    private HttpJsonFutureDetect(DetectLanguageRequest req, DetectLanguageResponse resp) {
      this.req = req;
      this.resp = resp;
    }
    
    @Override
    public boolean isDone() {
      return resp != null;
    }

    @Override
    public DetectLanguageResponse get() throws InterruptedException, ExecutionException {
      return resp;
    }
    
    @Override
    protected void interruptTask() {
//      call.cancel("HttpJsonFuture was cancelled", null);
      System.out.println("Call to interrupt req: " + req);
    }

    @Override
    public boolean set(DetectLanguageResponse value) {
      return super.set(value);
    }

    @Override
    public boolean setException(Throwable throwable) {
      return super.setException(throwable);
    }
  }

}
