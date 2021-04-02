package com.atguigu.gmall.payment.test;

import com.atguigu.gmall.mq.ActiveMQUtil;
import org.springframework.beans.factory.annotation.Autowired;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

public class ActiveMQTest {

    @Autowired
    ActiveMQUtil activeMQUtil;

    public static void main(String[] args) throws JMSException {
        new ActiveMQTest().test();
    }
    public void test() throws JMSException {
        ConnectionFactory connectFactory = activeMQUtil.getConnectionFactory();

        Connection connection = connectFactory.createConnection();

        System.out.println(connection);
    }
}
