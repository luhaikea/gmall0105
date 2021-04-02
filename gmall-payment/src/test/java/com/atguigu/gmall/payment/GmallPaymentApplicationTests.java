package com.atguigu.gmall.payment;

import com.atguigu.gmall.mq.ActiveMQUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

@SpringBootTest
class GmallPaymentApplicationTests {

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Test
    void contextLoads() throws JMSException {
        ConnectionFactory connectFactory = activeMQUtil.getConnectionFactory();

        Connection connection = connectFactory.createConnection();

        System.out.println(connection);
    }

}


