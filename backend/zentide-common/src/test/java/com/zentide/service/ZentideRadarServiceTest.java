package com.zentide.service;

import com.zentide.entity.po.ZentideRadar;
import com.zentide.entity.po.ZentideRadarTopic;
import com.zentide.entity.po.ZentideTopic;
import com.zentide.entity.dto.ZentideRadarCreateRequest;
import com.zentide.exception.BusinessException;
import com.zentide.mapper.ZentideRadarMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ZentideRadarServiceTest {
    @Test
    void createsRadarAndDeduplicatesTopics() {
        ZentideRadarMapper mapper = mock(ZentideRadarMapper.class);
        when(mapper.insertRadar(any())).thenAnswer(invocation -> {
            ZentideRadar radar = invocation.getArgument(0);
            radar.setRadarId(9L);
            return 1;
        });
        when(mapper.insertTopic(any())).thenAnswer(invocation -> {
            ZentideTopic topic = invocation.getArgument(0);
            topic.setTopicId(topic.getCanonicalName().equals("AI") ? 2L : 3L);
            return 1;
        });
        when(mapper.listTopics(9L)).thenReturn(List.of(topic("AI"), topic("Open Source")));

        ZentideRadar result = new ZentideRadarService(mapper)
                .create("u1", request("技术雷达", "[\"AI\",\"AI\",\"Open Source\"]"));

        assertEquals(9L, result.getRadarId());
        assertEquals(2, result.getTopics().size());
        assertEquals("HIGH", result.getAttentionLevel());
        assertEquals("IMPORTANT_ONLY", result.getNotificationStrategy());
        verify(mapper, times(2)).insertTopic(any());
        verify(mapper, times(2)).attachTopic(any());
        verify(mapper, times(2)).ensureTopicFollow(eq("u1"), anyLong());
    }

    @Test
    void rejectsRadarWithoutTopic() {
        ZentideRadarMapper mapper = mock(ZentideRadarMapper.class);

        assertThrows(BusinessException.class,
                () -> new ZentideRadarService(mapper).create("u1", request("空雷达", "[]")));
        verifyNoInteractions(mapper);
    }

    @Test
    void replacesOwnedRadarPlanTransactionally() {
        ZentideRadarMapper mapper = mock(ZentideRadarMapper.class);
        ZentideRadar owned = new ZentideRadar();
        owned.setRadarId(9L);
        when(mapper.findOwned("u1", 9L)).thenReturn(owned);
        when(mapper.updateRadar(any())).thenReturn(1);
        when(mapper.insertTopic(any())).thenAnswer(invocation -> {
            ZentideTopic topic = invocation.getArgument(0);
            topic.setTopicId(2L);
            return 1;
        });
        when(mapper.listTopics(9L)).thenReturn(List.of(topic("Spring Boot")));

        ZentideRadar result = new ZentideRadarService(mapper).update("u1", 9L,
                request("后端雷达", "[\"Spring Boot\"]"));

        assertEquals("后端雷达", result.getName());
        assertEquals(1, result.getTopics().size());
        verify(mapper).deleteTopics(9L);
        verify(mapper).attachTopic(any());
    }

    @Test
    void addsPublicTopicToOwnedRadar() {
        ZentideRadarMapper mapper = mock(ZentideRadarMapper.class);
        ZentideRadar radar = new ZentideRadar();
        radar.setRadarId(9L);
        radar.setStatus("ACTIVE");
        ZentideTopic publicTopic = new ZentideTopic();
        publicTopic.setTopicId(4L);
        publicTopic.setCanonicalName("Spring Boot");
        when(mapper.findOwned("u1", 9L)).thenReturn(radar);
        when(mapper.findActiveTopic(4L)).thenReturn(publicTopic);
        when(mapper.touchRadar("u1", 9L)).thenReturn(1);
        when(mapper.listTopics(9L)).thenReturn(List.of(topic("Spring Boot")));

        ZentideRadar result = new ZentideRadarService(mapper).addTopic("u1", 9L, 4L);

        assertEquals(1, result.getTopics().size());
        verify(mapper).attachTopic(argThat(link -> link.getRadarId().equals(9L) && link.getTopicId().equals(4L)));
        verify(mapper).ensureTopicFollow("u1", 4L);
        verify(mapper).touchRadar("u1", 9L);
    }

    private ZentideRadarCreateRequest request(String name, String topics) {
        return new ZentideRadarCreateRequest(name, topics, "HIGH", "IMPORTANT_ONLY",
                "[\"RELEASE\",\"SECURITY\"]", "[\"OFFICIAL\",\"GITHUB\"]", "[]", "Java 后端项目");
    }

    private ZentideRadarTopic topic(String name) {
        ZentideRadarTopic topic = new ZentideRadarTopic();
        topic.setCanonicalName(name);
        return topic;
    }
}
