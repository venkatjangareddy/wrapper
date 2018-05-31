/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oceaneering.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceaneering.common.Utility;
import com.oceaneering.exceptions.EsQueryException;
import com.oceaneering.exceptions.InputException;
import com.oceaneering.properties.NetworkProps;
import com.oceaneering.properties.WrapperProperties;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.elasticsearch.index.query.BoolQueryBuilder;
import static org.elasticsearch.index.query.QueryBuilders.*;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

/**
 *
 * @author SKashyap
 *
 */
public class ESQuery {

    private JsonNode queryParams;
    private JsonNode query;
    private Integer size = null;
    private int offset = 0;
    private BoolQueryBuilder mainQuery;
    private List<AggregationBuilder> aggs = new ArrayList();
    private QueryBuilder filter;
    private String source;
    private WrapperProperties properties;
    private NetworkProps netProps;
    private boolean debug = false;

    public ESQuery(String arrayBody, WrapperProperties properties) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        this.queryParams = mapper.readTree(arrayBody);
        this.properties = properties;
        this.setupNetworkConfig();
        this.mainQuery = boolQuery();
    }

    private void setupNetworkConfig() {
        this.netProps = NetworkProps.Instance();
    }

    public String get() throws IOException, EsQueryException, InputException {
        return this.setData().build();
    }

    private ESQuery setData() throws IOException, EsQueryException, InputException {
// because filter should be prepared before the query builds
        String filter = this.queryParams.has("filter") ? this.queryParams.get("filter").toString() : "";
        if (filter != "") {
            this.filter(this.queryParams.get("filter"));
        }
        Iterator<Map.Entry<String, JsonNode>> fields = this.queryParams.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = (Map.Entry<String, JsonNode>) fields.next();
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            switch (key) {
                case "search_keyword":
                    this.searchKeyword(value);
                    break;
                case "page_size":
                    this.pageSize(value);
                    break;
                case "offset":
                    this.offset(value);
                    break;
                case "aggs":
                    this.aggs(value);
                    break;
                case "debug":
                    this.debug = value.asBoolean();
                    break;
            }
        }
        return this;
    }

    private String build() throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices(this.properties.getIndex().asText());

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        if (this.filter != null) {
            this.mainQuery.filter(this.filter);
        }
        searchSourceBuilder.query(this.mainQuery);
        if (this.aggs != null) {
            for (int i = 0; i < this.aggs.size(); i++) {
                searchSourceBuilder.aggregation(this.aggs.get(i));
            }

        }
        SearchResponse response = null;
        searchSourceBuilder.size(this.getSize());
        searchSourceBuilder.from(this.offset);
        return searchSourceBuilder.toString();
    }

    private void aggs(JsonNode data) throws IOException, InputException {
        this.aggs = (new ESAggs(data, this.properties)).get();
    }

    private void filter(JsonNode data) throws EsQueryException {
        this.filter = (new ESFilter(data, this.properties)).get();

    }

    public void searchKeyword(JsonNode data) throws IOException {
        String[] keywords = Utility.split(data.asText());
        BoolQueryBuilder search_query = boolQuery();
        for (int idx = 0; idx < keywords.length; idx++) {
            String keyword = "*" + keywords[idx] + "*";
            QueryStringQueryBuilder qr = queryStringQuery(keyword);
            qr.fields(this.properties.getSearchableFields());
            search_query.should(qr);
        }
        this.mainQuery.must(search_query);
    }

    private void pageSize(JsonNode data) {
        this.size = data.asInt();
    }

    private int getSize() {
        if (this.size != null) {
            return this.size;
        }
        return this.properties.getSourceSize();
    }

    private void offset(JsonNode data) {
        this.offset = data.asInt();
    }

}
