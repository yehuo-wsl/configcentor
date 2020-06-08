package cm.yehuo.configcentor.controller;

import cm.yehuo.configcentor.redis.Channel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description:
 * @Author: 夜火
 * @Created Date: 2020年06月04日
 */
@Scope("refreshs")
@RestController
public class ConfigCentorController {

    @Value("${name:pre}")
    private String name;

    @Autowired
    private RedisTemplate redisTemplate;

    @Qualifier("configServiceImpl")
    @Autowired
    private ConfigService configService;

    @RequestMapping("/name")
    public String getName(){
        System.out.println(name);
        System.out.println(configService);
        configService.test();
        return name;
    }

    @RequestMapping("/convert")
    public String convert(){
        Map<String, String> map = new ConcurrentHashMap<>();
        map.put("pwd", "123456");
        map.put("class", "classname");
        map.put("name", "hahahah");
        System.out.println(configService);
        configService.test();
        redisTemplate.convertAndSend(Channel.ChannelAdd.name(), map);
        return name;
    }

}