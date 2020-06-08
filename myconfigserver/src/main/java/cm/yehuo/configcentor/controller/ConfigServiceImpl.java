package cm.yehuo.configcentor.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * @Description:
 * @Author: 夜火
 * @Created Date: 2020年06月08日
 */
@Service
@Scope("refreshs")
public class ConfigServiceImpl implements ConfigService {

    @Value("${name:first}")
    private String name;

    @Override
    public void test() {
        System.out.println("ConfigServiceImpl-first:  "+name);
    }
}