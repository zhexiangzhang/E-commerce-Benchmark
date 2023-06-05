package Marketplace.Types.State;

import Common.Entity.Customer;
import Common.Entity.Seller;
import Marketplace.Constant.Constants;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.statefun.sdk.java.TypeName;
import org.apache.flink.statefun.sdk.java.types.SimpleType;
import org.apache.flink.statefun.sdk.java.types.Type;

@Setter
@Getter
public class CustomerState {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static final Type<CustomerState> TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "CustomerState"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, CustomerState.class));

    @JsonProperty("customer")
    public Customer customer;
}
