package com.webnobis.vertx.openapi.webservice;

/**
 * Config service form fields
 *
 * @author SteffenNobis
 */
public enum ConfigServiceFormField {

    /**
     * id field
     */
    ID,

    /**
     * content field
     */
    CONTENT,

    /**
     * charset field
     */
    CHARSET;

    /**
     * @return id of the form field
     */
    public String getId() {
        return name().toLowerCase();
    }
}
