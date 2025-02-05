package eu.europeana.api.translation.service.google;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.cloud.translate.v3.DetectedLanguage;

import eu.europeana.api.translation.definitions.model.LanguageDetectionObj;
import eu.europeana.api.translation.service.exception.LanguageDetectionException;
import eu.europeana.api.translation.service.util.ThresholdByTextLength;

/**
 * Language detection service extending the google language detection service with
 * support for language hint and text-length-based confidence threshold
 * 
 * @author Nuno Freire
 *
 */
public class GoogleLangDetectWithThresholdService extends GoogleLangDetectService {
	List<ThresholdByTextLength> confidenceThresholdsWithHint;
	List<ThresholdByTextLength> confidenceThresholdsWithoutHint;

	public GoogleLangDetectWithThresholdService(String googleProjectId,
			GoogleTranslationServiceClientWrapper clientWrapperBean,
			List<ThresholdByTextLength> confidenceThresholdsWithHint,
			List<ThresholdByTextLength> confidenceThresholdsWithoutHint) {
		super(googleProjectId, clientWrapperBean);
		this.confidenceThresholdsWithHint = confidenceThresholdsWithHint;
		this.confidenceThresholdsWithoutHint = confidenceThresholdsWithoutHint;
	}

	@Override
	public void detectLang(List<LanguageDetectionObj> languageDetectionObjs) throws LanguageDetectionException {
		if (this.googleProjectId.equals(GoogleTranslationServiceClientWrapper.MOCK_CLIENT_PROJ_ID)) {
			String langHint = languageDetectionObjs.get(0).getHint();
			String value = StringUtils.isNotBlank(langHint) ? langHint : "en";
			for (LanguageDetectionObj obj : languageDetectionObjs) {
				obj.setDetectedLang(value);
			}
		} else
			super.detectLang(languageDetectionObjs);
	}

	/**
	 * Accepts/rejects the highest confidence detected language based on the length
	 * of text and the confidence
	 */
	protected String chooseDetectedLang(String sourceText, List<DetectedLanguage> detectedLanguages, String langHint) {
		if (detectedLanguages.isEmpty()) {
			return null;
		}

		List<ThresholdByTextLength> confidenceThresholds = StringUtils.isBlank(langHint)
				? confidenceThresholdsWithoutHint
				: confidenceThresholdsWithHint;

		String detectedLang = detectedLanguages.get(0).getLanguageCode();
		float confidence = detectedLanguages.get(0).getConfidence();
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
