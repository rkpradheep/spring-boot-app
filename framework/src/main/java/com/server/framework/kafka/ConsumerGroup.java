package com.server.framework.kafka;

import java.util.ArrayList;
import java.util.List;

public class ConsumerGroup
{
	private final String groupName;
	private final List<String> consumerIds = new ArrayList<>();
	private int nextConsumerIdx = 0;

	public ConsumerGroup(String groupName)
	{
		this.groupName = groupName;
	}

	public synchronized void addConsumer(String consumerId)
	{
		consumerIds.add(consumerId);
	}

	public synchronized String getNextConsumerId()
	{
		if(consumerIds.isEmpty())
			return null;
		String id = consumerIds.get(nextConsumerIdx);
		nextConsumerIdx = (nextConsumerIdx + 1) % consumerIds.size();
		return id;
	}

	public List<String> getConsumerIds()
	{
		return consumerIds;
	}
}

