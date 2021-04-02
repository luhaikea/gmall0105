package com.atguigu.gmall.search;


import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsSearchSkuInfo;
import com.atguigu.gmall.bean.PmsSkuAttrValue;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.service.SkuService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.BeanUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cglib.beans.BeanMap;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
class GmallSearchTestApplicationTests {

    @Reference
    SkuService skuService;

    @Autowired
    JestClient jestClient;

    @Test
    public void contextLoads() throws IOException {
        put();
    }

    @Test
    public void get() throws IOException {
        //用api执行复杂查询
        List<PmsSearchSkuInfo> pmsSearchSkuInfoList = new ArrayList<>();
        Search search = new Search.Builder("{\n" +
                "  \"query\": {\n" +
                "    \"bool\": {\n" +
                "      \"filter\": [\n" +
                "        {\"term\": {\"skuAttrValueList.valueId\":\"39\"}},\n" +
                "        {\"term\": {\"skuAttrValueList.valueId\":\"43\"}}\n" +
                "      ],\n" +
                "      \"must\": [\n" +
                "        {\"match\": {\n" +
                "          \"skuName\": \"华为\"\n" +
                "        }}\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}").addIndex("gmall0105").addType("PmsSkuInfo").build();
        SearchResult execute = jestClient.execute(search);
        List<SearchResult.Hit<PmsSearchSkuInfo, Void>> hits = execute.getHits(PmsSearchSkuInfo.class);

        for(SearchResult.Hit<PmsSearchSkuInfo, Void> hit : hits){
            PmsSearchSkuInfo source = hit.source;
            pmsSearchSkuInfoList.add(source);
        }
        System.out.println(pmsSearchSkuInfoList.size());
    }

    @Test
    public void put1() throws IOException {
        //jest的dsl工具
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //bool
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        //filter
        TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId",39);
        boolQueryBuilder.filter(termQueryBuilder);
        TermQueryBuilder termQueryBuilder1 = new TermQueryBuilder("skuAttrValueList.valueId",43);
        boolQueryBuilder.filter(termQueryBuilder1);
        //must
        MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName","华为");
        boolQueryBuilder.must(matchQueryBuilder);

        //query
        searchSourceBuilder.query(boolQueryBuilder);

        //from
        searchSourceBuilder.from(0);
        //size
        searchSourceBuilder.size(20);
        //highlight
        searchSourceBuilder.highlighter();

        String dslStr = searchSourceBuilder.toString();
        System.out.println(dslStr);

        //用api执行复杂查询
        List<PmsSearchSkuInfo> pmsSearchSkuInfoList = new ArrayList<>();
        Search search = new Search.Builder(dslStr).addIndex("gmall0105").addType("PmsSkuInfo").build();
        SearchResult execute = jestClient.execute(search);
        List<SearchResult.Hit<PmsSearchSkuInfo, Void>> hits = execute.getHits(PmsSearchSkuInfo.class);

        for(SearchResult.Hit<PmsSearchSkuInfo, Void> hit : hits){
            PmsSearchSkuInfo source = hit.source;
            pmsSearchSkuInfoList.add(source);
        }
        System.out.println(pmsSearchSkuInfoList.size());

    }

    public void put() throws IOException {

        // 查询mysql数据
        List<PmsSkuInfo> pmsSkuInfoList = new ArrayList<>();
        pmsSkuInfoList = skuService.getAllSku();

        // 转化为es的数据结构
        List<PmsSearchSkuInfo> pmsSearchSkuInfos = new ArrayList<>();

        for (PmsSkuInfo pmsSkuInfo : pmsSkuInfoList) {
            PmsSearchSkuInfo pmsSearchSkuInfo = new PmsSearchSkuInfo();
            BeanUtils.copyProperties(pmsSkuInfo, pmsSearchSkuInfo);
            pmsSearchSkuInfo.setId(Long.parseLong(pmsSkuInfo.getId()));
            pmsSearchSkuInfos.add(pmsSearchSkuInfo);

        }
        // 导入es
        for (PmsSearchSkuInfo pmsSearchSkuInfo : pmsSearchSkuInfos) {
            Index put = new Index.Builder(pmsSearchSkuInfo).index("gmall0105").type("PmsSkuInfo").id(pmsSearchSkuInfo.getId() + "").build();
            jestClient.execute(put);
        }
    }
}

