-- Mini-UPS Sample Data Script
-- 为Mini-UPS系统生成测试数据，包含用户、卡车、运输订单、包裹等
-- 使用真实场景数据，方便API测试和功能验证

-- =================
-- 1. 用户数据 (Users)
-- =================

-- 管理员账户
INSERT INTO users (id, username, email, password, first_name, last_name, phone, address, role, enabled, created_at, updated_at) VALUES
(1, 'admin', 'admin@miniups.com', '$2a$10$dXJ3SW6G7P9LVvVEgdgeOOKl9lJ4EUCrj4z/eQu9Qb8SfzE7e7G2S', 'Admin', 'User', '+1-555-0001', '123 Admin St, Durham, NC 27701', 'ADMIN', true, '2024-01-01 08:00:00', '2024-01-01 08:00:00'),
(2, 'operator1', 'operator@miniups.com', '$2a$10$dXJ3SW6G7P9LVvVEgdgeOOKl9lJ4EUCrj4z/eQu9Qb8SfzE7e7G2S', 'Operations', 'Manager', '+1-555-0002', '456 Ops Ave, Durham, NC 27701', 'OPERATOR', true, '2024-01-01 08:00:00', '2024-01-01 08:00:00');

-- 司机账户
INSERT INTO users (id, username, email, password, first_name, last_name, phone, address, role, enabled, created_at, updated_at) VALUES
(3, 'driver001', 'john.driver@miniups.com', '$2a$10$dXJ3SW6G7P9LVvVEgdgeOOKl9lJ4EUCrj4z/eQu9Qb8SfzE7e7G2S', 'John', 'Driver', '+1-555-1001', '789 Driver Rd, Durham, NC 27701', 'DRIVER', true, '2024-01-01 09:00:00', '2024-01-01 09:00:00'),
(4, 'driver002', 'mike.driver@miniups.com', '$2a$10$dXJ3SW6G7P9LVvVEgdgeOOKl9lJ4EUCrj4z/eQu9Qb8SfzE7e7G2S', 'Mike', 'Wilson', '+1-555-1002', '321 Delivery St, Durham, NC 27701', 'DRIVER', true, '2024-01-01 09:00:00', '2024-01-01 09:00:00'),
(5, 'driver003', 'sarah.driver@miniups.com', '$2a$10$dXJ3SW6G7P9LVvVEgdgeOOKl9lJ4EUCrj4z/eQu9Qb8SfzE7e7G2S', 'Sarah', 'Johnson', '+1-555-1003', '654 Transport Ave, Durham, NC 27701', 'DRIVER', true, '2024-01-01 09:00:00', '2024-01-01 09:00:00');

-- 普通用户账户
INSERT INTO users (id, username, email, password, first_name, last_name, phone, address, role, enabled, created_at, updated_at) VALUES
(6, 'user001', 'alice.smith@email.com', '$2a$10$dXJ3SW6G7P9LVvVEgdgeOOKl9lJ4EUCrj4z/eQu9Qb8SfzE7e7G2S', 'Alice', 'Smith', '+1-555-2001', '100 Main St, Raleigh, NC 27601', 'USER', true, '2024-01-02 10:00:00', '2024-01-02 10:00:00'),
(7, 'user002', 'bob.jones@email.com', '$2a$10$dXJ3SW6G7P9LVvVEgdgeOOKl9lJ4EUCrj4z/eQu9Qb8SfzE7e7G2S', 'Bob', 'Jones', '+1-555-2002', '200 Oak Ave, Charlotte, NC 28201', 'USER', true, '2024-01-02 10:30:00', '2024-01-02 10:30:00'),
(8, 'user003', 'carol.brown@email.com', '$2a$10$dXJ3SW6G7P9LVvVEgdgeOOKl9lJ4EUCrj4z/eQu9Qb8SfzE7e7G2S', 'Carol', 'Brown', '+1-555-2003', '300 Pine St, Greensboro, NC 27401', 'USER', true, '2024-01-02 11:00:00', '2024-01-02 11:00:00'),
(9, 'user004', 'david.wilson@email.com', '$2a$10$dXJ3SW6G7P9LVvVEgdgeOOKl9lJ4EUCrj4z/eQu9Qb8SfzE7e7G2S', 'David', 'Wilson', '+1-555-2004', '400 Elm St, Winston-Salem, NC 27101', 'USER', true, '2024-01-02 11:30:00', '2024-01-02 11:30:00'),
(10, 'user005', 'emma.davis@email.com', '$2a$10$dXJ3SW6G7P9LVvVEgdgeOOKl9lJ4EUCrj4z/eQu9Qb8SfzE7e7G2S', 'Emma', 'Davis', '+1-555-2005', '500 Maple Ave, Asheville, NC 28801', 'USER', true, '2024-01-02 12:00:00', '2024-01-02 12:00:00');

