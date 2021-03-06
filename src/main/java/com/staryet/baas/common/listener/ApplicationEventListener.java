package com.staryet.baas.common.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.stereotype.Component;

/**
 * 监听系统启动成功
 * Created by Codi on 15/10/30.
 */
@Component
public class ApplicationEventListener implements ApplicationListener<ApplicationReadyEvent> {

    public static boolean error;
    private static boolean ready;
    private Log log = LogFactory.getLog(getClass());

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;
    @Autowired
    private MongoTemplate mongoTemplate;


    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        ready = true;
        //check out database health
        try {
            RedisConnection connection = RedisConnectionUtils.getConnection(this.redisConnectionFactory);
            try {
                connection.info();
            } catch (Exception e) {
                error = true;
                log.error(e, e);
            } finally {
                RedisConnectionUtils.releaseConnection(connection, this.redisConnectionFactory);
            }
        } catch (Exception e) {
            error = true;
            log.error(e, e);
        }
        try {
            this.mongoTemplate.executeCommand("{ buildInfo: 1 }");
        } catch (Exception e) {
            error = true;
            log.error(e, e);
        }
        if (!error) {
            log.info("JavaBaasServer started.");
        } else {
            log.error("JavaBaasServer failed to start!");
        }
    }

    public static boolean isReady() {
        return ready;
    }
}
