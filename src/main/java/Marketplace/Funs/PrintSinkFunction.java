package Marketplace.Funs;

import org.apache.flink.statefun.sdk.reqreply.generated.TypedValue;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;

public class PrintSinkFunction implements SinkFunction<TypedValue> {
    @Override
    public void invoke(TypedValue value, Context context) throws Exception {
        System.out.println("PrintSinkFunction: " + value);
    }
}
