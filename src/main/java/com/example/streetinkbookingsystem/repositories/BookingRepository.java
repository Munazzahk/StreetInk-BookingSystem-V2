package com.example.streetinkbookingsystem.repositories;

import com.example.streetinkbookingsystem.models.Booking;
import com.example.streetinkbookingsystem.models.Client;
import com.example.streetinkbookingsystem.models.ProjectPicture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.metrics.buffering.StartupTimeline;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Repository
public class BookingRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * @author Nanna
     * @param date used to get all bookings for the date
     * @param username used to get bookings for the artist
     * @return a list of bookings for the date with limited information,
     * creating a Client and adding it as an attribute to the booking
     */
    public List<Booking> getBookingsForDay( LocalDate date, String username){
        String query = "SELECT * FROM booking JOIN client ON booking.client_id =  client.id " +
                "LEFT JOIN project_picture On booking.id = project_picture.booking_id WHERE booking.username = ? AND date =? ORDER BY start_time_slot;";
        RowMapper<Booking> rowMapper = (rs, rowNum) -> {
            Booking booking = new Booking();
            booking.setId(rs.getInt("booking.id"));
            booking.setStartTimeSlot(rs.getTime("start_time_slot").toLocalTime());
            booking.setEndTimeSlot(rs.getTime("end_time_slot").toLocalTime());
            booking.setProjectTitle(rs.getString("project_title"));

            Client client = new Client();

            client.setId(rs.getInt("client.id"));
            client.setFirstName(rs.getString("first_name"));
            client.setLastName(rs.getString("last_name"));
            booking.setClient(client);
            return booking;
        };
        return jdbcTemplate.query(query,rowMapper,username,date);
    }

    /**
     * @author Nanna & Tara
     * @param bookingId used to find the booking
     * @return all details about a booking, creating a Client and adding it as an attribute to the booking,
     */
    public Booking getBookingDetails(int bookingId) {
        String query = "SELECT booking.*, client.*, project_picture.id, project_picture.picture_data " +
                "FROM booking " +
                "JOIN client ON booking.client_id =  client.id " +
                "LEFT JOIN project_picture On booking.id = project_picture.booking_id " +
                "WHERE booking.id = ?;";

        return jdbcTemplate.query(query, new Object[]{bookingId}, new ResultSetExtractor<Booking>() {
            @Override
            public Booking extractData(ResultSet rs) throws SQLException, DataAccessException {
                    Booking booking = null;
                    Client client = null; // hvorfor dette?
                    List<ProjectPicture> projectPictures = new ArrayList<>();

                    while (rs.next()) {
                        if (booking == null) {
                            booking = new Booking();
                            booking.setId(rs.getInt("booking.id"));
                            booking.setStartTimeSlot(rs.getTime("start_time_slot").toLocalTime());
                            booking.setEndTimeSlot(rs.getTime("end_time_slot").toLocalTime());
                            booking.setDate(rs.getDate("date").toLocalDate());
                            booking.setProjectTitle(rs.getString("project_title"));
                            booking.setProjectDesc(rs.getString("project_desc"));
                            booking.setPersonalNote(rs.getString("personal_note"));
                            booking.setIsDepositPayed(rs.getBoolean("is_deposit_payed"));

                            client = new Client();
                            client.setId(rs.getInt("client.id"));
                            client.setFirstName(rs.getString("first_name"));
                            client.setLastName(rs.getString("last_name"));
                            client.setEmail(rs.getString("email"));
                            client.setPhoneNumber(rs.getInt("phone_number"));
                            client.setDescription(rs.getString("description"));

                            booking.setClient(client);
                        }

                        int pictureId = rs.getInt("project_picture.id");
                        if (pictureId != 0){
                            ProjectPicture projectPicture = new ProjectPicture();
                            projectPicture.setId(pictureId);
                            projectPicture.setBookingId(booking.getId());
                            projectPicture.setPictureData(rs.getBytes("picture_data"));
                            projectPictures.add(projectPicture);
                        }
                    }

                    if (booking != null) {
                        booking.setProjectPictures(projectPictures);
                    }

                    return booking;
            }
        });

    }


    /**
     * @Summary Creates a new booking with the specified details.
     *
     * @param startTimeSlot the start time slot for the booking
     * @param endTimeSlot   the end time slot for the booking
     * @param date          the date of the booking
     * @param username      the username of the tattoo artist
     * @param projectTitle  the title of the project
     * @param projectDesc   the description of the project
     * @param personalNote  a personal note about the booking
     * @param isDepositPayed whether the deposit has been paid
     * @return the created Booking object
     * @Author Tara
     */
    public Booking createNewBooking (LocalTime startTimeSlot, LocalTime endTimeSlot, LocalDate date,
                                  String username, String projectTitle, String projectDesc, String personalNote,
                                  boolean isDepositPayed){

        // tjekker om username eksisterer i tattoo_artist table
        String checkUsernameQuery = "SELECT COUNT(*) FROM tattoo_artist WHERE username = ?";
        Integer count = jdbcTemplate.queryForObject(checkUsernameQuery, new Object[]{username}, Integer.class);
        if (count == null || count == 0) {
            throw new IllegalArgumentException("Username does not exist in tattoo_artist table: " + username);
        }


        String query = "INSERT INTO booking (start_time_slot, end_time_slot, date, client_id, username, " +
                "project_title, project_desc, personal_note, is_deposit_payed)" +
                "values(?, ?, ?, 1, ?, ?, ?, ?, ?);";
        // givet client_id 1, så den pr. default har "Unknown Client",
        // indtil client view, hvor vi opdaterer til ny eller eksisterende client


        //Gemmer den generede primærnøgleværdi, efter indsættelse, så vi kan bruge den videre i
        //metoden, til f.eks at gemme projectpictures.
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement pS = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            pS.setTime(1, Time.valueOf(startTimeSlot));
            pS.setTime(2, Time.valueOf(endTimeSlot));
            pS.setDate(3, Date.valueOf(date));
            pS.setString(4, username);
            pS.setString(5, projectTitle);
            pS.setString(6, projectDesc);
            pS.setString(7, personalNote);
            pS.setBoolean(8, isDepositPayed);
            return pS;

        }, keyHolder);

        int bookingId = keyHolder.getKey().intValue();

        return findById(bookingId);

    }
    /**
     * Updates the booking with the specified details.
     *
     * @param bookingId     the ID of the booking to be updated
     * @param startTimeSlot the new start time slot for the booking
     * @param endTimeSlot   the new end time slot for the booking
     * @param date          the new date of the booking
     * @param projectTitle  the new title of the project
     * @param projectDesc   the new description of the project
     * @param personalNote  the new personal note about the booking
     * @param isDepositPayed whether the deposit has been paid
     * @Author Tara
     */
    public void updateBooking(int bookingId, LocalTime startTimeSlot, LocalTime endTimeSlot,
                                 LocalDate date, String projectTitle, String projectDesc,
                                 String personalNote, boolean isDepositPayed) {
        String query = "UPDATE booking SET start_time_slot = ?, end_time_slot = ?, date = ?, " +
                "project_title = ?, project_desc = ?, personal_note = ?, " +
                "is_deposit_payed = ? WHERE id = ?";

        jdbcTemplate.update(query, startTimeSlot, endTimeSlot, Date.valueOf(date),
                projectTitle, projectDesc, personalNote, isDepositPayed, bookingId);
    }

    /**
     * @author Munazzah
     * @param bookingId
     */
    public void deleteBooking(int bookingId) {
        // Get the client_id associated with the booking
        // Uses simple array Object[] instead of ArrayList or something else, so it can directly be
        // substituted (direct mapping instead of dynamic mapping) in SQL queries
        String getClientIdQuery = "SELECT client_id FROM booking WHERE id = ?";
        Integer clientId = jdbcTemplate.queryForObject(getClientIdQuery, new Object[]{bookingId}, Integer.class);

        // Delete the booking
        String deleteBookingQuery = "DELETE FROM booking WHERE id = ?";
        jdbcTemplate.update(deleteBookingQuery, bookingId);

        // Check if the client has any other bookings
        String checkClientBookingsQuery = "SELECT COUNT(*) FROM booking WHERE client_id = ?";
        int remainingBookings = jdbcTemplate.queryForObject(checkClientBookingsQuery, new Object[]{clientId}, Integer.class);

        // Delete the client if they have no other bookings
        if (remainingBookings == 0) {
            String deleteClientQuery = "DELETE FROM client WHERE id = ?";
            jdbcTemplate.update(deleteClientQuery, clientId);
        }
    }

    /**
     * @author Munazzah
     * @param bookingId
     * @summary Checks if the booking exist
     */
    private void validateBookingExistence(int bookingId) {
        String checkBookingQuery = "SELECT COUNT(*) FROM booking WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(checkBookingQuery, new Object[]{bookingId}, Integer.class);
        if (count == null || count == 0) {
            throw new IllegalArgumentException("This booking does not exist: " + bookingId);
        }
    }


    /**
     * @Author Tara
     * @param bookingId
     * @return will find the specifik booking from the bookingId.
     */
    public Booking findById(int bookingId) {
        validateBookingExistence(bookingId);

        String query = "SELECT * FROM booking WHERE id = ?";
        RowMapper<Booking> rowMapper = new BeanPropertyRowMapper<>(Booking.class);
        return jdbcTemplate.queryForObject(query, rowMapper, bookingId);
    }


    //Retunerer antallet af booking for en bestemt dato og username
    /**
     * @author Nanna
     * @param specificDate used to the total number of bookings for date
     * @param username used to find bookings related to the artist
     * @return
     */
    public int getBookingCountForDate(LocalDate specificDate, String username) {
        // Denne SQL-forespørgsel tæller antallet af rækker i tabellen booking,
        // der matcher den givne dato (specificDate) og brugernavn (username).
        String query = "SELECT COUNT(*) AS booking_count FROM booking WHERE date = ? AND username = ?";
        //queryForObject er en metode, der udfører en forespørgsel og returnerer et enkelt resultat.
        //Integer.class, angiver at resultatet skal mappes til en Integer.
        return jdbcTemplate.queryForObject(query, Integer.class, specificDate, username);
        //Selvom COUNT(*) altid retunerer en int, så skal man stadig bruge Integer.class, da queryForObject
        // kan retunere forskellige værdier, og har brug for specificering
    }

    /**
     * @author Emma
     * @param username The username of the tattoo artist
     * @return The count of bookings for the current week
     */
    public int getBookingCountForThisWeek(String username) {
        // Calculate the start date of the week based on the provided year, month, and week number

        LocalDate startOfWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // Calculate the end date of the week by adding 6 days to the start date (assuming a week ends on Sunday)
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        // Query the database to get the booking count for the specified week
        String query = "SELECT COUNT(*) AS booking_count FROM booking WHERE date BETWEEN ? AND ? AND username = ?";
        return jdbcTemplate.queryForObject(query, Integer.class, startOfWeek, endOfWeek, username);
    }

    /**
     * @author Emma
     * @param year The year for which to count the bookings
     * @param month The month for which to count the bookings (1 = January, 12 = December)
     * @param username The username of the tattoo artist
     * @return The count of bookings for the specified month and yea
     */
    public int getBookingCountForMonth(int year, int month, String username) {
        // Finde ud af start og slut dag på måneden
        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.with(TemporalAdjusters.lastDayOfMonth());

        String query = "SELECT COUNT(*) AS booking_count FROM booking WHERE date BETWEEN ? AND ? AND username = ?";
        return jdbcTemplate.queryForObject(query, Integer.class, startOfMonth, endOfMonth, username);
    }

    public List<Booking> getBookingsForMonth(int year, int month, String username) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        String query = "SELECT * FROM booking WHERE date BETWEEN ? AND ? AND username = ?";
        RowMapper<Booking> rowMapper = new BeanPropertyRowMapper<>(Booking.class);
        return jdbcTemplate.query(query, rowMapper, startDate, endDate, username);
    }

    /**
     * @author Emma
     * @param clientId The ID of the client whose bookings are to be retrieved
     * @return A list of Booking objects associated with the specified client, ordered by date in descending order
     */
    public List<Booking> getBookingsByClientId(int clientId) {
        String query = "SELECT * FROM booking WHERE client_id = ? ORDER BY date DESC";
        RowMapper<Booking> rowMapper = new BeanPropertyRowMapper<>(Booking.class);
        return jdbcTemplate.query(query, rowMapper, clientId);
    }

}
