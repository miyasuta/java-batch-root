package customer.java_batch_v4.handlers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;

import cds.gen.catalogservice.CatalogService_;
import cds.gen.catalogservice.PostBatchV4Context;
import cds.gen.catalogservice.PostOrderV4Context;
import cds.gen.catalogservice.ReadOrderV4Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpDestination;
import com.sap.cloud.sdk.datamodel.odatav4.core.BatchResponse;
import com.sap.cloud.sdk.datamodel.odatav4.core.CreateRequestBuilder;
import com.sap.cloud.sdk.datamodel.odatav4.core.ModificationRequestBuilder;
import com.sap.cloud.sdk.datamodel.odatav4.core.ModificationResponse;
import com.mycompany.vdm.services.DefaultSalesServiceV4Service;
import com.mycompany.vdm.namespaces.salesservicev4.OrderItems;
import com.mycompany.vdm.namespaces.salesservicev4.SalesOrders;

@Component
@ServiceName(CatalogService_.CDS_NAME)
public class CatalogServiceHandler implements EventHandler {
	Logger logger = LoggerFactory.getLogger(CatalogServiceHandler.class);

	@On(event = ReadOrderV4Context.CDS_NAME)
	public void ReadOrderV4(ReadOrderV4Context context) {
		logger.info("ReadV4 handler called");
		HttpDestination destination = DestinationAccessor.getDestination("salesorder-srv").asHttp();
		DefaultSalesServiceV4Service service = new DefaultSalesServiceV4Service().withServicePath("/odata/v4/sales");

		// get request
		final List<SalesOrders> salesOrdersResp = service.getAllSalesOrders()
				.select(SalesOrders.ID,
						SalesOrders.CUSTOMER,
						SalesOrders.ORDER_DATE,
						SalesOrders.TO_ITEMS)
				.execute(destination);

		// msp response
		List<cds.gen.catalogservice.SalesOrders> salesOrdersOut = salesOrdersResp.stream()
				.map(salesOrderResp -> {
					cds.gen.catalogservice.SalesOrders salesOrderOut = cds.gen.catalogservice.SalesOrders.create();
					salesOrderOut.setId(salesOrderResp.getID().toString());
					salesOrderOut.setCustomer(salesOrderResp.getCustomer());
					salesOrderOut.setOrderDate(salesOrderResp.getOrderDate());

					List<cds.gen.catalogservice.OrderItems> itemsOut = salesOrderResp.getItemsIfPresent()
							.map(itemsResp -> itemsResp.stream()
									.map(itemResp -> {
										cds.gen.catalogservice.OrderItems itemOut = cds.gen.catalogservice.OrderItems
												.create();
										itemOut.setId(itemResp.getID().toString());
										itemOut.setOrderId(itemResp.getOrder_ID().toString());
										itemOut.setProduct(itemResp.getProduct());
										itemOut.setQuantity(itemResp.getQuantity());
										itemOut.setPrice(itemResp.getPrice());
										return itemOut;
									})
									.collect(Collectors.toList()))
							.getOrElse(List.of());

					if (itemsOut.size() > 0) {
						salesOrderOut.setItems(itemsOut);
					}

					return salesOrderOut;
				})
				.collect(Collectors.toList());

		context.setResult(salesOrdersOut);
	}

