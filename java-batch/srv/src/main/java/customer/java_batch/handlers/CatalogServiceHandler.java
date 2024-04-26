package customer.java_batch.handlers;

import java.time.LocalDateTime;
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
import com.sap.cloud.sdk.datamodel.odata.helper.batch.BatchResponse;
import com.sap.cloud.sdk.datamodel.odata.helper.batch.BatchResponseChangeSet;
import com.mycompany.vdm.services.SalesServiceV2Service;
import com.mycompany.vdm.namespaces.salesservicev2.SalesOrdersCreateFluentHelper;
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

		//Get request
		final List<SalesOrders> salesorders = service.getAllSalesOrders().executeRequest(destination);
		logger.info(salesorders.toString());
		context.setResult(salesorders.toString());		
	}

	@On(event = PostOrderV2Context.CDS_NAME)
	public void PostOrderv2(PostOrderV2Context context) {
		logger.info("PostOrderv2 handler called");

		HttpDestination destination = DestinationAccessor.getDestination("salesorder-srv").asHttp();
		DefaultSalesServiceV2Service service = new DefaultSalesServiceV2Service().withServicePath("/odata/v2/sales");

		//Post request
		SalesOrders salesorder = new SalesOrders();
		salesorder.setCustomer("Java");
		salesorder.setOrderDate(LocalDateTime.now());

		OrderItems items = new OrderItems();
		items.setProduct("Product A");
		items.setPrice(1000);
		items.setQuantity(1);
		salesorder.addItems(items);

		ModificationResponse<SalesOrders> response = service.createSalesOrders(salesorder).executeRequest(destination);
		context.setResult(response.toString());
	}

	@On(event = PostBatchV2Context.CDS_NAME)
	public void PostBatchV2(PostBatchV2Context context) {
		logger.info("PostBatchv2 handler called");

		HttpDestination destination = DestinationAccessor.getDestination("salesorder-srv").asHttp();
		DefaultSalesServiceV2Service service = new DefaultSalesServiceV2Service();//.withServicePath("/odata/v2/sales");

		SalesOrders salesorder = new SalesOrders();
		salesorder.setCustomer("Java");
		salesorder.setOrderDate(LocalDateTime.now());

		OrderItems items = new OrderItems();
		items.setProduct("Product A");
		items.setPrice(1000);
		items.setQuantity(1);
		salesorder.addItems(items);

		//batch call
		BatchResponse result = service
								.batch()
								.beginChangeSet()
								.createSalesOrders(salesorder)
								.endChangeSet()
								.executeRequest(destination);

		Try<BatchResponseChangeSet> changeSetTry = result.get(0);
		context.setResult(changeSetTry.toString());
	}

}