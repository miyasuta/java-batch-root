package customer.java_batch_v4.handlers;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.sap.cds.Result;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;

import cds.gen.catalogservice.CatalogService_;
import cds.gen.catalogservice.PostBatchV4Context;
import cds.gen.catalogservice.PostOrderV4Context;
import cds.gen.catalogservice.ReadOrderV4Context;
import io.vavr.control.Option;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpDestination;
import com.sap.cloud.sdk.datamodel.odatav4.core.BatchResponse;
import com.sap.cloud.sdk.datamodel.odatav4.core.CreateRequestBuilder;
import com.sap.cloud.sdk.datamodel.odatav4.core.ModificationResponse;
import com.mycompany.vdm.services.DefaultSalesServiceV4Service;
import com.mycompany.vdm.namespaces.salesservicev4.OrderItems;
import com.mycompany.vdm.namespaces.salesservicev4.SalesOrders;

@Component
@ServiceName(CatalogService_.CDS_NAME)
public class CatalogServiceHandler implements EventHandler {
	Logger logger = LoggerFactory.getLogger(CatalogServiceHandler.class);
	HttpDestination destination = DestinationAccessor.getDestination("salesorder-srv").asHttp();
	DefaultSalesServiceV4Service service = new DefaultSalesServiceV4Service().withServicePath("/odata/v4/sales");

	@On(event = ReadOrderV4Context.CDS_NAME)
	public void ReadOrderV4(ReadOrderV4Context context) {
		logger.info("ReadV4 handler called");

		// Get request
		final List<SalesOrders> salesorders = service.getAllSalesOrders()
				.select(SalesOrders.ID,
						SalesOrders.CUSTOMER,
						SalesOrders.ORDER_DATE,
						SalesOrders.TO_ITEMS)
				.execute(destination);
		logger.info(salesorders.toString());
		List<cds.gen.catalogservice.SalesOrders> readorders = salesorders.stream()
				.map(salesorder -> {
					cds.gen.catalogservice.SalesOrders readorder = cds.gen.catalogservice.SalesOrders.create();
					readorder.setId(salesorder.getID().toString());
					readorder.setCustomer(salesorder.getCustomer());
					readorder.setOrderDate(salesorder.getOrderDate());

					List<cds.gen.catalogservice.OrderItems> readitems = salesorder.getItemsIfPresent()
							.map(items -> items.stream() // List<OrderItems>をStreamに変換
									.map(item -> {
										cds.gen.catalogservice.OrderItems readItem = cds.gen.catalogservice.OrderItems
												.create();
										readItem.setId(item.getID().toString());
										readItem.setOrderId(item.getOrder_ID().toString());
										readItem.setProduct(item.getProduct());
										readItem.setQuantity(item.getQuantity());
										readItem.setPrice(item.getPrice());										
										return readItem;
									})
									.collect(Collectors.toList())) // 変換したStreamをListに収集
							.getOrElse(List.of());

					if (readitems.size() > 0) {
						readorder.setItems(readitems);
					}

					return readorder;
				})
				.collect(Collectors.toList());
				
		context.setResult(readorders);
	}

	@On(event = PostOrderV4Context.CDS_NAME)
	public void PostOrderv4(PostOrderV4Context context) {
		logger.info("PostOrderV4 handler called");

		// Post request
		SalesOrders salesorder = new SalesOrders();
		salesorder.setCustomer("Java");
		salesorder.setOrderDate(LocalDate.now());

		OrderItems items = new OrderItems();
		items.setProduct("Product A");
		items.setPrice(1000);
		items.setQuantity(1);
		salesorder.addItems(items);

		ModificationResponse<SalesOrders> response = service.createSalesOrders(salesorder).execute(destination);
		context.setResult(response.toString());
	}

	@On(event = PostBatchV4Context.CDS_NAME)
	public void PostBatchV4(PostBatchV4Context context) {
		logger.info("PostBatchV4 handler called");

		SalesOrders salesorder = new SalesOrders();
		salesorder.setCustomer("Java Batch");
		salesorder.setOrderDate(LocalDate.now());

		OrderItems items = new OrderItems();
		items.setProduct("Product A");
		items.setPrice(1000);
		items.setQuantity(1);
		salesorder.addItems(items);

		CreateRequestBuilder<SalesOrders> createRequest = service.createSalesOrders(salesorder);

		// batch call
		try (
				BatchResponse result = service
						.batch()
						.addChangeset(createRequest)
						.execute(destination);) {
			ModificationResponse<SalesOrders> createResult = result.getModificationResult(createRequest);
			context.setResult(createResult.toString());
		}

	}
}
