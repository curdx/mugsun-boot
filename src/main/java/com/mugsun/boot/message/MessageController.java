package com.mugsun.boot.message;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.mugsun.boot.message.entity.SysMessage;
import com.mugsun.boot.message.entity.SysMessageTemplate;
import com.mugsun.boot.message.entity.SysMessageUser;
import com.mugsun.boot.message.mapper.SysMessageMapper;
import com.mugsun.boot.message.mapper.SysMessageTemplateMapper;
import com.mugsun.boot.message.mapper.SysMessageUserMapper;
import com.mugsun.core.tool.api.R;
import com.mugsun.core.tool.exception.ServiceException;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 站内信：管理员按模板占位发给指定收件人；收件人查阅、未读统计与标记已读。
 */
@RestController
@RequestMapping("/system/message")
@SaCheckLogin
public class MessageController {

	private final SysMessageMapper messageMapper;
	private final SysMessageUserMapper messageUserMapper;
	private final SysMessageTemplateMapper templateMapper;

	public MessageController(SysMessageMapper messageMapper, SysMessageUserMapper messageUserMapper,
							 SysMessageTemplateMapper templateMapper) {
		this.messageMapper = messageMapper;
		this.messageUserMapper = messageUserMapper;
		this.templateMapper = templateMapper;
	}

	/** 发送站内信：可选模板 + 占位替换 → 建消息主体 + 逐收件人未读记录 */
	@PostMapping("/send")
	@SaCheckPermission("sys:message:manage")
	public R<Void> send(@RequestBody MessageSendDTO dto) {
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
		dto.getRecipientIds().stream().distinct().forEach(uid -> {
			SysMessageUser mu = new SysMessageUser();
			mu.setMessageId(message.getId());
			mu.setUserId(uid);
			mu.setIsRead(0);
			messageUserMapper.insertSelective(mu);
		});
		return R.success("发送成功");
	}

	/** 当前用户未读数（顶栏角标） */
	@GetMapping("/unread-count")
	public R<Long> unreadCount() {
		return R.data(messageUserMapper.selectCountByQuery(QueryWrapper.create()
			.eq("user_id", StpUtil.getLoginIdAsLong()).eq("is_read", 0)));
	}

	/** 顶栏下拉：最近若干条 */
	@GetMapping("/recent")
	public R<List<SysMessageUser>> recent(@RequestParam(defaultValue = "8") int limit) {
		List<SysMessageUser> list = messageUserMapper.selectListByQuery(QueryWrapper.create()
			.eq("user_id", StpUtil.getLoginIdAsLong()).orderBy("id", false).limit(limit));
		fillMessages(list);
		return R.data(list);
	}

	/** 我的消息分页 */
	@GetMapping("/my/page")
	public R<Page<SysMessageUser>> myPage(@RequestParam(defaultValue = "1") long pageNum,
										  @RequestParam(defaultValue = "10") long pageSize,
										  @RequestParam(required = false) Integer isRead) {
		QueryWrapper query = QueryWrapper.create().eq("user_id", StpUtil.getLoginIdAsLong()).orderBy("id", false);
		if (isRead != null) {
			query.eq("is_read", isRead);
		}
		Page<SysMessageUser> page = messageUserMapper.paginate(pageNum, pageSize, query);
		fillMessages(page.getRecords());
		return R.data(page);
	}

	/** 标记单条已读（按消息 id + 当前用户） */
	@PostMapping("/read/{messageId}")
	public R<Void> read(@PathVariable Long messageId) {
		SysMessageUser patch = new SysMessageUser();
		patch.setIsRead(1);
		patch.setReadTime(LocalDateTime.now());
		messageUserMapper.updateByQuery(patch, QueryWrapper.create()
			.eq("message_id", messageId).eq("user_id", StpUtil.getLoginIdAsLong()).eq("is_read", 0));
		return R.success("已读");
	}

	/** 全部标记已读 */
	@PostMapping("/read-all")
	public R<Void> readAll() {
		SysMessageUser patch = new SysMessageUser();
		patch.setIsRead(1);
		patch.setReadTime(LocalDateTime.now());
		messageUserMapper.updateByQuery(patch, QueryWrapper.create()
			.eq("user_id", StpUtil.getLoginIdAsLong()).eq("is_read", 0));
		return R.success("已全部标记已读");
	}

	/** 收件人删除自己的消息记录 */
	@PostMapping("/remove")
	public R<Void> remove(@RequestBody List<Long> ids) {
		// 仅删当前用户自己的收件记录，避免越权删他人
		Long userId = StpUtil.getLoginIdAsLong();
		messageUserMapper.deleteByQuery(QueryWrapper.create().in("id", ids).eq("user_id", userId));
		return R.success("删除成功");
	}

	/** 装配收件记录所属消息的标题/内容/类型/发送时间 */
	private void fillMessages(List<SysMessageUser> records) {
		List<Long> msgIds = records.stream().map(SysMessageUser::getMessageId).distinct().toList();
		if (msgIds.isEmpty()) {
			return;
		}
		Map<Long, SysMessage> map = messageMapper.selectListByIds(msgIds).stream()
			.collect(Collectors.toMap(SysMessage::getId, Function.identity()));
		records.forEach(r -> {
			SysMessage m = map.get(r.getMessageId());
			if (m != null) {
				r.setTitle(m.getTitle());
				r.setContent(m.getContent());
				r.setType(m.getType());
				r.setSendTime(m.getCreateTime());
			}
		});
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
