package com.server.framework.service;

import com.server.framework.entity.WorkflowEventEntity;
import com.server.framework.entity.WorkflowInstanceEntity;
import com.server.framework.repository.WorkflowEventRepository;
import com.server.framework.repository.WorkflowInstanceRepository;
import com.server.framework.workflow.model.WorkflowEvent;
import com.server.framework.workflow.model.WorkflowInstance;
import com.server.framework.workflow.model.WorkflowState;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
@Transactional
public class WorkflowService
{

	private static final Logger LOGGER = Logger.getLogger(WorkflowService.class.getName());

	@Autowired
	private WorkflowInstanceRepository workflowInstanceRepository;

	@Autowired
	private WorkflowEventRepository workflowEventRepository;

	public void saveInstance(WorkflowInstance instance)
	{
		try
		{
			Optional<WorkflowInstanceEntity> existingEntityOpt = workflowInstanceRepository.findByReferenceID(instance.getReferenceID());

			WorkflowInstanceEntity entity;
			if(existingEntityOpt.isPresent())
			{
				entity = existingEntityOpt.get();
				updateEntityFromInstance(entity, instance);
			}
			else
			{
				entity = convertToEntity(instance);
				LOGGER.info("Creating new workflow instance: " + instance.getReferenceID());
			}

			entity.updateLastUpdateTime();

			workflowInstanceRepository.save(entity);

		}
		catch(Exception e)
		{
			LOGGER.severe("Error saving workflow instance " + instance.getReferenceID() + ": " + e.getMessage());
			throw new RuntimeException("Failed to save workflow instance", e);
		}
	}

	public Optional<WorkflowInstance> getInstance(String instanceId)
	{
		try
		{
			Optional<WorkflowInstanceEntity> entityOpt = workflowInstanceRepository.findByReferenceID(instanceId);
			if(entityOpt.isPresent())
			{
				WorkflowInstance instance = convertToModel(entityOpt.get());
				return Optional.of(instance);
			}
			return Optional.empty();
		}
		catch(Exception e)
		{
			LOGGER.severe("Error retrieving workflow instance " + instanceId + ": " + e.getMessage());
			return Optional.empty();
		}
	}

	public List<WorkflowInstance> getAllInstances()
	{
		try
		{
			List<WorkflowInstanceEntity> entities = workflowInstanceRepository.findAll();
			return entities.stream()
				.map(this::convertToModel)
				.collect(Collectors.toList());
		}
		catch(Exception e)
		{
			LOGGER.severe("Error retrieving all workflow instances: " + e.getMessage());
			return List.of();
		}
	}

	public List<WorkflowInstance> getInstancesByStatus(WorkflowState status)
	{
		try
		{
			List<WorkflowInstanceEntity> entities = workflowInstanceRepository.findByStatus(status.name());
			return entities.stream()
				.map(this::convertToModel)
				.collect(Collectors.toList());
		}
		catch(Exception e)
		{
			LOGGER.severe("Error retrieving workflow instances by status " + status + ": " + e.getMessage());
			return List.of();
		}
	}

	public List<WorkflowInstance> getRecentInstances(int limit)
	{
		try
		{
			List<WorkflowInstanceEntity> entities = workflowInstanceRepository.findRecentInstancesWithLimit(limit);
			return entities.stream()
				.map(this::convertToModel)
				.collect(Collectors.toList());
		}
		catch(Exception e)
		{
			LOGGER.severe("Error retrieving recent workflow instances: " + e.getMessage());
			return List.of();
		}
	}

	public void deleteInstance(String instanceId)
	{
		try
		{
			LOGGER.info("Deleting workflow instance: " + instanceId);
			Optional<WorkflowInstanceEntity> entityOpt = workflowInstanceRepository.findByReferenceID(instanceId);
			if(entityOpt.isPresent())
			{
				workflowInstanceRepository.deleteById(entityOpt.get().getReferenceID());
				LOGGER.info("Workflow instance deleted from database: " + instanceId);
			}
			else
			{
				LOGGER.warning("Workflow instance not found for deletion: " + instanceId);
			}
		}
		catch(Exception e)
		{
			LOGGER.severe("Error deleting workflow instance " + instanceId + ": " + e.getMessage());
		}
	}


	public void saveEvent(WorkflowEvent event, String instanceId)
	{
		try
		{
			WorkflowEventEntity entity = new WorkflowEventEntity(instanceId, event.getEventType());
			entity.setPayloadFromMap(event.getPayload());
			entity.setSource(event.getSource());
			entity.setCorrelationId(event.getCorrelationId());

			workflowEventRepository.save(entity);
			LOGGER.info("Workflow event saved: " + event.getEventType() + " for instance: " + instanceId);
		}
		catch(Exception e)
		{
			LOGGER.severe("Error saving workflow event: " + e.getMessage());
		}
	}

