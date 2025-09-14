package com.server.zoho.workflow.steps;

import com.server.framework.workflow.definition.WorkflowStep;
import com.server.framework.workflow.model.WorkflowEvent;
import com.server.framework.workflow.model.WorkflowInstance;

import org.springframework.stereotype.Component;

@Component
public class WorkflowCompletedStep extends WorkflowStep
{

	public WorkflowCompletedStep() {
		super();
		setName("Workflow Completed");
		setDescription("Terminal step indicating that the entire workflow has been completed successfully for all products");
		setType(StepType.ACTION);
		setTimeoutSeconds(10);
		setMaxRetries(0);
		setActionClass(this.getClass().getSimpleName());
	}
    
    @Override
    public WorkflowEvent execute(WorkflowInstance instance) {
        return null;
    }
}
