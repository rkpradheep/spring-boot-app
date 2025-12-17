package com.server.zoho;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.server.framework.common.CommonService;
import com.server.framework.http.HttpService;
import com.server.zoho.controller.BuildWorkflowController;

@Service
public class BuildAutomationService
{
	private static final Logger LOGGER = Logger.getLogger(BuildAutomationService.class.getName());
	@Autowired
	HttpService httpService;

	@Autowired
	IntegService integService;

	@Autowired
	BuildWorkflowController buildWorkflowController;

	public Set<String> startBuildAutomationForPayout() throws Exception
	{
		List<String> payoutProducts = new ArrayList<>((List<String>) ZohoService.getMetaConfig("PAYOUT_PRODUCTS"));
		payoutProducts.remove("payout_server");
		Set<String> productsForBuild = new HashSet<>(ZohoService.getProductsForBuildInitiation(payoutProducts));

		if(productsForBuild.isEmpty() && !ZohoService.generatePayoutChangSetsFromIDC().isEmpty())
		{
			productsForBuild.add("payout_server");
		}

		if(productsForBuild.isEmpty())
		{
			String initiatorEmail = ZohoService.getCurrentUserEmail();
			String initiatorMessage = StringUtils.equals(initiatorEmail, "SCHEDULER") ? initiatorEmail : "{@" + initiatorEmail + "}";
			initiatorMessage = "\n\nInitiated By : " + initiatorMessage;
			ZohoService.postMessageToChannel(CommonService.getDefaultChannelUrl(), "Build cannot be initiated since no products qualified for build." + initiatorMessage);
			LOGGER.info("Build Automation: No products found for build");
			return productsForBuild;
		}

		productsForBuild.add("payout_server");

		BuildWorkflowController.BuildWorkflowRequest buildWorkflowRequest = new BuildWorkflowController.BuildWorkflowRequest();
		buildWorkflowRequest.setProductNames(productsForBuild);

		buildWorkflowController.startBuildWorkflow(buildWorkflowRequest);

		return productsForBuild;
	}
}
