package com.server.framework.entity;

import jakarta.persistence.*;

import org.hibernate.annotations.GenericGenerator;

@Entity(name = "Job")
@Table(name = "Job")
public class JobEntity
{
    
    @Id
    @GeneratedValue(generator = "custom-id")
    @GenericGenerator(name = "custom-id", type = com.server.framework.id.CustomIdGenerator.class)
    @Column(name = "Id")
    private Long id;
    
    @Column(name = "TaskName", nullable = false, length = 255)
    private String taskName;
    
    @Column(name = "Data", nullable = false, columnDefinition = "LONGTEXT")
    private String data;
    
    @Column(name = "ScheduledTime", nullable = false)
    private Long scheduledTime;
    
    @Column(name = "DayInterval", nullable = false)
    private Integer dayInterval;
    
    @Column(name = "IsRecurring", nullable = false)
    private Boolean isRecurring = false;

    @Column(name = "IsRunning", nullable = false)
    private Boolean isRunning = false;

    public JobEntity() {}
    
    public JobEntity(String taskName, String data, Long scheduledTime, Integer dayInterval, Boolean isRecurring) {
        this.taskName = taskName;
        this.data = data;
        this.scheduledTime = scheduledTime;
        this.dayInterval = dayInterval;
        this.isRecurring = isRecurring;
    }

    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTaskName() {
        return taskName;
    }
    
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }
    
    public String getData() {
        return data;
    }
    
    public void setData(String data) {
        this.data = data;
    }
    
    public Long getScheduledTime() {
        return scheduledTime;
    }
    
    public void setScheduledTime(Long scheduledTime) {
        this.scheduledTime = scheduledTime;
    }
    
    public Integer getDayInterval() {
        return dayInterval;
    }
    
    public void setDayInterval(Integer dayInterval) {
        this.dayInterval = dayInterval;
    }
    
    public Boolean getIsRecurring() {
        return isRecurring;
    }
    
    public void setIsRecurring(Boolean isRecurring) {
        this.isRecurring = isRecurring;
    }

    public Boolean getIsRunning()
    {
        return isRunning;
    }

    public void setIsRunning(Boolean isRunning) {
        this.isRunning = isRunning;
    }
    
    @Override
    public String toString() {
        return "Job{" +
                "id=" + id +
                ", taskName='" + taskName + '\'' +
                ", scheduledTime=" + scheduledTime +
                ", dayInterval=" + dayInterval +
                ", isRecurring=" + isRecurring +
                ", isRunning=" + isRunning +
                '}';
    }
}
