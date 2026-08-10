// Bank Application 
### File: C:/Users/nikhil.prabhat/quarkus_workspace/bank-app/src/main/java/com/usk/dto/ErrorResponse.java
```java
package com.usk.dto;

public record ErrorResponse(String errorMessage, int errorStatus) { }

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/bank-app/src/main/java/com/usk/dto/RegisterUserRequest.java
```java
package com.usk.dto;

public record RegisterUserRequest(String name, String email, String phone) {
}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/bank-app/src/main/java/com/usk/dto/TransferFundRequest.java
```java
package com.usk.dto;

import java.math.BigDecimal;

public record TransferFundRequest(String fromAccount, String toAccount, BigDecimal amount) {
}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/bank-app/src/main/java/com/usk/dto/RegisterUserResponse.java
```java
package com.usk.dto;

import java.math.BigDecimal;

public record RegisterUserResponse(Long userId, String accountNumber, BigDecimal balance) {
}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/bank-app/src/main/java/com/usk/util/Constants.java
```java
package com.usk.util;

import java.time.LocalDateTime;

import static java.time.LocalDateTime.now;

public interface Constants {

    // DB Queries
    String FIND_BY_ACCOUNT_NUM_QUERY = "fromAccount = ?1 OR toAccount = ?1";
    String ACCOUNT_NUMBER = "accountNumber";
    String USER_ID = "userId";

    String ACCOUNT_NUMBER_FORMATTER = "%012d";
    Long ACCOUNT_NUMBER_FROM_RANGE = 100000000000L;
    Long ACCOUNT_NUMBER_TO_RANGE = 999999999999L;
    Long DEFAULT_ACCOUNT_BALANCE = 10000L;
    LocalDateTime CURRENT_TIME = now();
    String TRANSFER_SUCCESSFUL = "Transfer Successful !!";

    // Exception Message
    String FROM_ACCOUNT_NOT_FOUND_EXCEPTION_MSG = "From_Account Not Found !!";
    String TO_ACCOUNT_NOT_FOUND_EXCEPTION_MSG = "To_Account Not Found !!";
    String INSUFFICIENT_BALANCE_EXCEPTION_MSG = "Insufficient_Balance";


}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/bank-app/src/main/java/com/usk/util/AccountNumberGenerator.java
```java
package com.usk.util;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.concurrent.ThreadLocalRandom;

import static com.usk.util.Constants.*;

@ApplicationScoped
public class AccountNumberGenerator {

    public String generateAccountNumber() {
        long accountNumber = ThreadLocalRandom
                                .current()
                                .nextLong(ACCOUNT_NUMBER_FROM_RANGE, ACCOUNT_NUMBER_TO_RANGE);

        return String.format(ACCOUNT_NUMBER_FORMATTER, accountNumber);
    }
}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/bank-app/src/main/java/com/usk/entity/User.java
```java
package com.usk.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "users")
@Data
@Builder
public class User extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "name")
    private String username;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phoneNumber;
}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/bank-app/src/main/java/com/usk/entity/Account.java
```java
package com.usk.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "accounts")
@Data
@Builder
public class Account extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "balance")
    private BigDecimal balance;
}


