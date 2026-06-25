package com.orderplatform.payment.web;

import com.orderplatform.payment.payu.PayuProperties;
import com.orderplatform.payment.service.PaymentService;
import com.orderplatform.payment.service.PaymentService.CallbackResult;
import com.orderplatform.payment.service.PaymentService.PayuCheckout;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/payments/payu")
public class PayuController {

    private final PaymentService service;
    private final PayuProperties props;

    public PayuController(PaymentService service, PayuProperties props) {
        this.service = service;
        this.props = props;
    }

    /**
     * Returns the PayU action URL and the form fields (incl. hash) the SPA must POST to it. Routed
     * through the gateway (authenticated). 204 while no PENDING payment exists yet — the frontend
     * polls until the saga has reached the payment step.
     */
    @GetMapping("/checkout/{orderId}")
    public ResponseEntity<PayuCheckout> checkout(@PathVariable Long orderId) {
        return service.checkoutFor(orderId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * PayU posts here (via the customer's browser) after success or failure. We verify the hash,
     * resolve the payment, resume the saga, then 302 the browser back to the SPA's order page.
     * Public by design: it reaches this service directly (not the gateway), with no JWT.
     */
    @PostMapping("/callback")
    public ResponseEntity<Void> callback(@RequestParam Map<String, String> params) {
        CallbackResult result = service.confirmFromCallback(params);
        String location;
        if (result.orderId() != null) {
            location = props.getFrontendReturnUrl() + "/orders/" + result.orderId()
                    + "?payment=" + (result.success() ? "success" : "failed");
        } else {
            location = props.getFrontendReturnUrl() + "/?payment=error";
        }
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(location)).build();
    }
}
