/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oceaneering.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.oceaneering.common.Utility;
import com.oceaneering.exceptions.EsQueryException;
import com.oceaneering.properties.WrapperProperties;
import java.util.Iterator;
import java.util.Map;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.ExistsQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import static org.elasticsearch.index.query.QueryBuilders.*;
import org.elasticsearch.index.query.QueryStringQueryBuilder;

/**
 *
 * @author SKashyap
 *
 */
public class ESFilter {

    private JsonNode filterData;
    private String operator = "and";

    private QueryBuilder filterQuery;
    private WrapperProperties properties;
    private BoolQueryBuilder bool = boolQuery();

    ESFilter(JsonNode data, WrapperProperties properties) {
        this.filterData = data;
        this.properties = properties;
    }

    private ESFilter setData() throws EsQueryException {
        String operator = this.filterData.has("operator") ? this.filterData.get("operator").toString() : "";
        if (operator != "") {
            this.setOperator(this.filterData.get("operator"));

        }
        Iterator<Map.Entry<String, JsonNode>> fields = this.filterData.fields();
        boolean addToBuilder;
        try {

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = (Map.Entry<String, JsonNode>) fields.next();
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                QueryBuilder buildquery = null;
                addToBuilder = true;
                switch (key) {
                    case "gt":
                        buildquery = this.rangeGt(value);
                        break;
                    case "gte":
                        buildquery = this.rangeGte(value);
                        break;
                    case "lt":
                        buildquery = this.rangeLt(value);
                        break;
                    case "lte":
                        buildquery = this.rangeLte(value);
                        break;
                    case "not":
                        buildquery = this.mustNot(value);
                        break;
                    case "operator":
                        this.setOperator(value);
                        addToBuilder = false;
                        break;
                    default:
                        buildquery = this.termQry(key, value);
                        break;
                }
                if (addToBuilder == true) {
                    if (this.operator == "and") {
                        bool.must(buildquery);
                    } else if (this.operator == "or") {
                        bool.should(buildquery);
                    }
                }
            }
            this.filterQuery = bool;
        } catch (Exception e) {
            // write code for deciding the error code
            // expected to be a bad request
            throw new EsQueryException(e.getLocalizedMessage());
        }

        return this;
    }

    private void setOperator(JsonNode value) {
        if (value.asText().equalsIgnoreCase("or")) {
            this.operator = "or";
        }
    }

    private BoolQueryBuilder rangeGt(JsonNode data) {
        Iterator<Map.Entry<String, JsonNode>> fields = data.fields();
        BoolQueryBuilder bool = boolQuery();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = (Map.Entry<String, JsonNode>) fields.next();
            String key = entry.getKey();
            Object value = entry.getValue().asText();
            RangeQueryBuilder range = rangeQuery(key);
            range.gt(value);
            bool.must(range);
        }
        return bool;
    }

    private BoolQueryBuilder mustNot(JsonNode data) {
        Iterator<Map.Entry<String, JsonNode>> fields = data.fields();
        BoolQueryBuilder bool = boolQuery();
        QueryBuilder query = null;
        while (fields.hasNext()) {

            switch (data.get("criteria").toString()) {
                case "partial":
                    String[] keywords = Utility.split(data.get("value").asText());
                    BoolQueryBuilder search_query = boolQuery();
                    for (int idx = 0; idx < keywords.length; idx++) {
                        String keyword = "*" + keywords[idx] + "*";
                        QueryStringQueryBuilder qr = queryStringQuery(keyword);
                        qr.field(data.get("field").toString());
                        search_query.should(qr);
                    }
                    query = search_query;
                    break;

                case "exact":
                    query = this.termQry(data.get("field").toString(), data.get("value"));
                    break;
            }

            bool.mustNot(query);
        }
        return bool;
    }

    private BoolQueryBuilder rangeGte(JsonNode data) {
        Iterator<Map.Entry<String, JsonNode>> fields = data.fields();
        BoolQueryBuilder bool = boolQuery();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = (Map.Entry<String, JsonNode>) fields.next();
            String key = entry.getKey();
            Object value = entry.getValue().asText();
            RangeQueryBuilder range = rangeQuery(key);
            range.gte(value);
            bool.must(range);
        }
        return bool;
    }

    private BoolQueryBuilder rangeLt(JsonNode data) {
        Iterator<Map.Entry<String, JsonNode>> fields = data.fields();
        BoolQueryBuilder bool = boolQuery();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = (Map.Entry<String, JsonNode>) fields.next();
            String key = entry.getKey();
            JsonNode value = entry.getValue();

            RangeQueryBuilder range = rangeQuery(key);
            range.lt(value);
            bool.must(range);
        }

        return bool;
    }

    private BoolQueryBuilder rangeLte(JsonNode data) {
        Iterator<Map.Entry<String, JsonNode>> fields = data.fields();
        BoolQueryBuilder bool = boolQuery();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = (Map.Entry<String, JsonNode>) fields.next();
            String key = entry.getKey();
            Object value = entry.getValue().asText();
            RangeQueryBuilder range = rangeQuery(key);
            range.lte(value);
            bool.must(range);
        }
        return bool;
    }

    private QueryBuilder termQry(String key, JsonNode value) {
        if (value.isNull()) {
            BoolQueryBuilder querybuilder = boolQuery();
            ExistsQueryBuilder exists = existsQuery(key);
            return querybuilder.mustNot(exists);
        } else {
            if (value.isArray()) {
                return termsQuery(key, value);
            }
            return termQuery(key, value.asText());
        }
    }

    QueryBuilder get() throws EsQueryException {
        this.setData();
        return this.filterQuery;
    }
}
