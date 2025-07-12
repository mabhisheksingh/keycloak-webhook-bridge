package com.keycloak.event.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Tests for the WebhookConfig class. */
public class WebhookConfigTest {

  @Test
  public void testConfigConstants() {
    // Verify environment variable names
    assertEquals("WEBHOOK_URLS", WebhookConfig.WEBHOOK_URLS);
    assertEquals("HOST_IP", WebhookConfig.HOST_IP);

    // Verify timeout values
    assertEquals(20, WebhookConfig.CONNECTION_TIMEOUT_SECONDS);
    assertEquals(10, WebhookConfig.REQUEST_TIMEOUT_SECONDS);

    // Verify content type
    assertEquals("application/json", WebhookConfig.CONTENT_TYPE);
  }
}
