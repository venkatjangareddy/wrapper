/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oceaneering.validations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.oceaneering.elasticUtil.AuthenticationUtils;
import com.oceaneering.exceptions.ForbiddenException;
import com.oceaneering.exceptions.InputException;
import com.oceaneering.exceptions.TokenAuthenticationException;
import com.oceaneering.properties.WrapperProperties;
import java.io.IOException;
import java.util.Map;
import spark.Request;

/**
 *
 * @author SKashyap
 *
 */
public class Validator {

    private boolean isValid = true;
    private String requestBody = null;
    public boolean isJSONValid;
    public WrapperProperties properties;
    private JsonNode userRoles;

    public boolean validateRequest(Request request, String type) throws IOException, InputException, ForbiddenException, TokenAuthenticationException {
        String node = request.body();
        this.validateCommonRequestChecks(node);
        switch (type) {
            case "search":
                this.validateSearchRequest(node);
                break;
            case "update":
                this.validateUpdateRequest(node);
                break;
        }
        return this.isValid;
    }

    public boolean validateSearchRequest(String node) throws InputException, IOException, TokenAuthenticationException {
        return this.isValid;
    }

    private boolean validateCommonRequestChecks(String node) throws InputException, TokenAuthenticationException {
        try {
            JsonParser parser = new JsonParser();
            this.requestBody = parser.parse(node).toString();
        } catch (JsonSyntaxException jse) {
            this.isValid = false;
            throw new InputException(109);
        } catch (Exception e) {
            System.out.println(101);
        }

        try {
            String body = node;
            if (isValid) {
                String tokenAndUserInfo = null;
                ObjectMapper mapper = new ObjectMapper();
                ObjectNode arrayBody;
                try {
                    arrayBody = (ObjectNode) mapper.readValue(body, JsonNode.class);
                } catch (IOException ex) {
                    isValid = false;
                    throw new InputException(101);
                }
                String app_name = null;
                if (arrayBody.has("app_name")) {
                    app_name = arrayBody.get("app_name").asText();
                    isValid = true;
                } else {
                    isValid = false;
                    throw new InputException(102);
                }
                WrapperProperties properties = WrapperProperties.Instance(app_name);
                if (!properties.isValidNode()) {
                    isValid = false;
                    throw new InputException(104);
                }
                if (!arrayBody.has("access_token")) {
                    isValid = false;
                    throw new InputException(105);
                }
                tokenAndUserInfo = arrayBody.get("access_token").asText();
                Map<String, String> authenticated = AuthenticationUtils.validateToken(tokenAndUserInfo, properties.getSignedKey());
                if (authenticated.get("validated").equalsIgnoreCase("false")) {
                    isValid = false;
                    throw new TokenAuthenticationException(106);
                }
                String userRoles_string = properties.getRoles().toString();
                ObjectMapper rolemap = new ObjectMapper();
                this.userRoles = rolemap.readTree(userRoles_string); // invalid user roles test
            }
        } catch (IOException ex) {
            isValid = false;
            throw new InputException(112);
        }
        return isValid;
    }

    private boolean validateUpdateRequest(String node) throws InputException, TokenAuthenticationException {
        try {
            String body = node;
            if (isValid) {
                String tokenAndUserInfo = null;
                ObjectMapper mapper = new ObjectMapper();
                ObjectNode arrayBody;
                try {
                    arrayBody = (ObjectNode) mapper.readValue(body, JsonNode.class);
                } catch (IOException ex) {
                    isValid = false;
                    throw new InputException(101);
                }
                String app_name = null;
                if (arrayBody.has("app_name")) {
                    app_name = arrayBody.get("app_name").asText();
                    isValid = true;
                } else {
                    isValid = false;
                    throw new InputException(102);
                }
                WrapperProperties properties = WrapperProperties.Instance(app_name);
                if (!properties.isValidNode()) {
                    isValid = false;
                    throw new InputException(104);
                }
                if (!arrayBody.has("access_token")) {
                    isValid = false;
                    throw new InputException(105);
                }
                tokenAndUserInfo = arrayBody.get("access_token").asText();
                Map<String, String> authenticated = AuthenticationUtils.validateToken(tokenAndUserInfo, properties.getSignedKey());
                if (authenticated.get("validated").equalsIgnoreCase("false")) {
                    isValid = false;
                    throw new TokenAuthenticationException(106);
                }
                String userRoles = properties.getRoles().toString();
                ObjectMapper rolemap = new ObjectMapper();
                this.userRoles = rolemap.readTree(userRoles); // invalid user roles test
            }
        } catch (IOException ex) {
            isValid = false;
            throw new InputException(112);
        }
        return this.isValid;
    }

    public String getRequestBody() {
        return this.requestBody;
    }

    public boolean validateSearchSource(WrapperProperties properties) throws IOException, ForbiddenException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode requestBody = (ObjectNode) mapper.readValue(this.requestBody, JsonNode.class);
        String accessSource = getSource();
        if (!(accessSource.equals("[]")) && !accessSource.contains("\"" + requestBody.get("agg_field").asText() + "\"")) {
            this.isValid = false;
            throw new ForbiddenException(requestBody.get("agg_field").asText(), 111);
        }
        for (int i = 0; i < requestBody.get("agg_field").size(); i++) {
            if (!(accessSource.equals("[]")) && !accessSource.contains("\"" + requestBody.get("agg_field").get(i).asText() + "\"")) {
                this.isValid = false;
                throw new ForbiddenException(requestBody.get("agg_field").get(i).asText(), 111);
            }
        }
        return this.isValid;
    }

    private String getSource() throws IOException {
        Integer lowest_level = 99999; // a big number taken for a lowest priority
        String source = null;
        for (int i = 0; i < this.userRoles.size(); i++) {
            String current_role = this.userRoles.get(i).asText();
            int level = properties.getRoles().get(current_role).get("level").asInt();
            if (level < lowest_level) {
                lowest_level = level;
                source = properties.getRoles().get(current_role).get("source").toString();
            }
        }
        return source;
    }    
}