//    @Test
//    public void getAll(){
//        List<PmsSkuInfo> pmsSkuInfoList = new ArrayList<>();
//        pmsSkuInfoList = skuService.getAllSku();
//        for(PmsSkuInfo pmsSkuInfo : pmsSkuInfoList){
//
//            System.out.println("pmsSkuInfo"+pmsSkuInfo+"id     "+pmsSkuInfo.getId()+"产品id："+pmsSkuInfo.getProductId()+"产品id："+pmsSkuInfo.getSpuId());
//            PmsSearchSkuInfo pmsSearchSkuInfo = new PmsSearchSkuInfo();
//            BeanUtils.copyProperties(pmsSkuInfo, pmsSearchSkuInfo);
//            System.out.println("pmsSearchSkuInfo"+pmsSearchSkuInfo+"id"+pmsSearchSkuInfo.getId());
//
//        }
//
//    }

//    @Test
//    public void searchTest() throws IOException {
//
//        SearchRequest searchRequest = new SearchRequest().indices("gmall0105").types("PmsSkuInfo");
//
//        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
//        MatchAllQueryBuilder matchAllQueryBuilder = QueryBuilders.matchAllQuery();
////        MatchPhrasePrefixQueryBuilder mppqb = QueryBuilders.matchPhrasePrefixQuery("skuName","华为");
//        sourceBuilder.query(matchAllQueryBuilder);
//
//        SearchResponse search = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
//        SearchHits hits = search.getHits();
//
//        for (SearchHit hit:hits){
//
//            System.out.println("数据:"+hit.getSourceAsString());
//        }
//    }
//
//    @Test
//    public void contextLoads() throws IOException {
//
//        //查询MySQL数据
//        List<PmsSkuInfo> pmsSkuInfoList = new ArrayList<>();
//        pmsSkuInfoList = skuService.getAllSku();
//        //转化为es的数据结构
//        List<PmsSearchSkuInfo> pmsSearchSkuInfoList = new ArrayList<>();
//
//        for(PmsSkuInfo pmsSkuInfo : pmsSkuInfoList){
//
//            PmsSearchSkuInfo pmsSearchSkuInfo = new PmsSearchSkuInfo();
//
//            pmsSearchSkuInfo.setId(Long.parseLong(pmsSkuInfo.getId()));
//            pmsSearchSkuInfo.setSkuName(pmsSkuInfo.getSkuName());
//            pmsSearchSkuInfo.setSkuDesc(pmsSkuInfo.getSkuDesc());
//            pmsSearchSkuInfo.setCatalog3Id(pmsSkuInfo.getCatalog3Id());
//            pmsSearchSkuInfo.setPrice(pmsSkuInfo.getPrice());
//            pmsSearchSkuInfo.setSkuDefaultImg(pmsSkuInfo.getSkuDefaultImg());
//            pmsSearchSkuInfo.setHotScore(0.0);
//            pmsSearchSkuInfo.setProductId(pmsSkuInfo.getProductId());
//            pmsSearchSkuInfo.setSkuAttrValueList(pmsSkuInfo.getSkuAttrValueList());
//
//            pmsSearchSkuInfoList.add(pmsSearchSkuInfo);
//
//        }
//        //导入es
//        for (PmsSearchSkuInfo pmsSearchSkuInfo : pmsSearchSkuInfoList) {
//
//            System.out.println(pmsSearchSkuInfo+"id:"+pmsSearchSkuInfo.getId());
//            Map<String, Object> map = new HashMap<>();
//            if (pmsSearchSkuInfo != null) {
//                map.put("id", pmsSearchSkuInfo.getId());
//                map.put("skuName", pmsSearchSkuInfo.getSkuName());
//                map.put("skuDesc", pmsSearchSkuInfo.getSkuDesc());
//                map.put("catalog3Id", pmsSearchSkuInfo.getCatalog3Id());
//                map.put("price", pmsSearchSkuInfo.getPrice());
//                map.put("skuDefaultImg", pmsSearchSkuInfo.getSkuDefaultImg());
//                map.put("hotScore", pmsSearchSkuInfo.getHotScore());
//                map.put("productId", pmsSearchSkuInfo.getProductId());
//                List<Map<String, Object>> mapList = new ArrayList<>();
//                List<PmsSkuAttrValue> skuAttrValueList = pmsSearchSkuInfo.getSkuAttrValueList();
//                for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
//                    Map<String, Object> map1 = new HashMap<>();
//                    map1.put("id", pmsSkuAttrValue.getId());
//                    map1.put("attrId",pmsSkuAttrValue.getAttrId());
//                    map1.put("valueId",pmsSkuAttrValue.getValueId());
//                    map1.put("skuId",pmsSkuAttrValue.getSkuId());
//                    mapList.add(map1);
//                }
//                map.put("skuAttrValueList", mapList) ;
//            }
//
//            IndexRequest indexRequest = new IndexRequest("gmall0105", "PmsSkuInfo", pmsSearchSkuInfo.getId()+"").source(map);
//            IndexResponse indexResponse = restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
//
//        }
//    }


