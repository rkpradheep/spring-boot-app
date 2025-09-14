package com.server.framework.http;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

class SSLSocketFactoryWrapper extends SSLSocketFactory
{
	private final SSLSocketFactory base;
	private final String supportedProtocol;

	SSLSocketFactoryWrapper(SSLSocketFactory base, String supportedProtocol)
	{
		this.base = base;
		this.supportedProtocol = supportedProtocol;
	}

	SSLSocketFactoryWrapper(SSLSocketFactory base)
	{
		this.base = base;
		this.supportedProtocol = "TLSv1.2";
	}

	public String[] getDefaultCipherSuites()
	{
		return base.getDefaultCipherSuites();
	}

	public String[] getSupportedCipherSuites()
	{
		return base.getSupportedCipherSuites();
	}

	public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException
	{
		return getSocket(base.createSocket(s, host, port, autoClose));
	}

	public Socket createSocket(String host, int port) throws IOException
	{
		return getSocket(base.createSocket(host, port));
	}

	public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException
	{
		return getSocket(base.createSocket(host, port, localHost, localPort));
	}

	public Socket createSocket(InetAddress host, int port) throws IOException
	{
		return getSocket(base.createSocket(host, port));
	}

	public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException
	{
		return getSocket(base.createSocket(address, port, localAddress, localPort));
	}

	private Socket getSocket(Socket socket)
	{
		((SSLSocket) socket).setEnabledProtocols(new String[] {supportedProtocol});
		return socket;
	}
}
