-- 种子数据：3 条未订阅（可订）的票
-- 已订阅列表初始为空，用户通过聊天订票后才会出现

INSERT INTO bookings (title, status) VALUES ('北京-上海 G123', 'UNSUBSCRIBED');
INSERT INTO bookings (title, status) VALUES ('上海-深圳 D456', 'UNSUBSCRIBED');
INSERT INTO bookings (title, status) VALUES ('广州-北京 K789', 'UNSUBSCRIBED');
