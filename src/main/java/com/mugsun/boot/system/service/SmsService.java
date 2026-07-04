package com.mugsun.boot.system.service;

import com.mugsun.boot.system.entity.SysSmsCode;
import com.mugsun.boot.system.mapper.SysSmsCodeMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.dromara.sms4j.core.factory.SmsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * 短信服务：验证码发送（落库 + SMS4J 通道，未配通道时降级日志）与校验。
 */
@Service
public class SmsService {

	private static final Logger log = LoggerFactory.getLogger(SmsService.class);

	private final SysSmsCodeMapper smsCodeMapper;
	private final Random random = new Random();

	public SmsService(SysSmsCodeMapper smsCodeMapper) {
		this.smsCodeMapper = smsCodeMapper;
	}

	/** 发送验证码：生成 6 位码落库，尝试经 SMS4J 下发，通道未就绪则降级日志 */
	public void sendCode(String phone) {
		String code = String.valueOf(100000 + random.nextInt(900000));
		SysSmsCode entity = new SysSmsCode();
		entity.setPhone(phone);
		entity.setCode(code);
		entity.setExpireTime(LocalDateTime.now().plusMinutes(5));
		smsCodeMapper.insertSelective(entity);
		try {
			SmsResponse resp = SmsFactory.getSmsBlend().sendMessage(phone, code);
			if (resp == null || !resp.isSuccess()) {
				log.info("短信通道未就绪，降级输出验证码 phone={} code={}", phone, code);
			}
		} catch (Exception e) {
			log.info("短信通道未配置，降级输出验证码 phone={} code={}", phone, code);
		}
	}

	/** 校验验证码：命中未过期记录则通过并作废 */
	public boolean verifyCode(String phone, String code) {
		SysSmsCode rec = smsCodeMapper.selectOneByQuery(QueryWrapper.create()
			.eq("phone", phone).eq("code", code)
			.ge("expire_time", LocalDateTime.now())
			.orderBy("id", false));
		if (rec == null) {
			return false;
		}
		smsCodeMapper.deleteById(rec.getId());
		return true;
	}
}
