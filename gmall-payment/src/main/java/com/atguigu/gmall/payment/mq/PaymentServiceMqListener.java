package com.atguigu.gmall.payment.mq;

import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.service.PaymentService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.util.Date;
import java.util.Map;

@Component
public class PaymentServiceMqListener {

    @Autowired
    PaymentService paymentService;

    //产生异常消费七次之后，不会再消费 会在MQ中产生一个死信队列【也就是回反复监听七次】
    @JmsListener(destination = "PAYMENT_CHECK_QUEUE", containerFactory = "jmsQueueListener")
    public void consumePaymentCheckResult(MapMessage mapMessage){

        String out_trade_no = null;
        int count=0;
        try {
            out_trade_no = mapMessage.getString("out_trade_no");
            count = mapMessage.getInt("count");
        } catch (JMSException e) {
            e.printStackTrace();
        }

        //调用paymentService的支付宝检查接口
        Map<String, Object> resultMap = paymentService.checkAlipayPayment(out_trade_no);

        if(resultMap.isEmpty()){
            //继续发送延迟检查任务，计算延迟时间等
            if(count>0){
                count--;
                paymentService.sendDelayPaymentResultCheckQueue(out_trade_no, count);
            } else{
                //检查次数用尽
                System.out.println("检查次数用尽");
            }

        } else {
            //交易状态：WAIT_BUYER_PAY（交易创建，等待买家付款）、TRADE_CLOSED（未付款交易超时关闭，或支付完成后全额退款）、TRADE_SUCCESS（交易支付成功）、TRADE_FINISHED（交易结束，不可退款）
            String trade_status = (String) resultMap.get("trade_status");

            //根据查询的支付状态结果，判断是否进行下一次的延迟任务还是支付成功更新数据和后续任务
            if(StringUtils.isNotBlank(trade_status)&&trade_status.equals("TRADE_SUCCESS")){
                //支付成功，更新支付 发送支付队列

                PaymentInfo paymentInfo = new PaymentInfo();
                paymentInfo.setOrderSn(out_trade_no);
                paymentInfo.setPaymentStatus("已支付");
                paymentInfo.setAlipayTradeNo((String)resultMap.get("trade_no"));// 支付宝的交易凭证号
                paymentInfo.setCallbackContent((String) resultMap.get("call_back_content"));//回调请求字符串
                paymentInfo.setCallbackTime(new Date());

                paymentService.updatePayment(paymentInfo);

                System.out.println("已经支付成功，支付成功，更新支付 发送支付队列");

            } else {

                //继续发送延迟检查任务，计算延迟时间等
                if(count>0){
                    count--;
                    paymentService.sendDelayPaymentResultCheckQueue(out_trade_no, count);
                } else{
                    //检查次数用尽
                    System.out.println("检查次数用尽");
                }
            }

        }

    }

}
