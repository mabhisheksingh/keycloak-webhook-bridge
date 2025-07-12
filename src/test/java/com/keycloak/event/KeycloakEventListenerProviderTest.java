package com.keycloak.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;
import org.mockito.ArgumentCaptor;

class KeycloakEventListenerProviderTest {
  private HttpClientWebHookHandler webHookHandler;
  private KeycloakEventListenerProvider provider;
  private KeycloakSession session;
  private ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setup() {
    webHookHandler = mock(HttpClientWebHookHandler.class);
    session = mock(KeycloakSession.class);

    // Create a provider with mocked webhook handler using dependency injection
    provider = new KeycloakEventListenerProvider(session, webHookHandler);
  }

  @Test
  void testOnEventCallsWebhookHandler() throws Exception {
    // Create a real Event with some data
    Event event = createTestEvent();

    // Call the method under test
    provider.onEvent(event);

    // Capture the JSON payload sent to the webhook handler
    ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
    verify(webHookHandler).sendEventToAllWebhooks(payloadCaptor.capture());

    // Verify the JSON structure
    String payload = payloadCaptor.getValue();
    JsonNode root = objectMapper.readTree(payload);

    // Check that it contains the eventType field
    assertTrue(root.has("eventType"));
    assertEquals("USER_EVENT", root.get("eventType").asText());

    // Check that it contains the event data
    assertTrue(root.has("event"));
    JsonNode eventNode = root.get("event");
    assertEquals("LOGIN", eventNode.get("type").asText());
    assertEquals("test-user-id", eventNode.get("userId").asText());
    assertEquals("test-realm", eventNode.get("realmId").asText());
  }

  @Test
  void testOnAdminEventCallsWebhookHandler() throws Exception {
    // Create a real AdminEvent with some data
    AdminEvent adminEvent = createTestAdminEvent();

    // Call the method under test
    provider.onEvent(adminEvent, true);

    // Capture the JSON payload sent to the webhook handler
    ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
    verify(webHookHandler).sendEventToAllWebhooks(payloadCaptor.capture());

    // Verify the JSON structure
    String payload = payloadCaptor.getValue();
    JsonNode root = objectMapper.readTree(payload);

    // Check that it contains the eventType field
    assertTrue(root.has("eventType"));
    assertEquals("ADMIN_EVENT", root.get("eventType").asText());

    // Check that it contains the event data
    assertTrue(root.has("event"));
    JsonNode eventNode = root.get("event");
    assertEquals("CREATE", eventNode.get("operationType").asText());
    assertEquals("/admin/users/123", eventNode.get("resourcePath").asText());
    assertEquals("test-realm", eventNode.get("realmId").asText());
  }

  @Test
  void testOnEventWithNullEvent() throws Exception {
    // Call with null event
    provider.onEvent((Event) null);

    // Verify webhook handler was not called
    verify(webHookHandler, never()).sendEventToAllWebhooks(anyString());
  }

  @Test
  void testOnAdminEventWithNullEvent() throws Exception {
    // Call with null admin event
    provider.onEvent((AdminEvent) null, true);

    // Verify webhook handler was not called
    verify(webHookHandler, never()).sendEventToAllWebhooks(anyString());
  }

  @Test
  void testOnEventWithWebhookException() throws Exception {
    // Setup webhook handler to throw exception
    doThrow(new RuntimeException("Test webhook error"))
        .when(webHookHandler)
        .sendEventToAllWebhooks(anyString());

    // Create a real Event with some data
    Event event = createTestEvent();

    // Call should not throw exception
    provider.onEvent(event);

    // Verify webhook handler was called
    verify(webHookHandler).sendEventToAllWebhooks(anyString());
  }

  @Test
  void testOnAdminEventWithWebhookException() throws Exception {
    // Setup webhook handler to throw exception
    doThrow(new RuntimeException("Test webhook error"))
        .when(webHookHandler)
        .sendEventToAllWebhooks(anyString());

    // Create a real AdminEvent with some data
    AdminEvent adminEvent = createTestAdminEvent();

    // Call should not throw exception
    provider.onEvent(adminEvent, true);

    // Verify webhook handler was called
    verify(webHookHandler).sendEventToAllWebhooks(anyString());
  }

  @Test
  void testCloseMethod() {
    // Just for coverage
    provider.close();
  }

  private Event createTestEvent() {
    Event event = new Event();
    event.setType(EventType.LOGIN);
    event.setUserId("test-user-id");
    event.setRealmId("test-realm");
    event.setClientId("test-client");
    event.setSessionId("test-session");
    event.setIpAddress("127.0.0.1");

    Map<String, String> details = new HashMap<>();
    details.put("username", "testuser");
    details.put("remember_me", "true");
    event.setDetails(details);

    return event;
  }

  private AdminEvent createTestAdminEvent() {
    AdminEvent adminEvent = new AdminEvent();
    adminEvent.setOperationType(OperationType.CREATE);
    adminEvent.setRealmId("test-realm");
    adminEvent.setResourcePath("/admin/users/123");
    adminEvent.setRepresentation("{\"username\":\"newuser\",\"enabled\":true}");

    return adminEvent;
  }
}
