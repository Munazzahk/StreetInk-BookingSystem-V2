package com.example.streetinkbookingsystem.services;

import com.example.streetinkbookingsystem.models.TattooArtist;
import com.example.streetinkbookingsystem.repositories.TattooArtistRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class LoginService {

    private static final int SALT_LENGTH = 16;

    @Autowired
    private TattooArtistRepository tattooArtistRepository;
    @Autowired
    private HttpSession session;

    /**
     * @summary Hashes and saves the passwords that are already in the database
     * should be deleted when everyone have hashed the passwords on local database
     *
     * @author Munazzah
     */
    public void hashExistingPasswords() {
        List<TattooArtist> tattooArtists = tattooArtistRepository.showTattooArtists();
        for (TattooArtist tattooArtist : tattooArtists) {
            String plainPassword = tattooArtist.getPassword();
            String hashedPassword = hashPassword(plainPassword);
            tattooArtist.setPassword(hashedPassword);
            tattooArtistRepository.updatePassword(tattooArtist.getUsername(), tattooArtist.getPassword());
        }
    }

    /**
     * @summary Checks if the given password matches the hashed password in database. If it does,
     * then sets the session attributes loggedIn to true, and stores the username in session
     *
     * @author Munazzah
     * @param username TattooArtist username input
     * @param password TattooArtist password input
     * @return Boolean, returns true if username and password match the one in database or else
     * return false
     */
    public boolean authenticateUser(String username, String password) {
        TattooArtist tattooArtist = tattooArtistRepository.getTattooArtistByUsername(username);
        if (tattooArtist != null) {
            String storedPassword = tattooArtist.getPassword();
            if (verifyPassword(password, storedPassword)) {
                session.setAttribute("loggedIn", true); //Set "loggedIn" attribute in session
                session.setAttribute("username", username); //Set "username" attribute in session
                return true; //Authentication successful
            } else {
                return false; //Password mismatch
            }
        } else {
            return false; //User not found
        }
    }


    /**
     * @summary Salts and hashes the password first, and the adds the salt to the salted and hashed
     * password, so we can extract the salt afterward for verification
     *
     * @author Munazzah
     * @param password The password that needs to be hashed
     * @return The hashed password as a string
     */
    public String hashPassword(String password) {
        try {
            //Generates random characters - the salt part
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);

            //Combines salt with password and hashes it
            byte[] hashedPassword = saltAndHash(password, salt);

            //Combines the salt with the already hashed password, so we know what the salt is
            byte[] saltedHashedPassword = new byte[salt.length + hashedPassword.length];
            System.arraycopy(salt, 0, saltedHashedPassword, 0, salt.length);
            System.arraycopy(hashedPassword, 0, saltedHashedPassword, salt.length, hashedPassword.length);

            //Encodes the whole byte to a string,so it is saved like a string in database
            return Base64.getEncoder().encodeToString(saltedHashedPassword);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @summary Method to verify the given password with the hashed password (in the database)
     *
     * @author Munazzah
     * @param password The input password
     * @param hashedPassword The stored hashed password in the database
     * @return Boolean if they match or not
     */
    public boolean verifyPassword(String password, String hashedPassword) {
        try {
            //Decodes and extracts the original salt, so we know what the salt is
            byte[] decodedHashedPassword = Base64.getDecoder().decode(hashedPassword);
            byte[] salt = new byte[SALT_LENGTH];
            System.arraycopy(decodedHashedPassword, 0, salt, 0, salt.length);

            //Using the original salt, we get the combined salt and password hashed again
            byte[] hashedPasswordToCompare = saltAndHash(password, salt);

            //This is the hashed password that is already stored in the database, starting right after the salt
            byte[] storedHash = new byte[decodedHashedPassword.length - salt.length];
            System.arraycopy(decodedHashedPassword, salt.length, storedHash, 0, storedHash.length);

            int diff = getDiff(hashedPasswordToCompare, storedHash);

            return diff == 0;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * @summary Compares the bytes of the storedHash in database with the given password
     * that we have added the original salt to, and also hashed, to see if the bytes match
     *
     * @author Munazzah
     * @param hashedPasswordToCompare The given password input that is salted and hashed
     * @param storedHash The database password
     * @return The difference in bytes as an int, if it is 0, the passwords match
     */
    //Compares the storedHash password with the one made with the given password and the salt
    private static int getDiff(byte[] hashedPasswordToCompare, byte[] storedHash) {
        //First line compares the bitwise length. If they are different, the byte by byte comparison won't happen
        //(^) means bitwise XOR - compares pairs of bits and if the both are 1 or 0 the result is 0, else it is 1
        //(|) bitwise OR - If wither of the bits are 1, the result is 1, else it is 0
        int diff = hashedPasswordToCompare.length ^ storedHash.length;

        for (int i = 0; i < hashedPasswordToCompare.length && i < storedHash.length; i++) {
            //diff takes the result of XOR and ORs it with current value of diff to keep track of differences
            diff |= hashedPasswordToCompare[i] ^ storedHash[i];
        }

        //If it's 0, there were no differences between the bytes and true is returned
        return diff;
    }

    /**
     * @summary Creates a byte array with the length of the salt and password, adds the salt and password
     * and hashes the whole thing using a chosen algorithm
     *
     * @author Munazzah
     * @param password The given password that afterward need to be stored in the database
     * @param salt The salt for the password
     * @return byte[] - Byte array that contains the hashed password with the chosen algorith
     * @throws NoSuchAlgorithmException In case the chosen algorithm for hashing does not exist
     */
    //Method that uses the combined salt and password and hashes it
    private byte[] saltAndHash(String password, byte[] salt) throws NoSuchAlgorithmException {
        //Combines the salt and password length together to create a new byte array
        byte[] saltedPassword = new byte[salt.length + password.getBytes().length];

        //Basically converts the salt and password to a single byte array before hashing
        //First line copies the bytes of the salt in the beginning in saltedPassword
        //Second line copies the bytes of the password and places it just after the salt in the new byte
        System.arraycopy(salt, 0, saltedPassword, 0, salt.length);
        System.arraycopy(password.getBytes(), 0, saltedPassword, salt.length, password.getBytes().length);

        //Hashes the whole byte array using the chosen algorithm
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest(saltedPassword);
    }

    /**
     * @summary Generates a random 8 char password where all the validChars can be added
     *
     * @author Munazzah
     * @return Returns the random password as a String
     */
    public String randomPassword() {
        String validChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#&_+-=";
        StringBuilder password = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < 8; i++) {
            int index = random.nextInt(validChars.length());
            password.append(validChars.charAt(index));
        }
        return password.toString();
    }

    /**
     * @author Munazzah
     * @param password Given password as input
     * @param username Given username, to know which password needs updating
     */
    public void updatePassword(String password, String username) {
        String hashedPassword = hashPassword(password);
        tattooArtistRepository.updatePassword(username, hashedPassword);
    }

    /**
     * @summary Returns true if session attribute LoggedIn is true.
     * LoggedIn is cast as a boolean because session can return attributes as objects
     *
     * @author Munazzah og Nanna
     * @param session For login status
     * @return True if LoggedIn is true, else false
     */
    public boolean isUserLoggedIn(HttpSession session) {
        return session.getAttribute("loggedIn") != null && (boolean) session.getAttribute("loggedIn");
    }

    /**
     * @summary Method to add all the attributes if user is logged in, or else return false if not logged in
     *
     * @author Emma og Munazzah
     * @param model To add attributes used in view
     * @param session To check for login status
     */
    public TattooArtist addLoggedInUserInfo(Model model, HttpSession session, TattooArtistService tattooArtistService) {
        boolean loggedIn = isUserLoggedIn(session);
        model.addAttribute("loggedIn", loggedIn);

        if (loggedIn) {
            String username = (String) session.getAttribute("username");
            model.addAttribute("username", username);
            TattooArtist tattooArtist = tattooArtistService.getTattooArtistByUsername(username);
            model.addAttribute("tattooArtist", tattooArtist);
            return tattooArtist;
        }
        return null;
    }

}
