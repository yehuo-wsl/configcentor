package cm.yehuo.configcentor.refreshScope;
import java.util.Iterator;
import java.util.Set;
import	java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.http.HttpSession;
import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * @Description:
 * @Author: 夜火
 * @Created Date: 2020年06月04日
 */
public class RefreshScope implements Scope {
    private Map<String,Object> cache = new ConcurrentHashMap<String, Object> ();


    @Override
    public Object get(String name, ObjectFactory<?> objectFactory) {
        Object obj = cache.get(name);
        if (obj == null){
            synchronized (name) {
                if (obj == null){
                    Object object = objectFactory.getObject();
                    cache.put(name,object);
                    return object;
                }
            }
        }
        return obj;
    }

    @Override
    public Object remove(String name) {
        Object obj = cache.get(name);
        this.cache.remove(name);
        return obj;
    }


    @Override
    public void registerDestructionCallback(String name, Runnable callback) {}


    @Override
    public Object resolveContextualObject(String key) {
        return null;
    }


    @Override
    public String getConversationId() {
        return null;
    }
}