package com.orderplatform.shipping.kafka;

import com.orderplatform.events.Topics;
import com.orderplatform.events.commands.CancelShippingCommand;
import com.orderplatform.events.commands.ScheduleShippingCommand;
import com.orderplatform.shipping.service.ShippingService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ShippingCommandListener {

    private final ShippingService service;

    public ShippingCommandListener(ShippingService service) {
        this.service = service;
    }

    @KafkaListener(topics = Topics.SCHEDULE_SHIPPING, groupId = "shipping-service")
    public void onSchedule(ScheduleShippingCommand cmd) {
        service.handleSchedule(cmd);
    }

    @KafkaListener(topics = Topics.CANCEL_SHIPPING, groupId = "shipping-service")
    public void onCancel(CancelShippingCommand cmd) {
        service.handleCancel(cmd);
    }
}
