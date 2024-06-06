package com.example.streetinkbookingsystem.controllers;

import com.example.streetinkbookingsystem.models.Booking;
import com.example.streetinkbookingsystem.models.Client;
import com.example.streetinkbookingsystem.models.ProjectPicture;
import com.example.streetinkbookingsystem.models.TattooArtist;
import com.example.streetinkbookingsystem.services.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
public class BookingController {

    @Autowired
    private BookingService bookingService;
    @Autowired
    private LoginService loginService;
    @Autowired
    private TattooArtistService tattooArtistService;
    @Autowired
    private ProjectPictureService projectPictureService;
    @Autowired
    private ClientService clientService;
    @Autowired
    private EmailService emailService;

    /**
     *
     * @author Nanna & Tara
     * @param model The model to be populated with attributes for rendering the view.
     * @param session The HttpSession object to check if the user is logged in.
     * @param bookingId The ID of the booking that needs to be retrieved.
     * @return Booking view,
     *         If the user is not logged in, it redirects to the home page.
     */
    @GetMapping("/booking")
            public String booking(Model model, HttpSession session, @RequestParam int bookingId,
                                  @RequestParam(required = false) Integer bookingIdToDelete) {
                // Check if the user is logged in
                if (!loginService.isUserLoggedIn(session)) {
                    return "redirect:/";
        }

        // Add logged-in user information to the model
        loginService.addLoggedInUserInfo(model, session, tattooArtistService);

        // Retrieve booking details using the provided booking ID
        Booking booking = bookingService.getBookingDetail(bookingId);
        model.addAttribute("booking", booking);

        // Retrieve and convert project pictures to base64 format
        List<String> base64Images = projectPictureService.convertToBase64(booking.getProjectPictures());
        model.addAttribute("base64Images", base64Images);

        // For deleting the booking if necessary
        if (bookingIdToDelete != null) {
            model.addAttribute("bookingIdToDelete", bookingIdToDelete);
        }

        return "home/booking";
    }


    /**
     * @Author Tara
     * @param model used to "hold" the attributes to use in the view.
     * @param session Used to determine if the user is logged in or not. User will be redirected
     *                to index page if not logged in.
     * @param date Is used, sp that we create af booking on the specific date.
     * @return to the create bookings view
     */
    @GetMapping("/create-new-booking")
    public String createNewBooking(Model model, HttpSession session, @RequestParam LocalDate date, @RequestParam (required = false) Integer bookingId ){
        if (!loginService.isUserLoggedIn(session)) {
            return "redirect:/";
        }
        loginService.addLoggedInUserInfo(model, session, tattooArtistService);
        if (bookingId != null) {
            Booking booking =bookingService.getBookingDetail(bookingId);
            model.addAttribute("booking",booking);
        }
        //needed to return to day page in "back-arrow".
        model.addAttribute("date", date);
        return "home/create-new-booking";
    }

    /**
     * @summary Saves a new booking or updates an existing booking based on user input.
     *
     * @author Tara & Nanna
     * @param startTimeSlot The start time of the booking slot.
     * @param endTimeSlot The end time of the booking slot.
     * @param date The date of the booking.
     * @param session The HttpSession object to retrieve the username of the logged-in user.
     * @param projectTitle The title of the project for the booking.
     * @param projectDesc The description of the project for the booking.
     * @param personalNote The personal notes for the booking.
     * @param isDepositPayed A boolean indicating whether the deposit is paid for the booking (default is false).
     * @param projectPictures The pictures uploaded for the project.
     * @param action The action to be taken after saving the booking (add-client or existing-client).
     * @param bookingId The ID of the booking to be updated (optional).
     * @param redirectAttributes Used for adding flash attributes for redirection.
     * @param model The model to add attributes for rendering the view.
     * @return Redirects to add-client or existing-client page based on the action parameter.
     */
    @PostMapping("/create-new-booking")
    public String saveNewBooking(@RequestParam LocalTime startTimeSlot,
                                 @RequestParam LocalTime endTimeSlot,
                                 @RequestParam LocalDate date,
                                 HttpSession session,
                                 @RequestParam String projectTitle,
                                 @RequestParam String projectDesc,
                                 @RequestParam String personalNote,
                                 @RequestParam(name = "isDepositPayed", defaultValue = "false") boolean isDepositPayed,
                                 @RequestParam("projectPictures") MultipartFile[] projectPictures,
                                 @RequestParam String action,
                                 @RequestParam(required = false) Integer bookingId,
                                 RedirectAttributes redirectAttributes, Model model) {
        String username = (String) session.getAttribute("username");

        // Ensure that booking cannot have a end-time that is before the start-time
        if (endTimeSlot.isBefore(startTimeSlot)) {
            redirectAttributes.addFlashAttribute("errorMessage", "End time cannot be before start time.");
            return "redirect:/create-new-booking?date="+date;
        }

        //Either create new booking or update the one in the making
        Booking booking;
        if (bookingId != null) {

            // Update existing booking
            bookingService.updateBooking(bookingId, startTimeSlot, endTimeSlot, date, projectTitle, projectDesc, personalNote, isDepositPayed, getPictureList(projectPictures));
            booking = bookingService.getBookingDetail(bookingId);
            projectPictureService.updateProjectPictures(bookingId, getPictureList(projectPictures));

        } else {

            // The new booking will have a default client until another is chosen
            booking = bookingService.createNewBooking(startTimeSlot, endTimeSlot, date, username, projectTitle, projectDesc, personalNote, isDepositPayed, getPictureList(projectPictures));
        }

        int savedBookingId = booking.getId();

        //Create new client or choose an existing client:
        if ("new-client".equals(action)) {
            return "redirect:/add-client?bookingId=" + savedBookingId + "&date=" + date;
        } else if ("existing-client".equals(action)) {
            return "redirect:/client-list?bookingId=" + savedBookingId +  "&date=" + date;
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid action.");
            return "redirect:/create-new-booking?date=" + date;
        }
    }

