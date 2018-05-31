/*
 * To change this license header, choose License Headers in Project Properties.
 * and open the template in the editor.
 */
package com.oceaneering.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.oceaneering.exceptions.EsException;
import com.oceaneering.exceptions.ForbiddenException;
import com.oceaneering.exceptions.InputException;
import com.oceaneering.exceptions.TokenAuthenticationException;
import com.oceaneering.properties.WrapperProperties;
import com.oceaneering.search.EsRequest;
import com.oceaneering.update.AppInterface;
import com.oceaneering.update.DemandPlanningTool;
import com.oceaneering.update.EmployeeSearch;
import com.oceaneering.validations.Validator;
import static org.elasticsearch.common.xcontent.XContentFactory.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.json.*;
import java.util.regex.Pattern;
import spark.Response;

/**
 *
 * @author SKashyap
 *
 */
public class Utility {

    public static ObjectMapper mapper = null;

    public static String[] split(String text) {
        String[] arr = text.split("-|_|,|\\s");
        return arr;
    }

    public static Map<String, Float> convertStringToFloatMap(String str) {
        return (HashMap<String, Float>) Arrays.asList(str.split(","))
                .stream().map(s -> s.split(":"))
                .collect(Collectors.toMap(e -> e[0], e -> Float.parseFloat(e[1])));
    }

    public static Map<String, Integer> convertStringToIntMap(String str) {
        return (HashMap<String, Integer>) Arrays.asList(str.split(",")).stream().map(s -> s.split(":")).collect(Collectors.toMap(e -> e[0], e -> Integer.parseInt(e[1])));
    }

    public static Map<String, String> convertStringToMap(String str) {
        return (HashMap<String, String>) Arrays.asList(str.split(",")).stream().map(s -> s.split(":")).collect(Collectors.toMap(e -> e[0], e -> e[1]));
    }

    public static AppInterface makeClassNameInstance(String name) {
        try {
            String appName = "com.oceaneering.update." + capitalize(name);
            Class clazz = Class.forName(appName);
            return (AppInterface) clazz.newInstance();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String capitalize(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        char chars[] = name.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
    }

    public static boolean isUrl(String s) {
        String regex = "\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
        try {
            Pattern patt = Pattern.compile(regex);
            Matcher matcher = patt.matcher(s);
            return matcher.matches();
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static void main(String... args) throws IOException {
        //getOldEntity();
        String arrayBody = " { \n" +
"\"app_name\": \"PeopleFinder\", \n" +
"\"access_token\":\"eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJQZW9wbGVGaW5kZXIiLCJleHAiOjE1MjY5ODQ1MDAsInVzZXJuYW1lIjoiZGtob3NsYSIsInVzZXJJbmZvIjp7ImVtcElkIjoiMDIwMTI4MjQiLCJ1c2VybmFtZSI6ImRraG9zbGEifX0.n_6gyIvGqw-4Yi3O0JjM3vno9YLw73FcRt-iTCjdPbM\",\n" +
"\"document\": {\"empbusinessline\":\"This is  dummy businessline for test\",\"empaboutme\":[ { \"aboutme\": \"open source new\",\"aboutmehasorder\": \"3\",\"id\": \"1\"}]}\n" +
"\n" +
"}";

                ObjectMapper mapper = new ObjectMapper();
                JsonNode requestNode = (ObjectNode) mapper.readValue(arrayBody, JsonNode.class);
                String appName = requestNode.get("app_name").asText();
                WrapperProperties properties = WrapperProperties.Instance(appName);
                String className = properties.getClassName().asText();
                AppInterface clazz = Utility.makeClassNameInstance(className);
                JsonNode updateQuery = clazz
                        .setId("29891")
                        .setUpdateData(requestNode, properties)
                        .update();
                System.out.println(updateQuery);
       
    }
    
    
    private JsonNode setErrorResponse(Response response, EsException e) throws IOException {
        this.setJsonContentType(response);
        String jsonString = "{\"success\":\"false\",\"errorMessage\":" + e.getLocalizedMessage() + ",\"errorCode\":" + e.getCode() + "}";
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(jsonString);
    }

    private JsonNode setGeneralErrorResponse(Response response, Exception e) throws IOException {
        this.setJsonContentType(response);
        String jsonString = "{\"success\":\"false\",\"errorMessage\":" + e.getLocalizedMessage() + ",\"errorCode\":500}";
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(jsonString);
    }

    private JsonNode setSuccessResponse(Response response, JsonNode data) throws IOException {
        this.setJsonContentType(response);
        String jsonString = "{\"success\":\"true\",\"data\":" + data.toString() + "}";
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(jsonString);
    }

    private void setJsonContentType(Response response) {
        response.header("ContentType", "Application/Json");
    }

    public static void setCurrentIndex(String name, WrapperProperties properties) {
        if (name.isEmpty() == false && name.length() > 0) {
            properties.currentIndex = name;
        } else {
        }
    }

}
