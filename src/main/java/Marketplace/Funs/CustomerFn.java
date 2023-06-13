package Marketplace.Funs;

import Common.Entity.Customer;
import Common.Entity.Order;
import Common.Entity.Seller;
import Marketplace.Constant.Constants;
import Marketplace.Constant.Enums;
import Marketplace.Types.MsgToCustomer.*;
import Marketplace.Types.MsgToPaymentFn.FailOrder;
import Marketplace.Types.MsgToSeller.GetSeller;
import Marketplace.Types.MsgToSeller.InitSeller;
import Marketplace.Types.State.CustomerState;
import Marketplace.Types.State.SellerAsyncState;
import Marketplace.Types.State.SellerState;
import org.apache.flink.statefun.sdk.java.*;
import org.apache.flink.statefun.sdk.java.message.Message;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class CustomerFn implements StatefulFunction {

    Logger logger = Logger.getLogger("CustomerFn");

    static final TypeName TYPE = TypeName.typeNameOf(Constants.FUNS_NAMESPACE, "customer");

    static final ValueSpec<CustomerState> CUSTOMERSTATE = ValueSpec.named("customer").withCustomType(CustomerState.TYPE);

    public static final StatefulFunctionSpec SPEC = StatefulFunctionSpec.builder(TYPE)
            .withValueSpec(CUSTOMERSTATE)
            .withSupplier(CustomerFn::new)
            .build();

    private String getPartionText(String id) {
        return String.format("[ CustomerFn partitionId %s ] ", id);
    }

    @Override
    public CompletableFuture<Void> apply(Context context, Message message) throws Throwable {
        try {
            // client ---> customer (init customer type)
            if (message.is(InitCustomer.TYPE)) {
                onInitCustomer(context, message);
            }
            // client ---> seller (get seller type)
            else if (message.is(GetCustomer.TYPE)) {
                onGetCustomer(context);
            }
            // ShippmentFn ---> customer (notify shipped type)
            // OrderFn / PaymentFn ---> customer (notify failed payment type)
            // PaymentFn ---> customer (notify success payment type)
            else if (message.is(NotifyCustomer.TYPE)) {
                onhandleNotifyCustomer(context, message);
            }
        } catch (Exception e) {
            System.out.println("Exception in CustomerFn !!!!!!!!!!!!!!!!");
            e.printStackTrace();
        }
        return context.done();
    }

    private void showLog(String log) {
        logger.info(log);
//        System.out.println(log);
    }

    private CustomerState getCustomerState(Context context) {
        return context.storage().get(CUSTOMERSTATE).orElse(new CustomerState());
    }

    private void onInitCustomer(Context context, Message message) {
        InitCustomer initCustomer = message.as(InitCustomer.TYPE);
        Customer customer = initCustomer.getCustomer();
        CustomerState customerState = getCustomerState(context);
        customerState.setCustomer(customer);

        context.storage().set(CUSTOMERSTATE, customerState);

        String log = String.format(getPartionText(context.self().id())
                        + "init customer success\n"
                        + "customer ID: %s\n"
                , customer.getCustomerId());
        showLog(log);
    }

    private void onGetCustomer(Context context) {
        CustomerState customerState = getCustomerState(context);
        Customer customer = customerState.getCustomer();

        if (customer == null) {
            String log = String.format(getPartionText(context.self().id())
                    + "get customer failed as customer doesnt exist\n"
            );
            showLog(log);
            return;
        }

        String log = String.format(getPartionText(context.self().id())
                + "get customer success\n"
                + customer.toString()
                + "\n"
        );
        showLog(log);
    }

    private void onhandleNotifyCustomer(Context context, Message message) {
        CustomerState customerState = getCustomerState(context);
        Customer customer = customerState.getCustomer();

        NotifyCustomer notifyCustomer = message.as(NotifyCustomer.TYPE);
        Order order = notifyCustomer.getOrder();
        Enums.NotificationType notificationType = notifyCustomer.getNotifyType();

        String notificationInfo = "";
        int statistic = 0;
        String statisticInfo = "";

        switch (notificationType) {
            case notify_shipment:
                customer.setPendingDeliveriesCount(customer.getPendingDeliveriesCount() + 1);
                notificationInfo = "[ notify shipment ] ";
                statistic = customer.getPendingDeliveriesCount();
                statisticInfo = "pending deliveries count :";
                break;
            case notify_success_payment:
                customer.setSuccessPaymentCount(customer.getSuccessPaymentCount() + 1);
                notificationInfo = "[ notify success payment ] ";
                statistic = customer.getSuccessPaymentCount();
                statisticInfo = "successful payment count : ";
                break;
            // use in 2 case: fail order and fail payment
            case notify_failed_payment:
                customer.setFailedPaymentCount(customer.getFailedPaymentCount() + 1);
                notificationInfo = "[ notify failed payment ] ";
                statistic = customer.getFailedPaymentCount();
                statisticInfo = "failed payment count : ";
                break;
        }

        context.storage().set(CUSTOMERSTATE, customerState);
        String log = String.format(getPartionText(context.self().id())
                        + notificationInfo
                        + "customer ID: " + customer.getCustomerId() + "\n"
                        + statisticInfo + statistic + "\n"
                        + "order" + order.toString() + "\n");
        showLog(log);
    }
}
