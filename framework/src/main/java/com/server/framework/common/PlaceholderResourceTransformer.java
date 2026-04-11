package com.server.framework.common;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.ResourceTransformer;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.server.framework.security.SecurityUtil;

public class PlaceholderResourceTransformer implements ResourceTransformer
{
	@Override
	public Resource transform(HttpServletRequest request, Resource resource, ResourceTransformerChain chain) throws IOException
	{
		Resource transformed = chain.transform(request, resource);
		String filename = transformed.getFilename();
		if(filename == null)
		{
			return transformed;
		}

		String placeHolderName = null, placeHolderValue = null;

		if(filename.equals("app.html"))
		{
			placeHolderName = "${PUBLIC_IP}";
			placeHolderValue = SecurityUtil.getOriginatingUserIP();
		}
		else if(filename.equals("zohologin.html"))
		{
			placeHolderName = "${REDIRECT_URL}";
			String clientId = AppProperties.getProperty("oauth.local.client.id");
			String redirectUri = AppProperties.getProperty("oauth.local.client.redirecturi");
			String scopes = AppProperties.getProperty("zoho.auth.scopes");
			try
			{
				URIBuilder builder = new URIBuilder("https://accounts.localzoho.com/oauth/v2/auth");
				builder.addParameter("scope", scopes);
				builder.addParameter("client_id", clientId);
				builder.addParameter("response_type", "code");
				builder.addParameter("redirect_uri", redirectUri);
				//builder.addParameter("prompt", "consent");
				builder.addParameter("access_type", "online");
				builder.addParameter("state", SecurityUtil.getCurrentRequest().getParameter("origin"));
				placeHolderValue = builder.toString();
			}
			catch(Exception ignored)
			{
			}
		}

		if(StringUtils.isEmpty(placeHolderValue))
		{
			return transformed;
		}

		try(InputStream is = transformed.getInputStream())
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			is.transferTo(baos);
			String html = baos.toString(StandardCharsets.UTF_8);

			html = html.replace(placeHolderName, placeHolderValue);

			byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
			return new TransformedResource(transformed, bytes);
		}
	}

}
