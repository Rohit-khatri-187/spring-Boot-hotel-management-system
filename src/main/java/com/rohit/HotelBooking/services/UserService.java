package com.rohit.HotelBooking.services;

import com.rohit.HotelBooking.dtos.LoginRequest;
import com.rohit.HotelBooking.dtos.RegistrationRequest;
import com.rohit.HotelBooking.dtos.Response;
import com.rohit.HotelBooking.dtos.UserDTO;
import com.rohit.HotelBooking.entities.User;

public interface UserService {

    Response registerUser(RegistrationRequest registrationRequest);

    Response loginUser(LoginRequest loginRequest);

    Response getAllUsers();

    Response getOwnAccountDetails();

    User getCurrentLoggedInUser();

    Response updateOwnAccount(UserDTO userDTO);

    Response deleteOwnAccount();

    Response getMyBookingHistory();

}
