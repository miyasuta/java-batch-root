package customer.java_batch_v4.handlers;

import java.time.LocalDate;
import java.util.List;

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

		//Get request
		final List<SalesOrders> salesorders = service.getAllSalesOrders().execute(destination);
		logger.info(salesorders.toString());
		context.setResult(salesorders.toString());		
	}

	@On(event = PostOrderV4Context.CDS_NAME)
	public void PostOrderv4(PostOrderV4Context context) {
		logger.info("PostOrderV4 handler called");

		HttpDestination destination = DestinationAccessor.getDestination("salesorder-srv").asHttp();
		DefaultSalesServiceV4Service service = new DefaultSalesServiceV4Service().withServicePath("/odata/v4/sales");

		//Post request
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

		//batch call
		try(
			BatchResponse result = service
			.batch()
			.addChangeset(createRequest)
			.execute(destination);
		) {
			ModificationResponse<SalesOrders> createResult = result.getModificationResult(createRequest);
			context.setResult(createResult.toString());
		}	
		
	}	
}
