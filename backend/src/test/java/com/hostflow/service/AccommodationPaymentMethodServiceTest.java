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

/** Pruebas unitarias de la lógica de métodos de pago del alojamiento. */
class AccommodationPaymentMethodServiceTest {

    // =========================================================================
    // MOCKS E INICIALIZACIÓN
    // =========================================================================
    private final AccommodationRepository accommodationRepository = mock(AccommodationRepository.class);
    private final AccommodationPaymentMethodRepository paymentMethodRepository = mock(AccommodationPaymentMethodRepository.class);
    private final OwnerRepository ownerRepository = mock(OwnerRepository.class);
    private final EmailService emailService = mock(EmailService.class);

    private final AccommodationPaymentMethodService service = new AccommodationPaymentMethodService(
            accommodationRepository, paymentMethodRepository, ownerRepository, emailService);

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

    // CREATE

    @Test
    void creaMetodosGenericosSinCamposEspecificos() {
        for (PaymentMethodType type : new PaymentMethodType[]{
                PaymentMethodType.CASH, PaymentMethodType.CARD, PaymentMethodType.OTHER}) {
            AccommodationPaymentMethodResponse result = service.create(1L, request(type, "Método " + type));

            assertEquals(10L, result.accommodationId());
            assertEquals(type, result.paymentType());
            assertEquals("Método " + type, result.displayName());
            assertTrue(result.isActive());
        }
    }

    @Test
    void creaTransferenciaBancariaConTodosLosCamposObligatorios() {
        AccommodationPaymentMethodResponse result = service.create(1L, bankRequest());

        assertEquals(PaymentMethodType.BANK_TRANSFER, result.paymentType());
        assertEquals("Banco Nación", result.bankName());
        assertEquals("Titular de prueba", result.accountHolder());
        assertEquals("Caja de ahorro", result.accountType());
        assertEquals("123456", result.accountNumber());
        assertEquals("0000003100000000000001", result.cbuCvu());
        assertEquals("hostflow.pagos", result.alias());
    }

    @Test
    void creaBilleteraDigitalConTodosLosCamposObligatorios() {
        AccommodationPaymentMethodResponse result = service.create(1L, walletRequest());

        assertEquals(PaymentMethodType.DIGITAL_WALLET, result.paymentType());
        assertEquals("Titular de prueba", result.accountHolder());
        assertEquals("usuario.123", result.userIdentifier());
        assertEquals("+5491112345678", result.walletPhone());
        assertEquals("usuario@example.com", result.walletEmail());
    }

    @Test
    void creaMetodoConCamposComunesYEstadosSolicitados() {
        AccommodationPaymentMethodRequest request = request(PaymentMethodType.CASH, "Efectivo");
        request.setProviderName("  Proveedor  ");
        request.setTaxId("  20-12345678-9  ");
        request.setInstructions("  Pagar al llegar  ");
        request.setIsDefault(true);
        request.setIsActive(false);

        AccommodationPaymentMethodResponse result = service.create(1L, request);

        assertEquals("Proveedor", result.providerName());
        assertEquals("20-12345678-9", result.taxId());
        assertEquals("Pagar al llegar", result.instructions());
        assertTrue(result.isDefault());
        assertFalse(result.isActive());
        verify(paymentMethodRepository).clearDefaultForAccommodation(10L);
    }

    @Test
    void primerMetodoSeEstableceComoPredeterminadoCuandoNoExisteOtro() {
        when(paymentMethodRepository.existsByAccommodation_IdAndDefaultMethodTrue(10L)).thenReturn(false);

        AccommodationPaymentMethodResponse result = service.create(1L, request(PaymentMethodType.CASH, "Efectivo"));

        assertTrue(result.isDefault());
        verify(paymentMethodRepository).clearDefaultForAccommodation(10L);
    }

    @Test
    void rechazaCreacionSinTipoOPantallaVisible() {
        AccommodationPaymentMethodRequest missingType = new AccommodationPaymentMethodRequest();
        missingType.setDisplayName("Efectivo");
        assertThrows(BusinessException.class, () -> service.create(1L, missingType));

        AccommodationPaymentMethodRequest missingName = new AccommodationPaymentMethodRequest();
        missingName.setPaymentType(PaymentMethodType.CASH);
        assertThrows(BusinessException.class, () -> service.create(1L, missingName));

        verify(paymentMethodRepository, never()).save(any());
    }

