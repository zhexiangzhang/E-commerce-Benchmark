package Marketplace.Funs;

import java.util.logging.Logger;

import Common.Entity.BasketItem;
import Common.Entity.CustomerCheckout;
import Marketplace.Constant.Constants;
import Marketplace.Types.Entity.Checkout;

import Marketplace.Types.MsgToCartFn.AddToCart;
import Marketplace.Types.MsgToCartFn.CheckoutCart;
import Marketplace.Types.MsgToCartFn.GetCart;
import Marketplace.Types.MsgToCartFn.ClearCart;

import Marketplace.Types.Messages;
import Marketplace.Types.State.CartState;
import org.apache.flink.statefun.sdk.java.*;
import org.apache.flink.statefun.sdk.java.message.Message;
import org.apache.flink.statefun.sdk.java.message.MessageBuilder;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static Marketplace.Types.Messages.*;

public class CartFn implements StatefulFunction {

    Logger logger = Logger.getLogger("CartFn");

    // Statefun Type ，Logical name = <namespace> + <name>
    static final TypeName TYPE = TypeName.typeNameOf(Constants.FUNS_NAMESPACE, "cart");

    /*
        I think no need to save custom ID in ValueSpec, we can get the ID by: String id = context.self().id();
        also didnot find any usage of CUSTOMER, so didnot store it in ValueSpec
    */
    //    static final ValueSpec<Long> CUSTOMERID = ValueSpec.named("customerId").withLongType();
    static final ValueSpec<CartState> CARTSTATE = ValueSpec.named("cartState").withCustomType(CartState.TYPE);

    //  Contains all the information needed to create a function instance
    public static final StatefulFunctionSpec SPEC = StatefulFunctionSpec.builder(TYPE)
            .withValueSpec(CARTSTATE)
            .withSupplier(CartFn::new)
            .build();

    // for GET_CART_TYPE request, get items in cart and send to egress
    private static final TypeName ECOMMERCE_EGRESS = TypeName.typeNameOf(Constants.EGRESS_NAMESPACE, "egress");

    @Override
    public CompletableFuture<Void> apply(Context context, Message message) throws Throwable {
        // client ---> cart (add some item to cart)
        if (message.is(AddToCart.TYPE)) {
            onAddToCart(context, message);
        }
        // client ---> cart (send checkout request)
        else if (message.is(CheckoutCart.TYPE)) {
            onCheckoutCart(context, message);
        }
        // client ---> cart (clear cart)
        else if (message.is(ClearCart.TYPE)) {
            onClearCart(context);
        }
        // order ---> cart (send checkout result)
        else if (message.is(Messages.CHECKOUT_FINISH_TYPE)) {
            onCheckoutFinish(context, message);
        }
        // order ---> cart (get cart content)
        else if (message.is(GetCart.TYPE)) {
            onGetCart(context);
        }
        return context.done();
    }

    private void Seal(Context context) throws Exception {
        CartState cartState = context.storage().get(CARTSTATE).orElse(new CartState());
        if (cartState.getStatus() == CartState.Status.CHECKOUT_SENT) {
            cartState.setStatus(CartState.Status.OPEN);
            cartState.clear();
            context.storage().set(CARTSTATE, cartState);
        } else {
            System.out.println("Cannot seal a cart that has not been checked out");
            throw new Exception("Cannot seal a cart that has not been checked out");
        }
    }

    private void onAddToCart(Context context, Message message) {

        CartState cartState = context.storage().get(CARTSTATE).orElse(new CartState());
        AddToCart addToCart = message.as(AddToCart.TYPE);
        BasketItem item = addToCart.getItem();
        cartState.addItem(item.getProductId(), item);
        context.storage().set(CARTSTATE, cartState);

        logger.info(String.format("Item {%s} add to cart {%s} success", item.getProductId(), context.self().id()));
    }

    private void onCheckoutCart(Context context, Message message) {
        // TODO: 5/17/2023 与订单服务交互，生成订单，需要完成服务器完成订单后发送过来消息的else if

        CustomerCheckout customerCheckout = message.as(CheckoutCart.TYPE).getCustomerCheckout();

        logger.info(String.format("checkout cart {%s}...........", context.self().id()));

        CartState cartState = context.storage().get(CARTSTATE).orElse(new CartState());
        String custumerId = context.self().id();

        if (cartState.getStatus() == CartState.Status.CHECKOUT_SENT ){
            System.out.println(custumerId + " checkout already sent");
//                throw new Exception(custumerId + "checkout in progress");
        } else {
            if (cartState.getItems().isEmpty()) {
                System.out.println(custumerId + " cart is empty");
//              throw new Exception(custumerId + " cart is empty");
            } else {
                Checkout checkout = new Checkout(LocalDateTime.now(), customerCheckout, cartState.getItems());
                cartState.setStatus(CartState.Status.CHECKOUT_SENT);
                context.storage().set(CARTSTATE, cartState);
                context.send(MessageBuilder.forAddress(OrderFn.TYPE, "orderService0")
                        .withCustomType(CHECKOUT_TYPE, checkout)
                        .build());

                System.out.println("checkout has been sent to order...........");
            }
        }
    }

    private void onCheckoutFinish(Context context, Message message) throws Exception {
        System.out.println("checkout finish...........");

        String checkoutResult = message.as(Messages.CHECKOUT_FINISH_TYPE);

        if (checkoutResult.equals("success")) {
            Seal(context);
        } else {
            System.out.println("checkout fail");
        }
    }

    private void onClearCart(Context context) {
        final AddressScopedStorage storage = context.storage();
        storage
                .get(CARTSTATE)
                .ifPresent(
                        cartState -> {
                            cartState.clear();
                            storage.set(CARTSTATE, cartState);
                        });
        logger.info(String.format("clear cart {%s} success", context.self().id()));
    }


    private void onGetCart(Context context) {
        CartState cartState = context.storage().get(CARTSTATE).orElse(new CartState());

        String cartContent = cartState.getCartConent();

//        final EgressRecord egressRecord = new EgressRecord("getCart", cartContent);
//        context.send(
//                // create new EgressMessageBuilder object，target output address is ECOMMERCE_EGRESS
//                EgressMessageBuilder.forEgress(ECOMMERCE_EGRESS)
//                        // EGRESS_RECORD_JSON_TYPE:messgae type，egressRecord:message content
//                        .withCustomType(EGRESS_RECORD_JSON_TYPE, egressRecord)
//                        .build());
//

        logger.info(String.format("get cart {%s} success", context.self().id()));
        logger.info(String.format("cart content: {%s}", cartContent));
    }
}
