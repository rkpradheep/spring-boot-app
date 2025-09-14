package com.server.zoho.workflow.steps;

import com.server.framework.workflow.definition.WorkflowStep;
import com.server.zoho.entity.BuildProductEntity;
import com.server.zoho.service.BuildProductService;
import com.server.framework.workflow.model.WorkflowEvent;
import com.server.framework.workflow.model.WorkflowInstance;
import com.server.zoho.workflow.model.BuildEventType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class BuildCompletedStep extends WorkflowStep
{
    
    @Autowired
    private BuildProductService buildProductService;

    public BuildCompletedStep() {
        super();
        setName("Build Completed");
        setDescription("Handles successful build completion by marking the product as completed and transitioning to milestone creation");
        setType(StepType.ACTION);
        setTimeoutSeconds(10);
        setMaxRetries(0);
        setActionClass(this.getClass().getSimpleName());
    }
    
    @Override
    public WorkflowEvent execute(WorkflowInstance instance) {
        Map<String, Object> context = (Map<String, Object>) instance.getContext();
        Long productId = (Long) context.get("productId");
        
        if (productId != null) {
            Optional<BuildProductEntity> productOpt = buildProductService.getById(productId);
			productOpt.ifPresent(product -> buildProductService.markBuildSuccess(product));
        }
        
        return new WorkflowEvent(BuildEventType.MILESTONE_CREATION);
    }
}

