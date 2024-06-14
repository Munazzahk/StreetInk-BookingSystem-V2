package com.example.streetinkbookingsystem.exceptions;

import com.example.streetinkbookingsystem.models.TattooArtist;
import com.example.streetinkbookingsystem.services.LoginService;
import com.example.streetinkbookingsystem.services.TattooArtistService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @Autowired
    LoginService loginService;
    @Autowired
    TattooArtistService tattooArtistService;
    // Handle all exceptions
    @ExceptionHandler(Exception.class)
    public String handleAllExceptions(Model model, HttpSession session) {
       // For the header
        // Check if the user is logged in
        if (!loginService.isUserLoggedIn(session)) {
            return "redirect:/";
        }
        // Add logged-in user information to the model
        loginService.addLoggedInUserInfo(model, session, tattooArtistService);

        // The error-view
        model.addAttribute("errorMessage", "An unexpected error occurred. Please try again later. " +
                "If the problem persists, then please contact the developers.");
        return "home/custom-error";
    }
}


