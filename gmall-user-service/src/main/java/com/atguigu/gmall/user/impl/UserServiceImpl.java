package com.atguigu.gmall.user.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.user.mapper.UmsMemberReceiveAddressMapper;
import com.atguigu.gmall.user.mapper.UserMapper;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserMapper userMapper;

    @Autowired
    UmsMemberReceiveAddressMapper umsMemberReceiveAddressMapper;

    @Autowired
    RedisUtil redisUtil;

    @Override
    public List<UmsMember> getAllUser() {

     //   List<UmsMember> umsMemberList = userMapper.selectAllUser();
        List<UmsMember> umsMemberListTk = userMapper.selectAll();
        return umsMemberListTk;
    }

    @Override
    public List<UmsMemberReceiveAddress> getgetReceiveAddressByMemberId(String memberId) {

        UmsMemberReceiveAddress umsMemberReceiveAddress = new UmsMemberReceiveAddress();
        umsMemberReceiveAddress.setMemberId(memberId);
        //查询信息封装在umsMemberReceiveAddress里面哪一个字段不为空，就根据哪一个字段去查询
        //Ctrl + Alt + v  生成本地变量
     // List<UmsMemberReceiveAddress> umsMemberReceiveAddresses = umsMemberReceiveAddressMapper.selectByExample(umsMemberReceiveAddress);
        List<UmsMemberReceiveAddress> umsMemberReceiveAddresses = umsMemberReceiveAddressMapper.select(umsMemberReceiveAddress);
        return umsMemberReceiveAddresses;
    }

    @Override
    public UmsMember login(UmsMember umsMember) {

        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            if(jedis!=null){
                //需要在写注册功能是保证不能有相同的密码
                String umsMemberStr = jedis.get("user:" + umsMember.getPassword() + ":info");
                if(StringUtils.isNotBlank(umsMemberStr)){
                    //密码正确
                    UmsMember umsMemberFromCache = JSON.parseObject(umsMemberStr, UmsMember.class);
                    return umsMemberFromCache;
                } else{
                    //密码错误
                    //缓存中没有 开数据库查询
                    UmsMember umsMemberFromDb = loginFromDb(umsMember);
                    if(umsMemberFromDb!=null){
                        //带过期时间
                        jedis.setex("user:" + umsMember.getPassword() + ":info",60*60*24,JSON.toJSONString(umsMemberFromDb));
                    }
                    return umsMemberFromDb;
                }
            } else{
                //Jedis连接不上了 开启数据库  redis失效
                UmsMember umsMemberFromDb = loginFromDb(umsMember);
                if(umsMemberFromDb!=null){
                    //带过期时间
                    jedis.setex("user:" + umsMember.getPassword() + ":info",60*60*24,JSON.toJSONString(umsMemberFromDb));
                }
                return umsMemberFromDb;
            }

        } finally {
             jedis.close();
        }

    }

    @Override
    public void addUserToken(String token, String memberId) {
        Jedis jedis = redisUtil.getJedis();
        jedis.setex("user:"+memberId+":token", 60*60*2, token);
        jedis.close();
    }

    @Override
    public UmsMember addOauthUser(UmsMember umsMember) {

        userMapper.insertSelective(umsMember);

        return umsMember;

    }

    @Override
    public UmsMember checkOauthUser(UmsMember umsCheck) {
        UmsMember umsMember = userMapper.selectOne(umsCheck);
        return umsMember;
    }

    @Override
    public UmsMemberReceiveAddress getReceiveAddressById(String receiveAddressId) {

        //生产环境加try  异常处理
        UmsMemberReceiveAddress umsMemberReceiveAddress = new UmsMemberReceiveAddress();
        umsMemberReceiveAddress.setId(receiveAddressId);
        UmsMemberReceiveAddress umsMemberReceiveAddress1 = umsMemberReceiveAddressMapper.selectOne(umsMemberReceiveAddress);
        return umsMemberReceiveAddress1;
    }

    private UmsMember loginFromDb(UmsMember umsMember) {

        List<UmsMember> umsMembers = userMapper.select(umsMember);
        if(umsMembers!=null){
            return umsMembers.get(0);
        }

        return null;

    }
}
