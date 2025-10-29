package com.server.zoho;

class BuildLog
{
	private String checkout_label;
	private String status;
	private String report_needed;
	private String build_type;
	private String started_from;
	private String customize_info;
	private String success_mail;
	private String error_mail;
	private String security_report_needed;
	private String selected;
	private String product_id;
	private String instant_response;

	public String getCheckoutLabel()
	{
		return checkout_label;
	}

	public void setCheckoutLabel(String checkout_label)
	{
		this.checkout_label = checkout_label;
	}

	public String getStatus()
	{
		return status;
	}

	public void setStatus(String status)
	{
		this.status = status;
	}

	public String getReportNeeded()
	{
		return report_needed;
	}

	public void setReportNeeded(String report_needed)
	{
		this.report_needed = report_needed;
	}

	public String getBuildType()
	{
		return build_type;
	}

	public void setBuildType(String build_type)
	{
		this.build_type = build_type;
	}

	public String getStartedFrom()
	{
		return started_from;
	}

	public void setStartedFrom(String started_from)
	{
		this.started_from = started_from;
	}

	public String getCustomizeInfo()
	{
		return customize_info;
	}

	public void setCustomizeInfo(String customize_info)
	{
		this.customize_info = customize_info;
	}

	public String getSuccessMail()
	{
		return success_mail;
	}

	public void setSuccessMail(String success_mail)
	{
		this.success_mail = success_mail;
	}

	public String getErrorMail()
	{
		return error_mail;
	}

	public void setErrorMail(String error_mail)
	{
		this.error_mail = error_mail;
	}

	public String getSecurityReportNeeded()
	{
		return security_report_needed;
	}

	public void setSecurityReportNeeded(String security_report_needed)
	{
		this.security_report_needed = security_report_needed;
	}

	public String getSelected()
	{
		return selected;
	}

	public void setSelected(String selected)
	{
		this.selected = selected;
	}

	public String getProductId()
	{
		return product_id;
	}

	public void setProductId(String product_id)
	{
		this.product_id = product_id;
	}

	public String getInstantResponse()
	{
		return instant_response;
	}

	public void setInstantResponse(String instant_response)
	{
		this.instant_response = instant_response;
	}
}
