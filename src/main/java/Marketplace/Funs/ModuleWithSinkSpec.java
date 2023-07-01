package Marketplace.Funs;

import java.util.Map;
import org.apache.flink.statefun.flink.io.datastream.SinkFunctionSpec;
import org.apache.flink.statefun.sdk.io.EgressIdentifier;
import org.apache.flink.statefun.sdk.io.EgressSpec;
import org.apache.flink.statefun.sdk.reqreply.generated.TypedValue;
import org.apache.flink.statefun.sdk.spi.StatefulFunctionModule;

public class ModuleWithSinkSpec implements StatefulFunctionModule {

    @Override
    public void configure(Map<String, String> globalConfiguration, Binder binder) {
        System.out.println("StatefulFunctionModule");
        EgressIdentifier<TypedValue> id = new EgressIdentifier<>("e-commerce.fns", "custom-sink", TypedValue.class);
        EgressSpec<TypedValue> spec = new SinkFunctionSpec<>(id, new PrintSinkFunction()
        );
        binder.bindEgress(spec);
    }
}