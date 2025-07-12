package com.keycloak.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.keycloak.event.config.WebhookConfig;
import com.keycloak.event.exception.WebhookMultiException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for the HttpClientWebHookHandler class. */
@ExtendWith(MockitoExtension.class)
public class HttpClientWebHookHandlerTest {

  @Mock private HttpClient httpClient;

  @Mock private HttpResponse<String> httpResponse;

  private HttpClientWebHookHandler webHookHandler;

  private final String testPayload =
      "{\"eventType\":\"USER_EVENT\",\"event\":{\"type\":\"LOGIN\"}}";

  @BeforeEach
  public void setUp() {
    // Clear any environment variables that might interfere with tests
    System.clearProperty(WebhookConfig.WEBHOOK_URLS);
    System.clearProperty(WebhookConfig.HOST_IP);
  }

  @AfterEach
  public void tearDown() {
    // Clean up after tests
    System.clearProperty(WebhookConfig.WEBHOOK_URLS);
    System.clearProperty(WebhookConfig.HOST_IP);
  }

  @Test
  public void testConstructorWithNoUrls() {
    webHookHandler = new HttpClientWebHookHandler(httpClient);
    assertTrue(webHookHandler.getWebhookUrls().isEmpty());
  }

  @Test
  public void testConstructorWithSystemProperty() {
    System.setProperty(
        WebhookConfig.WEBHOOK_URLS, "http://example.com/webhook,http://localhost:8080/events");
    webHookHandler = new HttpClientWebHookHandler(httpClient);

    List<String> expectedUrls =
        Arrays.asList("http://example.com/webhook", "http://localhost:8080/events");
    assertEquals(expectedUrls, webHookHandler.getWebhookUrls());
  }

  @Test
  public void testConstructorWithHostIpReplacement() {
    System.setProperty(
        WebhookConfig.WEBHOOK_URLS, "http://localhost:8080/webhook,http://localhost:9090/events");
    System.setProperty(WebhookConfig.HOST_IP, "192.168.1.100");

    webHookHandler = new HttpClientWebHookHandler(httpClient);

    List<String> expectedUrls =
        Arrays.asList("http://192.168.1.100:8080/webhook", "http://192.168.1.100:9090/events");
    assertEquals(expectedUrls, webHookHandler.getWebhookUrls());
  }

  @Test
  public void testSendEventToAllWebhooksWithNoUrls() throws Exception {
    webHookHandler = new HttpClientWebHookHandler(httpClient);

    // Should not throw an exception
    webHookHandler.sendEventToAllWebhooks(testPayload);

    // Verify no HTTP requests were made
    verify(httpClient, never()).send(any(), any());
  }

  @Test
  public void testSendEventToAllWebhooksSuccess() throws Exception {
    // Set up webhook URLs
    System.setProperty(WebhookConfig.WEBHOOK_URLS, "http://example.com/webhook");
    webHookHandler = new HttpClientWebHookHandler(httpClient);

    // Mock successful HTTP response
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn("OK");
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    // Send event
    webHookHandler.sendEventToAllWebhooks(testPayload);

    // Verify HTTP request was made
    ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
    verify(httpClient).send(requestCaptor.capture(), any());

    HttpRequest capturedRequest = requestCaptor.getValue();
    assertEquals(URI.create("http://example.com/webhook"), capturedRequest.uri());
    assertEquals(
        "application/json", capturedRequest.headers().firstValue("Content-Type").orElse(null));
  }

  @Test
  public void testSendEventToAllWebhooksHttpError() throws Exception {
    // Set up webhook URLs
    System.setProperty(WebhookConfig.WEBHOOK_URLS, "http://example.com/webhook");
    webHookHandler = new HttpClientWebHookHandler(httpClient);

    // Mock HTTP error response
    when(httpResponse.statusCode()).thenReturn(500);
    when(httpResponse.body()).thenReturn("Internal Server Error");
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    // Send event and expect exception
    Exception exception =
        assertThrows(
            Exception.class,
            () -> {
              webHookHandler.sendEventToAllWebhooks(testPayload);
            });

    assertTrue(exception.getMessage().contains("HTTP error status: 500"));
  }

  @Test
  public void testSendEventToAllWebhooksNetworkError() throws Exception {
    // Set up webhook URLs
    System.setProperty(WebhookConfig.WEBHOOK_URLS, "http://example.com/webhook");
    webHookHandler = new HttpClientWebHookHandler(httpClient);

    // Mock network error
    IOException ioException = new IOException("Connection refused");
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenThrow(ioException);

    // Send event and expect exception
    Exception exception =
        assertThrows(
            Exception.class,
            () -> {
              webHookHandler.sendEventToAllWebhooks(testPayload);
            });

    assertEquals(ioException, exception);
  }

  @Test
  public void testSendEventToMultipleWebhooksWithErrors() throws Exception {
    // Set up multiple webhook URLs
    System.setProperty(
        WebhookConfig.WEBHOOK_URLS,
        "http://example.com/webhook1,http://example.com/webhook2,http://example.com/webhook3");
    webHookHandler = new HttpClientWebHookHandler(httpClient);

    // Mock responses for each URL
    HttpResponse<String> successResponse = mock(HttpResponse.class);
    when(successResponse.statusCode()).thenReturn(200);
    when(successResponse.body()).thenReturn("OK");

    HttpResponse<String> errorResponse = mock(HttpResponse.class);
    when(errorResponse.statusCode()).thenReturn(500);
    when(errorResponse.body()).thenReturn("Internal Server Error");

    // First URL succeeds, second fails with HTTP error, third fails with network error
    when(httpClient.send(
            argThat(req -> req != null && req.uri() != null && req.uri().toString().equals("http://example.com/webhook1")), any()))
        .thenAnswer(invocation -> successResponse);
    when(httpClient.send(
            argThat(req -> req != null && req.uri() != null && req.uri().toString().equals("http://example.com/webhook2")), any()))
        .thenAnswer(invocation -> errorResponse);
    when(httpClient.send(
            argThat(req -> req != null && req.uri() != null && req.uri().toString().equals("http://example.com/webhook3")), any()))
        .thenThrow(new IOException("Connection refused"));

    // Send event and expect WebhookMultiException
    Exception exception =
        assertThrows(
            WebhookMultiException.class,
            () -> {
              webHookHandler.sendEventToAllWebhooks(testPayload);
            });

    // Verify it's a WebhookMultiException with 2 suppressed exceptions
    WebhookMultiException multiException = (WebhookMultiException) exception;
    assertEquals(2, multiException.getSuppressed().length);
    assertTrue(
        multiException.getMessage().contains("Multiple webhook failures occurred (2/3 failed)"));
  }
}
