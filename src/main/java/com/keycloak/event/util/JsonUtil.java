package com.keycloak.event.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for JSON operations. Provides methods for serializing objects to JSON and creating
 * event wrappers.
 */
@Slf4j
public class JsonUtil {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /** Event type for user events */
  public static final String USER_EVENT_TYPE = "USER_EVENT";

  /** Event type for admin events */
  public static final String ADMIN_EVENT_TYPE = "ADMIN_EVENT";

  /** Private constructor to prevent instantiation */
  private JsonUtil() {
    // Utility class, no instantiation
  }

  /**
   * Creates a JSON wrapper for a Keycloak event. The wrapper includes an eventType field to
   * distinguish between user and admin events.
   *
   * @param eventType The type of event (USER_EVENT or ADMIN_EVENT)
   * @param event The event object to wrap
   * @return JSON string representation of the wrapped event
   * @throws JsonProcessingException If the event cannot be serialized to JSON
   */
  public static String createEventWrapper(String eventType, Object event)
      throws JsonProcessingException {
    ObjectNode wrapper = OBJECT_MAPPER.createObjectNode();
    wrapper.put("eventType", eventType);
    wrapper.set("event", OBJECT_MAPPER.valueToTree(event));

    return OBJECT_MAPPER.writeValueAsString(wrapper);
  }

  /**
   * Converts an object to a JSON string.
   *
   * @param object The object to convert
   * @return JSON string representation of the object
   * @throws JsonProcessingException If the object cannot be serialized to JSON
   */
  public static String toJson(Object object) throws JsonProcessingException {
    return OBJECT_MAPPER.writeValueAsString(object);
  }
}
