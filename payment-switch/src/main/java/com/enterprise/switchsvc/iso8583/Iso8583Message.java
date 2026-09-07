package com.enterprise.switchsvc.iso8583;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Models a practical subset of an ISO 8583 financial message: MTI + the
 * handful of data elements needed to demonstrate the authorization flow.
 * The real spec defines 128 possible data elements with bitmap-based
 * field presence encoding — this class captures the pattern (MTI-driven
 * message type, keyed field access) without the full bitmap implementation.
 *
 * Field numbers used here follow the real spec's numbering where practical:
 *  - DE2:  Primary Account Number (here: card token, not raw PAN)
 *  - DE4:  Transaction amount
 *  - DE11: System trace audit number (STAN)
 *  - DE39: Response code
 *  - DE41: Card acceptor terminal ID
 *  - DE42: Card acceptor (merchant) ID
 */
public class Iso8583Message {

    public enum MTI {
        AUTHORIZATION_REQUEST("0100"),
        AUTHORIZATION_RESPONSE("0110"),
        REVERSAL_REQUEST("0400"),
        REVERSAL_RESPONSE("0410");

        private final String code;
        MTI(String code) { this.code = code; }
        public String getCode() { return code; }

        public static MTI fromCode(String code) {
            for (MTI mti : values()) {
                if (mti.code.equals(code)) return mti;
            }
            throw new IllegalArgumentException("Unknown MTI: " + code);
        }
    }

    private MTI mti;
    private final Map<Integer, String> fields = new HashMap<>();

    public MTI getMti() { return mti; }
    public void setMti(MTI mti) { this.mti = mti; }

    public void setField(int number, String value) { fields.put(number, value); }
    public String getField(int number) { return fields.get(number); }

    public String getPan() { return getField(2); }
    public BigDecimal getAmount() {
        String v = getField(4);
        return v == null ? null : new BigDecimal(v);
    }
    public String getStan() { return getField(11); }
    public String getResponseCode() { return getField(39); }
    public String getTerminalId() { return getField(41); }
    public String getMerchantId() { return getField(42); }

    public static Iso8583Message authorizationRequest(String pan, BigDecimal amount, String merchantId, String stan) {
        Iso8583Message msg = new Iso8583Message();
        msg.setMti(MTI.AUTHORIZATION_REQUEST);
        msg.setField(2, pan);
        msg.setField(4, amount.toPlainString());
        msg.setField(11, stan);
        msg.setField(42, merchantId);
        return msg;
    }

    public static Iso8583Message authorizationResponse(Iso8583Message request, String responseCode) {
        Iso8583Message msg = new Iso8583Message();
        msg.setMti(MTI.AUTHORIZATION_RESPONSE);
        msg.setField(2, request.getPan());
        msg.setField(4, request.getField(4));
        msg.setField(11, request.getStan());
        msg.setField(39, responseCode);
        msg.setField(42, request.getMerchantId());
        return msg;
    }
}
