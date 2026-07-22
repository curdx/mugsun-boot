package com.mugsun.boot.message;

import cn.dev33.satoken.stp.StpUtil;
import com.mugsun.boot.common.constant.WsConstants;
import com.mugsun.boot.message.entity.SysMessage;
import com.mugsun.boot.message.entity.SysMessageTemplate;
import com.mugsun.boot.message.entity.SysMessageUser;
import com.mugsun.boot.message.mapper.SysMessageMapper;
import com.mugsun.boot.message.mapper.SysMessageTemplateMapper;
import com.mugsun.boot.message.mapper.SysMessageUserMapper;
import com.mugsun.boot.websocket.WsFrame;
import com.mugsun.boot.websocket.WsMessageSender;
import com.mugsun.core.tool.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 站内信服务：发送（模板渲染 + 消息主体 + 逐收件人未读记录），落库成功后向在线收件人实时推送。
 */
@Service
public class MessageService {

	private static final Logger log = LoggerFactory.getLogger(MessageService.class);

	private final SysMessageMapper messageMapper;
	private final SysMessageUserMapper messageUserMapper;
	private final SysMessageTemplateMapper templateMapper;
	private final WsMessageSender wsMessageSender;

	public MessageService(SysMessageMapper messageMapper, SysMessageUserMapper messageUserMapper,
						  SysMessageTemplateMapper templateMapper, WsMessageSender wsMessageSender) {
		this.messageMapper = messageMapper;
		this.messageUserMapper = messageUserMapper;
		this.templateMapper = templateMapper;
		this.wsMessageSender = wsMessageSender;
	}

	/** 发送站内信：可选模板 + 占位替换 → 建消息主体 + 逐收件人未读记录 → 实时推送在线收件人 */
	public void send(MessageSendDTO dto) {
		if (dto.getRecipientIds() == null || dto.getRecipientIds().isEmpty()) {
			throw new ServiceException("请选择收件人");
		}
		String title = dto.getTitle();
		String content = dto.getContent();
		if (dto.getTemplateId() != null) {
			SysMessageTemplate tpl = templateMapper.selectOneById(dto.getTemplateId());
			if (tpl != null) {
				title = tpl.getTitle();
				content = tpl.getContent();
			}
		}
		title = render(title, dto.getParams());
		content = render(content, dto.getParams());
		if (title == null || title.isBlank()) {
			throw new ServiceException("消息标题不能为空");
		}
		SysMessage message = new SysMessage();
		message.setTitle(title);
		message.setContent(content);
		message.setType(dto.getType() == null || dto.getType().isBlank() ? "system" : dto.getType());
		message.setSenderId(StpUtil.getLoginIdAsLong());
		messageMapper.insertSelective(message);
		List<Long> recipientIds = dto.getRecipientIds().stream().distinct().toList();
		recipientIds.forEach(uid -> {
			SysMessageUser mu = new SysMessageUser();
			mu.setMessageId(message.getId());
			mu.setUserId(uid);
			mu.setIsRead(0);
			messageUserMapper.insertSelective(mu);
		});
		pushNewMessage(message, recipientIds);
	}

	/** 实时推送新站内信：若处于事务中则提交后再推，推送失败不影响主流程 */
	private void pushNewMessage(SysMessage message, List<Long> recipientIds) {
		Map<String, Object> content = new LinkedHashMap<>();
		content.put("messageId", message.getId());
		content.put("title", message.getTitle());
		content.put("content", message.getContent());
		content.put("type", message.getType());
		// createTime 由数据库 now() 填充且不回写实体，推送帧取应用侧当前时间（与落库时刻同秒级一致）
		content.put("sendTime", LocalDateTime.now());
		WsFrame frame = WsFrame.of(WsConstants.MESSAGE_NEW, content);
		afterCommit(() -> {
			try {
				wsMessageSender.sendToUsers(recipientIds, frame);
			} catch (Exception e) {
				log.warn("站内信实时推送失败 messageId={}", message.getId(), e);
			}
		});
	}

	/** 事务激活时注册提交后执行（避免回滚后推出不存在的消息），否则立即执行 */
	private void afterCommit(Runnable action) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					action.run();
				}
			});
		} else {
			action.run();
		}
	}

	private String render(String template, Map<String, String> params) {
		if (template == null) {
			return null;
		}
		String result = template;
		if (params != null) {
			for (Map.Entry<String, String> e : params.entrySet()) {
				result = result.replace("${" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
			}
		}
		// 清除未提供的占位，避免 ${xxx} 字面泄漏
		return result.replaceAll("\\$\\{[^}]*}", "");
	}
}
