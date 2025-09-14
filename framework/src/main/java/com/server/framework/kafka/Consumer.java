package com.server.framework.kafka;

public class Consumer implements Runnable
{
	private final KafkaBroker broker;
	private final String topic;
	private final String groupName;
	private final String consumerId;
	private volatile boolean running = true;

	public Consumer(KafkaBroker broker, String topic, String groupName, String consumerId)
	{
		this.broker = broker;
		this.topic = topic;
		this.groupName = groupName;
		this.consumerId = consumerId;
		broker.registerConsumer(topic, groupName, consumerId);
	}

	public void stop()
	{
		running = false;
	}

	@Override
	public void run()
	{
		while(running)
		{
			String message = broker.consume(topic, groupName, consumerId);
			if(message != null)
			{
				System.out.println("Consumer [" + groupName + ", " + consumerId + "] received: " + message);
			}
			else
			{
				try
				{
					Thread.sleep(100); // Wait for new messages
				}
				catch(InterruptedException e)
				{
					Thread.currentThread().interrupt();
					break;
				}
			}
		}
	}
}
