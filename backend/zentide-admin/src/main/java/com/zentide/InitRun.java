package com.zentide;

import com.zentide.mapper.ZentideSystemMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component("initRun")
public class InitRun implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(InitRun.class);

    @Resource
    private ZentideSystemMapper systemMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (systemMapper.ping() != 1) {
                throw new IllegalStateException("MyBatis database health check failed");
            }
            redisTemplate.getConnectionFactory().getConnection().isClosed();
            logger.info("Admin服务启动成功，可以开始愉快的开发了");
        } catch (Exception e) {
            logger.error("服务启动失败", e);
        }
    }
}
