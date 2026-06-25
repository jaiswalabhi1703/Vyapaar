package com.orderplatform.payment.payu;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stateless helper for PayU's hosted-checkout protocol: builds the request parameter set with its
 * SHA-512 request hash, and verifies the SHA-512 response hash PayU returns on the callback.
 *
 * <p>Request hash sequence:
 * {@code key|txnid|amount|productinfo|firstname|email|udf1|udf2|udf3|udf4|udf5||||||salt}
 *
 * <p>Reverse (response) hash sequence — the mirror, with salt and key swapped to the ends:
 * {@code salt|status|udf10|udf9|udf8|udf7|udf6|udf5|udf4|udf3|udf2|udf1|email|firstname|productinfo|amount|txnid|key}
 *
 * <p>We use udf1=sagaId and udf2=orderId so the saga can be resumed from the callback without a
 * separate lookup, and because udf fields are covered by the hash they cannot be tampered with.
 */
@Component
public class PayuClient {

    private final PayuProperties props;

    public PayuClient(PayuProperties props) {
        this.props = props;
    }

    /** PayU wants the amount as a plain 2-decimal string (e.g. "4999.00"). */
    public static String formatAmount(BigDecimal amount) {
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * Build the full set of form fields to POST to {@link PayuProperties#getPaymentUrl()},
     * including the computed request hash. udf1=sagaId, udf2=orderId.
     */
    public Map<String, String> buildCheckoutParams(String txnid, BigDecimal amount, String productinfo,
                                                    String firstname, String email, String phone,
                                                    String sagaId, Long orderId) {
        String amt = formatAmount(amount);
        String[] udf = {sagaId, String.valueOf(orderId), "", "", ""};

        String hash = requestHash(txnid, amt, productinfo, firstname, email, udf);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("key", props.getKey());
        params.put("txnid", txnid);
        params.put("amount", amt);
        params.put("productinfo", productinfo);
        params.put("firstname", firstname);
        params.put("email", email);
        params.put("phone", phone);
        params.put("surl", props.getSurl());
        params.put("furl", props.getFurl());
        params.put("udf1", udf[0]);
        params.put("udf2", udf[1]);
        params.put("udf3", udf[2]);
        params.put("udf4", udf[3]);
        params.put("udf5", udf[4]);
        params.put("hash", hash);
        return params;
    }

    /** Forward (request) hash. */
    public String requestHash(String txnid, String amount, String productinfo,
                              String firstname, String email, String[] udf) {
        String seq = String.join("|",
                props.getKey(), txnid, amount, productinfo, firstname, email,
                udf[0], udf[1], udf[2], udf[3], udf[4],
                "", "", "", "", "",   // udf6..udf10 (unused)
                props.getSalt());
        return sha512(seq);
    }

    /**
     * Recompute the reverse hash from the values PayU posted back and compare to what it sent.
     * Uses the echoed amount/fields verbatim so any normalization on PayU's side still matches.
     */
    public boolean verifyResponse(Map<String, String> p) {
        String received = p.getOrDefault("hash", "");
        String status = p.getOrDefault("status", "");
        String[] udf = {
                p.getOrDefault("udf1", ""), p.getOrDefault("udf2", ""), p.getOrDefault("udf3", ""),
                p.getOrDefault("udf4", ""), p.getOrDefault("udf5", "")
        };
        String seq = String.join("|",
                props.getSalt(), status,
                "", "", "", "", "",   // udf10..udf6 (unused)
                udf[4], udf[3], udf[2], udf[1], udf[0],
                p.getOrDefault("email", ""), p.getOrDefault("firstname", ""),
                p.getOrDefault("productinfo", ""), p.getOrDefault("amount", ""),
                p.getOrDefault("txnid", ""), props.getKey());
        String expected = sha512(seq);
        return !received.isEmpty() && constantTimeEquals(expected, received.toLowerCase());
    }

    private static String sha512(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-512 not available", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
