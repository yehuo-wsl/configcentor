package cm.yehuo.configcentor.curator;

import cm.yehuo.configcentor.refreshScope.RefreshScopePostProcessor;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @Description:
 * @Author: 夜火
 * @Created Date: 2020年06月04日
 */
//@Component
public class ServerCentorZK implements ApplicationContextAware {

    private static final String hostaddr = "127.0.0.1:2181";
    private CuratorFramework client;
    private BeanDefinitionRegistry beanDefinitionRegistry;
    private ConfigurableApplicationContext applicationContext;
    private static final String zkpropertySource = "zookeeperSource";
    private ConcurrentMap map = new ConcurrentHashMap();
    private final String path="/config";
    private final String scopename = "refreshs";

    @Value("${config.server.enable:false")
    private boolean enable;

    @PostConstruct
    public void init() throws Exception {
        if (!enable) return;
        beanDefinitionRegistry = ((RefreshScopePostProcessor)this.applicationContext.getBean("refreshScopePostProcessor")).getRegistry();
        //创建zk的curator客户端
        client = CuratorFrameworkFactory
                .builder()
                .connectString(hostaddr)
                .retryPolicy(new ExponentialBackoffRetry(1000,3))
                .connectionTimeoutMs(5000)
                .build();
        client.start();
        Stat stat = client.checkExists().forPath(path);
        if (stat == null){//如果为null，创建此节点
            client.create().creatingParentContainersIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path,"create nodedata".getBytes());
        }else {
            apllyClientProperties(client,path);
        }
        nodeCacheListener(client,path);
    }

    private void nodeCacheListener(CuratorFramework client, String path) throws Exception {

        PathChildrenCache pathChildrenCache = new PathChildrenCache(client, path, false);
        pathChildrenCache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
        pathChildrenCache.getListenable().addListener(new PathChildrenCacheListener() {
            @Override
            public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                switch (event.getType()){
                    case CHILD_ADDED://子节点增加
                        addProperty(client,event.getData());
                        break;
                    case CHILD_UPDATED:
                        addProperty(client,event.getData());
                        break;
                    case CHILD_REMOVED:
                        delProperty(client,event.getData());
                        break;
                }
                refreshBean();
            }
        });

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

    private void addProperty(CuratorFramework client, ChildData childData){
        String paths = childData.getPath();
        String data = null;
        try {
            data = new String(client.getData().forPath(paths));
        }catch (Exception e){
            e.printStackTrace();
        }
        OriginTrackedMapPropertySource propertySource = (OriginTrackedMapPropertySource)this.applicationContext.getEnvironment().getPropertySources().get(zkpropertySource);
        if (propertySource != null){
            ConcurrentHashMap map = (ConcurrentHashMap)propertySource.getSource();
            map.put(paths.substring(path.length()+1), data);
        }
    }

    private void delProperty(CuratorFramework client, ChildData childData){
        String paths = childData.getPath();
        OriginTrackedMapPropertySource propertySource = (OriginTrackedMapPropertySource)this.applicationContext.getEnvironment().getPropertySources().get(zkpropertySource);
        if (propertySource != null){
            ConcurrentHashMap map = (ConcurrentHashMap)propertySource.getSource();
            map.remove(paths);
        }
    }




    /**
     * 从zk拉取最新的配置信息到本地
     * @param client
     * @param path
     */
    private void apllyClientProperties(CuratorFramework client, String path) throws Exception {
        if (!checkLocalProperties()){
            createLocalZkSourceProperties();
        }
        MutablePropertySources propertySources = this.applicationContext.getEnvironment().getPropertySources();
        PropertySource<?> propertySource = propertySources.get(zkpropertySource);
        ConcurrentHashMap map = (ConcurrentHashMap)propertySource.getSource();
        List<String> children = client.getChildren().forPath(path);
        for (String sourcename:children){
            map.put(sourcename,client.getData().forPath(path+"/"+sourcename));
        }

    }

    //初始化创建配置数据
    private void createLocalZkSourceProperties() {
        MutablePropertySources propertySources = this.applicationContext.getEnvironment().getPropertySources();
        OriginTrackedMapPropertySource originTrackedMapPropertySource = new OriginTrackedMapPropertySource(zkpropertySource, map);
        propertySources.addLast(originTrackedMapPropertySource);
    }

    //检查本地环境是否已经加载了配置数据
    private boolean checkLocalProperties() {
        MutablePropertySources propertySources = this.applicationContext.getEnvironment().getPropertySources();
        for (PropertySource propertySource : propertySources){
            if (propertySource.getName().equals(zkpropertySource)){
                return true;
            }
        }
        return false;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }
}