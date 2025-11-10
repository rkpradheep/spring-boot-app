package com.server.zoho.controller;

import com.server.framework.entity.WorkflowInstanceEntity;
import com.server.framework.error.AppException;
import com.server.framework.service.LockService;
import com.server.framework.workflow.WorkflowEngine;
import com.server.framework.workflow.definition.WorkFlowCommonEventType;
import com.server.zoho.BuildResponse;
import com.server.zoho.ZohoService;
import com.server.zoho.workflow.model.BuildEventType;
import com.server.framework.workflow.model.WorkflowInstance;
import com.server.framework.workflow.model.WorkflowStatus;
import com.server.framework.workflow.model.WorkflowEvent;
import com.server.framework.service.WorkflowService;
import com.server.zoho.IntegService;
import com.server.zoho.entity.BuildMonitorEntity;
import com.server.zoho.entity.BuildProductEntity;
import com.server.zoho.service.BuildMonitorService;
import com.server.zoho.service.BuildProductService;
import com.server.framework.builder.ApiResponseBuilder;
import com.server.zoho.workflow.model.BuildProductStatus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/zoho/workflow")
public class BuildWorkflowController
{

	@Autowired
	private WorkflowEngine workflowEngine;

	@Autowired
	private WorkflowService persistenceService;

	@Autowired
	private IntegService integService;

	@Autowired
	BuildMonitorService buildMonitorService;

	@Autowired
	BuildProductService buildProductService;

	@Autowired LockService lockService;

	@Autowired WorkflowService workflowService;

	@Transactional
	@PostMapping("/build/start")
	public ResponseEntity<Map<String, Object>> startBuildWorkflow(@RequestBody BuildWorkflowRequest request)
	{
		try
		{
			ZohoService.doAuthentication();
			lockService.acquireCommonLock();

			List<WorkflowInstanceEntity> workflowInstances = workflowService.findRunningInstances();
			if(!workflowInstances.isEmpty())
			{
				throw new AppException("A build workflow is already running. Please wait for it to complete before starting a new one.");
			}
			BuildResponse response = integService.scheduleBuilds(request.getProductNames());

			Map<String, Object> data = new HashMap<>();
			data.put("response", response.getText());
			data.put("productNames", request.getProductNames());
			data.put("workflowEngine", "Workflow");

			return ResponseEntity.ok(ApiResponseBuilder.success(response.getText(), data));
		}
		catch(AppException appException)
		{
			throw appException;
		}
		catch(Exception e)
		{
			return ResponseEntity.badRequest().body(ApiResponseBuilder.error("Error starting build workflow: " + e.getMessage(), 400));
		}
	}

	@Transactional
	@PostMapping("/patch-build/start")
	public ResponseEntity<Map<String, Object>> startPatchBuildWorkflow(@RequestParam("product_name") String productName, @RequestParam("branch_name") String branchName) throws Exception
	{
		ZohoService.doAuthentication();
		lockService.acquireCommonLock();

		BuildMonitorEntity monitor = buildMonitorService.createBuildMonitor(Set.of(productName));

		BuildProductEntity firstProduct = buildProductService.getNextPendingProduct(monitor.getId()).orElse(null);

		Map<String, Object> context = new HashMap<>()
		{
			{
				put("monitorId", monitor.getId());
				put("productId", firstProduct.getId());
				put("isPatchBuild", true);
				put("branchName", branchName);
				put("productName", productName);
			}
		};

		String referenceID = monitor.getId().toString();
		workflowEngine.scheduleWorkflow("BuildWorkflow", referenceID, context, ZohoService.getCurrentUserEmail());

		return ResponseEntity.badRequest().body(ApiResponseBuilder.success("Patch Build scheduled successfully", null));
	}

