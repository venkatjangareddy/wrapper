/*
 * To change this license header, choose License Headers in Project Properties.
 * and open the template in the editor.
 */
package com.oceaneering.properties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Map;
import spark.utils.IOUtils;
import com.oceaneering.common.Utility;

/**
 *
 * @author SKashyap
 *
 */
public class WrapperProperties {

    private JsonNode configProperties;
    private boolean isValid = false;

    public String currentIndex = null;

    /**
     * @return the node
     */
    public JsonNode getNode() {
        return this.node;
    }

    /**
     * @param app_name the node to set
     */
    public void setNode(String app_name) {
        if (this.getConfig().has(app_name) != false) {
            this.isValid = true;
            this.node = this.getConfig().get(app_name);
        } else {
            isValid = false;
        }
    }

    /**
     * @return the node
     */
    private JsonNode getConfig() {
        return this.configProperties;
    }

    /**
     * @param node the node to set
     */
    private void setConfig(JsonNode config) {
        if (this.configProperties == null) {
            this.configProperties = config;
        }
    }

    public static WrapperProperties Instance(String app_name) {
        if (_instance == null) {
            try {
                _instance = new WrapperProperties();
            } catch (Exception ex) {
                return null;
            }
        }
        _instance.setNode(app_name);
        return _instance;
    }

    private WrapperProperties() {
        InputStream in = null;
        try {
            if (this.configProperties == null) {
                //System.out.println("Reading JSON File Properties");
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                in = classLoader.getResourceAsStream("application.properties.json");
                StringWriter writer = new StringWriter();
                IOUtils.copy(in, writer);
                String jsonString = writer.toString();
                mapper = new ObjectMapper();
                this.setConfig(mapper.readTree(jsonString));
            }
        } catch (Exception ex) {
            //System.out.println("Exception ex" + ex);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    public JsonNode getIndex() {
        return this.getNode().get("index");
    }

    public boolean isExternalApp() {
        return this.getNode().get("authorizeViaMDS").asBoolean();
    }

    public JsonNode getESType() {
        return this.getNode().get("type");
    }

    public JsonNode getClassName() {
        return this.getNode().get("className");
    }

    public JsonNode getRoles() throws IOException {
        return this.getNode().get("roles");
    }

    public String getSignedKey() {
        return this.getNode().get("signatureKey").asText();
    }

    public int getSessionTimeOut() {
        return this.getNode().has("sessionTimeOut")
                ? this.getNode().get("sessionTimeOut").asInt() : 3600; // default value of 1 hour if no value defined in cofig file
    }

    public int getAggSize() {
        return this.getNode().has("aggSize")
                ? this.getNode().get("aggSize").asInt() : 200;
    }

    public int getAggCount() {
        return this.getNode().has("aggCount")
                ? this.getNode().get("aggCount").asInt() : 100;
    }

    public int getSourceSize() {
        return (this.getNode().has("sourceSize") && this.getNode().get("sourceSize").asInt() > 0)
                ? this.getNode().get("sourceSize").asInt() : 200;
    }

    /**
     * @return the mapper
     */
    public ObjectMapper getMapper() {
        return mapper;
    }

    /**
     * @param mapper the mapper to set
     */
    public void setMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public Map<String, Float> getSearchableFields() {
        String inputString = "*:1.0";
        if (this.getNode().has("searchableFields")) {
            inputString = this.getNode().get("searchableFields").asText();
        }
        return Utility.convertStringToFloatMap(inputString);
    }

    public boolean isValidNode() {
        return this.isValid;
    }
    private ObjectMapper mapper;
    private JsonNode node;
    private static WrapperProperties _instance = null;
}