-- 更新用户表序列
ALTER SEQUENCE users_id_seq RESTART WITH 11;

-- =================
-- 2. 卡车数据 (Trucks)
-- =================

INSERT INTO trucks (id, truck_id, status, current_x, current_y, capacity, driver_id, world_id, created_at, updated_at) VALUES
-- 空闲卡车
(1, 1001, 'IDLE', 50, 50, 100, 3, 1001, '2024-01-01 09:00:00', '2024-01-01 09:00:00'),
(2, 1002, 'IDLE', 75, 25, 150, 4, 1002, '2024-01-01 09:00:00', '2024-01-01 09:00:00'),
(3, 1003, 'IDLE', 30, 80, 120, 5, 1003, '2024-01-01 09:00:00', '2024-01-01 09:00:00'),

-- 繁忙卡车（正在配送）
(4, 1004, 'DELIVERING', 150, 200, 100, 3, 1004, '2024-01-01 09:00:00', '2024-01-03 14:30:00'),
(5, 1005, 'TRAVELING', 80, 120, 130, 4, 1005, '2024-01-01 09:00:00', '2024-01-03 15:00:00'),

-- 在仓库的卡车
(6, 1006, 'AT_WAREHOUSE', 100, 100, 140, 5, 1006, '2024-01-01 09:00:00', '2024-01-03 16:00:00'),
(7, 1007, 'LOADING', 100, 100, 110, 3, 1007, '2024-01-01 09:00:00', '2024-01-03 16:30:00');

-- 更新卡车表序列
ALTER SEQUENCE trucks_id_seq RESTART WITH 8;

-- =================
-- 3. 运输订单数据 (Shipments)
-- =================

INSERT INTO shipments (id, shipment_id, ups_tracking_id, amazon_order_id, status, origin_x, origin_y, dest_x, dest_y, weight, estimated_delivery, actual_delivery, pickup_time, world_id, user_id, truck_id, created_at, updated_at) VALUES

-- 已完成配送的订单
(1, 'AMZ-20240101-001', 'UPS-1Z001001', 'ORDER-001', 'DELIVERED', 100, 100, 150, 200, 2.50, '2024-01-02 16:00:00', '2024-01-02 15:45:00', '2024-01-02 10:00:00', 2001, 6, 1, '2024-01-01 14:00:00', '2024-01-02 15:45:00'),
(2, 'AMZ-20240101-002', 'UPS-1Z001002', 'ORDER-002', 'DELIVERED', 100, 100, 75, 250, 1.25, '2024-01-02 18:00:00', '2024-01-02 17:30:00', '2024-01-02 11:00:00', 2002, 7, 2, '2024-01-01 15:00:00', '2024-01-02 17:30:00'),

-- 正在配送的订单
(3, 'AMZ-20240102-001', 'UPS-1Z002001', 'ORDER-003', 'OUT_FOR_DELIVERY', 100, 100, 200, 150, 3.75, '2024-01-03 17:00:00', NULL, '2024-01-03 09:00:00', 2003, 8, 4, '2024-01-02 16:00:00', '2024-01-03 14:00:00'),
(4, 'AMZ-20240102-002', 'UPS-1Z002002', 'ORDER-004', 'IN_TRANSIT', 100, 100, 300, 100, 5.00, '2024-01-03 19:00:00', NULL, '2024-01-03 10:00:00', 2004, 9, 5, '2024-01-02 17:00:00', '2024-01-03 15:00:00'),

