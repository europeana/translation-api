package eu.europeana.api.translation.tests.web;

import java.util.Base64;
import com.google.common.primitives.Ints;

public class TestEncoding {

  public static void main(String args[]) {
    
    // to use inputText.hashCode() in the implementation
    int hascode = 1234567890;
    
    System.out.println(Base64.getEncoder().encodeToString(Ints.toByteArray(hascode)).trim());
     
    System.out.println(
        new String(Base64.getEncoder().withoutPadding().encode(Ints.toByteArray(hascode))));
    
    StringBuilder builder = (new StringBuilder()).append("de").append("en");
    byte[] hash = Base64.getEncoder().withoutPadding().encode(Ints.toByteArray(hascode));
    builder.append(new String(hash));
    System.out.println("Redis Key: " + builder.toString());
  }
}
