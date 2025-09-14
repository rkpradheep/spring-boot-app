package com.server.zoho.workflow.steps;

import com.server.framework.workflow.definition.WorkflowStep;
import com.server.zoho.entity.BuildMonitorEntity;
import com.server.zoho.entity.BuildProductEntity;
import com.server.zoho.service.BuildMonitorService;
import com.server.zoho.service.BuildProductService;
import com.server.framework.workflow.model.WorkflowEvent;
import com.server.framework.workflow.model.WorkflowInstance;
import com.server.framework.workflow.definition.WorkFlowCommonEventType;
import com.server.zoho.workflow.model.BuildEventType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class NextProductStep extends WorkflowStep
{

	@Autowired
	private BuildProductService buildProductService;

	@Autowired
	private BuildMonitorService buildMonitorService;

	public NextProductStep()
	{
		super();
		setName("Next Product");
		setDescription("Determines the next product to build or marks the workflow as completed if no more products");
		setType(WorkflowStep.StepType.DECISION);
		setTimeoutSeconds(30);
		setMaxRetries(0);
		setActionClass(this.getClass().getSimpleName());
	}

	@Override
	public WorkflowEvent execute(WorkflowInstance instance)
	{
		Map<String, Object> context = (Map<String, Object>) instance.getContext();
		Long monitorId = (Long) context.get("monitorId");

		if(monitorId != null)
		{
			try
			{
				Optional<BuildProductEntity> nextProductOpt = buildProductService.getNextPendingProduct(monitorId);

				if(nextProductOpt.isPresent())
				{
					BuildProductEntity nextProduct = nextProductOpt.get();
					context.put("productId", nextProduct.getId());
					context.put("productName", nextProduct.getProductName());

					BuildMonitorEntity monitor = buildMonitorService.getById(monitorId).orElse(null);
					if(monitor != null)
					{
						monitor.setCompletedProducts(monitor.getCompletedProducts() + 1);
						buildMonitorService.save(monitor);
					}

					return new WorkflowEvent(BuildEventType.BUILD_STARTED, Map.of("nextProduct", nextProduct.getProductName()));
				}
				else
				{
					BuildMonitorEntity monitor = buildMonitorService.getById(monitorId).orElse(null);
					if(monitor != null)
					{
						monitor.setStatus("COMPLETED");
						buildMonitorService.save(monitor);
					}

					return new WorkflowEvent(WorkFlowCommonEventType.WORKFLOW_COMPLETED, Map.of("message", "All products completed"));
				}
			}
			catch(Exception e)
			{
				return new WorkflowEvent(WorkFlowCommonEventType.WORKFLOW_FAILED, Map.of("error", "Failed to get next product: " + e.getMessage()));
			}
		}

		return new WorkflowEvent(WorkFlowCommonEventType.WORKFLOW_FAILED, Map.of("error", "Missing monitorId"));
	}
}

