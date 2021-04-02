package com.atguigu.gmall.passport.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.util.HttpclientUtil;

import java.util.HashMap;
import java.util.Map;

public class TestOauth2 {

    public static void main(String[] args) {
/*
1、浏览器向第三方服务器请求授权,请求的url中携带回调地址（也就是用第三方服务器的用户名和密码在第三方服务器给的页面上登录，
2、第三方服务器给回调地址返回一个授权码
3、gamll会将授权码发送给第三方服务器，（浏览器同时需要授权码、APP Key、App Secret三个一起去交换access_token），第三方服务器会给gmall一个access_token,
4、gmall将会凭借这个access_token去第三方服务器获取用户数据（access_token有一定的时间在这个时间内，gmall可以随时向第三方服务器请求用户信息）

 */
        //87510233
        //http://passport.gmall.com:8085/vlogin
        //第一个url 打开这个url会出现第三方服务器的登录界面
        //返回的这个s就是登录页面的html代码
        String s = HttpclientUtil.doGet("https://api.weibo.com/oauth2/authorize?client_id=87510233&response_type=code&redirect_uri=http://passport.gmall.com:8085/vlogin");
        //在浏览器输入这个url后第三方服务器会发起这样一个url请求【http://passport.gmall.com:8085/vlogin?code=2f07b7444954f51570fbbce2c28f6f03】这个code就是授权码
        System.out.println(s);

        //第二个url  http://passport.gmall.com:8085/vlogin这个是回调地址，通过回调地址获得授权码
        //http://passport.gmall.com:8085/vlogin?code=0b8e7513b05e82b8f14fec1fb8fab2ad

        //第三个url   这个必须用post请求
        //https://api.weibo.com/oauth2/access_token
        String s3 = "https://api.weibo.com/oauth2/access_token";
        Map<String, String> map = new HashMap();
        map.put("client_id", "87510233");
        map.put("client_secret", "5a789c350d00b1f3a3d454d2c3159aa2");
        map.put("grant_type", "authorization_code");
        map.put("redirect_uri", "http://passport.gmall.com:8085/vlogin");
        map.put("code", "d5a12bbbad7e2c0d1146070e9df14126");

        //这个access_token在一段时间后也会过期  它依赖于授权码当有新的授权码时（发送第一个url请求时）旧的access_token就会过期
        //code用后即毁  access_token在几天内是一样的
        //access_token在授权码有效期内可以使用，每新生成一次授权码，说明用户对第三方数据进行重启授权，之前的access_token和授权码过期
        String access_token = HttpclientUtil.doPost(s3, map);

        System.out.println(access_token);
        /*
         {
           "access_token":"2.00kEydFI046LvF80953404950cU38r",
           "remind_in":"157679999",
           "expires_in":157679999,       过期时间
           "uid":"7412470062",           当前授权的那个用户在第三方服务器上的id
           "isRealName":"true"
         }
        */
        Map<String, String> access_tokenMap = JSON.parseObject(access_token,Map.class);
        //打印access_token
        System.out.println(access_tokenMap.get("access_token"));

        //第四个url   用access_token查询用户信息
        //https://api.weibo.com/2/users/show.json?access_token=2.00kEydFI046LvF80953404950cU38r&uid=7412470062

        String s4 = "https://api.weibo.com/2/users/show.json?access_token=2.00kEydFI046LvF80953404950cU38r&uid=7412470062";
        String user_json = HttpclientUtil.doGet(s4);
        Map<String, String> user_jsonMap = JSON.parseObject(user_json, Map.class);
        System.out.println(user_json);
        /*
        {
           "id":7412470062,
           "idstr":"7412470062",
           "class":1,
           "screen_name":"阳光温热199107",
           "name":"阳光温热199107",
           "province":"62",
           "city":"1000",
           "location":"甘肃",
           "description":"",
           "url":"",
           "profile_image_url":"https://tvax3.sinaimg.cn/crop.0.0.996.996.50/0085DY4Kly8gcsk5n3icfj30ro0ro3yx.jpg?KID=imgbed,tva&Expires=1608395220&ssig=LQEsepCcf1","cover_image_phone":"http://ww1.sinaimg.cn/crop.0.0.640.640.640/549d0121tw1egm1kjly3jj20hs0hsq4f.jpg","profile_url":"u/7412470062","domain":"","weihao":"","gender":"m","followers_count":1,"friends_count":9,"pagefriends_count":0,"statuses_count":0,"video_status_count":0,"video_play_count":0,"favourites_count":0,"created_at":"Fri Mar 13 20:07:12 +0800 2020","following":false,"allow_all_act_msg":false,"geo_enabled":true,"verified":false,"verified_type":-1,"remark":"","insecurity":{"sexual_content":false},"ptype":0,"allow_all_comment":true,"avatar_large":"https://tvax3.sinaimg.cn/crop.0.0.996.996.180/0085DY4Kly8gcsk5n3icfj30ro0ro3yx.jpg?KID=imgbed,tva&Expires=1608395220&ssig=A4W84ebrh9","avatar_hd":"https://tvax3.sinaimg.cn/crop.0.0.996.996.1024/0085DY4Kly8gcsk5n3icfj30ro0ro3yx.jpg?KID=imgbed,tva&Expires=1608395220&ssig=4HCEGxnqVe","verified_reason":"","verified_trade":"","verified_reason_url":"","verified_source":"","verified_source_url":"","follow_me":false,"like":false,"like_me":false,"online_status":0,"bi_followers_count":0,"lang":"zh-cn","star":0,"mbtype":0,"mbrank":0,"block_word":0,"block_app":0,"credit_score":80,"user_ability":0,"urank":0,"story_read_state":-1,"vclub_member":0,"is_teenager":0,"is_guardian":0,"is_teenager_list":0,"pc_new":0,"special_follow":false,"planet_video":0,"video_mark":0,"live_status":0}

         */


    }


