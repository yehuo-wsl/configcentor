package cm.yehuo.configcentor.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @Description:
 * @Author: 夜火
 * @Created Date: 2020年06月05日
 */

public class RedisListenerContainer extends RedisMessageListenerContainer {

    /**
     * @param connectionFactory The connectionFactory to set.
     */
    /*@Override
    public void setConnectionFactory(RedisConnectionFactory connectionFactory) {
        super.setConnectionFactory(connectionFactory);
    }*/
}