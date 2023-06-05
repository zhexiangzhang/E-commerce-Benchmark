package Marketplace.Funs;

import Common.Entity.Product;
import Common.Entity.StockItem;
import Marketplace.Constant.Constants;
import Marketplace.Constant.Enums;
import Marketplace.Types.MsgToSeller.AddProduct;
import Marketplace.Types.MsgToSeller.DeleteProduct;
import Marketplace.Types.MsgToSeller.IncreaseStock;
import Marketplace.Types.MsgToSeller.TaskFinish;
import Marketplace.Types.State.StockState;
import org.apache.flink.statefun.sdk.java.*;
import org.apache.flink.statefun.sdk.java.message.Message;
import org.apache.flink.statefun.sdk.java.message.MessageBuilder;
import org.apache.flink.statefun.sdk.java.types.Type;
import org.apache.flink.statefun.sdk.java.types.Types;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class StockFn implements StatefulFunction {

    Logger logger = Logger.getLogger("StockFn");

    static final TypeName TYPE = TypeName.typeNameOf(Constants.FUNS_NAMESPACE, "stock");

    static final ValueSpec<StockState> STOCKSTATE = ValueSpec.named("stock").withCustomType(StockState.TYPE);

    //  Contains all the information needed to create a function instance
    public static final StatefulFunctionSpec SPEC = StatefulFunctionSpec.builder(TYPE)
            .withValueSpec(STOCKSTATE)
            .withSupplier(StockFn::new)
            .build();

    private String getPartionText(String id) {
        return String.format("\n[ StockFn partitionId %s ] \n", id);
    }

    @Override
    public CompletableFuture<Void> apply(Context context, Message message) throws Throwable {
        if (message.is(IncreaseStock.TYPE)) {
            onIncreaseStock(context, message);
        } else if (message.is(AddProduct.TYPE)) {
            onAddItem(context, message);
        } else if (message.is(DeleteProduct.TYPE)) {
            onDeleteItem(context, message);
        }
        return context.done();
    }

    private StockState getStockState(Context context) {
        return context.storage().get(STOCKSTATE).orElse(new StockState());
    }

    private void showLog(String log) {
//        logger.info(log);
        System.out.println(log);
    }

    private void onIncreaseStock(Context context, Message message) {
        IncreaseStock increaseStock = message.as(IncreaseStock.TYPE);
        Long productId = increaseStock.getProductId();
        int num = increaseStock.getNumber();
        StockState stockState = getStockState(context);
        stockState.increaseStock(productId, num);
        context.storage().set(STOCKSTATE, stockState);


        String log = String.format(getPartionText(context.self().id())
                        + "increaseStock success\n"
                        + "productId: %s\n"
                , productId);
        showLog(log);

        String result = "IncreaseStock success, productId: " + productId + ", number: " + num;
        sendMessageToCaller(context, Types.stringType(), result);
    }

    private void onAddItem(Context context, Message message) {
        StockState stockState = getStockState(context);
        AddProduct addProduct = message.as(AddProduct.TYPE);
        Product product = addProduct.getProduct();
        Long productId = product.getId();
        Long sellerId = product.getSellerId();
        LocalDateTime time = LocalDateTime.now();
        StockItem stockItem = new StockItem(
                productId,
                sellerId,
                0,
                0,
                0,
                true,
                time,
                time);
        stockState.addItem(stockItem);
        context.storage().set(STOCKSTATE, stockState);

        String log = String.format(getPartionText(context.self().id())
                        + "addProduct success\n"
                        + "productId: %s\n"
                , productId);
        showLog(log);

        sendTaskResToSeller(context, productId, Enums.TaskType.AddProductType);
    }

    private void onDeleteItem(Context context, Message message) {
        StockState stockState = getStockState(context);
        DeleteProduct deleteProduct = message.as(DeleteProduct.TYPE);
        Long productId = deleteProduct.getProduct_id();
        StockItem stockItem = stockState.getItem(productId);
        stockItem.setUpdatedAt(LocalDateTime.now());
        stockItem.setIs_active(false);
        context.storage().set(STOCKSTATE, stockState);

        String log = String.format(getPartionText(context.self().id())
                        + "deleteItem success\n"
                        + "productId: %s\n"
                , productId);
        showLog(log);

        sendTaskResToSeller(context, productId, Enums.TaskType.DeleteProductType);
    }

    private <T> void sendMessage(Context context, TypeName addressType, String addressId, Type<T> messageType, T messageContent) {
        Message msg = MessageBuilder.forAddress(addressType, addressId)
                .withCustomType(messageType, messageContent)
                .build();
        context.send(msg);
    }

    private <T> void sendMessageToCaller(Context context, Type<T> messageType, T messageContent) {
        final Optional<Address> caller = context.caller();
        if (caller.isPresent()) {
            context.send(
                    MessageBuilder.forAddress(caller.get())
                            .withCustomType(messageType, messageContent)
                            .build());
        } else {
            throw new IllegalStateException("There should always be a caller");
        }
    }

    private void sendTaskResToSeller(Context context, Long productId, Enums.TaskType taskType) {
        final Optional<Address> caller = context.caller();
        if (caller.isPresent()) {
            TaskFinish taskFinish = new TaskFinish(taskType, Enums.SendType.StockFn, productId);
            context.send(
                    MessageBuilder.forAddress(caller.get())
                            .withCustomType(TaskFinish.TYPE, taskFinish)
                            .build());
        } else {
            throw new IllegalStateException("There should always be a caller.");
        }
    }
}
