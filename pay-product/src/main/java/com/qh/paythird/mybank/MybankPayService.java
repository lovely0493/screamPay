package com.qh.paythird.mybank;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.qh.common.utils.R;
import com.qh.pay.api.Order;
import com.qh.pay.api.PayConstants;
import com.qh.pay.api.constenum.OrderState;
import com.qh.pay.api.constenum.OutChannel;
import com.qh.pay.api.utils.Md5Util;
import com.qh.pay.api.utils.ParamUtil;
import com.qh.pay.service.PayService;
import com.qh.redis.service.RedisUtil;

/**
 * 个人支付宝转银行卡
 *
 */
@Service
public class MybankPayService {


	private static final Logger logger = LoggerFactory.getLogger(MybankPayService.class);
	
	/**
	 * @Description 支付发起
	 * @param order
	 * @return
	 */
	public R order(Order order) {
		
		logger.info("个人支付宝转银行卡支付 开始------------------------------------------------------");
		Map<String, String> data = new HashMap<>();
		
		if (!order.getOutChannel().equals(OutChannel.ali.name())) {
			String errorMsg = "仅支持支付宝扫码通道！";
			logger.error(errorMsg);
			return R.error(errorMsg);
		}
		
		try {
			String code_url = PayService.mybankQrUrl(order);
			data.put(PayConstants.web_code_url, code_url);
			return R.okData(data);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("生成支付链接出错！");
		} finally {
			logger.info("个人支付宝转银行卡 结束------------------------------------------------------");
		}
		
		return R.error("个人支付宝转银行卡下单失败！");
	}
	
	/**
	 * @Description 支付回调
	 * @param order
	 * @param request
	 * @return
	 */
	public R notify(Order order, HttpServletRequest request, String requestBody) {
		logger.info("个人支付宝转银行卡 异步通知开始-------------------------------------------------");
		/* 回调格式  http://域名/pay/notify/mybank/{merchNo}/{orderNo}
		 * 回调方法：支持GET/POST
		 * 回调参数: money(金额), time(毫秒时间),  sign(MD5签名)
		 * sign = MD5(money=xx&time=xx&key=xx)
		 * 参考下面的验签过程
		 *  */
		JSONObject jsonParam = JSONObject.parseObject(requestBody);
		String money = jsonParam.getString("money");
		String time = jsonParam.getString("time");
		String sign = jsonParam.getString("sign");
		
		if (ParamUtil.isEmpty(money) || ParamUtil.isEmpty(sign)) {
			String errorMsg = "回调缺少必要参数";
			logger.error(errorMsg);
			return R.error(errorMsg);
		}

		String key = RedisUtil.getPayCommonValue(order.getPayMerch() + "key");
		String sourceStr = "money="+money+ "&time=" +time+ "&key="+key;
		String localSign = Md5Util.MD5(sourceStr).toUpperCase();
		logger.info("个人支付宝转银行卡异步通知，sourceStr=" + sourceStr);
		logger.info("个人支付宝转银行卡异步通知, localSign=" + localSign);
		logger.info("个人支付宝转银行卡异步通知, sign=" + sign);
		
		if (!localSign.equalsIgnoreCase(sign)) {
			String errorMsg = "个人支付宝转银行卡异步通知验签失败！";
			logger.error(errorMsg);
			return R.error(errorMsg);
		}

		if (order.getAmount().compareTo(new BigDecimal(money)) != 0) {
			String errorMsg = "个人支付宝转银行卡异步通知金额有误！";
			logger.error(errorMsg + ";订单金额="+order.getAmount()+";支付金额="+money);
			return R.error(errorMsg);		
		}
		
		/* 可以确定这是成功 */
		order.setOrderState(OrderState.succ.id());
		order.setRealAmount(order.getAmount());
	
		logger.info("{},{}", order.getMerchNo(), order.getOrderNo());
		logger.info("个人支付宝转银行卡 支付异步通知结束-------------------------------------------------");
		return R.ok();
	}
	
	/**
	 * @Description 支付查询
	 * @param order
	 * @return
	 */
	public R query(Order order) {
		logger.info("个人支付宝 查询 开始------------------------------------------------------------");

//		order.setRealAmount(order.getAmount());
//		order.setOrderState(OrderState.succ.id());
		logger.info("{},{}", order.getMerchNo(), order.getOrderNo());
		logger.info("个人支付宝 查询 结束-------------------------------------------------");
		
		return R.ok();
	}

}
