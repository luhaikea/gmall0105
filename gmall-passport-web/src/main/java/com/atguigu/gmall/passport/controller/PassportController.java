package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.util.HttpclientUtil;
import com.atguigu.gmall.util.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/*
单点登录可以跨顶级域名认证 所以就不能把登录信息放到session中 session不能跨顶级域名使用
把用户认证独立处理不仅gmall中的模块可以使用，其他网站也可以使用

                        三代单点登录的算法
第一代：session共享：【这是以前解决单点登录的方案   要解决分布式、跨域】
    每个模块就是每个spring boot中都有一个session，而每个模块中的session是不共享的，就是从一个模块跳到另一个模块session
    中的数据就会丢失，但是为了各个模块之间session可以共享，就需要配一个session池，一般用redis,也就是各个模块用的都是session池
    中的session
    如何判断用户登录：每个浏览器都有一个jsessionid，当用户通过浏览器对一个模块发起请求是，就用浏览器的jsessinid去session
    池中找，如果找到并且从中取出user数据，就说明用户是登录的
    缺点：jseesion可能被恶意获取，进而访问系统
第二代：现在的做法：直接用redis
    Token+redis
    用户在认证中心登录之后，认证中心会颁发给用户一个token，这个token将来会写在浏览器里面，同时也会在redis里面写一份，将来
    在用户在使用其他模块时就会用浏览器里的token去和redis中的token比较，如果token一致就认为用户登录，这么做就会导致用户的每一个
    点击都会去redis中做一次比较，显然是不行的，
第三代：用jwt[json web token],专门做token，对token会做一个加密，加密算法是自定义的，
    做法：在用户登录时会直接把jwt加密的token写入浏览器，并且在其中加入一些用户信息的要素，比如userId，将来用户在访问任何一个功
    能模块的时候，拿着token去认证中心去解密，就会的到userId，就会证明当前的浏览器是一个合法的浏览器并且携带的userID是一个真实的
    userId，而redis就可以用来存储用户信息和用户过期时间，当在redis中取不到数据时就说明用户过期了，需要重新登录
    第三代的缺点是对加密算法要求极高


1、点击登录按钮，在登录页面输入用户名和密码通过用户名和密码验证用户登录是否成功后，生成jwt的token返还给search.gmall.com/index,
   在拦截器里面将返回的token写入cookie 【不由认证中心写入主要原因是因为第三方登录，如果由认证中心写入，那cookie就在认证中心
   的域名下，业务模块还是没有对应的token，认证中心只负责颁发token，token的处理由具体模块负责】

   在search.gmall.com/index页面点击登录【 <a href="http://passport.gmall.com:8085/index?ReturnUrl=http://search.gmall.com:8083/
   index">你好，请登录</a> 】到达PassportController的index方法，会到达登录页面并携带回跳url【map.put("ReturnUrl", ReturnUrl);】，登录页面会
   携带用户名和密码到达PassportController的login方法，这个方法会调用用户服务验证用户名和密码是否正确，如果不正确会返回错误提示【if(token=="fail")
   {alert("用户名或者密码错误");}】，如果正确会生成对应的token返回给登录页面由登录页面跳转到回跳URL【 window.location.href=$("#ReturnUrl").v
   al()+"?token="+token;】。这个URL会被拦截器拦截并由拦截器将携带的token写入浏览器
2、被动登录【被拦截器拦截】

*/
//将认证中心写成一个controler是为了以后其他网站的调用

@Controller
public class PassportController {

    @Reference
    UserService userService;

