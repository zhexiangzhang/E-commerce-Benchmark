package Marketplace.Funs;

import Marketplace.Constant.Constants;
import Marketplace.Types.Messages;
import org.apache.flink.statefun.sdk.java.*;
import org.apache.flink.statefun.sdk.java.message.Message;
import org.apache.flink.statefun.sdk.java.message.MessageBuilder;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static Marketplace.Types.Messages.CHECKOUT_FINISH_TYPE;

public class OrderFn implements StatefulFunction {
    static final TypeName TYPE = TypeName.typeNameOf(Constants.FUNS_NAMESPACE, "order");
//test
//    private final Map<Long, Order> orders;
//    private final Map<Long, List<OrderItem>> items;
//    private final SortedMap<Long, List<OrderHistory>> history;

    //  存储的状态值
    static final ValueSpec<Integer> SEEN = ValueSpec.named("seen").withIntType();

    //  包含了创建函数实例所需的所有信息
    public static final StatefulFunctionSpec SPEC = StatefulFunctionSpec.builder(TYPE)
            .withValueSpec(SEEN) // 如果是两个，就是.withValueSpecs(SEEN, SEEN2)
            .withSupplier(OrderFn::new)
            .build();

    @Override
    public CompletableFuture<Void> apply(Context context, Message message) throws Throwable {
        if (message.is(Messages.CHECKOUT_TYPE)) {
            // TODO: 5/18/2023 具体的业务流程
//            等待5秒
            System.out.println("OrderFn: checkout cart");
            Thread.sleep(5000);
            String result = "success";
            final Optional<Address> caller = context.caller();
            if (caller.isPresent()) {
                context.send(
                        MessageBuilder.forAddress(caller.get())
                                .withCustomType(CHECKOUT_FINISH_TYPE, result)
                                .build());
            } else {
                throw new IllegalStateException("There should always be a caller to checkout");
            }
        }
        return context.done();
    }
}