	@Transactional
	@PostMapping("/build/stop/{workflowId}")
	public ResponseEntity<Map<String, Object>> stopBuildWorkflow(@PathVariable("workflowId") String workflowId)
	{
		try
		{
			ZohoService.doAuthentication();
			lockService.acquireCommonLock();

			Optional<WorkflowInstance> instanceOpt = workflowEngine.getInstance(workflowId);

			if(instanceOpt.isEmpty())
			{
				return ResponseEntity.badRequest().body(ApiResponseBuilder.error(
					"Workflow instance not found",
					404
				));
			}
			WorkflowInstance instance = instanceOpt.get();

			if(instance.getStatus() != WorkflowStatus.RUNNING)
			{
				return ResponseEntity.badRequest().body(ApiResponseBuilder.error(
					"Cannot stop a workflow that is not running. Current status: " + instance.getStatus(),
					400
				));
			}

			instance.setStatus(WorkflowStatus.CANCELLED);
			instance.setLastModifiedBy(ZohoService.getCurrentUserEmail());
			instance.setCurrentState(WorkFlowCommonEventType.WORKFLOW_CANCELLED.getValue());
			workflowService.saveInstance(instance);

			workflowService.saveEvent(new WorkflowEvent(WorkFlowCommonEventType.WORKFLOW_CANCELLED), instance.getReferenceID());

			Optional<BuildMonitorEntity> buildMonitorEntity = buildMonitorService.getById(Long.parseLong(instance.getReferenceID()));
			if(buildMonitorEntity.isPresent())
			{
				BuildMonitorEntity monitor = buildMonitorEntity.get();
				monitor.setStatus("CANCELLED");
				monitor.updateLastUpdateTime();
				buildMonitorService.save(monitor);

				List<BuildProductEntity> buildProducts = buildProductService.getProductsForMonitor(monitor.getId());
				for(BuildProductEntity buildProduct : buildProducts)
				{
					if(buildProduct.getStatus().equals(BuildProductStatus.PENDING.getName()))
					{
						buildProduct.setErrorMessage("Build cancelled by user " + ZohoService.getCurrentUserEmail());
						buildProduct.markAsCancelled();
						buildProductService.save(buildProduct);
					}
				}
			}

			return ResponseEntity.ok(ApiResponseBuilder.create().message(
				"Workflow cancellation initiated successfully"
			).build());

		}
		catch(AppException ae)
		{
			throw ae;
		}
		catch(Exception e)
		{
			throw new AppException("Error stopping workflow: " + e.getMessage());
		}

	}

	@GetMapping("/instances/{instanceId}")
	public ResponseEntity<Map<String, Object>> getWorkflowInstance(@PathVariable String instanceId)
	{
		try
		{
			Optional<WorkflowInstance> instanceOpt = workflowEngine.getInstance(instanceId);

			if(instanceOpt.isEmpty())
			{
				return ResponseEntity.badRequest().body(ApiResponseBuilder.error(
					"Workflow instance not found",
					404
				));
			}

			WorkflowInstance instance = instanceOpt.get();

			return ResponseEntity.ok(ApiResponseBuilder.success(
				"Workflow instance retrieved successfully",
				createInstanceResponse(instance)
			));

		}
		catch(Exception e)
		{
			return ResponseEntity.badRequest().body(ApiResponseBuilder.error(
				"Error retrieving workflow instance: " + e.getMessage(),
				400
			));
		}
	}

	@GetMapping("/instances")
	public ResponseEntity<Map<String, Object>> getAllWorkflowInstances()
	{
		try
		{
			List<WorkflowInstance> instances = workflowEngine.getAllInstances();

			List<Map<String, Object>> instanceResponses = instances.stream()
				.map(this::createInstanceResponse)
				.toList();

			Map<String, Object> data = new HashMap<>();
			data.put("instances", instanceResponses);
			data.put("total", instances.size());

			return ResponseEntity.ok(ApiResponseBuilder.success(
				"Workflow instances retrieved successfully",
				data
			));

		}
		catch(Exception e)
		{
			return ResponseEntity.badRequest().body(ApiResponseBuilder.error(
				"Error retrieving workflow instances: " + e.getMessage(),
				400
			));
		}
	}

