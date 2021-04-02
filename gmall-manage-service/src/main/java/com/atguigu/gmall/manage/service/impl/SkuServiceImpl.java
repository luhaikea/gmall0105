package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.PmsSkuAttrValue;
import com.atguigu.gmall.bean.PmsSkuImage;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.bean.PmsSkuSaleAttrValue;
import com.atguigu.gmall.manage.mapper.PmsSkuAttrValueMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuImageMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuInfoMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuSaleAttrValueMapper;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class SkuServiceImpl implements SkuService {

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    PmsSkuInfoMapper pmsSkuInfoMapper;

    @Autowired
    PmsSkuAttrValueMapper pmsSkuAttrValueMapper;

    @Autowired
    PmsSkuSaleAttrValueMapper pmsSkuSaleAttrValueMapper;

    @Autowired
    PmsSkuImageMapper pmsSkuImageMapper;

    @Override
    public void saveSkuInfo(PmsSkuInfo pmsSkuInfo) {

        // 插入skuInfo 使用通用mapper的insertSelective插入数据后,实体类初始为null的id主动映射为数据库自增长的id
        int i = pmsSkuInfoMapper.insertSelective(pmsSkuInfo);
        String skuId = pmsSkuInfo.getId();

        // 插入平台属性关联
        List<PmsSkuAttrValue> skuAttrValueList = pmsSkuInfo.getSkuAttrValueList();
        for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
            pmsSkuAttrValue.setSkuId(skuId);
            pmsSkuAttrValueMapper.insertSelective(pmsSkuAttrValue);
        }

        // 插入销售属性关联
        List<PmsSkuSaleAttrValue> skuSaleAttrValueList = pmsSkuInfo.getSkuSaleAttrValueList();
        for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuSaleAttrValueList) {
            pmsSkuSaleAttrValue.setSkuId(skuId);
            pmsSkuSaleAttrValueMapper.insertSelective(pmsSkuSaleAttrValue);
        }

        // 插入图片信息
        List<PmsSkuImage> skuImageList = pmsSkuInfo.getSkuImageList();
        for (PmsSkuImage pmsSkuImage : skuImageList) {
            pmsSkuImage.setSkuId(skuId);
            pmsSkuImageMapper.insertSelective(pmsSkuImage);
        }



    }

    @Override
    public PmsSkuInfo getSkuByIdFromDb(String skuId) {
        PmsSkuInfo pmsSkuInfo =  new PmsSkuInfo();
        pmsSkuInfo.setId(skuId);
        PmsSkuInfo pmsSkuInfo1 = pmsSkuInfoMapper.selectOne(pmsSkuInfo);
        //图片集合
        PmsSkuImage pmsSkuImage = new PmsSkuImage();
        pmsSkuImage.setSkuId(skuId);
        List<PmsSkuImage> pmsSkuImages = pmsSkuImageMapper.select(pmsSkuImage);
        pmsSkuInfo1.setSkuImageList(pmsSkuImages);


        return pmsSkuInfo1;
    }

    //redis中的数据存储策略（就是如何设计Key）:数据对象名：数据对象id：对象属性
    /*
      缓存在高并发和安全压力下的一些问题
      1、缓存穿透
            缓存穿透是指访问一个不存在的数据，由于缓存是不命中，将去查数据库，但数据库也无此记录，并且处于容错处理，
            我们没有将这次查询的null写入缓存，这将导致这个不存在的数据每次请求到要到存储层去查询，失去了缓存的意义，
            在流量大时，可能DB就挂掉了，要是有人利用不存在的 Key频繁攻击我们的应用，这就是漏洞。
            解决：将空结果进行缓存，并且设置很短的过期时间
      2、缓存击穿
            对于一些设置了过期时间的key，如果这些key可能会在某些时间点被超高并发地访问，是一个非常热点的数据，这个
            时候，需要考虑一个问题：如果这Key在大量请求同时进来前正好失效，那么所有对这个Key的数据查询都落到db，我
            们称为缓存击穿
               和缓存雪崩的区别：击穿是一个热点key的失效
                              雪崩是很多key的集体失效
             缓存击穿可以当做缓存失效也就是所有缓存不工作了
             解决：分布式锁（也就是做一个限流处理，在请求和DB之间加一个锁，让请求慢慢来访问DB）
                  使用redis数据库的分布式锁解决mysql的访问压力
             两种解决方式：
             两种分布式锁:           这是在解决有多个gmall-manage-service的情况下
                   第一种分布式锁；redis自带的一个分布式锁 set ex nx     【这种解决方案是需要另外一个redis专门做锁的】
                        nx是set方法的一个参数：表示只有键不存在时才对键进行设置操作  这个就是redis的分布式锁
                            此时redis已经失效在redis中get不到对应的值，这时节点去 set nx 一下，但只有一个节点set nx 成功，然后set nx成功后 拿着自己
                            的锁去访问mysql数据库，访问完了就删除set nx 的键，其他的节点就可以继续去访问
                            命令：set sku:108:lock 1 px 600000 nx  键：sku:108:lock 值：1 过期时间：px 600000  分布式锁参数：nx
                        xx只有键已经存在时，才对键进行设置操作
                   第二种分布式锁：redisson框架（一个redis的juc的实现，既有redis的功能，也有juc【java.util .concurrent工具包的简称】的功能）
                        jedis是没有多线程锁的机制，也就是多个jedis客户端同时访问一个redis可能发生错误
                    redisson简介：
                       Redisson是一个在Redis的基础上实现的Java驻内存数据网格（In-Memory Data Grid）。它不仅提供了一系列的分布式
                       的Java常用对象，还提供了许多分布式服务。其中包括(BitSet, Set, Multimap, SortedSet, Map, List, Queue,
                       BlockingQueue, Deque, BlockingDeque, Semaphore, Lock, AtomicLong, CountDownLatch, Publish /
                       Subscribe, Bloom filter, Remote service, Spring cache, Executor service, Live Object service,
                       Scheduler service) Redisson提供了使用Redis的最简单和最便捷的方法。Redisson的宗旨是促进使用者对Redis的关
                       注分离（Separation of Concern），从而让使用者能够将精力更集中地放在处理业务逻辑上。

      3、缓存雪崩
            缓存雪崩是指在我们设置缓存时采用了相同的过期时间，导致缓存在某一时刻同时失效，请求全部转发到DB,DB瞬时压
            力过重雪崩
            解决:原有的失效时间基础上增加一个随机值，比如1-5分钟的随机，这样每一个缓存的过期时间的重复率就会降低，就
            很难引发集体失效的事件
     */


    @Override
    public PmsSkuInfo getSkuById(String skuId) {
        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();

        //链接缓存
        Jedis jedis = redisUtil.getJedis();
        //查询缓存
        String skuKey = "sku:"+skuId+":info";
        String skuJson = jedis.get(skuKey);

        if(StringUtils.isNoneBlank(skuJson)){
            pmsSkuInfo = JSON.parseObject(skuJson, PmsSkuInfo.class);
        } else{
            //这里是缓存中没有对应的值  有两种情况一个是确实没有，另一个是缓存击穿（缓存失效）这时需要用加分布式锁的方式去访问DB
            //设置分布式锁
            //锁是锁一个sku其他skubu不影响
            //将锁的值设置成唯一的，代表这个唯一的线程  防止线程删除其他线程的锁
            //也就是一个线程得到锁然后去访问数据库，还没来得及回来删除自己的锁，自己的锁就过期了，另外一个线程有获得了锁，这时前面那个线程有复活删除了这个线程的锁
            String token = UUID.randomUUID().toString();
            String ok = jedis.set("sku:"+skuId+":lock",token,"nx","px",10*1000);
            if(StringUtils.isNoneBlank(ok)&&ok.equals("OK")){
                //设置成功，有权在10秒的过期时间内访问数据库
                pmsSkuInfo = getSkuByIdFromDb(skuId);

                if(pmsSkuInfo != null){
                    jedis.set(skuKey, JSON.toJSONString(pmsSkuInfo));
                } else{  //数据库中不存在该sku 为了防止缓存穿透
                    jedis.setex(skuKey,60*3 ,JSON.toJSONString(""));//加缓存时间
                }

                //此处应该释放锁
                //先确定是不是自己的锁再释放    先进行非空判断是为了防止空指针异常
                //还有一种极限情况就是在执行下面这个判断时锁过期了 也就是lockToken是自己锁的值，但jedis.del()删的确是其他线程的锁
                // 所以下面这个还存在问题  解决：jedis.eval("lua")可用lua脚本，在查询到key的同时删除该key，防止高并发下的意外发生

                String lockToken = jedis.get("sku:"+skuId+":lock");
                if(StringUtils.isNoneBlank(lockToken)&&lockToken.equals(token)){
                    jedis.del("sku:"+skuId+":lock");
                }

            } else{
                //设置失败 有节点已经加锁  自旋（该线程下睡眠几秒后，重新尝试访问访问本方法）
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //不能直接getSkuById(skuId)必须是return
                return getSkuById(skuId);
            }
        }

        jedis.close();
        return pmsSkuInfo;
    }

    @Override
    public List<PmsSkuInfo> getSkuSaleAttrValueListBySpu(String productId) {

        List<PmsSkuInfo> pmsSkuInfos = pmsSkuInfoMapper.selectSkuSaleAttrValueListBySpu(productId);
        return pmsSkuInfos;
    }

    @Override
    public List<PmsSkuInfo> getAllSku() {

        List<PmsSkuInfo> pmsSkuInfos = pmsSkuInfoMapper.selectAll();

        for(PmsSkuInfo pmsSkuInfo : pmsSkuInfos){

            String skuId = pmsSkuInfo.getId();
            PmsSkuAttrValue pmsSkuAttrValue = new PmsSkuAttrValue();
            pmsSkuAttrValue.setSkuId(skuId);
            List<PmsSkuAttrValue> select = pmsSkuAttrValueMapper.select(pmsSkuAttrValue);
            pmsSkuInfo.setSkuAttrValueList(select);

        }
        return pmsSkuInfos;
    }

    @Override
    public boolean checkPrice(String productSkuId, BigDecimal productPrice) {

        boolean b = false;

        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();
        pmsSkuInfo.setId(productSkuId);
        PmsSkuInfo pmsSkuInfo1 = pmsSkuInfoMapper.selectOne(pmsSkuInfo);

        BigDecimal price = pmsSkuInfo1.getPrice();

        if(productPrice.compareTo(price)==0){
            b = true;
        }



        return b;
    }
}
/*
{  "catalog3Id": "999",
   "id":         "106",
   "price":      1222.0,
   "productId":  "70",
   "skuDefaultImg": "http://114.55.140.81/group1/M00/00/00/rBn8sV_GCuuAd4p7AAPHcdLqmJ846.jpeg",
   "skuDesc":   "bebe8 耐克AJ1 NIKE AIR JORDAN 1 MID 乔1 男女子小扣碎中帮篮球",
   "skuImageList":[    { "id":"1007",
                         "imgName":"mn10.jpeg",
                         "imgUrl":"http://114.55.140.81/group1/M00/00/00/rBn8sV_GCuuAd4p7AAPHcdLqmJ846.jpeg",
                         "isDefault":"1",
                         "skuId":"106"
                       },
                       { "id":"1008",
                         "imgName":"mn7.jpg",
                         "imgUrl":"http://114.55.140.81/group1/M00/00/00/rBn8sV_GCuuAIymtAABkkt5n068403.jpg",
                         "isDefault":"0",
                         "skuId":"106"
                       }
                   ],
   "skuName":  "bebe8 耐克AJ1 NIKE AIR JORDAN 1 MID 乔1 男女子小扣碎中帮篮球鞋 黑红禁穿男女554724/554725-054 43",
   "weight":   2.0
}
 */
