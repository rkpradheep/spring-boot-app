package com.server.framework.job;

import com.server.framework.common.DateUtil;

public interface Task
{
	void run(long jobID) throws Exception;

	default boolean canAddJobAgain()
	{
		return false;
	}

	default long getDelayBeforeAddJobAgain()
	{
		return DateUtil.ONE_MINUTE_IN_MILLISECOND * 5;
	}
}
