package io.pivotal.security.service;

import static com.greghaskins.spectrum.Spectrum.beforeEach;
import static com.greghaskins.spectrum.Spectrum.describe;
import static com.greghaskins.spectrum.Spectrum.it;
import static io.pivotal.security.helper.SpectrumHelper.itThrows;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.greghaskins.spectrum.Spectrum;
import io.pivotal.security.exceptions.KeyNotFoundException;
import java.security.Key;
import java.security.ProviderException;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.crypto.IllegalBlockSizeException;
import org.junit.runner.RunWith;
import org.mockito.InOrder;

@RunWith(Spectrum.class)
public class RetryingEncryptionServiceTest {

  private RetryingEncryptionService subject;

  private EncryptionKeyCanaryMapper keyMapper;
  private ReentrantReadWriteLock.ReadLock readLock;
  private ReentrantReadWriteLock.WriteLock writeLock;
  private Key firstKey;
  private Key secondKey;
  private EncryptionService encryptionService;
  private RemoteEncryptionConnectable remoteEncryptionConnectable;
  private UUID keyUuid;

  private ReentrantReadWriteLock readWriteLock;

  {
    beforeEach(() -> {
      keyMapper = mock(EncryptionKeyCanaryMapper.class);
      firstKey = mock(Key.class, "first key");
      secondKey = mock(Key.class, "second key");
      encryptionService = mock(EncryptionService.class);
      remoteEncryptionConnectable = mock(RemoteEncryptionConnectable.class);

      keyUuid = UUID.randomUUID();

      when(keyMapper.getKeyForUuid(eq(keyUuid)))
          .thenReturn(firstKey)
          .thenReturn(secondKey);

      subject = new RetryingEncryptionService(encryptionService, keyMapper,
          remoteEncryptionConnectable);

      final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
      readLock = spy(rwLock.readLock());
      writeLock = spy(rwLock.writeLock());
      readWriteLock = spy(ReentrantReadWriteLock.class);
      when(readWriteLock.readLock()).thenReturn(readLock);
      when(readWriteLock.writeLock()).thenReturn(writeLock);
      subject.readWriteLock = readWriteLock;
    });

    describe("#encrypt", () -> {
      describe("happy path", () -> {
        it("should encrypt the string without attempting to reconnect", () -> {
          reset(encryptionService);

          Encryption expectedEncryption = mock(Encryption.class);
          when(encryptionService.encrypt(keyUuid, firstKey, "fake-plaintext"))
              .thenReturn(expectedEncryption);

          Encryption encryptedValue = subject.encrypt(keyUuid, "fake-plaintext");

          assertThat(encryptedValue, equalTo(expectedEncryption));

          verify(remoteEncryptionConnectable, times(0))
              .reconnect(any(IllegalBlockSizeException.class));
          verify(keyMapper, times(0)).mapUuidsToKeys();
        });
      });

      describe("when encrypt throws errors", () -> {
        beforeEach(() -> {
          when(encryptionService.encrypt(any(UUID.class), any(Key.class), anyString()))
              .thenThrow(new ProviderException("function 'C_GenerateRandom' returns 0x30"));
        });

        it("retries encryption failures", () -> {
          try {
            subject.encrypt(keyUuid, "a value");
            fail("Expected exception");
          } catch (ProviderException e) {
            // expected
          }

          final InOrder inOrder = inOrder(encryptionService, remoteEncryptionConnectable);
          inOrder.verify(encryptionService).encrypt(eq(keyUuid), eq(firstKey), anyString());
          inOrder.verify(remoteEncryptionConnectable).reconnect(any(ProviderException.class));
          inOrder.verify(encryptionService).encrypt(eq(keyUuid), eq(secondKey), anyString());
        });

        it("unlocks after exception and locks again before encrypting", () -> {
          reset(writeLock);

          try {
            subject.encrypt(keyUuid, "a value");
          } catch (ProviderException e) {
            // expected
          }

          verify(readLock, times(2)).lock();
          verify(readLock, times(2)).unlock();

          verify(writeLock, times(1)).unlock();
          verify(writeLock, times(1)).lock();
        });

        it("creates new keys for UUIDs", () -> {
          try {
            subject.encrypt(keyUuid, "a value");
            fail("Expected exception");
          } catch (ProviderException e) {
            // expected
          }
          verify(keyMapper).mapUuidsToKeys();
        });
      });

      describe("when the operation succeeds only after reconnection", () -> {
        it("should return the encrypted string", () -> {
          Encryption expectedEncryption = mock(Encryption.class);
          reset(encryptionService);

          when(encryptionService.encrypt(keyUuid, firstKey, "fake-plaintext"))
              .thenThrow(new IllegalBlockSizeException("test exception"));
          when(encryptionService.encrypt(keyUuid, secondKey, "fake-plaintext"))
              .thenReturn(expectedEncryption);

          assertThat(subject.encrypt(keyUuid, "fake-plaintext"), equalTo(expectedEncryption));

          verify(remoteEncryptionConnectable, times(1))
              .reconnect(any(IllegalBlockSizeException.class));
          verify(keyMapper, times(1)).mapUuidsToKeys();
        });
      });

      describe("encryption locks", () -> {
        it("acquires a Luna Usage readLock", () -> {
          reset(writeLock);

          subject.encrypt(keyUuid, "a value");
          verify(readLock, times(1)).lock();
          verify(readLock, times(1)).unlock();

          verify(writeLock, times(0)).unlock();
          verify(writeLock, times(0)).lock();
        });
      });

      describe("using two threads", () -> {
        it("won't retry twice", () -> {
          final Object lock = new Object();
          final Thread firstThread = new Thread("first") {
            @Override
            public void run() {
              try {
                subject.encrypt(keyUuid, "a value 1");
              } catch (Exception e) {
              }
            }
          };
          final Thread secondThread = new Thread("second") {
            @Override
            public void run() {
              try {
                subject.encrypt(keyUuid, "a value 2");
              } catch (Exception e) {
              }
            }
          };

          subject = new RacingRetryingEncryptionServiceForTest(firstThread, secondThread, lock);

          when(encryptionService.encrypt(eq(keyUuid), any(Key.class), anyString()))
              .thenThrow(new ProviderException("function 'C_GenerateRandom' returns 0x30"));

          firstThread.start();

          firstThread.join();
          secondThread.join();

          verify(keyMapper, times(1)).mapUuidsToKeys();
        });
      });
    });

    describe("#decrypt", () -> {
      it("should return the decrypted string without attempting to reconnect", () -> {
        reset(encryptionService);

        when(encryptionService
            .decrypt(firstKey, "fake-encrypted-value".getBytes(), "fake-nonce".getBytes()))
            .thenReturn("fake-plaintext");

        assertThat(
            subject.decrypt(keyUuid, "fake-encrypted-value".getBytes(), "fake-nonce".getBytes()),
            equalTo("fake-plaintext"));

        verify(remoteEncryptionConnectable, times(0))
            .reconnect(any(IllegalBlockSizeException.class));
        verify(keyMapper, times(0)).mapUuidsToKeys();
      });

      describe("when decrypt throws errors", () -> {
        beforeEach(() -> {
          when(encryptionService.decrypt(any(Key.class), any(byte[].class), any(byte[].class)))
              .thenThrow(new ProviderException("function 'C_GenerateRandom' returns 0x30"));
        });

        it("retries decryption failures", () -> {
          try {
            subject.decrypt(keyUuid, "an encrypted value".getBytes(), "a nonce".getBytes());
            fail("Expected exception");
          } catch (ProviderException e) {
            // expected
          }

          final InOrder inOrder = inOrder(encryptionService, remoteEncryptionConnectable);
          inOrder.verify(encryptionService)
              .decrypt(eq(firstKey), any(byte[].class), any(byte[].class));
          inOrder.verify(remoteEncryptionConnectable).reconnect(any(ProviderException.class));
          inOrder.verify(encryptionService)
              .decrypt(eq(secondKey), any(byte[].class), any(byte[].class));
        });

        it("unlocks after exception and locks again before encrypting", () -> {
          reset(writeLock);

          try {
            subject.decrypt(keyUuid, "an encrypted value".getBytes(), "a nonce".getBytes());
          } catch (ProviderException e) {
            // expected
          }

          verify(readLock, times(2)).lock();
          verify(readLock, times(2)).unlock();

          verify(writeLock, times(1)).lock();
          verify(writeLock, times(1)).unlock();
        });

        // no need to test this for encryption because the behavior is the same
        it("locks and unlocks the reconnect lock when login errors", () -> {
          reset(writeLock);
          doThrow(new RuntimeException()).when(remoteEncryptionConnectable)
              .reconnect(any(Exception.class));

          try {
            subject.decrypt(keyUuid, "an encrypted value".getBytes(), "a nonce".getBytes());
          } catch (IllegalBlockSizeException | RuntimeException e) {
            // expected
          }

          verify(readLock, times(2)).lock();
          verify(readLock, times(2)).unlock();

          verify(writeLock, times(1)).lock();
          verify(writeLock, times(1)).unlock();
        });

        describe("when the operation succeeds only after reconnection", () -> {
          it("should return the decrypted string", () -> {
            reset(encryptionService);

            when(encryptionService
                .decrypt(firstKey, "fake-encrypted-value".getBytes(), "fake-nonce".getBytes()))
                .thenThrow(new IllegalBlockSizeException("test exception"));
            when(encryptionService
                .decrypt(secondKey, "fake-encrypted-value".getBytes(), "fake-nonce".getBytes()))
                .thenReturn("fake-plaintext");

            assertThat(subject
                    .decrypt(keyUuid, "fake-encrypted-value".getBytes(), "fake-nonce".getBytes()),
                equalTo("fake-plaintext"));

            verify(remoteEncryptionConnectable, times(1))
                .reconnect(any(IllegalBlockSizeException.class));
            verify(keyMapper, times(1)).mapUuidsToKeys();
          });
        });

        describe("when the encryption key for the credential cannot be found", () -> {
          itThrows("should throw an appropriate exception", KeyNotFoundException.class, () -> {
            UUID fakeUUID = UUID.randomUUID();
            reset(encryptionService);
            when(keyMapper.getKeyForUuid(fakeUUID)).thenReturn(null);
            subject.decrypt(fakeUUID, "something we cant read".getBytes(), "nonce".getBytes());
          });
        });
      });

      describe("decryption locks", () -> {
        it("acquires a Luna Usage readLock", () -> {
          when(encryptionService.decrypt(any(Key.class), any(byte[].class), any(byte[].class)))
              .thenReturn("the result");

          reset(writeLock);

          subject.decrypt(keyUuid, "an encrypted value".getBytes(), "a nonce".getBytes());
          verify(readLock, times(1)).lock();
          verify(readLock, times(1)).unlock();

          verify(writeLock, times(0)).lock();
          verify(writeLock, times(0)).unlock();
        });
      });

      describe("using two threads", () -> {
        it("won't retry twice", () -> {
          final Object lock = new Object();
          final Key key = mock(Key.class);
          final Thread firstThread = new Thread("first") {
            @Override
            public void run() {
              try {
                subject.decrypt(keyUuid, "a value 1".getBytes(), "nonce".getBytes());
              } catch (Exception e) {
              }
            }
          };
          final Thread secondThread = new Thread("second") {
            @Override
            public void run() {
              try {
                subject.decrypt(keyUuid, "a value 2".getBytes(), "nonce".getBytes());
              } catch (Exception e) {
              }
            }
          };

          subject = new RacingRetryingEncryptionServiceForTest(firstThread, secondThread, lock);

          when(encryptionService.decrypt(any(Key.class), any(byte[].class), any(byte[].class)))
              .thenThrow(new ProviderException("function 'C_GenerateRandom' returns 0x30"));
          when(keyMapper.getUuidForKey(eq(key))).thenReturn(keyUuid);

          firstThread.start();

          firstThread.join();
          secondThread.join();

          verify(keyMapper, times(1)).mapUuidsToKeys();
        });
      });
    });
  }

  private class RacingRetryingEncryptionServiceForTest extends RetryingEncryptionService {

    private final Thread firstThread;
    private final Thread secondThread;
    private final Object lock;

    RacingRetryingEncryptionServiceForTest(Thread firstThread, Thread secondThread, Object lock) {
      super(RetryingEncryptionServiceTest.this.encryptionService,
          RetryingEncryptionServiceTest.this.keyMapper,
          RetryingEncryptionServiceTest.this.remoteEncryptionConnectable);
      this.firstThread = firstThread;
      this.secondThread = secondThread;
      this.lock = lock;
    }

    @Override
    void setNeedsReconnectFlag() {
      try {
        if (Thread.currentThread().equals(firstThread)) {
          secondThread.start();
          synchronized (lock) {
            lock.wait(); // pause the first thread
          }
          Thread.sleep(10); // give thread two a chance to get all the way through the retry
        } else {
          synchronized (lock) {
            lock.notify(); // unpause the first thread
          }
        }
      } catch (Exception e) {
      }
      super
          .setNeedsReconnectFlag(); // give thread one a chance to set the needsRetry flag after thread two finishes. sets us up for reconnecting twice
    }
  }
}
