package com.server.protocol.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.security.Permission;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;

public class HttpURLConnectionWrapper extends HttpsURLConnection
{
	private static final Logger LOGGER = Logger.getLogger(HttpURLConnectionWrapper.class.getName());
	private final HttpURLConnection connection;
	private boolean callInitiated = false;
	private boolean callCompleted = false;
	private long startedTime;
	private Long httpLogId;
	private ByteArrayInputStream inputStreamWrapper;
	private ByteArrayInputStream errorStreamWrapper;
	private final Map<String, List<String>> requestPropertiesWrapper = new HashMap<>();

	public HttpURLConnectionWrapper(URL url, HttpURLConnection connection)
	{
		super(url);
		this.connection = connection;
	}

	private String getURLString()
	{
		try
		{
			return (String) Thread.currentThread().getContextClassLoader().loadClass("com.server.framework.security.SecurityUtil").getDeclaredMethod("getURLString", URL.class).invoke(null, getURL());
		}
		catch(Exception ignored)
		{
			return null;
		}
	}

	private void addOrUpdateHttpsLog()
	{
		try
		{
			if(Objects.isNull(httpLogId))
			{
				if(callCompleted)
				{
					return;
				}
				httpLogId = (long) Thread.currentThread().getContextClassLoader().loadClass("com.server.framework.security.SecurityUtil").getDeclaredMethod("addHttpLog", HttpURLConnection.class).invoke(null, this);
			}
			else
			{
				Thread.currentThread().getContextClassLoader().loadClass("com.server.framework.security.SecurityUtil").getDeclaredMethod("updateHttpLog", long.class, HttpURLConnection.class).invoke(null, httpLogId, this);
			}
		}
		catch(Exception e)
		{
			LOGGER.log(Level.SEVERE, "Exception in adding/updating http log", e);
		}
	}

	public void start()
	{

		if(callInitiated)
		{
			return;
		}
		LOGGER.log(Level.INFO, "HTTP call started :: URL : {0} :: Method {1}", new Object[] {getURLString(), connection.getRequestMethod()});
		startedTime = System.currentTimeMillis();
		callInitiated = true;
		addOrUpdateHttpsLog();
	}

	public void end()
	{
		if(callCompleted)
		{
			return;
		}
		callCompleted = true;
		long totalTimeTaken = System.currentTimeMillis() - startedTime;
		LOGGER.log(Level.INFO, "HTTP call completed :: URL : {0} :: StatusCode : {1} :: Duration : {2} second(s)", new Object[] {getURLString(), getResponseCode(), (totalTimeTaken / 1000f)});
		addOrUpdateHttpsLog();
	}

	public void connect() throws IOException
	{
		connection.connect();
	}

	public HttpsURLConnection getHttpsConnection()
	{
		return (HttpsURLConnection) connection;
	}

	public void disconnect()
	{
		end();
		connection.disconnect();
	}

	public boolean equals(Object arg0)
	{
		return connection.equals(arg0);
	}

	public boolean getAllowUserInteraction()
	{
		return connection.getAllowUserInteraction();
	}

	public String getCipherSuite()
	{
		return getHttpsConnection().getCipherSuite();
	}

	public int getConnectTimeout()
	{
		return connection.getConnectTimeout();
	}

	public Object getContent() throws IOException
	{
		return connection.getContent();
	}

	public Object getContent(Class[] arg0) throws IOException
	{
		return connection.getContent(arg0);
	}

	public String getContentEncoding()
	{
		return connection.getContentEncoding();
	}

	public int getContentLength()
	{
		return connection.getContentLength();
	}

	public String getContentType()
	{
		return connection.getContentType();
	}

	public long getDate()
	{
		return connection.getDate();
	}

	public boolean getDefaultUseCaches()
	{
		return connection.getDefaultUseCaches();
	}

	public boolean getDoInput()
	{
		return connection.getDoInput();
	}

	public boolean getDoOutput()
	{
		return connection.getDoOutput();
	}

