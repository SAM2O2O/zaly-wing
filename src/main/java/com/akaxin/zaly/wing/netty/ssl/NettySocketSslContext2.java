package com.akaxin.zaly.wing.netty.ssl;

import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.internal.EmptyArrays;

public class NettySocketSslContext2 {
	private static final Logger logger = LoggerFactory.getLogger(NettySocketSslContext2.class);

	private static SslContext sslContext;

	private NettySocketSslContext2() {

	}

	public static SslContext getSSLContext() {
		try {
			if (sslContext == null) {
				sslContext = SslContextBuilder.forClient().trustManager(ZalyTrustManagerFactory.INSTANCE).build();
			}
		} catch (Exception e) {
			throw new Error("Failed to initialize akaxin-platform server-side SSLContext", e);
		}

		return sslContext;
	}

	private static final TrustManager tm = new X509TrustManager() {
		@Override
		public void checkClientTrusted(X509Certificate[] chain, String s) {
			logger.debug("Accepting a client certificate: " + chain[0].getSubjectDN());
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String s) {
			logger.debug("Accepting a server certificate: " + chain[0].getSubjectDN());
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return EmptyArrays.EMPTY_X509_CERTIFICATES;
		}
	};

}
