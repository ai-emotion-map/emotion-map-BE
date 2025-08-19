package com.emomap.emomap;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnableAutoConfiguration(exclude = {RedisAutoConfiguration.class})
class EmomapApplicationTests {

    @Test
    void contextLoads() {
    }

}