```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/bank-app/src/main/java/com/usk/entity/Transaction.java
```java
package com.usk.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "transactions")
@Data
@Builder
public class Transaction extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "from_account")
    private String fromAccount;

    @Column(name = "to_account")
    private String toAccount;

    private BigDecimal amount;

    @Column(name = "transaction_time")
    private LocalDateTime transactionTime;

}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/bank-app/src/main/java/com/usk/service/BankService.java
```java
package com.usk.service;

import com.usk.dto.RegisterUserRequest;
import com.usk.dto.RegisterUserResponse;
import com.usk.dto.TransferFundRequest;
import com.usk.entity.Account;
import com.usk.entity.Transaction;
import com.usk.entity.User;
import com.usk.exception.AccountNotFoundException;
import com.usk.exception.InsufficientBalanceException;
import com.usk.repository.AccountRepository;
import com.usk.repository.TransactionRepository;
import com.usk.repository.UserRepository;
import com.usk.util.AccountNumberGenerator;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.List;

import static com.usk.util.Constants.*;

@ApplicationScoped
public class BankService {

    @Inject
    private UserRepository userRepository;

    @Inject
    private AccountRepository accountRepository;

    @Inject
    private TransactionRepository transactionRepository;

    @Inject
    private AccountNumberGenerator accountNumberGenerator;

    @WithTransaction
    public Uni<RegisterUserResponse> registerUser(RegisterUserRequest registerUserRequest) {
        User user = User.builder()
                .username(registerUserRequest.name())
                .email(registerUserRequest.email())
                .phoneNumber(registerUserRequest.phone())
                .build();

        return userRepository.persist(user)
                .replaceWith(user)
                .flatMap(savedUser -> {
                    Account account = Account.builder()
                            .userId(savedUser.getId())
                            .accountNumber(accountNumberGenerator.generateAccountNumber())
                            .balance(BigDecimal.valueOf(DEFAULT_ACCOUNT_BALANCE))
                            .build();

                    return accountRepository.persist(account)
                            .replaceWith(new RegisterUserResponse(savedUser.getId(), account.getAccountNumber(), account.getBalance()));
                });
    }

    @WithTransaction
    public Uni<String> transferFund(TransferFundRequest transferFundRequest) {
        return accountRepository.findByAccountNumber(transferFundRequest.fromAccount())
                .onItem()
                .ifNull()
                .failWith(() -> new AccountNotFoundException(FROM_ACCOUNT_NOT_FOUND_EXCEPTION_MSG))
                .flatMap(fromAccount -> {
                    return accountRepository.findByAccountNumber(transferFundRequest.toAccount())
                            .onItem()
                            .ifNull()
                            .failWith(() -> new AccountNotFoundException(TO_ACCOUNT_NOT_FOUND_EXCEPTION_MSG))
                            .flatMap(toAccount -> {
                                Uni<Transaction> currentTransaction = handleAccountValidationAndGetTransaction(fromAccount, toAccount, transferFundRequest);

                                return accountRepository.persist(fromAccount)
                                        .flatMap(toAccountPar -> accountRepository.persist(toAccount))
                                        .flatMap(transactionPar -> currentTransaction
                                                .flatMap(transaction -> transactionRepository.persist(transaction)))
                                        .replaceWith(TRANSFER_SUCCESSFUL);
                            });
                });
    }

    @WithSession
    public Uni<List<Transaction>> getTransactionHistory(String accountNumber) {
        return transactionRepository.findByAccount(accountNumber);
    }

    @WithSession
    public Uni<Account> getAccountByUserId(Long userId) {
        return accountRepository.findByUserId(userId);
    }

    private Uni<Transaction> handleAccountValidationAndGetTransaction(Account fromAccount, Account toAccount, TransferFundRequest transferFundRequest) {
        if(fromAccount.getBalance().compareTo(transferFundRequest.amount()) < 0) {
            return Uni.createFrom().failure(new InsufficientBalanceException(INSUFFICIENT_BALANCE_EXCEPTION_MSG));
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(transferFundRequest.amount()));
        toAccount.setBalance(toAccount.getBalance().add(transferFundRequest.amount()));

        Transaction transaction = Transaction.builder()
                .fromAccount(transferFundRequest.fromAccount())
                .toAccount(transferFundRequest.toAccount())
                .amount(transferFundRequest.amount())
                .transactionTime(CURRENT_TIME)
                .build();

        return Uni.createFrom().item(transaction);
    }
}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/bank-app/src/main/java/com/usk/resource/BankResource.java
```java
package com.usk.resource;


import com.usk.dto.RegisterUserRequest;
import com.usk.dto.TransferFundRequest;
import com.usk.service.BankService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.jboss.resteasy.reactive.RestResponse.Status.CREATED;
import static org.jboss.resteasy.reactive.RestResponse.StatusCode.OK;

