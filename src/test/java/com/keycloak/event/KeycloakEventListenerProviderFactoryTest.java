package com.keycloak.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * Tests for the KeycloakEventListenerProviderFactory class.
 */
class KeycloakEventListenerProviderFactoryTest {

    private KeycloakEventListenerProviderFactory factory;
    private KeycloakSession session;
    private Config.Scope config;
    private KeycloakSessionFactory sessionFactory;

    @BeforeEach
    void setUp() {
        factory = new KeycloakEventListenerProviderFactory();
        session = mock(KeycloakSession.class);
        config = mock(Config.Scope.class);
        sessionFactory = mock(KeycloakSessionFactory.class);
    }

    @Test
    void testCreate() {
        // Call the method under test
        EventListenerProvider provider = factory.create(session);
        
        // Verify the provider is created and is of the correct type
        assertNotNull(provider);
        assertTrue(provider instanceof KeycloakEventListenerProvider);
    }

    @Test
    void testInit() {
        // This method doesn't do much besides logging, so just verify it doesn't throw
        assertDoesNotThrow(() -> factory.init(config));
    }

    @Test
    void testPostInit() {
        // This method doesn't do much besides logging, so just verify it doesn't throw
        assertDoesNotThrow(() -> factory.postInit(sessionFactory));
    }

    @Test
    void testClose() {
        // This method doesn't do much besides logging, so just verify it doesn't throw
        assertDoesNotThrow(() -> factory.close());
    }

    @Test
    void testGetId() {
        // Verify the ID is as expected
        assertEquals("custom-event-listener", factory.getId());
    }
}
