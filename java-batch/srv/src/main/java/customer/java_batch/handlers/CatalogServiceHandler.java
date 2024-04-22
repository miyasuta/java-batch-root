package customer.java_batch.handlers;

import java.util.stream.Stream;

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
		context.setResult("completed");
	}

}