package com.server.framework.job;

public enum JobStatus
{
	JOB_NOT_RUNNING(-1),
	JOB_DISPATCHED(0),
	JOB_RUNNING(1);

	private final int status;

	JobStatus(int status)
	{
		this.status = status;
	}

	public int getStatus()
	{
		return status;
	}
}
