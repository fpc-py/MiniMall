package com.mall.ai.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.mall.ai.config.AiProperties;
import com.mall.ai.dto.AiChatContext;
import com.mall.ai.dto.AiChatRequest;
import com.mall.ai.entity.AiConversation;
import com.mall.ai.entity.AiMessage;
import com.mall.ai.mapper.AiConversationMapper;
import com.mall.ai.mapper.AiMessageMapper;
import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 客服会话服务测试：会话创建与标题摘要、用户/客服消息落库、历史装配、归属校验。
 */
@ExtendWith(MockitoExtension.class)
class AiConversationServiceTest {

    private static final long MEMBER_ID = 1L;
    private static final long OTHER_MEMBER_ID = 2L;
    private static final long CONVERSATION_ID = 500L;
    private static final long MESSAGE_ID = 900L;

    @Mock
    private AiConversationMapper conversationMapper;
    @Mock
    private AiMessageMapper messageMapper;

    private AiConversationService conversationService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, AiConversation.class);
        TableInfoHelper.initTableInfo(assistant, AiMessage.class);
    }

    @BeforeEach
    void setUp() {
        AiProperties aiProperties = new AiProperties();
        aiProperties.setHistoryLimit(20);
        conversationService = new AiConversationService(conversationMapper, messageMapper, aiProperties);
    }

    private AiChatRequest request(String content, Long conversationId) {
        AiChatRequest request = new AiChatRequest();
        request.setContent(content);
        request.setConversationId(conversationId);
        return request;
    }

    private AiConversation conversation(long memberId) {
        AiConversation conversation = new AiConversation();
        conversation.setId(CONVERSATION_ID);
        conversation.setMemberId(memberId);
        conversation.setStatus(0);
        conversation.setLastMessageTime(LocalDateTime.now());
        return conversation;
    }

    @Test
    @DisplayName("首轮提问：新建会话（标题截断 30 字）并落库用户消息")
    void prepareCreatesConversationAndPersistsUserMessage() {
        doAnswer(invocation -> {
            AiConversation saved = invocation.getArgument(0);
            saved.setId(CONVERSATION_ID);
            return 1;
        }).when(conversationMapper).insert(any(AiConversation.class));
        doAnswer(invocation -> {
            AiMessage saved = invocation.getArgument(0);
            saved.setId(MESSAGE_ID);
            return 1;
        }).when(messageMapper).insert(any(AiMessage.class));
        when(messageMapper.selectList(any())).thenReturn(List.of());

        String longQuestion = "七天无理由退货的具体规则和申请流程是什么样的，请详细说明一下并给出示例";
        AiChatContext context = conversationService.prepare(MEMBER_ID, request(longQuestion, null));

        assertThat(context.conversationId()).isEqualTo(CONVERSATION_ID);
        assertThat(context.userMessageId()).isEqualTo(MESSAGE_ID);

        ArgumentCaptor<AiConversation> conversationCaptor = ArgumentCaptor.forClass(AiConversation.class);
        verify(conversationMapper).insert(conversationCaptor.capture());
        AiConversation created = conversationCaptor.getValue();
        assertThat(created.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(created.getStatus()).isZero();
        assertThat(created.getMessageCount()).isZero();
        assertThat(created.getTitle()).hasSize(30).isEqualTo(longQuestion.substring(0, 30));

        ArgumentCaptor<AiMessage> messageCaptor = ArgumentCaptor.forClass(AiMessage.class);
        verify(messageMapper).insert(messageCaptor.capture());
        AiMessage userMessage = messageCaptor.getValue();
        assertThat(userMessage.getRole()).isEqualTo(1);
        assertThat(userMessage.getConversationId()).isEqualTo(CONVERSATION_ID);
        assertThat(userMessage.getContent()).isEqualTo(longQuestion);

        // 会话计数与末次消息时间在同一事务内递增
        verify(conversationMapper).update(any(), any());
    }

    @Test
    @DisplayName("历史装配：客服消息转 AssistantMessage，用户消息转 UserMessage，按时间正序")
    void prepareLoadsHistoryInChronologicalOrder() {
        when(conversationMapper.selectById(CONVERSATION_ID)).thenReturn(conversation(MEMBER_ID));
        doAnswer(invocation -> {
            AiMessage saved = invocation.getArgument(0);
            saved.setId(MESSAGE_ID);
            return 1;
        }).when(messageMapper).insert(any(AiMessage.class));

        // 仓储按 id 倒序返回（与实现一致），服务内部需反转为正序
        AiMessage assistant = new AiMessage();
        assistant.setId(12L);
        assistant.setRole(2);
        assistant.setContent("您好，退货运费由买家承担。");
        AiMessage user = new AiMessage();
        user.setId(11L);
        user.setRole(1);
        user.setContent("退货运费谁出？");
        when(messageMapper.selectList(any())).thenReturn(new ArrayList<>(List.of(assistant, user)));

        List<Message> history = conversationService
                .prepare(MEMBER_ID, request("那退款多久到账？", CONVERSATION_ID)).history();

        assertThat(history).hasSize(2);
        assertThat(history.get(0)).isInstanceOf(UserMessage.class);
        assertThat(history.get(0).getText()).isEqualTo("退货运费谁出？");
        assertThat(history.get(1)).isInstanceOf(AssistantMessage.class);
        assertThat(history.get(1).getText()).isEqualTo("您好，退货运费由买家承担。");
    }

    @Test
    @DisplayName("续轮提问：会话不存在抛 AI_CONVERSATION_NOT_FOUND，且不落库消息")
    void prepareThrowsWhenConversationMissing() {
        when(conversationMapper.selectById(CONVERSATION_ID)).thenReturn(null);

        assertThatThrownBy(() -> conversationService.prepare(MEMBER_ID, request("在吗", CONVERSATION_ID)))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.AI_CONVERSATION_NOT_FOUND.getCode());

        verify(messageMapper, never()).insert(any(AiMessage.class));
    }

    @Test
    @DisplayName("越权续轮：会话属于他人抛 AI_CONVERSATION_FORBIDDEN，且不落库消息")
    void prepareRejectsForeignConversation() {
        when(conversationMapper.selectById(CONVERSATION_ID)).thenReturn(conversation(OTHER_MEMBER_ID));

        assertThatThrownBy(() -> conversationService.prepare(MEMBER_ID, request("在吗", CONVERSATION_ID)))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.AI_CONVERSATION_FORBIDDEN.getCode());

        verify(messageMapper, never()).insert(any(AiMessage.class));
    }

    @Test
    @DisplayName("落库客服回答：role=2 并返回消息ID")
    void appendAssistantPersistsAssistantMessage() {
        doAnswer(invocation -> {
            AiMessage saved = invocation.getArgument(0);
            saved.setId(MESSAGE_ID);
            return 1;
        }).when(messageMapper).insert(any(AiMessage.class));

        Long messageId = conversationService.appendAssistant(CONVERSATION_ID, MEMBER_ID, "您的订单已发货。");

        assertThat(messageId).isEqualTo(MESSAGE_ID);
        ArgumentCaptor<AiMessage> captor = ArgumentCaptor.forClass(AiMessage.class);
        verify(messageMapper).insert(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(2);
        assertThat(captor.getValue().getContent()).isEqualTo("您的订单已发货。");
        verify(conversationMapper).update(any(), any());
    }

    @Test
    @DisplayName("消息列表：越权访问他人会话抛 AI_CONVERSATION_FORBIDDEN")
    void listMessagesRejectsForeignConversation() {
        when(conversationMapper.selectById(CONVERSATION_ID)).thenReturn(conversation(OTHER_MEMBER_ID));

        assertThatThrownBy(() -> conversationService.listMessages(MEMBER_ID, CONVERSATION_ID))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.AI_CONVERSATION_FORBIDDEN.getCode());

        verify(messageMapper, never()).selectList(any());
    }
}
