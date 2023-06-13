package Marketplace.Funs;

import Common.Entity.*;
import Marketplace.Constant.Constants;
import Marketplace.Constant.Enums;
import Marketplace.Types.MsgToCustomer.NotifyCustomer;
import Marketplace.Types.MsgToOrderFn.OrderStateUpdate;
import Marketplace.Types.MsgToShipment.ProcessShipment;
import Marketplace.Types.State.ShipmentState;
import org.apache.flink.statefun.sdk.java.*;
import org.apache.flink.statefun.sdk.java.message.Message;
import org.apache.flink.statefun.sdk.java.message.MessageBuilder;
import org.apache.flink.statefun.sdk.java.types.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class ShipmentFn implements StatefulFunction {

    Logger logger = Logger.getLogger("ShipmentFn");

    static final TypeName TYPE = TypeName.typeNameOf(Constants.FUNS_NAMESPACE, "shipment");

    static final ValueSpec<Long> SHIPMENTIDSTATE = ValueSpec.named("shipmentIdState").withLongType();
    static final ValueSpec<ShipmentState> SHIPMENT_STATE = ValueSpec.named("shipmentState").withCustomType(ShipmentState.TYPE);

    //  Contains all the information needed to create a function instance
    public static final StatefulFunctionSpec SPEC = StatefulFunctionSpec.builder(TYPE)
            .withValueSpecs(SHIPMENTIDSTATE, SHIPMENT_STATE)
            .withSupplier(ShipmentFn::new)
            .build();

    @Override
    public CompletableFuture<Void> apply(Context context, Message message) throws Throwable {
        try {
            if (message.is(ProcessShipment.TYPE)) {
                onProcessShipment(context, message);
            }
        } catch (Exception e) {
            System.out.println("ShipmentFn Exception !!!!!!!!!!!!!!!!");
        }
        return context.done();
    }

    private Long generateNextShipmentId(Context context) {
        Long shipmentId = context.storage().get(SHIPMENTIDSTATE).orElse(0L) + 1;
        context.storage().set(SHIPMENTIDSTATE, shipmentId);
        return shipmentId;
    }

    private String getPartionText(String id) {
        return String.format("[ ShipmentFn partitionId %s ] ", id);
    }

    private ShipmentState getShipmentState(Context context) {
        return context.storage().get(SHIPMENT_STATE).orElse(new ShipmentState());
    }

    private void showLog(String log) {
        logger.info(log);
//        System.out.println(log);
    }

    private void showLogPrt(String log) {
//        logger.info(log);
        System.out.println(log);
    }

    private void onProcessShipment(Context context, Message message) {
        ProcessShipment processShipment = message.as(ProcessShipment.TYPE);
        Invoice invoice = processShipment.getInvoice();
        CustomerCheckout customerCheckout = invoice.getCustomerCheckout();

        String log = getPartionText(context.self().id()) + "ShipmentFn: onProcessShipment: " + processShipment.toString();
        logger.info(log);

        long shipmentId = generateNextShipmentId(context);
        Shipment shipment = new Shipment(
                shipmentId,
                invoice.getOrder().getId(),
                customerCheckout.getCustomerId(),
                customerCheckout.getName(),
                invoice.getItems().size(),
                customerCheckout.getAddress(),
                customerCheckout.getZipCode(),
                invoice.getOrder().getPurchaseTimestamp(),
                Enums.PackageStatus.CREATED
        );

        int packageId = 1;
        List<OrderItem> orderItems = invoice.getItems();
        List<PackageItem> packages = new ArrayList<>();
        for (OrderItem orderItem : orderItems) {
            PackageItem pkg = new PackageItem(
                    packageId,
                    shipmentId,
                    orderItem.getProductId(),
                    orderItem.getQuantity(),
                    Enums.PackageStatus.SHIPPED
            );
            packages.add(pkg);
            packageId++;
        }

        ShipmentState shipmentState = getShipmentState(context);
        shipmentState.addShipment(shipmentId, shipment);
        shipmentState.addPackage(shipmentId, packages);
        context.storage().set(SHIPMENT_STATE, shipmentState);

        /**
         * Based on olist (https://dev.olist.com/docs/orders), the status of the order is
         * shipped when "at least one order item has been shipped"
         * All items are considered shipped here, so just signal the order about that
         */

        String orderPartition = invoice.getOrderPartitionID();
        sendMessage(
                context,
                OrderFn.TYPE,
                String.valueOf(orderPartition),
                OrderStateUpdate.TYPE,
                new OrderStateUpdate(
                        invoice.getOrder().getId(),
                        Enums.OrderStatus.SHIPPED
                )
        );

        // send message to customer
        sendMessage(
                context,
                CustomerFn.TYPE,
                String.valueOf(customerCheckout.getCustomerId()),
                NotifyCustomer.TYPE,
                new NotifyCustomer(
                        customerCheckout.getCustomerId(),
                        invoice.getOrder(),
                        Enums.NotificationType.notify_shipment
                )
        );

        String log_ = getPartionText(context.self().id())
                + "order is shipped, send message to orderFn and customerFn\n";
        showLog(log_);

        // TODO: 6/12/2023 whether need to confirm ????

    }

    private <T> void sendMessage(Context context, TypeName addressType, String addressId, Type<T> messageType, T messageContent) {
        Message msg = MessageBuilder.forAddress(addressType, addressId)
                .withCustomType(messageType, messageContent)
                .build();
        context.send(msg);
    }
}
