package Marketplace.Constant;

import org.omg.CORBA.UNKNOWN;

public class Enums {
    public enum TaskType
    {
        //
        AddProductType,
        UpdatePriceType,
        GetAllProductsType,
        DeleteProductType,
        // OrderFn
        AttemptReservationsType,
        ConfirmReservationsType,
        CancelReservationsType
    };

    public enum SendType
    {
        ProductFn,
        StockFn,
        None
    };

    public enum ItemStatus
    {
        DELETED, // deleted from DB
        OUT_OF_STOCK, //
        PRICE_DIVERGENCE,
        IN_STOCK,
        // Strictly speaking it is not item status, but used as a placeholder.
        UNKNOWN
    };
}
