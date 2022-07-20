package com.webnobis.vertx.openapi.webservice.server;

import com.webnobis.vertx.openapi.webservice.ConfigService;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.api.service.ServiceRequest;
import io.vertx.ext.web.api.service.ServiceResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
class ConfigServiceVerticleTest {

    private static final int PORT = 60000;

    private static final String USERNAME = ConfigServiceVerticleTest.class.getSimpleName().concat("User");

    private static final String PASSWORD = "ich-bin-ein-$chweres-P4ss{o}d";

    private static final Buffer RESPONSE_BODY = Buffer.buffer("a test body content");

    private static final String ID = "the-id";

    private static final String CONTENT = "the real content";

    private static Path tmpBasicAuthenticationTestProperties;

    private static String content;

    private WebClient client;

    private TestConfigService testConfigService;

    private Verticle configService;

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        tmpBasicAuthenticationTestProperties = Files.createTempFile(ConfigServiceVerticle.class.getSimpleName(),
                ".properties");
        Files.write(tmpBasicAuthenticationTestProperties,
                "user.".concat(USERNAME).concat("=").concat(PASSWORD).getBytes(StandardCharsets.UTF_8));
    }

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext testContext) throws Exception {
        client = WebClient.create(vertx, new WebClientOptions().setDefaultPort(PORT));

        testConfigService = new TestConfigService();
        configService = new ConfigServiceVerticle(testConfigService, tmpBasicAuthenticationTestProperties.toString(),
                new HttpServerOptions().setPort(PORT).setHost("127.0.0.1"));
        vertx.deployVerticle(configService, testContext.succeeding(unused -> testContext.completeNow()));
    }

    @AfterEach
    void tearDown() throws Exception {
        client.close();
    }

    @AfterAll
    static void tearDownAfterClass() throws Exception {
        Files.delete(tmpBasicAuthenticationTestProperties);
    }

    @Test
    void testGetConfigForm(VertxTestContext testContext) {
        client.get("/config/v1/update").basicAuthentication(USERNAME, PASSWORD).send(testContext.succeeding(res -> {
            assertEquals(HttpStatus.SC_OK, res.statusCode());
            assertTrue(res.body().getBytes().length > 0);
            testContext.completeNow();
        }));
    }

    @Test
    void testSetConfig(VertxTestContext testContext) {
        testSetConfig("/config/v1/update", testContext);
    }

    @Test
    void testSetConfigWorkaround(VertxTestContext testContext) {
        testSetConfig("/config/v1/workaround", testContext);
    }

    private void testSetConfig(String path, VertxTestContext testContext) {
        MultiMap form = MultiMap.caseInsensitiveMultiMap().add("id", ID).add("content", CONTENT).add("charset",
                StandardCharsets.UTF_8.name());
        client.post(path).basicAuthentication(USERNAME, PASSWORD).sendForm(form,
                testContext.succeeding(res -> {
                    assertEquals(HttpStatus.SC_OK, res.statusCode());
                    assertEquals(RESPONSE_BODY, res.body());
                    assertEquals(ID, testConfigService.idRef.get());
                    assertEquals(CONTENT, testConfigService.contentRef.get());
                    assertEquals(StandardCharsets.UTF_8.name(), testConfigService.charsetRef.get());
                    testContext.completeNow();
                }));
    }

    @Test
    void testMissingAuthentication(VertxTestContext testContext) {
        client.get("/config/v1/update").send(testContext.succeeding(res -> {
            assertEquals(HttpStatus.SC_UNAUTHORIZED, res.statusCode());
            testContext.completeNow();
        }));
    }

    private class TestConfigService implements ConfigService {

        private final AtomicReference<String> idRef = new AtomicReference<>();

        private final AtomicReference<String> contentRef = new AtomicReference<>();

        private final AtomicReference<String> charsetRef = new AtomicReference<>();

        @Override
        public void setConfig(String id, String content, String charset, ServiceRequest context, Handler<AsyncResult<ServiceResponse>> resultHandler) {
            idRef.set(id);
            contentRef.set(content);
            charsetRef.set(charset);
            Promise<ServiceResponse> promise = Promise.promise();
            promise.future().onComplete(resultHandler);
            promise.complete(ServiceResponse.completedWithPlainText(RESPONSE_BODY));
        }
    }
}
