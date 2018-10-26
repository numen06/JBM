package com.jbm.test.disruptor.translator;

import java.util.Random;

import com.lmax.disruptor.EventTranslator;
import com.jbm.test.disruptor.bean.TradeTransaction;

public class TradeTransactionEventTranslator implements EventTranslator<TradeTransaction> {

	private Random random = new Random();

	@Override
	public void translateTo(TradeTransaction event, long sequence) {
		event.setSequence(sequence);
		this.generateTradeTransaction(event);
	}

	private TradeTransaction generateTradeTransaction(TradeTransaction trade) {
		trade.setPrice(random.nextDouble() * 9999);
		return trade;
	}
}
