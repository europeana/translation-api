package eu.europeana.api.translation.service.tika;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.tika.language.detect.LanguageResult;

import eu.europeana.api.translation.service.util.ThresholdByTextLength;

/**
 * Language detection service extending the Apache Tika language detection service with
 * support for language hint and text-length-based confidence thresholds
 * 
 * @author Nuno Freire
 * @since 29/01/2025
 */
public class ApacheTikaLangDetectWithThresholdsService extends ApacheTikaLangDetectService {
	List<ThresholdByTextLength> confidenceThresholdsWithHint;
	List<ThresholdByTextLength> confidenceThresholdsWithoutHint;

	public ApacheTikaLangDetectWithThresholdsService(List<ThresholdByTextLength> confidenceThresholdsWithHint,
			List<ThresholdByTextLength> confidenceThresholdsWithoutHint) {
		super();
		this.confidenceThresholdsWithHint = confidenceThresholdsWithHint;
		this.confidenceThresholdsWithoutHint = confidenceThresholdsWithoutHint;
	}

	/**
	 * Accepts/rejects the highest confidence detected language based on the length
	 * of text and the confidence given by Tika
	 */
	protected String chooseDetectedLang(String sourceText, List<LanguageResult> tikaLanguages, String langHint) {
		if (tikaLanguages.isEmpty()) {
			return null;
		}

		List<ThresholdByTextLength> confidenceThresholds = StringUtils.isBlank(langHint)
				? confidenceThresholdsWithoutHint
				: confidenceThresholdsWithHint;

		String detectedLang = tikaLanguages.get(0).getLanguage();
		float confidence = tikaLanguages.get(0).getRawScore();
		for (ThresholdByTextLength threshold : confidenceThresholds) {
			Boolean acceptDetection = threshold.acceptDetection(sourceText, confidence);
			if (acceptDetection != null) {
				if (acceptDetection)
					return detectedLang;
				else
					return StringUtils.isBlank(langHint) ? null : langHint;
			}
		}
		return null;
	}
}
