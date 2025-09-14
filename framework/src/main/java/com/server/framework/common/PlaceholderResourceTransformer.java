package com.server.framework.common;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.ResourceTransformer;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.server.framework.security.SecurityUtil;

public class PlaceholderResourceTransformer implements ResourceTransformer {
	@Override
	public Resource transform(HttpServletRequest request, Resource resource, ResourceTransformerChain chain) throws IOException {
		Resource transformed = chain.transform(request, resource);
		String filename = transformed.getFilename();
		if (filename == null || !StringUtils.endsWithIgnoreCase(filename, ".html")) {
			return transformed;
		}

		try (InputStream is = transformed.getInputStream()) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			is.transferTo(baos);
			String html = baos.toString(StandardCharsets.UTF_8);

			String publicIp = SecurityUtil.getOriginatingUserIP();
			html = html.replace("${PUBLIC_IP}", publicIp);

			byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
			return new TransformedResource(transformed, bytes);
		}
	}

}
