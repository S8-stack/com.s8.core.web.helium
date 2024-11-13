package com.s8.stack.arch.tests.web.ssl;

import com.s8.core.web.helium.ssl.v1.SSL_Connection;
import com.s8.core.web.helium.ssl.v1.SSL_Inbound;
import com.s8.core.web.helium.ssl.v1.SSL_WebConfiguration;

public abstract class SSL_Inbound_Impl02 extends SSL_Inbound {

	public SSL_Inbound_Impl02(String name, SSL_WebConfiguration configuration) {
		super(name, configuration);
	}

	SSL_Connection connection;
	
	@Override
	public SSL_Connection getConnection() {
		return connection;
	}


}
