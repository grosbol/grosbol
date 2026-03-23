package com.example.confluence.sso.saml;

import com.example.confluence.sso.config.SSOConfig;
import net.shibboleth.utilities.java.support.xml.ParserPool;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.io.UnmarshallerFactory;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.*;
import org.opensaml.saml.saml2.metadata.*;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.xmlsec.signature.support.SignatureValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Core SAML 2.0 handler: builds AuthnRequests, parses/validates Responses,
 * and generates SP metadata.
 *
 * OpenSAML is initialised once via a static block so it is safe to
 * instantiate this class multiple times (e.g. after a config reload).
 */
public class SAMLHandler {

    private static final Logger log = LoggerFactory.getLogger(SAMLHandler.class);

    static {
        try {
            InitializationService.initialize();
            log.info("OpenSAML initialised successfully.");
        } catch (Exception e) {
            throw new ExceptionInInitializerError("Failed to initialise OpenSAML: " + e.getMessage());
        }
    }

    private final SSOConfig config;

    public SAMLHandler(SSOConfig config) {
        this.config = config;
    }

    // ── AuthnRequest ──────────────────────────────────────────────────────────

    /**
     * Build a SAML AuthnRequest and return its Base64 + URL-encoded form
     * ready for the HTTP-Redirect binding.
     *
     * @param relayState opaque value echoed back by the IdP (e.g. returnTo URL)
     * @return {@link SAMLRequestData} containing the redirect URL
     */
    public SAMLRequestData buildAuthnRequest(String relayState) throws Exception {
        AuthnRequest authnRequest = buildXmlObject(AuthnRequest.DEFAULT_ELEMENT_NAME);

        authnRequest.setID("_" + UUID.randomUUID().toString().replace("-", ""));
        authnRequest.setVersion(SAMLVersion.VERSION_20);
        authnRequest.setIssueInstant(Instant.now());
        authnRequest.setDestination(config.getSamlIdpSsoUrl());
        authnRequest.setProtocolBinding("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");
        authnRequest.setAssertionConsumerServiceURL(config.getSamlSpAcsUrl());
        authnRequest.setIsPassive(false);
        authnRequest.setForceAuthn(false);

        // Issuer
        Issuer issuer = buildXmlObject(Issuer.DEFAULT_ELEMENT_NAME);
        issuer.setValue(config.getSamlSpEntityId());
        authnRequest.setIssuer(issuer);

        // NameIDPolicy
        NameIDPolicy nameIDPolicy = buildXmlObject(NameIDPolicy.DEFAULT_ELEMENT_NAME);
        nameIDPolicy.setFormat(NameIDType.UNSPECIFIED);
        nameIDPolicy.setAllowCreate(true);
        authnRequest.setNameIDPolicy(nameIDPolicy);

        // Requested AuthnContext (password or better)
        RequestedAuthnContext rac = buildXmlObject(RequestedAuthnContext.DEFAULT_ELEMENT_NAME);
        rac.setComparison(AuthnContextComparisonTypeIdentifier.MINIMUM);
        AuthnContextClassRef classRef = buildXmlObject(AuthnContextClassRef.DEFAULT_ELEMENT_NAME);
        classRef.setURI("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport");
        rac.getAuthnContextClassRefs().add(classRef);
        authnRequest.setRequestedAuthnContext(rac);

        String idpUrl = config.getSamlIdpSsoUrl();
        String encoded = SAMLEncodingUtil.encodeForRedirect(authnRequest);

        StringBuilder redirectUrl = new StringBuilder(idpUrl)
            .append(idpUrl.contains("?") ? "&" : "?")
            .append("SAMLRequest=").append(encoded);

        if (relayState != null && !relayState.isBlank()) {
            redirectUrl.append("&RelayState=").append(
                java.net.URLEncoder.encode(relayState, StandardCharsets.UTF_8));
        }

        return new SAMLRequestData(authnRequest.getID(), redirectUrl.toString());
    }

    // ── Response parsing ──────────────────────────────────────────────────────

