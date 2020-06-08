package cm.yehuo.configcentor.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description:
 * @Author: 夜火
 * @Created Date: 2020年06月05日
 */
@Component
public class RedisListenerMessageListener implements MessageListener {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    ServerCentorRedis serverCentorRedis;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        byte[] body = message.getBody();
        byte[] channel = message.getChannel();
        Object obj = redisTemplate.getHashValueSerializer().deserialize(body);
        String channelname = redisTemplate.getHashKeySerializer().deserialize(channel).toString();
        if (obj!=null){
            serverCentorRedis.updataLocalProperties(channelname,obj);
        }
    }
}