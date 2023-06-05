package Marketplace.Types;

import Marketplace.Constant.Constants;
import Marketplace.Types.Entity.Checkout;
import Marketplace.Types.Entity.TmpUserLogin;
import Marketplace.Types.Entity.TmpUserPofile;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.statefun.sdk.java.TypeName;
import org.apache.flink.statefun.sdk.java.types.SimpleType;
import org.apache.flink.statefun.sdk.java.types.Type;

public final class Messages {
    private Messages() {}

    private static final ObjectMapper mapper = new ObjectMapper(); // JSON

//    private static final String TYPES_NAMESPACE = "e-commerce.types";

    public static final Type<TmpUserLogin> USER_LOGIN_JSON_TYPE =
            SimpleType.simpleImmutableTypeFrom(
                TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "UserLogin"),
                mapper::writeValueAsBytes,
                bytes -> mapper.readValue(bytes, TmpUserLogin.class)
    );

    public static final Type<TmpUserPofile> USER_PROFILE_JSON_TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "UserProfile"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, TmpUserPofile.class)
            );

    /* ingress (customer) -> cart */
//    public static final Type<AddToCart> ADD_TO_CART_TYPE =
//            SimpleType.simpleImmutableTypeFrom(
//                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "AddToCart"),
//                    mapper::writeValueAsBytes,
//                    bytes -> mapper.readValue(bytes, AddToCart.class));

    /* ingress (customer) -> cart */
//    public static final Type<CheckoutCart> CHECKOUT_CART_TYPE =
//            SimpleType.simpleImmutableTypeFrom(
//                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "CheckoutCart"),
//                    mapper::writeValueAsBytes,
//                    bytes -> mapper.readValue(bytes, CheckoutCart.class));

//    /* ingress (customer) -> cart */
//    public static final Type<ClearCart> CLEAR_CART_TYPE =
//            SimpleType.simpleImmutableTypeFrom(
//                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "ClearCart"),
//                    mapper::writeValueAsBytes,
//                    bytes -> mapper.readValue(bytes, ClearCart.class));

    /* cart -> order */
    public static final Type<Checkout> CHECKOUT_TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "Checkout"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, Checkout.class));
    /* order -> cart */
    public static final Type<String> CHECKOUT_FINISH_TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "CheckoutFinish"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, String.class)
            );

    //    这种应该没必要写成一个类
//    public static final Type<GetCart> GET_CART_TYPE =
//            SimpleType.simpleImmutableTypeFrom(
//                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "GetCart"),
//                    mapper::writeValueAsBytes,
//                    bytes -> mapper.readValue(bytes, GetCart.class));
//    public static final Type<Long> GET_CART_TYPE =
//            SimpleType.simpleImmutableTypeFrom(
//                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "GetCart"),
//                    mapper::writeValueAsBytes,
//                    bytes -> mapper.readValue(bytes, Long.class));



    public static final Type<EgressRecord> EGRESS_RECORD_JSON_TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.EGRESS_NAMESPACE, "EgressRecord"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, EgressRecord.class)
            );

}
