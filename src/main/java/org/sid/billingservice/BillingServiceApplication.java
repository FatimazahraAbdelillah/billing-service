package org.sid.billingservice;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.*;
import java.security.PublicKey;
import java.util.Collection;
import java.util.Date;
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
class Bill{
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private Date billingDate;
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private Long customerID;
	@Transient
	private Customer customer;
	@OneToMany(mappedBy = "bill")
	private Collection<ProductItem> productItems;
}
@RepositoryRestResource
interface BillRepository extends JpaRepository<Bill,Long>{

}
@Projection(name = "fullBill",types = Bill.class)
interface BillProjection{
public Long getId();
public  Date getBillingDate();
public Long getCustomerID();
public Collection <ProductItem> getProductItems();
}
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
class ProductItem{
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private Long productID;
	private double price;
	@Transient
	private Product product;
	private double quantity;
	@ManyToOne
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private Bill bill;
}
@RepositoryRestResource
interface ProductItemRepository extends JpaRepository<ProductItem,Long>{

}
@Data
class Customer{
	private Long id;
	private String name;
	private String email;
}
@FeignClient(name = "CUSTOMER-SERVICE")
interface CustomerService{
	@GetMapping("/customers/{id}")
	 public Customer findCustomerById(@PathVariable(name="id") Long id);
}
@Data
class Product{
	private Long id;
	private String name;
	private double price;
}
@FeignClient(name = "PRODUCTS-SERVICE")
interface InventoryService{
	@GetMapping("/products/{id}")
	public Product findProductById(@PathVariable(name="id") Long id);
	@GetMapping("/products")
	public PagedModel<Product> findAllProducts();
}
@SpringBootApplication
@EnableFeignClients
public class BillingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BillingServiceApplication.class, args);
	}
@Bean
	CommandLineRunner start(BillRepository billRepository,
							RepositoryRestConfiguration repositoryRestConfiguration,
							ProductItemRepository productItemRepository, CustomerService customerService,
							InventoryService inventoryService){
		return args -> {
			repositoryRestConfiguration.exposeIdsFor(Customer.class);
			Customer c1=customerService.findCustomerById(1L);
			System.out.println(c1.getId());
			System.out.println("name"+c1.getName());
			System.out.println("email"+c1.getEmail());
			Product p1=inventoryService.findProductById(1L);
				Bill b1=billRepository.save(new Bill(null,new Date(),c1.getId(),null,null));
				PagedModel<Product> products=inventoryService.findAllProducts();
				products.getContent().forEach(p -> {
					productItemRepository.save(new ProductItem(null,p.getId(),p.getPrice(),null,30,b1));
				});
		};
}
}
@RestController
 class BillController{
	@Autowired
	private BillRepository billRepository;
	@Autowired
	private ProductItemRepository productItemRepository;
	@Autowired
	private CustomerService customerService;
	@Autowired
	private InventoryService inventoryService;
@GetMapping("/fullBill/{id}")
	public Bill getBill(@PathVariable(name="id") Long id){
Bill bill=billRepository.findById(id).get();
bill.setCustomer(customerService.findCustomerById(bill.getCustomerID()));
bill.getProductItems().forEach(productItem -> {
	productItem.setProduct(inventoryService.findProductById(productItem.getProductID()));
});
return bill;
	}
		}