    public static String getCode(){

        // 1 获得授权码
        // 187638711
        // http://passport.gmall.com:8085/vlogin

        String s1 = HttpclientUtil.doGet("https://api.weibo.com/oauth2/authorize?client_id=187638711&response_type=code&redirect_uri=http://passport.gmall.com:8085/vlogin");

        System.out.println(s1);

        // 在第一步和第二部返回回调地址之间,有一个用户操作授权的过程

        // 2 返回授权码到回调地址

        return null;
    }

    public static String getAccess_token(){
        // 换取access_token
        // client_secret=a79777bba04ac70d973ee002d27ed58c
        // client_id=187638711
        String s3 = "https://api.weibo.com/oauth2/access_token?";//?client_id=187638711&client_secret=a79777bba04ac70d973ee002d27ed58c&grant_type=authorization_code&redirect_uri=http://passport.gmall.com:8085/vlogin&code=CODE";
        Map<String,String> paramMap = new HashMap<>();
        paramMap.put("client_id","187638711");
        paramMap.put("client_secret","a79777bba04ac70d973ee002d27ed58c");
        paramMap.put("grant_type","authorization_code");
        paramMap.put("redirect_uri","http://passport.gmall.com:8085/vlogin");
        paramMap.put("code","b882d988548ed2b9174af641d20f0dc1");// 授权有效期内可以使用，没新生成一次授权码，说明用户对第三方数据进行重启授权，之前的access_token和授权码全部过期
        String access_token_json = HttpclientUtil.doPost(s3, paramMap);

        Map<String,String> access_map = JSON.parseObject(access_token_json,Map.class);

        System.out.println(access_map.get("access_token"));
        System.out.println(access_map.get("uid"));

        return access_map.get("access_token");
    }

    public static Map<String,String> getUser_info(){

        // 4 用access_token查询用户信息
        String s4 = "https://api.weibo.com/2/users/show.json?access_token=2.00HMAs7H0p5_hMdbefcb34140Lydjf&uid=6809985023";
        String user_json = HttpclientUtil.doGet(s4);
        Map<String,String> user_map = JSON.parseObject(user_json,Map.class);

        System.out.println(user_map.get("1"));

        return user_map;
    }
}
