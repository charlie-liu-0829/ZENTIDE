package com.zentide.service;

import com.zentide.mapper.ZentideGovernanceMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 治理控制面只验证数据转换和委托，不连接真实数据库。 */
class ZentideGovernanceControlPlaneServiceTest {
    @Test
    void rulesDecodeKeywordsJsonForPythonAgent() {
        ZentideGovernanceMapper mapper = mock(ZentideGovernanceMapper.class);
        when(mapper.listRules(5L)).thenReturn(List.of(Map.of(
                "rule_id", 9, "scene_id", 5, "violation_type", "spam",
                "keywords_json", "[\"广告\",\"刷屏\"]", "severity", "medium", "enabled", 1)));

        List<Map<String, Object>> rules = new ZentideGovernanceControlPlaneService(mapper).rules(5L);

        assertEquals(List.of("广告", "刷屏"), rules.get(0).get("keywords"));
        verify(mapper).listRules(5L);
    }

    @Test
    void savesResultAsJsonInTheMainDatabase() {
        ZentideGovernanceMapper mapper = mock(ZentideGovernanceMapper.class);
        Map<String, Object> payload = Map.of(
                "result_id", "gov_123", "content_id", 18, "scene_id", 5,
                "risk_level", "high", "suggested_action", "temporary_hide");

        new ZentideGovernanceControlPlaneService(mapper).saveResult(payload);

        verify(mapper).insertResult(eq("gov_123"), eq(18L), eq(5L), eq("high"), eq("temporary_hide"), org.mockito.ArgumentMatchers.any(), anyString());
    }
}
