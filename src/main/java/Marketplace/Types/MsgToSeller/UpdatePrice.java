package Marketplace.Types.MsgToSeller;

import Marketplace.Constant.Constants;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.statefun.sdk.java.TypeName;
import org.apache.flink.statefun.sdk.java.types.SimpleType;
import org.apache.flink.statefun.sdk.java.types.Type;

@Setter
@Getter
public class UpdatePrice {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static final Type<UpdatePrice> TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "UpdatePrice"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, UpdatePrice.class));

    @JsonProperty("product_id")
    private Long product_id;
    @JsonProperty("price")
    private Double price;

    public UpdatePrice() {
    }

    @JsonCreator
    public UpdatePrice(@JsonProperty("product_id") Long product_id,
                       @JsonProperty("price") Double price) {
        this.product_id = product_id;
        this.price = price;
    }
}
