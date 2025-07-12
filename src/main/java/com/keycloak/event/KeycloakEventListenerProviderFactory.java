package com.keycloak.event;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * Factory for creating KeycloakEventListenerProvider instances.
 *
 * <p>This factory is responsible for creating and initializing the event listener provider that
 * forwards Keycloak events to webhook endpoints. It is registered with Keycloak through the Service
 * Provider Interface (SPI) mechanism.
 */
@Slf4j
public class KeycloakEventListenerProviderFactory implements EventListenerProviderFactory {

  /**
   * Creates a new event listener provider for the given session.
   *
   * @param session The Keycloak session
   * @return A new KeycloakEventListenerProvider instance
   */
  @Override
  public EventListenerProvider create(KeycloakSession session) {
    log.debug("Creating KeycloakEventListenerProvider for session: {}", session);
    return new KeycloakEventListenerProvider(session);
  }

  /**
   * Initializes this factory with the given configuration. Called once when the factory is created.
   *
   * @param config The configuration scope
   */
  @Override
  public void init(org.keycloak.Config.Scope config) {
    log.info("Initializing KeycloakEventListenerProviderFactory with config: {}", config);
  }

  /**
   * Called after all provider factories have been initialized.
   *
   * @param factory The Keycloak session factory
   */
  @Override
  public void postInit(KeycloakSessionFactory factory) {
    log.info(
        "Post-initializing KeycloakEventListenerProviderFactory with session factory: {}", factory);
  }

  /** Closes this factory. Called when the server is shutting down. */
  @Override
  public void close() {
    log.info("Closing KeycloakEventListenerProviderFactory");
  }

  /**
   * Returns the ID of this provider factory. This ID is used to reference this provider in the
   * Keycloak configuration.
   *
   * @return The provider ID
   */
  @Override
  public String getId() {
    return "custom-event-listener";
  }
}
