package io.pivotal.security.jna.libcrypto;

import static com.greghaskins.spectrum.Spectrum.beforeEach;
import static com.greghaskins.spectrum.Spectrum.describe;
import static com.greghaskins.spectrum.Spectrum.it;
import static io.pivotal.security.helper.SpectrumHelper.getBouncyCastleProvider;
import static io.pivotal.security.jna.libcrypto.Crypto.RSA_NO_PADDING;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import com.greghaskins.spectrum.Spectrum;
import com.sun.jna.Pointer;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import javax.crypto.Cipher;
import org.junit.runner.RunWith;

@RunWith(Spectrum.class)
public class CryptoWrapperTest {

  private CryptoWrapper subject;

  {
    beforeEach(() -> {
      subject = new CryptoWrapper(getBouncyCastleProvider());
    });

    it("can generate keypairs", () -> {
      // We expect that the openssl random number generator is seeded automatically.
      // RSA_generate_key_ex uses BN_generate_prime for primes
      // BN_generate_prime uses RAND, and RAND is transparently seeded with /dev/urandom

      // https://www.openssl.org/docs/man1.0.1/crypto/RSA_generate_key.html
      // https://www.openssl.org/docs/man1.0.1/crypto/BN_generate_prime.html
      // https://www.openssl.org/docs/man1.0.1/crypto/RAND_add.html

      subject.generateKeyPair(1024, first -> {
        KeyPair firstKeyPair = subject.toKeyPair(first);
        assertThat(firstKeyPair.getPublic(), notNullValue());

        subject.generateKeyPair(1024, second -> {
          KeyPair secondKeyPair = subject.toKeyPair(second);
          assertThat(secondKeyPair.getPublic(), notNullValue());

          assertThat(secondKeyPair.getPublic().getEncoded(),
              not(equalTo(firstKeyPair.getPublic().getEncoded())));
        });
      });
    });

    it("can transform RSA structs into KeyPairs", () -> {
      subject.generateKeyPair(1024, rsa -> {
        byte[] plaintext = new byte[128];
        byte[] message = "OpenSSL for speed".getBytes();
        System.arraycopy(message, 0, plaintext, 0, message.length);

        byte[] ciphertext = new byte[Crypto.RSA_size(rsa)];
        int result = Crypto
            .RSA_private_encrypt(plaintext.length, plaintext, ciphertext, rsa, RSA_NO_PADDING);
        if (result == -1) {
          System.out.println(subject.getError());
        }
        assert result >= 0;

        KeyPair keyPair = subject.toKeyPair(rsa);
        PrivateKey privateKey = keyPair.getPrivate();

        Cipher cipher = Cipher.getInstance(CryptoWrapper.ALGORITHM, getBouncyCastleProvider());
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);
        byte[] javaCipherText = cipher.doFinal(plaintext);

        assertThat("Encryption should work the same inside and outside openssl", javaCipherText,
            equalTo(ciphertext));
      });
    });

    describe("converting BIGNUM to BigInteger", () -> {
      it("works for small positive numbers", () -> {
        Pointer bn = Crypto.BN_new();
        try {
          Crypto.BN_set_word(bn, 18);
          BigInteger converted = subject.convert(bn);
          Pointer hex = Crypto.BN_bn2hex(bn);
          try {
            assertThat(hex.getString(0), equalTo("12"));
            assertThat(converted.toString(16).toUpperCase(), equalTo(hex.getString(0)));
          } finally {
            Crypto.CRYPTO_free(hex);
          }
        } finally {
          Crypto.BN_free(bn);
        }
      });

      it("works for small negative numbers", () -> {
        Pointer bn = Crypto.BN_new();
        try {
          Crypto.BN_set_word(bn, 16);
          Crypto.BN_set_negative(bn, 1);
          BigInteger converted = subject.convert(bn);
          Pointer hex = Crypto.BN_bn2hex(bn);
          try {
            assertThat(hex.getString(0), equalTo("-10"));
            assertThat(converted.toString(16).toUpperCase(), equalTo(hex.getString(0)));
          } finally {
            Crypto.CRYPTO_free(hex);
          }
        } finally {
          Crypto.BN_free(bn);
        }
      });

      it("works with more than 64 bits", () -> {
        Pointer bn = Crypto.BN_new();
        try {
          Crypto.BN_set_word(bn, 0x1234567800000000L);
          Crypto.BN_mul_word(bn, 0xFFFFFFFFFFFFFFFFL);
          BigInteger converted = subject.convert(bn);
          Pointer hex = Crypto.BN_bn2hex(bn);
          try {
            assertThat(hex.getString(0), equalTo("12345677FFFFFFFFEDCBA98800000000"));
            assertThat(converted.toString(16).toUpperCase(), equalTo(hex.getString(0)));
          } finally {
            Crypto.CRYPTO_free(hex);
          }
        } finally {
          Crypto.BN_free(bn);
        }
      });
    });
  }
}
