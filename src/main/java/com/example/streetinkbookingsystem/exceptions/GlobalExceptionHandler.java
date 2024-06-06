package com.example.streetinkbookingsystem.exceptions;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Handle all exceptions

    @ExceptionHandler(Exception.class)
    public ModelAndView handleAllExceptions(Exception ex) {
        ModelAndView modelAndView = new ModelAndView("home/custom-error");
        modelAndView.addObject("errorMessage", "An unexpected error occurred. Please try again later." +
                " If the problem persists, then please contact the developers.");
        return modelAndView;
    }

}


