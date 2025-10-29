package com.server.framework.job;

import java.util.Arrays;

import com.server.framework.common.AppContextHolder;
import com.server.framework.error.AppException;

public enum TaskEnum
{
	MAIL("mail", "Mail Scheduler", MailTask.class.getName()),
	REMINDER("reminder", "Invitation Reminder Job", ReminderTask.class.getName()),
	WORKFLOW_TASK("workflow_task", "Workflow Task", WorkFlowTask.class.getName()),
	ZOHO_COMMON_TASK("zoho_common_task", "Zoho Common Task", "com.server.zoho.ZohoCommonTask"),
	PAYOUT_BUILD_AUTOMATION_TASK("payout_build_automation_task", "Payout Build Automation Task", "com.server.zoho.PayoutBuildAutomationTask");

	private final String taskName;
	private final String taskDisplayName;
	private final String taskClassName;

	public String getTaskName()
	{
		return taskName;
	}

	public String getTaskDisplayName()
	{
		return taskDisplayName;
	}

	TaskEnum(String taskName, String taskDisplayName, String taskClassName)
	{
		this.taskName = taskName;
		this.taskDisplayName = taskDisplayName;
		this.taskClassName = taskClassName;
	}

	public static TaskEnum getTaskEnum(String taskName) throws AppException
	{
		return Arrays.stream(values()).filter(taskEnum -> taskEnum.taskName.equals(taskName))
			.findFirst().orElseThrow(() -> new AppException("task with given name doesn't exist"));
	}

	public static Task getTaskInstance(String taskName) throws Exception
	{
		return (Task) AppContextHolder.getBean(Class.forName(getTaskEnum(taskName).taskClassName));
	}
}
