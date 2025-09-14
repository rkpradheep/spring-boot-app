package com.server.framework.kafka;

import java.util.Scanner;

public class Kafka
{
	public static void main(String[] args)
	{
		KafkaBroker broker = new KafkaBroker();
		String topic = "test-topic";
		broker.createTopic(topic);

		Producer producer = new Producer(broker);

		// Create consumers in groupA (load balancing)
		Consumer consumerA1 = new Consumer(broker, topic, "groupA", "A1");
		Consumer consumerA2 = new Consumer(broker, topic, "groupA", "A2");
		Thread threadA1 = new Thread(consumerA1, "ConsumerA1");
		Thread threadA2 = new Thread(consumerA2, "ConsumerA2");

		// Create consumer in groupB (independent consumption)
		Consumer consumerB1 = new Consumer(broker, topic, "groupB", "B1");
		Thread threadB1 = new Thread(consumerB1, "ConsumerB1");

		threadA1.start();
		threadA2.start();
		threadB1.start();

		Scanner scanner = new Scanner(System.in);
		System.out.println("Type messages to publish. Type 'exit' to quit.");
		while(true)
		{
			System.out.print("Publish: ");
			String input = scanner.nextLine();
			if(input.equalsIgnoreCase("exit"))
			{
				break;
			}
			producer.send(topic, input);
		}
		scanner.close();

		// Stop all consumer threads
		consumerA1.stop();
		consumerA2.stop();
		consumerB1.stop();
		threadA1.interrupt();
		threadA2.interrupt();
		threadB1.interrupt();
		try
		{
			threadA1.join();
			threadA2.join();
			threadB1.join();
		}
		catch(InterruptedException e)
		{
			Thread.currentThread().interrupt();
		}
	}
}
