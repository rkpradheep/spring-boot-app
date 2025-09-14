package com.server.framework.entity;

import jakarta.persistence.*;

@Entity(name = "BatchTable")
@Table(name = "BatchTable")
public class BatchTableEntity
{
    
    @Id
    @Column(name = "AccountId")
    private Long accountId;
    
    @Column(name = "BatchStart", nullable = false)
    private Long batchStart;

    public BatchTableEntity() {}
    
    public BatchTableEntity(Long accountId, Long batchStart) {
        this.accountId = accountId;
        this.batchStart = batchStart;
    }

    public Long getAccountId() {
        return accountId;
    }
    
    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }
    
    public Long getBatchStart() {
        return batchStart;
    }
    
    public void setBatchStart(Long batchStart) {
        this.batchStart = batchStart;
    }
    
    @Override
    public String toString() {
        return "BatchTable{" +
                "accountId=" + accountId +
                ", batchStart=" + batchStart +
                '}';
    }
}
