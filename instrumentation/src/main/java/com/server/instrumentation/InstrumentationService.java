package com.server.instrumentation;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InstrumentationService implements ClassFileTransformer
{
	private static final Logger LOGGER = Logger.getLogger(InstrumentationService.class.getName());

	private final String targetClassName;
	private final String targetMethodName;

	public InstrumentationService(String targetClassName, String targetMethodName)
	{
		this.targetClassName = targetClassName;
		this.targetMethodName = targetMethodName;
	}

	public static void premain(String agentArgs, Instrumentation inst)
	{
		LOGGER.info("[Agent] In premain method");
		inst.addTransformer(new InstrumentationService("com.server.framework.security.SecurityFilter", "doFilter"));
	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)
	{
		byte[] byteCode = classfileBuffer;
		if(!className.equals(this.targetClassName.replaceAll("\\.", "/")))
		{
			return byteCode;
		}

		LOGGER.info("[Agent] Transforming class " + this.targetClassName);
		try
		{
			ClassPool cp = ClassPool.getDefault();
			CtClass cc = cp.makeClass(new ByteArrayInputStream(classfileBuffer));
			CtMethod m = cc.getDeclaredMethod(this.targetMethodName);
			m.addLocalVariable("startTime", CtClass.longType);
			m.insertBefore("startTime = System.currentTimeMillis();");

			StringBuilder endBlock = new StringBuilder();

			m.addLocalVariable("endTime", CtClass.longType);
			endBlock.append("endTime = System.currentTimeMillis();");

			endBlock.append("try {");
			endBlock.append("jakarta.servlet.http.HttpServletRequest httpServletRequest = ((jakarta.servlet.http.HttpServletRequest) request);");
			endBlock.append("jakarta.servlet.http.HttpServletResponse httpServletResponse = ((jakarta.servlet.http.HttpServletResponse) response);");
			endBlock.append("String requestURI = httpServletRequest.getRequestURI().replaceFirst(httpServletRequest.getContextPath(), \"\");");
			endBlock.append("boolean isResource = com.server.framework.security.SecurityUtil.isResourceFetchRequest(httpServletRequest);");
			endBlock.append("if(!isResource){");
			endBlock.append("LOGGER.info(\"Request completed in \" + ((endTime - startTime)/1000f) + \" second(s)\");");
			endBlock.append("}");
			endBlock.append("} catch (Exception e) {LOGGER.log(java.util.logging.Level.INFO,\"Exception occurred- \" + e.toString()); }");

			m.insertAfter(endBlock.toString());

			byteCode = cc.toBytecode();
			cc.detach();

			LOGGER.info("[Agent] Transforming class completed for " + this.targetClassName);
		}
		catch(Throwable e)
		{
			LOGGER.log(Level.SEVERE, "Exception", e);
		}

		return byteCode;
	}
}