@Path("/bank")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class BankResource {

    @Inject
    private BankService bankService;

    @POST
    @Path("/register")
    public Uni<Response> registerUser(RegisterUserRequest registerUserRequest) {
        return bankService.registerUser(registerUserRequest)
                .map(registerUserResponse -> Response
                        .status(CREATED)
                        .entity(registerUserResponse)
                        .build());
    }

    @POST
    @Path("/transfer")
    public Uni<Response> transferFund(TransferFundRequest transferFundRequest) {
        return bankService.transferFund(transferFundRequest)
                .map(transferResponse -> Response
                        .status(OK)
                        .entity(transferResponse)
                        .build());
    }

    @GET
    @Path("/transactions/{accountNumber}")
    public Uni<Response> getTransactionHistory(@PathParam("accountNumber") String accountNumber) {
        return bankService.getTransactionHistory(accountNumber)
                .map(transactionList -> Response
                        .status(OK)
                        .entity(transactionList)
                        .build());
    }


    @GET
    @Path("/account/user/{userId}")
    public Uni<Response> getAccount(@PathParam("userId") Long userId) {
        return bankService.getAccountByUserId(userId)
                .map(userAccountId -> Response
                .status(OK)
                .entity(userAccountId)
                .build());
    }

}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/bank-app/src/main/java/com/usk/exception/GlobalExceptionHandler.java
```java
package com.usk.exception;

import com.usk.dto.ErrorResponse;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.jboss.resteasy.reactive.RestResponse.Status.*;

public class GlobalExceptionHandler {

    @ServerExceptionMapper
    public Response handleInsufficientBalanceException(InsufficientBalanceException insufficientBalanceException) {
        ErrorResponse errorResponse = new ErrorResponse(insufficientBalanceException.getMessage(), 402);

        return Response.status(PAYMENT_REQUIRED)
                .entity(errorResponse)
                .type(APPLICATION_JSON)
                .build();
    }

    @ServerExceptionMapper
    public Response handleAccountNotFoundException(AccountNotFoundException accountNotFoundException) {
        ErrorResponse errorResponse = new ErrorResponse(accountNotFoundException.getMessage(), 404);

        return Response.status(NOT_FOUND)
                .entity(errorResponse)
                .type(APPLICATION_JSON)
                .build();
    }

    @ServerExceptionMapper
    public Response handleGenericException(Throwable ex) {
        ErrorResponse errorResponse = new ErrorResponse(ex.getMessage(), 500);

        return Response.status(INTERNAL_SERVER_ERROR)
                .entity(errorResponse)
                .type(APPLICATION_JSON)
                .build();
    }

}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/bank-app/src/main/java/com/usk/exception/AccountNotFoundException.java
```java
package com.usk.exception;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String message) {
        super(message);
    }
}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/bank-app/src/main/java/com/usk/exception/InsufficientBalanceException.java
```java
package com.usk.exception;

public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(String message) {
        super(message);
    }
}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/bank-app/src/main/java/com/usk/repository/UserRepository.java
```java
package com.usk.repository;

import com.usk.entity.User;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserRepository implements PanacheRepository<User> {
}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/bank-app/src/main/java/com/usk/repository/AccountRepository.java
```java
package com.usk.repository;

import com.usk.entity.Account;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import static com.usk.util.Constants.ACCOUNT_NUMBER;
import static com.usk.util.Constants.USER_ID;

@ApplicationScoped
public class AccountRepository implements PanacheRepository<Account> {

    public Uni<Account> findByAccountNumber(String accountNumber) {
        return find(ACCOUNT_NUMBER, accountNumber)
                .firstResult();
    }

    public Uni<Account> findByUserId(Long userId) {
        return find(USER_ID, userId)
                .firstResult();
    }
}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/bank-app/src/main/java/com/usk/repository/TransactionRepository.java
```java
package com.usk.repository;

import com.usk.entity.Transaction;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

import static com.usk.util.Constants.FIND_BY_ACCOUNT_NUM_QUERY;

