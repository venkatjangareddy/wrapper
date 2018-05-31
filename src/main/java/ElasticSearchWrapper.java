/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import spark.Spark;
import spark.servlet.SparkApplication;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.oceaneering.common.Utility;
import com.oceaneering.elasticUtil.AuthenticationUtils;
import com.oceaneering.exceptions.EsException;
import com.oceaneering.properties.NetworkProps;
import com.oceaneering.properties.WrapperProperties;
import com.oceaneering.search.ESQuery;
import com.oceaneering.exceptions.EsQueryException;
import com.oceaneering.exceptions.ForbiddenException;
import com.oceaneering.exceptions.InputException;
import com.oceaneering.exceptions.TokenAuthenticationException;
import com.oceaneering.search.EsRequest;
import com.oceaneering.update.AppInterface;
import com.oceaneering.validations.Validator;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import org.apache.log4j.Logger;
import spark.Request;
import spark.Response;

/**
 *
 * @author SKashyap
 *
 */
public class ElasticSearchWrapper
        implements SparkApplication {

    final static Logger logger = Logger.getLogger(ElasticSearchWrapper.class);

    public static void main(String[] args) throws IOException {
        new ElasticSearchWrapper().init();
    }

    @Override
    public void init() {
        Spark.get((String) "/", (request, response) -> {
            response.redirect("/sparkServlet/hello");
            return null;
        });

        Spark.post((String) "/revoketoken", (request, response) -> {
            Validator validator = new Validator();
             try {
            boolean validRequest = validator.validateRequest(request, "revoketoken");
            return null;
             } catch (ForbiddenException e) {
                response.status(403);
                return this.setErrorResponse(response, e);
            } catch (InputException e) {
                response.status(400);
                return this.setErrorResponse(response, e);
            } catch (TokenAuthenticationException e) {
                response.status(401);
                return this.setErrorResponse(response, e);
            } catch (Exception e) {
                logger.error("Sorry, something wrong!", e);
                response.status(500);
                return this.setGeneralErrorResponse(response, e);
            }
        });

        Spark.post((String) "/search", (request, response) -> {
            try {
                NetworkProps net = NetworkProps.Instance();
                Validator validator = new Validator();
                boolean validRequest = validator.validateRequest(request, "search");
                ObjectMapper mapper = new ObjectMapper();
                String arrayBody = validator.getRequestBody();
                JsonNode requestNode = (ObjectNode) mapper.readValue(arrayBody, JsonNode.class);
                String app_name = requestNode.get("app_name").asText();
                WrapperProperties properties = WrapperProperties.Instance(app_name);
                Map<String, String> map = new HashMap();
                map.put("pretty", "true");
                map.put("filter_path", "hits.hits._id,hits.hits._source,aggregations");
                ESQuery esquery = new ESQuery(arrayBody, properties);
                String query = esquery.get();
                Boolean debug = requestNode.has("debug") ? requestNode.get("debug").asBoolean() : false;
                if (debug) {
                    JsonNode qrynode = (JsonNode) mapper.readValue(query, JsonNode.class);
                    return this.setSuccessResponse(response, qrynode);
                }

                JsonNode esresponse = (new EsRequest(query, properties))
                        .setUrlQueryParams(map)
                        .setEndPoint("_search")
                        .post();

                return this.setSuccessResponse(response, esresponse);
            } catch (ForbiddenException e) {
                response.status(403);
                return this.setErrorResponse(response, e);
            } catch (InputException e) {
                response.status(400);
                return this.setErrorResponse(response, e);
            } catch (TokenAuthenticationException e) {
                response.status(401);
                return this.setErrorResponse(response, e);
            } catch (EsQueryException e) {
                response.status(400);
                return this.setErrorResponse(response, e);
            } catch (Exception e) {
                logger.error("Sorry, something wrong!", e);
                response.status(500);
                return this.setGeneralErrorResponse(response, e);
            }
        });

        Spark.post((String) "/update/:id", (request, response) -> {
            try {
                System.out.println("id");
                System.out.println(request.params("id"));
                Validator validator = new Validator();
                boolean validRequest = validator.validateRequest(request, "update");
                JsonNode data = null;
                ObjectMapper mapper = new ObjectMapper();
                String arrayBody = validator.getRequestBody();
                JsonNode requestNode = (ObjectNode) mapper.readValue(arrayBody, JsonNode.class);
                String appName = requestNode.get("app_name").asText();
                WrapperProperties properties = WrapperProperties.Instance(appName);
                String className = properties.getClassName().asText();
                AppInterface clazz = Utility.makeClassNameInstance(className);
                JsonNode updateQuery = clazz
                        .setId(request.params("id"))
                        .setUpdateData(requestNode, properties)
                        .update();
                return this.setSuccessResponse(response, updateQuery);
            } catch (ForbiddenException e) {
                response.status(403);
                return this.setErrorResponse(response, e);
            } catch (InputException e) {
                response.status(400);
                return this.setErrorResponse(response, e);
            } catch (TokenAuthenticationException e) {
                response.status(401);
                return this.setErrorResponse(response, e);
            } catch (Exception e) {
                logger.error("Sorry, something wrong!", e);
                response.status(500);
                return this.setGeneralErrorResponse(response, e);
            }
        }
        );

        Spark.post((String) "/generateAuthenticationToken", (request, response) -> {
            String body = request.body();
            if (body.length() == 0) {
                response.status(400);
                return this.setError("Request body cannot be empty");
            }
            String tokenAndUserInfo = null;
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode arrayBody = (ObjectNode) mapper.readValue(body, JsonNode.class);
            String app_name = null;
            if (arrayBody.has("app_name")) {
                app_name = arrayBody.get("app_name").asText();
            } //add exception if app name is not there
            WrapperProperties properties = WrapperProperties.Instance(app_name);
            properties.setNode(app_name);
            if (!properties.isValidNode()) {
                response.status(400);
                return this.setError("Invalid app_name");
            }
            String username = null;
            if (arrayBody.has("username")) {
                username = arrayBody.get("username").asText();
            } else {
                response.status(400);
                return this.setError("Mandatory username in JSON request");

            }

            List<String> userRoles = new ArrayList<String>();
            Map<String, String> userInfo = AuthenticationUtils.getUserDetails(username);
            if (!properties.isExternalApp()) {
                if (arrayBody.has("roles")) {
                    JsonNode rolesList = arrayBody.get("roles");
                    int size = rolesList.size();
                    for (int i = 0; i < size; i++) {
                        userRoles.add(rolesList.get(i).asText());
                    }
                } else {
                    response.status(400);
                    return this.setError("Mandatory roles in JSON request for " + app_name + ".");
                }
            } else {
                if (userRoles.isEmpty() || userRoles.size() <= 0) {
                    List<String> userRolesThroughMDS = AuthenticationUtils.getUserRole(app_name, username);
                    if (userRolesThroughMDS != null && userRolesThroughMDS.size() > 0) {
                        userInfo.put("userRoles", userRolesThroughMDS.toString());
                    } else {
//                userInfo.put("userRoles", userRoles.toString());
                        userInfo.put("authorize", "false");
                    }
                }
            }
            if (userInfo.get("authorize").equalsIgnoreCase("true")) {
                userInfo.remove("authorize");
                Date date = new Date(System.currentTimeMillis() + properties.getSessionTimeOut() * 1000);
//                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSz");
                final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss Z");
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                String signKey = properties.getSignedKey();
                String jwt = Jwts.builder()
                        .setIssuer(app_name)
                        .setExpiration(date)
                        .claim("username", username)
                        .claim("userInfo", userInfo)
                        .signWith(SignatureAlgorithm.HS256, signKey.getBytes("UTF-8"))
                        .compact();
                userInfo.put("jwtToken", jwt);
                Gson gson = new Gson();
                tokenAndUserInfo = gson.toJson(userInfo);
                WrapperProperties jwtProps = WrapperProperties.Instance("JWT Expiry");
                Map<String, String> tokenToBeSaved = userInfo;
                tokenToBeSaved.put("expiry", sdf.format(date));
                String tokenData = gson.toJson(tokenToBeSaved);
                JsonNode esresponse = (new EsRequest(tokenData, jwtProps))
                        .setEndPoint("JwtNested/" + jwt)
                        .debug()
                        .post();
                return tokenAndUserInfo;
            }
            return "User is not Authorized";
        });

        Spark.options(
                "/*",
                (request, response) -> {

                    String accessControlRequestHeaders = request
                            .headers("Access-Control-Request-Headers");
                    if (accessControlRequestHeaders != null) {
                        response.header("Access-Control-Allow-Headers",
                                accessControlRequestHeaders);
                    }

                    String accessControlRequestMethod = request
                            .headers("Access-Control-Request-Method");
                    if (accessControlRequestMethod != null) {
                        response.header("Access-Control-Allow-Methods",
                                accessControlRequestMethod);
                    }
                    return "OK";
                }
        );

        Spark.before(
                (request, response) -> {
                    response.header("Access-Control-Allow-Origin", "*");
                });
    }

    private String setError(String message) {
        return "Error: {message :" + message + "}";
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
