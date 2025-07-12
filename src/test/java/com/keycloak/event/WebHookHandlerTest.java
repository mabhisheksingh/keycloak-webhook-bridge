package com.keycloak;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.keycloak.event.HttpClientWebHookHandler;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WebHookHandlerTest {

  @BeforeEach
  void setUp() {
    // Clear any system properties before each test
    System.clearProperty("WEBHOOK_URLS");
    System.clearProperty("HOST_IP");
  }

  @AfterEach
  void tearDown() {
    // Clean up after each test
    System.clearProperty("WEBHOOK_URLS");
    System.clearProperty("HOST_IP");
  }

  @Test
  void testWebhookUrlParsingFromEnv() {
    System.setProperty("WEBHOOK_URLS", "http://localhost:9999,https://example.com");
    HttpClientWebHookHandler handler = new HttpClientWebHookHandler();
    List<String> urls = handler.getWebhookUrls();
    assertEquals(2, urls.size());
    assertEquals("http://localhost:9999", urls.get(0));
    assertEquals("https://example.com", urls.get(1));
  }

  @Test
  void testWebhookUrlParsingEmptyEnv() {
    System.clearProperty("WEBHOOK_URLS");
    HttpClientWebHookHandler handler = new HttpClientWebHookHandler();
    assertNotNull(handler.getWebhookUrls());
    assertTrue(handler.getWebhookUrls().isEmpty());
  }

  @Test
  void testWebhookUrlParsingWithSpaces() {
    System.setProperty("WEBHOOK_URLS", " http://localhost:9999 , https://example.com ");
    HttpClientWebHookHandler handler = new HttpClientWebHookHandler();
    List<String> urls = handler.getWebhookUrls();
    assertEquals(2, urls.size());
    assertEquals("http://localhost:9999", urls.get(0));
    assertEquals("https://example.com", urls.get(1));
  }

  @Test
  void testWebhookUrlParsingWithEmptyEntries() {
    System.setProperty("WEBHOOK_URLS", "http://localhost:9999,,https://example.com,");
    HttpClientWebHookHandler handler = new HttpClientWebHookHandler();
    List<String> urls = handler.getWebhookUrls();
    assertEquals(2, urls.size());
    assertEquals("http://localhost:9999", urls.get(0));
    assertEquals("https://example.com", urls.get(1));
  }

  @Test
  void testHostIpReplacement() {
    System.setProperty("WEBHOOK_URLS", "http://localhost:9999/webhook");
    System.setProperty("HOST_IP", "192.168.1.15");
    HttpClientWebHookHandler handler = new HttpClientWebHookHandler();
    List<String> urls = handler.getWebhookUrls();
    assertEquals(1, urls.size());
    assertEquals("http://192.168.1.15:9999/webhook", urls.get(0));
  }

  @Test
  void testHostIpReplacementMultipleUrls() {
    System.setProperty(
        "WEBHOOK_URLS", "http://localhost:9999/webhook,https://localhost:8443/events");
    System.setProperty("HOST_IP", "192.168.1.15");
    HttpClientWebHookHandler handler = new HttpClientWebHookHandler();
    List<String> urls = handler.getWebhookUrls();
    assertEquals(2, urls.size());
    assertEquals("http://192.168.1.15:9999/webhook", urls.get(0));
    assertEquals("https://192.168.1.15:8443/events", urls.get(1));
  }

  @Test
  void testHostIpNotSetNoReplacement() {
    System.setProperty("WEBHOOK_URLS", "http://localhost:9999/webhook");
    System.clearProperty("HOST_IP");
    HttpClientWebHookHandler handler = new HttpClientWebHookHandler();
    List<String> urls = handler.getWebhookUrls();
    assertEquals(1, urls.size());
    assertEquals("http://localhost:9999/webhook", urls.get(0));
  }

  @Test
  void testHostIpEmptyNoReplacement() {
    System.setProperty("WEBHOOK_URLS", "http://localhost:9999/webhook");
    System.setProperty("HOST_IP", "");
    HttpClientWebHookHandler handler = new HttpClientWebHookHandler();
    List<String> urls = handler.getWebhookUrls();
    assertEquals(1, urls.size());
    assertEquals("http://localhost:9999/webhook", urls.get(0));
  }

  @Test
  void testNonLocalhostUrlNoReplacement() {
    System.setProperty("WEBHOOK_URLS", "http://example.com:9999/webhook");
    System.setProperty("HOST_IP", "192.168.1.15");
    HttpClientWebHookHandler handler = new HttpClientWebHookHandler();
    List<String> urls = handler.getWebhookUrls();
    assertEquals(1, urls.size());
    assertEquals("http://example.com:9999/webhook", urls.get(0));
  }

  // The following tests use dependency injection to provide a mock HttpClient

  @Test
  void testSendEventToAllWebhooksSuccess() throws Exception {
    // Create a test implementation with mocked HttpClient
    System.setProperty("WEBHOOK_URLS", "http://localhost:9999");

    HttpClient mockClient = mock(HttpClient.class);
    HttpResponse<String> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(200);
    when(mockResponse.body()).thenReturn("success");
    when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    HttpClientWebHookHandler handler = new HttpClientWebHookHandler(mockClient);
    handler.sendEventToAllWebhooks("{\"test\":\"data\"}");

    // Verify the HttpClient was called with appropriate request
    verify(mockClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
  }

  @Test
  void testSendEventToMultipleWebhooksSuccess() throws Exception {
    // Create a test implementation with mocked HttpClient
    System.setProperty("WEBHOOK_URLS", "http://localhost:9999,http://example.com/webhook");

    HttpClient mockClient = mock(HttpClient.class);
    HttpResponse<String> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(200);
    when(mockResponse.body()).thenReturn("success");
    when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    HttpClientWebHookHandler handler = new HttpClientWebHookHandler(mockClient);
    handler.sendEventToAllWebhooks("{\"test\":\"data\"}");

    // Verify the HttpClient was called twice (once for each URL)
    verify(mockClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
  }

  @Test
  void testSendEventToAllWebhooksHttpError() throws Exception {
    // Create a test implementation with mocked HttpClient
    System.setProperty("WEBHOOK_URLS", "http://localhost:9999");

    HttpClient mockClient = mock(HttpClient.class);
    HttpResponse<String> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(500);
    when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    HttpClientWebHookHandler handler = new HttpClientWebHookHandler(mockClient);

    Exception exception =
        assertThrows(
            Exception.class,
            () -> {
              handler.sendEventToAllWebhooks("{\"test\":\"data\"}");
            });

    assertTrue(exception.getMessage().contains("HTTP error status: 500"));
  }

  @Test
  void testSendEventToAllWebhooksGeneralError() throws Exception {
    // Create a test implementation with mocked HttpClient
    System.setProperty("WEBHOOK_URLS", "http://localhost:9999");

    HttpClient mockClient = mock(HttpClient.class);
    when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenThrow(new IOException("Connection refused"));

    HttpClientWebHookHandler handler = new HttpClientWebHookHandler(mockClient);

    Exception exception =
        assertThrows(
            Exception.class,
            () -> {
              handler.sendEventToAllWebhooks("{\"test\":\"data\"}");
            });

    assertTrue(exception.getMessage().contains("Connection refused"));
  }

  @Test
  void testSendEventToMultipleWebhooksPartialFailure() throws Exception {
    // Create a test implementation with mocked HttpClient
    System.setProperty("WEBHOOK_URLS", "http://localhost:9999,http://example.com/webhook");

    HttpClient mockClient = mock(HttpClient.class);

    // First call succeeds
    HttpResponse<String> successResponse = mock(HttpResponse.class);
    when(successResponse.statusCode()).thenReturn(200);

    // Second call fails
    HttpResponse<String> errorResponse = mock(HttpResponse.class);
    when(errorResponse.statusCode()).thenReturn(500);

    // Set up the mock to return different responses for different URIs
    when(mockClient.send(
            argThat(
                request ->
                    request != null && "http://localhost:9999".equals(request.uri().toString())),
            any(HttpResponse.BodyHandler.class)))
        .thenReturn(successResponse);

    when(mockClient.send(
            argThat(
                request ->
                    request != null
                        && "http://example.com/webhook".equals(request.uri().toString())),
            any(HttpResponse.BodyHandler.class)))
        .thenReturn(errorResponse);

    HttpClientWebHookHandler handler = new HttpClientWebHookHandler(mockClient);

    Exception exception =
        assertThrows(
            Exception.class,
            () -> {
              handler.sendEventToAllWebhooks("{\"test\":\"data\"}");
            });

    assertTrue(exception.getMessage().contains("HTTP error status: 500"));
    verify(mockClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
  }

  @Test
  void testNoWebhooksNoError() throws Exception {
    System.clearProperty("WEBHOOK_URLS");
    HttpClientWebHookHandler handler = new HttpClientWebHookHandler();
    // This should not throw an exception
    handler.sendEventToAllWebhooks("{\"test\":\"data\"}");
  }
}
