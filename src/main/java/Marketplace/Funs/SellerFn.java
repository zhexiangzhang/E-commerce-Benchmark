package Marketplace.Funs;

import Common.Entity.Product;
import Common.Entity.Seller;
import Marketplace.Constant.Enums;
import Marketplace.Constant.Enums.SendType;
import Marketplace.Constant.Constants;
import Marketplace.Types.MsgToProdFn.*;
import Marketplace.Types.MsgToSeller.*;
import Marketplace.Types.State.SellerAsyncState;
import Marketplace.Types.State.SellerState;
import org.apache.flink.statefun.sdk.java.*;
import org.apache.flink.statefun.sdk.java.message.Message;
import org.apache.flink.statefun.sdk.java.message.MessageBuilder;
import org.apache.flink.statefun.sdk.java.types.Type;
import org.apache.flink.statefun.sdk.java.types.Types;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;

public class SellerFn implements StatefulFunction {

    Logger logger = Logger.getLogger("SellerFn");

    static final TypeName TYPE = TypeName.typeNameOf(Constants.FUNS_NAMESPACE, "seller");

    static final ValueSpec<SellerState> SELLERSTATE = ValueSpec.named("sellerState").withCustomType(SellerState.TYPE);
    static final ValueSpec<SellerAsyncState> SELLERASYNCSTATE = ValueSpec.named("sellerAsyncState").withCustomType(SellerAsyncState.TYPE);
    //  Contains all the information needed to create a function instance
    public static final StatefulFunctionSpec SPEC = StatefulFunctionSpec.builder(TYPE)
            .withValueSpecs(SELLERSTATE, SELLERASYNCSTATE)
            .withSupplier(SellerFn::new)
            .build();

    private String getPartionText(String id) {
        return String.format("\n[ SellerFn partitionId %s ] \n", id);
    }

    @Override
    public CompletableFuture<Void> apply(Context context, Message message) throws Throwable {
        // client ---> seller (init seller type)
        if (message.is(InitSeller.TYPE)) {
            onInitSeller(context, message);
        }
        // client ---> seller (get seller type)
        else if (message.is(GetSeller.TYPE)) {
            onGetSeller(context);
        }
        // client ---> seller (get all products of seller)
        else if (message.is(GetProducts.TYPE)) {
            onGetProdAsyncBegin(context);
        }
        // client ---> seller ( delete product)
        else if (message.is(DeleteProduct.TYPE)) {
            onDeleteProdAsyncBegin(context, message);
        }
        // client ---> seller (update price)
        else if (message.is(UpdatePrice.TYPE)) {
            onUpdatePrice(context, message);
        }
        // client ---> seller (increase stock)
        else if (message.is(IncreaseStock.TYPE)) {
            onIncrStockAsyncBegin(context, message);
        }
        // product ---> seller (get the product info to check if it is still active)
        else if (message.is(IncreaseStockChkProd.TYPE)) {
            onIncrStockAsyncChkProd(context, message);
        }
        // client ---> seller ( add product)
        else if (message.is(AddProduct.TYPE)) {
            onAddProdAsyncBegin(context, message);
        }
        // stock / product ---> seller (the result of async task)
        else if (message.is(TaskFinish.TYPE)) {
            onAsyncTaskFinish(context, message);
        }
        // xxxxx ---> seller
        else if (message.is(Types.stringType())) {
            String result = message.as(Types.stringType());
            Long sellerId = Long.parseLong(context.self().id());
            logger.info(String.format("Seller ID: %s", sellerId, result));
        }

        return context.done();
    }

    private void showLog(String log) {
        logger.info(log);
//        System.out.println(log);
    }

    private SellerState getSellerState(Context context) {
        return context.storage().get(SELLERSTATE).orElse(new SellerState());
    }

    private SellerAsyncState getSellerAsyncState(Context context) {
        return context.storage().get(SELLERASYNCSTATE).orElse(new SellerAsyncState());
    }

    private void onGetProdAsyncBegin(Context context) throws InterruptedException {
        SellerState sellerState = getSellerState(context);
        SellerAsyncState sellerAsyncState = getSellerAsyncState(context);
//        给每个partition发送消息
        if (sellerAsyncState.isQueryProdTaskInProcess()) {
            String log = String.format(getPartionText(context.self().id())
                            + "get products task is in process, please wait....\n"
                    );
            showLog(log);
        } else {
            Long sellerId = sellerState.getSeller().getId();
            for (int i = 0; i < Constants.nProductPartitions; i++) {
                sendMessage(context,
                        ProductFn.TYPE,
                        String.valueOf(i),
                        GetAllProducts.TYPE,
                        new GetAllProducts(sellerId));
            }
            sellerAsyncState.setQueryProdTaskInProcess(true);
        }
    }