	@GetMapping("/instances/recent")
	public ResponseEntity<Map<String, Object>> getRecentWorkflowInstances()
	{
		try
		{
			List<WorkflowInstance> instances = persistenceService.getRecentInstances(3);

			List<Map<String, Object>> instanceResponses = instances.stream()
				.map(this::createInstanceResponse)
				.toList();

			Map<String, Object> data = new HashMap<>();
			data.put("instances", instanceResponses);
			data.put("total", instances.size());

			return ResponseEntity.ok(ApiResponseBuilder.success(
				"Recent workflow instances retrieved successfully",
				data
			));

		}
		catch(Exception e)
		{
			return ResponseEntity.badRequest().body(ApiResponseBuilder.error(
				"Error retrieving recent workflow instances: " + e.getMessage(),
				400
			));
		}
	}

	@DeleteMapping("/instances/{instanceId}")
	public ResponseEntity<Map<String, Object>> deleteWorkflow(@PathVariable String instanceId)
	{
		try
		{
			Optional<WorkflowInstance> instanceOpt = workflowEngine.getInstance(instanceId);

			if(instanceOpt.isEmpty())
			{
				return ResponseEntity.badRequest().body(ApiResponseBuilder.error(
					"Workflow instance not found",
					404
				));
			}

			WorkflowInstance instance = instanceOpt.get();

			if(instance.getStatus() == WorkflowStatus.RUNNING)
			{
				return ResponseEntity.badRequest().body(ApiResponseBuilder.error(
					"Cannot delete a running workflow. Please wait for it to complete or fail.",
					400
				));
			}

			workflowEngine.deleteWorkflow(instanceId);

			Optional<BuildMonitorEntity> buildMonitor = buildMonitorService.getById(Long.parseLong(instanceId));
			buildMonitor.ifPresent(monitor -> buildMonitorService.deleteById(monitor.getId()));

			Map<String, Object> data = new HashMap<>();
			data.put("instanceId", instanceId);

			return ResponseEntity.ok(ApiResponseBuilder.success(
				"Workflow deleted successfully",
				data
			));

		}
		catch(Exception e)
		{
			return ResponseEntity.badRequest().body(ApiResponseBuilder.error(
				"Error deleting workflow: " + e.getMessage(),
				400
			));
		}
	}

	@GetMapping("/statistics")
	public ResponseEntity<Map<String, Object>> getWorkflowStatistics()
	{
		try
		{
			WorkflowService.WorkflowStatistics stats = persistenceService.getStatistics();

			Map<String, Object> statistics = new HashMap<>();
			statistics.put("total", stats.getTotal());
			statistics.put("running", stats.getRunning());
			statistics.put("completed", stats.getCompleted());
			statistics.put("failed", stats.getFailed());
			statistics.put("suspended", stats.getSuspended());
			statistics.put("cancelled", stats.getCancelled());
			statistics.put("successRate", stats.getSuccessRate());
			statistics.put("failureRate", stats.getFailureRate());

			return ResponseEntity.ok(ApiResponseBuilder.success(
				"Workflow statistics retrieved successfully",
				statistics
			));

		}
		catch(Exception e)
		{
			return ResponseEntity.badRequest().body(ApiResponseBuilder.error(
				"Error retrieving workflow statistics: " + e.getMessage(),
				400
			));
		}
	}

