package com.example.streetinkbookingsystem.services;

import com.example.streetinkbookingsystem.models.Booking;
import com.example.streetinkbookingsystem.models.Client;
import com.example.streetinkbookingsystem.models.TattooArtist;
import com.example.streetinkbookingsystem.repositories.TattooArtistRepository;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import org.springframework.mail.SimpleMailMessage;

import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
    public class EmailService {

    @Autowired
    private JavaMailSender javaMailSender;
    @Autowired
    private TemplateEngine templateEngine;
    @Autowired
    private TattooArtistRepository tattooArtistRepository;
    @Autowired
    private BookingService bookingService;
    @Autowired
    private TattooArtistService tattooArtistService;


    /**
     * @summary Sends a confirmation email to the client for a specific booking.
     *  * This method retrieves the booking details, tattoo artist details, and client details,
     *  * then prepares and sends a confirmation email using an HTML template.
     *
     * @author Nanna
      * @param bookingId The ID of the booking
     * @param username The username of the tattoo artist
     */
    public String sendConfirmationMail(int bookingId, String username) {
        // Retrieve booking details based on the booking ID
        Booking booking = bookingService.getBookingDetail(bookingId);

        // Retrieve tattoo artist details based on the username
        TattooArtist tattooArtist = tattooArtistService.getTattooArtistByUsername(username);

        // Retrieve client details from the booking
        Client client = booking.getClient();

        // Format booking start time, end time, and date
        String bookingEnd = booking.getEndTimeSlot().format(DateTimeFormatter.ofPattern("HH:mm"));
        String bookingStart = booking.getStartTimeSlot().format(DateTimeFormatter.ofPattern("HH:mm"));
        String bookingDate = booking.getDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        // Prepare context for email template
        Context context = new Context();
        context.setVariable("ClientFirstName", client.getFirstName());
        context.setVariable("ArtistFirstName", tattooArtist.getFirstName());
        context.setVariable("ArtistLastName", tattooArtist.getLastName());
        context.setVariable("ArtistPhone", tattooArtist.getPhoneNumber());
        context.setVariable("ArtistEmail", tattooArtist.getEmail());
        context.setVariable("ArtistFacebook", tattooArtist.getFacebook());
        context.setVariable("ArtistInstagram", tattooArtist.getInstagram());
        context.setVariable("BookingStart", bookingStart);
        context.setVariable("BookingEnd", bookingEnd);
        context.setVariable("BookingDate", bookingDate);
        context.setVariable("BookingTitle", booking.getProjectTitle());
        context.setVariable("BookingDescription", booking.getProjectDesc());

        // Process the email template
        String processedHTMLTemplate = templateEngine.process("home/confirmation-mail", context);

        // Prepare the email
        MimeMessagePreparator preparator = message -> {
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(client.getEmail());
            helper.setSubject("Booking Confirmation");
            helper.setText(processedHTMLTemplate, true);
        };

        // Send the email
        try {
            javaMailSender.send(preparator);
            return "Mail sent successfully";
        } catch (MailException mailException) {
            return "Booking is saved. Unfortunately there was an error sending the email confirmation. Please try again later." +
                    " If the problem persists, check your mail account.";
        } catch (Exception e) {
            return "An unexpected error occurred. Please try again later.";
        }
    }

    /**
     * @author Nanna og Munazzah
     * param recipient The email address of the recipient
     * @param subject The subject of the email
     * @param content The content of the email
     */

    public void sendEmail(String recipient, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();

        // Set the recipient, subject, and content of the email
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(content);

        // Send the email using JavaMailSender
        javaMailSender.send(message);
    }

    /**
     * @summary Uses regex to check if the structure of the mail is valid fx xxxx@yyy.mmm
     * and if the email is the same that is written in the database
     *
     * @author Munazzah
     * @param email Input
     * @param username To check if the given email is the same as the one in database
     * @return True, if the email is structured as it should and is the same as in database
     */
    public boolean isValidEmail(String email, String username) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pattern = Pattern.compile(emailRegex);
        Matcher matcher = pattern.matcher(email);

        return matcher.matches() && tattooArtistRepository.getEmail(username).equals(email);
    }



}
