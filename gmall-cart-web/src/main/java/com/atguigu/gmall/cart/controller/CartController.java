package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.util.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
public class CartController {

    @Reference
    SkuService skuService;
    @Reference
    CartService cartService;

    @RequestMapping("checkCart")
    @LoginRequired(loginSuccess = false)
    public String checkCart(String isChecked, String skuId, ModelMap modelMap, HttpServletRequest request, HttpServletResponse response, HttpSession session){

        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");

        //调用服务，修改状态
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        omsCartItem.setProductSkuId(skuId);
        omsCartItem.setIsChecked(isChecked);
        cartService.checkCart(omsCartItem);

        //将最新的数据从缓存中查出
        List<OmsCartItem> omsCartItems = cartService.cartList(memberId);
        modelMap.put("cartList",omsCartItems);

        //购物车总价格
        BigDecimal totalAmount = getTotalAmount(omsCartItems);
        modelMap.put("totalAmount",totalAmount);

        return "cartListInner";
        //修改选中状态      这里是ajax异步+内嵌也的使用
    }




    @RequestMapping("cartList")
    @LoginRequired(loginSuccess = false)
    public String cartList(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap){

        List<OmsCartItem> omsCartItems = new ArrayList<>();

        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");

        if(StringUtils.isNotBlank(memberId)){
            //已经登录查db
            omsCartItems = cartService.cartList(memberId);
        } else {
            //没有登录查cookie
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            if(StringUtils.isNotBlank(cartListCookie)){
                omsCartItems = JSON.parseArray(cartListCookie, OmsCartItem.class);
            }
        }

        for (OmsCartItem omsCartItem : omsCartItems) {
            omsCartItem.setTotalPrice(omsCartItem.getPrice().multiply(omsCartItem.getQuantity()));
        }

        modelMap.put("cartList", omsCartItems);

        BigDecimal totalAmount = getTotalAmount(omsCartItems);
        modelMap.put("totalAmount",totalAmount);
        return "cartList";
    }

    private BigDecimal getTotalAmount(List<OmsCartItem> omsCartItems) {

        BigDecimal totalAmount = new BigDecimal("0");
        for (OmsCartItem omsCartItem : omsCartItems) {
            if(omsCartItem.getIsChecked().equals("1")) {
                BigDecimal totalPrice = omsCartItem.getTotalPrice();
                totalAmount = totalPrice.add(totalPrice);
            }
        }
        return totalAmount;

    }


    @RequestMapping("addToCart")
    @LoginRequired(loginSuccess = false)
    public String addToCart(String skuId, int quantity, HttpServletRequest request, HttpServletResponse response){

        //调用商品服务查询商品信息
        PmsSkuInfo pmsSkuInfo = skuService.getSkuById(skuId);

        //将商品信息封装成购物车信息
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setCreateDate(new Date());
        omsCartItem.setDeleteStatus(0);
        omsCartItem.setModifyDate(new Date());
        omsCartItem.setPrice(pmsSkuInfo.getPrice());
        omsCartItem.setProductAttr("");
        omsCartItem.setProductBrand("");
        omsCartItem.setProductCategoryId(pmsSkuInfo.getCatalog3Id());
        omsCartItem.setProductId(pmsSkuInfo.getProductId());
        omsCartItem.setProductName(pmsSkuInfo.getSkuName());
        omsCartItem.setProductPic(pmsSkuInfo.getSkuDefaultImg());
        omsCartItem.setProductSkuCode("111111111111");
        omsCartItem.setProductSkuId(skuId);
        omsCartItem.setQuantity(new BigDecimal(quantity));
        omsCartItem.setIsChecked("1");
//        omsCartItem.setSp1();
//        omsCartItem.setSp2();
//        omsCartItem.setSp3();

        //判断用户是否登录
        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");

        if(StringUtils.isBlank(memberId)){
            //用户没有登录
            //item.gamll.com下的cookie与cart.gamll.com下的cookie是不共享的，默认cookie是从自己域名下的那个文件夹下取数据
            //所以需要跨域  解决方案：给cookie设置setDomain getDomain
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            List<OmsCartItem> omsCartItems = new ArrayList<>();
            if(StringUtils.isBlank(cartListCookie)){
                omsCartItems.add(omsCartItem);
            }else {
                omsCartItems = JSON.parseArray(cartListCookie, OmsCartItem.class);
                boolean exist = if_cart_exist(omsCartItems, omsCartItem);
                if(exist){
                    //之前添加过，需要更新购物车数量
                    for (OmsCartItem cartItem : omsCartItems) {
                        if(cartItem.getProductSkuId().equals(omsCartItem.getProductSkuId())){
                            cartItem.setQuantity(cartItem.getQuantity().add(omsCartItem.getQuantity()));
                        }
                    }
                } else{
                    //之前没有添加，只需要添加
                    omsCartItems.add(omsCartItem);
                }
            }

            CookieUtil.setCookie(request,response,"cartListCookie", JSON.toJSONString(omsCartItems), 60*60*72, true);
        } else{
            //用户已经登录
            //从db中查出购物车数据
            List<OmsCartItem> omsCartItems = new ArrayList<>();
            OmsCartItem omsCartItemFromDb = cartService.ifCartExistByUser(memberId, skuId);

            if(omsCartItemFromDb == null){
                //该用户没有添加过当前商品
                omsCartItem.setMemberId(memberId);
                omsCartItem.setMemberNickname("test小明");
                omsCartItem.setQuantity(new BigDecimal(quantity));
                cartService.addCart(omsCartItem);
            } else{
                //该用户添加过当前商品
                omsCartItemFromDb.setQuantity(omsCartItem.getQuantity().add(omsCartItemFromDb.getQuantity()));
                cartService.updateCart(omsCartItemFromDb);
            }

            //同步缓存
            cartService.flushCartCache(memberId);

        }

        return "redirect:/success.html";
    }

    private boolean if_cart_exist(List<OmsCartItem> omsCartItems, OmsCartItem omsCartItem) {
        boolean ret = false;
        for (OmsCartItem cartItem : omsCartItems) {

            String productSkuId = cartItem.getProductSkuId();
            if(productSkuId.equals(omsCartItem.getProductSkuId()))
                ret = true;
        }
        return ret;
    }

}






/*
oms_cart_item表的sp1,sp2,sp3是三个销售属性是为了方便用户确定自己选择的是否正确

1、购物车在不登录的情况下，也可以使用，但需要引入对浏览器cookie的使用
2、购物车在登录的情况下，需要使用mysql和redis来存储数据  redis作为购物车的缓存
3、在缓存的情况下，或者用户已经添加购物车后，允许购物车中的数据和原始商品数据的不一致性
4、购物车同步问题  （1）是么时候同步（结算、登录）【就是cookie购物车和mysql购物车的同步】
                （2）同步购物车后，是否需要删除cookie数据
5、用户在不同的客户端同时登录，如何处理购物车数据
       就是统一用户在不同客户端加入了不同的商品，在一个客户端结算时是否应该同步两个客户端的商品
*/