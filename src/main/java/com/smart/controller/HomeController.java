package com.smart.controller;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.dao.UserRepository;
import com.smart.entities.User;
import com.smart.helper.Message;

@Controller
public class HomeController {
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	BCryptPasswordEncoder passwordEncoder;

	@RequestMapping("/")
	public String home(Model model) {
		model.addAttribute("title", "Home-Smart Contact Manager");
		return "home";
	}

	@RequestMapping("/about")
	public String about(Model model) {
		model.addAttribute("title", "About-Smart Contact Manager");
		return "about";
	}

	@RequestMapping("/signup")
	public String signup(Model model) {
		model.addAttribute("title", "Register-Smart Contact Manager");
		model.addAttribute("user", new User());
		return "signup";
	}

	@RequestMapping(value = "/do_register", method = RequestMethod.POST)
	public String register(@Valid @ModelAttribute("user") User user, BindingResult bindingResult,
			@RequestParam(value = "agreement", defaultValue = "false") boolean agreement, Model model,
			HttpSession session) {

		try {
			if (bindingResult.hasErrors()) {
				System.out.println("Error : " + bindingResult);
				return "signup";
			}

			if (!agreement) {
				System.out.println("you have not agreed terms and Condition, .");
				throw new Exception("you have not accept terms and Condition.");
			}
			System.out.println("agreement : " + agreement);
			user.setPassword(passwordEncoder.encode(user.getPassword()));
			user.setImageUrl("profile.jpg");
			user.setEnabled(true);
			user.setRole("ROLE_USER");

			System.out.println("Before Register User : " + user);
			// User result = userService.userRegister(user);
			User saveResult = userRepository.save(user);
			System.out.println("After Registered User : " + saveResult);

			// after successfully registered, form data must return empty
			User emptyUser = new User();
			model.addAttribute("user", emptyUser);
			session.setAttribute("message", new Message("Registration successfully", "alert-success"));

		} catch (Exception e) {
			e.printStackTrace();
			model.addAttribute("user", user);
			session.setAttribute("message", new Message("ohhh..!" + e.getMessage(), "alert-danger"));
			return "signup";
		}
		return "signup";
	}
	@GetMapping("/signin")
	public String login(Model model) {
		model.addAttribute("title", "Login : Smart contact Manager");
		return "login";
	}
}