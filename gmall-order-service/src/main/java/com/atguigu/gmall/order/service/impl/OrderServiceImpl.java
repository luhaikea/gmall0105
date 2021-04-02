package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.OmsOrderItem;
import com.atguigu.gmall.mq.ActiveMQUtil;
import com.atguigu.gmall.order.mapper.OmsOrderItemMapper;
import com.atguigu.gmall.order.mapper.OmsOrderMapper;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    OmsOrderMapper omsOrderMapper;

    @Autowired
    OmsOrderItemMapper omsOrderItemMapper;

    @Reference
    CartService cartService;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Override
    public String genTradeCode(String memberId) {

        Jedis jedis = redisUtil.getJedis();
        String tradeCode = UUID.randomUUID().toString();
        String tradeKey = "user:"+memberId+":tradeCode";
        jedis.setex(tradeKey,60*15, tradeCode);
        jedis.close();
        return tradeCode;
    }

    @Override
    public String checkTradeCode(String memberId, String tradeCode) {

        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            String tradeKey = "user:"+memberId+":tradeCode";
            //这里有一个并发问题 就是一个线程判断执行完成后被中断但没有删除交易码 之后另外一个线程到来就会发生问题
            //如何防止并发情况下的一key多用，使用lua脚本在查询到该key的时候，马上删除
            //防止黑客攻击
//            String tradeCodeFromCache = jedis.get(tradeKey);
//            if(StringUtils.isNotBlank(tradeCodeFromCache) && tradeCodeFromCache.equals(tradeCode)) {
//                //删除交易码
//                jedis.del(tradeCode);
//                return "success";
//            } else {
//                return "fail";
//            }

            //lua脚本解决
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            Long eval = (Long) jedis.eval(script, Collections.singletonList(tradeKey), Collections.singletonList(tradeCode));

            if (eval!=null&&eval!=0) {
                jedis.del(tradeKey);
                return "success";
            } else {
                return "fail";
            }

        } finally {
            jedis.close();
        }


    }

    @Override
    public void saveOrder(OmsOrder omsOrder) {

        // 保存订单表
        omsOrderMapper.insertSelective(omsOrder);
        String orderId = omsOrder.getId();
        // 保存订单详情
        List<OmsOrderItem> omsOrderItems = omsOrder.getOmsOrderItems();
        for (OmsOrderItem omsOrderItem : omsOrderItems) {
            omsOrderItem.setOrderId(orderId);
            omsOrderItemMapper.insertSelective(omsOrderItem);
            // 删除购物车数据
            // cartService.delCart();
        }
    }

    @Override
    public OmsOrder getOrderByOutTradeNo(String outTradeNo) {

        OmsOrder omsOrder = new OmsOrder();
        omsOrder.setOrderSn(outTradeNo);
        OmsOrder omsOrder1 = omsOrderMapper.selectOne(omsOrder);

        return omsOrder1;
    }

    @Override
    public void updateOrder(OmsOrder omsOrder) {

        Example example = new Example(OmsOrder.class);
        example.createCriteria().andEqualTo("orderSn", omsOrder.getOrderSn());

        OmsOrder omsOrderUpdate = new OmsOrder();
        omsOrderUpdate.setStatus("1");

        //发送一个订单已支付的对列，提供给库存消费
        /*
           订单消息（订单已支付队列）-->库存拆单-->
        */

        Connection connection = null;
        Session session = null;
        try {
            connection = activeMQUtil.getConnectionFactory().createConnection();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
        } catch (JMSException jmsException) {
            jmsException.printStackTrace();
        }

        try {
            //这里更新与发送消息在一起，在一定程度上提高了并发
            omsOrderMapper.updateByExampleSelective(omsOrderUpdate, example);

            Queue payment_check_queue = session.createQueue("ORDER_PAY_QUEUE");
            MessageProducer producer = session.createProducer(payment_check_queue);
            MapMessage mapMessage = new ActiveMQMapMessage();

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