	@GetMapping("/status/{workflowId}")
	public ResponseEntity<Map<String, Object>> getWorkflowStatus(@PathVariable("workflowId") String workflowId)
	{
		try
		{
			Long monitorId = Long.parseLong(workflowId);

			Optional<BuildMonitorEntity> monitorOpt = buildMonitorService.getById(monitorId);

			if(monitorOpt.isEmpty())
			{
				return ResponseEntity.badRequest().body(ApiResponseBuilder.error(
					"Workflow not found with ID: " + workflowId,
					404
				));
			}

			BuildMonitorEntity monitor = monitorOpt.get();
			List<BuildProductEntity> products = buildProductService.getProductsForMonitor(monitorId);

			Map<String, Object> data = new HashMap<>();
			data.put("workflowId", workflowId);
			data.put("status", monitor.getStatus());
			data.put("startTime", monitor.getStartTime());
			data.put("lastUpdateTime", monitor.getLastUpdateTime());
			data.put("totalProducts", monitor.getTotalProducts());
			data.put("completedProducts", monitor.getCompletedProducts());
			data.put("products", products);

			return ResponseEntity.ok(ApiResponseBuilder.success(
				"Workflow status retrieved successfully",
				data
			));

		}
		catch(Exception e)
		{
			return ResponseEntity.badRequest().body(ApiResponseBuilder.error(
				"Error retrieving workflow status: " + e.getMessage(),
				400
			));
		}
	}

	@PostMapping("/instances/{workflowId}/retry")
	public ResponseEntity<Map<String, Object>> retryWorkflow(@PathVariable("workflowId") String workflowId)
	{
		try
		{
			Optional<WorkflowInstance> instanceOpt = workflowEngine.getInstance(workflowId);

			if(!instanceOpt.isPresent())
			{
				return ResponseEntity.badRequest().body(ApiResponseBuilder.error(
					"Workflow not found with ID: " + workflowId,
					404
				));
			}

			WorkflowInstance instance = instanceOpt.get();

			if(instance.getStatus() != WorkflowStatus.FAILED && !(instance.getStatus() == WorkflowStatus.COMPLETED && "WORKFLOW_FAILED".equals(instance.getCurrentState())))
			{
				return ResponseEntity.badRequest().body(ApiResponseBuilder.error(
					"Workflow is not in FAILED state or COMPLETED with WORKFLOW_FAILED. Current status: " +
						instance.getStatus() + ", Current state: " + instance.getCurrentState(),
					400
				));
			}

			instance.setStatus(WorkflowStatus.RUNNING);
			instance.setLastModifiedBy(ZohoService.getCurrentUserEmail());

			workflowService.saveInstance(instance);

			WorkflowEvent retryEvent = new WorkflowEvent(BuildEventType.RETRY_REQUESTED);

			workflowEngine.processEvent(workflowId, retryEvent);

			Map<String, Object> data = new HashMap<>();
			data.put("workflowId", workflowId);
			data.put("newStatus", "RUNNING");

			return ResponseEntity.ok(ApiResponseBuilder.success(
				"Workflow retry initiated successfully",
				data
			));

		}
		catch(Exception e)
		{
			return ResponseEntity.badRequest().body(ApiResponseBuilder.error(
				"Error retrying workflow: " + e.getMessage(),
				400
			));
		}
	}

	private Map<String, Object> createInstanceResponse(WorkflowInstance instance)
	{
		Map<String, Object> response = new HashMap<>();
		response.put("instanceId", instance.getReferenceID());
		response.put("workflowName", instance.getWorkflowName());
		response.put("currentState", instance.getCurrentState());
		response.put("status", instance.getStatus());
		response.put("startTime", instance.getStartTime());
		response.put("endTime", instance.getEndTime());
		response.put("lastUpdateTime", instance.getLastUpdateTime());
		response.put("duration", instance.getDuration());
		response.put("errorMessage", instance.getErrorMessage());
		response.put("canRetry", instance.canRetry());
		response.put("variables", instance.getVariables());
		response.put("eventHistory", instance.getEventHistory());
		response.put("createdBy", instance.getCreatedBy());
		response.put("lastModifiedBy", instance.getLastModifiedBy());

		return response;
	}

	public static class BuildWorkflowRequest
	{
		private Set<String> productNames;

		public Set<String> getProductNames()
		{
			return productNames;
		}

		public void setProductNames(Set<String> productNames)
		{
			this.productNames = productNames;
		}
	}
}
