package Marketplace.Types.State;

import Common.Entity.BasketItem;
import Marketplace.Constant.Constants;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.statefun.sdk.java.TypeName;
import org.apache.flink.statefun.sdk.java.types.SimpleType;
import org.apache.flink.statefun.sdk.java.types.Type;

import java.util.HashMap;
import java.util.Map;

public class CartState {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static final Type<CartState> TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "CartState"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, CartState.class));

    public enum Status
    {
        OPEN,
        CHECKOUT_SENT,
        PRODUCT_DIVERGENCE
    };

    @JsonProperty("status")
    public Status status;
    @JsonProperty("items")
    public Map<Long, BasketItem> items; //     private final Map<Long, BasketItem> items;

    public CartState() {
        this.status = Status.OPEN;
        this.items = new HashMap<>();
    }

//    写一个方法，接受Long和BasketItem，给items添加一个元素
    public void addItem(Long itemId, BasketItem item) {
//      todo 如果存在了，就用新的item覆盖原来itemid对应的值，感觉不是很合理,c#这么做
        if (this.items.containsKey(itemId)) {
            this.items.replace(itemId, item);
        }
        this.items.put(itemId, item);
    }

    public void removeItem(Long itemId) {
        this.items.remove(itemId);
    }

    public void clear() {
        this.items.clear();
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return this.status;
    }

    public Map<Long, BasketItem> getItems() {
        return this.items;
    }

//  拼接所有iTem的内容
    @JsonIgnore
    public String getCartConent() {
        return String.join("\n", this.items.values().toString());
    }
}
