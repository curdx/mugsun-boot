package com.mugsun.boot.mail;

import com.mugsun.boot.mail.entity.SysMailTemplate;
import com.mugsun.boot.mail.mapper.SysMailTemplateMapper;
import com.mugsun.boot.notify.NotifyTemplateRenderer;
import com.mugsun.core.tool.exception.ServiceException;
import com.mybatisflex.core.query.QueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 邮件发送：按模板 ${key} 占位渲染后发送；无 SMTP 凭证时降级为日志（同短信）。
 * <p>渲染收编统一渲染器 {@link NotifyTemplateRenderer}（缺参 fail-fast，取代原"未提供占位原样保留"）。
 */
@Service
public class MailService {

	private static final Logger log = LoggerFactory.getLogger(MailService.class);

	private final JavaMailSender mailSender;
	private final SysMailTemplateMapper templateMapper;
	private final NotifyTemplateRenderer renderer;

	@Value("${spring.mail.username:noreply@mugsun.com}")
	private String from;

	public MailService(JavaMailSender mailSender, SysMailTemplateMapper templateMapper,
					   NotifyTemplateRenderer renderer) {
		this.mailSender = mailSender;
		this.templateMapper = templateMapper;
		this.renderer = renderer;
	}

	/** 按模板发送，返回渲染后内容（便于调试/降级展示） */
	public String sendByTemplate(String to, String code, Map<String, String> params) {
		SysMailTemplate tpl = templateMapper.selectOneByQuery(QueryWrapper.create().eq("code", code));
		if (tpl == null) {
			throw new ServiceException("邮件模板不存在: " + code);
		}
		String content = renderer.render(tpl.getContent(), params);
		send(to, renderer.render(tpl.getSubject(), params), content);
		return content;
	}

	/** 发送纯文本邮件；失败降级为日志，不阻断业务 */
	public void send(String to, String subject, String content) {
		try {
			SimpleMailMessage msg = new SimpleMailMessage();
			msg.setFrom(from);
			msg.setTo(to);
			msg.setSubject(subject);
			msg.setText(content);
			mailSender.send(msg);
			log.info("邮件已发送 to={} subject={}", to, subject);
		} catch (Exception e) {
			log.warn("邮件发送失败(降级为日志) to={} subject={} content={} err={}", to, subject, content, e.getMessage());
		}
	}
}
