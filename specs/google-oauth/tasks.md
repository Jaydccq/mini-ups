# 实施计划

## 阶段1：基础实现

- [ ] 1. 添加OAuth2 Client依赖到pom.xml
  - 添加spring-boot-starter-oauth2-client依赖
  - _需求: 需求4-系统集成_

- [ ] 2. 创建AuthProvider枚举类
  - 定义LOCAL和GOOGLE提供商类型
  - _需求: 需求2-安全账户关联_

- [ ] 3. 更新User实体模型
  - 添加authProvider和providerId字段
  - 修改password字段为可空
  - _需求: 需求1-Google OAuth登录, 需求2-安全账户关联_

- [ ] 4. 创建数据库迁移脚本
  - 添加auth_provider和provider_id列
  - 修改password列约束
  - 更新现有数据为LOCAL类型
  - _需求: 需求4-系统集成_

- [ ] 5. 更新AuthService添加OAuth2处理方法
  - 实现processOAuth2PostLogin方法
  - 实现registerNewOAuth2User方法
  - _需求: 需求1-Google OAuth登录_

- [ ] 6. 创建OAuth2AuthenticationSuccessHandler
  - 处理OAuth2认证成功事件
  - 生成JWT令牌并重定向到前端
  - _需求: 需求1-Google OAuth登录, 需求3-用户体验优化_

- [ ] 7. 创建OAuth2AuthenticationFailureHandler
  - 处理OAuth2认证失败情况
  - 友好的错误处理和重定向
  - _需求: 需求3-用户体验优化_

- [ ] 8. 更新SecurityConfig配置
  - 添加oauth2Login配置
  - 集成success和failure handlers
  - _需求: 需求4-系统集成_

- [ ] 9. 配置Google OAuth2属性
  - 在application.yml中添加Google配置
  - 配置重定向URI
  - _需求: 需求1-Google OAuth登录_

- [ ] 10. 运行数据库迁移并测试基础功能
  - 执行数据库迁移
  - 测试Google OAuth2登录流程
  - 验证JWT令牌生成
  - _需求: 需求1-Google OAuth登录, 需求4-系统集成_

## 测试验证

- [ ] 11. 编写单元测试
  - AuthService的OAuth2方法测试
  - SuccessHandler和FailureHandler测试
  - _需求: 需求4-系统集成_

- [ ] 12. 集成测试
  - 模拟OAuth2登录流程
  - 验证数据库操作
  - _需求: 需求1-Google OAuth登录_

- [ ] 13. 手动测试
  - 配置Google Cloud Console
  - 端到端OAuth2流程测试
  - _需求: 需求3-用户体验优化_