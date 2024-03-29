package com.jbm.test.disruptor.consumer;

import com.alibaba.fastjson.JSON;
import com.jbm.test.disruptor.bean.TradeTransaction;
import com.lmax.disruptor.EventHandler;

import java.util.UUID;

/**
 * 入库消费者
 *
 * @author wesley.zhang
 */
public class TradeTransactionInDBHandler implements EventHandler<TradeTransaction> {

    @Override
    public void onEvent(TradeTransaction event, long sequence, boolean endOfBatch) throws Exception {
        this.onEvent(event);
    }

    public void onEvent(TradeTransaction event) throws Exception {
        event.setId(UUID.randomUUID().toString());
        event.setRuku(1);
        System.out.println("我是入库：" + JSON.toJSONString(event));

    }
}
