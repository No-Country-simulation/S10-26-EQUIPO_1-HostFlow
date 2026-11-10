package com.hostflow.service;

import com.hostflow.dto.request.AccommodationPaymentMethodRequest;
import com.hostflow.dto.response.AccommodationPaymentMethodResponse;
import com.hostflow.enums.PaymentMethodType;
import com.hostflow.exception.BusinessException;
import com.hostflow.model.Accommodation;
import com.hostflow.model.AccommodationPaymentMethod;
import com.hostflow.model.Owner;
import com.hostflow.repository.AccommodationPaymentMethodRepository;
import com.hostflow.repository.AccommodationRepository;
import com.hostflow.repository.OwnerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link AccommodationPaymentMethodService}.
 *
 * <p><strong>🆕 Actualización v2.3:</strong> Los tests ahora esperan DTOs
 * ({@link AccommodationPaymentMethodResponse}) en lugar de entidades JPA,
 * alineándose con la refactorización del servicio para no exponer la base de datos.</p>
 */
class AccommodationPaymentMethodServiceTest {

    // =========================================================================
    // MOCKS E INICIALIZACIÓN
    // =========================================================================
    private final AccommodationRepository accommodationRepository = mock(AccommodationRepository.class);
    private final AccommodationPaymentMethodRepository paymentMethodRepository = mock(AccommodationPaymentMethodRepository.class);
    private final OwnerRepository ownerRepository = mock(OwnerRepository.class); // ✅ Agregado
    private final EmailService emailService = mock(EmailService.class);

    // ✅ Constructor actualizado con los 4 argumentos requeridos por el servicio
    private final AccommodationPaymentMethodService service =
            new AccommodationPaymentMethodService(accommodationRepository, paymentMethodRepository, ownerRepository, emailService);

    private final Accommodation accommodation = new Accommodation();
    private final Owner owner = new Owner();

