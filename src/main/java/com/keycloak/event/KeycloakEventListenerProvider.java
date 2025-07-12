package com.keycloak.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.keycloak.event.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;

/**
 * Keycloak SPI implementation that listens for user and admin events and forwards them to
 * configured webhook endpoints.
 *
 * <p>This provider captures all Keycloak events and wraps them with an eventType field to
 * distinguish between user events and admin events before sending them as JSON payloads to the
 * configured webhook URLs.
 */
@Slf4j
public class KeycloakEventListenerProvider implements EventListenerProvider {
  private final HttpClientWebHookHandler webHookHandler;
  private final KeycloakSession keycloakSession;

  /**
   * Creates a new KeycloakEventListenerProvider with the specified session. This constructor is
   * used in production.
   *
   * @param keycloakSession The Keycloak session
   */
  public KeycloakEventListenerProvider(KeycloakSession keycloakSession) {
    log.info("Initializing KeycloakEventListenerProvider with session: {}", keycloakSession);
    this.keycloakSession = keycloakSession;
    this.webHookHandler = new HttpClientWebHookHandler();
  }

  /**
   * Constructor for testing that allows injecting a mock webhook handler. This constructor is used
   * in tests to provide a mock webhook handler.
   *
   * @param keycloakSession The Keycloak session
   * @param webHookHandler The webhook handler to use
   */
  public KeycloakEventListenerProvider(
      KeycloakSession keycloakSession, HttpClientWebHookHandler webHookHandler) {
    log.info(
        "Initializing KeycloakEventListenerProvider with session: {} and custom webhook handler",
        keycloakSession);
    this.keycloakSession = keycloakSession;
    this.webHookHandler = webHookHandler;
  }

  /**
   * Handles user events from Keycloak. Wraps the event with an eventType field and sends it to
   * configured webhooks.
   *
   * @param event The user event
   */
  @Override
  public void onEvent(Event event) {
    if (event == null) {
      log.warn("Received null Keycloak event");
      return;
    }
    log.debug("Received Keycloak event: {}", event);
    log.info(
        "Keycloak Event: {} - User: {} - Realm: {}",
        event.getType(),
        event.getUserId(),
        event.getRealmId());
    log.info("Configured Webhook URLs: {}", webHookHandler.getWebhookUrls());
    try {
      String payload = JsonUtil.createEventWrapper(JsonUtil.USER_EVENT_TYPE, event);
      webHookHandler.sendEventToAllWebhooks(payload);
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize event to JSON: {}", e.getMessage(), e);
    } catch (Exception e) {
      log.error("Failed to send event to webhook(s): {}", e.getMessage(), e);
    }
  }

  /**
   * Handles admin events from Keycloak. Wraps the event with an eventType field and sends it to
   * configured webhooks.
   *
   * @param adminEvent The admin event
   * @param includeRepresentation Whether to include the representation in the event
   */
  @Override
  public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
    if (adminEvent == null) {
      log.warn("Received null Keycloak admin event");
      return;
    }
    log.debug("Received Keycloak admin event: {}", adminEvent);
    log.info(
        "Keycloak Admin Event: {} - Resource: {} - Realm: {}",
        adminEvent.getOperationType(),
        adminEvent.getResourcePath(),
        adminEvent.getRealmId());
    log.info("Configured Webhook URLs: {}", webHookHandler.getWebhookUrls());
    try {
      String payload = JsonUtil.createEventWrapper(JsonUtil.ADMIN_EVENT_TYPE, adminEvent);
      webHookHandler.sendEventToAllWebhooks(payload);
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize admin event to JSON: {}", e.getMessage(), e);
    } catch (Exception e) {
      log.error(
          "Failed to send admin event to webhook(s): {}: {}", e.getClass().getSimpleName(), e);
    }
  }

  /** Closes this provider. This method is called when the provider is no longer needed. */
  @Override
  public void close() {
    // Nothing to close
  }
}
