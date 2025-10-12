package com.beyt.jdq.resolver;

import com.beyt.jdq.dto.Criteria;
import com.beyt.jdq.dto.CriteriaList;
import com.beyt.jdq.dto.enums.CriteriaOperator;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Role;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Arrays;

@Component
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class CriteriaListArgumentResolver implements HandlerMethodArgumentResolver {
    private static final String KEY_FIELD_START = "key";
    private static final String OPERATION_FIELD_START = "operation";
    private static final String VALUES_FIELD_START = "values";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return CriteriaList.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        CriteriaList filter = new CriteriaList();
        for (int i = 0; ; i++) {
            String keyField = KEY_FIELD_START + i;
            String operationField = OPERATION_FIELD_START + i;
            String valuesField = VALUES_FIELD_START + i;
            String key = webRequest.getParameter(keyField);
            String operation = webRequest.getParameter(operationField);
            String values = webRequest.getParameter(valuesField);
            if (key != null && operation != null && values != null) {
                Criteria criteria = new Criteria(key, CriteriaOperator.valueOf(operation), null);
                criteria.setValues(Arrays.asList(values));
                filter.add(criteria);
            } else {
                break;
            }
        }
        return filter;
    }
}