@ApplicationScoped
public class TransactionRepository implements PanacheRepository<Transaction> {

    public Uni<List<Transaction>> findByAccount(String accountNumber) {
        return find(FIND_BY_ACCOUNT_NUM_QUERY, accountNumber)
                .list();
    }
}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/bank-app/src/main/java/com/usk/BankAppApplication.java
```java
package com.usk;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class BankAppApplication {
    public static void main(String[] args) {
        Quarkus.run();
    }
}

```

// Order & Inventory
### File: C:/Users/nikhil.prabhat/quarkus_workspace/training-demo/src/main/java/com/usk/dto/Item.java
```java
package com.usk.dto;

public record Item(Long productId, Integer quantity) { }

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/training-demo/src/main/java/com/usk/dto/DeliveryEvent.java
```java
package com.usk.dto;

public record DeliveryEvent(Long orderId, Long userId, Integer totalAmount) {
}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/training-demo/src/main/java/com/usk/dto/ErrorResponse.java
```java
package com.usk.dto;

public record ErrorResponse(String errorMessage, int errorStatus) { }

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/training-demo/src/main/java/com/usk/dto/AccountResponse.java
```java
package com.usk.dto;

import java.math.BigDecimal;

public record AccountResponse(Long id, Long userId, String accountNumber, BigDecimal balance) { }

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/training-demo/src/main/java/com/usk/dto/CreateOrderRequest.java
```java
package com.usk.dto;

import java.util.List;

public record CreateOrderRequest(Long userId, List<Item> itemList) { }

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/training-demo/src/main/java/com/usk/dto/TransferFundRequest.java
```java
package com.usk.dto;

import java.math.BigDecimal;

public record TransferFundRequest(String fromAccount, String toAccount, BigDecimal amount) {
}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/training-demo/src/main/java/com/usk/util/Constants.java
```java
package com.usk.util;

public interface Constants {

    // Query Constants
    String PRODUCT_SEARCH_QUERY = "LOWER(name) like ?1";
    String ORDER_SEARCH_QUERY = "user.id = ?1 ORDER BY createdAt DESC";

    // Exception Message
    String PRODUCT_NOT_FOUND_EXCEPTION_MSG = "Product Not Found with Id: ";
    String USER_NOT_FOUND_EXCEPTION_MSG = "User Not Found with Id: ";

    String BANK_CLIENT_BASE_URL = "http://localhost:8081";
    String ECOMM_ACCOUNT_ID = "100000000001";
}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/training-demo/src/main/java/com/usk/client/BankClient.java
```java
package com.usk.client;

import com.usk.dto.TransferFundRequest;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import static com.usk.util.Constants.BANK_CLIENT_BASE_URL;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@RegisterRestClient(baseUri = BANK_CLIENT_BASE_URL)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
@Path("/bank")
public interface BankClient {

    @POST
    @Path("/transfer")
    public Uni<Response> transferFund(TransferFundRequest transferFundRequest);

    @GET
    @Path("/account/user/{userId}")
    public Uni<Response> getAccount(@PathParam("userId") Long userId);
}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/training-demo/src/main/java/com/usk/entity/User.java
```java
package com.usk.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Data;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Data
@Table(name = "users")
public class User extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "name")
    private String username;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phoneNumber;
}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/training-demo/src/main/java/com/usk/entity/Order.java
```java
package com.usk.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name = "orders")
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString(exclude = {"user", "orderItemList"})
public class Order extends PanacheEntity {

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    public User user;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    public List<OrderItem> orderItemList;

    @Transient
    public Integer totalOrderCost = 0;
}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/training-demo/src/main/java/com/usk/entity/Product.java
```java
package com.usk.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;

@Entity
@Data
public class Product extends PanacheEntity {

    @Column
    public String name;

    @Column
    public int price;

    @Column
    public String description;
}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/training-demo/src/main/java/com/usk/entity/OrderItem.java
```java
package com.usk.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Data
@Entity
@Table(name = "order_item")
@ToString(exclude = {"order", "product"})
public class OrderItem extends PanacheEntity {

