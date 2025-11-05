package com.server.framework.common;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.*;

public class SecurityRequestWrapper extends HttpServletRequestWrapper
{

	private final byte[] cachedBody;

	public SecurityRequestWrapper(HttpServletRequest request) throws IOException
	{
		super(request);
		try(InputStream requestInputStream = request.getInputStream())
		{
			this.cachedBody = requestInputStream.readAllBytes();
		}
	}

	@Override
	public ServletInputStream getInputStream()
	{
		return new ServletInputStream()
		{
			private final ByteArrayInputStream buffer = new ByteArrayInputStream(cachedBody);

			@Override
			public boolean isFinished()
			{
				return buffer.available() == 0;
			}

			@Override
			public boolean isReady()
			{
				return true;
			}

			@Override
			public void setReadListener(ReadListener listener)
			{
			}

			@Override
			public int read()
			{
				return buffer.read();
			}
		};
	}

	@Override
	public BufferedReader getReader()
	{
		return new BufferedReader(new InputStreamReader(getInputStream()));
	}
}
