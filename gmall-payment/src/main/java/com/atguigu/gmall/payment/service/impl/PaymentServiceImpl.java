package com.atguigu.gmall.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.mq.ActiveMQUtil;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.service.PaymentService;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    AlipayClient alipayClient;

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Autowired
    PaymentInfoMapper paymentInfoMapper;

    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {

        paymentInfoMapper.insertSelective(paymentInfo);

    }

    @Override
    public void sendDelayPaymentResultCheckQueue(String outTradeNo, int count) {


        System.out.println("发送延迟队列:"+count);
        Connection connection = null;
        Session session = null;
        try {
            connection = activeMQUtil.getConnectionFactory().createConnection();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        try{
            Queue payhment_success_queue = session.createQueue("PAYMENT_CHECK_QUEUE");
            MessageProducer producer = session.createProducer(payhment_success_queue);

            //TextMessage textMessage=new ActiveMQTextMessage();//字符串文本

            MapMessage mapMessage = new ActiveMQMapMessage();// hash结构

            mapMessage.setString("out_trade_no",outTradeNo);
            mapMessage.setInt("count",count);

            // 为消息加入延迟时间  延迟一分钟之后有效 3秒后会被监听到
            mapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY,1000*120);

            producer.send(mapMessage);

            session.commit();
        }catch (Exception ex){
            // 消息回滚
            try {
                session.rollback();
            } catch (JMSException e1) {
                e1.printStackTrace();
            }
        }finally {
            try {
                connection.close();
            } catch (JMSException e1) {
                e1.printStackTrace();
            }
        }

    }

    //@Transactional  加普通事务
    //将消息的事务和普通事务放在一起，同时提交或者回滚  只要保证消息被成功消费就可以了
    //   没有像数据库那样让事务一起提交一起回滚，这样效率太低
    @Override
    public void updatePayment(PaymentInfo paymentInfo) {
        
        //幂等性检查
        PaymentInfo paymentInfoParam = new PaymentInfo();
        paymentInfoParam.setOrderSn(paymentInfo.getOrderSn());
        PaymentInfo paymentInfoResult = paymentInfoMapper.selectOne(paymentInfo);
        if(StringUtils.isNotBlank(paymentInfoResult.getPaymentStatus()) && paymentInfoResult.getPaymentStatus().equals("已支付")){
            return ;
        } else {

            String orderSn = paymentInfo.getOrderSn();
            Example e = new Example(PaymentInfo.class);
            e.createCriteria().andEqualTo("orderSn", orderSn);
            paymentInfoMapper.updateByExampleSelective(paymentInfo, e);

            Connection connection = null;
            Session session = null;
            try {
                connection = activeMQUtil.getConnectionFactory().createConnection();
                session = connection.createSession(true, Session.SESSION_TRANSACTED);
            } catch (JMSException jmsException) {
                jmsException.printStackTrace();
            }

            try {

                paymentInfoMapper.updateByExampleSelective(paymentInfo, e);
                //调用mq发送支付成功的消息【1】
                Queue payment_check_queue = session.createQueue("PAYMENT_SUCCESS_QUEUE");
                MessageProducer producer = session.createProducer(payment_check_queue);
                MapMessage mapMessage = new ActiveMQMapMessage();
                mapMessage.setString("out_trade_no", paymentInfo.getOrderSn());
                producer.send(mapMessage);
                session.commit();
            } catch (Exception ex) {
                //消息回滚
                try {
                    //在producer端rollback()是指消息不会发送
                    session.rollback();
                } catch (JMSException jmsException) {
                    jmsException.printStackTrace();
                }
            } finally {
                try {
                    connection.close();
                } catch (JMSException jmsException) {
                    jmsException.printStackTrace();
                }
            }

        }
    }

    @Override
    public Map<String, Object> checkAlipayPayment(String out_trade_no) {

        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("out_trade_no", out_trade_no);
        request.setBizContent(JSON.toJSONString(requestMap));
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        Map<String, Object> resultMap = new HashMap<>();
        if(response.isSuccess()){
            resultMap.put("out_trade_no", response.getOutTradeNo());
            resultMap.put("trade_no", response.getTradeNo());
            resultMap.put("trade_status",response.getTradeStatus());
            requestMap.put("call_back_content", response.getMsg());
            System.out.println("调用成功");
        } else {
            System.out.println("调用失败");
        }

        return resultMap;
    }


}
