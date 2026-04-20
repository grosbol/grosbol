package com.example.confluence.sso.saml;

import net.shibboleth.utilities.java.support.xml.SerializeSupport;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Marshaller;
import org.opensaml.core.xml.io.MarshallerFactory;
import org.w3c.dom.Element;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/** Utility methods for encoding/decoding SAML XML objects. */
final class SAMLEncodingUtil {

    private SAMLEncodingUtil() {}

    /**
     * Marshal an {@link XMLObject} to an XML string.
     */
    static String marshalToString(XMLObject obj) throws Exception {
        MarshallerFactory factory = XMLObjectProviderRegistrySupport.getMarshallerFactory();
        Marshaller marshaller = factory.getMarshaller(obj);
        if (marshaller == null) {
            throw new IllegalArgumentException("No marshaller for " + obj.getClass().getName());
        }
        Element element = marshaller.marshall(obj);
        return SerializeSupport.prettyPrintXML(element);
    }

    /**
     * Encode an {@link XMLObject} for the SAML HTTP-Redirect binding:
     * marshal → deflate → base64 → URL-encode.
     */
    static String encodeForRedirect(XMLObject obj) throws Exception {
        String xml = marshalToString(obj);
        byte[] xmlBytes = xml.getBytes(StandardCharsets.UTF_8);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Deflater deflater = new Deflater(Deflater.DEFLATED, true);
        try (DeflaterOutputStream dos = new DeflaterOutputStream(bos, deflater)) {
            dos.write(xmlBytes);
        }

        return java.net.URLEncoder.encode(
            Base64.getEncoder().encodeToString(bos.toByteArray()),
            StandardCharsets.UTF_8);
    }
}