-- 已取货但未配送的订单
(5, 'AMZ-20240103-001', 'UPS-1Z003001', 'ORDER-005', 'PICKED_UP', 100, 100, 120, 180, 2.00, '2024-01-04 16:00:00', NULL, '2024-01-03 14:00:00', 2005, 10, 6, '2024-01-03 10:00:00', '2024-01-03 14:00:00'),

-- 新创建的订单
(6, 'AMZ-20240103-002', 'UPS-1Z003002', 'ORDER-006', 'CREATED', 100, 100, 80, 220, 1.75, '2024-01-04 18:00:00', NULL, NULL, 2006, 6, NULL, '2024-01-03 16:00:00', '2024-01-03 16:00:00'),
(7, 'AMZ-20240103-003', 'UPS-1Z003003', 'ORDER-007', 'TRUCK_DISPATCHED', 100, 100, 250, 180, 4.50, '2024-01-04 20:00:00', NULL, NULL, 2007, 7, 7, '2024-01-03 17:00:00', '2024-01-03 18:00:00'),

-- 异常订单
(8, 'AMZ-20240102-003', 'UPS-1Z002003', 'ORDER-008', 'DELIVERY_ATTEMPTED', 100, 100, 180, 120, 3.25, '2024-01-03 15:00:00', NULL, '2024-01-03 08:00:00', 2008, 8, 1, '2024-01-02 14:00:00', '2024-01-03 13:00:00'),

-- 取消的订单
(9, 'AMZ-20240101-003', 'UPS-1Z001003', 'ORDER-009', 'CANCELLED', 100, 100, 90, 160, 1.00, '2024-01-02 14:00:00', NULL, NULL, 2009, 9, NULL, '2024-01-01 16:00:00', '2024-01-01 20:00:00');

-- 更新运输订单表序列
ALTER SEQUENCE shipments_id_seq RESTART WITH 10;

-- =================
-- 4. 包裹数据 (Packages)
-- =================

INSERT INTO packages (id, package_id, description, weight, length_cm, width_cm, height_cm, value, fragile, shipment_id, created_at, updated_at) VALUES

-- 订单1的包裹
(1, 'PKG-001-001', 'Electronics - Smartphone', 0.45, 15.0, 8.0, 2.0, 299.99, false, 1, '2024-01-01 14:00:00', '2024-01-01 14:00:00'),
(2, 'PKG-001-002', 'Phone Case and Screen Protector', 0.15, 20.0, 12.0, 3.0, 29.99, false, 1, '2024-01-01 14:00:00', '2024-01-01 14:00:00'),

-- 订单2的包裹
(3, 'PKG-002-001', 'Book - Programming Guide', 1.25, 25.0, 20.0, 3.5, 49.99, false, 2, '2024-01-01 15:00:00', '2024-01-01 15:00:00'),

-- 订单3的包裹（正在配送）
(4, 'PKG-003-001', 'Kitchen Appliance - Blender', 2.80, 35.0, 25.0, 30.0, 129.99, false, 3, '2024-01-02 16:00:00', '2024-01-02 16:00:00'),
(5, 'PKG-003-002', 'Cookware Set', 0.95, 40.0, 30.0, 10.0, 79.99, true, 3, '2024-01-02 16:00:00', '2024-01-02 16:00:00'),

-- 订单4的包裹（运输中）
(6, 'PKG-004-001', 'Furniture - Office Chair', 5.00, 80.0, 60.0, 40.0, 199.99, false, 4, '2024-01-02 17:00:00', '2024-01-02 17:00:00'),

