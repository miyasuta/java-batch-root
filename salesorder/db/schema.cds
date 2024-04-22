using { cuid, managed } from '@sap/cds/common';
namespace sales;

entity SalesOrders: cuid, managed {
    orderDate: Date;
    customer: String(50);
    items: Composition of many OrderItems on items.order = $self;
}

entity OrderItems: cuid {
    order: Association to SalesOrders;
    product: String(50);
    quantity: Integer;
    price: Integer;
}