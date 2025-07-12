package com.rohit.HotelBooking.payments.stripe;

import com.rohit.HotelBooking.dtos.NotificationDTO;
import com.rohit.HotelBooking.entities.Booking;
import com.rohit.HotelBooking.entities.PaymentEntity;
import com.rohit.HotelBooking.enums.NotificationType;
import com.rohit.HotelBooking.enums.PaymentGateway;
import com.rohit.HotelBooking.enums.PaymentStatus;
import com.rohit.HotelBooking.exceptions.NotFoundException;
import com.rohit.HotelBooking.payments.stripe.dto.PaymentRequest;
import com.rohit.HotelBooking.repositories.BookingRepository;
import com.rohit.HotelBooking.repositories.PaymentRepository;
import com.rohit.HotelBooking.services.NotificationService;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;

    @Value("${stripe.api.secret.key}")
    private String secretKey;

    // we are

    public String createPaymentIntent(PaymentRequest paymentRequest){
        log.info("Inside createPaymentIntent");

        Stripe.apiKey = secretKey;

        String bookingReference = paymentRequest.getBookingReference();

        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new NotFoundException("Booking Not Found"));


        if (booking.getPaymentStatus() == PaymentStatus.COMPLETED ){
            throw new NotFoundException("Payment already made for this booking");
        }

        try{

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(paymentRequest.getAmount().multiply(BigDecimal.valueOf(100)).longValue())
                    .setCurrency("usd")
                    .putMetadata("bookingReference",bookingReference)
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            return intent.getClientSecret();

        }catch (Exception e){
            throw new RuntimeException("Error creating payment intent");
        }

    }

    public void updatePaymentBooking( PaymentRequest paymentRequest ){
        log.info("Inside updatePaymentBooking");

        String bookingReference = paymentRequest.getBookingReference();

        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(()-> new NotFoundException("Booking Not found") );

        PaymentEntity payment = new PaymentEntity();
        payment.setPaymentGateway(PaymentGateway.STRIPE);
        payment.setAmount(paymentRequest.getAmount());
        payment.setTransactionId(paymentRequest.getTransactionId());
        payment.setPaymentStatus(paymentRequest.isSuccess() ? PaymentStatus.COMPLETED : PaymentStatus.FAILED );
        payment.setPaymentDate(LocalDateTime.now());
        payment.setBookingReference(bookingReference);
        payment.setUser(booking.getUser());

        if( !paymentRequest.isSuccess() ){
            payment.setFailureReason(paymentRequest.getFailureReason());
        }

        paymentRepository.save(payment);

        NotificationDTO notificationDTO = NotificationDTO.builder()
                .recipient(booking.getUser().getEmail())
                .type(NotificationType.EMAIL)
                .bookingReference(bookingReference)
                .build();

        log.info("About to send notification inside  by email");

        if ( paymentRequest.isSuccess() ){
            booking.setPaymentStatus(PaymentStatus.COMPLETED);
            bookingRepository.save(booking);

            notificationDTO.setSubject("Booking Payment Successful");
            notificationDTO.setBody("Congratulation!! Your payment for booking with reference : "+ bookingReference + "is successful");
            notificationService.sendEmail(notificationDTO);

        }else {
            booking.setPaymentStatus(PaymentStatus.COMPLETED);
            bookingRepository.save(booking);

            notificationDTO.setSubject("Booking Payment Failed");
            notificationDTO.setBody("Your payment for booking with reference : "+ bookingReference + " failed with reason " + paymentRequest.getFailureReason() );
            notificationService.sendEmail(notificationDTO);

        }

    }

}
