package Marketplace.Types.MsgToSeller;

import Marketplace.Constant.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.statefun.sdk.java.TypeName;
import org.apache.flink.statefun.sdk.java.types.SimpleType;
import org.apache.flink.statefun.sdk.java.types.Type;

public class GetProducts {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static final Type<GetProducts> TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "GetProducts"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, GetProducts.class));

    GetProducts() {
    }

}
