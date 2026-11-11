package com.hostflow.integration;

import com.hostflow.model.Guest;
import com.hostflow.repository.GuestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class GuestProfileConcurrencyIntegrationTest {

    @Autowired GuestRepository guests;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void pessimisticLockMakesConcurrentProfileUpdateWaitForCurrentTransaction() throws Exception {
        String documentNumber = "LOCK-" + UUID.randomUUID().toString().substring(0, 20);
        Guest guest = guests.saveAndFlush(Guest.builder()
                .firstName("Ana").lastName("Pérez").fullName("Ana Pérez")
                .email("ana@example.test").phone("+541100000000")
                .documentNumber(documentNumber).active(true).build());

        CountDownLatch firstTransactionHasLock = new CountDownLatch(1);
        CountDownLatch releaseFirstTransaction = new CountDownLatch(1);
        CountDownLatch secondTransactionStarted = new CountDownLatch(1);
        CountDownLatch secondTransactionHasLock = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        try {
            Future<?> first = executor.submit(() -> transaction.executeWithoutResult(status -> {
                assertThat(guests.findByDocumentNumberForUpdate(documentNumber)).isPresent();
                firstTransactionHasLock.countDown();
                await(releaseFirstTransaction);
            }));

            assertThat(firstTransactionHasLock.await(5, TimeUnit.SECONDS)).isTrue();

            Future<?> second = executor.submit(() -> transaction.executeWithoutResult(status -> {
                secondTransactionStarted.countDown();
                assertThat(guests.findByDocumentNumberForUpdate(documentNumber)).isPresent();
                secondTransactionHasLock.countDown();
            }));
            assertThat(secondTransactionStarted.await(5, TimeUnit.SECONDS)).isTrue();

            // Mientras la primera transacción conserva el lock, la segunda no debe adquirirlo.
            assertThat(secondTransactionHasLock.await(300, TimeUnit.MILLISECONDS)).isFalse();
            releaseFirstTransaction.countDown();

            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
            assertThat(secondTransactionHasLock.getCount()).isZero();
        } finally {
            releaseFirstTransaction.countDown();
            executor.shutdownNow();
            guests.deleteById(guest.getId());
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("La prueba agotó el tiempo esperando liberar el bloqueo.");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError("La prueba fue interrumpida.", ex);
        }
    }
}
