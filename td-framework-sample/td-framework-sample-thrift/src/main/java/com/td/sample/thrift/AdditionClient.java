package com.td.sample.thrift;

import javax.annotation.PostConstruct;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AdditionClient {
	private AdditionService.Client client;
	private TTransport transport;

	@PostConstruct
	private void init() {

		transport = new TSocket("localhost", 9090);
		TProtocol protocol = new TBinaryProtocol(transport);
		client = new AdditionService.Client(protocol);
	}

	@Scheduled(cron = "0/1 * *  * * ? ")
	public void post() {
		try {
			if (!transport.isOpen()) {
				transport.open();
			}
			System.out.println(client.add(100, 200));
		} catch (TException e) {
			e.printStackTrace();
		}
	}

}
