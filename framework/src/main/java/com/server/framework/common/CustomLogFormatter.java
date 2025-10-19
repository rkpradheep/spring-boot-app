package com.server.framework.common;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.LayoutBase;

public class CustomLogFormatter extends LayoutBase<ILoggingEvent>
{

	private static final int MAX_CAUSE_DEPTH = 10;

	@Override
	public String doLayout(ILoggingEvent event)
	{
		StringBuilder sb = new StringBuilder();

		sb.append(System.lineSeparator());
		sb.append(System.lineSeparator());

		sb.append(Thread.currentThread().getName());
		sb.append(" ----> ");

		sb.append(DateUtil.getFormattedTime(event.getTimeStamp(), DateUtil.DATE_WITH_TIME_SECONDS_FORMAT));
		sb.append(" ");

		String level = event.getLevel().toString();
		sb.append(String.format("%-5s", level));

		sb.append(" ");
		sb.append(event.getLoggerName());
		sb.append(" - ");

		sb.append(event.getFormattedMessage());
		if(event.getThrowableProxy() != null)
		{
			appendThrowableWithCause(sb, event.getThrowableProxy(), 0);
		}

		sb.append(System.lineSeparator());
		sb.append(System.lineSeparator());

		return sb.toString();
	}

	private void appendThrowableWithCause(StringBuilder sb, ch.qos.logback.classic.spi.IThrowableProxy throwableProxy, int depth)
	{
		if(throwableProxy == null || depth > MAX_CAUSE_DEPTH)
		{
			if(depth > MAX_CAUSE_DEPTH)
			{
				sb.append(System.lineSeparator());
				sb.append("  ".repeat(depth));
				sb.append("... (cause chain truncated at depth ").append(MAX_CAUSE_DEPTH).append(")");
				sb.append(System.lineSeparator());
			}
			return;
		}

		String indent = "  ".repeat(depth);

		sb.append(System.lineSeparator());
		sb.append(indent);

		if(depth == 0)
		{
			sb.append("Exception: ");
		}
		else
		{
			sb.append("Caused by: ");
		}

		sb.append(throwableProxy.getClassName());

		if(throwableProxy.getMessage() != null)
		{
			sb.append(": ");
			sb.append(throwableProxy.getMessage());
		}
		if(throwableProxy.getStackTraceElementProxyArray() != null)
		{
			sb.append(System.lineSeparator());
			for(int i = 0; i < throwableProxy.getStackTraceElementProxyArray().length; i++)
			{
				sb.append(indent);
				sb.append("\tat ");
				sb.append(throwableProxy.getStackTraceElementProxyArray()[i].toString());
				sb.append(System.lineSeparator());
			}
		}

		if(throwableProxy.getCommonFrames() > 0)
		{
			sb.append(indent);
			sb.append("\t... ");
			sb.append(throwableProxy.getCommonFrames());
			sb.append(" more");
			sb.append(System.lineSeparator());
		}
		if(throwableProxy.getSuppressed() != null && throwableProxy.getSuppressed().length > 0)
		{
			sb.append(indent);
			sb.append("Suppressed: ");
			sb.append(throwableProxy.getSuppressed().length);
			sb.append(" exception(s)");
			sb.append(System.lineSeparator());

			for(IThrowableProxy suppressed : throwableProxy.getSuppressed())
			{
				appendThrowableWithCause(sb, suppressed, depth + 1);
			}
		}

		if(throwableProxy.getCause() != null)
		{
			appendThrowableWithCause(sb, throwableProxy.getCause(), depth + 1);
		}
	}
}
