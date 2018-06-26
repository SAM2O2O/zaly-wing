package com.akaxin.zaly.wing.netty.ssl;

import java.net.Socket;
import java.security.KeyStore;
import java.security.Principal;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.ssl.util.SimpleTrustManagerFactory;
import io.netty.util.internal.EmptyArrays;

public class ZalyTrustManagerFactory extends SimpleTrustManagerFactory {
	private static final Logger logger = LoggerFactory.getLogger(ZalyTrustManagerFactory.class);

	public static final ZalyTrustManagerFactory INSTANCE = new ZalyTrustManagerFactory();

	private static X509ExtendedTrustManager tm = new X509ExtendedTrustManager() {

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return EmptyArrays.EMPTY_X509_CERTIFICATES;
		}

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String s) {
			logger.info("Accepting a zaly client certificate: " + chain[0].getSubjectDN());
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String s) throws CertificateException {
			logger.info("checkServerTrusted 0 : " + chain[0].getSubjectDN());
		}

		@Override
		public void checkClientTrusted(X509Certificate[] arg0, String arg1, Socket arg2) throws CertificateException {
			logger.info("checkClientTrusted: " + arg0[0].getSubjectDN());

		}

		@Override
		public void checkClientTrusted(X509Certificate[] arg0, String arg1, SSLEngine arg2)
				throws CertificateException {
			logger.info("checkClientTrusted: " + arg0[0].getSubjectDN());

		}

		@Override
		public void checkServerTrusted(X509Certificate[] arg0, String arg1, Socket arg2) throws CertificateException {
			logger.info("checkServerTrusted 1 : " + arg0[0].getSubjectDN());

		}

		@Override
		public void checkServerTrusted(X509Certificate[] arg0, String arg1, SSLEngine arg2)
				throws CertificateException {
			logger.debug("ssl check Server Trusted : {} {} {} ", arg0[0].getSubjectDN(), arg1, arg2.toString());

			String peerHost = arg2.getPeerHost();
			Principal pri = arg0[0].getSubjectDN();
			String serverHost = pri.getName().substring(3);
			if (StringUtils.isEmpty(serverHost) || !serverHost.equals(peerHost)) {
				throw new CertificateException("untrust server host : " + serverHost);
			}

		}
	};

	private ZalyTrustManagerFactory() {
	}

	@Override
	protected void engineInit(KeyStore keyStore) throws Exception {
	}

	@Override
	protected void engineInit(ManagerFactoryParameters managerFactoryParameters) throws Exception {
	}

	@Override
	protected TrustManager[] engineGetTrustManagers() {
		return new TrustManager[] { tm };
	}

}
