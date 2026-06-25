package com.orderplatform.payment.payu;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * PayU merchant configuration. Secrets come from the environment (PAYU_KEY / PAYU_SALT); the
 * defaults in application.yml are the test-merchant credentials for local development only.
 */
@ConfigurationProperties(prefix = "payu")
public class PayuProperties {

    /** PayU hosted-checkout endpoint. Test: https://test.payu.in/_payment */
    private String paymentUrl = "https://test.payu.in/_payment";

    /** Merchant key. */
    private String key;

    /** Merchant salt (used for request + response hashing). Never log this. */
    private String salt;

    /** Success URL PayU browser-POSTs to. Points straight at this service (bypasses the gateway). */
    private String surl;

    /** Failure URL PayU browser-POSTs to. */
    private String furl;

    /** Where the callback sends the browser back to (the SPA's order page base). */
    private String frontendReturnUrl = "http://localhost:5173";

    /** Fallback customer details when the order/profile doesn't carry them (PayU requires them). */
    private String defaultEmail = "test@example.com";
    private String defaultPhone = "9999999999";

    public String getPaymentUrl() {
        return paymentUrl;
    }

    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getSurl() {
        return surl;
    }

    public void setSurl(String surl) {
        this.surl = surl;
    }

    public String getFurl() {
        return furl;
    }

    public void setFurl(String furl) {
        this.furl = furl;
    }

    public String getFrontendReturnUrl() {
        return frontendReturnUrl;
    }

    public void setFrontendReturnUrl(String frontendReturnUrl) {
        this.frontendReturnUrl = frontendReturnUrl;
    }

    public String getDefaultEmail() {
        return defaultEmail;
    }

    public void setDefaultEmail(String defaultEmail) {
        this.defaultEmail = defaultEmail;
    }

    public String getDefaultPhone() {
        return defaultPhone;
    }

    public void setDefaultPhone(String defaultPhone) {
        this.defaultPhone = defaultPhone;
    }
}
