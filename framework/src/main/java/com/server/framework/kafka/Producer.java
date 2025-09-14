package com.server.framework.kafka;

public class Producer
{
	private final KafkaBroker broker;

	public Producer(KafkaBroker broker)
	{
		this.broker = broker;
	}

	public void send(String topic, String message)
	{
		broker.publish(topic, message);
	}
}

