package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String patientUsername = tokens[1];
        String PatientPassword = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(patientUsername)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(PatientPassword, salt);
        // create the caregiver
        try {
            currentPatient = new Patient.PatientBuilder(patientUsername, salt, hash).build();
            // save to caregiver information to our database
            currentPatient.saveToDB();
            System.out.println(" *** Account created successfully *** ");
        } catch (SQLException e) {
            System.out.println("Create failed");
            e.printStackTrace();
        }
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again! The input length should be 3");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            currentCaregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentCaregiver.saveToDB();
            System.out.println(" *** Account created successfully *** ");
        } catch (SQLException e) {
            System.out.println("Create failed");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        // login_patient <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentPatient != null || currentPatient != null) {
            System.out.println("Already logged-in!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when logging in");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Please try again!");
        } else {
            System.out.println("Caregiver logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("Already logged-in!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when logging in");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Please try again!");
        } else {
            System.out.println("Caregiver logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        //first check to see if the user has logged in
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Need to be logged in!");
        } else if (tokens.length != 2) {
            System.out.println("Please try again!");
        } else {
            //if the user has logged in
            //make the connection
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            //obtain the first token in user input as the date
            String date = tokens[1];

            String vaccines = "SELECT * FROM Vaccines";
            try {
                PreparedStatement vaccineStatement = con.prepareStatement(vaccines);
                ResultSet VaccineByCaregivers = vaccineStatement.executeQuery();
                List<String> availableCaregivers = AvailableCaregiversGetter(date);

                System.out.println("List of Caregivers Available\n-------");
                for (String individualCaregivers : availableCaregivers) {
                    //remove all the white space from both ends of a string
                    System.out.println(individualCaregivers.trim());
                }
                System.out.println("\nList of Vaccines & Doses Counts\n-------");
                while (VaccineByCaregivers.next()) {
                    String vaccineName = VaccineByCaregivers.getString("Name").trim();
                    int doseCount = VaccineByCaregivers.getInt("Doses");
                    System.out.println(vaccineName + ": " + doseCount);
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Please enter a valid date!");
            } catch (SQLException e) {
                System.out.println("Error occurred when getting caregiver schedule");
                e.printStackTrace();
            } finally {
                cm.closeConnection();
            }
        }
    }



    private static List<String> AvailableCaregiversGetter(String date) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String queryForAvailableCaregivers = "SELECT a.Username FROM Availabilities a " +
                "WHERE a.Time = ? AND a.Username NOT IN (" +
                "SELECT b.CaregiverUsername " +
                "FROM Appointments b " +
                "WHERE b.Time = ?" +
                ")";


        try {
            PreparedStatement caregiverGetterStatement = con.prepareStatement(queryForAvailableCaregivers);
            Date selectedDate = Date.valueOf(date);
            caregiverGetterStatement.setDate(1, selectedDate);
            caregiverGetterStatement.setDate(2, selectedDate);
            ResultSet resultingAvailableCaregiverByQuery = caregiverGetterStatement.executeQuery();

            List<String> newAvailableCaregiverList = new ArrayList<>();
            while (resultingAvailableCaregiverByQuery.next()) {
                newAvailableCaregiverList.add(resultingAvailableCaregiverByQuery.getString("Username").trim());
            }

            return newAvailableCaregiverList;
        } catch (IllegalArgumentException e) {
            System.out.println("the input date is not valid");
        } catch (SQLException e) {
            System.out.println("Fail to create an appoinment. Please try again");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return null;
    }



    private static void reserve(String[] tokens) {
        // TODO: Part 2
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static String cancelQueryHelper(String userType) {
        String cancelQuery = "SELECT AppointmentID FROM Appointments " +
                "WHERE AppointmentID = ? AND " +
                userType + " = ?";
        return cancelQuery;
    }

    private static void cancel(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Not logged in. In order to cancel, you have to first log in");
        } else if (tokens.length != 2) {
            System.out.println("The number of input parameter is incorrect. Please try inputting only the username and password ");
        } else {
            //if the user is logged in
            String cancelQuery = "";
            String username;
            String appointmentID = tokens[1];
            String userType;
            String deletedQuery = "DELETE FROM Appointments WHERE AppointmentID = ?";
            if (currentCaregiver == null && currentPatient != null) {
                //if the users logged in as patient
                userType = "PatientUsername";
                username = currentPatient.getUsername();
                cancelQuery = cancelQueryHelper(userType);
            } else {
                //if the users logged in as caregiver
                userType = "CaregiverUsername";
                username = currentCaregiver.getUsername();
                cancelQuery = cancelQueryHelper(userType);
            }

            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            try {
                PreparedStatement statement = con.prepareStatement(cancelQuery);
                ResultSet queryResult = statement.executeQuery();
                statement.setString(1, appointmentID);
                statement.setString(2, username);

                if (queryResult.next()) {
                    PreparedStatement statement2 = con.prepareStatement(deletedQuery);
                    statement2.setString(1, appointmentID);
                    statement2.executeUpdate();
                    System.out.println("** the following appointment " + appointmentID + " has been deleted! **");
                } else {
                    System.out.println("This appointment doesn't exist or you are not authorize to delete it");
                }
            } catch (SQLException e) {
                System.out.println("failed to delete appointment, Please try again");
                e.printStackTrace();
            } finally {
                cm.closeConnection();
            }


        }
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }


//    private static void showAppointmentsQueryHelper(String userType){
//        String username;
//        String query;
//        String accountType;
//        String opposedAccountType;
//        if (userType == "Patient") {
//            //if the user is logged in as caregiver
//            accountType = userType + "Username";
//            opposedAccountType = "PatientUsername";
//            username = currentCaregiver.getUsername();
//            query = "SELECT AppointmentID, Vaccine, Time, " + opposedAccountType +
//                    "FROM Appointments " +
//                    "WHERE " + accountType +" = ?";
//        } else if (userType == "Caregiver") {
//            //if the user is logged in as patient
//            accountType = userType + "Username";
//            opposedAccountType = "CaregiverUsername";
//            username = currentPatient.getUsername();
//            query = "SELECT AppointmentID, Vaccine, Time, " + opposedAccountType +
//                    "FROM Appointments " +
//                    "WHERE " + accountType +" = ?";
//        } else {
//            System.out.println("You haven't logged in yet.");
//            return;
//        }
//    }

    private static String queryGenerator(String accountType){
        String opposedTypeUsername;
        if(accountType.contains("Caregiver")){
            opposedTypeUsername = "PatientUsername";
        }else{
            opposedTypeUsername = "CaregiverUsername";
        }
        String query = "SELECT AppointmentID, Vaccine, Time, " +accountType +
                "FROM Appointments " +
                "WHERE "+ opposedTypeUsername + " = ?";
        return query;
    }

    private static void showAppointments(String[] tokens) {
        String resultingQuery = "";
        String accountType = "";
        String username = "";


        if (currentCaregiver != null && currentPatient == null) {
            //if the user is logged in as caregiver
            username = currentCaregiver.getUsername();
            accountType = "PatientUsername";
            resultingQuery = queryGenerator(accountType);

        } else if (currentCaregiver == null && currentPatient != null) {
            //if the user is logged in as patient
            accountType = "CaregiverUsername";
            username = currentPatient.getUsername();
            resultingQuery = queryGenerator(accountType);

        } else {
            System.out.println("Please login first!");
            return;
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        try {
            PreparedStatement statement = con.prepareStatement(resultingQuery);
            statement.setString(1, username);
            ResultSet queryResult = statement.executeQuery();
            while (queryResult.next()) {
                String date = queryResult.getDate("Time").toString();
                String name = queryResult.getString(accountType).trim();
                String appointmentID = queryResult.getString("AppointmentID").trim();
                String vaccineName = queryResult.getString("Vaccine").trim();
                System.out.println(" -- Name: " + name + " || Date: " + date +
                                "Appointment ID: " + appointmentID + " || Vaccine: " + vaccineName);
            }
        } catch (SQLException e) {
            System.out.println("failed to show appointments");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void logout(String[] tokens) {
        //first make sure that person is logged in before logging them out
        if (currentCaregiver != null || currentPatient != null) {
            //nullify users regardless of type
            currentCaregiver = null;
            currentPatient = null;
            System.out.println("** Successfully logged out **");
        } else {
            System.out.println("Already logged out!");
        }
    }
}
