package customer.java_batch.handlers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;

import cds.gen.catalogservice.CatalogService_;
import cds.gen.catalogservice.PostBatchV2Context;
import cds.gen.catalogservice.PostOrderV2Context;
import cds.gen.catalogservice.ReadOrderV2Context;
import io.vavr.control.Try;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpDestination;
import com.sap.cloud.sdk.datamodel.odata.helper.ModificationResponse;
import com.sap.cloud.sdk.datamodel.odata.helper.VdmEntity;
import com.sap.cloud.sdk.datamodel.odata.helper.batch.BatchResponse;
import com.sap.cloud.sdk.datamodel.odata.helper.batch.BatchResponseChangeSet;
import com.mycompany.vdm.services.SalesServiceV2Service;
import com.mycompany.vdm.namespaces.salesservicev2.SalesOrdersCreateFluentHelper;
import com.mycompany.vdm.namespaces.salesservicev2.batch.DefaultSalesServiceV2ServiceBatch;
import com.mycompany.vdm.namespaces.salesservicev2.batch.SalesServiceV2ServiceBatch;
import com.mycompany.vdm.services.DefaultSalesServiceV2Service;
import com.mycompany.vdm.namespaces.salesservicev2.OrderItems;
import com.mycompany.vdm.namespaces.salesservicev2.SalesOrders;

@Component
@ServiceName(CatalogService_.CDS_NAME)
public class CatalogServiceHandler implements EventHandler {

	Logger logger = LoggerFactory.getLogger(CatalogServiceHandler.class);

	@On(event = ReadOrderV2Context.CDS_NAME)
	public void ReadOrderV2(ReadOrderV2Context context) {
		logger.info("Readv2 handler called");

		HttpDestination destination = DestinationAccessor.getDestination("salesorder-srv").asHttp();
		DefaultSalesServiceV2Service service = new DefaultSalesServiceV2Service().withServicePath("/odata/v2/sales");

		// Get request
		final List<SalesOrders> salesorders = service.getAllSalesOrders()
				.select(SalesOrders.ID,
						SalesOrders.CUSTOMER,
						SalesOrders.ORDER_DATE,
						SalesOrders.TO_ITEMS)
				.executeRequest(destination);
		// logger.info(salesorders.toString());
		// context.setResult(salesorders.toString());
		// msp response
		List<cds.gen.catalogservice.SalesOrders> readorders = salesorders.stream()
				.map(salesorder -> {
					cds.gen.catalogservice.SalesOrders readorder = cds.gen.catalogservice.SalesOrders.create();
					readorder.setId(salesorder.getID().toString());
					readorder.setCustomer(salesorder.getCustomer());
					readorder.setOrderDate(salesorder.getOrderDate().toLocalDate());

					List<cds.gen.catalogservice.OrderItems> readitems = salesorder.getItemsIfPresent()
							.map(items -> items.stream()
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
									.collect(Collectors.toList()))
							.getOrElse(List.of());

					if (readitems.size() > 0) {
						readorder.setItems(readitems);
					}
					return readorder;
				})
				.collect(Collectors.toList());

		context.setResult(readorders);
	}

	@On(event = PostOrderV2Context.CDS_NAME)
	public void PostOrderv2(PostOrderV2Context context) {
		logger.info("PostOrderv2 handler called");

		HttpDestination destination = DestinationAccessor.getDestination("salesorder-srv").asHttp();
		DefaultSalesServiceV2Service service = new DefaultSalesServiceV2Service().withServicePath("/odata/v2/sales");

		// map request to salesorder
		SalesOrders salesorder = new SalesOrders();
		salesorder.setCustomer(context.getOrder().getCustomer());
		salesorder.setOrderDate(context.getOrder().getOrderDate().atStartOfDay());

		context.getOrder().getItems().forEach(item -> {
			OrderItems newitem = new OrderItems();
			newitem.setProduct(item.getProduct());
			newitem.setQuantity(item.getQuantity());
			newitem.setPrice(item.getPrice());
			salesorder.addItems(newitem);
		});

		// //Post request
		// SalesOrders salesorder = new SalesOrders();
		// salesorder.setCustomer("Java");
		// salesorder.setOrderDate(LocalDateTime.now());

		// OrderItems items = new OrderItems();
		// items.setProduct("Product A");
		// items.setPrice(1000);
		// items.setQuantity(1);
		// salesorder.addItems(items);

		ModificationResponse<SalesOrders> response = service.createSalesOrders(salesorder).executeRequest(destination);
		// map response
		cds.gen.catalogservice.SalesOrders createdorder = cds.gen.catalogservice.SalesOrders.create();
		createdorder.setId(response.getModifiedEntity().getID().toString());
		createdorder.setCustomer(response.getModifiedEntity().getCustomer());
		createdorder.setOrderDate(response.getModifiedEntity().getOrderDate().toLocalDate());

		List<cds.gen.catalogservice.OrderItems> createditems = response.getModifiedEntity().getItemsIfPresent()
				.map(items -> items.stream()
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
						.collect(Collectors.toList()))
				.getOrElse(List.of());

		if (createditems.size() > 0) {
			createdorder.setItems(createditems);
		}
		context.setResult(createdorder);
		// context.setResult(response.toString());
	}

