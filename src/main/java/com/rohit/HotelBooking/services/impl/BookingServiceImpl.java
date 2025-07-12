package com.rohit.HotelBooking.services.impl;

import com.rohit.HotelBooking.dtos.BookingDTO;
import com.rohit.HotelBooking.dtos.NotificationDTO;
import com.rohit.HotelBooking.dtos.Response;
import com.rohit.HotelBooking.entities.Booking;
import com.rohit.HotelBooking.entities.Room;
import com.rohit.HotelBooking.entities.User;
import com.rohit.HotelBooking.enums.BookingStatus;
import com.rohit.HotelBooking.enums.PaymentStatus;
import com.rohit.HotelBooking.exceptions.InvalidBookingStateAndDateException;
import com.rohit.HotelBooking.exceptions.NotFoundException;
import com.rohit.HotelBooking.repositories.BookingRepository;
import com.rohit.HotelBooking.repositories.RoomRepository;
import com.rohit.HotelBooking.services.BookingCodeGenerator;
import com.rohit.HotelBooking.services.BookingService;
import com.rohit.HotelBooking.services.NotificationService;
import com.rohit.HotelBooking.services.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;


@Service
@Slf4j
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final NotificationService notificationService;
    private final ModelMapper modelMapper;
    private final UserService userService;

    private final BookingCodeGenerator bookingCodeGenerator;


    @Override
    public Response getAllBookings() {
        List<Booking> bookingList = bookingRepository.findAll(
                Sort.by(Sort.Direction.DESC,"id")
        );
        List<BookingDTO> bookingDTOList = modelMapper.map(bookingList,
                new TypeToken<List<BookingDTO>>() {}.getType());

        for (BookingDTO bookingDTO : bookingDTOList){
            bookingDTO.setUser(null);
            bookingDTO.setRoom(null);
        }


        return Response.builder()
                .status(200)
                .message("success")
                .bookings(bookingDTOList)
                .build();
    }

    @Override
    public Response createBooking(BookingDTO bookingDTO) {
        User currUser = userService.getCurrentLoggedInUser();
        Room room = roomRepository.findById(bookingDTO.getRoomId())
                .orElseThrow( () -> new NotFoundException("Room not found"));

        // validation: Ensure the check in date is not before today
        if (bookingDTO.getCheckInDate().isBefore(LocalDate.now()) ){
            throw new InvalidBookingStateAndDateException("Check in date cannot be before today");
        }

        // validation: ensure the check-out date is not before check-in date
        if(bookingDTO.getCheckOutDate().isBefore(bookingDTO.getCheckInDate())){
            throw new InvalidBookingStateAndDateException("Check out date cannot before check in date");
        }

        // validation: ensure the check-in date is not same as checkout date
        if(bookingDTO.getCheckInDate().isEqual(bookingDTO.getCheckOutDate())){
            throw new InvalidBookingStateAndDateException("check in date cannot be equal to the check out date");
        }

        // validate room available
        boolean isAvailable = bookingRepository.isRoomAvailable(room.getId(), bookingDTO.getCheckInDate() , bookingDTO.getCheckOutDate() );

        if (!isAvailable){
            throw new InvalidBookingStateAndDateException(
                    "Room is not available for the selected date range"
            );
        }

        // calculate the total price needed to pay for stay
        BigDecimal totalPrice = calculateTotalPrice(room,bookingDTO);

        String bookingReference = bookingCodeGenerator.generateBookingReference();

        //create and save the booking
        Booking booking = new Booking();
        booking.setUser(currUser);
        booking.setRoom(room);
        booking.setCheckInDate(bookingDTO.getCheckInDate());
        booking.setCheckOutDate(bookingDTO.getCheckOutDate());
        booking.setTotalPrice(totalPrice);
        booking.setBookingReference(bookingReference);
        booking.setBookingStatus(BookingStatus.BOOKED);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setCreatedAt(LocalDateTime.now());

        bookingRepository.save(booking);

        // generate the payment url which will be sent via email
        String paymentUrl = "http://localhost:3000/payment/"+ bookingReference + "/" + totalPrice;

        log.info("PAYMENT LINK : {}",paymentUrl);

        //send notification via email
        NotificationDTO notificationDTO = NotificationDTO.builder()
                .recipient(currUser.getEmail())
                .subject("Booking Confirmation")
                .body(String.format("Your booking has been created successfully. please proceed with your payment using the payment link below."
                + "\n\n%s ",paymentUrl
                ))
                .bookingReference(bookingReference)
                .build();

        notificationService.sendEmail(notificationDTO);

        return Response.builder()
                .status(200)
                .message("Booking successfully")
                .booking(bookingDTO)
                .build();

    }


    @Override
    public Response findBookingByReferenceNo(String bookingReference) {

        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow( ()-> new NotFoundException("Booking with reference No : "+ bookingReference + "Not found" ) );

        BookingDTO  bookingDTO = modelMapper.map(booking,BookingDTO.class);

        return Response.builder()
                .status(200)
                .message("success")
                .booking(bookingDTO)
                .build();
    }

    @Override
    public Response updateBooking(BookingDTO bookingDTO) {

        if(bookingDTO.getId() == null){
            throw new NotFoundException("Booking id is required");
        }

        Booking existingBooking = bookingRepository.findById(bookingDTO.getId())
                .orElseThrow( ()-> new NotFoundException("Booking Not Found"));

        if (bookingDTO.getBookingStatus() != null){
            existingBooking.setBookingStatus(bookingDTO.getBookingStatus());
        }

        if (bookingDTO.getPaymentStatus() != null){
            existingBooking.setPaymentStatus(bookingDTO.getPaymentStatus());
        }

        bookingRepository.save(existingBooking);

        return Response.builder()
                .status(200)
                .message("Booking Updated Successfully")
                .build();

    }


    private BigDecimal calculateTotalPrice(Room room, BookingDTO bookingDTO) {
        BigDecimal pricePerNight = room.getPricePerNight();
        long days = ChronoUnit.DAYS.between(bookingDTO.getCheckInDate(),bookingDTO.getCheckOutDate());

        return pricePerNight.multiply(BigDecimal.valueOf(days));
    }

}