-- 订单5的包裹（已取货）
(7, 'PKG-005-001', 'Clothing - Winter Jacket', 1.20, 30.0, 25.0, 8.0, 89.99, false, 5, '2024-01-03 10:00:00', '2024-01-03 10:00:00'),
(8, 'PKG-005-002', 'Accessories - Gloves and Scarf', 0.80, 25.0, 20.0, 5.0, 34.99, false, 5, '2024-01-03 10:00:00', '2024-01-03 10:00:00'),

-- 订单6的包裹（新创建）
(9, 'PKG-006-001', 'Sports Equipment - Tennis Racket', 1.75, 70.0, 30.0, 5.0, 149.99, true, 6, '2024-01-03 16:00:00', '2024-01-03 16:00:00'),

-- 订单7的包裹（卡车已派遣）
(10, 'PKG-007-001', 'Home Decor - Lamp', 2.20, 25.0, 25.0, 45.0, 79.99, true, 7, '2024-01-03 17:00:00', '2024-01-03 17:00:00'),
(11, 'PKG-007-002', 'Art Supplies Set', 1.30, 35.0, 25.0, 8.0, 59.99, false, 7, '2024-01-03 17:00:00', '2024-01-03 17:00:00'),
(12, 'PKG-007-003', 'Stationery Items', 1.00, 30.0, 20.0, 5.0, 24.99, false, 7, '2024-01-03 17:00:00', '2024-01-03 17:00:00'),

-- 订单8的包裹（配送尝试失败）
(13, 'PKG-008-001', 'Gaming Console', 2.50, 40.0, 30.0, 15.0, 399.99, false, 8, '2024-01-02 14:00:00', '2024-01-02 14:00:00'),
(14, 'PKG-008-002', 'Gaming Controllers (2x)', 0.75, 25.0, 15.0, 8.0, 119.99, false, 8, '2024-01-02 14:00:00', '2024-01-02 14:00:00'),

-- 订单9的包裹（已取消）
(15, 'PKG-009-001', 'Beauty Products Set', 1.00, 20.0, 15.0, 10.0, 69.99, true, 9, '2024-01-01 16:00:00', '2024-01-01 16:00:00');

-- 更新包裹表序列
ALTER SEQUENCE packages_id_seq RESTART WITH 16;

-- =================
-- 5. 运输状态历史记录 (Shipment Status History)
-- =================

INSERT INTO shipment_status_history (id, shipment_id, status, timestamp, notes, created_at, updated_at) VALUES

-- 订单1的状态历史（已完成）
(1, 1, 'CREATED', '2024-01-01 14:00:00', 'Order created by Amazon system', '2024-01-01 14:00:00', '2024-01-01 14:00:00'),
(2, 1, 'TRUCK_DISPATCHED', '2024-01-02 08:00:00', 'Truck 1001 assigned and dispatched', '2024-01-02 08:00:00', '2024-01-02 08:00:00'),
(3, 1, 'PICKED_UP', '2024-01-02 10:00:00', 'Package picked up from warehouse', '2024-01-02 10:00:00', '2024-01-02 10:00:00'),
(4, 1, 'IN_TRANSIT', '2024-01-02 10:30:00', 'En route to destination', '2024-01-02 10:30:00', '2024-01-02 10:30:00'),
(5, 1, 'OUT_FOR_DELIVERY', '2024-01-02 14:00:00', 'Out for final delivery', '2024-01-02 14:00:00', '2024-01-02 14:00:00'),
(6, 1, 'DELIVERED', '2024-01-02 15:45:00', 'Successfully delivered to customer', '2024-01-02 15:45:00', '2024-01-02 15:45:00'),

