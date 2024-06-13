package com.example.streetinkbookingsystem.services;

import com.example.streetinkbookingsystem.models.Client;
import com.example.streetinkbookingsystem.repositories.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

    /**
     * @summary Takes the Arraylist from the repository, sorts it using Collections.sort() and
     * returns the sorted list
     *
     * @author Munazzah
     * @return ArrayList of Clients
     */
    public ArrayList<Client> getSortedListOfClients() {
        ArrayList<Client> clients = clientRepository.getListOfClients();
        Collections.sort(clientRepository.getListOfClients());
        ArrayList<Client> sortedList = new ArrayList<>(clients);
        return sortedList;
    }

    /**
     * @author Munazzah
     * @param phoneNumber To get client by. Used in search
     * @return Client
     */
    public List<Client> getClientsByPhoneNumber(int phoneNumber) {
        return clientRepository.getClientsByPhoneNumber(phoneNumber);
    }

    /**
     * @author Munazzah
     * @param firstname To get client by. Used in search
     * @return List of clients
     */
    public List<Client> getClientsByFistName(String firstname) {
        return clientRepository.getClientsByFirstName(firstname);
    }

    /**
     * @author Emma
     * @param clientId id of client to get
     * @return Client object
     */
    public Client getClientFromClientId(int clientId) {
        return clientRepository.getClientFromClientId(clientId);
    }

    /**
     * @author Emma
     *
     * @param firstName   The first name of the client.
     * @param lastName    The last name of the client.
     * @param email       The email address of the client.
     * @param phoneNumber The phone number of the client.
     * @param description A brief description or note about the client.
     * @param clientId    The unique identifier of the client to be updated.
     */
    public void updateClient(String firstName, String lastName, String email, int phoneNumber, String description,
                             int clientId ) {
        clientRepository.updateClient(firstName, lastName,email, phoneNumber, description, clientId);
    }

    /**
     * @author Emma
     *
     * @param clientId client id to delete
     */
    public void deleteClientInfoByClientId(int clientId) {
        clientRepository.deleteClient(clientId);
    }

    /**
     * Saves a new client to the database.
     *
     * @param client the client object containing the details to be saved
     * @return the saved Client object with the generated ID
     * @Author Tara
     */
    public Client saveClient(Client client) {
        return clientRepository.saveClient(client);
    }

    /**
     * Updates the client ID associated with a specific booking.
     *
     * @param bookingId the ID of the booking to be updated
     * @param clientId  the new client ID to associate with the booking
     * @Author Tara
     */
    public void updateClientOnBooking(int bookingId, int clientId) {
        clientRepository.updateClientOnBooking(bookingId, clientId);
    }

    /**
     * @summary Is a scheduled method that runs every day at midnight, where it finds clients
     * that haven't had any booking in the past 5 years, adds them to a list, and then inactivates
     * the clients, do their information are overwritten with the default client
     * The cron is in sec, minutes, hours, day, month, day of week
     *
     * @author Munazzah
     */
    @Scheduled(cron = "0 0 0 * * ?") //Runs every day at midnight
    public void cleanupInactiveClients() {
        List<Client> inactiveClientIds = clientRepository.findInactivateClients();
        if (inactiveClientIds != null) {
            for (Client client : inactiveClientIds) {
                clientRepository.deleteClient(client.getId());
            }
        }
    }
}
