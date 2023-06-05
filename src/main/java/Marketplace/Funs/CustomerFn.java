package Marketplace.Funs;

import org.apache.flink.statefun.sdk.java.Context;
import org.apache.flink.statefun.sdk.java.StatefulFunction;
import org.apache.flink.statefun.sdk.java.message.Message;

import java.util.concurrent.CompletableFuture;

public class CustomerFn implements StatefulFunction {
    @Override
    public CompletableFuture<Void> apply(Context context, Message message) throws Throwable {
        return null;
    }
}
