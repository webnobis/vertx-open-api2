package com.webnobis.vertx.openapi.webservice.server;

import com.webnobis.vertx.openapi.webservice.ConfigService;
import io.vertx.core.*;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.properties.PropertyFileAuthentication;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.ext.web.handler.BasicAuthHandler;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.serviceproxy.ServiceBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Config service web server, providing the api definition
 * 'config-service/config-service.yaml'
 *
 * @author SteffenNobis
 */
public class ConfigServiceVerticle extends AbstractVerticle {

    private static final String CONFIG_SERVICE = "config_api.config_service";

    private static final String CONFIG_SERVICE_YAML = "config-service/config-service.yaml";

    private static final Logger log = LoggerFactory.getLogger(ConfigServiceVerticle.class);

    private final ConfigService configService;

    private final String authenticationPropertiesPath;

    final HttpServerOptions options;

    private HttpServer server;

    /**
     * Creates the config service web server
     *
     * @param configService the config service implementation
     * @param options       http server options
     */
    public ConfigServiceVerticle(ConfigService configService, String authenticationPropertiesPath,
                                 HttpServerOptions options) {
        this.configService = Objects.requireNonNull(configService);
        this.authenticationPropertiesPath = Objects.requireNonNull(authenticationPropertiesPath);
        this.options = Objects.requireNonNull(options);
    }

    @Override
    public void init(Vertx vertx, Context context) {
        super.init(vertx, context);
        ServiceBinder serviceBinder = new ServiceBinder(vertx);
        serviceBinder.setAddress(CONFIG_SERVICE).register(ConfigService.class, configService);
        log.debug("config service {} based on {} from class path registered", configService, CONFIG_SERVICE_YAML);
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        String protocol = options.isSsl() ? "https" : "http";
        String url = MessageFormat.format("{0}://{1}:{2}", protocol, options.getHost(),
                String.valueOf(options.getPort()));
        startPromise.future().onSuccess(unused -> log.info("config service {} started: {}", configService, url))
                .onFailure(t -> log.error(MessageFormat.format("config service {0} starting failed at {1}: {2}",
                        configService, url, t.getMessage()), t));

        vertx.executeBlocking(blockingPromise -> {
            Promise<RouterBuilder> routerPromise = Promise.promise();
            routerPromise.future().compose(this::startService).onComplete(blockingPromise);
            RouterBuilder.create(vertx, getApiDefinition(), routerPromise);
        }, startPromise);
    }

    private static String getApiDefinition() {
        try {
            URI uri = Objects.requireNonNull(ClassLoader.getSystemResource(CONFIG_SERVICE_YAML)).toURI();
            if ("jar".equals(uri.getScheme())) {
                try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap(), null)) {
                    return fs.getPath(CONFIG_SERVICE_YAML).toString();
                }
            } else {
                return Paths.get(uri).toString();
            }
        } catch (URISyntaxException | IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private Future<Void> startService(RouterBuilder routerBuilder) {
        AuthenticationProvider authProvider = PropertyFileAuthentication.create(vertx, authenticationPropertiesPath);
        AuthenticationHandler authHandler = BasicAuthHandler.create(authProvider);
        // Add security and mount services, based on extensions within yaml file
        Router router = routerBuilder.securityHandler("basicAuth", authHandler).mountServicesFromExtensions()
                        .rootHandler(ctx -> {
                            if (HttpMethod.POST.equals(ctx.request().method())) {
                                MultiMap map = ctx.request().formAttributes();
                                if(map.isEmpty()) {
                                    log.error("______________Missing form attributes_______________");
                                } else {
                                    log.info("______________Form attributes: " + map.entries().stream().map(e -> String.join("=", e.getKey(), e.getValue())).collect(
                                                    Collectors.joining("|")));
                                }
                            }
                            ctx.next();
                        })
                .createRouter();

        // Start the HTTP Server and bind Router
        Promise<HttpServer> serverPromise = Promise.promise();
        server = vertx.createHttpServer(options).requestHandler(router).listen(serverPromise);
        return serverPromise.future().mapEmpty();
    }

    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {
        if (server != null) {
            server.close(stopPromise);
        } else {
            stopPromise.complete();
        }
    }
}
