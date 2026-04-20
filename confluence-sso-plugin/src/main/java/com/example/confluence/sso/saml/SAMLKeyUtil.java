package com.example.confluence.sso.saml;

import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml.saml2.metadata.UsageType;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.X509Certificate;
import org.opensaml.xmlsec.signature.X509Data;

import javax.xml.namespace.QName;

/** Utility for building OpenSAML KeyDescriptor elements from PEM certificates. */
final class SAMLKeyUtil {

    private SAMLKeyUtil() {}

    static KeyDescriptor buildKeyDescriptor(String pemCertificate, UsageType usageType) {
        String certValue = pemCertificate
            .replace("-----BEGIN CERTIFICATE-----", "")
            .replace("-----END CERTIFICATE-----", "")
            .replaceAll("\\s", "");

        X509Certificate x509Cert = build(X509Certificate.DEFAULT_ELEMENT_NAME);
        x509Cert.setValue(certValue);

        X509Data x509Data = build(X509Data.DEFAULT_ELEMENT_NAME);
        x509Data.getX509Certificates().add(x509Cert);

        KeyInfo keyInfo = build(KeyInfo.DEFAULT_ELEMENT_NAME);
        keyInfo.getX509Datas().add(x509Data);

        KeyDescriptor kd = build(KeyDescriptor.DEFAULT_ELEMENT_NAME);
        kd.setUse(usageType);
        kd.setKeyInfo(keyInfo);

        return kd;
    }

    @SuppressWarnings("unchecked")
    private static <T> T build(QName name) {
        return (T) XMLObjectProviderRegistrySupport
            .getBuilderFactory()
            .getBuilder(name)
            .buildObject(name);
    }
}
