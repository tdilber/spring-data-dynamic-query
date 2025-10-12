package com.beyt.jdq.interceptor;

import com.beyt.jdq.context.DBSelectionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

public class DatabaseSelectionInterceptor implements HandlerInterceptor {

    @Autowired
    private DBSelectionContext dbSelectionContext;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        if (getMapping != null) {
            dbSelectionContext.setDatabase(DBSelectionContext.Database.READ);
        }

        return true;
    }
}
