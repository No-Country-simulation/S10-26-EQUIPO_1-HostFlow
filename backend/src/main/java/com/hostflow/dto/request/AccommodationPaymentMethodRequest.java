package com.hostflow.dto.request;

import com.hostflow.enums.PaymentMethodType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * DTO para las operaciones de creación y actualización (PATCH) de métodos de pago.
 *
 * <p>Este objeto representa los datos que el propietario puede enviar al registrar
 * o modificar un método de pago para su alojamiento. Soporta múltiples tipos de pago:
 * transferencias bancarias, billeteras digitales, efectivo, tarjetas, etc.</p>
 *
 * <p><strong>Comportamiento PATCH:</strong> Los campos omitidos o null no se modifican
 * en la entidad existente. Solo se actualizan los campos explícitamente enviados.</p>
 *
 * <p><strong>Validaciones:</strong></p>
 * <ul>
 *   <li>{@code paymentType}: obligatorio para crear, opcional para actualizar.</li>
 *   <li>{@code displayName}: obligatorio y máximo 120 caracteres.</li>
 *   <li>Campos específicos según el tipo de pago (validados en el servicio).</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 2.2.0
 */
@Schema(description = "Datos de un método de pago. Para PATCH, los campos omitidos o null no se modifican.")
public class AccommodationPaymentMethodRequest {

 // =========================================================================
 // CAMPOS PRINCIPALES
 // =========================================================================

 /**
  * Tipo de método de pago (BANK_TRANSFER, DIGITAL_WALLET, CASH, CARD, etc.).
  * Obligatorio al crear un nuevo método de pago.
  */
 @Schema(description = "Tipo de método de pago (BANK_TRANSFER, DIGITAL_WALLET, CASH, CARD)", requiredMode = Schema.RequiredMode.REQUIRED)
 private PaymentMethodType paymentType;

 /**
  * Nombre visible del método de pago para el huésped (ej: "Mi cuenta bancaria", "Mercado Pago").
  * Máximo 120 caracteres.
  */
 @Schema(description = "Nombre visible del método de pago para el huésped", example = "Mi cuenta bancaria")
 @Size(max = 120, message = "El nombre del método de pago no puede exceder 120 caracteres")
 private String displayName;

 // =========================================================================
 // CAMPOS PARA TRANSFERENCIA BANCARIA (BANK_TRANSFER)
 // =========================================================================

 /**
  * Nombre del proveedor o entidad financiera (ej: "Banco Nacional", "BBVA").
  * Máximo 150 caracteres.
  */
 @Schema(description = "Nombre del proveedor o entidad financiera", example = "Banco Nacional")
 @Size(max = 150, message = "El nombre del proveedor no puede exceder 150 caracteres")
 private String providerName;

 /**
  * Nombre del banco donde se encuentra la cuenta.
  * Máximo 150 caracteres.
  */
 @Schema(description = "Nombre del banco", example = "Banco de la Nación")
 @Size(max = 150, message = "El nombre del banco no puede exceder 150 caracteres")
 private String bankName;

 /**
  * Nombre del titular de la cuenta bancaria.
  * Máximo 200 caracteres.
  */
 @Schema(description = "Nombre del titular de la cuenta", example = "Juan Pérez")
 @Size(max = 200, message = "El nombre del titular no puede exceder 200 caracteres")
 private String accountHolder;

 /**
  * Tipo de cuenta bancaria (AHORROS, CORRIENTE, etc.).
  * Máximo 80 caracteres.
  */
 @Schema(description = "Tipo de cuenta bancaria (AHORROS, CORRIENTE)", example = "AHORROS")
 @Size(max = 80, message = "El tipo de cuenta no puede exceder 80 caracteres")
 private String accountType;

 /**
  * Número de cuenta bancaria.
  * Máximo 100 caracteres.
  */
 @Schema(description = "Número de cuenta bancaria", example = "1234567890")
 @Size(max = 100, message = "El número de cuenta no puede exceder 100 caracteres")
 private String accountNumber;

 /**
  * CBU o CVU de la cuenta bancaria (Argentina).
  * Máximo 100 caracteres.
  */
 @Schema(description = "CBU o CVU de la cuenta bancaria", example = "0110123450012345678901")
 @Size(max = 100, message = "El CBU/CVU no puede exceder 100 caracteres")
 private String cbuCvu;

 /**
  * Alias de la cuenta bancaria (Argentina).
  * Máximo 100 caracteres.
  */
 @Schema(description = "Alias de la cuenta bancaria", example = "juan.perez.mp")
 @Size(max = 100, message = "El alias no puede exceder 100 caracteres")
 private String alias;

 // =========================================================================
 // CAMPOS PARA BILLETERA DIGITAL (DIGITAL_WALLET)
 // =========================================================================

