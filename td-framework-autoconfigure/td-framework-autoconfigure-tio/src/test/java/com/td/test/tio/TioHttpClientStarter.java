package com.td.test.tio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.UUID;

import javax.net.SocketFactory;

import jodd.http.HttpRequest;
import jodd.http.net.SocketHttpConnection;

public class TioHttpClientStarter {
	public static void main(String[] args) throws InterruptedException, UnknownHostException, IOException {
		HttpRequest httpRequest = HttpRequest.post("http://127.0.0.1:9090/test/abtest");
		httpRequest.connectionKeepAlive(true);
		SocketHttpConnection httpConnection = new SocketHttpConnection(TioHttpClientStarter.createSocket(httpRequest.host(), httpRequest.port(), httpRequest.connectionTimeout()));
		httpRequest.open(httpConnection);
		ReadHttpForm form = new ReadHttpForm(httpConnection);
		form.startAsync();
		for (int i = 0; i < 10; i++) {
			httpRequest.bodyText(UUID.randomUUID().toString());
			Thread.sleep(1000l);
			httpRequest.send();
			// System.out.println(httpRequest.send());
		}

		Thread.sleep(30 * 1000l);

	}

	public static Socket createSocket(String host, int port, int connectionTimeout) throws IOException {
		SocketFactory socketFactory = SocketFactory.getDefault();
		if (connectionTimeout < 0) {
			return socketFactory.createSocket(host, port);
		} else {
			// creates unconnected socket
			Socket socket = socketFactory.createSocket();

			socket.connect(new InetSocketAddress(host, port), connectionTimeout);

			return socket;
		}
	}
}