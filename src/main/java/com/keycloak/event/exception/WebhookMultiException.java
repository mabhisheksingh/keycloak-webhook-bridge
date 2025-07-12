package com.keycloak.event.exception;

/**
 * Exception thrown when multiple webhook calls fail. Contains all individual exceptions as
 * suppressed exceptions.
 */
public class WebhookMultiException extends Exception {

  /**
   * Creates a new WebhookMultiException with the specified message.
   *
   * @param message The error message
   */
  public WebhookMultiException(String message) {
    super(message);
  }

  /**
   * Creates a new WebhookMultiException with the specified message and cause.
   *
   * @param message The error message
   * @param cause The cause of this exception
   */
  public WebhookMultiException(String message, Throwable cause) {
    super(message, cause);
  }
}
