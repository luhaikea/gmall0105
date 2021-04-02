package com.atguigu.gmall.redissontest.redissonTest;

import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import redis.clients.jedis.Jedis;

@Controller
public class RedissonController {

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    RedissonClient redissonClient;

    @RequestMapping("/testRedisson")
    @ResponseBody
    public String testRedisson(){

        //基于Redis的Redisson分布式可重入锁RLock java对象实现了java.util.concurrent.locks.Lock接口
        //java.util.concurrent.locks.Lock锁的是同一个jvm的一个或几个线程
        //而Rlock锁的是分布式环境下的redis的操作连接
        Jedis jedis = redisUtil.getJedis();
        RLock lock = redissonClient.getLock("lock");
        lock.lock();
        //开始   如果不加锁从开始到结束的这段代码就有可能出现安全问题
        try{
            String v = jedis.get("k");
            if(StringUtils.isBlank(v)){
                v = "1";
            }
            System.out.println("->"+v);

            jedis.set("k",(Integer.parseInt(v)+1)+"");
        } finally {
            jedis.close();
            lock.unlock();
        }
        //结束
        return "s";
    }
}

/*
                          Nginx
Nginx是一个高性能的HTTP和反向代理web服务器，特点是并发能力强，占用内存少









*/


























