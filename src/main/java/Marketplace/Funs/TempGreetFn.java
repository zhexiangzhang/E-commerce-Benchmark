package Marketplace.Funs;

import static Marketplace.Types.Messages.USER_PROFILE_JSON_TYPE;
import static Marketplace.Types.Messages.EGRESS_RECORD_JSON_TYPE;

import Marketplace.Constant.Constants;
import Marketplace.Types.EgressRecord;
import Marketplace.Types.Entity.TmpUserPofile;

import Marketplace.Types.Messages;
import org.apache.flink.statefun.sdk.java.*;
import org.apache.flink.statefun.sdk.java.message.EgressMessage;
import org.apache.flink.statefun.sdk.java.message.EgressMessageBuilder;
import org.apache.flink.statefun.sdk.java.message.Message;


import java.util.concurrent.CompletableFuture;

public class TempGreetFn implements StatefulFunction {
    //  注册函数，逻辑名称=<namespace>+<name>
//    static final TypeName TYPE = TypeName.typeNameFromString("e-commerce.fns/greet");
    static final TypeName TYPE = TypeName.typeNameOf(Constants.FUNS_NAMESPACE, "greet");

    //  包含了创建函数实例所需的所有信息
    public static final StatefulFunctionSpec SPEC = StatefulFunctionSpec.builder(TYPE)
            .withSupplier(TempGreetFn::new)  // 如果是两个，就是.withValueSpecs(SEEN, SEEN2)
            .build();

    private static final TypeName ECOMMERCE_EGRESS = TypeName.typeNameOf(Constants.EGRESS_NAMESPACE, "egress");

    @Override
    public CompletableFuture<Void> apply(Context context, Message message) {
        if (message.is(USER_PROFILE_JSON_TYPE)) {
            final TmpUserPofile userPofile = message.as(USER_PROFILE_JSON_TYPE);
            final String greetings = String.format("Hello, %s!, cnt %s", userPofile.getUsername(), userPofile.getLoginCnt());
            final EgressRecord egressRecord = new EgressRecord("greeting", greetings);

//            向一个叫做 ECOMMERCE_EGRESS 的出口（Egress）发送一个消息
            context.send(
                    EgressMessageBuilder.forEgress(ECOMMERCE_EGRESS)  // 创建一个新的 EgressMessageBuilder 对象，它的目标出口是 ECOMMERCE_EGRESS
                            .withCustomType(EGRESS_RECORD_JSON_TYPE, egressRecord) // EGRESS_RECORD_JSON_TYPE 是消息的类型，egressRecord 是消息的内容。
                            .build());
        }
        return context.done();
    }
}
