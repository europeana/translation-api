package eu.europeana.api.translation.web;

import eu.europeana.api.commons.JsonSchemaLocation;

public interface TranslationSchemaLocation extends JsonSchemaLocation {

  String JSON_SCHEMA_URI = "https://rnd-2.eanadev.org/share/api/schema/json/translation/ws.schema.json";
  String JSON_SCHEMA_NESTED="#/definitions/LangDetectRequest";

}
