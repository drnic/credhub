package io.pivotal.security.controller.v1;

import static com.greghaskins.spectrum.Spectrum.describe;
import static com.greghaskins.spectrum.Spectrum.it;
import static io.pivotal.security.helper.SpectrumHelper.itThrows;
import static io.pivotal.security.helper.SpectrumHelper.itThrowsWithMessage;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.greghaskins.spectrum.Spectrum;
import io.pivotal.security.exceptions.ParameterizedValidationException;
import io.pivotal.security.util.CertificateReader;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.junit.runner.RunWith;

@RunWith(Spectrum.class)
public class CertificateSecretParametersTest {

  private CertificateSecretParameters certificateSecretParameters;

  {
    describe("when not given a certificate string", () -> {
      it("constructs DN string correctly from parameters", () -> {
        CertificateSecretParameters params = new CertificateSecretParameters()
            .setCountry("My Country")
            .setState("My State")
            .setOrganization("My Organization")
            .setOrganizationUnit("My Organization Unit")
            .setCommonName("My Common Name")
            .setLocality("My Locality");

        assertThat(
            params.getDn().toString(),
            equalTo(
                "O=My Organization,ST=My State,C=My Country,"
                    + "CN=My Common Name,OU=My Organization Unit,L=My Locality")
        );
      });

      it("can add alternative names", () -> {
        CertificateSecretParameters params = new CertificateSecretParameters()
            .addAlternativeNames("alternative-name-1", "alternative-name-2", ".foo.com",
                "foo.com.test", "*.foo.com.test");

        ASN1Sequence sequence = ASN1Sequence.getInstance(params.getAlternativeNames());
        assertThat(sequence.getObjectAt(0),
            equalTo(new GeneralName(GeneralName.dNSName, "alternative-name-1")));
        assertThat(sequence.getObjectAt(1),
            equalTo(new GeneralName(GeneralName.dNSName, "alternative-name-2")));
        assertThat(sequence.getObjectAt(2),
            equalTo(new GeneralName(GeneralName.dNSName, ".foo.com")));
        assertThat(sequence.getObjectAt(3),
            equalTo(new GeneralName(GeneralName.dNSName, "foo.com.test")));
        assertThat(sequence.getObjectAt(4),
            equalTo(new GeneralName(GeneralName.dNSName, "*.foo.com.test")));
      });

      it("can add alternative names, that are valid IPs", () -> {
        CertificateSecretParameters params = new CertificateSecretParameters()
            .addAlternativeNames("107.23.170.203", "52.72.132.140", "2607:f8b0:4005:808::200e");

        ASN1Sequence sequence = ASN1Sequence.getInstance(params.getAlternativeNames());
        assertThat(sequence.getObjectAt(0),
            equalTo(new GeneralName(GeneralName.iPAddress, "107.23.170.203")));
        assertThat(sequence.getObjectAt(1),
            equalTo(new GeneralName(GeneralName.iPAddress, "52.72.132.140")));
        assertThat(sequence.getObjectAt(2),
            equalTo(new GeneralName(GeneralName.iPAddress, "2607:f8b0:4005:808::200e")));
      });

      itThrows("with invalid names with many levels", ParameterizedValidationException.class,
          () -> {
            new CertificateSecretParameters()
                .addAlternativeNames(
                    ".foo.com.foo.com.foo.com.foo.com.foo.com.foo.com.foo.com.foo.com.foo.com.foo"
                        + ".com.foo.com.foo.com.foo.com.foo.com.foo.com.foo.com.foo.com.foo.com"
                        + ".foo.com.foo.com.foo.com.foo.com.foo.com.foo.com.foo.com.foo.com.foo"
                        + ".com.foo.com.foo.com.foo.com.foo.com%");
          });

      itThrows("when an invalid IP is given", ParameterizedValidationException.class, () -> {
        new CertificateSecretParameters()
            .addAlternativeNames("107.23.170.203333", "5...55");
      });

      it("can add extended key usages", () -> {
        CertificateSecretParameters params = new CertificateSecretParameters()
            .addExtendedKeyUsage("server_auth", "client_auth", "code_signing", "email_protection",
                "time_stamping");

        ExtendedKeyUsage extendedKeyUsages = ExtendedKeyUsage
            .getInstance(params.getExtendedKeyUsage());
        assertThat(extendedKeyUsages.getUsages()[0], equalTo(KeyPurposeId.id_kp_serverAuth));
        assertThat(extendedKeyUsages.getUsages()[1], equalTo(KeyPurposeId.id_kp_clientAuth));
        assertThat(extendedKeyUsages.getUsages()[2], equalTo(KeyPurposeId.id_kp_codeSigning));
        assertThat(extendedKeyUsages.getUsages()[3], equalTo(KeyPurposeId.id_kp_emailProtection));
        assertThat(extendedKeyUsages.getUsages()[4], equalTo(KeyPurposeId.id_kp_timeStamping));
      });

      it("can add key usages", () -> {
        CertificateSecretParameters params = new CertificateSecretParameters()
            .setCountry("My Country")
            .addKeyUsage(
                "digital_signature",
                "non_repudiation",
                "key_encipherment",
                "data_encipherment",
                "key_agreement",
                "key_cert_sign",
                "crl_sign",
                "encipher_only",
                "decipher_only"
            );

        KeyUsage keyUsages = KeyUsage.getInstance(params.getKeyUsage());
        assertThat(keyUsages.hasUsages(KeyUsage.digitalSignature), equalTo(true));
        assertThat(keyUsages.hasUsages(KeyUsage.nonRepudiation), equalTo(true));
        assertThat(keyUsages.hasUsages(KeyUsage.keyEncipherment), equalTo(true));
        assertThat(keyUsages.hasUsages(KeyUsage.dataEncipherment), equalTo(true));
        assertThat(keyUsages.hasUsages(KeyUsage.keyAgreement), equalTo(true));
        assertThat(keyUsages.hasUsages(KeyUsage.keyCertSign), equalTo(true));
        assertThat(keyUsages.hasUsages(KeyUsage.cRLSign), equalTo(true));
        assertThat(keyUsages.hasUsages(KeyUsage.encipherOnly), equalTo(true));
        assertThat(keyUsages.hasUsages(KeyUsage.decipherOnly), equalTo(true));

        params = new CertificateSecretParameters()
            .setCountry("My Country")
            .addKeyUsage("digital_signature", "non_repudiation", "decipher_only");

        keyUsages = KeyUsage.getInstance(params.getKeyUsage());
        assertThat(keyUsages.hasUsages(KeyUsage.digitalSignature), equalTo(true));
        assertThat(keyUsages.hasUsages(KeyUsage.nonRepudiation), equalTo(true));
        assertThat(keyUsages.hasUsages(KeyUsage.keyEncipherment), equalTo(false));
        assertThat(keyUsages.hasUsages(KeyUsage.dataEncipherment), equalTo(false));
        assertThat(keyUsages.hasUsages(KeyUsage.keyAgreement), equalTo(false));
        assertThat(keyUsages.hasUsages(KeyUsage.keyCertSign), equalTo(false));
        assertThat(keyUsages.hasUsages(KeyUsage.cRLSign), equalTo(false));
        assertThat(keyUsages.hasUsages(KeyUsage.encipherOnly), equalTo(false));
        assertThat(keyUsages.hasUsages(KeyUsage.decipherOnly), equalTo(true));
      });

      it("validates key usages", () -> {
        try {
          new CertificateSecretParameters()
              .setCountry("My Country")
              .addKeyUsage("key_agreement", "digital_sinnature");
          fail();
        } catch (ParameterizedValidationException pve) {
          assertThat(pve.getLocalizedMessage(), equalTo("error.invalid_key_usage"));
          assertThat(pve.getParameter(), equalTo("digital_sinnature"));
        }
      });

      it("sets default duration to 365 days", () -> {
        assertThat(new CertificateSecretParameters().getDurationDays(), equalTo(365));
      });

      it("sets default key length to 2048 bits", () -> {
        assertThat(new CertificateSecretParameters().getKeyLength(), equalTo(2048));
      });
    });

    describe("when given a certificate string", () -> {
      it("should set all the parameters from a PEM string", () -> {
        CertificateReader reader = mock(CertificateReader.class);
        when(reader.getSubjectName()).thenReturn(new X500Name("CN=test-common-name"));
        when(reader.getKeyLength()).thenReturn(1024);
        when(reader.isSelfSigned()).thenReturn(true);
        when(reader.getDurationDays()).thenReturn(30);
        ExtendedKeyUsage extendedKeyUsage = mock(ExtendedKeyUsage.class);
        when(reader.getExtendedKeyUsage()).thenReturn(extendedKeyUsage);
        GeneralNames alternativeNames = mock(GeneralNames.class);
        when(reader.getAlternativeNames()).thenReturn(alternativeNames);
        when(reader.getKeyUsage()).thenReturn(new KeyUsage(10));
        when(reader.isCa()).thenReturn(true);

        certificateSecretParameters = new CertificateSecretParameters(reader, "some-ca-name");
        assertThat(certificateSecretParameters.getDn().toString(), equalTo("CN=test-common-name"));
        assertThat(certificateSecretParameters.getKeyLength(), equalTo(1024));
        assertThat(certificateSecretParameters.isSelfSigned(), equalTo(true));
        assertThat(certificateSecretParameters.getDurationDays(), equalTo(30));
        assertThat(certificateSecretParameters.getExtendedKeyUsage(), equalTo(extendedKeyUsage));
        assertThat(certificateSecretParameters.getAlternativeNames(), equalTo(alternativeNames));
        assertThat(certificateSecretParameters.getKeyUsage(), equalTo(new KeyUsage(10)));
        assertThat(certificateSecretParameters.isCa(), equalTo(true));

        assertThat(certificateSecretParameters.getCaName(), equalTo("some-ca-name"));
      });
    });

    describe("#validate", () -> {
      it("validates extended key usages", () -> {
        try {
          new CertificateSecretParameters()
              .setCountry("My Country")
              .addExtendedKeyUsage("client_auth", "server_off");
          fail();
        } catch (ParameterizedValidationException pve) {
          assertThat(pve.getLocalizedMessage(), equalTo("error.invalid_extended_key_usage"));
          assertThat(pve.getParameter(), equalTo("server_off"));
        }
      });

      describe("with self_sign set to true and no ca name", () -> {
        itThrowsWithMessage("when is_ca is set to false", ParameterizedValidationException.class,
            "error.missing_signing_ca", () -> {
              new CertificateSecretParameters()
                  .setCommonName("foo")
                  .setIsCa(false)
                  .validate();
            });

        describe("when is_ca is set to true", () -> {
          it("should not throw an exception", () -> {
            new CertificateSecretParameters()
                .setCommonName("foo")
                .setIsCa(true)
                .validate();
          });
        });
      });

      describe("with self_sign set to true and a ca name", () -> {
        itThrowsWithMessage("", ParameterizedValidationException.class, "error.ca_and_self_sign",
            () -> {
              new CertificateSecretParameters()
                  .setCommonName("foo")
                  .setCaName("test-ca")
                  .setSelfSigned(true)
                  .validate();
            });
      });

      itThrowsWithMessage("when duration is less than 1", ParameterizedValidationException.class,
          "error.invalid_duration", () -> {
            new CertificateSecretParameters()
                .setCaName("test-ca")
                .setCommonName("foo")
                .setDurationDays(0)
                .validate();
          });

      itThrowsWithMessage("when duration is greater than 3650",
          ParameterizedValidationException.class, "error.invalid_duration", () -> {
            new CertificateSecretParameters()
                .setCaName("test-ca")
                .setCommonName("foo")
                .setDurationDays(3651)
                .validate();
          });

      itThrowsWithMessage("when all of DN parameters are empty",
          ParameterizedValidationException.class, "error.missing_certificate_parameters", () -> {
            new CertificateSecretParameters()
                .setCaName("test-ca")
                .setOrganization("")
                .setState("")
                .setCountry("")
                .setCommonName("")
                .setOrganizationUnit("")
                .setLocality("").validate();
          });

      describe("when key lengths are invalid", () -> {
        itThrowsWithMessage("when key length is less than 2048",
            ParameterizedValidationException.class, "error.invalid_key_length", () -> {
              new CertificateSecretParameters()
                  .setCaName("test-ca")
                  .setCommonName("foo")
                  .setKeyLength(1024)
                  .validate();
            });

        itThrowsWithMessage("when key length is between 2048 and 3072",
            ParameterizedValidationException.class, "error.invalid_key_length", () -> {
              new CertificateSecretParameters()
                  .setCaName("test-ca")
                  .setCommonName("foo")
                  .setKeyLength(2222)
                  .validate();
            });

        itThrowsWithMessage("when key length is greater than 4096",
            ParameterizedValidationException.class, "error.invalid_key_length", () -> {
              new CertificateSecretParameters()
                  .setCaName("test-ca")
                  .setCommonName("foo")
                  .setKeyLength(9192)
                  .validate();
            });
      });
    });
  }
}
