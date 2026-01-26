package com.magic.withdraw.core.loader;

import com.magic.withdraw.core.annotation.TradePlatform;
import com.magic.withdraw.core.exception.TradeException;
import com.magic.withdraw.core.service.TradeService;
import com.magic.withdraw.core.service.ValidService;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author lgy
 * @since 2026/1/13
 */
public class MagicLoader implements ApplicationContextAware {

    public static Map<String, TradeService> magicTradeMap = new HashMap<>();
    public static Map<String, ValidService> magicValidMap = new HashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Collection<TradeService> magicTrades = applicationContext.getBeansOfType(TradeService.class).values();
        magicTrades.forEach(m -> {
            TradePlatform annotation = m.getClass().getAnnotation(TradePlatform.class);
            if (annotation != null) {
                magicTradeMap.put(annotation.value(), m);
            }
        });

        Collection<ValidService> magicValids = applicationContext.getBeansOfType(ValidService.class).values();
        magicValids.forEach(m -> {
            TradePlatform annotation = m.getClass().getAnnotation(TradePlatform.class);
            if (annotation != null) {
                magicValidMap.put(annotation.value(), m);
            }
        });
    }

    /**
     * 获取对应的交易类型
     * @param platform 平台
     * @return 结果
     */
    public static TradeService getTradeService(String platform){
        TradeService tradeService = magicTradeMap.get(platform);
        if(tradeService != null){
            return tradeService;
        }else{
            throw new TradeException("未找到适配的交易类型,交易平台id:" + platform);
        }
    }

    /**
     * 获取对应的验签类型
     * @param platform 平台
     * @return 结果
     */
    public static ValidService getValidService(String platform){
        ValidService validService = magicValidMap.get(platform);
        if(validService != null){
            return validService;
        }else{
            throw new TradeException("未找到适配的交易类型,交易平台id:" + platform);
        }
    }
}
