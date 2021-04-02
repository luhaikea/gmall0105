package com.atguigu.gmall.interceptors;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.HttpclientUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import java.util.Map;

//拦截器是基于每个springboot的模块
@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 拦截代码
        // 判断被拦截的请求的访问的方法的注解(是否是需要拦截的)
        //这里用到的是反射

        HandlerMethod hm = null;
        //获取执行方法上的注解
        if(handler instanceof  HandlerMethod){
            hm = (HandlerMethod) handler;
        }else{
            return true;
        }


        LoginRequired methodAnnotation = hm.getMethodAnnotation(LoginRequired.class);

        StringBuffer url = request.getRequestURL();

        // 是否拦截
        if (methodAnnotation == null) {
            //也就是这个方法上没有@LoginRequired这个注解
            return true;
        }

        String token = "";

        String oldToken = CookieUtil.getCookieValue(request, "oldToken", true);
        if (StringUtils.isNotBlank(oldToken)) {
            token = oldToken;
        }

        String newToken = request.getParameter("token");
        if (StringUtils.isNotBlank(newToken)) {
            token = newToken;
        }
/*  老token：浏览器中的token         新token：url中的token
                          老token空                      老token不空

    新token空     【从未登录过，是直接被浏览器拦截的】      【之前登录过，但访问每个其他的需要被拦截的页面都会进入这个分支】

    新token不空   【刚刚登录过，也就是直接点击登录按钮的情况】【过期 也就是浏览器中有一个token但老token验证不过[验证不过即是过期]，又被踢回到认证中心，所以由携带了新token】

*/

        // 是否必须登录  也就是的到注解的值
        boolean loginSuccess = methodAnnotation.loginSuccess();// 获得该请求是否必登录成功

        // 调用认证中心进行验证
        String success = "fail";
        Map<String,String> successMap = new HashMap<>();

        if(StringUtils.isNotBlank(token)){
            String ip = request.getHeader("x-forwarded-for");// 通过nginx转发的客户端ip
            if(StringUtils.isBlank(ip)){
                ip = request.getRemoteAddr();// 从request中获取ip
                if(StringUtils.isBlank(ip)){
                    ip = "127.0.0.1";
                }
            }
            String successJson = HttpclientUtil.doGet("http://passport.gmall.com:8085/verify?token=" + token+"&currentIp="+ip);
            successMap = JSON.parseObject(successJson,Map.class);
            success = successMap.get("status");

        }

        if (loginSuccess) {
            // 必须登录成功才能使用
            if (!success.equals("success")) {
                //重定向会passport登录
                StringBuffer requestURL = request.getRequestURL();
                response.sendRedirect("http://passport.gmall.com:8085/index?ReturnUrl="+requestURL);
                return false;
            }

            // 需要将token携带的用户信息写入
            request.setAttribute("memberId", successMap.get("memberId"));
            request.setAttribute("nickname", successMap.get("nickname"));
            //验证通过，覆盖cookie中的token
            if(StringUtils.isNotBlank(token)){
                CookieUtil.setCookie(request,response,"oldToken",token,60*60*2,true);
            }

        } else {
            // 没有登录也能用，但是必须验证
            //也就是需要拦截但是拦截校验失败（用户没有登录或者登录过期）也可以继续访问方法，比如购物车中的所有方法
            //购物车需要根据是否登录来判定来走那条分支
            //购物车是需要登录的 因为要存储购物车中的信息 但为了方便用户 可以不登录 只是使用cookie来存储购物车信息
            if (success.equals("success")) {
                // 需要将token携带的用户信息写入
                request.setAttribute("memberId", successMap.get("memberId"));
                request.setAttribute("nickname", successMap.get("nickname"));

                //验证通过，覆盖cookie中的token
                if(StringUtils.isNotBlank(token)){
                    CookieUtil.setCookie(request,response,"oldToken",token,60*60*2,true);
                }

            }
        }


        return true;
    }
}