	@On(event = PostOrderV4Context.CDS_NAME)
	public void PostOrderv4(PostOrderV4Context context) {
		logger.info("PostOrderV4 handler called");
		HttpDestination destination = DestinationAccessor.getDestination("salesorder-srv").asHttp();
		DefaultSalesServiceV4Service service = new DefaultSalesServiceV4Service().withServicePath("/odata/v4/sales");

		// map request to salesorder
		SalesOrders salesOrderReq = new SalesOrders();
		salesOrderReq.setCustomer(context.getOrder().getCustomer());
		salesOrderReq.setOrderDate(context.getOrder().getOrderDate());

		context.getOrder().getItems().forEach(item -> {
			OrderItems itemReq = new OrderItems();
			itemReq.setProduct(item.getProduct());
			itemReq.setQuantity(item.getQuantity());
			itemReq.setPrice(item.getPrice());
			salesOrderReq.addItems(itemReq);
		});

		// post salesorder
		ModificationResponse<SalesOrders> response = service.createSalesOrders(salesOrderReq).execute(destination);

		// map response
		cds.gen.catalogservice.SalesOrders salesOrderOut = cds.gen.catalogservice.SalesOrders.create();
		salesOrderOut.setId(response.getModifiedEntity().getID().toString());
		salesOrderOut.setCustomer(response.getModifiedEntity().getCustomer());
		salesOrderOut.setOrderDate(response.getModifiedEntity().getOrderDate());

		List<cds.gen.catalogservice.OrderItems> itemsOut = response.getModifiedEntity().getItemsIfPresent()
				.map(itemsResp -> itemsResp.stream()
						.map(itemResp -> {
							cds.gen.catalogservice.OrderItems itemOut = cds.gen.catalogservice.OrderItems
									.create();
							itemOut.setId(itemResp.getID().toString());
							itemOut.setOrderId(itemResp.getOrder_ID().toString());
							itemOut.setProduct(itemResp.getProduct());
							itemOut.setQuantity(itemResp.getQuantity());
							itemOut.setPrice(itemResp.getPrice());
							return itemOut;
						})
						.collect(Collectors.toList()))
				.getOrElse(List.of());

		if (itemsOut.size() > 0) {
			salesOrderOut.setItems(itemsOut);
		}
		context.setResult(salesOrderOut);
	}

	@On(event = PostBatchV4Context.CDS_NAME)
	public void PostBatchV4(PostBatchV4Context context) {
		logger.info("PostBatchV4 handler called");
		HttpDestination destination = DestinationAccessor.getDestination("salesorder-srv").asHttp();
		DefaultSalesServiceV4Service service = new DefaultSalesServiceV4Service().withServicePath("/odata/v4/sales");

		// create request
		List<CreateRequestBuilder<SalesOrders>> createReusests = context.getOrders().stream()
				.map(salesOrderIn -> {
					SalesOrders salesOrderReq = new SalesOrders();
					salesOrderReq.setCustomer(salesOrderIn.getCustomer());
					salesOrderReq.setOrderDate(salesOrderIn.getOrderDate());

					salesOrderIn.getItems().stream()
							.map(itemIn -> {
								OrderItems itemReq = new OrderItems();
								itemReq.setProduct(itemIn.getProduct());
								itemReq.setPrice(itemIn.getPrice());
								itemReq.setQuantity(itemIn.getQuantity());
								return itemReq;
							}).forEach(salesOrderReq::addItems);
					return service.createSalesOrders(salesOrderReq);
				}).collect(Collectors.toList());

		ModificationRequestBuilder<?>[] requestArray = new ModificationRequestBuilder<?>[createReusests.size()];
		createReusests.toArray(requestArray);

		// batch call
		try (
				BatchResponse result = service
						.batch()
						.addChangeset(requestArray)
						.execute(destination);) {

			// map response
			List<cds.gen.catalogservice.SalesOrders> salesOrdersOut = createReusests.stream().map(request -> {
				cds.gen.catalogservice.SalesOrders salesOrderOut = cds.gen.catalogservice.SalesOrders.create();
				SalesOrders salesOrderResp = result.getModificationResult(request).getModifiedEntity();
				salesOrderOut.setId(salesOrderResp.getID().toString());
				salesOrderOut.setCustomer(salesOrderResp.getCustomer());
				salesOrderOut.setOrderDate(salesOrderResp.getOrderDate());

				List<cds.gen.catalogservice.OrderItems> itemsOut = salesOrderResp.getItemsIfPresent()
						.map(itemsResp -> itemsResp.stream()
								.map(itemResp -> {
									cds.gen.catalogservice.OrderItems itemOut = cds.gen.catalogservice.OrderItems
											.create();
											itemOut.setId(itemResp.getID().toString());
											itemOut.setOrderId(itemResp.getOrder_ID().toString());
											itemOut.setProduct(itemResp.getProduct());
											itemOut.setQuantity(itemResp.getQuantity());
											itemOut.setPrice(itemResp.getPrice());
									return itemOut;
								})
								.collect(Collectors.toList()))
						.getOrElse(List.of());

				if (itemsOut.size() > 0) {
					salesOrderOut.setItems(itemsOut);
				}
				return salesOrderOut;
			}).collect(Collectors.toList());

			context.setResult(salesOrdersOut);
		}

	}
}
