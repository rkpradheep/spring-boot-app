package com.server.framework.job;

public class JobMeta {
    private Long id;
    private String taskName;
    private String data;
    private long scheduledTime;
    private boolean isRecurring;
    private int intervalInDays;
    private CustomRunnable runnable;

    private JobMeta(Builder builder) {
        this.id = builder.id;
        this.taskName = builder.taskName;
        this.data = builder.data;
        this.scheduledTime = builder.scheduledTime;
        this.isRecurring = builder.isRecurring;
        this.intervalInDays = builder.intervalInDays;
        this.runnable = builder.runnable;
    }

    public static class Builder {
        private Long id;
        private String taskName;
        private String data;
        private long scheduledTime;
        private boolean isRecurring;
        private int intervalInDays;
        private CustomRunnable runnable;

        public Builder setId(Long id) {
            this.id = id;
            return this;
        }

        public Builder setTaskName(String taskName) {
            this.taskName = taskName;
            return this;
        }

        public Builder setData(String data) {
            this.data = data;
            return this;
        }

        public Builder setScheduledTime(long scheduledTime) {
            this.scheduledTime = scheduledTime;
            return this;
        }

        public Builder setRecurring(boolean isRecurring) {
            this.isRecurring = isRecurring;
            return this;
        }

        public Builder setIntervalInDays(int intervalInDays) {
            this.intervalInDays = intervalInDays;
            return this;
        }

        public Builder setRunnable(CustomRunnable runnable) {
            this.runnable = runnable;
            return this;
        }

        public JobMeta build() {
            return new JobMeta(this);
        }
    }

    public Long getId() {
        return id;
    }

    public String getTaskName() {
        return taskName;
    }

    public String getData() {
        return data;
    }

    public long getScheduledTime() {
        return scheduledTime;
    }

    public boolean isRecurring() {
        return isRecurring;
    }

    public int getIntervalInDays() {
        return intervalInDays;
    }

    public CustomRunnable getRunnable() {
        return runnable;
    }
}