    @BeforeEach
    void setUp() {
        owner.setId(1L);
        owner.setEmail("test@owner.com");
        accommodation.setId(10L);
        accommodation.setOwner(owner);

        when(ownerRepository.findByEmail("test@owner.com")).thenReturn(Optional.of(owner));
        when(accommodationRepository.findByOwner_Id(1L)).thenReturn(Optional.of(accommodation));
        when(paymentMethodRepository.existsByAccommodation_IdAndDefaultMethodTrue(10L)).thenReturn(true);
        when(paymentMethodRepository.save(any(AccommodationPaymentMethod.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // =========================================================================
    // PRUEBAS DE CREACIÓN (CREATE)
    // =========================================================================
    @Test
    void creaMetodosGenericosSinCamposEspecificos() {
        for (PaymentMethodType type : new PaymentMethodType[]{PaymentMethodType.CASH, PaymentMethodType.CARD, PaymentMethodType.OTHER}) {
            AccommodationPaymentMethodRequest request = request(type, "Método " + type);

            // ✅ El servicio ahora retorna un DTO
            AccommodationPaymentMethodResponse result = service.create(1L, request);

            // ✅ Uso de accessors de Record en lugar de getters
            assertEquals(type, result.paymentType());
            assertEquals("Método " + type, result.displayName());
        }
    }

    @Test
    void creaTransferenciaBancariaConTodosLosCamposObligatorios() {
        AccommodationPaymentMethodResponse result = service.create(1L, bankRequest());

        assertEquals(PaymentMethodType.BANK_TRANSFER, result.paymentType());
        assertEquals("Banco Nación", result.bankName());
        assertEquals("Caja de ahorro", result.accountType());
        assertEquals("123456", result.accountNumber());
        assertEquals("0000003100000000000001", result.cbuCvu());
        assertEquals("hostflow.pagos", result.alias());
    }

    @Test
    void creaBilleteraDigitalConTodosLosCamposObligatorios() {
        AccommodationPaymentMethodResponse result = service.create(1L, walletRequest());

        assertEquals(PaymentMethodType.DIGITAL_WALLET, result.paymentType());
        assertEquals("usuario.123", result.userIdentifier());
        assertEquals("+5491112345678", result.walletPhone());
        assertEquals("usuario@example.com", result.walletEmail());
    }

    // =========================================================================
    // PRUEBAS DE VALIDACIÓN Y RECHAZO (CREATE)
    // =========================================================================
    @Test
    void rechazaTransferenciaBancariaSinUnCampoObligatorio() {
        AccommodationPaymentMethodRequest request = bankRequest();
        request.setAlias(" "); // Campo inválido

        BusinessException error = assertThrows(BusinessException.class, () -> service.create(1L, request));
        assertTrue(error.getMessage().contains("alias"));
        verify(paymentMethodRepository, never()).save(any());
    }

    @Test
    void rechazaBilleteraDigitalConCorreoInvalido() {
        AccommodationPaymentMethodRequest request = walletRequest();
        request.setWalletEmail("correo-no-valido");

        BusinessException error = assertThrows(BusinessException.class, () -> service.create(1L, request));
        assertTrue(error.getMessage().contains("walletEmail"));
        verify(paymentMethodRepository, never()).save(any());
    }

    // =========================================================================
    // PRUEBAS DE ACTUALIZACIÓN PARCIAL (PATCH)
    // =========================================================================
    @Test
    void patchParcialActualizaSoloElCampoEnviadoYConservaLosDemas() {
        AccommodationPaymentMethod existing = bankMethod();
        when(paymentMethodRepository.findByIdAndAccommodation_Id(50L, 10L)).thenReturn(Optional.of(existing));

        AccommodationPaymentMethodRequest patch = new AccommodationPaymentMethodRequest();
        patch.setAlias("nuevo.alias");

        // ✅ El servicio retorna DTO
        AccommodationPaymentMethodResponse result = service.update(1L, 50L, patch);

        assertEquals("nuevo.alias", result.alias());
        assertEquals("Banco Nación", result.bankName());
        assertEquals("Titular de prueba", result.accountHolder());
        assertEquals("Caja de ahorro", result.accountType());
        assertEquals("123456", result.accountNumber());
        assertEquals("0000003100000000000001", result.cbuCvu());
        verify(paymentMethodRepository).save(existing);
    }

    @Test
    void patchRechazaCuerpoVacio() {
        AccommodationPaymentMethodRequest patch = new AccommodationPaymentMethodRequest();
        assertThrows(BusinessException.class, () -> service.update(1L, 50L, patch));
        verify(paymentMethodRepository, never()).findByIdAndAccommodation_Id(anyLong(), anyLong());
    }

    @Test
    void patchPermiteCompletarCamposFaltantesDeUnaTransferenciaAntesDeValidarEstadoFinal() {
        AccommodationPaymentMethod existing = new AccommodationPaymentMethod();
        existing.setId(50L);
        existing.setAccommodation(accommodation);
        existing.setPaymentType(PaymentMethodType.BANK_TRANSFER);
        existing.setDisplayName("Transferencia");
        existing.setBankName("Banco Nación");
        existing.setAccountHolder("Titular de prueba");
        existing.setAccountType("Caja de ahorro");
        existing.setAccountNumber("123456");
        existing.setCbuCvu("0000003100000000000001");
        existing.setAlias(null);

        when(paymentMethodRepository.findByIdAndAccommodation_Id(50L, 10L)).thenReturn(Optional.of(existing));

        AccommodationPaymentMethodRequest patch = new AccommodationPaymentMethodRequest();
        patch.setAlias("hostflow.pagos");

        AccommodationPaymentMethodResponse result = service.update(1L, 50L, patch);
        assertEquals("hostflow.pagos", result.alias());
    }

    @Test
    void patchRechazaCambioDeTipoSiLosDatosFinalesNoCumplenLasReglas() {
        AccommodationPaymentMethod existing = bankMethod();
        when(paymentMethodRepository.findByIdAndAccommodation_Id(50L, 10L)).thenReturn(Optional.of(existing));

        AccommodationPaymentMethodRequest patch = new AccommodationPaymentMethodRequest();
        patch.setPaymentType(PaymentMethodType.DIGITAL_WALLET);

        BusinessException error = assertThrows(BusinessException.class, () -> service.update(1L, 50L, patch));
        assertTrue(error.getMessage().contains("userIdentifier"));
        verify(paymentMethodRepository, never()).save(any());
    }

    @Test
    void patchActualizaDatosDeBilleteraYValidaElEstadoFinal() {
        AccommodationPaymentMethod existing = walletMethod();
        when(paymentMethodRepository.findByIdAndAccommodation_Id(50L, 10L)).thenReturn(Optional.of(existing));

        AccommodationPaymentMethodRequest patch = new AccommodationPaymentMethodRequest();
        patch.setWalletPhone("+5491198765432");
        patch.setWalletEmail("nuevo@example.com");

        AccommodationPaymentMethodResponse result = service.update(1L, 50L, patch);
        assertEquals("+5491198765432", result.walletPhone());
        assertEquals("nuevo@example.com", result.walletEmail());
        assertEquals("usuario.123", result.userIdentifier());
    }

    @Test
    void patchRechazaEliminarUnCampoObligatorioDeBilleteraConCadenaVacia() {
        AccommodationPaymentMethod existing = walletMethod();
        when(paymentMethodRepository.findByIdAndAccommodation_Id(50L, 10L)).thenReturn(Optional.of(existing));

        AccommodationPaymentMethodRequest patch = new AccommodationPaymentMethodRequest();
        patch.setUserIdentifier(" ");

        BusinessException error = assertThrows(BusinessException.class, () -> service.update(1L, 50L, patch));
        assertTrue(error.getMessage().contains("userIdentifier"));
        verify(paymentMethodRepository, never()).save(any());
    }

    // =========================================================================
    // MÉTODOS AUXILIARES (DATA BUILDERS)
    // =========================================================================
    private AccommodationPaymentMethodRequest request(PaymentMethodType type, String name) {
        AccommodationPaymentMethodRequest request = new AccommodationPaymentMethodRequest();
        request.setPaymentType(type);
        request.setDisplayName(name);
        return request;
    }

    private AccommodationPaymentMethodRequest bankRequest() {
        AccommodationPaymentMethodRequest request = request(PaymentMethodType.BANK_TRANSFER, "Transferencia");
        request.setBankName("Banco Nación");
        request.setAccountHolder("Titular de prueba");
        request.setAccountType("Caja de ahorro");
        request.setAccountNumber("123456");
        request.setCbuCvu("0000003100000000000001");
        request.setAlias("hostflow.pagos");
        return request;
    }

    private AccommodationPaymentMethodRequest walletRequest() {
        AccommodationPaymentMethodRequest request = request(PaymentMethodType.DIGITAL_WALLET, "Billetera");
        request.setAccountHolder("Titular de prueba");
        request.setUserIdentifier("usuario.123");
        request.setWalletPhone("+5491112345678");
        request.setWalletEmail("usuario@example.com");
        return request;
    }

    private AccommodationPaymentMethod bankMethod() {
        AccommodationPaymentMethod method = new AccommodationPaymentMethod();
        method.setId(50L);
        method.setAccommodation(accommodation);
        method.setPaymentType(PaymentMethodType.BANK_TRANSFER);
        method.setDisplayName("Transferencia");
        method.setBankName("Banco Nación");
        method.setAccountHolder("Titular de prueba");
        method.setAccountType("Caja de ahorro");
        method.setAccountNumber("123456");
        method.setCbuCvu("0000003100000000000001");
        method.setAlias("hostflow.pagos");
        return method;
    }

    private AccommodationPaymentMethod walletMethod() {
        AccommodationPaymentMethod method = new AccommodationPaymentMethod();
        method.setId(50L);
        method.setAccommodation(accommodation);
        method.setPaymentType(PaymentMethodType.DIGITAL_WALLET);
        method.setDisplayName("Billetera");
        method.setAccountHolder("Titular de prueba");
        method.setUserIdentifier("usuario.123");
        method.setWalletPhone("+5491112345678");
        method.setWalletEmail("usuario@example.com");
        return method;
    }
}