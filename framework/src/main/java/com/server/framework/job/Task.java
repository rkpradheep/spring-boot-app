package com.server.framework.job;

public interface Task
{
	void run(long jobID) throws Exception;
}
