package com.atguigu.gmall.payment.test;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.jms.*;
// producer
//    开启事务
//        只执行send并不会提交到队列中，只有当执行session.commit()时，消息才被真正的提交到队列中。
//    不开启事务
//        只要执行send，就进入到队列中
public class QueueProducer {

    public static void main(String[] args) {

        ConnectionFactory connect = new ActiveMQConnectionFactory("tcp://114.55.140.81:61616");
        try {
            Connection connection = connect.createConnection();
            connection.start();
            //第一个值表示是否使用事务，如果选择true，第二个值相当于选择0
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);// 开启事务
            Queue testqueue = session.createQueue("myqueue");// 队列模式的消息  参数为标题

            //Topic t = session.createTopic("");// 话题模式的消息
            MessageProducer producer = session.createProducer(testqueue);

            TextMessage textMessage=new ActiveMQTextMessage();
            textMessage.setText("发送一个消息");     //参数为消息内容
            /*
            在发送者将消息发送出去后，消息中心首先将消息存储到本地数据文件、内存数据库或远程数据库等再试图将消息发送给接
            收者，成功则将消息从存储中删除，失败则继续尝试发送。消息中心启动后首先要检查指定的存储位置，若有未发送成功的消
            息，则需要把消息发送出去。
             */
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);  //deliver:传送、发出  传输模式  持久化
            producer.send(textMessage);

            TextMessage textMessage1 = new ActiveMQTextMessage();
            textMessage1.setText("发送另外一条消息");
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);  //deliver:传送、发出  传输模式  持久化
            producer.send(textMessage1);

            session.commit();// 提交事务
            connection.close();//关闭链接

        } catch (JMSException e) {
            e.printStackTrace();
        }

    }
}
