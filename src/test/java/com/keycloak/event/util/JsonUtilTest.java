package com.keycloak.event.util;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;

/** Tests for the JsonUtil class. */
public class JsonUtilTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void testCreateUserEventWrapper() throws JsonProcessingException {
    // Create a test user event
    Event event = new Event();
    event.setType(EventType.LOGIN);
    event.setUserId("test-user-id");
    event.setRealmId("test-realm");
    event.setClientId("test-client");

    // Create the wrapper JSON
    String json = JsonUtil.createEventWrapper(JsonUtil.USER_EVENT_TYPE, event);

    // Parse and verify the JSON structure
    JsonNode root = objectMapper.readTree(json);
    assertEquals(JsonUtil.USER_EVENT_TYPE, root.get("eventType").asText());

    JsonNode eventNode = root.get("event");
    assertNotNull(eventNode);
    assertEquals("LOGIN", eventNode.get("type").asText());
    assertEquals("test-user-id", eventNode.get("userId").asText());
    assertEquals("test-realm", eventNode.get("realmId").asText());
    assertEquals("test-client", eventNode.get("clientId").asText());
  }

  @Test
  public void testCreateAdminEventWrapper() throws JsonProcessingException {
    // Create a test admin event
    AdminEvent adminEvent = new AdminEvent();
    adminEvent.setOperationType(OperationType.CREATE);
    adminEvent.setRealmId("admin-realm");
    adminEvent.setResourcePath("/users/123");

    // Create the wrapper JSON
    String json = JsonUtil.createEventWrapper(JsonUtil.ADMIN_EVENT_TYPE, adminEvent);

    // Parse and verify the JSON structure
    JsonNode root = objectMapper.readTree(json);
    assertEquals(JsonUtil.ADMIN_EVENT_TYPE, root.get("eventType").asText());

    JsonNode eventNode = root.get("event");
    assertNotNull(eventNode);
    assertEquals("CREATE", eventNode.get("operationType").asText());
    assertEquals("admin-realm", eventNode.get("realmId").asText());
    assertEquals("/users/123", eventNode.get("resourcePath").asText());
  }

  @Test
  public void testToJson() throws JsonProcessingException {
    // Create a simple test object
    TestObject testObject = new TestObject("test-name", 42);

    // Convert to JSON
    String json = JsonUtil.toJson(testObject);

    // Parse and verify the JSON structure
    JsonNode root = objectMapper.readTree(json);
    assertEquals("test-name", root.get("name").asText());
    assertEquals(42, root.get("value").asInt());
  }

  /** Simple test class for JSON serialization */
  private static class TestObject {
    private final String name;
    private final int value;

    public TestObject(String name, int value) {
      this.name = name;
      this.value = value;
    }

    public String getName() {
      return name;
    }

    public int getValue() {
      return value;
    }
  }
}
