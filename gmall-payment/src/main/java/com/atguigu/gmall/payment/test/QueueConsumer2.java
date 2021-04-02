package com.atguigu.gmall.payment.test;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
//consumer
//     开启事务 签收必须写 Session.SESSION_TRANSACTED
//          收到消息后，消息并没有真正的被消费。消息只是被锁住。一旦出现该线程死掉、抛异
//          常，或者程序执行了session.rollback()那么消息会释放，重新回到队列中被别的消费端再次消费。
//     不开启事务  签收方式选择 Session.AUTO_ACKNOWLEDGE
//          只要调用comsumer.receive方法 ，自动确认。
//     不开启事务，签收方式选择 Session.CLIENT_ACKNOWLEDGE
//          需要客户端执行 message.acknowledge(),否则视为未提交状态，线程结束后，其他线程还可以接收到。
//          这种方式跟事务模式很像，区别是不能手动回滚,而且可以单独确认某个消息
//    不开启 事务，签收方式选择Session.DUPS_OK_ACKNOWLEDGE
//          在Topic模式下做批量签收时用的，可以提高性能。但是某些情况消息可能会被重复提交，使用这种模式的consumer要可以处理重复提交的问题。

//消费者都是在监听
public class QueueConsumer2 {
    public static void main(String[] args) {
        ConnectionFactory connect = new ActiveMQConnectionFactory(ActiveMQConnection.DEFAULT_USER,ActiveMQConnection.DEFAULT_PASSWORD,"tcp://114.55.140.81:61616");
        try {
            Connection connection = connect.createConnection();
            connection.start();
            //第一个值表示是否使用事务，如果选择true，第二个值相当于选择0
            //不开启事务  不需要提交和回滚
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination testqueue = session.createQueue("drink");

            MessageConsumer consumer = session.createConsumer(testqueue);
            consumer.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    if(message instanceof TextMessage){
                        try {
                            String text = ((TextMessage) message).getText();
                            System.err.println(text+"我来了，我来执行。。。我叫飞龙");

                            // session.commit();
                            // session.rollback();
                        } catch (JMSException e) {
                            // TODO Auto-generated catch block
                            // session.rollback();
                            e.printStackTrace();
                        }
                    }
                }
            });


        }catch (Exception e){
            e.printStackTrace();;
        }
    }

}