-- 订单2的状态历史（已完成）
(7, 2, 'CREATED', '2024-01-01 15:00:00', 'Order created by Amazon system', '2024-01-01 15:00:00', '2024-01-01 15:00:00'),
(8, 2, 'TRUCK_DISPATCHED', '2024-01-02 09:00:00', 'Truck 1002 assigned and dispatched', '2024-01-02 09:00:00', '2024-01-02 09:00:00'),
(9, 2, 'PICKED_UP', '2024-01-02 11:00:00', 'Package picked up from warehouse', '2024-01-02 11:00:00', '2024-01-02 11:00:00'),
(10, 2, 'IN_TRANSIT', '2024-01-02 11:30:00', 'En route to destination', '2024-01-02 11:30:00', '2024-01-02 11:30:00'),
(11, 2, 'OUT_FOR_DELIVERY', '2024-01-02 15:00:00', 'Out for final delivery', '2024-01-02 15:00:00', '2024-01-02 15:00:00'),
(12, 2, 'DELIVERED', '2024-01-02 17:30:00', 'Successfully delivered to customer', '2024-01-02 17:30:00', '2024-01-02 17:30:00'),

-- 订单3的状态历史（正在配送）
(13, 3, 'CREATED', '2024-01-02 16:00:00', 'Order created by Amazon system', '2024-01-02 16:00:00', '2024-01-02 16:00:00'),
(14, 3, 'TRUCK_DISPATCHED', '2024-01-03 07:00:00', 'Truck 1004 assigned and dispatched', '2024-01-03 07:00:00', '2024-01-03 07:00:00'),
(15, 3, 'PICKED_UP', '2024-01-03 09:00:00', 'Package picked up from warehouse', '2024-01-03 09:00:00', '2024-01-03 09:00:00'),
(16, 3, 'IN_TRANSIT', '2024-01-03 09:30:00', 'En route to destination', '2024-01-03 09:30:00', '2024-01-03 09:30:00'),
(17, 3, 'OUT_FOR_DELIVERY', '2024-01-03 14:00:00', 'Out for final delivery', '2024-01-03 14:00:00', '2024-01-03 14:00:00'),

-- 订单4的状态历史（运输中）
(18, 4, 'CREATED', '2024-01-02 17:00:00', 'Order created by Amazon system', '2024-01-02 17:00:00', '2024-01-02 17:00:00'),
(19, 4, 'TRUCK_DISPATCHED', '2024-01-03 08:00:00', 'Truck 1005 assigned and dispatched', '2024-01-03 08:00:00', '2024-01-03 08:00:00'),
(20, 4, 'PICKED_UP', '2024-01-03 10:00:00', 'Package picked up from warehouse', '2024-01-03 10:00:00', '2024-01-03 10:00:00'),
(21, 4, 'IN_TRANSIT', '2024-01-03 10:30:00', 'En route to destination', '2024-01-03 10:30:00', '2024-01-03 10:30:00'),

-- 订单5的状态历史（已取货）
(22, 5, 'CREATED', '2024-01-03 10:00:00', 'Order created by Amazon system', '2024-01-03 10:00:00', '2024-01-03 10:00:00'),
(23, 5, 'TRUCK_DISPATCHED', '2024-01-03 12:00:00', 'Truck 1006 assigned and dispatched', '2024-01-03 12:00:00', '2024-01-03 12:00:00'),
(24, 5, 'PICKED_UP', '2024-01-03 14:00:00', 'Package picked up from warehouse', '2024-01-03 14:00:00', '2024-01-03 14:00:00'),

-- 订单6的状态历史（新创建）
(25, 6, 'CREATED', '2024-01-03 16:00:00', 'Order created by Amazon system', '2024-01-03 16:00:00', '2024-01-03 16:00:00'),

-- 订单7的状态历史（卡车已派遣）
(26, 7, 'CREATED', '2024-01-03 17:00:00', 'Order created by Amazon system', '2024-01-03 17:00:00', '2024-01-03 17:00:00'),
(27, 7, 'TRUCK_DISPATCHED', '2024-01-03 18:00:00', 'Truck 1007 assigned and dispatched', '2024-01-03 18:00:00', '2024-01-03 18:00:00'),