    @Test
    void rechazaTransferenciaBancariaSinUnCampoObligatorio() {
        AccommodationPaymentMethodRequest request = bankRequest();
        request.setAlias(" ");

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

    // PATCH

    @Test
    void patchParcialActualizaSoloElCampoEnviadoYConservaLosDemas() {
        AccommodationPaymentMethod existing = bankMethod();
        when(paymentMethodRepository.findByIdAndAccommodation_Id(50L, 10L)).thenReturn(Optional.of(existing));
        AccommodationPaymentMethodRequest patch = new AccommodationPaymentMethodRequest();
        patch.setAlias("nuevo.alias");

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
        assertThrows(BusinessException.class,
                () -> service.update(1L, 50L, new AccommodationPaymentMethodRequest()));
        verify(paymentMethodRepository, never()).findByIdAndAccommodation_Id(anyLong(), anyLong());
    }

    @Test
    void patchPermiteCompletarCamposFaltantesAntesDeValidarEstadoFinal() {
        AccommodationPaymentMethod existing = bankMethod();
        existing.setAlias(null);
        when(paymentMethodRepository.findByIdAndAccommodation_Id(50L, 10L)).thenReturn(Optional.of(existing));
        AccommodationPaymentMethodRequest patch = new AccommodationPaymentMethodRequest();
        patch.setAlias("hostflow.pagos");

        AccommodationPaymentMethodResponse result = service.update(1L, 50L, patch);
        assertEquals("hostflow.pagos", result.alias());
    }

    @Test
    void patchRechazaCambioDeTipoSiLosDatosFinalesNoCumplenLasReglas() {
        when(paymentMethodRepository.findByIdAndAccommodation_Id(50L, 10L))
                .thenReturn(Optional.of(bankMethod()));
        AccommodationPaymentMethodRequest patch = new AccommodationPaymentMethodRequest();
        patch.setPaymentType(PaymentMethodType.DIGITAL_WALLET);

        BusinessException error = assertThrows(BusinessException.class, () -> service.update(1L, 50L, patch));
        assertTrue(error.getMessage().contains("userIdentifier"));
        verify(paymentMethodRepository, never()).save(any());
    }

    @Test
    void patchActualizaDatosDeBilleteraYValidaElEstadoFinal() {
        when(paymentMethodRepository.findByIdAndAccommodation_Id(50L, 10L))
                .thenReturn(Optional.of(walletMethod()));
        AccommodationPaymentMethodRequest patch = new AccommodationPaymentMethodRequest();
        patch.setWalletPhone("+5491198765432");
        patch.setWalletEmail("nuevo@example.com");

        AccommodationPaymentMethodResponse result = service.update(1L, 50L, patch);
        assertEquals("+5491198765432", result.walletPhone());
        assertEquals("nuevo@example.com", result.walletEmail());
        assertEquals("usuario.123", result.userIdentifier());
    }

    @Test
    void patchRechazaEliminarUnCampoObligatorioConCadenaVacia() {
        when(paymentMethodRepository.findByIdAndAccommodation_Id(50L, 10L))
                .thenReturn(Optional.of(walletMethod()));
        AccommodationPaymentMethodRequest patch = new AccommodationPaymentMethodRequest();
        patch.setUserIdentifier(" ");

        BusinessException error = assertThrows(BusinessException.class, () -> service.update(1L, 50L, patch));
        assertTrue(error.getMessage().contains("userIdentifier"));
        verify(paymentMethodRepository, never()).save(any());
    }

    @Test
    void patchActualizaCamposComunesYEstadoActivo() {
        AccommodationPaymentMethod existing = bankMethod();
        when(paymentMethodRepository.findByIdAndAccommodation_Id(50L, 10L)).thenReturn(Optional.of(existing));
        AccommodationPaymentMethodRequest patch = new AccommodationPaymentMethodRequest();
        patch.setProviderName("Nuevo proveedor");
        patch.setTaxId("30-12345678-0");
        patch.setInstructions("Nuevas instrucciones");
        patch.setIsActive(false);
        patch.setIsDefault(true);

        AccommodationPaymentMethodResponse result = service.update(1L, 50L, patch);

        assertEquals("Nuevo proveedor", result.providerName());
        assertEquals("30-12345678-0", result.taxId());
        assertEquals("Nuevas instrucciones", result.instructions());
        assertFalse(result.isActive());
        assertTrue(result.isDefault());
    }

    // DATA BUILDERS

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