    @ManyToOne
    @JoinColumn(name = "order_id")
    @JsonIgnore
    public Order order;

    @ManyToOne
    @JoinColumn(name = "product_id")
    public Product product;

    @Column
    public Integer quantity;

    @Transient
    public Integer productCost;
}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/training-demo/src/main/java/com/usk/service/OrderService.java
```java
package com.usk.service;

import com.usk.client.BankClient;
import com.usk.dto.*;
import com.usk.entity.Order;
import com.usk.entity.OrderItem;
import com.usk.exception.ProductNotFoundException;
import com.usk.exception.UserNotFoundException;
import com.usk.messaging.DeliverProducer;
import com.usk.repository.OrderItemRepository;
import com.usk.repository.OrderRepository;
import com.usk.repository.ProductRepository;
import com.usk.repository.UserRepository;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static com.usk.util.Constants.ECOMM_ACCOUNT_ID;
import static java.time.LocalDateTime.now;

@ApplicationScoped
public class OrderService {

    @Inject
    private OrderRepository orderRepository;

    @Inject
    private OrderItemRepository orderItemRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private ProductRepository productRepository;

    @Inject
    private DeliverProducer deliverProducer;

    @RestClient
    private BankClient bankClient;

    public Uni<Order> createOrderAndNotify(CreateOrderRequest createOrderRequest) {
        return createOrderAndTransferFund(createOrderRequest)
                .flatMap(savedOrder-> {
                    DeliveryEvent deliveryEvent = new DeliveryEvent(savedOrder.id, savedOrder.user.getId(), savedOrder.totalOrderCost);
                    return deliverProducer.publishDeliveryEvent(deliveryEvent)
                            .replaceWith(savedOrder);
                });
    }

    public Uni<Order> createOrderAndTransferFund(CreateOrderRequest createOrderRequest) {
        return bankClient.getAccount(createOrderRequest.userId())
                .flatMap(accountResponse -> {
                    if (accountResponse.getStatus() != 200) {
                        return Uni.createFrom().failure(new RuntimeException("Account not found"));
                    }

                    AccountResponse account = accountResponse.readEntity(AccountResponse.class);
                    return calculateTotalCost(createOrderRequest.itemList())
                            .flatMap(totalCost -> {
                                TransferFundRequest transferRequest = new TransferFundRequest(account.accountNumber(), ECOMM_ACCOUNT_ID, BigDecimal.valueOf(totalCost));
                                return bankClient.transferFund(transferRequest)
                                        .flatMap(transferResponse -> {
                                            if (transferResponse.getStatus() != 200) {
                                                return Uni.createFrom().failure(new RuntimeException("Payment failed"));
                                            }

                                            return createOrder(createOrderRequest);
                                        });
                            });
                });
    }

    @WithTransaction
    public Uni<Order> createOrder(CreateOrderRequest createOrderRequest) {

        Order order = new Order();
        order.setCreatedAt(now());

        return userRepository.findById(createOrderRequest.userId())
                .onItem()
                .ifNull()
                .failWith(() -> new UserNotFoundException(createOrderRequest.userId()))
                .flatMap(user -> {
                    order.setUser(user);
                    return orderRepository.persist(order);
                })
                .flatMap(savedOrder -> processOrderItems(savedOrder, createOrderRequest.itemList(), 0, 0));

    }

    private Uni<Order> processOrderItems(Order order, List<Item> items, int currentOrderItemIndex, int totalOrderCost) {

        if (currentOrderItemIndex >= items.size()) {
            order.setTotalOrderCost(totalOrderCost);
            return Uni.createFrom().item(order);
        }

        Item currentOrderItem = items.get(currentOrderItemIndex);
        return productRepository.findById(currentOrderItem.productId())
                .onItem()
                .ifNull()
                .failWith(() -> new ProductNotFoundException(currentOrderItem.productId()))
                .flatMap(product -> {
                    int itemCost = product.getPrice() * currentOrderItem.quantity();

                    OrderItem orderItem = new OrderItem();
                    orderItem.setOrder(order);
                    orderItem.setProduct(product);
                    orderItem.setQuantity(currentOrderItem.quantity());
                    orderItem.setProductCost(itemCost);

                    return orderItemRepository.persist(orderItem)
                            .flatMap(savedOrderItem -> processOrderItems(order, items, currentOrderItemIndex + 1, totalOrderCost + itemCost));
                });
    }

    @WithSession
    public Uni<List<Order>> getLatestOrders(Long userId) {
        return orderRepository.findLatestOrders(userId)
                .map(orders -> {
                    orders.forEach(order -> {
                        int totalCost = order.getOrderItemList()
                                .stream()
                                .mapToInt(item -> {
                                    int itemCost = item.getProduct().getPrice() * item.getQuantity();
                                    item.setProductCost(itemCost);
                                    return itemCost;
                                })
                                .sum();

                        order.setTotalOrderCost(totalCost);
                    });

                    return orders;
                });
    }

    private Uni<Integer> calculateTotalCost(List<Item> items) {
        return calculateTotalCost(items, 0, 0);
    }

    private Uni<Integer> calculateTotalCost(List<Item> items, int currentIndex, int totalCost) {
        if (currentIndex >= items.size()) {
            return Uni.createFrom().item(totalCost);
        }

        Item item = items.get(currentIndex);
        return productRepository.findById(item.productId())
                .onItem()
                .ifNull()
                .failWith(() -> new ProductNotFoundException(item.productId()))
                .flatMap(product -> {
                    int itemCost = product.getPrice() * item.quantity();
                    return calculateTotalCost(items, currentIndex + 1, totalCost + itemCost);
                });
    }

}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/training-demo/src/main/java/com/usk/service/ProductService.java
```java
package com.usk.service;

