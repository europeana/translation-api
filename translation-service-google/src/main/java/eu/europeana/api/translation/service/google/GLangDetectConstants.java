package eu.europeana.api.translation.service.google;

import java.util.List;

import eu.europeana.api.translation.service.util.ThresholdByTextLength;

/**
 * Constants used by the Google language detection service
 * 
 * @author Nuno Freire
 * @since 29/01/2025
 */
public class GLangDetectConstants {

	protected static final List<ThresholdByTextLength> thresholdsForEdmLanguageHint = List.of(
			new ThresholdByTextLength(0, 15, 0.75),
			new ThresholdByTextLength(16, 30, 0.5),
			new ThresholdByTextLength(31, 40, 0.3),
			new ThresholdByTextLength(41, Integer.MAX_VALUE, 0));

	protected static final List<ThresholdByTextLength> thresholdsWithoutHintVeryHighPrecision = List.of(
			new ThresholdByTextLength(0, 40, 0.99),
			new ThresholdByTextLength(41, Integer.MAX_VALUE, 9));

	protected static final List<ThresholdByTextLength> thresholdsWithoutHintHighPrecision = List.of(
			new ThresholdByTextLength(0, 15, 0.98),
			new ThresholdByTextLength(16, 40, 0.9),
			new ThresholdByTextLength(41, Integer.MAX_VALUE, 7));

	protected static final List<ThresholdByTextLength> thresholdsWithoutHintMediumPrecision = List.of(
			new ThresholdByTextLength(0, 10, 0.9),
			new ThresholdByTextLength(11, 40, 0.7),
			new ThresholdByTextLength(41, Integer.MAX_VALUE, 0.5));

	protected static final List<ThresholdByTextLength> thresholdsWithoutHintLowPrecision = List.of(
			new ThresholdByTextLength(0, 10, 0.7),
			new ThresholdByTextLength(11, Integer.MAX_VALUE, 0.5));

	protected static final List<ThresholdByTextLength> thresholdsWithoutHintVeryLowPrecision = List.of(
			new ThresholdByTextLength(0, 10, 0.5),
			new ThresholdByTextLength(11, Integer.MAX_VALUE, 0));

}
