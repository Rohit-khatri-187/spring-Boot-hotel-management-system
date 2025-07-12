package com.rohit.HotelBooking.services;

import com.rohit.HotelBooking.dtos.NotificationDTO;

public interface NotificationService {

    void sendEmail(NotificationDTO notificationDTO);

    void sendSms();

    void sendWhatsapp();

}
