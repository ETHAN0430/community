package com.cyf.community.controller.interceptor;

import com.cyf.community.entity.LoginTicket;
import com.cyf.community.entity.User;
import com.cyf.community.service.UserService;
import com.cyf.community.util.CookieUtil;
import com.cyf.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

@Component
public class LoginTicketInterceptor implements HandlerInterceptor {

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ticket = CookieUtil.getValue(request, "ticket");
        if (ticket != null) {
            LoginTicket loginTicket = userService.findLoginTicket(ticket);
            if (loginTicket != null && loginTicket.getStatus() == 0 && loginTicket.getExpired().after(new Date())) {
                User user = userService.findUserById(loginTicket.getUserId());
                // 本次请求持有用户 threadLocal
                hostHolder.setUser(user);
                // 构建用户认证的结果,并存入SecurityContext,以便于Security进行授权
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        user, user.getPassword(), userService.getAuthorities(user.getId()));

                SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
            }
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user = hostHolder.getUser();
        if (user != null && modelAndView != null) {
            modelAndView.addObject("loginUser", user);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        hostHolder.clean();
        // 此处会出现问题
        // 个人认为的原因是：点击私信后执行了afterCompletion中的SecurityContextHolder.clearContext() 已经被清除掉了
        //                 然后点具体通知经过filter时发现已经被清除掉了就会被拦截  而设置SecurityContextHolder是在之后的interceptor中
        //
        //和hostHolder不一样的点在于：
        //hostHolder的拦截 set clear方法都在interceptor中  而SecurityContextHolder只有set和clear方法在interceptor中 而拦截过程在interceptor之前的filter中
        //个人理解 可能说得不对哈 我感觉视频里的应该是有问题的但是竟然没有出bug 可能是以前的版本兼容性强？

        //SecurityContextHolder.clearContext();
    }
}
