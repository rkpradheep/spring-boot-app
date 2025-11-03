package com.server.zoho.workflow.definition;

import com.server.framework.workflow.definition.WorkflowDefinition;
import com.server.framework.workflow.definition.WorkflowTransition;
import com.server.zoho.workflow.model.BuildEventType;
import com.server.framework.workflow.definition.WorkFlowCommonEventType;
import com.server.zoho.workflow.model.BuildStates;
import com.server.zoho.workflow.steps.*;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BuildWorkflowDefinition extends WorkflowDefinition
{

	@Autowired
	private BuildInitiationStep buildInitiationStep;

	@Autowired
	private BuildInProgressStep buildInProgressStep;

	@Autowired
	private MilestoneCreationStep milestoneCreationStep;

	@Autowired
	private ChannelMappingStep channelMappingStep;

	@Autowired
	private NextProductStep nextProductStep;

	@Autowired
	private BuildFailedStep buildFailedStep;

	@Autowired
	private MilestoneFailedStep milestoneFailedStep;

	@Autowired
	private ChannelFailedStep channelFailedStep;

	@Autowired
	private WorkflowFailedStep workflowFailedStep;

	@Autowired
	private WorkflowCompletedStep workflowCompletedStep;

	@Autowired
	private BuildCompletedStep buildCompletedStep;

	@Autowired
	private SDCsezBuildUploadStep sdCsezBuildUploadStep;

	@Autowired
	private SDLocalBuildUploadStep sdLocalBuildUploadStep;

	@Autowired
	private SDPreBuildUploadStep sdPreBuildUploadStep;

	@Override
	protected void initializeDefinition()
	{
		setName("BuildWorkflow");
		setInitialState(BuildStates.BUILD_INITIATION.getValue());
		setSteps(new HashMap<>());
		setTransitions(new HashMap<>());
		defineSteps();
		defineTransitions();
	}

	private void defineSteps()
	{
		addStep(BuildStates.BUILD_INITIATION.getValue(), buildInitiationStep);
		addStep(BuildStates.BUILD_IN_PROGRESS.getValue(), buildInProgressStep);
		addStep(BuildStates.BUILD_COMPLETED.getValue(), buildCompletedStep);
		addStep(BuildStates.BUILD_FAILED.getValue(), buildFailedStep);
		addStep(BuildStates.MILESTONE_CREATION.getValue(), milestoneCreationStep);
		addStep(BuildStates.MILESTONE_FAILED.getValue(), milestoneFailedStep);
		addStep(BuildStates.CHANNEL_MAPPING.getValue(), channelMappingStep);
		addStep(BuildStates.CHANNEL_MAPPING_FAILED.getValue(), channelFailedStep);
		addStep(BuildStates.NEXT_PRODUCT.getValue(), nextProductStep);
		addStep(BuildStates.WORKFLOW_COMPLETED.getValue(), workflowCompletedStep);
		addStep(BuildStates.WORKFLOW_FAILED.getValue(), workflowFailedStep);
		addStep(BuildStates.SD_CSEZ_BUILD_UPLOAD.getValue(), sdCsezBuildUploadStep);
		addStep(BuildStates.SD_LOCAL_BUILD_UPLOAD.getValue(), sdLocalBuildUploadStep);
		addStep(BuildStates.SD_PRE_BUILD_UPLOAD.getValue(), sdPreBuildUploadStep);
	}

	private void defineTransitions()
	{
		addTransition(BuildStates.BUILD_INITIATION.getValue(),
			new WorkflowTransition(BuildStates.BUILD_INITIATION.getValue(), BuildStates.BUILD_IN_PROGRESS.getValue(),
				BuildEventType.BUILD_STARTED.getValue()));

		addTransition(BuildStates.BUILD_IN_PROGRESS.getValue(),
			new WorkflowTransition(BuildStates.BUILD_IN_PROGRESS.getValue(), BuildStates.BUILD_COMPLETED.getValue(),
				BuildEventType.BUILD_COMPLETED.getValue()));

		addTransition(BuildStates.BUILD_INITIATION.getValue(),
			new WorkflowTransition(BuildStates.BUILD_INITIATION.getValue(), BuildStates.BUILD_FAILED.getValue(),
				BuildEventType.BUILD_FAILED.getValue()));

		addTransition(BuildStates.BUILD_IN_PROGRESS.getValue(),
			new WorkflowTransition(BuildStates.BUILD_IN_PROGRESS.getValue(), BuildStates.BUILD_FAILED.getValue(),
				BuildEventType.BUILD_FAILED.getValue()));

		addTransition(BuildStates.BUILD_IN_PROGRESS.getValue(),
			new WorkflowTransition(BuildStates.BUILD_IN_PROGRESS.getValue(), BuildStates.BUILD_IN_PROGRESS.getValue(),
				BuildEventType.BUILD_STATUS_CHECK.getValue()));

		addTransition(BuildStates.BUILD_COMPLETED.getValue(),
			new WorkflowTransition(BuildStates.BUILD_COMPLETED.getValue(), BuildStates.MILESTONE_CREATION.getValue(),
				BuildEventType.MILESTONE_CREATION.getValue()));

		addTransition(BuildStates.MILESTONE_CREATION.getValue(),
			new WorkflowTransition(BuildStates.MILESTONE_CREATION.getValue(), BuildStates.CHANNEL_MAPPING.getValue(),
				BuildEventType.CHANNEL_MAPPING.getValue()));

		addTransition(BuildStates.MILESTONE_CREATION.getValue(),
			new WorkflowTransition(BuildStates.MILESTONE_CREATION.getValue(), BuildStates.SD_CSEZ_BUILD_UPLOAD.getValue(),
				BuildEventType.SD_CSEZ_BUILD_UPLOAD.getValue()));

		addTransition(BuildStates.SD_CSEZ_BUILD_UPLOAD.getValue(),
			new WorkflowTransition(BuildStates.SD_CSEZ_BUILD_UPLOAD.getValue(), BuildStates.SD_LOCAL_BUILD_UPLOAD.getValue(),
				BuildEventType.SD_LOCAL_BUILD_UPLOAD.getValue()));

		addTransition(BuildStates.SD_CSEZ_BUILD_UPLOAD.getValue(),
			new WorkflowTransition(BuildStates.SD_CSEZ_BUILD_UPLOAD_FAILED.getValue(), BuildStates.SD_CSEZ_BUILD_UPLOAD_FAILED.getValue(),
				WorkFlowCommonEventType.WORKFLOW_FAILED.getValue(), true));

		addTransition(BuildStates.SD_LOCAL_BUILD_UPLOAD.getValue(),
			new WorkflowTransition(BuildStates.SD_LOCAL_BUILD_UPLOAD.getValue(), BuildStates.WORKFLOW_COMPLETED.getValue(),
				WorkFlowCommonEventType.WORKFLOW_COMPLETED.getValue(), true));

		addTransition(BuildStates.SD_LOCAL_BUILD_UPLOAD.getValue(),
			new WorkflowTransition(BuildStates.SD_LOCAL_BUILD_UPLOAD_FAILED.getValue(), BuildStates.SD_LOCAL_BUILD_UPLOAD_FAILED.getValue(),
				WorkFlowCommonEventType.WORKFLOW_FAILED.getValue(), true));

		addTransition(BuildStates.BUILD_COMPLETED.getValue(),
			new WorkflowTransition(BuildStates.BUILD_COMPLETED.getValue(), BuildStates.SD_PRE_BUILD_UPLOAD.getValue(),
				BuildEventType.PATCH_BUILD.getValue()));

		addTransition(BuildStates.MILESTONE_CREATION.getValue(),
			new WorkflowTransition(BuildStates.MILESTONE_CREATION.getValue(), BuildStates.MILESTONE_FAILED.getValue(),
				BuildEventType.MILESTONE_FAILED.getValue()));

		addTransition(BuildStates.CHANNEL_MAPPING.getValue(),
			new WorkflowTransition(BuildStates.CHANNEL_MAPPING.getValue(), BuildStates.NEXT_PRODUCT.getValue(),
				BuildEventType.NEXT_PRODUCT.getValue()));

		addTransition(BuildStates.CHANNEL_MAPPING.getValue(),
			new WorkflowTransition(BuildStates.CHANNEL_MAPPING.getValue(), BuildStates.CHANNEL_MAPPING_FAILED.getValue(),
				BuildEventType.CHANNEL_MAPPING_FAILED.getValue()));

		addTransition(BuildStates.NEXT_PRODUCT.getValue(),
			new WorkflowTransition(BuildStates.NEXT_PRODUCT.getValue(), BuildStates.BUILD_INITIATION.getValue(),
				BuildEventType.BUILD_STARTED.getValue()));

		addTransition(BuildStates.NEXT_PRODUCT.getValue(),
			new WorkflowTransition(BuildStates.NEXT_PRODUCT.getValue(), BuildStates.WORKFLOW_COMPLETED.getValue(),
				WorkFlowCommonEventType.WORKFLOW_COMPLETED.getValue(), true));

		addTransition(BuildStates.BUILD_FAILED.getValue(),
			new WorkflowTransition(BuildStates.BUILD_FAILED.getValue(), BuildStates.BUILD_FAILED.getValue(),
				WorkFlowCommonEventType.WORKFLOW_FAILED.getValue(), true));

		addTransition(BuildStates.MILESTONE_FAILED.getValue(),
			new WorkflowTransition(BuildStates.MILESTONE_FAILED.getValue(), BuildStates.MILESTONE_FAILED.getValue(),
				WorkFlowCommonEventType.WORKFLOW_FAILED.getValue(), true));

		addTransition(BuildStates.CHANNEL_MAPPING_FAILED.getValue(),
			new WorkflowTransition(BuildStates.CHANNEL_MAPPING_FAILED.getValue(), BuildStates.CHANNEL_MAPPING_FAILED.getValue(),
				WorkFlowCommonEventType.WORKFLOW_FAILED.getValue(), true));

		addTransition(BuildStates.BUILD_FAILED.getValue(),
			new WorkflowTransition(BuildStates.BUILD_FAILED.getValue(), BuildStates.BUILD_INITIATION.getValue(),
				BuildEventType.RETRY_REQUESTED.getValue()));

		addTransition(BuildStates.MILESTONE_FAILED.getValue(),
			new WorkflowTransition(BuildStates.MILESTONE_FAILED.getValue(), BuildStates.MILESTONE_CREATION.getValue(),
				BuildEventType.RETRY_REQUESTED.getValue()));

		addTransition(BuildStates.CHANNEL_MAPPING_FAILED.getValue(),
			new WorkflowTransition(BuildStates.CHANNEL_MAPPING_FAILED.getValue(), BuildStates.CHANNEL_MAPPING.getValue(),
				BuildEventType.RETRY_REQUESTED.getValue()));

		addTransition(BuildStates.SD_CSEZ_BUILD_UPLOAD_FAILED.getValue(),
			new WorkflowTransition(BuildStates.SD_CSEZ_BUILD_UPLOAD_FAILED.getValue(), BuildStates.SD_CSEZ_BUILD_UPLOAD.getValue(),
				BuildEventType.RETRY_REQUESTED.getValue()));

		addTransition(BuildStates.SD_LOCAL_BUILD_UPLOAD_FAILED.getValue(),
			new WorkflowTransition(BuildStates.SD_LOCAL_BUILD_UPLOAD_FAILED.getValue(), BuildStates.SD_LOCAL_BUILD_UPLOAD.getValue(),
				BuildEventType.RETRY_REQUESTED.getValue(), true));

		addTransition(BuildStates.WORKFLOW_FAILED.getValue(), new WorkflowTransition(BuildStates.WORKFLOW_FAILED.getValue(), BuildStates.BUILD_INITIATION.getValue(),
				BuildEventType.RETRY_REQUESTED.getValue()));

		addTransition(BuildStates.SD_PRE_BUILD_UPLOAD.getValue(),
			new WorkflowTransition(BuildStates.SD_PRE_BUILD_UPLOAD.getValue(), BuildStates.WORKFLOW_COMPLETED.getValue(),
				WorkFlowCommonEventType.WORKFLOW_COMPLETED.getValue(), true));

		addTransition(BuildStates.SD_PRE_BUILD_UPLOAD.getValue(),
			new WorkflowTransition(BuildStates.SD_PRE_BUILD_UPLOAD.getValue(), BuildStates.SD_PRE_BUILD_UPLOAD_FAILED.getValue(),
				WorkFlowCommonEventType.WORKFLOW_FAILED.getValue(), true));

		addTransition(BuildStates.SD_PRE_BUILD_UPLOAD_FAILED.getValue(),
			new WorkflowTransition(BuildStates.SD_PRE_BUILD_UPLOAD_FAILED.getValue(), BuildStates.SD_PRE_BUILD_UPLOAD.getValue(),
				BuildEventType.RETRY_REQUESTED.getValue()));
	}
}
