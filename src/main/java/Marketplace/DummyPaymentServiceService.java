package Marketplace;

import Common.Entity.CustomerCheckout;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class DummyPaymentServiceService implements PaymentService {

    public CompletableFuture<Boolean> ContactESP(CustomerCheckout customerCheckout, BigDecimal value) {
        return CompletableFuture.supplyAsync( // non-blocking
            () -> {
                try {
                    TimeUnit.SECONDS.sleep(5); // Simulate a 5-second delay
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // Perform some payment processing logic here
                return true;
            }
        );
    }
}