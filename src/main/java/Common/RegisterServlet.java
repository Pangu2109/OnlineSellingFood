package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Account;
import dal.AccountDAO;
import dal.ContactInformationDAO;
import model.ContactInformation;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "RegisterServlet", urlPatterns = {"/register"})
public class RegisterServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/nest-frontend/page-register.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        List<String> errorMessages = new ArrayList<>();

        // Get form parameters
        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");
        String email = request.getParameter("email");
        String birthYearStr = request.getParameter("birthYear");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");
        String phoneNumber = request.getParameter("phone");
        String address = request.getParameter("address");

        // Validate input lengths
        if (address.length() < 5 || address.length() > 200) {
            errorMessages.add("Address must be between 5 and 200 characters.");
        }
        if (phoneNumber.length() < 6 || phoneNumber.length() > 11) {
            errorMessages.add("Phone number must be between 6 and 11 characters.");
        }

        ContactInformationDAO contactInfoDAO = new ContactInformationDAO();
        AccountDAO accountDAO = new AccountDAO();

        // Check if contact information already exists
        String existingContactID = contactInfoDAO.getContactInformationIDbyAddressAndPhone(address, phoneNumber);

        if (existingContactID != null) {
            errorMessages.add("Contact information already exists with the provided phone number and address.");
            request.setAttribute("errorMessages", errorMessages);
            request.getRequestDispatcher("error.jsp").forward(request, response);
            return; // Exit the method to prevent further processing
        }

        try {
            // Validate passwords
            if (!password.equals(confirmPassword)) {
                errorMessages.add("Passwords do not match.");
                request.setAttribute("errorMessages", errorMessages);
                request.getRequestDispatcher("error.jsp").forward(request, response);
                return;
            }

            // Create and add contact information
            ContactInformation contactInfo = new ContactInformation();
            contactInfo.setAddress(address);
            contactInfo.setPhoneNumber(phoneNumber);

            int contactInfoResult = contactInfoDAO.addContact(contactInfo);
            if (contactInfoResult == 0) {
                errorMessages.add("Failed to add contact information.");
                request.setAttribute("errorMessages", errorMessages);
                request.getRequestDispatcher("error.jsp").forward(request, response);
                return;
            }

            // Retrieve the newly inserted ContactInformationID
            String contactInfoIDStr = contactInfoDAO.getContactInformationIDbyAddressAndPhone(address, phoneNumber);

            if (contactInfoIDStr == null) {
                errorMessages.add("No contact information found with the provided phone number and address.");
                request.setAttribute("errorMessages", errorMessages);
                request.getRequestDispatcher("error.jsp").forward(request, response);
                return;
            }

            // Convert ContactInformationID to int
            int contactInfoID;
            try {
                contactInfoID = Integer.parseInt(contactInfoIDStr);
            } catch (NumberFormatException e) {
                errorMessages.add("Invalid ContactInformationID format.");
                request.setAttribute("errorMessages", errorMessages);
                request.getRequestDispatcher("error.jsp").forward(request, response);
                return;
            }

            // Create the new Account
            Account newAccount = new Account();
            newAccount.setFirstName(firstName);
            newAccount.setLastName(lastName);
            newAccount.setEmail(email);
            newAccount.setBirthYear(birthYearStr);
            newAccount.setContactInformationID(contactInfoIDStr); // still as String for Account
            newAccount.setRoleID("6"); // Default RoleID
            newAccount.setStatusID("1"); // Default StatusID
            newAccount.setPassword(password);
            newAccount.setTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)); // Current time

            // Debugging information
            System.out.println("Inserting Account with details: ");
            System.out.println("FirstName: " + firstName);
            System.out.println("LastName: " + lastName);
            System.out.println("Email: " + email);
            System.out.println("BirthYear: " + birthYearStr);
            System.out.println("ContactInformationID: " + contactInfoIDStr);
            System.out.println("RoleID: " + newAccount.getRoleID());
            System.out.println("StatusID: " + newAccount.getStatusID());
            System.out.println("Password: [HIDDEN]");
            System.out.println("Time: " + newAccount.getTime());

            // Add the account to the database
            int accountResult = accountDAO.addAccount(newAccount);
            if (accountResult > 0) {
                response.sendRedirect("success.jsp");
            } else {
                // Rollback the contact information in case of failure
                contactInfoDAO.deleteContact(contactInfoID);
                errorMessages.add("Registration failed. Please try again.");
                request.setAttribute("errorMessages", errorMessages);
                request.getRequestDispatcher("error.jsp").forward(request, response);
            }

        } catch (Exception e) {
            // Log the actual exception message to the console and display on the error page
            e.printStackTrace();
            errorMessages.add("An error occurred during registration: " + e.getMessage());
            request.setAttribute("errorMessages", errorMessages);
            request.getRequestDispatcher("error.jsp").forward(request, response);
        }
    }
}