    private void onInitSeller(Context context, Message message) {
        InitSeller initSeller = message.as(InitSeller.TYPE);
        Seller seller = initSeller.getSeller();
        SellerState sellerState = getSellerState(context);
        sellerState.setSeller(seller);

        context.storage().set(SELLERSTATE, sellerState);

        String log = String.format(getPartionText(context.self().id())
                        + "init seller success\n"
                        + "sellerId: %s\n"
                , seller.getId());
        showLog(log);
    }

    private void onGetSeller(Context context) {
        SellerState sellerState = getSellerState(context);
        Seller seller = sellerState.getSeller();

        String log = String.format(getPartionText(context.self().id())
                        + "get seller success\n"
                        + seller.toString()
                        + "\n"
                );
        showLog(log);
    }

    private void onUpdatePrice(Context context, Message message) {
        UpdatePrice updatePrice = message.as(UpdatePrice.TYPE);
        long productId = updatePrice.getProduct_id();
        int prodFnPartitionID = (int) (productId % Constants.nProductPartitions);
        sendMessage(context,
                ProductFn.TYPE,
                String.valueOf(prodFnPartitionID),
                UpdatePrice.TYPE,
                updatePrice);
        }

    private void onIncrStockAsyncBegin(Context context, Message message) {
        IncreaseStock increaseStock = message.as(IncreaseStock.TYPE);
        long productId = increaseStock.getProductId();
        int prodFnPartitionID = (int) (productId % Constants.nProductPartitions);
//        sendGetProdMsgToProdFn(context, increaseStock, prodFnPartitionID);
        sendMessage(context,
                ProductFn.TYPE,
                String.valueOf(prodFnPartitionID),
                IncreaseStock.TYPE,
                increaseStock);
    }

    private void onIncrStockAsyncChkProd(Context context, Message message) {
        IncreaseStockChkProd increaseStockChkProd = message.as(IncreaseStockChkProd.TYPE);
        Product product = increaseStockChkProd.getProduct();
        long productId = product.getId();
        if(product.isActive()) {
            int stockFnPartitionID = (int) (productId % Constants.nStockPartitions);
//            sendIncrStockMsgToStockFn(context, increaseStockChkProd.getIncreaseStock(), stockFnPartitionID);
            sendMessage(context,
                    StockFn.TYPE,
                    String.valueOf(stockFnPartitionID),
                    IncreaseStock.TYPE,
                    increaseStockChkProd.getIncreaseStock());

        } else {
            String log = String.format(getPartionText(context.self().id())
                    + " increase stock fail as product is not active \n"
                    + "productId: %s\n"
                    , productId
            ) ;
            showLog(log);
        }
    }

    private void onDeleteProdAsyncBegin(Context context, Message message) {
        DeleteProduct deleteProduct = message.as(DeleteProduct.TYPE);
        long productId = deleteProduct.getProduct_id();
        String prodFnPartitionID = String.valueOf((int) (productId % Constants.nProductPartitions));
        String stockFnPartitionID = String.valueOf((int) (productId % Constants.nStockPartitions));
        sendMessage(context, ProductFn.TYPE, prodFnPartitionID, DeleteProduct.TYPE, deleteProduct);
        sendMessage(context, StockFn.TYPE, stockFnPartitionID, DeleteProduct.TYPE, deleteProduct);
        saveDeleteProdAsyncTask(context, productId);
    }

    private void onAddProdAsyncBegin(Context context, Message message) {
        AddProduct addProduct = message.as(AddProduct.TYPE);
        long productId = addProduct.getProduct().getId();
        String prodFnPartitionID = String.valueOf((int) (productId % Constants.nProductPartitions)) ;
        String stockFnPartitionID =String.valueOf((int) (productId % Constants.nStockPartitions));
        sendMessage(context, ProductFn.TYPE, prodFnPartitionID, AddProduct.TYPE, addProduct);
        sendMessage(context, StockFn.TYPE, stockFnPartitionID, AddProduct.TYPE, addProduct);
        // TODO: 6/4/2023  确认两个消息都发送成功了才算成功
        // TODO: 6/4/2023
        saveAddProdAsyncTask(context, productId);
    }

