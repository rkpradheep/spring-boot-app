package com.server.framework.kafka;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class KafkaBroker
{
	private final Map<String, List<String>> topics = new ConcurrentHashMap<>();
	// groupName -> (topic -> offset)
	private final Map<String, Map<String, Integer>> groupOffsets = new ConcurrentHashMap<>();
	private final Map<String, ConsumerGroup> consumerGroups = new ConcurrentHashMap<>(); // groupName -> ConsumerGroup
	private final Map<String, Map<String, Map<String, Queue<String>>>> consumerQueues = new ConcurrentHashMap<>(); // topic -> groupName -> consumerId -> queue

	public void createTopic(String topic)
	{
		topics.putIfAbsent(topic, Collections.synchronizedList(new ArrayList<>()));
	}

	public void registerConsumerGroup(String groupName, String topic)
	{
		groupOffsets.putIfAbsent(groupName, new ConcurrentHashMap<>());
		groupOffsets.get(groupName).putIfAbsent(topic, 0);
	}

	public void registerConsumer(String topic, String groupName, String consumerId)
	{
		consumerGroups.putIfAbsent(groupName, new ConsumerGroup(groupName));
		consumerGroups.get(groupName).addConsumer(consumerId);
		consumerQueues.putIfAbsent(topic, new ConcurrentHashMap<>());
		consumerQueues.get(topic).putIfAbsent(groupName, new ConcurrentHashMap<>());
		consumerQueues.get(topic).get(groupName).putIfAbsent(consumerId, new ConcurrentLinkedQueue<>());
	}

	public void publish(String topic, String message)
	{
		// Assign message to next consumer in each group (round-robin)
		if(!consumerQueues.containsKey(topic))
			return;
		Map<String, Map<String, Queue<String>>> groupMap = consumerQueues.get(topic);
		for(String groupName : groupMap.keySet())
		{
			ConsumerGroup group = consumerGroups.get(groupName);
			if(group == null)
				continue;
			String consumerId = group.getNextConsumerId();
			if(consumerId == null)
				continue;
			Queue<String> queue = groupMap.get(groupName).get(consumerId);
			if(queue != null)
			{
				queue.offer(message);
			}
		}
	}

	// Returns next message for the group and advances offset
	public synchronized String consume(String topic, String groupName)
	{
		List<String> messages = topics.get(topic);
		Map<String, Integer> offsets = groupOffsets.get(groupName);
		if(messages != null && offsets != null)
		{
			int offset = offsets.getOrDefault(topic, 0);
			if(offset < messages.size())
			{
				String msg = messages.get(offset);
				offsets.put(topic, offset + 1);
				return msg;
			}
		}
		return null;
	}

	public String consume(String topic, String groupName, String consumerId)
	{
		if(!consumerQueues.containsKey(topic))
			return null;
		Map<String, Map<String, Queue<String>>> groupMap = consumerQueues.get(topic);
		if(!groupMap.containsKey(groupName))
			return null;
		Map<String, Queue<String>> consumerMap = groupMap.get(groupName);
		Queue<String> queue = consumerMap.get(consumerId);
		if(queue == null)
			return null;
		return queue.poll();
	}

	public int getGroupOffset(String topic, String groupName)
	{
		Map<String, Integer> offsets = groupOffsets.get(groupName);
		return offsets != null ? offsets.getOrDefault(topic, 0) : 0;
	}

	public int getLatestOffset(String topic)
	{
		List<String> messages = topics.get(topic);
		return messages != null ? messages.size() - 1 : -1;
	}
}
