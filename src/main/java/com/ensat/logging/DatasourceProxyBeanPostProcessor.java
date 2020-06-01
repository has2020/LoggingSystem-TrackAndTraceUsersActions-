package com.ensat.logging;

import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.listener.logging.CommonsLogLevel;
import net.ttddyy.dsproxy.listener.logging.CommonsQueryLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.List;


@Component
public class DatasourceProxyBeanPostProcessor implements BeanPostProcessor {


    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof DataSource && !(bean instanceof ProxyDataSource)) {
            final ProxyFactory factory = new ProxyFactory(bean);
            factory.setProxyTargetClass(true);
            factory.addAdvice(new ProxyDataSourceInterceptor((DataSource) bean));
            return factory.getProxy();
        }
        return bean;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    private static class ProxyDataSourceInterceptor implements MethodInterceptor {
        private final DataSource dataSource;
        public ProxyDataSourceInterceptor(final DataSource dataSource) {
            //Initialize listener
            CommonsQueryLoggingListener listener = new CommonsQueryLoggingListener();
            listener.setLogLevel(CommonsLogLevel.INFO);
            this.dataSource = ProxyDataSourceBuilder.create(dataSource)
                    .name("datasource-proxy")
                    .listener(new QueryExecutionListener() {
                        @Override
                        public void beforeQuery(ExecutionInfo info, List<QueryInfo> queryInfos) {
                            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                            if (auth==null){
                                listener.setLog("Indeterminate");
                            }
                            else {
                                listener.setLog(auth.getName());
                            }
                        }
                        @Override
                        public void afterQuery(ExecutionInfo info, List<QueryInfo> queryInfos) {

                        }
                    }).listener(listener).multiline().build();
        }

        @Override
        public Object invoke(final MethodInvocation invocation) throws Throwable {
            final Method proxyMethod = ReflectionUtils.findMethod(this.dataSource.getClass(),
                    invocation.getMethod().getName());
            if (proxyMethod != null) {
                return proxyMethod.invoke(this.dataSource, invocation.getArguments());
            }
            return invocation.proceed();
        }
    }
}
