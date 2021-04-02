package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PmsBaseAttrInfo;
import com.atguigu.gmall.bean.PmsBaseAttrValue;
import com.atguigu.gmall.bean.PmsBaseSaleAttr;

import java.util.List;
import java.util.Set;

public interface AttrService {
    public List<PmsBaseAttrInfo> attrInfoList(String catalog3Id) ;

    public String saveAttrInfo(PmsBaseAttrInfo pmsBaseAttrInfo);

    public List<PmsBaseAttrValue> getAttrValueList(String attrId);

    public List<PmsBaseSaleAttr> baseSaleAttrList();

    List<PmsBaseAttrInfo> getAttrValueListByValueId(Set<String> valueIdSet);

    String getAttrValueNameByValueId(String valueId);
}