import com.usk.entity.Product;
import com.usk.repository.ProductRepository;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class ProductService {

    @Inject
    private ProductRepository productRepository;

    @WithSession
    public Uni<List<Product>> getProducts(String nameKeyword, int page, int pageSize) {
        return productRepository.searchProductWithNameKeyword(nameKeyword, page, pageSize);
    }
}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/training-demo/src/main/java/com/usk/resource/OrderResource.java
```java
package com.usk.resource;

import com.usk.dto.CreateOrderRequest;
import com.usk.service.OrderService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.jboss.resteasy.reactive.RestResponse.Status.CREATED;
import static org.jboss.resteasy.reactive.RestResponse.Status.OK;

@Path("/orders")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class OrderResource {

    @Inject
    private OrderService orderService;

    @POST
    public Uni<Response> createOrder(CreateOrderRequest createOrderRequest) {
        return orderService.createOrderAndNotify(createOrderRequest)
                .map(order -> Response
                        .status(CREATED)
                        .entity(order)
                        .build());
    }

    @GET
    @Path("/latest/{userId}")
    public Uni<Response> getLatestOrders(@PathParam("userId") Long userId) {
        return orderService.getLatestOrders(userId)
                .map(order -> Response
                        .status(OK)
                        .entity(order)
                        .build());
    }
}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/training-demo/src/main/java/com/usk/resource/ProductResource.java
```java
package com.usk.resource;

import com.usk.entity.Product;
import com.usk.service.ProductService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.jboss.resteasy.reactive.RestResponse.Status.OK;

@Path("/products")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class ProductResource {

    @Inject
    private ProductService productService;

    @GET
    public Uni<Response> getProducts(@QueryParam("nameKeyword") String nameKeyword,
                                          @QueryParam("page") @DefaultValue("0") int page,
                                          @QueryParam("size") @DefaultValue("10") int pageSize) {
        return productService.getProducts(nameKeyword, page, pageSize)
                .map(order -> Response
                        .status(OK)
                        .entity(order)
                        .build());
    }
}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/training-demo/src/main/java/com/usk/exception/UserNotFoundException.java
```java
package com.usk.exception;

import static com.usk.util.Constants.USER_NOT_FOUND_EXCEPTION_MSG;

public class UserNotFoundException extends RuntimeException{

    public UserNotFoundException(Long userId){
        super(USER_NOT_FOUND_EXCEPTION_MSG + userId);
    }
}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/training-demo/src/main/java/com/usk/exception/GlobalExceptionHandler.java
```java
package com.usk.exception;

