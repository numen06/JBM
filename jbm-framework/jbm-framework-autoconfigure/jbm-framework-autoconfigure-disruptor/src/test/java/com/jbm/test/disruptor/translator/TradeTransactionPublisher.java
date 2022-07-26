package com.jbm.test.disruptor.translator;

import com.jbm.test.disruptor.bean.TradeTransaction;
import com.lmax.disruptor.dsl.Disruptor;

import java.util.concurrent.CountDownLatch;

public class TradeTransactionPublisher implements Runnable {
    private static int LOOP = 100;// 模拟一千万次交易的发生
    Disruptor<TradeTransaction> disruptor;
    private CountDownLatch latch;

    public TradeTransactionPublisher(CountDownLatch latch, Disruptor<TradeTransaction> disruptor) {
        this.disruptor = disruptor;
        this.latch = latch;
    }

    @Override
    public void run() {
        TradeTransactionEventTranslator tradeTransloator = new TradeTransactionEventTranslator();
        for (int i = 0; i < LOOP; i++) {
            disruptor.publishEvent(tradeTransloator);
        }
        latch.countDown();
    }
}
