package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

@Controller
@RequestMapping("/user")
public class UserController {
	

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
	
	@Autowired
	private ContactRepository contactRepository;

	@Autowired
	private UserRepository userRepository;

	// This method run for every method index, add_contact or etc.
	// Method for adding common data for response.
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String userName = principal.getName();
		User user = userRepository.getUserByUserName(userName);
		System.out.println("User :" + user);
		model.addAttribute("user", user);
		model.addAttribute("title", "User Dashboard");
	}

	/* User home handler */
	@RequestMapping("/index")
	public String dashBoard(Model model, Principal principal) {
		model.addAttribute("title", "Home Contact");
		return "user/user_dashboard";
	}

	// Open add form handler
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());
		return "user/add_contact_form";

	}

	// processing add contact form
	@PostMapping("/process-contact")
	public String processAddContact(@ModelAttribute Contact contact,
			@RequestParam("profileImage") MultipartFile multipartFile, 
			Principal principal,HttpSession session) {
		try {
			String name = principal.getName();
			User user = this.userRepository.getUserByUserName(name);
			
			// processing and uploading file
			 if (multipartFile.isEmpty()) {
				   // if the file is empty then try our message
	                System.out.println("File not Uploaded");
	                contact.setImage("contact.png");
	                
			 }
			 else {
	                contact.setImage(multipartFile.getOriginalFilename());
	                // image save to static folder
	                File saveFile = new ClassPathResource("static/image").getFile();

	                Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + multipartFile.getOriginalFilename());
	                Files.copy(multipartFile.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
	                System.out.println("File is Uploaded");
	            }
			
			
			contact.setUser(user);
			user.getContact().add(contact);
			this.userRepository.save(user);

			System.out.println("Data " + contact);
			System.out.println("Added to data base");
			// message success......
		     session.setAttribute("message",
			 new Message("Your Contact is Successfully added!!! Add More...", "success"));
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error :" + e.getMessage());
			// error message
		    session.setAttribute("message", new Message("Something went wrong !! Try Again....", "danger"));
		}
		return "user/add_contact_form";
	}
	// show contacts handler
		// per page=5[n]
		// current page=0[page]
	    @GetMapping("/show-contacts/{page}")
	    public String showContacts(@PathVariable("page") Integer page, Model m, Principal principal ){
			
	    m.addAttribute("title", "Show User Contact");
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		Pageable pageable = PageRequest.of(page, 3);
		Page<Contact> contacts = this.contactRepository.findcontactByUser(user.getId(),pageable);
		
		m.addAttribute("contacts",contacts);
		m.addAttribute("currentPage", page);
		m.addAttribute("totalPages", contacts.getTotalPages());
			
			return "user/show_contacts";
		}

	    // Showing particular contact details
	    @RequestMapping("/{cId}/contact")
	    public String showContactDetails(@PathVariable("cId") Integer cId, Model model, Principal principal) {
	        // System.out.println("Cid:"+cid);
	        

	        Optional<Contact> contactOptional = this.contactRepository.findById(cId);


	        if (contactOptional.isPresent()) {
	            Contact contact = contactOptional.get();

	            // get current user
	            String username = principal.getName();
	            User user = this.userRepository.getUserByUserName(username);

	            // show contact only current user
	            if (user.getId() == contact.getUser().getId()) {
	                model.addAttribute("contact", contact);
	                model.addAttribute("title", contact.getName());
	            }
	        }

	        return "user/contact_details";
	    }
	 // delete contact handler
	 // delete contact handler
		@GetMapping("/delete/{cId}")
		public String deleteContact(@PathVariable("cId") Integer cId, Model model, Principal principal,
				HttpSession session) {
			Contact contact = this.contactRepository.findById(cId).get();
			System.out.println("Contact " + contact.getcId());
			// contact.setUser(null);
			// String userName = principal.getName();
			// User user = this.userRepository.getUserByUserName(userName);

			/*
			 * if(user.getId()==contact.getUser().getId()) {
			 * this.contactRepository.delete(contact); System.out.println("deleted");
			 * session.setAttribute("message", new
			 * Message("Contact deleted successfully...","success"));
			 * 
			 * }
			 */
			// this.contactRepository.delete(contact);
			User user = this.userRepository.getUserByUserName(principal.getName());
			user.getContact().remove(contact);
			this.userRepository.save(user);

			System.out.println("deleted");
			session.setAttribute("message", new Message("Contact deleted successfully...", "success"));

			return "redirect:/user/show-contacts/0";

		}
		// open update form handler
		@PostMapping("/update-contact/{cId}")
		public String updateForm(@PathVariable("cId") Integer cId, Model m) {
			m.addAttribute("title", "Update contact");
			Contact contact = this.contactRepository.findById(cId).get();
			m.addAttribute("contact", contact);
			return "user/update_form";
		}

		// update contact handler
		@RequestMapping(value = "/process-update", method = RequestMethod.POST)
		public String updateHandler(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
				Model m, HttpSession session, Principal principal) {

			try {
				// old contact details
				Contact oldcontactDetail = this.contactRepository.findById(contact.getcId()).get();
				// image
				if (!file.isEmpty()) {
					// file work
					// rewrite
					// delete old photo
					File deleteFile = new ClassPathResource("static/image").getFile();
					File file1 = new File(deleteFile, oldcontactDetail.getImage());
					file1.delete();

					// update new photo
					File saveFile = new ClassPathResource("static/image").getFile();
					Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
					Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
					contact.setImage(file.getOriginalFilename());
				} else {
					contact.setImage(oldcontactDetail.getImage());
				}
				User user = this.userRepository.getUserByUserName(principal.getName());
				contact.setUser(user);
				this.contactRepository.save(contact);
				session.setAttribute("message", new Message("Your contact is updated...", "success"));

			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.print("CONTACT NAME " + contact.getName());
			System.out.print("CONTACT ID " + contact.getcId());
			return "redirect:/user/" + contact.getcId() + "/contact";
		}
		// your profile handler
		@GetMapping("/profile")
		public String yourProfile(Model model) {
			model.addAttribute("title", "Profile Page");
			return "user/profile";
		}
//		// open settings handler
		@GetMapping("/settings")
		public String openSetting() {
			return "user/settings";
		}
		// change password handler
	    @PostMapping("/change-password")
	    public String changePassword(Model model, Principal principal, HttpSession session,
	                                 @RequestParam("oldPassword") String oldPassword, @RequestParam("newPassword") String newPassword) {

	        String username = principal.getName();
	        User currentUser = this.userRepository.getUserByUserName(username);

	        if (this.bCryptPasswordEncoder.matches(oldPassword, currentUser.getPassword())) {
	            currentUser.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
	            this.userRepository.save(currentUser);
	            session.setAttribute("message", new Message("Your password is successfully changed...", "alert-success"));

	        } else {
	            session.setAttribute("message", new Message("Your old password is wrong!!", "alert-danger"));
	            return "redirect:/user/settings";
	        }

	        return "redirect:/user/index";

	    }
}