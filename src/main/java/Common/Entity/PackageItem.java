package Common.Entity;

import Marketplace.Constant.Enums;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PackageItem {
    @JsonProperty("packageId") private long packageId;
    @JsonProperty("shipmentId") private long shipmentId;

    // FK
    // product identification
//    @JsonProperty("sellerId")  public long sellerId;
    @JsonProperty("productId") public long productId;
    @JsonProperty("quantity") public int quantity;
    @JsonProperty("packageStatus") public Enums.PackageStatus packageStatus;

    @JsonCreator
    public PackageItem(
            @JsonProperty("packageId") int packageId,
            @JsonProperty("shipmentId") long shipmentId,
//            @JsonProperty("sellerId") long sellerId,
            @JsonProperty("productId") long productId,
            @JsonProperty("quantity") int quantity,
            @JsonProperty("packageStatus") Enums.PackageStatus packageStatus) {
        this.packageId = packageId;
        this.shipmentId = shipmentId;
//        this.sellerId = sellerId;
        this.productId = productId;
        this.quantity = quantity;
        this.packageStatus = packageStatus;
    }

}
