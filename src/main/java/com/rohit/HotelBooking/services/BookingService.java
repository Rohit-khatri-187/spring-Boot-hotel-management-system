package com.rohit.HotelBooking.services;

import com.rohit.HotelBooking.dtos.BookingDTO;
import com.rohit.HotelBooking.dtos.Response;

public interface BookingService {

    Response getAllBookings();
    Response createBooking(BookingDTO bookingDTO);
    Response findBookingByReferenceNo(String bookingReference);
    Response updateBooking(BookingDTO bookingDTO);

}
