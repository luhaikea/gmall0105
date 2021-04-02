package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OmsOrder;

public interface OrderService {
    String genTradeCode(String memberId);

    String checkTradeCode(String memberId, String tradeCode);

    void saveOrder(OmsOrder omsOrder);

    OmsOrder getOrderByOutTradeNo(String outTradeNo);

    void updateOrder(OmsOrder omsOrder);
}
