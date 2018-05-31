/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oceaneering.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceaneering.exceptions.InputException;
import com.oceaneering.properties.WrapperProperties;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.lucene.util.automaton.RegExp;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import static org.elasticsearch.search.aggregations.AggregationBuilders.terms;
import static org.elasticsearch.search.aggregations.AggregationBuilders.topHits;
import org.elasticsearch.search.aggregations.bucket.terms.IncludeExclude;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHitsAggregationBuilder;
import org.elasticsearch.search.sort.SortOrder;

/**
 *
 * @author SKashyap
 *
 */
public class ESAggs {

    private JsonNode aggs_data;
    private List<AggregationBuilder> aggsquery = new ArrayList();
    private WrapperProperties properties;

    ESAggs(JsonNode data, WrapperProperties properties) {
        this.aggs_data = data;
        this.properties = properties;
    }

    List<AggregationBuilder> get() throws IOException, InputException {
        this.setData();
        return this.aggsquery;
    }

    private ESAggs setData() throws IOException, InputException {
        Iterator<JsonNode> fields = this.aggs_data.iterator();

        while (fields.hasNext()) {
            JsonNode entry = fields.next();
            // iterate on sub nodes
            AggregationBuilder agg = this.createAggQuery(entry);
            this.aggsquery.add(agg);
        }
        return this;
    }

    private AggregationBuilder createAggQuery(JsonNode entry) throws IOException, InputException {
        Iterator<Map.Entry<String, JsonNode>> sub_aggsdata = entry.fields();
        String type = null, name = null;
        JsonNode fields = null;
        boolean tophits = false;
        while (sub_aggsdata.hasNext()) {
            Map.Entry<String, JsonNode> entry1 = (Map.Entry<String, JsonNode>) sub_aggsdata.next();
            String key = entry1.getKey();
            if (key == "agg_type") {
                type = entry1.getValue().asText();
            } else if (key == "agg_name") {
                name = entry1.getValue().asText();
            } else if (key == "agg_field") {
                fields = entry1.getValue();
            } else if (key == "fetchrecords") {
                tophits = entry1.getValue().asBoolean();
            }
        }
        AggregationBuilder aggquerybuilder;
        switch (type) {
            case "count":
                TermsAggregationBuilder aggquery = terms(name);
                IncludeExclude incExc = new IncludeExclude(null, new RegExp(".*null.*|.{0}"));
                Map<String, Object> map = new HashMap();
                ObjectMapper mapper = new ObjectMapper();
                aggquery.size(properties.getAggCount());
                aggquery.includeExclude(incExc);

                if (fields.isArray() == true) {

                    String composite_keys = this.getCompositeKey(fields, "-");
                    Map<String, String> options = new HashMap();
                    options.put("source", composite_keys);
                    Script script = new Script(composite_keys);
                    aggquery.script(script);

                } else {
                    aggquery.field(fields.asText());
                }
                if (tophits == true) {
                    TopHitsAggregationBuilder tophitsquery = topHits(name + "_top_hits");
                    tophitsquery.sort("_score", SortOrder.DESC);
                    tophitsquery.size(this.properties.getAggSize());
                    tophitsquery.fetchSource("*", "");
                    aggquery.subAggregation(tophitsquery);
                }
                aggquerybuilder = aggquery;

                break;
            default:
                throw new InputException(113);
        }
        return aggquerybuilder;

    }

    private String getCompositeKey(JsonNode fields, String delimiter) {
        String composite_keys = "";
        if (delimiter == null) {
            delimiter = "-";
        }
        for (int i = 0; i < fields.size(); i++) {
            composite_keys += "doc['" + fields.get(i).asText() + "'].value";
            composite_keys += ((i != fields.size() - 1) ? "+'-'+" : "");
        }
        return composite_keys;
    }
}
