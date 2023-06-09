package Marketplace.Funs;

import Common.Entity.BasketItem;
import Marketplace.Constant.Constants;
import Marketplace.Constant.Enums;
import Marketplace.Types.Entity.Checkout;
import Marketplace.Types.MsgToCartFn.CheckoutCartResult;
import Marketplace.Types.MsgToStock.CheckoutResv;
import Marketplace.Types.State.OrderAsyncTaskState;
import Marketplace.Types.State.OrderTempInfoState;
import org.apache.flink.statefun.sdk.java.*;
import org.apache.flink.statefun.sdk.java.message.Message;
import org.apache.flink.statefun.sdk.java.message.MessageBuilder;
import org.apache.flink.statefun.sdk.java.types.Type;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class OrderFn implements StatefulFunction {
    static final TypeName TYPE = TypeName.typeNameOf(Constants.FUNS_NAMESPACE, "order");

    Logger logger = Logger.getLogger("OrderFn");

    // generate unique Identifier
    static final ValueSpec<Long> ATPTRESTASKIDSTATE = ValueSpec.named("atptResvTaskIdState").withLongType();
    // store checkout info
    static final ValueSpec<OrderTempInfoState> TEMPCKINFOSTATE = ValueSpec.named("tempCKInfoState").withCustomType(OrderTempInfoState.TYPE);
    // tmp store async task state
    static final ValueSpec<OrderAsyncTaskState> ASYNCTASKSTATE = ValueSpec.named("asyncTaskState").withCustomType(OrderAsyncTaskState.TYPE);
    //  包含了创建函数实例所需的所有信息
    public static final StatefulFunctionSpec SPEC = StatefulFunctionSpec.builder(TYPE)
            .withValueSpecs(ATPTRESTASKIDSTATE, ASYNCTASKSTATE, TEMPCKINFOSTATE)
            .withSupplier(OrderFn::new)
            .build();

    @Override
    public CompletableFuture<Void> apply(Context context, Message message) throws Throwable {
        try {
            // cart --> order, (checkout request)
            if (message.is(Checkout.TYPE)) {
                onAtptReservation(context, message);
            }
            // stock --> order, (checkout response)
            else if (message.is(CheckoutResv.TYPE)) {
                onHandleCheckoutResponse(context, message);
            }
        } catch (Exception e) {
            System.out.println("OrderFn error: !!!!!!!!!!!!" + e.getMessage());
            e.printStackTrace();
        }
        return context.done();
    }

    private OrderAsyncTaskState getAtptResvTaskState(Context context) {
        return context.storage().get(ASYNCTASKSTATE).orElse(new OrderAsyncTaskState());
    }

    private OrderTempInfoState getTempCKInfoState(Context context) {
        return context.storage().get(TEMPCKINFOSTATE).orElse(new OrderTempInfoState());
    }

    private Long generateNextAtptResvTaskId(Context context) {
        Long nextId = context.storage().get(ATPTRESTASKIDSTATE).orElse(0L) + 1;
        context.storage().set(ATPTRESTASKIDSTATE, nextId);
        return nextId;
    }

    private String getPartionText(String id) {
        return String.format("\n[ OrderFn partitionId %s ] \n", id);
    }

    private void showLog(String log) {
        logger.info(log);
//        System.out.println(log);
    }

    private void showLogPrt(String log) {
//        logger.info(log);
        System.out.println(log);
    }

    private void onAtptReservation(Context context, Message message) {
        OrderAsyncTaskState atptResvTaskState = getAtptResvTaskState(context);
        OrderTempInfoState tempCKInfoState = getTempCKInfoState(context);
        Checkout checkout = message.as(Checkout.TYPE);

        Map<Long, BasketItem> items = checkout.getItems();
        long customerId = checkout.getCustomerCheckout().getCustomerId();
        int nItems = items.size();

        tempCKInfoState.addCheckout(customerId, checkout);
//        atptResvTaskState.addNewTaskCnt(customerId, nItems);
        atptResvTaskState.addNewTask(customerId, nItems, Enums.TaskType.AttemptReservationsType);
        context.storage().set(ASYNCTASKSTATE, atptResvTaskState);
        context.storage().set(TEMPCKINFOSTATE, tempCKInfoState);

        for (Map.Entry<Long, BasketItem> entry : items.entrySet()) {
            int stockPartitionId = (int) (entry.getValue().getProductId() % Constants.nStockPartitions);
            sendMessage(context,
                    StockFn.TYPE,
                    String.valueOf(stockPartitionId),
                    CheckoutResv.TYPE,
                    new CheckoutResv(
                            customerId,
                            entry.getValue(),
                            Enums.TaskType.AttemptReservationsType,
                            Enums.ItemStatus.UNKNOWN));
        }

        String log = getPartionText(context.self().id())
                + "OrderFn: attempt reservation, message sent to stock, customerId: "
                + customerId + "\n";
        showLogPrt(log);
    }

    private void onHandleCheckoutResponse(Context context, Message message) {
        CheckoutResv checkoutResv = message.as(CheckoutResv.TYPE);
        Enums.TaskType taskType = checkoutResv.getTaskType();
        long customerId = checkoutResv.getCustomerId();
        switch (taskType) {
            case AttemptReservationsType:
                System.out.println("OrderFn: attempt reservation response, customerId: " + customerId);
                dealAttemptResponse(context, checkoutResv);
                break;
            case ConfirmReservationsType:
                System.out.println("OrderFn: confirm reservation response, customerId: " + customerId);
                dealConfirmResponse(context, customerId);
                break;
            case CancelReservationsType:
                System.out.println("OrderFn: cancel reservation response, customerId: " + customerId);
                dealCancelResponse(context, customerId);
                break;
            default:
                break;
        }
    }

    private void dealAttemptResponse(Context context, CheckoutResv checkoutResv) {
        long customerId = checkoutResv.getCustomerId();
        OrderAsyncTaskState atptResvTaskState = getAtptResvTaskState(context);
        atptResvTaskState.addCompletedSubTask(customerId, checkoutResv, Enums.TaskType.AttemptReservationsType);
        boolean isTaskComplete = atptResvTaskState.isTaskComplete(customerId, Enums.TaskType.AttemptReservationsType);
        if (isTaskComplete) {
            List<CheckoutResv> checkoutResvs = atptResvTaskState.getSingleCheckoutResvTask(customerId);
            if (atptResvTaskState.isTaskSuccess(customerId)) {
                showLogPrt("OrderFn: attempt reservation success, customerId: " + customerId);
                // set anync task state
                atptResvTaskState.addNewTask(customerId, checkoutResvs.size(), Enums.TaskType.ConfirmReservationsType);
                onDecideReservations(context, checkoutResvs, Enums.TaskType.ConfirmReservationsType);
            } else {
                showLogPrt("OrderFn: attempt reservation failed, customerId: " + customerId);

                List<CheckoutResv> checkoutResvsSuccessSub = atptResvTaskState.getSuccessAttempResvSubtask(customerId);
                // set anync task state
                atptResvTaskState.addNewTask(customerId, checkoutResvsSuccessSub.size(), Enums.TaskType.CancelReservationsType);
                onDecideReservations(context, checkoutResvsSuccessSub, Enums.TaskType.CancelReservationsType);
            }
            atptResvTaskState.removeTask(customerId);
        }
        context.storage().set(ASYNCTASKSTATE, atptResvTaskState);
    }

//    confirm reservation or cancel reservation (second step, send confirm or cancel message to stock)
    private void onDecideReservations(Context context, List<CheckoutResv> checkoutResvs, Enums.TaskType taskType) {
        for (CheckoutResv checkoutResv : checkoutResvs) {
            int stockPartitionId = (int) (checkoutResv.getItem().getProductId() % Constants.nStockPartitions);
            checkoutResv.setTaskType(taskType);
            sendMessage(context,
                    StockFn.TYPE,
                    String.valueOf(stockPartitionId),
                    CheckoutResv.TYPE,
                    checkoutResv);
        }
        String log = "OrderFn: decide reservation, message sent to stock, customerId: " + taskType + "\n";
        showLogPrt(log);
    }

//    last step of case 1: confirm reservation
    private void dealConfirmResponse(Context context, Long customerId) {
        OrderAsyncTaskState atptResvTaskState = getAtptResvTaskState(context);
        atptResvTaskState.addCompletedSubTask(customerId, null, Enums.TaskType.ConfirmReservationsType);
        boolean isTaskComplete = atptResvTaskState.isTaskComplete(customerId, Enums.TaskType.ConfirmReservationsType);
        if (isTaskComplete) {
            String log = getPartionText(context.self().id())
                    + " @@@@ OrderFn: confirm reservation finish, customerId: " + customerId + "\n";
            showLogPrt(log);
            // TODO: 6/6/2023  do something
            sendMessage(context,
                    CartFn.TYPE,
                    String.valueOf(customerId),
                    CheckoutCartResult.TYPE,
                    new CheckoutCartResult(true));
        }
        context.storage().set(ASYNCTASKSTATE, atptResvTaskState);
    }

//    last step of case 2: cancel reservation
    private void dealCancelResponse(Context context, Long customerId) {
        OrderAsyncTaskState atptResvTaskState = getAtptResvTaskState(context);
        atptResvTaskState.addCompletedSubTask(customerId, null, Enums.TaskType.CancelReservationsType);
        boolean isTaskComplete = atptResvTaskState.isTaskComplete(customerId, Enums.TaskType.CancelReservationsType);
        if (isTaskComplete) {

            String log = getPartionText(context.self().id())
                    + " @@@@ OrderFn: cancel reservation finish, customerId: " + customerId + "\n";
            showLogPrt(log);
            // TODO: 6/6/2023  do something
            sendMessage(context,
                    CartFn.TYPE,
                    String.valueOf(customerId),
                    CheckoutCartResult.TYPE,
                    new CheckoutCartResult(false));
        }
        context.storage().set(ASYNCTASKSTATE, atptResvTaskState);
    }

    private <T> void sendMessage(Context context, TypeName addressType, String addressId, Type<T> messageType, T messageContent) {
        Message msg = MessageBuilder.forAddress(addressType, addressId)
                .withCustomType(messageType, messageContent)
                .build();
        context.send(msg);
    }
}
