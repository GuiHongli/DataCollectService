-- 在vmos_data表中添加是否已替换数据标志字段（保存到数据库后用于恢复“已执行”状态）
ALTER TABLE `vmos_data`
  ADD COLUMN `speed_replaced` tinyint(4) DEFAULT 0 COMMENT '下行速率是否已替换并保存：0-否，1-是',
  ADD COLUMN `game_rtt_replaced` tinyint(4) DEFAULT 0 COMMENT '游戏内RTT是否已替换并保存：0-否，1-是',
  ADD COLUMN `network_rtt_replaced` tinyint(4) DEFAULT 0 COMMENT '网络侧RTT是否已替换并保存：0-否，1-是';
