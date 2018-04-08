package com.jbm.sample.thrift;

import javax.annotation.PostConstruct;

import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TServer.Args;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jbm.sample.thrift.AdditionService.Processor;

import jodd.util.ThreadUtil;

@Service
public class ThriftServer {

	@Autowired
	private AdditionServiceHandler additionServiceHandler;

	@PostConstruct
	public void StartsimpleServer() {

		ThreadUtil.newCoreThreadPool("ths").execute(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					TServerTransport serverTransport = new TServerSocket(9090);
					Args args = new Args(serverTransport);
					AdditionService.Processor<AdditionServiceHandler> processor = new Processor<AdditionServiceHandler>(additionServiceHandler);
					args.processor(processor);
					TServer server = new TSimpleServer(args);
					System.out.println("Starting the simple server...");
					server.serve();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

}
