package eu.europeana.api.translation.web;

public interface I18nErrorMessageKeys {

  //error keys for the exceptions messages
  static final String ERROR_MANDATORY_PARAM_EMPTY = "error.mandatory_param_empty";
  static final String ERROR_INVALID_PARAM_VALUE = "error.invalid_param_value";
  static final String ERROR_UNSUPPORTED_LANG = "error.unsupported_language";
  static final String ERROR_GOOGLE_QUOTA_LIMIT = "error.google_quota_limit_reached";
  static final String ERROR_TRANSLATION_SERVICE_CALL = "error.translation.external_service_call";
  static final String ERROR_LANG_DETECT_SERVICE_CALL = "error.detection.external_service_call";
}
