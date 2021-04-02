package com.atguigu.gmall.payment.test;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.jms.*;

public class TopicProducer {

    public static void main(String[] args) {

        ConnectionFactory connect = new ActiveMQConnectionFactory("tcp://localhost:61616");
        try {
            Connection connection = connect.createConnection();
            connection.start();
            //第一个值表示是否使用事务，如果选择true，第二个值相当于选择0
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);// 开启事务

            Topic topic = session.createTopic("speaking");// 话题模式的消息

            MessageProducer producer = session.createProducer(topic);
            TextMessage textMessage=new ActiveMQTextMessage();
            textMessage.setText("为尚硅谷的伟大复兴而努力奋斗！");
            //话题模式默认不持久化  必须要有消费者正在监听
            //之所以不持久化是因为 队列模式是只要有一个消费者消费了其他人就不用消费了则证明这个消息成功了
            //话题模式在一个消费者消费了之后还有可能被其他消费者消费 也就是话题模式不去记录谁消费了
            //它只会在发出的一瞬间如果谁在监听就让谁消费
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            producer.send(textMessage);
            session.commit();// 提交事务
            connection.close();//关闭链接

        } catch (JMSException e) {
            e.printStackTrace();
        }

    }
}
