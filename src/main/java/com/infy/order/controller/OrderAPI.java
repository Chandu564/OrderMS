package com.infy.order.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
//import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.infy.order.dto.CartDTO;
import com.infy.order.dto.OrderDTO;
import com.infy.order.dto.ProductDTO;
import com.infy.order.service.OrderService;

@RestController
@CrossOrigin
@RequestMapping("/api/orders")
public class OrderAPI {

	@Autowired
	private OrderService orderService;

//	@Autowired
//	DiscoveryClient client;

	@Value("${user.uri}")
	String userUri;

	@Value("${product.uri}")
	String productUri;

	@KafkaListener(topics = "simple", groupId = "group_id")
	public void placeOrder(String msg) {

		try {

			System.out.println(msg);
			List<String> list = Arrays.asList(msg.split(", "));
			System.out.println(list);
			List<ProductDTO> productList = new ArrayList<>();

			List<CartDTO> cartList = new ArrayList<>();

			list.forEach(item -> {
				// Each item has format of = B100 P100 10
				String[] values = item.split(" ");
				String buyerId = values[0];
				String prodId = values[1];
				Integer quantity = Integer.parseInt(values[2]);

				cartList.add(new CartDTO(buyerId, prodId, quantity));

				ProductDTO prod = new RestTemplate().getForObject(productUri + "/api/products/" + prodId,
						ProductDTO.class);
				productList.add(prod);

			});

			System.out.println(cartList);
			System.out.println(productList);

			OrderDTO order = new OrderDTO();

			order.setAddress("Address");

			orderService.placeOrder(productList, cartList, order);

			// removing from the cart after ordering
			cartList.forEach(item -> {
				removeFromCart(item.getBuyerId(), item.getProdId());
			});

		} catch (Exception e) {
			String message = "no message";
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message, e);
		}
	}

//	@PostMapping(value = "/placeOrder/{buyerId}")
//	public ResponseEntity<String> placeOrder(@PathVariable String buyerId, @RequestBody OrderDTO order){
//
//		try {
////			List<ServiceInstance> userInstances=client.getInstances("USERMS");
////			ServiceInstance userInstance=userInstances.get(0);
////			URI userUri = userInstance.getUri();
//
//			ObjectMapper mapper = new ObjectMapper();
//			List<ProductDTO> productList = new ArrayList<>();
//			List<CartDTO> cartList = mapper.convertValue(
//					new RestTemplate().getForObject(userUri+"/userMS/buyer/cart/get/" + buyerId, List.class),
//				    new TypeReference<List<CartDTO>>(){}
//				);
//
////			List<ServiceInstance> instances=client.getInstances("PRODUCTMS");
////			ServiceInstance instance=instances.get(0);
////			URI productUri = instance.getUri();
//
//			cartList.forEach(item ->{
//				ProductDTO prod = new RestTemplate().getForObject(productUri+"/api/products/" +item.getProdId(),ProductDTO.class) ;
//				System.out.println(prod.getDescription());
//				productList.add(prod);
//			});
//
//			OrderPlacedDTO orderPlaced = orderService.placeOrder(productList,cartList,order);
//			cartList.forEach(item->{
//				new RestTemplate().put(productUri+"/api/products/stock/" +item.getProdId()+"/"+item.getQuantity(), boolean.class);
//				new RestTemplate().postForObject(userUri+"/userMS/buyer/cart/remove/"+buyerId+"/"+item.getProdId(),null, String.class);
//			});
//
//			new RestTemplate().getForObject(userUri+"/userMS/updateRewardPoints/"+buyerId+"/"+orderPlaced.getRewardPoints() , String.class);
//
//			return new ResponseEntity<>(orderPlaced.getOrderId(),HttpStatus.ACCEPTED);
//		}
//		catch(Exception e)
//		{
//			String newMsg = "There was some error";
//			if(e.getMessage().equals("404 null"))
//			{
//				newMsg = "Error while placing the order";
//			}
//			return new ResponseEntity<>(newMsg,HttpStatus.UNAUTHORIZED);
//		}
//
//	}

	@GetMapping(value = "")
	public ResponseEntity<List<OrderDTO>> viewAllOrder() {
		try {
			List<OrderDTO> allOrders = orderService.viewAllOrders();
			return new ResponseEntity<>(allOrders, HttpStatus.OK);
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
		}
	}

	@GetMapping(value = "/buyer/{buyerId}")
	public ResponseEntity<List<OrderDTO>> viewsOrdersByBuyerId(@PathVariable String buyerId) {
		try {
			List<OrderDTO> allOrders = orderService.viewOrdersByBuyer(buyerId);
			return new ResponseEntity<>(allOrders, HttpStatus.OK);
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
		}
	}

	@GetMapping(value = "/{orderId}")
	public ResponseEntity<OrderDTO> viewsOrderByOrderId(@PathVariable String orderId) {
		try {
			OrderDTO allOrders = orderService.viewOrder(orderId);
			return new ResponseEntity<>(allOrders, HttpStatus.OK);
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
		}
	}

	@PostMapping(value = "/reOrder/{buyerId}/{orderId}")
	public ResponseEntity<String> reOrder(@PathVariable String buyerId, @PathVariable String orderId) {

		try {

			String id = orderService.reOrder(buyerId, orderId);
			return new ResponseEntity<>("Order ID: " + id, HttpStatus.ACCEPTED);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.UNAUTHORIZED);
		}
	}

	@PostMapping(value = "/cart/add")
	public ResponseEntity<String> addToCart(@RequestBody CartDTO cartDTO) {

		try {

//			List<ServiceInstance> userInstances=client.getInstances("USERMS");
//			ServiceInstance userInstance=userInstances.get(0);
//			URI userUri = userInstance.getUri();

			String successMsg = new RestTemplate().postForObject(userUri + "cart", cartDTO, String.class);

			return new ResponseEntity<>(successMsg, HttpStatus.ACCEPTED);
		} catch (Exception e) {
			String newMsg = "There was some error";
			if (e.getMessage().equals("404 null")) {
				newMsg = "There are no PRODUCTS for the given product ID";
			}
			return new ResponseEntity<>(newMsg, HttpStatus.UNAUTHORIZED);
		}
	}

	@DeleteMapping(value = "/cart/remove/{buyerId}/{prodId}")
	public ResponseEntity<String> removeFromCart(@PathVariable String buyerId, @PathVariable String prodId) {

		try {

//			List<ServiceInstance> userInstances=client.getInstances("USERMS");
//			ServiceInstance userInstance=userInstances.get(0);
//			URI userUri = userInstance.getUri();
//			System.out.println(userUri);

			new RestTemplate().delete(userUri + "/cart/" + buyerId + "/" + prodId, null, String.class);

			return new ResponseEntity<>("Successfully Deleted", HttpStatus.ACCEPTED);
		} catch (Exception e) {
			String newMsg = "There was some error";
			if (e.getMessage().equals("404 null")) {
				newMsg = "There are no PRODUCTS for the given product ID";
			}
			return new ResponseEntity<>(newMsg, HttpStatus.UNAUTHORIZED);
		}
	}

}
