package cm.yehuo.configcentor.redis;

import cm.yehuo.configcentor.refreshScope.RefreshScopePostProcessor;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @Description:
 * @Author: 夜火
 * @Created Date: 2020年06月04日
 */
@Component
public class ServerCentorRedis implements ApplicationContextAware {

    private BeanDefinitionRegistry beanDefinitionRegistry;
    private ConfigurableApplicationContext applicationContext;
    private static final String configsource = "configsource";
    private static final ConcurrentMap map = new ConcurrentHashMap();
    private final String path="config";
    private final String scopename = "refreshs";

    @Value("#{new Boolean(${config.server.enable:false})}")
    private boolean enable;

    @Autowired
    private RedisTemplate redisTemplate;

    static {
        map.put("isExit", true);
    }


    @PostConstruct
    public void init() throws Exception {
        if (!enable) return;
        beanDefinitionRegistry = ((RefreshScopePostProcessor)this.applicationContext.getBean("refreshScopePostProcessor")).getRegistry();
        if (redisTemplate.hasKey(path)){
            apllyClientProperties();
        }else {
            redisTemplate.opsForHash().putAll(path,map);
            createLocalZkSourceProperties();
        }
    }


    private void refreshBean(){
        String[] beanDefinitionNames = beanDefinitionRegistry.getBeanDefinitionNames();
        for (String beanDefinitionName : beanDefinitionNames){
            BeanDefinition beanDefinition = beanDefinitionRegistry.getBeanDefinition(beanDefinitionName);
            if (beanDefinition.getScope().equals(scopename)){
                this.applicationContext.getBeanFactory().destroyScopedBean(beanDefinitionName);
                //this.applicationContext.getBean(beanDefinitionName);
            }
        }
    }

    private void addProperty(Object obj){
        ConcurrentHashMap newMap = null;
        if (obj instanceof Map){
            newMap = (ConcurrentHashMap)obj;
        }
        OriginTrackedMapPropertySource propertySource = (OriginTrackedMapPropertySource)this.applicationContext.getEnvironment().getPropertySources().get(configsource);
        if (propertySource != null){
            ConcurrentHashMap map = (ConcurrentHashMap)propertySource.getSource();
            map.putAll(newMap);
        }
    }

    private void delProperty(Object obj){
        ConcurrentHashMap newMap = null;
        if (obj instanceof Map){
            newMap = (ConcurrentHashMap)obj;
        }
        OriginTrackedMapPropertySource propertySource = (OriginTrackedMapPropertySource)this.applicationContext.getEnvironment().getPropertySources().get(configsource);
        if (propertySource != null){
            ConcurrentHashMap map = (ConcurrentHashMap)propertySource.getSource();
            ConcurrentHashMap.KeySetView keySetView = newMap.keySet();
            keySetView.forEach(key->{
                map.remove(key);
            });
        }
    }




    /**
     * 从redis拉取最新的配置信息到本地
     */
    private void apllyClientProperties() throws Exception {
        if (!checkLocalProperties()){
            createLocalZkSourceProperties();
        }
        MutablePropertySources propertySources = this.applicationContext.getEnvironment().getPropertySources();
        PropertySource<?> propertySource = propertySources.get(configsource);
        ConcurrentHashMap map = (ConcurrentHashMap)propertySource.getSource();
        Map entries = redisTemplate.opsForHash().entries(path);
        map.putAll(entries);
    }

    //初始化创建配置数据
    private void createLocalZkSourceProperties() {
        MutablePropertySources propertySources = this.applicationContext.getEnvironment().getPropertySources();
        OriginTrackedMapPropertySource originTrackedMapPropertySource = new OriginTrackedMapPropertySource(configsource, map);
        propertySources.addLast(originTrackedMapPropertySource);
    }

    //检查本地环境是否已经加载了配置数据
    private boolean checkLocalProperties() {
        MutablePropertySources propertySources = this.applicationContext.getEnvironment().getPropertySources();
        for (PropertySource propertySource : propertySources){
            if (propertySource.getName().equals(configsource)){
                return true;
            }
        }
        return false;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }

    public void updataLocalProperties(String channelName,Object obj) {
        MutablePropertySources propertySources = this.applicationContext.getEnvironment().getPropertySources();
        PropertySource<?> propertySource = propertySources.get(configsource);
        ConcurrentHashMap oldmap = (ConcurrentHashMap)propertySource.getSource();
        Channel channel = Channel.valueOf(channelName);
        switch (channel){
            case ChannelAdd:
                addProperty(obj);
                break;
            case ChannelDel:
                delProperty(obj);
                break;
            case ChannelUpdate:
                addProperty(obj);
                break;
        }
        refreshBean();
    }
}