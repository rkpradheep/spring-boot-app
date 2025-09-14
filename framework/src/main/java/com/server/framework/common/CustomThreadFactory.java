package com.server.framework.common;

public class CustomThreadFactory implements java.util.concurrent.ThreadFactory {
		private final String namePrefix;
		private int count = 0;

		public CustomThreadFactory(String namePrefix) {
			this.namePrefix = namePrefix;
		}

		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r, namePrefix + count++);
			thread.setDaemon(false);
			return thread;
		}
	}
