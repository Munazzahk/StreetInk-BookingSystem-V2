package com.example.streetinkbookingsystem.repositories;

import com.example.streetinkbookingsystem.models.Client;
import com.example.streetinkbookingsystem.models.TattooArtist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ClientRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    BookingRepository bookingRepository;

    public Client getClientFromClientId(int clientId) {
        String query = "SELECT * FROM client WHERE id = ?";
        RowMapper<Client> rowMapper = new BeanPropertyRowMapper<>(Client.class);
            return jdbcTemplate.queryForObject(query, rowMapper, clientId);
    }

    /**
     * @author Munazzah
     * @param phoneNumber
     * @return List of Clients
     */
    public List<Client> getClientsByPhoneNumber(int phoneNumber) {
        String query = "SELECT * FROM client WHERE phone_number LIKE ?";
        RowMapper<Client> rowMapper = new BeanPropertyRowMapper<>(Client.class);
        try {
            return jdbcTemplate.query(query, rowMapper, "%" + phoneNumber + "%");
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /**
     * @author Munazzah
     * @param firstname
     * @return List of clients
     * @summary Uses LOWER so it isnt case sensitive, and also uses '%' for partial searching with
     * LIKE operator
     */
    public List<Client> getClientsByFirstName(String firstname) {
        String query = "SELECT * FROM client WHERE LOWER(first_name) LIKE LOWER(?)";
        RowMapper<Client> rowMapper = new BeanPropertyRowMapper<>(Client.class);
        try {
            return jdbcTemplate.query(query, rowMapper, "%" + firstname + "%");
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /**
     * Updates the client with the specified details.
     *
     * @param firstName   the new first name of the client
     * @param lastName    the new last name of the client
     * @param email       the new email address of the client
     * @param phoneNumber the new phone number of the client
     * @param description the new description or notes about the client
     * @param clientId    the ID of the client to be updated
     * @Author Tara
     */
    public void updateClient(String firstName, String lastName, String email, int phoneNumber, String description, int clientId) {
        String query = "UPDATE client SET first_name = ?, last_name = ?, email = ?, phone_number = ?, description = ? WHERE id = ?";
        try {
            jdbcTemplate.update(query,
                    firstName,
                  lastName,
                 email,
                    phoneNumber,
                    description,
                   clientId);
        } catch (EmptyResultDataAccessException e) {
            System.out.println("something went wrong");
        }
    }

    /**
     * @author Munazzah
     * @return list of clients
     * @summary Checks if any clients' booking date was over 5 years ago, if yes,
     * then adds them to the list
     */
    public List<Client> findInactivateClients() {
        String query = "SELECT c.* FROM Client c LEFT JOIN Booking b ON c.id = b.client_id " +
                "GROUP BY c.id " +
                "HAVING MAX(b.date) < (CURRENT_DATE - INTERVAL 5 YEAR)";

        RowMapper<Client> rowMapper = new BeanPropertyRowMapper(Client.class);

        try {
            return jdbcTemplate.query(query, rowMapper);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public void deleteClient(int clientId) {
        String updateBookingsQuery = "UPDATE booking SET client_id = ? WHERE client_id = ?";
        String deleteClientQuery = "DELETE FROM client WHERE id = ?";

        try {
            jdbcTemplate.update(updateBookingsQuery, 1, clientId);
            jdbcTemplate.update(deleteClientQuery, clientId);

        } catch (EmptyResultDataAccessException e) {
            System.out.println("Client not found.");
        }
    }

    /**
     * @author Munazzah
     * @return Arraylist of Client
     */
    //Don't know if I can do it like this, or if I should use collections.sort separately?
    public ArrayList<Client> getAllClientsSortedByFirstName() {
        String query = "SELECT * FROM client ORDER BY first_name";
        RowMapper<Client> rowMapper = new BeanPropertyRowMapper<>(Client.class);
        try {
            List<Client> clients = jdbcTemplate.query(query, rowMapper);
            return new ArrayList<>(clients);
        } catch (EmptyResultDataAccessException e) {
            return new ArrayList<>(); //Empty list if no clients are found
        }
    }

    /**
     * @author Munazzah
     * @return ArrayList of Client
     */
    public ArrayList<Client> getListOfClients() {
        String query = "SELECT * FROM client";
        RowMapper<Client> rowMapper = new BeanPropertyRowMapper<>(Client.class);
        try {
            List<Client> clients = jdbcTemplate.query(query, rowMapper);
            return new ArrayList<>(clients);
        } catch (EmptyResultDataAccessException e) {
            return new ArrayList<>(); //Empty list if no clients are found
        }
    }

    /**
     * Saves a new client to the database and returns the saved client with the generated ID.
     *
     * @param client the client object containing the details to be saved
     * @return the saved Client object with the generated ID
     * @Author Tara
     */
    public Client saveClient(Client client){
        String query = "INSERT INTO client (first_name, last_name, email, phone_number, description) " +
                "VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection ->{
            PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, client.getFirstName());
            ps.setString(2, client.getLastName());
            ps.setString(3, client.getEmail());
            ps.setInt(4, client.getPhoneNumber());
            ps.setString(5, client.getDescription());
            return ps;
        }, keyHolder);

        client.setId(keyHolder.getKey().intValue());
        return client;
    }


    /**
     * Updates the client ID associated with a specific booking.
     *
     * @param bookingId the ID of the booking to be updated
     * @param clientId  the new client ID to associate with the booking
     * @Author Tara
     */
    public void updateClientOnBooking(int bookingId, int clientId){
        String query = "UPDATE booking SET client_id = ? WHERE id = ?";
        jdbcTemplate.update(query, clientId, bookingId);
    }







}
