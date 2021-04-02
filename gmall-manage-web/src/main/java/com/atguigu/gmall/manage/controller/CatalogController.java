package com.atguigu.gmall.manage.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsBaseCatalog1;
import com.atguigu.gmall.bean.PmsBaseCatalog2;
import com.atguigu.gmall.bean.PmsBaseCatalog3;
import com.atguigu.gmall.service.CatalogService;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@CrossOrigin          //解决跨域
public class CatalogController {

    @Reference
    CatalogService catalogService;


    @RequestMapping("getCatalog1")
    @ResponseBody
    public List<PmsBaseCatalog1> getCatalog1(){

        List<PmsBaseCatalog1> catalog1s = catalogService.getCatalog1();
        return catalog1s;

    }
    //spu(standard product unit):标准化产品单元，是商品信息聚合的最小单元是一组
    //可复用、易检索的标准化信息的集合该集合描述了一个产品的特征
    //SKU(stock keeping unit):库质量单元、即库存进出计量的基本单元，现已被引申为产品统一编号的简称
    //每种产品均对应有唯一的SKU号

    //商品的sku由商品的平台属性（由平台提供）和商品的SPU（由商家提供）组成

    //商品评论是基于spu的

    //图片的对象数据保存在分布式文件系统上
    //图片的元数据保存在数据库中
    //销售属性是由包含于spu由商家管理
    //一个spu加上一组具体销售属性就是一个SKU
    @RequestMapping("getCatalog2")
    @ResponseBody
    public List<PmsBaseCatalog2> getCatalog2(String catalog1Id){
        List<PmsBaseCatalog2> catalog2s = catalogService.getCatalog2(catalog1Id);
        return catalog2s;
    }

    //一个三级分类对应一组平台属性
    //spu包含sku  spu是一个系列 比如iphone12 sku是指spu的一个具体型号，
    // 比如iPhone12金色8g内存256g外存 sku往下就是库存中这个型号的哪一部具体手机
    //
    @RequestMapping("getCatalog3")
    @ResponseBody
    public List<PmsBaseCatalog3> getCatalog3(String catalog2Id){
        List<PmsBaseCatalog3> catalog3s = catalogService.getCatalog3(catalog2Id);
        return catalog3s;
    }
}
