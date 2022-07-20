/**
 * Vertx Open Api
 *
 * @author SteffenNobis
 */
module de.db.pp.adapter.commons {
    exports com.webnobis.vertx.openapi.webservice;
    exports com.webnobis.vertx.openapi.webservice.server;

    requires io.vertx.core;
    requires io.vertx.auth.common;
    requires io.vertx.auth.properties;
    requires io.vertx.web;
    requires io.vertx.web.openapi;
    requires io.vertx.web.apiservice;
    requires io.vertx.serviceproxy;
    requires io.vertx.codegen;
    requires io.vertx.web.client;
    requires org.slf4j;
    requires org.apache.httpcomponents.httpcore;

}
