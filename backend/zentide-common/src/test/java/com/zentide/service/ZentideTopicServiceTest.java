package com.zentide.service;

import com.zentide.entity.po.ZentideTopic;
import com.zentide.mapper.ZentideTopicMapper;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ZentideTopicServiceTest {
    @Test
    void createsAndFollowsANewTopic() {
        ZentideTopicMapper mapper = mock(ZentideTopicMapper.class);
        doAnswer(invocation -> {
            invocation.<ZentideTopic>getArgument(0).setTopicId(41L);
            return 1;
        }).when(mapper).insertTopic(any());

        ZentideTopic topic = new ZentideTopicService(mapper).createAndFollow("u1", "独立游戏", "GAME");

        org.junit.jupiter.api.Assertions.assertEquals(41L, topic.getTopicId());
        org.junit.jupiter.api.Assertions.assertEquals("独立游戏", topic.getCanonicalName());
        org.junit.jupiter.api.Assertions.assertTrue(topic.getFollowed());
        verify(mapper).follow(41L, "u1", "DAILY");
    }
}
