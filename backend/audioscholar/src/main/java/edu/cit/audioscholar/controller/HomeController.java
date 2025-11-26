package edu.cit.audioscholar.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

	@GetMapping("/")
	public String showStatusPage() {
		// Redirect to the external UptimeRobot status page
		return "redirect:https://stats.uptimerobot.com/pJIct6UIpu";
	}
}
