package com.rohit.HotelBooking.services;

import com.rohit.HotelBooking.entities.BookingReference;
import com.rohit.HotelBooking.repositories.BookingReferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class BookingCodeGenerator {

    private final BookingReferenceRepository bookingReferenceRepository;

    public String generateBookingReference(){
        String bookingReference;
        // keep generating until a unique code is found
        do {
            // generate code of length 10
            bookingReference = generateRandomAlphaNumericCode(10);

        }while (isBookingReferenceExist(bookingReference));
        // check code if the code already exist . if it does not exist
        //  regenerate

        saveBookingReferenceToDatabase(bookingReference);
        // save the code to the database
        return bookingReference;
    }

    private String generateRandomAlphaNumericCode(int length ){
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789";
        Random random = new Random();
        StringBuilder stringBuilder = new StringBuilder(length);

        for (int i = 0 ; i < length; i++){
            int idx = random.nextInt(characters.length());
            stringBuilder.append(characters.charAt(idx));
        }

        return stringBuilder.toString();
    }

    private boolean isBookingReferenceExist(String bookingReference ){
        return bookingReferenceRepository.findByReferenceNo(bookingReference).isPresent();
    }

    private void saveBookingReferenceToDatabase(String bookingReference){
        BookingReference newBookingReference = BookingReference.builder()
                .referenceNo(bookingReference)
                .build();
        bookingReferenceRepository.save(newBookingReference);
    }


}
