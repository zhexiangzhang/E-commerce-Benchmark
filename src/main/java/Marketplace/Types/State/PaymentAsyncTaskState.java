package Marketplace.Types.State;

import Common.Entity.Invoice;
import Common.Entity.Order;
import Marketplace.Constant.Constants;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.statefun.sdk.java.TypeName;
import org.apache.flink.statefun.sdk.java.types.SimpleType;
import org.apache.flink.statefun.sdk.java.types.Type;

import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
public class PaymentAsyncTaskState {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static final Type<PaymentAsyncTaskState> TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "paymentAsyncTaskState"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, PaymentAsyncTaskState.class));

    @JsonProperty("TaskList")
//  how many partitions have not finish
    Map<Long, Integer> TaskList = new HashMap<>();

    @JsonProperty("TmpInvoiceInfo")
    Map<Long, Invoice> TmpInvoiceInfo = new HashMap<>();

    @JsonIgnore
    public void addInvoice(long orderId, Invoice invoice) {
        TmpInvoiceInfo.put(orderId, invoice);
    }

    @JsonIgnore
    public Invoice getInvoice(long orderId) {
        return TmpInvoiceInfo.get(orderId);
    }

    @JsonIgnore
    public void removeSingleInvoice(long orderId) {
        TmpInvoiceInfo.remove(orderId);
    }

    @JsonIgnore
    public void addNewTask(long orderId, int taskNum) {
        TaskList.put(orderId, taskNum);
    }

    @JsonIgnore
    public boolean isTaskComplete(long orderId) {
        return TaskList.get(orderId) == 0;
    }

    @JsonIgnore
    public void removeTask(long orderId) {
        TaskList.remove(orderId);
    }

    @JsonIgnore
    public void addCompletedSubTask(long orderId) {
        TaskList.put(orderId, TaskList.get(orderId) - 1);
    }

    @JsonIgnore
    public boolean isAllSubTaskCompleted(long orderId) {
        return TaskList.get(orderId) == 0;
    }

}