	@On(event = PostBatchV2Context.CDS_NAME)
	public void PostBatchV2(PostBatchV2Context context) {
		logger.info("PostBatchv2 handler called");

		HttpDestination destination = DestinationAccessor.getDestination("salesorder-srv").asHttp();
		DefaultSalesServiceV2Service service = new DefaultSalesServiceV2Service().withServicePath("/odata/v2/sales");

		// create batch request
		SalesServiceV2ServiceBatch batchrequest = service.batch();
		Collection<cds.gen.catalogservice.SalesOrders> salesordersIn = context.getOrders();
		for (cds.gen.catalogservice.SalesOrders salesorderIn : salesordersIn) {
			// map request to salesorder
			SalesOrders salesorder = new SalesOrders();
			salesorder.setCustomer(salesorderIn.getCustomer());
			salesorder.setOrderDate(salesorderIn.getOrderDate().atStartOfDay());

			salesorderIn.getItems().forEach(item -> {
				OrderItems newitem = new OrderItems();
				newitem.setProduct(item.getProduct());
				newitem.setQuantity(item.getQuantity());
				newitem.setPrice(item.getPrice());
				salesorder.addItems(newitem);
			});

			batchrequest = batchrequest.beginChangeSet()
					.createSalesOrders(salesorder)
					.endChangeSet();
		}

		// SalesOrders salesorder = new SalesOrders();
		// salesorder.setCustomer("Java");
		// salesorder.setOrderDate(LocalDateTime.now());

		// OrderItems items = new OrderItems();
		// items.setProduct("Product A");
		// items.setPrice(1000);
		// items.setQuantity(1);
		// salesorder.addItems(items);

		// batch call
		BatchResponse result = batchrequest.executeRequest(destination);

		int index = 0;
		Collection<cds.gen.catalogservice.SalesOrders> salesordersresult = new ArrayList<>();

		for (cds.gen.catalogservice.SalesOrders salesorderIn : salesordersIn) {
			Try<BatchResponseChangeSet> changeset = result.get(index);
			index++; // increment index for next loop
			if (changeset.isSuccess()) {
				List<VdmEntity<?>> createdorders = changeset.get().getCreatedEntities();
				List<cds.gen.catalogservice.SalesOrders> salesOrdersList = createdorders.stream()
						.map(entity -> {
							logger.info(entity.toString());
							cds.gen.catalogservice.SalesOrders salesorder = cds.gen.catalogservice.SalesOrders.create();
							if (entity instanceof SalesOrders) {
								SalesOrders createdorder = (SalesOrders) entity;
								salesorder.setId(createdorder.getID().toString());
								salesorder.setCustomer(createdorder.getCustomer());
								salesorder.setOrderDate(createdorder.getOrderDate().toLocalDate());

								List<cds.gen.catalogservice.OrderItems> createditems = createdorder.getItemsIfPresent()
										.map(items -> items.stream()
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
												.collect(Collectors.toList()))
										.getOrElse(List.of());

								if (createditems.size() > 0) {
									salesorder.setItems(createditems);
								}
							}
							return salesorder;

						})
						.collect(Collectors.toList());
				salesordersresult.addAll(salesOrdersList);
			}
		}
		context.setResult(salesordersresult);
	}

}