package com.jbm.test.disruptor.consumer;

import com.alibaba.fastjson.JSON;
import com.jbm.test.disruptor.bean.TradeTransaction;
import com.lmax.disruptor.EventHandler;

/**
 * 刷卡消费者
 *
 * @author wesley.zhang
 */
public class TradeTransactionVasConsumer implements EventHandler<TradeTransaction> {

    @Override
    public void onEvent(TradeTransaction event, long sequence, boolean endOfBatch) throws Exception {
        event.setVas(1);
        System.out.println("我是Vas：" + JSON.toJSONString(event));
    }

}
