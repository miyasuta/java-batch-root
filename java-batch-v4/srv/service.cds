service CatalogService {   
    entity SalesOrders {
        key ID: UUID;
        orderDate: Date;
        customer: String(50);
        items: Composition of many OrderItems on items.order = $self;
    }

    entity OrderItems {
        key ID: UUID;
        order: Association to SalesOrders;
        product: String(50);
        quantity: Integer;
        price: Integer;
    }

    entity BatchResponse {
        code: Integer;
        ID: UUID;
    }

    function readOrderV4() returns array of SalesOrders;
    action postOrderV4(order: SalesOrders) returns SalesOrders;
    action postBatchV4(orders: array of SalesOrders) returns array of SalesOrders;
}