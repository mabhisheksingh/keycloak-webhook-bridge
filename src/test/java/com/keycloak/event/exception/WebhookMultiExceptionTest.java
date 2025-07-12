package com.keycloak.event.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Tests for the WebhookMultiException class. */
public class WebhookMultiExceptionTest {

  @Test
  public void testConstructorWithMessage() {
    String errorMessage = "Multiple webhook failures";
    WebhookMultiException exception = new WebhookMultiException(errorMessage);

    assertEquals(errorMessage, exception.getMessage());
    assertEquals(0, exception.getSuppressed().length);
  }

  @Test
  public void testConstructorWithMessageAndCause() {
    String errorMessage = "Multiple webhook failures";
    Exception cause = new RuntimeException("Root cause");
    WebhookMultiException exception = new WebhookMultiException(errorMessage, cause);

    assertEquals(errorMessage, exception.getMessage());
    assertEquals(cause, exception.getCause());
    assertEquals(0, exception.getSuppressed().length);
  }

  @Test
  public void testAddSuppressedExceptions() {
    String errorMessage = "Multiple webhook failures";
    WebhookMultiException exception = new WebhookMultiException(errorMessage);

    // Add suppressed exceptions
    Exception exception1 = new RuntimeException("Error 1");
    Exception exception2 = new RuntimeException("Error 2");
    Exception exception3 = new RuntimeException("Error 3");

    exception.addSuppressed(exception1);
    exception.addSuppressed(exception2);
    exception.addSuppressed(exception3);

    // Verify suppressed exceptions
    Throwable[] suppressed = exception.getSuppressed();
    assertEquals(3, suppressed.length);
    assertEquals(exception1, suppressed[0]);
    assertEquals(exception2, suppressed[1]);
    assertEquals(exception3, suppressed[2]);
  }
}