    //http://passport.gmall.com:8085/vlogin?code=0b8e7513b05e82b8f14fec1fb8fab2ad
    //当用户授权你的应用后，开放平台会回调你填写的这个地址。
    @RequestMapping("vlogin")
    public String vlogin(String code, HttpServletRequest request){

        //每次的重新登录都会生成新的code, access_token

        System.out.println("code:"+code);
        //授权码换取access_token
        String s3 = "https://api.weibo.com/oauth2/access_token";

        Map<String, String> map = new HashMap();
        map.put("client_id", "87510233");
        map.put("client_secret", "5a789c350d00b1f3a3d454d2c3159aa2");
        map.put("grant_type", "authorization_code");
        map.put("redirect_uri", "http://passport.gmall.com:8085/vlogin");
        map.put("code", code);

        String accessTokenStr = HttpclientUtil.doPost(s3, map);
        Map<String, String> accessTokenMap = JSON.parseObject(accessTokenStr,Map.class);
        String accessToken = (String) accessTokenMap.get("access_token");

        String uid = (String) accessTokenMap.get("uid");


        //access_token换取用户信息
        String showUserUrl = "https://api.weibo.com/2/users/show.json?access_token="+accessToken+"&uid="+uid;
        String userJson = HttpclientUtil.doGet(showUserUrl);
        Map<String, Object> userJsonMap = JSON.parseObject(userJson, Map.class);

        //将用户信息保存数据库，用户类型设置为微博用户
        UmsMember umsMember = new UmsMember();
        umsMember.setSourceType("2");
        umsMember.setAccessCode(code);
        umsMember.setAccessToken(accessToken);
        umsMember.setSourceUid((String)userJsonMap.get("idstr"));
        umsMember.setUsername((String)userJsonMap.get("screen_name"));
        umsMember.setNickname((String)userJsonMap.get("name"));

        UmsMember umsCheck = new UmsMember();
        umsCheck.setSourceUid(umsMember.getSourceUid());
        UmsMember umsCheckRes = userService.checkOauthUser(umsCheck);

        UmsMember umsMemberSave = null;
        if(umsCheckRes == null){
            umsMemberSave = userService.addOauthUser(umsMember);
        } else {
            umsMemberSave = umsCheckRes;
        }

        //生成token,并且重定向到首页，携带该token
        String token = "";

        if(umsMemberSave!=null){
            //登录成功

            //用jwt制作token
            String memberId = umsMemberSave.getId();
            String nickname = umsMemberSave.getNickname();
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("memberId", memberId);
            userMap.put("nickname", nickname);

            //获得IP  这个方法获得的是直接发送请求的服务器地址，如果有负载均衡的话得到的就是负载均衡服务器的IP
            //String remoteAddr = request.getRemoteAddr();

            String ip = request.getHeader("x-forwarded-for");//通过nginx代理转发的客户端ip
            if(StringUtils.isBlank(ip)){
                ip = request.getRemoteAddr();
                if(StringUtils.isBlank(ip)){
                    ip = "127.0.0.1";
                }
            }
            //需要对参数加密后生成token
            token = JwtUtil.encode("2019gmall110105", userMap, ip);

            //将token存入redis一份  由于cookie中的token的过期时间可能被篡改
            userService.addUserToken(token, memberId);

        } else{
            //登录失败
            token = "fail";
        }

        return "redirect:http://search.gmall.com:8083/index?token="+token;
    }



    @RequestMapping("verify")
    @ResponseBody
    /*
       注意这里必须使用currentIp不能用request的IP，这里的流程是浏览器访问某个业务，被这个业务的拦截器拦截
       ，再由这个业务的拦截器对verify方法发送http请求，所以这个request的IP是业务的IP，不是浏览器的IP,这里需要浏览器的IP
       【这里可以通过测试得出】

    */
    public String verify(String token, String currentIp){
        //通过jwt校验token真假
        Map<String, String> map = new HashMap<>();

        Map<String, Object> decode = JwtUtil.decode(token, "2019gmall110105", currentIp);
        if(decode!=null){
            map.put("status", "success");
            map.put("memberId", (String) decode.get("memberId"));
            map.put("nickname", (String) decode.get("nickname"));

        } else{
            map.put("status", "fail");

        }

        return JSON.toJSONString(map);
    }


    //对应第一种情况
    @RequestMapping("login")
    @ResponseBody
    public String login(UmsMember umsMember, HttpServletRequest request){

        String token = "";
        //调用用户服务验证用户名和密码
        UmsMember umsMemberLogin= userService.login(umsMember);
        System.out.println("PassportCController中的login方法："+umsMemberLogin.toString());
        if(umsMemberLogin!=null){
            //登录成功

            //用jwt制作token
            String memberId = umsMemberLogin.getId();
            String nickname = umsMemberLogin.getNickname();
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("memberId", memberId);
            userMap.put("nickname", nickname);

            //获得IP  这个方法获得的是直接发送请求的服务器地址，如果有负载均衡的话得到的就是负载均衡服务器的IP
            //String remoteAddr = request.getRemoteAddr();

            String ip = request.getHeader("x-forwarded-for");//通过nginx代理转发的客户端ip
            if(StringUtils.isBlank(ip)){
                ip = request.getRemoteAddr();
                if(StringUtils.isBlank(ip)){
                    ip = "127.0.0.1";
                }
            }
            //需要对参数加密后生成token
            token = JwtUtil.encode("2019gmall110105", userMap, ip);

            //将token存入redis一份  由于cookie中的token的过期时间可能被篡改
            userService.addUserToken(token, memberId);

        } else{
            //登录失败
            token = "fail";
        }
        System.out.println("PassportCController中的login方法token："+token);
        return token;
    }

    @RequestMapping("index")
    @LoginRequired(loginSuccess = false)
    public String index(String ReturnUrl, ModelMap map){

        map.put("ReturnUrl", ReturnUrl);
        return "index";
    }

}