	public InputStream getErrorStream()
	{
		try
		{
			return getErrorStreamWrapper();
		}
		catch(IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public long getExpiration()
	{
		return connection.getExpiration();
	}

	public String getHeaderField(int arg0)
	{
		start();
		return connection.getHeaderField(arg0);
	}

	public String getHeaderField(String name)
	{
		start();
		return connection.getHeaderField(name);
	}

	public long getHeaderFieldDate(String name, long Default)
	{
		start();
		return connection.getHeaderFieldDate(name, Default);
	}

	public int getHeaderFieldInt(String name, int Default)
	{
		start();
		return connection.getHeaderFieldInt(name, Default);
	}

	public String getHeaderFieldKey(int arg0)
	{
		start();
		return connection.getHeaderFieldKey(arg0);
	}

	public Map<String, List<String>> getHeaderFields()
	{
		start();
		return connection.getHeaderFields();
	}

	public HostnameVerifier getHostnameVerifier()
	{
		return getHttpsConnection().getHostnameVerifier();
	}

	public long getIfModifiedSince()
	{
		return connection.getIfModifiedSince();
	}

	public InputStream getInputStream() throws IOException
	{
		try
		{
			start();
			return getInputStreamWrapper();
		}
		finally
		{
			end();
		}
	}

	private InputStream getInputStreamWrapper() throws IOException
	{
		if(Objects.nonNull(inputStreamWrapper))
		{
			inputStreamWrapper.reset();
			return inputStreamWrapper;
		}
		inputStreamWrapper = new ByteArrayInputStream(connection.getInputStream().readAllBytes());
		return inputStreamWrapper;
	}

	private InputStream getErrorStreamWrapper() throws IOException
	{
		if(Objects.nonNull(errorStreamWrapper))
		{
			errorStreamWrapper.reset();
			return errorStreamWrapper;
		}

		InputStream errorStream = connection.getErrorStream();
		if(Objects.isNull(errorStream))
		{
			return null;
		}
		errorStreamWrapper = new ByteArrayInputStream(errorStream.readAllBytes());
		return errorStreamWrapper;
	}

	public boolean getInstanceFollowRedirects()
	{
		return connection.getInstanceFollowRedirects();
	}

	public long getLastModified()
	{
		return connection.getLastModified();
	}

	public Certificate[] getLocalCertificates()
	{
		return getHttpsConnection().getLocalCertificates();
	}

	public Principal getLocalPrincipal()
	{
		return getHttpsConnection().getLocalPrincipal();
	}

	public OutputStream getOutputStream() throws IOException
	{
		return connection.getOutputStream();
	}

	public Principal getPeerPrincipal() throws SSLPeerUnverifiedException
	{
		return getHttpsConnection().getPeerPrincipal();
	}

	public Permission getPermission() throws IOException
	{
		return connection.getPermission();
	}

	public int getReadTimeout()
	{
		return connection.getReadTimeout();
	}

	public String getRequestMethod()
	{
		return connection.getRequestMethod();
	}

	public Map<String, List<String>> getRequestProperties()
	{
		return requestPropertiesWrapper;
	}

	public String getRequestProperty(String arg0)
	{
		return connection.getRequestProperty(arg0);
	}

	public int getResponseCode()
	{
		try
		{
			start();
			return connection.getResponseCode();
		}
		catch(Exception ignored)
		{
			return -1;
		}
	}

	public String getResponseMessage() throws IOException
	{
		start();
		return connection.getResponseMessage();
	}

	public SSLSocketFactory getSSLSocketFactory()
	{
		return getHttpsConnection().getSSLSocketFactory();
	}

	public Certificate[] getServerCertificates() throws SSLPeerUnverifiedException
	{
		return getHttpsConnection().getServerCertificates();
	}

	public URL getURL()
	{
		return connection.getURL();
	}

	public boolean getUseCaches()
	{
		return connection.getUseCaches();
	}

	public int hashCode()
	{
		return connection.hashCode();
	}

	public void setAllowUserInteraction(boolean arg0)
	{
		connection.setAllowUserInteraction(arg0);
	}

	public void setChunkedStreamingMode(int arg0)
	{
		connection.setChunkedStreamingMode(arg0);
	}

	public void setConnectTimeout(int arg0)
	{
		connection.setConnectTimeout(arg0);
	}

	public void setDefaultUseCaches(boolean arg0)
	{
		connection.setDefaultUseCaches(arg0);
	}

	public void setDoInput(boolean arg0)
	{
		connection.setDoInput(arg0);
	}

	public void setDoOutput(boolean arg0)
	{
		connection.setDoOutput(arg0);
	}

	public void setFixedLengthStreamingMode(int arg0)
	{
		connection.setFixedLengthStreamingMode(arg0);
	}

	public void setHostnameVerifier(HostnameVerifier arg0)
	{
		getHttpsConnection().setHostnameVerifier(arg0);
	}

	public void setIfModifiedSince(long arg0)
	{
		connection.setIfModifiedSince(arg0);
	}

	public void setInstanceFollowRedirects(boolean arg0)
	{
		connection.setInstanceFollowRedirects(arg0);
	}

	public void setReadTimeout(int arg0)
	{
		connection.setReadTimeout(arg0);
	}

	public void setRequestMethod(String arg0) throws ProtocolException
	{
		connection.setRequestMethod(arg0);
	}

	public void setRequestProperty(String arg0, String arg1)
	{
		requestPropertiesWrapper.put(arg0, Collections.singletonList(arg1));
		connection.setRequestProperty(arg0, arg1);
	}

	@Override
	public void addRequestProperty(String key, String value)
	{
		requestPropertiesWrapper.put(key, Collections.singletonList(value));
		connection.setRequestProperty(key, value);
	}

	public void setSSLSocketFactory(SSLSocketFactory arg0)
	{
		getHttpsConnection().setSSLSocketFactory(arg0);
	}

	public void setUseCaches(boolean arg0)
	{
		connection.setUseCaches(arg0);
	}

	public String toString()
	{
		return connection.toString();
	}

	public boolean usingProxy()
	{
		return connection.usingProxy();
	}
}