import com.usk.dto.ErrorResponse;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.jboss.resteasy.reactive.RestResponse.Status.INTERNAL_SERVER_ERROR;
import static org.jboss.resteasy.reactive.RestResponse.Status.NOT_FOUND;

public class GlobalExceptionHandler {

    @ServerExceptionMapper
    public Response handleUserNotFoundException(UserNotFoundException userNotFoundException) {
        ErrorResponse errorResponse = new ErrorResponse(userNotFoundException.getMessage(), 404);

        return Response.status(NOT_FOUND)
                .entity(errorResponse)
                .type(APPLICATION_JSON)
                .build();
    }

    @ServerExceptionMapper
    public Response handleProductNotFoundException(ProductNotFoundException productNotFoundException) {
        ErrorResponse errorResponse = new ErrorResponse(productNotFoundException.getMessage(), 404);

        return Response.status(NOT_FOUND)
                .entity(errorResponse)
                .type(APPLICATION_JSON)
                .build();
    }

    @ServerExceptionMapper
    public Response handleGenericException(Throwable ex) {
        ErrorResponse errorResponse = new ErrorResponse(ex.getMessage(), 500);

        return Response.status(INTERNAL_SERVER_ERROR)
                .entity(errorResponse)
                .type(APPLICATION_JSON)
                .build();
    }

}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/training-demo/src/main/java/com/usk/exception/ProductNotFoundException.java
```java
package com.usk.exception;

import static com.usk.util.Constants.PRODUCT_NOT_FOUND_EXCEPTION_MSG;

public class ProductNotFoundException extends RuntimeException{

    public ProductNotFoundException(Long productId) {
        super(PRODUCT_NOT_FOUND_EXCEPTION_MSG + productId);
    }
}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/training-demo/src/main/java/com/usk/messaging/DeliverProducer.java
```java
package com.usk.messaging;

import com.usk.dto.DeliveryEvent;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

@ApplicationScoped
public class DeliverProducer {

    @Inject
    @Channel("ecomm-topic")
    Emitter<DeliveryEvent> deliveryEventEmitter;

    public Uni<Void> publishDeliveryEvent(DeliveryEvent deliveryEvent) {
        return Uni.createFrom()
                .completionStage(() -> deliveryEventEmitter.send(deliveryEvent));
    }
}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/training-demo/src/main/java/com/usk/repository/UserRepository.java
```java
package com.usk.repository;

import com.usk.entity.User;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserRepository implements PanacheRepository<User> {
}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/training-demo/src/main/java/com/usk/repository/OrderRepository.java
```java
package com.usk.repository;

import com.usk.entity.Order;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

import static com.usk.util.Constants.ORDER_SEARCH_QUERY;

@ApplicationScoped
public class OrderRepository implements PanacheRepository<Order> {

    public Uni<List<Order>> findLatestOrders(Long userId) {
        return find(ORDER_SEARCH_QUERY, userId)
                .page(0,5)
                .list();
    }
}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/training-demo/src/main/java/com/usk/repository/ProductRepository.java
```java
package com.usk.repository;

import com.usk.entity.Product;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

import static com.usk.util.Constants.PRODUCT_SEARCH_QUERY;

@ApplicationScoped
public class ProductRepository implements PanacheRepository<Product> {

    public Uni<List<Product>> searchProductWithNameKeyword(String nameKeyword, int page, int size) {
        return find(PRODUCT_SEARCH_QUERY, "%" + nameKeyword.toLowerCase()+ "%")
                .page(page, size)
                .list();
    }
}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/training-demo/src/main/java/com/usk/repository/OrderItemRepository.java
```java
package com.usk.repository;

