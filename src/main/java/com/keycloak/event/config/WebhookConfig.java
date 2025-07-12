package com.keycloak.event.config;

/**
 * Configuration constants for the Keycloak Webhook Bridge. Centralizes all configuration parameters
 * and default values.
 */
public class WebhookConfig {
  /** Environment variable/system property name for webhook URLs */
  public static final String WEBHOOK_URLS = "WEBHOOK_URLS";

  /** Environment variable/system property name for host IP */
  public static final String HOST_IP = "HOST_IP";

  /** Connection timeout in seconds for the HTTP client */
  public static final int CONNECTION_TIMEOUT_SECONDS = 20;

  /** Request timeout in seconds for webhook HTTP requests */
  public static final int REQUEST_TIMEOUT_SECONDS = 10;

  /** Content type for webhook payloads */
  public static final String CONTENT_TYPE = "application/json";

  /** Private constructor to prevent instantiation */
  private WebhookConfig() {
    // Utility class, no instantiation
  }
}