	public List<WorkflowEvent> getEventsForInstance(String instanceId)
	{
		try
		{
			List<WorkflowEventEntity> entities = workflowEventRepository.findByReferenceIDOrderByTimestampAsc(instanceId);
			return entities.stream()
				.map(this::convertEventToModel)
				.collect(Collectors.toList());
		}
		catch(Exception e)
		{
			LOGGER.severe("Error retrieving workflow events for instance " + instanceId + ": " + e.getMessage());
			return List.of();
		}
	}

	public WorkflowStatistics getStatistics()
	{
		try
		{
			long total = workflowInstanceRepository.count();
			long running = workflowInstanceRepository.countByStatus("RUNNING");
			long completed = workflowInstanceRepository.countByStatus("COMPLETED");
			long failed = workflowInstanceRepository.countByStatus("FAILED");
			long suspended = workflowInstanceRepository.countByStatus("SUSPENDED");
			long cancelled = workflowInstanceRepository.countByStatus("CANCELLED");

			return new WorkflowStatistics(total, running, completed, failed, suspended, cancelled);
		}
		catch(Exception e)
		{
			LOGGER.severe("Error retrieving workflow statistics: " + e.getMessage());
			return new WorkflowStatistics(0, 0, 0, 0, 0, 0);
		}
	}

	private void updateEntityFromInstance(WorkflowInstanceEntity entity, WorkflowInstance instance)
	{
		entity.setReferenceID(instance.getReferenceID());
		entity.setWorkflowName(instance.getWorkflowName());
		entity.setCurrentState(instance.getCurrentState());
		entity.setStatus(instance.getStatus().name());
		entity.setContextFromObject(instance.getContext());
		entity.setVariablesFromMap(instance.getVariables());
		entity.setStartTime(instance.getStartTime());
		entity.setEndTime(instance.getEndTime());
		entity.setErrorMessage(instance.getErrorMessage());
	}

	private WorkflowInstanceEntity convertToEntity(WorkflowInstance instance)
	{
		WorkflowInstanceEntity entity = new WorkflowInstanceEntity(
			instance.getReferenceID(),
			instance.getWorkflowName(),
			instance.getCurrentState(),
			instance.getStatus().name()
		);

		entity.setContextFromObject(instance.getContext());
		entity.setVariablesFromMap(instance.getVariables());
		entity.setStartTime(instance.getStartTime());
		entity.setEndTime(instance.getEndTime());
		entity.setLastUpdateTime(instance.getLastUpdateTime());
		entity.setErrorMessage(instance.getErrorMessage());

		return entity;
	}

	private WorkflowInstance convertToModel(WorkflowInstanceEntity entity)
	{
		WorkflowInstance instance = new WorkflowInstance();

		instance.setReferenceID(entity.getReferenceID());
		instance.setWorkflowName(entity.getWorkflowName());
		instance.setCurrentState(entity.getCurrentState());
		instance.setStatus(WorkflowState.valueOf(entity.getStatus()));
		instance.setContext(entity.getContextAsObject());
		instance.setVariables(entity.getVariablesAsMap());
		instance.setStartTime(entity.getStartTime());
		instance.setEndTime(entity.getEndTime());
		instance.setLastUpdateTime(entity.getLastUpdateTime());
		instance.setErrorMessage(entity.getErrorMessage());

		List<WorkflowEvent> events = getEventsForInstance(entity.getReferenceID());
		instance.setEventHistory(events);

		return instance;
	}

	private WorkflowEvent convertEventToModel(WorkflowEventEntity entity)
	{
		WorkflowEvent event = new WorkflowEvent(entity.getEventType(), entity.getPayloadAsMap());
		event.setSource(entity.getSource());
		event.setCorrelationId(entity.getCorrelationId());
		event.setTimestamp(entity.getTimestamp());
		return event;
	}

	public static class WorkflowStatistics
	{
		private final long total;
		private final long running;
		private final long completed;
		private final long failed;
		private final long suspended;
		private final long cancelled;

		public WorkflowStatistics(long total, long running, long completed, long failed, long suspended, long cancelled)
		{
			this.total = total;
			this.running = running;
			this.completed = completed;
			this.failed = failed;
			this.suspended = suspended;
			this.cancelled = cancelled;
		}

		public long getTotal()
		{
			return total;
		}

		public long getRunning()
		{
			return running;
		}

		public long getCompleted()
		{
			return completed;
		}

		public long getFailed()
		{
			return failed;
		}

		public long getSuspended()
		{
			return suspended;
		}

		public long getCancelled()
		{
			return cancelled;
		}

		public double getSuccessRate()
		{
			return total > 0 ? (double) completed / total * 100 : 0;
		}

		public double getFailureRate()
		{
			return total > 0 ? (double) failed / total * 100 : 0;
		}
	}
}