-- 订单8的状态历史（配送尝试失败）
(28, 8, 'CREATED', '2024-01-02 14:00:00', 'Order created by Amazon system', '2024-01-02 14:00:00', '2024-01-02 14:00:00'),
(29, 8, 'TRUCK_DISPATCHED', '2024-01-03 06:00:00', 'Truck 1001 assigned and dispatched', '2024-01-03 06:00:00', '2024-01-03 06:00:00'),
(30, 8, 'PICKED_UP', '2024-01-03 08:00:00', 'Package picked up from warehouse', '2024-01-03 08:00:00', '2024-01-03 08:00:00'),
(31, 8, 'IN_TRANSIT', '2024-01-03 08:30:00', 'En route to destination', '2024-01-03 08:30:00', '2024-01-03 08:30:00'),
(32, 8, 'OUT_FOR_DELIVERY', '2024-01-03 12:00:00', 'Out for final delivery', '2024-01-03 12:00:00', '2024-01-03 12:00:00'),
(33, 8, 'DELIVERY_ATTEMPTED', '2024-01-03 13:00:00', 'Customer not available, will retry tomorrow', '2024-01-03 13:00:00', '2024-01-03 13:00:00'),

-- 订单9的状态历史（已取消）
(34, 9, 'CREATED', '2024-01-01 16:00:00', 'Order created by Amazon system', '2024-01-01 16:00:00', '2024-01-01 16:00:00'),
(35, 9, 'CANCELLED', '2024-01-01 20:00:00', 'Order cancelled by customer request', '2024-01-01 20:00:00', '2024-01-01 20:00:00');

-- 更新状态历史表序列
ALTER SEQUENCE shipment_status_history_id_seq RESTART WITH 36;

-- =================
-- 6. 卡车位置历史记录 (Truck Location History)
-- =================

INSERT INTO truck_location_history (id, truck_id, x_coordinate, y_coordinate, timestamp, created_at, updated_at) VALUES

-- 卡车1001的位置历史
(1, 1, 50, 50, '2024-01-01 09:00:00', '2024-01-01 09:00:00', '2024-01-01 09:00:00'),
(2, 1, 100, 100, '2024-01-02 08:30:00', '2024-01-02 08:30:00', '2024-01-02 08:30:00'),
(3, 1, 125, 150, '2024-01-02 12:00:00', '2024-01-02 12:00:00', '2024-01-02 12:00:00'),
(4, 1, 150, 200, '2024-01-02 15:45:00', '2024-01-02 15:45:00', '2024-01-02 15:45:00'),
(5, 1, 180, 120, '2024-01-03 13:00:00', '2024-01-03 13:00:00', '2024-01-03 13:00:00'),

-- 卡车1002的位置历史
(6, 2, 75, 25, '2024-01-01 09:00:00', '2024-01-01 09:00:00', '2024-01-01 09:00:00'),
(7, 2, 100, 100, '2024-01-02 09:30:00', '2024-01-02 09:30:00', '2024-01-02 09:30:00'),
(8, 2, 85, 180, '2024-01-02 14:00:00', '2024-01-02 14:00:00', '2024-01-02 14:00:00'),
(9, 2, 75, 250, '2024-01-02 17:30:00', '2024-01-02 17:30:00', '2024-01-02 17:30:00'),

-- 卡车1004的位置历史（正在配送）
(10, 4, 100, 100, '2024-01-03 07:30:00', '2024-01-03 07:30:00', '2024-01-03 07:30:00'),
(11, 4, 130, 120, '2024-01-03 10:00:00', '2024-01-03 10:00:00', '2024-01-03 10:00:00'),
(12, 4, 160, 160, '2024-01-03 12:00:00', '2024-01-03 12:00:00', '2024-01-03 12:00:00'),
(13, 4, 180, 180, '2024-01-03 14:00:00', '2024-01-03 14:00:00', '2024-01-03 14:00:00'),
(14, 4, 150, 200, '2024-01-03 14:30:00', '2024-01-03 14:30:00', '2024-01-03 14:30:00'),

