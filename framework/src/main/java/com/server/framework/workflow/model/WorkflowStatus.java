package com.server.framework.workflow.model;

public enum WorkflowStatus
{
    RUNNING,    // Workflow is currently executing
    COMPLETED,  // Workflow completed successfully
    FAILED,     // Workflow failed and can be retried
    SUSPENDED,  // Workflow is suspended (waiting for external input)
    CANCELLED   // Workflow was cancelled
}
