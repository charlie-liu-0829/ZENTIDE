package com.zentide.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zentide.entity.vo.ZentideSearchHistoryItem;
import com.zentide.mapper.UserAccountMapper;
import com.zentide.mapper.ZentideInterestMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZentideSearchServiceTest {
    @Mock private ZentideInterestMapper interestMapper;
    @Mock private UserAccountMapper accountMapper;

    @Test
    void keepsFiveLatestEntriesAndDeduplicatesByTypeAndKeyword() throws Exception {
        when(accountMapper.findSearchHistory("u1")).thenReturn("""
                [{"query":"科技","type":"post"},{"query":"音乐","type":"hub"},
                 {"query":"游戏","type":"post"},{"query":"电影","type":"post"},
                 {"query":"旅行","type":"hub"}]
                """);
        when(interestMapper.searchPosts(anyString(), anyInt())).thenReturn(List.of());
        ZentideSearchService service = new ZentideSearchService(interestMapper, accountMapper, "", "posts", "hubs");

        service.search("u1", "post", "科技", 20);

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(accountMapper).updateSearchHistory(eq("u1"), json.capture());
        List<ZentideSearchHistoryItem> history = new ObjectMapper().readValue(json.getValue(), new TypeReference<>() {});
        assertEquals(5, history.size());
        assertEquals("科技", history.getFirst().getQuery());
        assertEquals(1, history.stream().filter(item -> "科技".equals(item.getQuery()) && "post".equals(item.getType())).count());
    }

    @Test
    void clearsHistoryWithoutDeletingAccountData() {
        ZentideSearchService service = new ZentideSearchService(interestMapper, accountMapper, "", "posts", "hubs");
        service.clearHistory("u1");
        verify(accountMapper).updateSearchHistory("u1", "[]");
    }
}