    /**
     * @summary Converts an array of MultipartFile objects to a list of byte arrays.
     *
     * @author Nanna og Tara
     * @param projectPictures The array of MultipartFile objects.
     * @return A list of byte arrays.
     */
    private List<byte[]> getPictureList(MultipartFile[] projectPictures) {
        // Stream the array of MultipartFile objects
        return Stream.of(projectPictures)
                // Filter out empty MultipartFile objects
                .filter(file -> !file.isEmpty())
                // Map each MultipartFile object to its byte array representation
                .map(file -> {
                    try {
                        // Retrieve the byte array content of the MultipartFile object
                        return file.getBytes();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                // Collect the byte arrays into a list
                .collect(Collectors.toList());
    }


    /**
     * @Summary A preview of the booking you created, where you can choose to save it or cancel it
     * @Author Tara
     * @param model Containing all the attributes used i the views
     * @param session The HttpSession object to retrieve the username of the logged-in user.
     * @param bookingId So you can see the booking you just made.
     * @param clientId Used i the view, so we can see the client in the booking. And update the booking with the choosen
     *                 new/ existing client.
     * @return booking-preview
     */
    @GetMapping("/booking-preview")
    public String bookingPreview (Model model, HttpSession session, @RequestParam int bookingId,
                                  @RequestParam int clientId){

        if (!loginService.isUserLoggedIn(session)) {
            return "redirect:/";
        }
        loginService.addLoggedInUserInfo(model, session, tattooArtistService);

        //Tilføjer den "nye" ClientId til bookingen
        clientService.updateClientOnBooking(bookingId, clientId);
        Booking booking = bookingService.getBookingDetail(bookingId);
        model.addAttribute("booking", booking);

        // Henter billeder fra den specifikke booking
        List<String> base64Images = projectPictureService.convertToBase64(booking.getProjectPictures());
        model.addAttribute("base64Images", base64Images);

        return "home/booking-preview";
    }

    /**
     * @summary Deletes the booking and redirects to the day page.
     *
     * @author Nanna
     * @param bookingId The ID of the booking to be deleted.
     * @param date The date of the booking used to redirect back to the day page.
     * @return Redirects to the day page with the specified date.
     */
    @GetMapping("/cancel-booking")
    public String cancelBooking(@RequestParam int bookingId, @RequestParam String date) {
        bookingService.deleteBooking(bookingId);
        System.out.println("here");
        return "redirect:/day?date=" + date;
    }

    /**
     * @summary Sends a confirmation email to the user.
     *
     * @author Nanna og Tara
     * @param bookingId The ID of the booking to be saved.
     * @param session The HttpSession object to retrieve user information.
     * @return A redirection to the booking details page with the specified bookingId.
     */
    @GetMapping("/save-booking")
    public String saveBooking(@RequestParam int bookingId, HttpSession session,
                              @RequestParam String date, RedirectAttributes redirectAttributes) {
        String username = (String) session.getAttribute("username");
        String message = emailService.sendConfirmationMail(bookingId, username);
        System.out.println(message);
        redirectAttributes.addFlashAttribute("message", message);
        return "redirect:/day?date=" + date;
    }

    /**
     * @summary Retrieves all the information from the booking, so they can be presented in the view
     *
     * @author Munazzah
     * @param model To add attributes
     * @param session To check for login and retrieve information from session
     * @param bookingId To know which booking that need editing
     * @return Edit booking view
     */
    @GetMapping("/edit-booking")
    public String editBooking(Model model, HttpSession session, @RequestParam("bookingId") int bookingId) {
        if (!loginService.isUserLoggedIn(session)) {
            return "redirect:/";
        }
        loginService.addLoggedInUserInfo(model, session, tattooArtistService);

        Booking booking = bookingService.getBookingDetail(bookingId);
        model.addAttribute("booking", booking);

        //Henter de billeder der allerede findes
        List<ProjectPicture> projectPictures = projectPictureService.getPicturesAsObjects(bookingId);
        model.addAttribute("projectPictures", projectPictures);
        List<String> base64Images = projectPictureService.getPicturesByBooking(bookingId);
        model.addAttribute("base64Images", base64Images);

        return "home/edit-booking";
    }

    /**
     * @Summary Updates the booking with the new information that has been written in view
     *
     * @author Munazzah
     * @param startTimeSlot The start time of the booking slot.
     * @param endTimeSlot The end time of the booking slot.
     * @param date The date of the booking.
     * @param session The HttpSession object to retrieve the username of the logged-in user.
     * @param projectTitle The title of the project for the booking.
     * @param projectDesc The description of the project for the booking.
     * @param personalNote The personal notes for the booking.
     * @param isDepositPayed A boolean indicating whether the deposit is paid for the booking (default is false).
     * @param projectPictures The pictures uploaded for the project.
     * @param bookingId The ID of the booking to be updated (optional).
     * @param redirectAttributes Used for adding flash attributes for redirection.
     * @param model The model to add attributes for rendering the view.
     * @return Redirects to booking view
     */
    @PostMapping("/edit-booking")
    public String updateBooking(@RequestParam LocalTime startTimeSlot, @RequestParam LocalTime endTimeSlot, @RequestParam LocalDate date,
                                @RequestParam String projectTitle, @RequestParam String projectDesc,
                                @RequestParam String personalNote,  @RequestParam(name = "isDepositPayed", defaultValue = "false") boolean isDepositPayed,  @RequestParam(required = false) List<Integer> deletePictures,
                                @RequestParam("projectPictures") MultipartFile[] projectPictures,
                                @RequestParam int bookingId, Model model, HttpSession session, @RequestParam(required = false) boolean sendMail, RedirectAttributes redirectAttributes) {


        // Ensure that booking cannot have a end-time that is before the start-time
        if (endTimeSlot.isBefore(startTimeSlot)) {
            redirectAttributes.addFlashAttribute("errorMessage", "End time cannot be before start time.");
            return "redirect:/edit-booking?bookingId=" + bookingId;
        }

        if (deletePictures != null) {
            projectPictureService.deleteProjectPictures(deletePictures);
        }

        List<byte[]> pictureList = Stream.of(projectPictures).filter(file -> !file.isEmpty())
                .map(file -> {
                    try {
                        return file.getBytes();
                    } catch (IOException e){
                        e.printStackTrace();
                        return null;
                    }
                })
                .collect(Collectors.toList());

        bookingService.updateBooking(bookingId, startTimeSlot, endTimeSlot, date, projectTitle, projectDesc, personalNote, isDepositPayed, pictureList);

        //If saved with the intent to send mail
        if (sendMail) {
            String username = (String) session.getAttribute("username");
            emailService.sendConfirmationMail(bookingId, username);
        }

        return "redirect:/booking?bookingId=" + bookingId + "&username=" + session.getAttribute("username");
    }

    /**
     * @summary Adds a bookingIdToDelete attribute to the view though the delete button,
     * and when it is present, the booking page shows teh warning message thanks to the th:block
     *
     * @author Munazzah
     * @param bookingIdToDelete The booking that needs to be deleted
     * @param redirectAttributes So the bookingId can be redirected to the delete method that deletes the booking
     * @param session To check for login
     * @param model To render the view
     * @return Redirects to the booking view with the specified bookingId
     */
    @PostMapping("/booking")
    public String deleteBookingWithWarning(@RequestParam Integer bookingIdToDelete, RedirectAttributes redirectAttributes, HttpSession session, Model model) {
        if (!loginService.isUserLoggedIn(session)) {
            return "redirect:/";
        }
        loginService.addLoggedInUserInfo(model, session, tattooArtistService);

        Booking booking = bookingService.getBookingDetail(bookingIdToDelete);
        model.addAttribute("booking", booking);
        model.addAttribute("bookingIdToDelete", bookingIdToDelete);

        redirectAttributes.addAttribute("bookingIdToDelete", bookingIdToDelete);
        return "redirect:/booking?bookingId=" + bookingIdToDelete;
    }

    /**
     * @summary Deletes the specifik booking
     *
     * @author Munazzah
     * @param bookingIdToDelete bookingId of booking to be deleted
     * @param session For login
     * @param model to add things to the view
     * @return Redirects to the calendar
     */
    @PostMapping("/delete-booking")
    public String deleteBooking(@RequestParam Integer bookingIdToDelete, @RequestParam String date, HttpSession session, Model model) {
        if (!loginService.isUserLoggedIn(session)) {
            return "redirect:/";
        }
        loginService.addLoggedInUserInfo(model, session, tattooArtistService);
        bookingService.deleteBooking(bookingIdToDelete);
        return "redirect:/day?date=" + date;
    }
}


