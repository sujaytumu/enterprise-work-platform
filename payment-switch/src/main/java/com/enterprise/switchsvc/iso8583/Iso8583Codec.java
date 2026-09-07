package com.enterprise.switchsvc.iso8583;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Encodes/decodes Iso8583Message to/from a JSON wire format for this demo.
 * Real switches speak binary ISO 8583 over TCP with bitmap field presence
 * indicators; JSON is used here so the message model and routing logic can
 * be demonstrated over plain HTTP without a binary codec layer.
 */
public class Iso8583Codec {

    private final ObjectMapper mapper = new ObjectMapper();

    public ObjectNode toJson(Iso8583Message msg) {
        ObjectNode node = mapper.createObjectNode();
        node.put("mti", msg.getMti().getCode());
        ObjectNode fields = node.putObject("fields");
        if (msg.getPan() != null) fields.put("2", msg.getPan());
        if (msg.getAmount() != null) fields.put("4", msg.getAmount().toPlainString());
        if (msg.getStan() != null) fields.put("11", msg.getStan());
        if (msg.getResponseCode() != null) fields.put("39", msg.getResponseCode());
        if (msg.getTerminalId() != null) fields.put("41", msg.getTerminalId());
        if (msg.getMerchantId() != null) fields.put("42", msg.getMerchantId());
        return node;
    }

    public Iso8583Message fromJson(ObjectNode node) {
        Iso8583Message msg = new Iso8583Message();
        msg.setMti(Iso8583Message.MTI.fromCode(node.get("mti").asText()));
        ObjectNode fields = (ObjectNode) node.get("fields");
        fields.fieldNames().forEachRemaining(name ->
                msg.setField(Integer.parseInt(name), fields.get(name).asText()));
        return msg;
    }
}
