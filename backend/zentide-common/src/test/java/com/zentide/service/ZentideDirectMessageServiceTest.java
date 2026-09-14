package com.zentide.service;

import com.zentide.entity.po.ZentideDirectMessage;
import com.zentide.exception.BusinessException;
import com.zentide.mapper.ZentideDirectMessageMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ZentideDirectMessageServiceTest {
    @Test
    void sendsTrimmedPrivateMessageToExistingUser() {
        ZentideDirectMessageMapper mapper = mock(ZentideDirectMessageMapper.class);
        when(mapper.countUser("u2")).thenReturn(1);
        when(mapper.insertMessage(any())).thenAnswer(invocation -> {
            invocation.<ZentideDirectMessage>getArgument(0).setMessageId(9L);
            return 1;
        });

        ZentideDirectMessage result = new ZentideDirectMessageService(mapper).send("u1", "u2", "  一起聊聊这场演出  ");

        ArgumentCaptor<ZentideDirectMessage> captor = ArgumentCaptor.forClass(ZentideDirectMessage.class);
        verify(mapper).insertMessage(captor.capture());
        assertEquals("一起聊聊这场演出", captor.getValue().getBody());
        assertEquals(9L, result.getMessageId());
        assertNotNull(result.getCreatedAt());
    }

    @Test
    void cannotMessageSelfOrUnknownUser() {
        ZentideDirectMessageMapper mapper = mock(ZentideDirectMessageMapper.class);
        ZentideDirectMessageService service = new ZentideDirectMessageService(mapper);

        assertThrows(BusinessException.class, () -> service.send("u1", "u1", "你好"));
        assertThrows(BusinessException.class, () -> service.send("u1", "missing", "你好"));
        verify(mapper, never()).insertMessage(any());
    }

    @Test
    void loadingThreadMarksIncomingMessagesReadAndReturnsChronologicalOrder() {
        ZentideDirectMessageMapper mapper = mock(ZentideDirectMessageMapper.class);
        when(mapper.countUser("u2")).thenReturn(1);
        ZentideDirectMessage newest = message(3L);
        ZentideDirectMessage oldest = message(1L);
        when(mapper.listMessages("u1", "u2", 200)).thenReturn(new ArrayList<>(List.of(newest, oldest)));

        List<ZentideDirectMessage> result = new ZentideDirectMessageService(mapper).messages("u1", "u2", null);

        verify(mapper).markConversationRead("u1", "u2");
        assertEquals(List.of(1L, 3L), result.stream().map(ZentideDirectMessage::getMessageId).toList());
    }

    @Test
    void rejectsOversizedMessages() {
        ZentideDirectMessageMapper mapper = mock(ZentideDirectMessageMapper.class);
        when(mapper.countUser("u2")).thenReturn(1);
        BusinessException error = assertThrows(BusinessException.class,
            () -> new ZentideDirectMessageService(mapper).send("u1", "u2", "x".repeat(2001)));
        assertTrue(error.getMessage().contains("2000"));
    }

    private ZentideDirectMessage message(Long id) {
        ZentideDirectMessage message = new ZentideDirectMessage();
        message.setMessageId(id);
        return message;
    }
}
