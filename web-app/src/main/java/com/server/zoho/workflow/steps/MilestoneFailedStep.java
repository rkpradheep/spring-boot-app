package com.server.zoho.workflow.steps;

import com.server.framework.workflow.definition.WorkflowStep;
import com.server.zoho.entity.BuildProductEntity;
import com.server.zoho.service.BuildProductService;
import com.server.framework.workflow.model.WorkflowEvent;
import com.server.framework.workflow.model.WorkflowInstance;
import com.server.framework.workflow.definition.WorkFlowCommonEventType;
import com.server.zoho.workflow.model.BuildProductStatus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class MilestoneFailedStep extends WorkflowStep
{

	@Autowired
	private BuildProductService buildProductService;

	public MilestoneFailedStep()
	{
		super();
		setName("Milestone Failed");
		setDescription("Handles milestone creation failure by marking the product as failed and transitioning to workflow failure");
		setType(StepType.ACTION);
		setTimeoutSeconds(10);
		setMaxRetries(0);
		setActionClass(this.getClass().getSimpleName());
	}

	@Override
	public WorkflowEvent execute(WorkflowInstance instance)
	{
		Map<String, Object> context = (Map<String, Object>) instance.getContext();
		Long productId = (Long) context.get("productId");
		String errorMessage = (String) context.getOrDefault("error", "Milestone creation failed");

		if(productId != null)
		{
			Optional<BuildProductEntity> productOpt = buildProductService.getById(productId);
			productOpt.ifPresent(product -> buildProductService.markBuildFailed(product, BuildProductStatus.MILESTONE_FAILED.getName(), errorMessage));
		}

		return new WorkflowEvent(WorkFlowCommonEventType.WORKFLOW_FAILED, Map.of("error", "Milestone creation failed: " + errorMessage));
	}
}

