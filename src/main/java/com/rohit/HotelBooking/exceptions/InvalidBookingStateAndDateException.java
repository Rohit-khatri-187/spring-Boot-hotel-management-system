package com.rohit.HotelBooking.exceptions;

public class InvalidBookingStateAndDateException extends RuntimeException{

    public InvalidBookingStateAndDateException(String message){
        super(message);
    }

}
