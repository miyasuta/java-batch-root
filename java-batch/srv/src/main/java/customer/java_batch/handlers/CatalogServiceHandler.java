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
import cds.gen.catalogservice.Books;
import cds.gen.catalogservice.PostBatchv2Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpDestination;
import com.sap.cloud.sdk.datamodel.odata.helper.ModificationResponse;

import com.mycompany.vdm.services.SalesServiceV2Service;
import com.mycompany.vdm.namespaces.salesservicev2.SalesOrdersCreateFluentHelper;
import com.mycompany.vdm.services.DefaultSalesServiceV2Service;
import com.mycompany.vdm.namespaces.salesservicev2.SalesOrders;

@Component
@ServiceName(CatalogService_.CDS_NAME)
public class CatalogServiceHandler implements EventHandler {

	Logger logger = LoggerFactory.getLogger(CatalogServiceHandler.class);

	@After(event = CqnService.EVENT_READ)
	public void discountBooks(Stream<Books> books) {
		books.filter(b -> b.getTitle() != null && b.getStock() != null)
		.filter(b -> b.getStock() > 200)
		.forEach(b -> b.setTitle(b.getTitle() + " (discounted)"));
	}

	@On(event = PostBatchv2Context.CDS_NAME)
	public void PostBatchv2(PostBatchv2Context context) {
		logger.info("PostBatchv2 handler called");

		HttpDestination destination = DestinationAccessor.getDestination("salesorder-srv").asHttp();
		DefaultSalesServiceV2Service service = new DefaultSalesServiceV2Service().withServicePath("/odata/v2/sales");

		//Getリクエスト
		// final List<SalesOrders> salesorders = service.getAllSalesOrders().executeRequest(destination);
		// logger.info(salesorders.toString());
		// context.setResult(salesorders.toString());

		//Postリクエスト
		SalesOrders salesorder = new SalesOrders();
		salesorder.setCustomer("Java");
		salesorder.setOrderDate(LocalDateTime.now());
		ModificationResponse<SalesOrders> response = service.createSalesOrders(salesorder).executeRequest(destination);
		context.setResult(response.toString());
	}

}