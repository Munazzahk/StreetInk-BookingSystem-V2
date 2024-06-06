package com.example.streetinkbookingsystem.controllers;

import com.example.streetinkbookingsystem.services.EmailService;
import com.example.streetinkbookingsystem.services.LoginService;
import com.example.streetinkbookingsystem.services.TattooArtistService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
public class LoginController {

    @Autowired
    private LoginService loginService;
    @Autowired
    private EmailService emailService;
    @Autowired
    private TattooArtistService tattooArtistService;


    /**
     * @author Munazzah
     * @return View of login page
     */
    @GetMapping("/login")
    public String login() {
        //If you have just run the script for the database
        // for the first time, then you need to run this method to ensure that the
        // passwords are hashed, otherwise you will get an error
        // Go to login, DON'T LOG IN, stop the application, remove this method
        // and run the application again!

        // /!\ ATTENTION : brug kun denne metode hvis programmet køres med lokal database!!!!!
        // hvis du bruger azure databasen skal du IKKE bruge den /!\

        //loginService.hashExistingPasswords();
        return "home/login";
    }

    /**
     * @summary Checks if the username and password matches the ones in database, if yes,
     * then redirects to dashboard and user is logged in, else redirects to login page with error message
     *
     * @author Munazzah
     * @param username Entered username
     * @param password Entered password
     * @param redirectAttributes For displaying error message
     * @return Redirects to either dashboard or login page based on authentication
     */
    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password,
                        RedirectAttributes redirectAttributes) {
        if (loginService.authenticateUser(username, password)){
            return "redirect:/dashboard";
        } else {
            redirectAttributes.addFlashAttribute("message", "Invalid username or password");
            return "redirect:/login";
        }
    }

    /**
     * @author Muanzzah
     * @return view of forgotten-password page
     */
    @GetMapping("/forgotten-password")
    public String forgottenPassword() {
        return "home/forgotten-password";
    }

    /**
     * @summary Page if forgotten password, that will send an email with a new code, if everything is valid
     *
     * @author Munazzah
     * @param email The email the password is sent to, and is also the same that has been used to create the account
     * @param username TattooArtist username
     * @param redirectAttributes For error message
     * @return Redirect to login if username is correct and mail is valid (and the same as the one
     * in the database) else redirects to the forgotten-password page with error message
     */
    @PostMapping("/forgotten-password")
    public String forgottenPassword(@RequestParam String email, @RequestParam String username, RedirectAttributes redirectAttributes) {
        if (tattooArtistService.getTattooArtistByUsername(username) == null) {
            redirectAttributes.addFlashAttribute("message", "Invalid username");
            return "redirect:/forgotten-password";
        }

        if(!emailService.isValidEmail(email, username)) {
            redirectAttributes.addFlashAttribute("message", "Invalid email. Use the one saved in your profile");
            return "redirect:/forgotten-password";
        }

        String newPassword = loginService.randomPassword();
        String mailContent = "Here is your new password: " + newPassword;
        emailService.sendEmail(email, "New Password", mailContent);
        loginService.updatePassword(newPassword, username);

        return "redirect:/login";
    }

}