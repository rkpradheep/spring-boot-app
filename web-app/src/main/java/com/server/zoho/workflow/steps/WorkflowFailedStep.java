package com.server.zoho.workflow.steps;

import com.server.framework.workflow.definition.WorkflowStep;
import com.server.zoho.service.BuildMonitorService;
import com.server.framework.workflow.model.WorkflowEvent;
import com.server.framework.workflow.model.WorkflowInstance;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WorkflowFailedStep extends WorkflowStep
{

	@Autowired
	private BuildMonitorService buildMonitorService;

	public WorkflowFailedStep() {
		super();
		setName("Workflow Failed");
		setDescription("Handles overall workflow failure and provides retry mechanism to reset monitor status");
		setType(StepType.ACTION);
		setTimeoutSeconds(30);
		setMaxRetries(0);
		setActionClass(this.getClass().getSimpleName());
	}
    
    @Override
    public WorkflowEvent execute(WorkflowInstance instance) {
        return null;
    }
}