    /**
     * Parse, decrypt (if needed), and validate a Base64-encoded SAML Response
     * received via HTTP-POST binding.
     *
     * @param samlResponseBase64 raw value of the {@code SAMLResponse} POST parameter
     * @return populated {@link SAMLUserInfo}
     * @throws SAMLException if the response is invalid, expired, or untrusted
     */
    public SAMLUserInfo parseAndValidateResponse(String samlResponseBase64) throws SAMLException {
        try {
            byte[] decoded = Base64.getDecoder().decode(samlResponseBase64);
            Document doc   = parseXml(decoded);
            Element  root  = doc.getDocumentElement();

            UnmarshallerFactory factory = XMLObjectProviderRegistrySupport.getUnmarshallerFactory();
            Unmarshaller unmarshaller   = factory.getUnmarshaller(root);
            if (unmarshaller == null) {
                throw new SAMLException("No unmarshaller found for SAML Response element.");
            }

            XMLObject xmlObject = unmarshaller.unmarshall(root);
            if (!(xmlObject instanceof Response)) {
                throw new SAMLException("Parsed XML is not a SAML Response.");
            }

            Response response = (Response) xmlObject;

            // ── Status check ─────────────────────────────────────────────────
            StatusCode statusCode = response.getStatus().getStatusCode();
            if (!StatusCode.SUCCESS.equals(statusCode.getValue())) {
                throw new SAMLException("IdP returned non-success status: " + statusCode.getValue());
            }

            // ── Signature validation ──────────────────────────────────────────
            if (config.isSamlWantAssertionsSigned()) {
                Credential idpCredential = loadIdpCredential();
                validateSignature(response, idpCredential);
            }

            // ── Extract assertion ─────────────────────────────────────────────
            List<Assertion> assertions = response.getAssertions();
            if (assertions.isEmpty()) {
                throw new SAMLException("SAML Response contains no assertions.");
            }
            Assertion assertion = assertions.get(0);

            // ── Audience restriction ──────────────────────────────────────────
            validateAudienceRestriction(assertion);

            // ── Time validity ─────────────────────────────────────────────────
            validateConditions(assertion);

            // ── Extract user info ─────────────────────────────────────────────
            return extractUserInfo(assertion);

        } catch (SAMLException e) {
            throw e;
        } catch (Exception e) {
            throw new SAMLException("Failed to parse SAML response: " + e.getMessage(), e);
        }
    }

    // ── SP Metadata ───────────────────────────────────────────────────────────

