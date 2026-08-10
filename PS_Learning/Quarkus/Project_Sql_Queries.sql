-- user table creation script
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100),
    phone VARCHAR(20)
);

-- product table creation script
CREATE TABLE product (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255),
    price INT,
    description TEXT
);

-- order table creation script
CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    user_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user
        FOREIGN KEY(user_id) 
        REFERENCES users(id)
);

-- order_item table creation script
CREATE TABLE order_item (
    id SERIAL PRIMARY KEY,
    order_id INT,
    product_id INT,
    quantity INT,
    CONSTRAINT fk_order
        FOREIGN KEY(order_id) REFERENCES orders(id),
    CONSTRAINT fk_product
        FOREIGN KEY(product_id) REFERENCES product(id)
);

CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    account_number VARCHAR(12) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    balance NUMERIC(15,2) NOT NULL
);

CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    from_account VARCHAR(12),
    to_account VARCHAR(12),
    amount NUMERIC(15,2),
    transaction_time TIMESTAMP
);

CREATE TABLE delivery
(
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT,
    user_id BIGINT,
    status VARCHAR(50),
    created_at TIMESTAMP
);

-- Insert data into users table
INSERT INTO users (name, email, phone) VALUES
('Ecomm App', 'ecom-app@email.com', '9876543210');

-- Insert into accounts table
INSERT INTO accounts (account_number, user_id, balance) VALUES
('100000000001', 1, 25000.00);


-- Insert data into product table
INSERT INTO product (name, price, description) VALUES
('iPhone 14', 8000, 'Apple smartphone'),
('Samsung S23', 7500, 'Samsung flagship phone'),
('OnePlus 11', 6000, 'OnePlus premium phone'),
('Laptop Dell', 9000, 'Dell business laptop'),
('Sony Headphones', 1500, 'Noise cancelling headphones');

-- Insert data into order table
INSERT INTO orders (user_id) VALUES
(1),
(1),
(2);

-- Insert data into order_item table
INSERT INTO order_item (order_id, product_id, quantity) VALUES
(1, 1, 1),
(1, 5, 2),
(2, 2, 1),
(2, 3, 1),
(3, 4, 1);

-- get data
select * from users;
select * from product;
select * from orders;
select * from order_item;
select * from accounts;
select * from transactions;

-- delete data
delete from order_item where order_id in (1502, 1503);
delete from orders where user_id=3;
delete from users;
delete from product;
delete from accounts;

-- drop tables
drop table users;
drop table product;
drop table orders;
drop table order_item;
drop table transactions;
drop table accounts;
