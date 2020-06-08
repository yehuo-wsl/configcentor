package cm.yehuo.configcentor.refreshScope;

import cm.yehuo.configcentor.refreshScope.RefreshScope;
import lombok.Data;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.stereotype.Component;

/**
 * @Description:
 * @Author: 夜火
 * @Created Date: 2020年06月04日
 */
@Component
@Data
public class RefreshScopePostProcessor implements BeanDefinitionRegistryPostProcessor {

    private BeanDefinitionRegistry registry;

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        this.registry = registry;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        beanFactory.registerScope("refreshs",new RefreshScope());
    }
}