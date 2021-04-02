package com.atguigu.gmall.search.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.PmsSearchParam;
import com.atguigu.gmall.bean.PmsSearchSkuInfo;
import com.atguigu.gmall.service.SearchService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    JestClient jestClient;


    //缓存是减轻数据库的压力，同步请求是减轻服务器的压力

    @Override
    public List<PmsSearchSkuInfo> list(PmsSearchParam pmsSearchParam)  {

        String[] valueId = pmsSearchParam.getValueId();
        String keyword = pmsSearchParam.getKeyword();
        String catalog3Id = pmsSearchParam.getCatalog3Id();

        //jest的dsl工具
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //bool
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        //filter
        if(valueId != null){
            for (String s : valueId) {
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId",s);
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }
        //must
        if(StringUtils.isNoneBlank(keyword)){
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName",keyword);
            boolQueryBuilder.must(matchQueryBuilder);
        }

        if(StringUtils.isNoneBlank(catalog3Id)){
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("catalog3Id",catalog3Id);
            boolQueryBuilder.must(matchQueryBuilder);
        }

        //query
        searchSourceBuilder.query(boolQueryBuilder);

        //from
        searchSourceBuilder.from(0);
        //size
        searchSourceBuilder.size(20);
        //highlight
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<span style='color:red;'>");
        highlightBuilder.field("skuName");
        highlightBuilder.postTags("</span>");
        searchSourceBuilder.highlighter(highlightBuilder);
        //sort 倒序排序
        searchSourceBuilder.sort("id",SortOrder.DESC);

        //aggs聚合函数
        //groupby_attr是一个别名 就是查找每个skuAttrValueList.valueId值出现的次数
        TermsAggregationBuilder groupby_attr = AggregationBuilders.terms("groupby_attr").field("skuAttrValueList.valueId");
        searchSourceBuilder.aggregation(groupby_attr);

        String dslStr = searchSourceBuilder.toString();

        //用api执行复杂查询
        List<PmsSearchSkuInfo> pmsSearchSkuInfoList = new ArrayList<>();
        Search search = new Search.Builder(dslStr).addIndex("gmall0105").addType("PmsSkuInfo").build();
        SearchResult execute = null;
        try {
            execute = jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<SearchResult.Hit<PmsSearchSkuInfo, Void>> hits = execute.getHits(PmsSearchSkuInfo.class);

        for(SearchResult.Hit<PmsSearchSkuInfo, Void> hit : hits){
            PmsSearchSkuInfo source = hit.source;

            Map<String, List<String>> highlight = hit.highlight;
            if(highlight!=null){
                String skuName = highlight.get("skuName").get(0);
                source.setSkuName(skuName);
            }

            pmsSearchSkuInfoList.add(source);
        }

        return pmsSearchSkuInfoList;
    }
}