 /**
  * Identificador único del usuario en la plataforma de pago (ej: ID de PayPal, Mercado Pago).
  * Máximo 200 caracteres.
  */
 @Schema(description = "Identificador único del usuario en la plataforma de pago", example = "usuario123")
 @Size(max = 200, message = "El identificador de usuario no puede exceder 200 caracteres")
 private String userIdentifier;

 /**
  * Número de teléfono asociado a la billetera digital.
  * Máximo 50 caracteres.
  */
 @Schema(description = "Número de teléfono de la billetera digital", example = "+5491112345678")
 @Size(max = 50, message = "El número de teléfono no puede exceder 50 caracteres")
 private String walletPhone;

 /**
  * Correo electrónico asociado a la billetera digital.
  * Máximo 254 caracteres (estándar RFC 5321).
  */
 @Schema(description = "Correo electrónico de la billetera digital", example = "usuario@ejemplo.com")
 @Size(max = 254, message = "El correo electrónico no puede exceder 254 caracteres")
 private String walletEmail;

 // =========================================================================
 // CAMPOS COMUNES
 // =========================================================================

 /**
  * Identificación fiscal o número de impuestos del titular (ej: CUIT, RUT, RFC).
  * Máximo 30 caracteres.
  */
 @Schema(description = "Identificación fiscal del titular (CUIT, RUT, RFC)", example = "20-12345678-9")
 @Size(max = 30, message = "La identificación fiscal no puede exceder 30 caracteres")
 private String taxId;

 /**
  * Instrucciones adicionales para el huésped sobre cómo realizar el pago.
  * Máximo 5000 caracteres.
  */
 @Schema(description = "Instrucciones adicionales para el huésped", example = "Transferir solo en días hábiles")
 @Size(max = 5000, message = "Las instrucciones no pueden exceder 5000 caracteres")
 private String instructions;

 /**
  * Indica si este método de pago es el predeterminado para el alojamiento.
  * Si es true, se limpiará el flag de los demás métodos.
  */
 @Schema(description = "Indica si es el método de pago predeterminado", example = "true")
 private Boolean isDefault;

 /**
  * Indica si el método de pago está activo y disponible para los huéspedes.
  * Por defecto es true si no se especifica.
  */
 @Schema(description = "Indica si el método de pago está activo", example = "true")
 private Boolean isActive;

 // =========================================================================
 // GETTERS Y SETTERS
 // =========================================================================

 public PaymentMethodType getPaymentType() {
  return paymentType;
 }

 public void setPaymentType(PaymentMethodType paymentType) {
  this.paymentType = paymentType;
 }

 public String getDisplayName() {
  return displayName;
 }

 public void setDisplayName(String displayName) {
  this.displayName = displayName;
 }

 public String getProviderName() {
  return providerName;
 }

 public void setProviderName(String providerName) {
  this.providerName = providerName;
 }

 public String getBankName() {
  return bankName;
 }

 public void setBankName(String bankName) {
  this.bankName = bankName;
 }

 public String getAccountHolder() {
  return accountHolder;
 }

 public void setAccountHolder(String accountHolder) {
  this.accountHolder = accountHolder;
 }

 public String getAccountType() {
  return accountType;
 }

 public void setAccountType(String accountType) {
  this.accountType = accountType;
 }

 public String getAccountNumber() {
  return accountNumber;
 }

 public void setAccountNumber(String accountNumber) {
  this.accountNumber = accountNumber;
 }

 public String getCbuCvu() {
  return cbuCvu;
 }

 public void setCbuCvu(String cbuCvu) {
  this.cbuCvu = cbuCvu;
 }

 public String getAlias() {
  return alias;
 }

 public void setAlias(String alias) {
  this.alias = alias;
 }

 public String getUserIdentifier() {
  return userIdentifier;
 }

 public void setUserIdentifier(String userIdentifier) {
  this.userIdentifier = userIdentifier;
 }

 public String getWalletPhone() {
  return walletPhone;
 }

 public void setWalletPhone(String walletPhone) {
  this.walletPhone = walletPhone;
 }

 public String getWalletEmail() {
  return walletEmail;
 }

 public void setWalletEmail(String walletEmail) {
  this.walletEmail = walletEmail;
 }

 public String getTaxId() {
  return taxId;
 }

 public void setTaxId(String taxId) {
  this.taxId = taxId;
 }

 public String getInstructions() {
  return instructions;
 }

 public void setInstructions(String instructions) {
  this.instructions = instructions;
 }

 public Boolean getIsDefault() {
  return isDefault;
 }

 public void setIsDefault(Boolean isDefault) {
  this.isDefault = isDefault;
 }

 public Boolean getIsActive() {
  return isActive;
 }

 public void setIsActive(Boolean isActive) {
  this.isActive = isActive;
 }
}