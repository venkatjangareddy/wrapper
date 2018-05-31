/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oceaneering.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceaneering.properties.NetworkProps;
import com.oceaneering.properties.WrapperProperties;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;

/**
 *
 * @author SKashyap
 *
 */
public class EsRequest {

    private String query;
    private String id;
    private WrapperProperties properties;
    private String method = "GET";
    private String endpoint = "";
    private Map<String, String> headers;
    private Map<String, String> urlParams;
    private Map<String, String> params;
    private boolean debug = false;

    public EsRequest(String query, WrapperProperties properties) throws IOException {
        this.query = query;
        this.properties = properties;
    }

    public JsonNode post() throws IOException {
        this.method = "POST";
        return this.send();
    }

    public JsonNode get() throws IOException {
        this.method = "GET";
        return this.send();
    }

    public JsonNode put() throws IOException {
        this.method = "PUT";
        return this.send();
    }

    public JsonNode head() throws IOException {
        this.method = "HEAD";
        return this.send();
    }
    
    public JsonNode delete() throws IOException {
        this.method = "DELETE";
        return this.send();
    }

    public EsRequest setHeader(Map urlParams) {
        this.urlParams = urlParams;
        return this;
    }

    public EsRequest debug() {
        this.debug = true;
        return this;
    }

    public EsRequest setEndPoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public EsRequest setUrlQueryParams(Map params) {
        this.params = params;
        return this;
    }

    public EsRequest setId(String id) {
        this.id = id;
        return this;
    }

    public JsonNode send() throws IOException {
        NStringEntity entity = new NStringEntity(this.query, ContentType.APPLICATION_JSON);
        String index = properties.getIndex().asText();
        ObjectMapper mapper = new ObjectMapper();
        String error = "";
        NetworkProps net = NetworkProps.Instance();
        RestClient restClient = RestClient.builder((HttpHost[]) new HttpHost[]{new HttpHost(net.host, net.port)}).build();
        JsonNode array;
        try {
            if (this.params == null) {
                Map<String, String> str = new HashMap();
                this.params = str;
            }
            Response responseFromEs = restClient.performRequest(this.method, "/" + index + "/" + this.endpoint, this.params, (HttpEntity) entity, new Header[0]);
            return mapper.readValue(responseFromEs.getEntity().getContent(), JsonNode.class);
        } catch (ResponseException ex) {
            if (ex.getLocalizedMessage().contains("Fielddata is disabled on text fields by default")) {
                error = "{ \"Error\": \"Fielddata is disabled on text fields by default\"}";
                return mapper.readValue(error, JsonNode.class);
            }
            if (ex.getLocalizedMessage().contains("404 Not Found")) {
                error = "{ \"Error\": \"404 - Not Found\"}";
                return mapper.readValue(error, JsonNode.class);
            }
            if (this.debug == true) {
                System.out.println(this.query);
                System.out.println(ex.getLocalizedMessage());
                return mapper.readValue(ex.getLocalizedMessage(), JsonNode.class);
            }
        } catch (Exception ex) {
            error = "{ \"Error\": \"Some error occured at elastic search server\"}";
            return mapper.readValue(error, JsonNode.class);
        }
        return mapper.readValue(error, JsonNode.class);
    }
}
