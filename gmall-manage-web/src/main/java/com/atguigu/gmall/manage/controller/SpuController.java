package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsProductImage;
import com.atguigu.gmall.bean.PmsProductInfo;
import com.atguigu.gmall.bean.PmsProductSaleAttr;
import com.atguigu.gmall.manage.util.PmsUploadUtil;
import com.atguigu.gmall.service.SpuService;
import org.csource.common.MyException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller
@CrossOrigin
public class SpuController {

    @Reference
    SpuService spuService;

    @RequestMapping("spuImageList")
    @ResponseBody
    public List<PmsProductImage> spuImageList(String spuId){

        List<PmsProductImage> pmsProductImages = spuService.spuImageList(spuId);
        return pmsProductImages;
    }

    @RequestMapping("spuSaleAttrList")
    @ResponseBody
    public List<PmsProductSaleAttr> spuSaleAttrList(String spuId){

        List<PmsProductSaleAttr> pmsProductSaleAttrs = spuService.spuSaleAttrList(spuId);
        return pmsProductSaleAttrs;
    }

    @RequestMapping("spuList")
    @ResponseBody
    public List<PmsProductInfo> spuList(String catalog3Id){

        List<PmsProductInfo> spus = spuService.spuList(catalog3Id);
        return spus;
    }

    @RequestMapping("saveSpuInfo")
    @ResponseBody
    public String saveSpuInfo(@RequestBody PmsProductInfo pmsProductInfo){

        spuService.saveSpuInfo(pmsProductInfo);

        return "success";
    }


    @RequestMapping("fileUpload")
    @ResponseBody
    public String fileUpload(@RequestParam("file") MultipartFile multipartFile) throws IOException, MyException {
        //将图片或者音视频上传到分布式文件系统

        String imgUrl="http://114.55.140.81/";
        if (multipartFile != null && !multipartFile.isEmpty()) {
            try {

                String filename = multipartFile.getOriginalFilename();
                String extName = filename.substring(filename.lastIndexOf(".") + 1);
                // 保存文件
                PmsUploadUtil pmsUploadUtil = new PmsUploadUtil();
                imgUrl += pmsUploadUtil.uploadFile(multipartFile.getBytes(), extName);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println(imgUrl);
        //将图片的存储路径返回给页面
        return imgUrl;
    }
}