import com.usk.entity.OrderItem;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrderItemRepository implements PanacheRepository<OrderItem> {
}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/training-demo/src/main/java/com/usk/TrainingDemoApplication.java
```java
package com.usk;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class TrainingDemoApplication {
    public static void main(String[] args) {
        Quarkus.run();
    }
}

```
// Delivery App
### File: C:/Users/nikhil.prabhat/quarkus_workspace/delivery-app/src/main/java/com/usk/dto/DeliveryEvent.java
```java
package com.usk.dto;

public record DeliveryEvent(Long orderId, Long userId, Integer totalAmount) {
}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/delivery-app/src/main/java/com/usk/entity/Delivery.java
```java
package com.usk.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "delivery")
@Data
@Builder
public class Delivery extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "total_amount")
    private Integer totalAmount;

    @Column(name = "status")
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/delivery-app/src/main/java/com/usk/service/DeliveryService.java
```java
package com.usk.service;

import com.usk.dto.DeliveryEvent;
import com.usk.entity.Delivery;
import com.usk.repository.DeliveryRepository;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

import static java.time.LocalDateTime.now;

@ApplicationScoped
public class DeliveryService {

    @Inject
    private DeliveryRepository deliveryRepository;

    @WithTransaction
    public Uni<Delivery> createDelivery(DeliveryEvent deliveryEvent) {
        Delivery delivery = Delivery.builder()
                .userId(deliveryEvent.userId())
                .orderId(deliveryEvent.orderId())
                .totalAmount(deliveryEvent.totalAmount())
                .status("CREATED")
                .createdAt(now())
                .build();

        return deliveryRepository.persist(delivery)
                .replaceWith(delivery);
    }

    @WithSession
    public Uni<List<Delivery>> getDelivery(Long orderId) {
        return deliveryRepository.find("orderId", orderId).list();
    }
}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/delivery-app/src/main/java/com/usk/resource/DeliveryResource.java
```java
package com.usk.resource;

import com.usk.service.DeliveryService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.jboss.resteasy.reactive.RestResponse.StatusCode.OK;

@Path("/delivery")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class DeliveryResource {

    @Inject
    private DeliveryService deliveryService;

    @GET
    @Path("/{orderId}")
    public Uni<Response> getDeliveryByOrderId(@PathParam("orderId") Long orderId) {
        return deliveryService.getDelivery(orderId)
                .map(deliveryList -> Response
                        .status(OK)
                        .entity(deliveryList)
                        .build());
    }
}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/delivery-app/src/main/java/com/usk/messaging/DeliveryConsumer.java
```java
package com.usk.messaging;

import com.usk.dto.DeliveryEvent;
import com.usk.service.DeliveryService;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class DeliveryConsumer {

    @Inject
    private DeliveryService deliveryService;

    @Incoming("ecomm-topic")
    public Uni<Void> consumeDeliveryEvent(DeliveryEvent deliveryEvent) {
        Log.info("Received Delivery Event : "+ deliveryEvent);
        return deliveryService.createDelivery(deliveryEvent)
                .replaceWithVoid();
    }

}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/delivery-app/src/main/java/com/usk/messaging/DeliveryEventDeserialiser.java
```java
package com.usk.messaging;

import com.usk.dto.DeliveryEvent;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public class DeliveryEventDeserialiser extends ObjectMapperDeserializer<DeliveryEvent> {

    public DeliveryEventDeserialiser() {
        super(DeliveryEvent.class);
    }
}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/delivery-app/src/main/java/com/usk/repository/DeliveryRepository.java
```java
package com.usk.repository;

import com.usk.entity.Delivery;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DeliveryRepository implements PanacheRepository<Delivery> {
}

```


### File: C:/Users/nikhil.prabhat/quarkus_workspace/delivery-app/src/main/java/com/usk/DeliveryAppApplication.java
```java
package com.usk;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class DeliveryAppApplication {
    public static void main(String[] args) {
        Quarkus.run();
    }
}

```
