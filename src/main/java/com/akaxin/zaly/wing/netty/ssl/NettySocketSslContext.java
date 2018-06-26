package com.akaxin.zaly.wing.netty.ssl;

import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import io.netty.util.internal.SystemPropertyUtil;

public class NettySocketSslContext {

	private static SSLContext sslContext;

	private NettySocketSslContext() {

	}

	public static SSLContext getSSLContext() {
		String algorithm = SystemPropertyUtil.get("ssl.KeyManagerFactory.algorithm");
		if (algorithm == null) {
			algorithm = "SunX509";
		}

		try {
			if (sslContext == null) {
				//
				KeyStore keystore = KeyStore.getInstance("JKS");
				// need storepass
				keystore.load(SslKeyStore.resourcesAsInputStream(), SslKeyStore.getKeyStorePassword());

				// init kmf
				KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
				// need keypass
				kmf.init(keystore, SslKeyStore.getCertificatePassword());

				TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
				tmf.init(keystore);

				// Initialize the SSLContext with kmf
				sslContext = SSLContext.getInstance("TLS");
				// sslContext = SslContextBuilder.forClient().trustManager(tmf).build();
				// sslContext2.newEngine();
				sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
			}
		} catch (Exception e) {
			throw new Error("Failed to initialize akaxin-platform server-side SSLContext", e);
		}

		return sslContext;
	}

}
