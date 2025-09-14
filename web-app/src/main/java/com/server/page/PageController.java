package com.server.page;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Controller
public class PageController
{

	@GetMapping({"/", "/app"})
	public String home(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		return "forward:/app.html";
	}

	//    @GetMapping("/login")
	//    public RedirectView login(HttpServletRequest request, HttpServletResponse response) throws IOException {
	//        RedirectView redirectView = new RedirectView("/login.html");
	//        redirectView.setPropagateQueryParams(true);
	//        return redirectView;
	//    }
	@GetMapping("/login")
	public String login() throws IOException
	{
		return "forward:/login.html";
	}

	@GetMapping("/zoho")
	public String zoho() throws IOException
	{
		return "forward:/zoho/zoho.html";
	}

	@GetMapping("/zoho/build-tool")
	public String zohoBuildUpdate()
	{
		return "forward:/zoho/workflow-manager.html";
	}

	@GetMapping("/admin/login")
	public String adminLogin()
	{
		return "forward:/login.html";
	}

	@GetMapping("/csv")
	public String csvPage()
	{
		return "forward:/csv.html";
	}

	@GetMapping("/hotswap")
	public String hotswapPage()
	{
		return "forward:/hotswap.html";
	}

	@GetMapping("/network")
	public String networkPage()
	{
		return "forward:/network.html";
	}

	@GetMapping("/livelogs")
	public String livelogsPage()
	{
		return "forward:/livelogs.html";
	}

	@GetMapping("/freessl")
	public String freesslPage()
	{
		return "forward:/freessl.html";
	}

	@GetMapping("/snakegame")
	public String snakegamePage()
	{
		return "forward:/snakegame.html";
	}

	@GetMapping("/stats")
	public String statsPage()
	{
		return "forward:/stats.html";
	}

	@GetMapping("/sasstats")
	public String sasstatsPage()
	{
		return "forward:/sasstats.html";
	}

	@GetMapping("/payoutlogs")
	public String payoutlogsPage()
	{
		return "forward:/payoutlogs.html";
	}

	@GetMapping({"/dbtool.jsp"})
	public String dbtoolJspPage()
	{
		return "redirect:/zoho/db-tool";
	}

	@GetMapping({"/zoho/db-tool"})
	public String zohoDBTool()
	{
		return "forward:/zoho/dbtool.html";
	}

	@GetMapping("/admin/dbtool")
	public String adminDbtoolJspPage()
	{
		return "redirect:/admin/db-tool";
	}

	@GetMapping("/admin/db-tool")
	public String adminDbtoolPage()
	{
		return "forward:/admin/dbtool.html";
	}

	@GetMapping("/commandExecutor")
	public String commandExecutorJspPage()
	{
		return "forward:/commandExecutor.html";
	}

	@GetMapping("/tokenGen")
	public String tokenGenJspPage()
	{
		return "redirect:/zoho/oauth-tool";
	}

	@GetMapping("/zoho/oauth-tool")
	public String zohoOAuthTool()
	{
		return "forward:/zoho/tokenGen.html";
	}

	@GetMapping("/tokenGenCustom")
	public String tokenGenCustomJspPage()
	{
		return "forward:/tokenGenCustom.html";
	}
}
