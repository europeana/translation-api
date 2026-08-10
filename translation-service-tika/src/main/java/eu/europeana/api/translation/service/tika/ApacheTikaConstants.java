package eu.europeana.api.translation.service.tika;

import java.util.List;
import java.util.Map;
import static java.util.Map.entry;
import java.util.Set;

/**
 * Constants used by the Apache Tika language detection service
 * 
 * @author Nuno Freire
 * @since 29/01/2025
 */
public final class ApacheTikaConstants {

  protected static final Set<String> supportedLanguages = Set.of("af", "an", "ar", "ast", "be", "br", "ca", "bg", "bn",
      "cs", "cy", "da", "de", "el", "en", "es", "et", "eu", "fa", "fi", "fr", "ga", "gl", "gu", "he", "hi", "hr", "ht",
      "hu", "id", "is", "it", "ja", "km", "kn", "ko", "lt", "lv", "mk", "ml", "mr", "ms", "mt", "ne", "nl", "no", "oc",
      "pa", "pl", "pt", "ro", "ru", "sk", "sl", "so", "sq", "sr", "sv", "sw", "ta", "te", "th", "tl", "tr", "uk", "ur",
      "vi", "wa", "yi", "zh-cn", "zh-tw");
  
  public static final Map<String, Set<String>> closeLanguages = Map.ofEntries(
      entry("bg", Set.of("mk", "chu")),
      entry("cs", Set.of("sk", "hsb", "dsb")),
      entry("da", Set.of("no", "sv", "fo", "is")),
      entry("de", Set.of("gsw", "bar", "yi", "ltz")),
      entry("el", Set.of("pnt", "tsd", "cpg", "grc")),
      entry("en", Set.of("sco", "frs", "fy")),
      entry("es", Set.of("ca", "cat", "gl", "glg", "ast", "lns", "ext", "fal", "arg", "lad", "mwl")),
      entry("et", Set.of("fi", "fkv", "izh", "vot")),
      entry("fi", Set.of("et", "krl", "vep", "fkv")),
      entry("fr", Set.of("wln", "pcd", "frp", "oc")),
      entry("ga", Set.of("gd", "gv")),
      entry("hr", Set.of("bs", "sr", "cnr")),
      entry("hu", Set.of("mdf", "myv", "kca", "mns")),
      entry("it", Set.of("scn", "nap", "co", "lij")),
      entry("lt", Set.of("lv", "sgs", "ltg")),
      entry("lv", Set.of("lt", "ltg", "sgs")),
      entry("mt", Set.of("ary", "arz", "acy")),
      entry("nl", Set.of("af", "lim", "nds")),
      entry("pl", Set.of("csb", "szl", "hsb")),
      entry("pt", Set.of("gl", "fal", "mwl")),
      entry("ro", Set.of("ruo", "rup", "rum")),
      entry("sk", Set.of("cs", "pl", "szl")),
      entry("sl", Set.of("hr", "sr", "bs")),
      entry("sv", Set.of("da", "no", "gln", "dlc")),
      entry("no", Set.of("da", "sv", "is", "fo")),
      entry("ca", Set.of("oc", "es", "fr", "it"))
  );
  
  /**
   * No instances
   */
  private ApacheTikaConstants() {
  }
}
