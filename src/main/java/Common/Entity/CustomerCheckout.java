package Common.Entity;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public class CustomerCheckout {

    @JsonProperty("customerId") private long customerId;
//    private String firstName;
//    private String lastName;
//    private String street;
//    private String complement;
//    private String city;
//    private String state;
//    private String zipCode;
//    private String paymentType;
    private String cardNumber;
//    private String cardHolderName;
//    private String cardExpiration;
//    private String cardSecurityNumber;
//    private String cardBrand;
//    private int installments;
//    private BigDecimal[] vouchers;

    // getters and setters
    public long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(long customerId) {
        this.customerId = customerId;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }
}
