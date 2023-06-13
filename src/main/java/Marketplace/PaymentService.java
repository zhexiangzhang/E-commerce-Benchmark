package Marketplace;

import Common.Entity.CustomerCheckout;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

public interface PaymentService {
    CompletableFuture<Boolean> ContactESP(CustomerCheckout customerCheckout, BigDecimal value);
}
