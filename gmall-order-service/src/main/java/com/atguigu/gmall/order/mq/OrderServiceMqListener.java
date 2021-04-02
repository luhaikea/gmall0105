package com.atguigu.gmall.order.mq;

import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class OrderServiceMqListener {

    @Autowired
    OrderService orderService;

    //产生异常消费七次之后，不会再消费 会在MQ中产生一个死信队列【也就是回反复监听七次】
    @JmsListener(destination = "PAYMENT_SUCCESS_QUEUE", containerFactory = "jmsQueueListener")
    public void consumePayMentResult(MapMessage mapMessage){

        String out_trade_no = null;
        try {
            out_trade_no = mapMessage.getString("out_trade_no");
        } catch (JMSException e) {
            e.printStackTrace();
        }

        //更新订单状态业务
        OmsOrder omsOrder = new OmsOrder();
        omsOrder.setOrderSn(out_trade_no);
        orderService.updateOrder(omsOrder);

    }




}
