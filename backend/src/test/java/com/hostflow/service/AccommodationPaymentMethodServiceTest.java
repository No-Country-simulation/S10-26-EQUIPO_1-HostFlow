package com.hostflow.service;

import com.hostflow.dto.request.AccommodationPaymentMethodRequest;
import com.hostflow.enums.PaymentMethodType;
import com.hostflow.exception.BusinessException;
import com.hostflow.model.Accommodation;
import com.hostflow.model.AccommodationPaymentMethod;
import com.hostflow.repository.AccommodationPaymentMethodRepository;
import com.hostflow.repository.AccommodationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccommodationPaymentMethodServiceTest {

    private final AccommodationRepository accommodationRepository = mock(AccommodationRepository.class);
    private final AccommodationPaymentMethodRepository paymentMethodRepository =
            mock(AccommodationPaymentMethodRepository.class);
    private final AccommodationPaymentMethodService service =
            new AccommodationPaymentMethodService(accommodationRepository, paymentMethodRepository);

    private final Accommodation accommodation = new Accommodation();

    @BeforeEach
    void setUp() {
        accommodation.setId(10L);
        when(accommodationRepository.findByOwner_Id(1L)).thenReturn(Optional.of(accommodation));
        when(paymentMethodRepository.existsByAccommodation_IdAndDefaultMethodTrue(10L)).thenReturn(true);
        when(paymentMethodRepository.save(any(AccommodationPaymentMethod.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void creaMetodosGenericosSinCamposEspecificos() {
        for (PaymentMethodType type : new PaymentMethodType[]{
                PaymentMethodType.CASH, PaymentMethodType.CARD, PaymentMethodType.OTHER}) {
            AccommodationPaymentMethodRequest request = request(type, "Método " + type);

            AccommodationPaymentMethod result = service.create(1L, request);

            assertEquals(type, result.getPaymentType());
            assertEquals("Método " + type, result.getDisplayName());
        }
    }

    @Test
    void creaTransferenciaBancariaConTodosLosCamposObligatorios() {
        AccommodationPaymentMethod result = service.create(1L, bankRequest());

        assertEquals(PaymentMethodType.BANK_TRANSFER, result.getPaymentType());
        assertEquals("Banco Nación", result.getBankName());
        assertEquals("Caja de ahorro", result.getAccountType());
        assertEquals("123456", result.getAccountNumber());
        assertEquals("0000003100000000000001", result.getCbuCvu());
        assertEquals("hostflow.pagos", result.getAlias());
    }

    @Test
    void rechazaTransferenciaBancariaSinUnCampoObligatorio() {
        AccommodationPaymentMethodRequest request = bankRequest();
        request.setAlias(" ");

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.create(1L, request));

        assertTrue(error.getMessage().contains("alias"));
        verify(paymentMethodRepository, never()).save(any());
    }

    @Test
    void creaBilleteraDigitalConTodosLosCamposObligatorios() {
        AccommodationPaymentMethod result = service.create(1L, walletRequest());

        assertEquals(PaymentMethodType.DIGITAL_WALLET, result.getPaymentType());
        assertEquals("usuario.123", result.getUserIdentifier());
        assertEquals("+5491112345678", result.getWalletPhone());
        assertEquals("usuario@example.com", result.getWalletEmail());
    }

    @Test
    void rechazaBilleteraDigitalConCorreoInvalido() {
        AccommodationPaymentMethodRequest request = walletRequest();
        request.setWalletEmail("correo-no-valido");

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.create(1L, request));

        assertTrue(error.getMessage().contains("walletEmail"));
        verify(paymentMethodRepository, never()).save(any());
    }

    @Test
    void patchParcialActualizaSoloElCampoEnviadoYConservaLosDemas() {
        AccommodationPaymentMethod existing = bankMethod();
        when(paymentMethodRepository.findByIdAndAccommodation_Id(50L, 10L))
                .thenReturn(Optional.of(existing));

        AccommodationPaymentMethodRequest patch = new AccommodationPaymentMethodRequest();
        patch.setAlias("nuevo.alias");

        AccommodationPaymentMethod result = service.update(1L, 50L, patch);

        assertEquals("nuevo.alias", result.getAlias());
        assertEquals("Banco Nación", result.getBankName());
        assertEquals("Titular de prueba", result.getAccountHolder());
        assertEquals("Caja de ahorro", result.getAccountType());
        assertEquals("123456", result.getAccountNumber());
        assertEquals("0000003100000000000001", result.getCbuCvu());
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
        // Simula un registro antiguo que todavía no tiene alias.
        existing.setAlias(null);
        when(paymentMethodRepository.findByIdAndAccommodation_Id(50L, 10L))
                .thenReturn(Optional.of(existing));

        AccommodationPaymentMethodRequest patch = new AccommodationPaymentMethodRequest();
        patch.setAlias("hostflow.pagos");

        AccommodationPaymentMethod result = service.update(1L, 50L, patch);

        assertEquals("hostflow.pagos", result.getAlias());
    }

    @Test
    void patchRechazaCambioDeTipoSiLosDatosFinalesNoCumplenLasReglas() {
        AccommodationPaymentMethod existing = bankMethod();
        when(paymentMethodRepository.findByIdAndAccommodation_Id(50L, 10L))
                .thenReturn(Optional.of(existing));

        AccommodationPaymentMethodRequest patch = new AccommodationPaymentMethodRequest();
        patch.setPaymentType(PaymentMethodType.DIGITAL_WALLET);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.update(1L, 50L, patch));

        assertTrue(error.getMessage().contains("userIdentifier"));
        verify(paymentMethodRepository, never()).save(any());
    }

    @Test
    void patchActualizaDatosDeBilleteraYValidaElEstadoFinal() {
        AccommodationPaymentMethod existing = walletMethod();
        when(paymentMethodRepository.findByIdAndAccommodation_Id(50L, 10L))
                .thenReturn(Optional.of(existing));

        AccommodationPaymentMethodRequest patch = new AccommodationPaymentMethodRequest();
        patch.setWalletPhone("+5491198765432");
        patch.setWalletEmail("nuevo@example.com");

        AccommodationPaymentMethod result = service.update(1L, 50L, patch);

        assertEquals("+5491198765432", result.getWalletPhone());
        assertEquals("nuevo@example.com", result.getWalletEmail());
        assertEquals("usuario.123", result.getUserIdentifier());
    }

    @Test
    void patchRechazaEliminarUnCampoObligatorioDeBilleteraConCadenaVacia() {
        AccommodationPaymentMethod existing = walletMethod();
        when(paymentMethodRepository.findByIdAndAccommodation_Id(50L, 10L))
                .thenReturn(Optional.of(existing));

        AccommodationPaymentMethodRequest patch = new AccommodationPaymentMethodRequest();
        patch.setUserIdentifier(" ");

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.update(1L, 50L, patch));

        assertTrue(error.getMessage().contains("userIdentifier"));
        verify(paymentMethodRepository, never()).save(any());
    }

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
