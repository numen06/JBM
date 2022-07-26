package com.jbm.test.disruptor.translator;

import com.jbm.test.disruptor.bean.TradeTransaction;
import com.lmax.disruptor.EventTranslator;

import java.util.Random;

public class TradeTransactionEventTranslator implements EventTranslator<TradeTransaction> {

    private Random random = new Random();

    @Override
    public void translateTo(TradeTransaction event, long sequence) {
        System.out.println(sequence);
        this.generateTradeTransaction(event);
    }

    private TradeTransaction generateTradeTransaction(TradeTransaction trade) {
        trade.setPrice(random.nextDouble() * 9999);
        return trade;
    }
}
