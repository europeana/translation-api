package eu.europeana.api.translation.service.util;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import eu.europeana.api.translation.definitions.language.Language;
import eu.europeana.api.translation.definitions.language.LanguagePair;

/**
 * Definition of a confidence threshold applicable to a text length range
 * 
 * @author Nuno Freire
 * @since 29/01/2025
 */
public class ThresholdByTextLength {

	/**
	 * The minimum text length where the threshold is applicable (inclusive)
	 */
	private int minimumLength;

	/**
	 * The maximum text length where the threshold is applicable (inclusive)
	 */
	private int maximumLength;
	
	/**
	 * The minimum confidence required for accepting a detection (inclusive)
	 */	
	private double minimumConfidence;

	public ThresholdByTextLength(int minimumLength, int maximumLength, double minimumConfidence) {
		super();
		this.minimumLength = minimumLength;
		this.maximumLength = maximumLength;
		this.minimumConfidence = minimumConfidence;
	}
	
	/**
	 * Checks if a detected language is above the threshold
	 * 
	 * @param sourceText the text where a language was detected
	 * @param confidence the confidence on the detected language
	 * @return true, when the detection is accepted, false otherwise. If this threshold is not applicable to the source text, returns null.  
	 */
	public Boolean acceptDetection(String sourceText, double confidence) {
		if(sourceText.length() >= minimumLength && sourceText.length() <= maximumLength)
			return confidence>=minimumConfidence;
		return null;
	}
	
}
