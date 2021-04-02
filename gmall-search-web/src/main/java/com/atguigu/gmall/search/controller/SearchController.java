package com.atguigu.gmall.search.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.AttrService;
import com.atguigu.gmall.service.SearchService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;

@Controller
@CrossOrigin
public class SearchController {

    @Reference
    SearchService searchService;

    @Reference
    AttrService attrService;

    @RequestMapping("index")
    @LoginRequired(loginSuccess = false)
    public String index(){
        return "index";
    }

    @RequestMapping("list.html")
    public String list(PmsSearchParam pmsSearchParam, ModelMap map){

        //get请求有利于分享

        //1、当前请求url中所包含的属性=面包屑中所包含的属性
        //2、属性列表中的属性是排除了当前请求（面包屑请求）中的属性的剩余属性
        //3、当点击面包屑后=面包屑的url是当前请求-被点击面包屑的新请求（也就是点击面包屑上的叉去掉该面包屑属性）
        //4、当点击属性列表后=属性列表url是当前请求+被点击的属性列表的新请求

        List<PmsSearchSkuInfo> pmsSearchSkuInfos = searchService.list(pmsSearchParam);
        map.put("skuLsInfoList", pmsSearchSkuInfos);

        //抽取检索结果所包含的平台属性集合
        Set<String> valueIdSet = new HashSet<>();
        for (PmsSearchSkuInfo pmsSearchSkuInfo : pmsSearchSkuInfos) {
            List<PmsSkuAttrValue> skuAttrValueList = pmsSearchSkuInfo.getSkuAttrValueList();
            for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
                String valueId = pmsSkuAttrValue.getValueId();
                valueIdSet.add(valueId);
            }
        }

        //根据valueId将平台属性集合列表查出来
        List<PmsBaseAttrInfo> pmsBaseAttrInfos = attrService.getAttrValueListByValueId(valueIdSet);
        map.put("attrList", pmsBaseAttrInfos);

        //对平台属性集合进一步处理，去掉当前条件中valueId所在的属性组
        //也就是减去pmsSearchParam中包含的属性
        String[] delValueIds = pmsSearchParam.getValueId();
        if(delValueIds != null){
            Iterator<PmsBaseAttrInfo> iterator = pmsBaseAttrInfos.iterator();
            while(iterator.hasNext()){

                PmsBaseAttrInfo pmsBaseAttrInfo = iterator.next();
                List<PmsBaseAttrValue> attrValueList = pmsBaseAttrInfo.getAttrValueList();
                for (PmsBaseAttrValue pmsBaseAttrValue : attrValueList) {
                    String valueId = pmsBaseAttrValue.getId();
                    for (String delValueId : delValueIds) {
                        if(delValueId.equals(valueId)){
                            iterator.remove();
                        }
                    }
                }

            }
        }

        //当前请求  用于筛选条件
        String urlParam=getUrlParam(pmsSearchParam);
        map.put("urlParam",urlParam);

        //关键字
        String keyword = pmsSearchParam.getKeyword();
        if(StringUtils.isNoneBlank(keyword)){
            map.put("keyword", keyword);
        }

        //面包屑是减属性
        List<PmsSearchCrumb> pmsSearchCrumbs = new ArrayList<>();
        String[] valueIds = pmsSearchParam.getValueId();
        if(valueIds != null){
            //如果valueIds不为空，说明当前请求中包含属性参数，每一个属性参数都应该生产一个面包屑
            for (String valueId : valueIds) {
                PmsSearchCrumb pmsSearchCrumb = new PmsSearchCrumb();
                pmsSearchCrumb.setValueId(valueId);
                String valueName = attrService.getAttrValueNameByValueId(valueId);
                pmsSearchCrumb.setValueName(valueName);
                //面包屑的url就是减去这个面包屑的valueId形成的url
                pmsSearchCrumb.setUrlParam(getUrlParamForCrumb(pmsSearchParam,valueId));
                pmsSearchCrumbs.add(pmsSearchCrumb);
            }
        }
        //已经选择的条件
        map.put("attrValueSelectedList", pmsSearchCrumbs);

        return "list";
    }
    //可变形参
    private String getUrlParamForCrumb(PmsSearchParam pmsSearchParam, String valueIdDel) {

        String urlParam="";

        String keyword = pmsSearchParam.getKeyword();
        String catlog3Id = pmsSearchParam.getCatalog3Id();
        String valueIds[] = pmsSearchParam.getValueId();

        if(StringUtils.isNoneBlank(keyword)){
            if(StringUtils.isNoneBlank(urlParam)){
                urlParam = urlParam + "&";
            }
            urlParam = urlParam+"keyword="+keyword;
        }

        if(StringUtils.isNoneBlank(catlog3Id)){
            if(StringUtils.isNoneBlank(urlParam)){
                urlParam = urlParam + "&";
            }
            urlParam = urlParam+"catlog3Id="+catlog3Id;
        }

        if(valueIds != null){
            for (String valueId : valueIds) {
                if(!valueId.equals(valueIdDel)) {
                    urlParam = urlParam + "&valueId=" + valueId;
                }
            }
        }
        return urlParam;
    }

    private String getUrlParam(PmsSearchParam pmsSearchParam) {

        String urlParam="";

        String keyword = pmsSearchParam.getKeyword();
        String catlog3Id = pmsSearchParam.getCatalog3Id();
        String valueIds[] = pmsSearchParam.getValueId();

        if(StringUtils.isNoneBlank(keyword)){
            if(StringUtils.isNoneBlank(urlParam)){
                urlParam = urlParam + "&";
            }
            urlParam = urlParam+"keyword="+keyword;
        }

        if(StringUtils.isNoneBlank(catlog3Id)){
            if(StringUtils.isNoneBlank(urlParam)){
                urlParam = urlParam + "&";
            }
            urlParam = urlParam+"catlog3Id="+catlog3Id;
        }

        if(valueIds != null){
            for (String valueId : valueIds) {
                urlParam = urlParam+"&valueId="+valueId;

            }
        }
        return urlParam;
    }
}
