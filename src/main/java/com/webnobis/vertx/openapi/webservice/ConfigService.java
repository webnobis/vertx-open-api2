package com.webnobis.vertx.openapi.webservice;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.service.ServiceRequest;
import io.vertx.ext.web.api.service.ServiceResponse;
import io.vertx.ext.web.api.service.WebApiServiceGen;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Objects;

/**
 * Config service, to get or set informations<br>
 * <a href=
 * "https://vertx.io/docs/vertx-web-api-service/java/">vertx-web-api-service</a>
 *
 * @author SteffenNobis
 */
@WebApiServiceGen
public interface ConfigService {

    /**
     * Workaround: Same as 'getConfigForm', because duplicate definitions are not allowed
     *
     * @param context       request context
     * @param resultHandler response result handler
     * @see #getConfigForm(ServiceRequest, Handler)
     */
    default void getConfigFormSame(ServiceRequest context, Handler<AsyncResult<ServiceResponse>> resultHandler) {
        getConfigForm(context, resultHandler);
    }

    /**
     * Gets the config form to could post a changed configuration.
     *
     * @param context       request context
     * @param resultHandler response result handler
     */
    default void getConfigForm(ServiceRequest context, Handler<AsyncResult<ServiceResponse>> resultHandler) {
        Promise<ServiceResponse> promise = Promise.promise();
        promise.future().onComplete(resultHandler);
        String formFile = "config-service/config.html";
        try {
            URI uri = Objects
                    .requireNonNull(ConfigService.class.getClassLoader().getResource(formFile)).toURI();
            byte[] bytes;
            if ("jar".equals(uri.getScheme())) {
                try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap(), null)) {
                    bytes = Files.readAllBytes(fs.getPath(formFile));
                }
            } else {
                bytes = Files.readAllBytes(Paths.get(uri));
            }
            promise.complete(ServiceResponse.completedWithPlainText(Buffer.buffer(bytes))
                    .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "text/html"));
        } catch (IOException | URISyntaxException e) {
            promise.fail(e);
        }
    }

    /**
     * Workaround: Uses body instead of form fields.
     *
     * @param body          form body
     * @param context       request context
     * @param resultHandler response result handler
     * @see #setConfig(String, String, String, ServiceRequest, Handler)
     */
    default void setConfigBody(JsonObject body, ServiceRequest context, Handler<AsyncResult<ServiceResponse>> resultHandler) {
        String id = body.getString(ConfigServiceFormField.ID.getId());
        String content = body.getString(ConfigServiceFormField.CONTENT.getId());
        String charset = body.getString(ConfigServiceFormField.CHARSET.getId());

        // call real method from workaround
        setConfig(id, content, charset, context, resultHandler);
    }

    /**
     * Overwrites the id bound configuration with the plain text content.
     *
     * @param id            the id
     * @param content       the content
     * @param charset       the charset, maybe 'UTF-8'
     * @param context       request context
     * @param resultHandler response result handler
     */
    void setConfig(String id, String content, String charset, ServiceRequest context, Handler<AsyncResult<ServiceResponse>> resultHandler);
}
