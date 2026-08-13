package eu.europeana.api.translation.service.tika;

import eu.europeana.api.translation.definitions.model.LanguageDetectionObj;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ApacheTikaLangDetectServiceTest {

    private ApacheTikaLangDetectService tika;

    @BeforeEach
    void setUp() throws Exception {
        tika = new ApacheTikaLangDetectService();
    }

    @ParameterizedTest(name = "[{index}] text=''{0}'', hint=''{1}'' -> expectedLang=''{2}''")
    @MethodSource("provideLanguageDetectionCases")
    @DisplayName("Detect language with optional hints")
    void testDetectLang(String text, String hint, String expectedLang) throws Exception {
        LanguageDetectionObj detectObj = new LanguageDetectionObj();
        detectObj.setText(text);
        detectObj.setHint(hint);

        List<LanguageDetectionObj> testCases = Collections.singletonList(detectObj);
        tika.detectLang(testCases);

        assertNotNull(detectObj.getDetectedLang(), "Detected language should not be null");
        
        // Assert expected output match
        if (expectedLang != null) {
            assertEquals(expectedLang, detectObj.getDetectedLang());
        }
    }

    private static Stream<Arguments> provideLanguageDetectionCases() {
        return Stream.of(
            // text, hint, expectedLang
            Arguments.of("Irán-Historia-1501-1736 (Dinastía Safávida)", null, "gl"),
            Arguments.of("Irán-Historia-1501-1736 (Dinastía Safávida)", "es", "es"),
            Arguments.of("Irán-Historia-1501-1736 (Dinastía Safávida)", "fr", "gl"),
            Arguments.of("Iglesia Católica-Relaciones-Europa", "null", "ast"),
            Arguments.of("Iglesia Católica-Relaciones-Europa", "es", "es"),
            Arguments.of("Iglesia Católica-Relaciones-Europa", "fr", "ast"),
            Arguments.of("Relazioni di ambasciatori italiani [Manuscrito]", "es", "it")
        );
    }
}