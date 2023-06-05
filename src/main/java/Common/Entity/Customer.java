package Common.Entity;

import java.math.BigDecimal;

public class Customer {
    // olist data set
    private long id;

    // added
    private String firstName;
    private String lastName;
    private String address;
    private String complement;
    private String birthDate;

    // olist data set
    private String zipCodePrefix;
    private String city;
    private String state;

    // card
    private String cardNumber;
    private String cardSecurityNumber;
    private String cardExpiration;
    private String cardHolderName;
    private String cardType;

    // statistics
    private int successPaymentCount;
    private int failedPaymentCount;
    private int pendingDeliveriesCount;
    private int deliveryCount;
    private int abandonedCartCount;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getComplement() {
        return complement;
    }

    public void setComplement(String complement) {
        this.complement = complement;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getZipCodePrefix() {
        return zipCodePrefix;
    }

    public void setZipCodePrefix(String zipCodePrefix) {
        this.zipCodePrefix = zipCodePrefix;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getCardSecurityNumber() {
        return cardSecurityNumber;
    }

    public void setCardSecurityNumber(String cardSecurityNumber) {
        this.cardSecurityNumber = cardSecurityNumber;
    }

    public String getCardExpiration() {
        return cardExpiration;
    }

    public void setCardExpiration(String cardExpiration) {
        this.cardExpiration = cardExpiration;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public void setCardHolderName(String cardHolderName) {
        this.cardHolderName = cardHolderName;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public int getSuccessPaymentCount() {
        return successPaymentCount;
    }

    public void setSuccessPaymentCount(int successPaymentCount) {
        this.successPaymentCount = successPaymentCount;
    }

    public int getFailedPaymentCount() {
        return failedPaymentCount;
    }

    public void setFailedPaymentCount(int failedPaymentCount) {
        this.failedPaymentCount = failedPaymentCount;
    }

    public int getPendingDeliveriesCount() {
        return pendingDeliveriesCount;
    }

    public void setPendingDeliveriesCount(int pendingDeliveriesCount) {
        this.pendingDeliveriesCount = pendingDeliveriesCount;
    }

    public int getDeliveryCount() {
        return deliveryCount;
    }

    public void setDeliveryCount(int deliveryCount) {
        this.deliveryCount = deliveryCount;
    }

    public int getAbandonedCartCount() {
        return abandonedCartCount;
    }

    public void setAbandonedCartCount(int abandonedCartCount) {
        this.abandonedCartCount = abandonedCartCount;
    }

    public BigDecimal getTotalSpentItems() {
        return totalSpentItems;
    }

    public void setTotalSpentItems(BigDecimal totalSpentItems) {
        this.totalSpentItems = totalSpentItems;
    }

    public BigDecimal getTotalSpentFreights() {
        return totalSpentFreights;
    }

    public void setTotalSpentFreights(BigDecimal totalSpentFreights) {
        this.totalSpentFreights = totalSpentFreights;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    private BigDecimal totalSpentItems;
    private BigDecimal totalSpentFreights;

    // additional
    private String data;

}