    /**
     * Generate SP metadata XML for registration with the IdP.
     */
    public String generateSpMetadata() throws Exception {
        EntityDescriptor entityDescriptor = buildXmlObject(EntityDescriptor.DEFAULT_ELEMENT_NAME);
        entityDescriptor.setEntityID(config.getSamlSpEntityId());

        SPSSODescriptor spssoDescriptor = buildXmlObject(SPSSODescriptor.DEFAULT_ELEMENT_NAME);
        spssoDescriptor.setAuthnRequestsSigned(config.isSamlSignRequests());
        spssoDescriptor.setWantAssertionsSigned(config.isSamlWantAssertionsSigned());
        spssoDescriptor.addSupportedProtocol(SAMLConstants.SAML20P_NS);

        // ACS
        AssertionConsumerService acs = buildXmlObject(AssertionConsumerService.DEFAULT_ELEMENT_NAME);
        acs.setBinding("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");
        acs.setLocation(config.getSamlSpAcsUrl());
        acs.setIndex(1);
        acs.setIsDefault(true);
        spssoDescriptor.getAssertionConsumerServices().add(acs);

        // SLO
        if (config.getSamlSpSloUrl() != null && !config.getSamlSpSloUrl().isBlank()) {
            SingleLogoutService slo = buildXmlObject(SingleLogoutService.DEFAULT_ELEMENT_NAME);
            slo.setBinding("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect");
            slo.setLocation(config.getSamlSpSloUrl());
            spssoDescriptor.getSingleLogoutServices().add(slo);
        }

        // SP certificate (for key descriptor)
        if (config.getSamlSpCertificate() != null && !config.getSamlSpCertificate().isBlank()) {
            spssoDescriptor.getKeyDescriptors().add(
                SAMLKeyUtil.buildKeyDescriptor(config.getSamlSpCertificate(), UsageType.SIGNING));
            spssoDescriptor.getKeyDescriptors().add(
                SAMLKeyUtil.buildKeyDescriptor(config.getSamlSpCertificate(), UsageType.ENCRYPTION));
        }

        entityDescriptor.getRoleDescriptors().add(spssoDescriptor);

        return SAMLEncodingUtil.marshalToString(entityDescriptor);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private <T extends XMLObject> T buildXmlObject(QName name) {
        return (T) XMLObjectProviderRegistrySupport
            .getBuilderFactory()
            .getBuilder(name)
            .buildObject(name);
    }

    private Document parseXml(byte[] bytes) throws Exception {
        ParserPool parserPool = XMLObjectProviderRegistrySupport.getParserPool();
        return parserPool.parse(new ByteArrayInputStream(bytes));
    }

    private Credential loadIdpCredential() throws Exception {
        String pem = config.getSamlIdpCertificate()
            .replace("-----BEGIN CERTIFICATE-----", "")
            .replace("-----END CERTIFICATE-----", "")
            .replaceAll("\\s", "");

        byte[] certBytes = Base64.getDecoder().decode(pem);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert  = (X509Certificate) cf.generateCertificate(
            new ByteArrayInputStream(certBytes));

        return new BasicX509Credential(cert);
    }

    private void validateSignature(Response response, Credential credential) throws SAMLException {
        try {
            if (response.isSigned()) {
                SignatureValidator.validate(response.getSignature(), credential);
                return;
            }
            for (Assertion a : response.getAssertions()) {
                if (a.isSigned()) {
                    SignatureValidator.validate(a.getSignature(), credential);
                    return;
                }
            }
            throw new SAMLException("Neither the Response nor any Assertion is signed.");
        } catch (Exception e) {
            throw new SAMLException("Signature validation failed: " + e.getMessage(), e);
        }
    }

    private void validateAudienceRestriction(Assertion assertion) throws SAMLException {
        Conditions conditions = assertion.getConditions();
        if (conditions == null) return;

        String spEntityId = config.getSamlSpEntityId();
        for (AudienceRestriction ar : conditions.getAudienceRestrictions()) {
            for (Audience audience : ar.getAudiences()) {
                if (spEntityId.equals(audience.getURI())) return;
            }
        }
        throw new SAMLException("SP entity ID '" + spEntityId + "' not in assertion audience.");
    }

    private void validateConditions(Assertion assertion) throws SAMLException {
        Conditions conditions = assertion.getConditions();
        if (conditions == null) return;

        Instant now = Instant.now();
        if (conditions.getNotBefore() != null && now.isBefore(conditions.getNotBefore())) {
            throw new SAMLException("Assertion not yet valid (notBefore=" + conditions.getNotBefore() + ")");
        }
        if (conditions.getNotOnOrAfter() != null && !now.isBefore(conditions.getNotOnOrAfter())) {
            throw new SAMLException("Assertion has expired (notOnOrAfter=" + conditions.getNotOnOrAfter() + ")");
        }
    }

    private SAMLUserInfo extractUserInfo(Assertion assertion) throws SAMLException {
        // NameID
        Subject subject = assertion.getSubject();
        if (subject == null || subject.getNameID() == null) {
            throw new SAMLException("Assertion has no Subject/NameID.");
        }
        String nameId = subject.getNameID().getValue();

        // Attributes
        String email     = null;
        String fullName  = null;
        java.util.List<String> groups = new java.util.ArrayList<>();

        String attrEmail    = config.getSamlAttributeEmail();
        String attrFullName = config.getSamlAttributeFullName();
        String attrGroups   = config.getSamlAttributeGroups();

        for (AttributeStatement stmt : assertion.getAttributeStatements()) {
            for (Attribute attr : stmt.getAttributes()) {
                String attrName = attr.getName();
                List<XMLObject> values = attr.getAttributeValues();
                if (values.isEmpty()) continue;

                String firstValue = values.get(0).getDOM() != null
                    ? values.get(0).getDOM().getTextContent()
                    : "";

                if (attrName.equals(attrEmail)) {
                    email = firstValue;
                } else if (attrName.equals(attrFullName)) {
                    fullName = firstValue;
                } else if (attrName.equals(attrGroups)) {
                    for (XMLObject v : values) {
                        if (v.getDOM() != null) groups.add(v.getDOM().getTextContent());
                    }
                }
            }
        }

        return new SAMLUserInfo(nameId, email, fullName, groups,
            subject.getNameID().getSPNameQualifier());
    }

    // ── Inner types ───────────────────────────────────────────────────────────

    /** Holds the redirect URL and request ID for in-flight SAML requests. */
    public static final class SAMLRequestData {
        public final String requestId;
        public final String redirectUrl;
        public SAMLRequestData(String requestId, String redirectUrl) {
            this.requestId   = requestId;
            this.redirectUrl = redirectUrl;
        }
    }
}
