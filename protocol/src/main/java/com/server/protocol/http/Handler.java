package com.server.protocol.http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

public class Handler extends sun.net.www.protocol.http.Handler
{
	protected URLConnection openConnection(URL u, Proxy p) throws IOException
	{
		HttpURLConnection conn = (HttpURLConnection) super.openConnection(u, p);
		return new HttpURLConnectionWrapper(u, conn);
	}
}
