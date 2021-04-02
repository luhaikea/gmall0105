package com.atguigu.gmall.payment.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PaymentController {

    @Autowired
    AlipayClient alipayClient;

    @Autowired
    PaymentService paymentService;

    @Reference
    OrderService orderService;

    @RequestMapping("alipay/callback/return")
    @LoginRequired(loginSuccess = true)
    //getParameter参数是在请求体中的，get请求时参数在方法参数中得到，post请求时参数在request.getParameter()得到

    //支付成功后回调函数（谷粒商城被支付宝调用）
    public String aliPayCallBackReturn(HttpServletRequest request, ModelMap modelMap){

        // 回调请求中获取支付宝参数   支付成功后回调方法
        String sign = request.getParameter("sign");
        String trade_no = request.getParameter("trade_no");
        String out_trade_no = request.getParameter("out_trade_no");
        String trade_status = request.getParameter("trade_status");
        String total_amount = request.getParameter("total_amount");
        String subject = request.getParameter("subject");
        String call_back_content = request.getQueryString();


        // 通过支付宝的paramsMap进行签名验证，2.0版本的接口将paramsMap参数去掉了，导致同步请求没法验签
        if(StringUtils.isNotBlank(sign)){
            // 验签成功
            // 更新用户的支付状态
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setOrderSn(out_trade_no);
            paymentInfo.setPaymentStatus("已支付");
            paymentInfo.setAlipayTradeNo(trade_no);// 支付宝的交易凭证号
            paymentInfo.setCallbackContent(call_back_content);//回调请求字符串
            paymentInfo.setCallbackTime(new Date());

            paymentService.updatePayment(paymentInfo);

        }

        //支付成功后，将引起一系列系统服务--》订单服务的更新--》库存服务--》物流

        //一个支付的行为引起了多个服务的并发   分布式环境下的并行服务 不同服务对应的后台事务的一致性
        //分布式环境下如何保证事务的一致性    单一环境下自己用spring配置解决
        //    订单------->事务-------->DB
        //    物流------->事务-------->DB            保证在分布式环境下数据的一致性
        //    库存------->事务-------->DB
        //这三个事务启动的时间点是不一样的 但必须保证一个事务失败其他事务一块回滚  就是不求同年同月生，但求同年同月死
        //问题是如何保证这三个事务的一致性  单个事务用spring配置解决
        //解决方案（不唯一）【基于消息的，采取最终一致性策略的分布式事务】
        //      其他解决方案 ：1、xa协议下的 两段式提交
        //                  2、xa两段式提交的进阶版：tcc(try confirm cancle)
        //  也就是订单物流的事务都执行成功但库存执行失败，此时订单物流不回滚，记录库存失败的原因由人工或者系统后期去处理，一个原则就是坚决不退钱
        //  原理：在一个事务正在进行的同时，发出消息给其他业务，如果消息发送失败，或者消息的执行失败，就是接受该消息并且去执行相应业务的系统
        //       出现问题，这时要回滚消息，重复执行，反复执行失败后，记录失败信息，后期补充性的处理
        //  在消息系统中开启事务，消息的事务是指，保证消息被正常消费，否则回滚的一种机制
        //
        //消息队列还有许多其他的用途：
        //      同步数据库和elasticsearch与缓存，就是当更新或写入数据库时需要返送消息给elasticsearch与缓存让其也同步更新其中数据


        //这里存在一个问题，当用户支付成功后，立即关闭了浏览器，支付宝没有来得及回调系统，后面一系列的操作就没有了，不能给用户发货等
        //因此要有一个主动的定时的检查阿里支付接口，去检查该笔交易的状态，这个叫支付结果检查  用的是延迟队列解决

        /*
                               分布式事务的业务模型
            0、提交订单的延迟检查（支付服务）
               PAYMENT_CHECK_QUEUE  由支付服务消费
            1、支付完成（支付服务）
               PAYMENT_SUCCESS_QUEUE：由订单服务消费【PAYMENT_SUCCESS_QUEUE由支付服务产生，由订单服务消费，为了引起支付已完成的业务】
            2、订单已支付（订单服务）
               ORDER_PAY_QUEUE：由库存系统消费
            3、库存锁定（库存系统）
               SKU_DEDUCT_QUEUE【deduct:扣除，减去】：由订单服务消费
            4、订单已出库（订单服务）
               ORDER_SUCCESS_QUEUE：可能由日志系统或者后期的跟进系统消费

            其他两个用到消息队列的地方
               1、购物车合并
               2、商品管理同步队列
        */

        return "finish";
    }



    @RequestMapping("alipay/submit")
    @LoginRequired(loginSuccess = true)
    @ResponseBody
    //在index页面选择支付宝支付后点击立即提交，会进入下面这个方法，这个方法会返回一个二维码支付页面【是支付宝的页面】
    //这个页面就是一个表单
    //这个表单会发起这样一个请求：method="post" action="https://openapi.alipay.com/gateway.do
    //这个请求会转到二维码支付页面【是支付宝的页面】https://excashier.alipay.com
    //在https://excashier.alipay.com页面会实时监测用户是否支付成功，如果支付成功就会回调@RequestMapping("alipay/callback/return")
    //在请求url【https://openapi.alipay.com】中有一个sign参数 是谷类请求支付宝（谷粒的签名）
    //在回调url【@RequestMapping("alipay/callback/return")】中也有一个sign参数 这个是支付宝回调谷粒（支付宝的签名）
    //谷粒的签名是由商家我们自己的私钥生成的只能由支付宝的公钥【这个公钥是谷粒保存在支付宝上的公钥】可以解开的一个签名，
    //      如果这个签名可以由支付宝的公钥解开就说明这是谷粒
    //支付宝的签名是由支付宝生成的，由支付宝保存在谷粒商城上的公钥去解，也就是鼓励商城验签
    // 谷粒的私钥                     谷粒的公钥
    // 支付宝的公钥                   支付宝的私钥
    //     密钥的生成   自己在本地电脑上用RSA签名验签工具生成  会生成商户私钥和商会公钥  商户公钥要上传到支付宝
    //以前的支付宝公钥要去支付宝拷贝，现在支付宝公钥已经封装在sdk中
    public String alipay(String outTradeNo, BigDecimal totalAmount, HttpServletRequest request, ModelMap modelMap){

        // 获得一个支付宝请求的客户端(它并不是一个链接，而是一个封装好的http的表单请求)
        String form = null;
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request
        // 回调函数
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);

        Map<String,Object> map = new HashMap<>();
        map.put("out_trade_no",outTradeNo);
        map.put("product_code","FAST_INSTANT_TRADE_PAY");
        map.put("total_amount",100);
        map.put("subject","尚硅谷感光徕卡Pro300瞎命名系列手机");

        String param = JSON.toJSONString(map);

        alipayRequest.setBizContent(param);

        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
            System.out.println(form);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        // 生成并且保存用户的支付信息
        OmsOrder omsOrder = orderService.getOrderByOutTradeNo(outTradeNo);

        PaymentInfo paymentInfo = new PaymentInfo();

        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOrderId(omsOrder.getId());
        paymentInfo.setOrderSn(outTradeNo);
        paymentInfo.setPaymentStatus("未付款");
        paymentInfo.setSubject("谷粒商城商品一件");
        paymentInfo.setTotalAmount(totalAmount);

        paymentService.savePaymentInfo(paymentInfo);

        // 向消息中间件发送一个检查支付状态(支付服务消费)的延迟消息队列
        //paymentService.sendDelayPaymentResultCheckQueue(outTradeNo,5);

        //这里存在一个问题，当用户支付成功后，立即关闭了浏览器，支付宝没有来得及回调系统，后面一系列的操作就没有了，不能给用户发货等
        //因此要有一个主动的定时的检查阿里支付接口，去检查该笔交易的状态，这个叫支付结果检查  用的是延迟队列解决

        //延迟队列：定时任务
        //在提交支付时，向消息队列发送一个延迟执行的消息任务，当该任务被支付服务执行时，在消费任务的程序中去查询
        //当前交易的交易状态，根据交易状态决定解除延迟任务
        //默认延迟属性是关闭的，要去开启消息队列的延迟属性

        //向消息中间件发送一个检查支付状态（支付服务消费）的延迟消息队列
        paymentService.sendDelayPaymentResultCheckQueue(outTradeNo,5);

        /*
          消费延迟队列（支付服务）
              检查当前交易的状态【根据outTradeNo在alipay上检查】两种情况：
                    没有成功支付：设置重新发送延迟检查的时间和队列
                    支付成功   ：更新支付信息发送订单队列【有可能支付宝回调的那条线路已经执行了更新支付信息发送订单队
                               列，所以这里要进行幂等性检查（对于服务器的写操作，只做一次，如果相同的请求再次过来只
                               返回相同的结果就可以了）】
        */

        // 提交请求到支付宝
        return form;
    }

    @RequestMapping("index")
    @LoginRequired(loginSuccess = true)
    //在结算页面点击提交订单后，会到达submitOrder方法，这个方法会重定向到下面这个方法
    public String index(String outTradeNo, BigDecimal totalAmount, HttpServletRequest request, ModelMap modelMap){
        String memberId = (String)request.getAttribute("memberId");
        String nickname = (String)request.getAttribute("nickname");

        modelMap.put("nickname",nickname);
        modelMap.put("outTradeNo",outTradeNo);
        modelMap.put("totalAmount",totalAmount);

        return "index";
    }


    @RequestMapping("mx/submit")
    @LoginRequired(loginSuccess = true)
    public String mx(String outTradeNo, BigDecimal totalAmount, HttpServletRequest request, ModelMap modelMap){


        return null;
    }
}
