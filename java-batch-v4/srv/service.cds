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

    function readOrderV4() returns array of SalesOrders;
    // function readOrderV4() returns String;
    action postOrderV4() returns String;
    action postBatchV4() returns String;
}