    private void onAsyncTaskFinish(Context context, Message message) {
        TaskFinish taskFinish = message.as(TaskFinish.TYPE);
        Long taskId = taskFinish.getProductId();
        Enums.SendType sendType = taskFinish.getSenderType();
        switch (taskFinish.getTaskType()) {
            case AddProductType:
                caseAddProd(context, taskId, sendType);
                break;
            case UpdatePriceType:
                caseUpdatePrice(context, taskId, sendType);
                break;
            case GetAllProductsType:
                Product[] products = taskFinish.getProductsOfSeller();
                caseGetAllProducts(context, products);
                break;
            case DeleteProductType:
                caseDeleteProd(context, taskId, sendType);
                break;
            default:
                // 默认操作
                break;
        }

    }

    private void caseAddProd(Context context, Long productId, Enums.SendType sendType) {
        SellerAsyncState sellerAsyncState = getSellerAsyncState(context);
        if (sellerAsyncState.checkAddProdTask(productId, sendType)) {

            String log = String.format(getPartionText(context.self().id())
                            + "add product success\n"
                            + "productId: %s\n"
                    , productId
            );
            showLog(log);
        }
        context.storage().set(SELLERASYNCSTATE, sellerAsyncState);
    }

    private void caseDeleteProd(Context context, Long productId, Enums.SendType sendType) {
        SellerAsyncState sellerAsyncState = getSellerAsyncState(context);
        if (sellerAsyncState.checkDeleteProdTask(productId, sendType)) {
            String log = String.format(getPartionText(context.self().id())
                            + "delete product success\n"
                            + "productId: %s\n"
                    , productId
            );
            showLog(log);
        }
        context.storage().set(SELLERASYNCSTATE, sellerAsyncState);
    }

    private void caseUpdatePrice(Context context, Long productId, Enums.SendType sendType) {
        String log = String.format(getPartionText(context.self().id())
                        + "update price success\n"
                        + "productId: %s\n"
                , productId
        );
        showLog(log);
    }

    private void caseGetAllProducts(Context context, Product[] products) {
//        System.out.println("receive products: " + Arrays.toString(products));
        SellerAsyncState sellerAsyncState = getSellerAsyncState(context);
        sellerAsyncState.setQueryProdTaskCnt(sellerAsyncState.getQueryProdTaskCnt() + 1);

        sellerAsyncState.addQueryProdTask(products);
//        System.out.println("sellerAsyncState.getQueryProdTaskCnt(): " + sellerAsyncState.getQueryProdTaskCnt());
        if (sellerAsyncState.checkQueryProdTask()) {
            Product[] productList = sellerAsyncState.getQueryProdTaskRes();
//            每个product用回车分隔
            String productListStr = Arrays.stream(productList).map(Product::toString).collect(Collectors.joining("\n"));
            String log = String.format(getPartionText(context.self().id())
                            + "get all products success\n"
                            + "products: \n%s\n"
                    , productListStr
            );
            showLog(log);
            sellerAsyncState.setQueryProdTaskInProcess(false);
            sellerAsyncState.clearQueryProdTask();
            sellerAsyncState.setQueryProdTaskCnt(0);
        }
        context.storage().set(SELLERASYNCSTATE, sellerAsyncState);
    }

    private <T> void sendMessage(Context context, TypeName addressType, String addressId, Type<T> messageType, T messageContent) {
        Message msg = MessageBuilder.forAddress(addressType, addressId)
                .withCustomType(messageType, messageContent)
                .build();
        context.send(msg);
    }

    private void saveAddProdAsyncTask(Context context, long productId) {
        SellerAsyncState sellerAsyncState = getSellerAsyncState(context);
        Map<Long, SendType> addProdTaskList = sellerAsyncState.getAddProdTaskList();
        addProdTaskList.put(productId, SendType.None);
        context.storage().set(SELLERASYNCSTATE, sellerAsyncState);
    }

    private void saveDeleteProdAsyncTask(Context context, long productId) {
        SellerAsyncState sellerAsyncState = getSellerAsyncState(context);
        Map<Long, SendType> deleteProdTaskList = sellerAsyncState.getDeleteProdTaskList();
        deleteProdTaskList.put(productId, SendType.None);
        context.storage().set(SELLERASYNCSTATE, sellerAsyncState);
    }
}

//# 关于异步的处理： 除了动态buffer，还可以为每一种异步请求新建一个handler，然后在seller这里产生唯一的id，然后在handler里面定义状态变量