-- 卡车1005的位置历史（运输中）
(15, 5, 100, 100, '2024-01-03 08:30:00', '2024-01-03 08:30:00', '2024-01-03 08:30:00'),
(16, 5, 150, 110, '2024-01-03 11:00:00', '2024-01-03 11:00:00', '2024-01-03 11:00:00'),
(17, 5, 220, 105, '2024-01-03 13:30:00', '2024-01-03 13:30:00', '2024-01-03 13:30:00'),
(18, 5, 280, 102, '2024-01-03 15:00:00', '2024-01-03 15:00:00', '2024-01-03 15:00:00');

-- 更新位置历史表序列
ALTER SEQUENCE truck_location_history_id_seq RESTART WITH 19;

-- =================
-- 7. 地址变更记录 (Address Changes)
-- =================

INSERT INTO address_changes (id, shipment_id, original_x, original_y, new_x, new_y, original_address, new_address, reason, status, requested_by, request_time, processed_time, created_at, updated_at) VALUES

-- 成功的地址变更
(1, 3, 200, 150, 180, 170, '123 Old Street, City A', '456 New Avenue, City A', 'Customer moved to new address', 'APPROVED', 8, '2024-01-03 08:00:00', '2024-01-03 08:30:00', '2024-01-03 08:00:00', '2024-01-03 08:30:00'),

-- 待处理的地址变更
(2, 5, 120, 180, 140, 160, '789 Current Rd, City B', '321 Updated St, City B', 'More convenient delivery location', 'PENDING', 10, '2024-01-03 15:00:00', NULL, '2024-01-03 15:00:00', '2024-01-03 15:00:00'),

-- 被拒绝的地址变更
(3, 8, 180, 120, 200, 140, '555 Original Ave, City C', '777 Different Blvd, City C', 'Customer preference', 'REJECTED', 8, '2024-01-03 11:00:00', '2024-01-03 11:30:00', '2024-01-03 11:00:00', '2024-01-03 11:30:00');

-- 更新地址变更表序列
ALTER SEQUENCE address_changes_id_seq RESTART WITH 4;

-- =================
-- 8. 设置数据库配置
-- =================

-- 确保序列正确设置
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('trucks_id_seq', (SELECT MAX(id) FROM trucks));
SELECT setval('shipments_id_seq', (SELECT MAX(id) FROM shipments));
SELECT setval('packages_id_seq', (SELECT MAX(id) FROM packages));
SELECT setval('shipment_status_history_id_seq', (SELECT MAX(id) FROM shipment_status_history));
SELECT setval('truck_location_history_id_seq', (SELECT MAX(id) FROM truck_location_history));
SELECT setval('address_changes_id_seq', (SELECT MAX(id) FROM address_changes));

-- =================
-- 9. 数据验证查询
-- =================

-- 验证插入的数据
SELECT 'Users' as table_name, COUNT(*) as count FROM users
UNION ALL
SELECT 'Trucks', COUNT(*) FROM trucks  
UNION ALL
SELECT 'Shipments', COUNT(*) FROM shipments
UNION ALL
SELECT 'Packages', COUNT(*) FROM packages
UNION ALL
SELECT 'Status History', COUNT(*) FROM shipment_status_history
UNION ALL
SELECT 'Location History', COUNT(*) FROM truck_location_history
UNION ALL
SELECT 'Address Changes', COUNT(*) FROM address_changes;

-- 显示不同状态的运输订单统计
SELECT status, COUNT(*) as count 
FROM shipments 
GROUP BY status 
ORDER BY status;

-- 显示不同角色的用户统计
SELECT role, COUNT(*) as count 
FROM users 
GROUP BY role 
ORDER BY role;

-- 显示不同状态的卡车统计
SELECT status, COUNT(*) as count 
FROM trucks 
GROUP BY status 
ORDER BY status;

COMMIT;

-- 测试数据插入完成！
-- 
-- 测试账户信息：
-- 管理员: admin@miniups.com / password123
-- 操作员: operator@miniups.com / password123  
-- 司机: john.driver@miniups.com / password123
-- 用户: alice.smith@email.com / password123
--
-- 注意：所有密码都是 "password123" 的BCrypt加密版本
-- 实际应用中请使用更强的密码策略