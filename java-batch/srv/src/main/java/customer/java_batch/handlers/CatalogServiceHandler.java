package customer.java_batch.handlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sap.cds.services.handler.EventHandler;
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
		final List<SalesOrders> salesordersResp = service.getAllSalesOrders()
				.select(SalesOrders.ID,
						SalesOrders.CUSTOMER,
						SalesOrders.ORDER_DATE,
						SalesOrders.TO_ITEMS)
				.executeRequest(destination);

		// map response
		List<cds.gen.catalogservice.SalesOrders> salesOrdersOut = salesordersResp.stream()
				.map(salesorderResp -> {
					cds.gen.catalogservice.SalesOrders salesOrderOut = cds.gen.catalogservice.SalesOrders.create();
					salesOrderOut.setId(salesorderResp.getID().toString());
					salesOrderOut.setCustomer(salesorderResp.getCustomer());
					salesOrderOut.setOrderDate(salesorderResp.getOrderDate().toLocalDate());

					List<cds.gen.catalogservice.OrderItems> itemsOut = salesorderResp.getItemsIfPresent()
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

	@On(event = PostOrderV2Context.CDS_NAME)
	public void PostOrderv2(PostOrderV2Context context) {
		logger.info("PostOrderv2 handler called");

		HttpDestination destination = DestinationAccessor.getDestination("salesorder-srv").asHttp();
		DefaultSalesServiceV2Service service = new DefaultSalesServiceV2Service().withServicePath("/odata/v2/sales");

		// map request to salesorder
		SalesOrders salesOrderReq = new SalesOrders();
		salesOrderReq.setCustomer(context.getOrder().getCustomer());
		salesOrderReq.setOrderDate(context.getOrder().getOrderDate().atStartOfDay());

		context.getOrder().getItems().forEach(itemIn -> {
			OrderItems itemReq = new OrderItems();
			itemReq.setProduct(itemIn.getProduct());
			itemReq.setQuantity(itemIn.getQuantity());
			itemReq.setPrice(itemIn.getPrice());
			salesOrderReq.addItems(itemReq);
		});

		ModificationResponse<SalesOrders> response = service.createSalesOrders(salesOrderReq)
				.executeRequest(destination);
		// map response
		cds.gen.catalogservice.SalesOrders salesOrderOut = cds.gen.catalogservice.SalesOrders.create();
		salesOrderOut.setId(response.getModifiedEntity().getID().toString());
		salesOrderOut.setCustomer(response.getModifiedEntity().getCustomer());
		salesOrderOut.setOrderDate(response.getModifiedEntity().getOrderDate().toLocalDate());

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

	@On(event = PostBatchV2Context.CDS_NAME)
	public void PostBatchV2(PostBatchV2Context context) {
		logger.info("PostBatchv2 handler called");

		HttpDestination destination = DestinationAccessor.getDestination("salesorder-srv").asHttp();
		DefaultSalesServiceV2Service service = new DefaultSalesServiceV2Service().withServicePath("/odata/v2/sales");

		// create batch request
		SalesServiceV2ServiceBatch batchrequest = service.batch();
		Collection<cds.gen.catalogservice.SalesOrders> salesOrdersIn = context.getOrders();
		for (cds.gen.catalogservice.SalesOrders salesOrderIn : salesOrdersIn) {
			// map request to salesorder
			SalesOrders salesOrderReq = new SalesOrders();
			salesOrderReq.setCustomer(salesOrderIn.getCustomer());
			salesOrderReq.setOrderDate(salesOrderIn.getOrderDate().atStartOfDay());

			salesOrderIn.getItems().forEach(itemIn -> {
				OrderItems itemReq = new OrderItems();
				itemReq.setProduct(itemIn.getProduct());
				itemReq.setQuantity(itemIn.getQuantity());
				itemReq.setPrice(itemIn.getPrice());
				salesOrderReq.addItems(itemReq);
			});

			batchrequest = batchrequest.beginChangeSet()
					.createSalesOrders(salesOrderReq)
					.endChangeSet();
		}

		// batch call
		BatchResponse result = batchrequest.executeRequest(destination);

		int index = 0;
		Collection<cds.gen.catalogservice.SalesOrders> salesOrdersOut = new ArrayList<>();

		for (cds.gen.catalogservice.SalesOrders salesOrderIn : salesOrdersIn) {
			Try<BatchResponseChangeSet> changeset = result.get(index);
			index++; // increment index for next loop
			if (changeset.isSuccess()) {
				List<VdmEntity<?>> SalesOrdersResp = changeset.get().getCreatedEntities();
				List<cds.gen.catalogservice.SalesOrders> salesOrdersList = SalesOrdersResp.stream()
						.map(entity -> {
							logger.info(entity.toString());
							cds.gen.catalogservice.SalesOrders salesOrderOut = cds.gen.catalogservice.SalesOrders
									.create();
							if (entity instanceof SalesOrders) {
								SalesOrders salesOrderResp = (SalesOrders) entity;
								salesOrderOut.setId(salesOrderResp.getID().toString());
								salesOrderOut.setCustomer(salesOrderResp.getCustomer());
								salesOrderOut.setOrderDate(salesOrderResp.getOrderDate().toLocalDate());

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
							}
							return salesOrderOut;

						})
						.collect(Collectors.toList());
				salesOrdersOut.addAll(salesOrdersList);
			}
		}
		context.setResult(salesOrdersOut);
	}

}