package com.s8.stack.arch.tests.web.ssl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import com.s8.core.arch.silicon.SiliconConfiguration;
import com.s8.core.arch.silicon.SiliconEngine;
import com.s8.core.web.helium.rx.RxConnection;
import com.s8.core.web.helium.ssl.SSL_Client;
import com.s8.core.web.helium.ssl.SSL_Server;
import com.s8.core.web.helium.ssl.SSL_WebConfiguration;

public class SSL_RoundTrip02 {

	public static void main(String[] args) throws Exception {

		System.out.println("Starting...");
		
		SiliconConfiguration appConfiguration = new SiliconConfiguration();
		
		SSL_WebConfiguration serverConfig = SSL_WebConfiguration.load("config/ssl/server_config.xml");
		//serverConfig.isRxVerbose = true;
		
		//SSLContext context = SSL_Module.createContext(serverConfig);
		SiliconEngine ng = new SiliconEngine(appConfiguration);
		ng.start();
		

		SSL_Server server = new SSL_Server() {

			@Override
			public RxConnection createConnection(SelectionKey key, SocketChannel channel) throws IOException {

				return new SSL_Connection_Impl02(this, key, channel, 
						new SSL_Inbound_Impl02("server", serverConfig) {

							@Override
							public void ssl_onReceived(ByteBuffer buffer) {
								int n = buffer.limit();
								byte[] bytes = new byte[n];
								buffer.get(bytes);
								System.out.println("[SSL_outbound (test)] "+new String(bytes));
							}

							@Override
							public void ssl_onReinitializing() {
								System.out.println("[SSL_outbound (test)] RE_INIT requested");
								
							}

						},
						new SSL_Outbound_Impl02("server", serverConfig) {
							private int count = 0;

							@Override
							public void ssl_onSending(ByteBuffer buffer) {
								if(count<4) {
									buffer.put("Hi! this is server side!!".getBytes());
									count++;
								}
							}

							@Override
							public void ssl_onHandshakingCompleted() {
							}
							
							@Override
							public void ssl_onReinitializing() {
								System.out.println("[SSL_outbound (test)] RE_INIT requested");
								
							}

						});
			}

			@Override
			public SSL_WebConfiguration getWebConfiguration() {
				return serverConfig;
			}

			
			public @Override SiliconEngine getSiliconEngine() { return ng; }
		};
		

		SSL_WebConfiguration clientConfig = SSL_WebConfiguration.load("config/ssl/client_config.xml");
		

		SSL_Client client = new SSL_Client() {

			@Override
			public RxConnection createConnection(SelectionKey key, SocketChannel channel) throws IOException {
				return new SSL_Connection_Impl02(this, key, channel, 
						new SSL_Inbound_Impl02("client", clientConfig) {

							@Override
							public void ssl_onReceived(ByteBuffer buffer) {
								int n = buffer.limit();
								byte[] bytes = new byte[n];
								buffer.get(bytes);
								System.out.println("[SSL_Inbound (test)] "+new String(bytes));
							}

							@Override
							public void ssl_onReinitializing() {
								System.out.println("[SSL_Inbound (test)] RE-INIT requested");	
							}
						},
						new SSL_Outbound_Impl02("client", clientConfig) {
							private int count = 0;

							@Override
							public void ssl_onSending(ByteBuffer buffer) {

								byte[] messageBytes = "Hi this is client!!".getBytes();
								if(count<4 && buffer.remaining()>messageBytes.length) {
									buffer.put(messageBytes);
								}
								count++;	
							}

							@Override
							public void ssl_onHandshakingCompleted() {
								System.out.println("[SSL_Outbound (test)] HANDSHAKE COMPLETED");	
							}
							

							@Override
							public void ssl_onReinitializing() {
								System.out.println("[SSL_Outbound (test)] RE-INIT requested");		
							}
						});
			}

			@Override
			public SSL_WebConfiguration getWebConfiguration() {
				return clientConfig;
			}

			@Override
			public void stop() throws Exception {
			}
			
			public @Override SiliconEngine getSiliconEngine() { return ng; }
		};
		
		// lauching sequence
		
		server.start();

		client.start();
		client.send();

	}

}
