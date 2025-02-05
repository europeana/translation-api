package eu.europeana.api.translation.service.tika;

import java.util.List;
import java.util.Set;
import eu.europeana.api.translation.service.util.ThresholdByTextLength;

/**
 * Constants used by the Apache Tika language detection service
 * 
 * @author Nuno Freire
 * @since 29/01/2025
 */
public class ApacheTikaConstants {

	protected static final Set<String> supportedLanguages = Set.of("af", "an", "ar", "ast", "be", "br", "ca", "bg",
			"bn", "cs", "cy", "da", "de", "el", "en", "es", "et", "eu", "fa", "fi", "fr", "ga", "gl", "gu", "he", "hi",
			"hr", "ht", "hu", "id", "is", "it", "ja", "km", "kn", "ko", "lt", "lv", "mk", "ml", "mr", "ms", "mt", "ne",
			"nl", "no", "oc", "pa", "pl", "pt", "ro", "ru", "sk", "sl", "so", "sq", "sr", "sv", "sw", "ta", "te", "th",
			"tl", "tr", "uk", "ur", "vi", "wa", "yi", "zh-cn", "zh-tw");

	public static final List<ThresholdByTextLength> thresholdsForEdmLanguageHint = List.of(
			new ThresholdByTextLength(0, 20, Double.MAX_VALUE), new ThresholdByTextLength(21, 30, 0.99),
			new ThresholdByTextLength(31, 40, 0.3), new ThresholdByTextLength(41, Integer.MAX_VALUE, 0));

	public static final List<ThresholdByTextLength> thresholdsWithoutHintVeryHighPrecision = List.of(
			new ThresholdByTextLength(0, 40, Double.MAX_VALUE), new ThresholdByTextLength(41, Integer.MAX_VALUE, 0.9));

	public static final List<ThresholdByTextLength> thresholdsWithoutHintHighPrecision = List
			.of(new ThresholdByTextLength(0, 40, 0.99), new ThresholdByTextLength(41, Integer.MAX_VALUE, 0.9));

	public static final List<ThresholdByTextLength> thresholdsWithoutHintMediumPrecision = List
			.of(new ThresholdByTextLength(0, 30, 0.99), new ThresholdByTextLength(31, Integer.MAX_VALUE, 0.5));

	public static final List<ThresholdByTextLength> thresholdsWithoutHintLowPrecision = List.of(
			new ThresholdByTextLength(0, 30, 0.9), new ThresholdByTextLength(31, 40, 0.5),
			new ThresholdByTextLength(41, Integer.MAX_VALUE, 0));

	public static final List<ThresholdByTextLength> thresholdsWithoutHintVeryLowPrecision = List
			.of(new ThresholdByTextLength(0, 30, 0.5), new ThresholdByTextLength(31, Integer.MAX_VALUE, 0));

}