/*
public void add() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("id", "20190909");
        map.put("name", "测试");
        map.put("age", 22);
        try {
            IndexRequest indexRequest = new IndexRequest("content", "doc", map.get("id").toString()).source(map);
            IndexResponse indexResponse = restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
            System.out.println(indexResponse.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


 public void add() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("id", "20190909");
        map.put("name", "测试");
        map.put("age", 22);
        Index index = new Index.Builder(map).id("20190909").index("content").type("doc").build();
        try {
            JestResult jestResult = jestClient.execute(index);
            System.out.println(jestResult.getJsonString());
        } catch (Exception e) {

        }
    }



package com.atguigu.gmall.search;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsSearchSkuInfo;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.service.SkuService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import jodd.io.findfile.FindFile;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.elasticsearch.jest.JestAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallSearchServiceApplicationTests {

	@Reference
	SkuService skuService;// 查询mysql


	@Autowired
	JestClient jestClient;

	@Test
	public void contextLoads() throws IOException {
		put();
	}

	public void put() throws IOException {

		// 查询mysql数据
		List<PmsSkuInfo> pmsSkuInfoList = new ArrayList<>();

		pmsSkuInfoList = skuService.getAllSku("61");

		// 转化为es的数据结构
		List<PmsSearchSkuInfo> pmsSearchSkuInfos = new ArrayList<>();

		for (PmsSkuInfo pmsSkuInfo : pmsSkuInfoList) {
			PmsSearchSkuInfo pmsSearchSkuInfo = new PmsSearchSkuInfo();

			BeanUtils.copyProperties(pmsSkuInfo,pmsSearchSkuInfo);

			pmsSearchSkuInfo.setId(Long.parseLong(pmsSkuInfo.getId()));

			pmsSearchSkuInfos.add(pmsSearchSkuInfo);

		}

		// 导入es
		for (PmsSearchSkuInfo pmsSearchSkuInfo : pmsSearchSkuInfos) {
			Index put = new Index.Builder(pmsSearchSkuInfo).index("gmall0105").type("PmsSkuInfo").id(pmsSearchSkuInfo.getId()+"").build();
			jestClient.execute(put);
		}

	}

	public void get() throws IOException {

		// jest的dsl工具
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		// bool
		BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
		// filter
		TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId","43");
		boolQueryBuilder.filter(termQueryBuilder);
		// must
		MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName","华为");
		boolQueryBuilder.must(matchQueryBuilder);
		// query
		searchSourceBuilder.query(boolQueryBuilder);
		// from
		searchSourceBuilder.from(0);
		// size
		searchSourceBuilder.size(20);
		// highlight
		searchSourceBuilder.highlight(null);

		String dslStr = searchSourceBuilder.toString();

		System.err.println(dslStr);


		// 用api执行复杂查询
		List<PmsSearchSkuInfo> pmsSearchSkuInfos = new ArrayList<>();

		Search search = new Search.Builder(dslStr).addIndex("gmall0105").addType("PmsSkuInfo").build();

		SearchResult execute = jestClient.execute(search);

		List<SearchResult.Hit<PmsSearchSkuInfo, Void>> hits = execute.getHits(PmsSearchSkuInfo.class);

		for (SearchResult.Hit<PmsSearchSkuInfo, Void> hit : hits) {
			PmsSearchSkuInfo source = hit.source;

			pmsSearchSkuInfos.add(source);
		}

		System.out.println(pmsSearchSkuInfos.size());
	}



}

*/
