package customer.java_batch.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


class CatalogServiceHandlerTest {

	private CatalogServiceHandler handler = new CatalogServiceHandler();

	@Test
	void dummy() {
		assertTrue(true);
	}

	// @BeforeEach
	// public void prepareBook() {
	// 	book.setTitle("title");
	// }

	// @Test
	// void testDiscount() {
	// 	book.setStock(500);
	// 	handler.discountBooks(Stream.of(book));
	// 	assertEquals("title (discounted)", book.getTitle());
	// }

	// @Test
	// void testNoDiscount() {
	// 	book.setStock(100);
	// 	handler.discountBooks(Stream.of(book));
	// 	assertEquals("title", book.getTitle());
	// }

	// @Test
	// void testNoStockAvailable() {
	// 	handler.discountBooks(Stream.of(book));
	// 	assertEquals("title", book.getTitle());
	// }

}
