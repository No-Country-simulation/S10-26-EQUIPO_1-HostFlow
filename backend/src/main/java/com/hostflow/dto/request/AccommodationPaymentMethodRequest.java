package com.hostflow.dto.request;

import com.hostflow.enums.PaymentMethodType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos de un método de pago. Para PATCH, los campos omitidos o null no se modifican.")
public class AccommodationPaymentMethodRequest {
    private PaymentMethodType paymentType;
    @Size(max = 120) private String displayName;
    @Size(max = 150) private String providerName;
    @Size(max = 150) private String bankName;
    @Size(max = 200) private String accountHolder;
    @Size(max = 80) private String accountType;
    @Size(max = 100) private String accountNumber;
    @Size(max = 100) private String cbuCvu;
    @Size(max = 100) private String alias;
    @Size(max = 30) private String taxId;
    @Size(max = 200) private String userIdentifier;
    @Size(max = 50) private String walletPhone;
    @Size(max = 254) private String walletEmail;
    @Size(max = 5000) private String instructions;
    private Boolean isDefault;
    private Boolean isActive;

    public PaymentMethodType getPaymentType() { return paymentType; }
    public void setPaymentType(PaymentMethodType paymentType) { this.paymentType = paymentType; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getAccountHolder() { return accountHolder; }
    public void setAccountHolder(String accountHolder) { this.accountHolder = accountHolder; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getCbuCvu() { return cbuCvu; }
    public void setCbuCvu(String cbuCvu) { this.cbuCvu = cbuCvu; }
    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }
    public String getUserIdentifier() { return userIdentifier; }
    public void setUserIdentifier(String userIdentifier) { this.userIdentifier = userIdentifier; }
    public String getWalletPhone() { return walletPhone; }
    public void setWalletPhone(String walletPhone) { this.walletPhone = walletPhone; }
    public String getWalletEmail() { return walletEmail; }
    public void setWalletEmail(String walletEmail) { this.walletEmail = walletEmail; }
    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
