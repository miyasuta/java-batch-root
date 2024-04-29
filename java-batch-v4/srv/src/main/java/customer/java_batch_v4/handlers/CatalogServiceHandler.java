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
	// HttpDestination destination = DestinationAccessor.getDestination("salesorder-srv").asHttp();
	// DefaultSalesServiceV4Service service = new DefaultSalesServiceV4Service().withServicePath("/odata/v4/sales");

	@On(event = ReadOrderV4Context.CDS_NAME)
	public void ReadOrderV4(ReadOrderV4Context context) {
		logger.info("ReadV4 handler called");
		HttpDestination destination = DestinationAccessor.getDestination("salesorder-srv").asHttp();
		DefaultSalesServiceV4Service service = new DefaultSalesServiceV4Service().withServicePath("/odata/v4/sales");		

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
										cds.gen.catalogservice.OrderItems readitem = cds.gen.catalogservice.OrderItems
												.create();
										readitem.setId(item.getID().toString());
										readitem.setOrderId(item.getOrder_ID().toString());
										readitem.setProduct(item.getProduct());
										readitem.setQuantity(item.getQuantity());
										readitem.setPrice(item.getPrice());
										return readitem;
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
		HttpDestination destination = DestinationAccessor.getDestination("salesorder-srv").asHttp();
		DefaultSalesServiceV4Service service = new DefaultSalesServiceV4Service().withServicePath("/odata/v4/sales");

		logger.info("PostOrderV4 handler called");

		// map request to salesorder
		SalesOrders salesorder = new SalesOrders();
		salesorder.setCustomer(context.getOrder().getCustomer());
		salesorder.setOrderDate(context.getOrder().getOrderDate());

		//itemが登録されていない-> Node.js側でリクエストデータを見てみる
		context.getOrder().getItems().stream()
						.map(item -> {
							OrderItems newitem = new OrderItems();
							newitem.setProduct(item.getProduct());
							newitem.setQuantity(item.getQuantity());
							newitem.setPrice(item.getPrice());
							salesorder.addItems(newitem);
							return newitem;
						});

		// post salesorder
		ModificationResponse<SalesOrders> response = service.createSalesOrders(salesorder).execute(destination);

		// map response
		cds.gen.catalogservice.SalesOrders createdorder = cds.gen.catalogservice.SalesOrders.create();
		createdorder.setId(response.getModifiedEntity().getID().toString());
		createdorder.setCustomer(response.getModifiedEntity().getCustomer());
		createdorder.setOrderDate(response.getModifiedEntity().getOrderDate());

		List<cds.gen.catalogservice.OrderItems> createditems = response.getModifiedEntity().getItemsIfPresent()
		.map(items -> items.stream() // List<OrderItems>をStreamに変換
				.map(item -> {
					cds.gen.catalogservice.OrderItems createditem = cds.gen.catalogservice.OrderItems
							.create();
							createditem.setId(item.getID().toString());
							createditem.setOrderId(item.getOrder_ID().toString());
							createditem.setProduct(item.getProduct());
							createditem.setQuantity(item.getQuantity());
							createditem.setPrice(item.getPrice());
					return createditem;
				})
				.collect(Collectors.toList())) // 変換したStreamをListに収集
		.getOrElse(List.of());

		if (createditems.size() > 0) {
			createdorder.setItems(createditems);
		}
		context.setResult(createdorder);
	}

	@On(event = PostBatchV4Context.CDS_NAME)
	public void PostBatchV4(PostBatchV4Context context) {
		logger.info("PostBatchV4 handler called");
	HttpDestination destination = DestinationAccessor.getDestination("salesorder-srv").asHttp();
	DefaultSalesServiceV4Service service = new DefaultSalesServiceV4Service().withServicePath("/odata/v4/sales");		

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
