package com.zentide.service;

import com.zentide.entity.po.ZentideChange;
import com.zentide.entity.po.ZentideInsight;
import com.zentide.entity.po.ZentideRadarTopic;
import com.zentide.exception.BusinessException;
import com.zentide.mapper.ZentideInsightMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ZentideInsightServiceTest {
    @Test
    void generatesAnExplainableInsightOnlyWhenAVerifiedChangeMatchesARadarTopic() {
        ZentideInsightMapper mapper = mock(ZentideInsightMapper.class);
        ZentideRadarTopic topic = new ZentideRadarTopic();
        topic.setCanonicalName("Spring Boot");
        topic.setWeight(new BigDecimal("1.50"));
        ZentideChange change = new ZentideChange();
        change.setChangeId(9L);
        change.setTitle("Spring Boot 4.0 发布");
        change.setSummary("新版发布说明");
        change.setImportance("HIGH");
        when(mapper.listActiveTopics("u1")).thenReturn(List.of(topic));
        when(mapper.listVerifiedChanges(200)).thenReturn(List.of(change));
        when(mapper.listPublished("u1", 20)).thenReturn(List.of());

        new ZentideInsightService(mapper).refreshAndList("u1", null);

        ArgumentCaptor<ZentideInsight> captor = ArgumentCaptor.forClass(ZentideInsight.class);
        verify(mapper).upsertInsight(captor.capture());
        assertEquals(new BigDecimal("60.00"), captor.getValue().getRelevanceScore());
        org.junit.jupiter.api.Assertions.assertTrue(captor.getValue().getWhyJson().contains("Spring Boot"));
        org.junit.jupiter.api.Assertions.assertTrue(captor.getValue().getWhyJson().contains("VERIFIED"));
    }

    @Test
    void doesNotCreateInsightForAnUnmatchedTopic() {
        ZentideInsightMapper mapper = mock(ZentideInsightMapper.class);
        ZentideRadarTopic topic = new ZentideRadarTopic();
        topic.setCanonicalName("Kotlin");
        ZentideChange change = new ZentideChange();
        change.setChangeId(9L);
        change.setTitle("Spring Boot 4.0 发布");
        change.setImportance("HIGH");
        when(mapper.listActiveTopics("u1")).thenReturn(List.of(topic));
        when(mapper.listVerifiedChanges(200)).thenReturn(List.of(change));
        when(mapper.listPublished("u1", 20)).thenReturn(List.of());

        new ZentideInsightService(mapper).refreshAndList("u1", null);

        verify(mapper, never()).upsertInsight(any());
    }

    @Test
    void irrelevantFeedbackDismissesOnlyThePersonalInsight() {
        ZentideInsightMapper mapper = mock(ZentideInsightMapper.class);
        when(mapper.findOwned("u1", 7L)).thenReturn(new ZentideInsight());

        new ZentideInsightService(mapper).feedback("u1", 7L, "irrelevant", null);

        verify(mapper).upsertFeedback("u1", 7L, "IRRELEVANT", null);
        verify(mapper).updateStatus("u1", 7L, "DISMISSED");
    }

    @Test
    void rejectsFeedbackForAnotherUsersInsight() {
        ZentideInsightMapper mapper = mock(ZentideInsightMapper.class);
        when(mapper.findOwned("u1", 7L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> new ZentideInsightService(mapper).feedback("u1", 7L, "USEFUL", null));
    }
}
