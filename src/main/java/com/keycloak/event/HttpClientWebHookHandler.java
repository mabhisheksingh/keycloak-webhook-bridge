package com.keycloak.event;

import com.keycloak.event.config.WebhookConfig;
import com.keycloak.event.exception.WebhookMultiException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles the forwarding of Keycloak events to configured webhook endpoints. Uses Java's HttpClient
 * to send POST requests with JSON payloads. Supports multiple webhook URLs and host IP replacement
 * for container environments.
 */
@Slf4j
public class HttpClientWebHookHandler {

  @Getter private final List<String> webhookUrls;
  private final HttpClient httpClient;

  /**
   * Default constructor that creates a new HttpClient with default settings. Reads webhook URLs
   * from environment variables or system properties.
   */
  public HttpClientWebHookHandler() {
    this(
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(WebhookConfig.CONNECTION_TIMEOUT_SECONDS))
            .build());
  }

  /**
   * Constructor for testing that allows injecting a mock HttpClient. Reads webhook URLs from
   * environment variables or system properties.
   *
   * @param httpClient The HTTP client to use for webhook requests
   */
  public HttpClientWebHookHandler(HttpClient httpClient) {
    String urls = System.getProperty(WebhookConfig.WEBHOOK_URLS);
    if (urls == null) {
      urls = System.getenv(WebhookConfig.WEBHOOK_URLS);
    }

    // Get host IP if specified
    String hostIp = System.getProperty(WebhookConfig.HOST_IP);
    if (hostIp == null) {
      hostIp = System.getenv(WebhookConfig.HOST_IP);
    }

    if (urls != null && !urls.trim().isEmpty()) {
      // If host IP is specified, replace localhost with the host IP
      if (hostIp != null && !hostIp.trim().isEmpty()) {
        final String finalHostIp = hostIp.trim();
        webhookUrls =
            Arrays.stream(urls.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(url -> url.replace("localhost", finalHostIp))
                .collect(Collectors.toList());
        log.info("Replaced localhost with host IP {} in webhook URLs", finalHostIp);
      } else {
        webhookUrls =
            Arrays.stream(urls.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
      }
    } else {
      webhookUrls = new ArrayList<>();
    }

    this.httpClient = httpClient;
    log.info("HttpClientWebHookHandler initialized with webhook URLs: {}", webhookUrls);
  }

  /**
   * Sends an event payload to all configured webhook URLs. If multiple webhook calls fail, a
   * WebhookMultiException is thrown with all exceptions as suppressed.
   *
   * @param payload The JSON payload to send to webhooks
   * @throws Exception If any webhook call fails
   */
  public void sendEventToAllWebhooks(String payload) throws Exception {
    if (webhookUrls.isEmpty()) {
      log.info("No webhook URLs configured, skipping webhook notifications");
      return;
    }

    log.debug("Sending payload to {} webhook(s): {}", webhookUrls.size(), payload);
    List<Exception> exceptions = new ArrayList<>();

    for (String url : webhookUrls) {
      try {
        log.info("Sending webhook to URL: {}", url);

        HttpRequest request =
            HttpRequest.newBuilder()
                .uri(URI.create(url.trim()))
                .header("Content-Type", WebhookConfig.CONTENT_TYPE)
                .timeout(Duration.ofSeconds(WebhookConfig.REQUEST_TIMEOUT_SECONDS))
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        log.debug("Webhook request created: {}", request);

        HttpResponse<String> response =
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        log.info(
            "Webhook response from {}: status={}, body={}",
            url,
            response.statusCode(),
            response.body());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
          log.info("Webhook successfully sent to {}", url);
        } else {
          String errorMsg =
              String.format("HTTP error status: %d for URL: %s", response.statusCode(), url);
          log.error(
              "Webhook error for {}: status code {}, response: {}",
              url,
              response.statusCode(),
              response.body());
          exceptions.add(new Exception(errorMsg));
        }
      } catch (Exception e) {
        log.error("Webhook error for {}: {} ({})", url, e.getMessage(), e.getClass().getName(), e);
        exceptions.add(e);
      }
    }

    // Enhanced error handling - throw a multi-exception if multiple failures occurred
    if (!exceptions.isEmpty()) {
      if (exceptions.size() == 1) {
        throw exceptions.get(0);
      } else {
        WebhookMultiException multiException =
            new WebhookMultiException(
                String.format(
                    "Multiple webhook failures occurred (%d/%d failed)",
                    exceptions.size(), webhookUrls.size()));
        exceptions.forEach(multiException::addSuppressed);
        throw multiException;
      }
    }
  }
}
