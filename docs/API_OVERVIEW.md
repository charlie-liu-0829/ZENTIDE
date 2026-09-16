# ZENTIDE API 接口注释

本文档是项目 Controller 接口的快速说明。所有路径默认带服务上下文 `/api`：

- 用户端：`http://localhost:6050/api`
- 管理端：`http://localhost:6061/api`

用户端接口通过请求头 `token` 识别用户；标记“登录”的接口由 `@GlobalInterceptor(checkLogin = true)` 保护。除特别说明外，接口统一返回 `ResponseVO`。

## 一、用户端 API（zentide-web）

### 1. 账号与文件

Controller：`AccountController`、`FileController`

| 方法 | 路径 | 登录 | 功能 |
|---|---|---:|---|
| POST | `/account/checkCode` | 否 | 生成图形验证码，并将验证码保存到 Redis |
| POST | `/account/register` | 否 | 注册用户，校验验证码、邮箱、昵称和密码 |
| POST | `/account/login` | 否 | 校验验证码和密码，创建 Redis 登录会话 |
| POST | `/account/autoLogin` | 否 | 根据已有 Token 恢复登录并刷新有效期 |
| POST | `/account/logout` | 否 | 删除当前 Token，使会话失效 |
| POST | `/account/updatePassword` | 是 | 修改当前用户密码 |
| POST | `/account/security-question` | 是 | 设置或修改密保问题 |
| POST | `/account/forgot-password/question` | 否 | 根据邮箱查询密保问题 |
| POST | `/account/forgot-password/reset` | 否 | 通过密保答案重置密码 |
| POST | `/file/uploadImage` | 否 | 上传普通图片 |
| POST | `/file/uploadPostMedia` | 否 | 上传帖子图片或视频素材 |
| GET | `/file/getResource` | 否 | 读取已保存的静态资源 |

### 2. 平台信息

Controller：`PlatformController`、`ZentidePlatformController`

| 方法 | 路径 | 登录 | 功能 |
|---|---|---:|---|
| GET | `/v1/platform/info` | 否 | 返回平台版本、运行环境等公开信息 |

### 3. Radar、Change、Fact 和 Evidence

这些接口围绕“外部变化监测、事实、证据”组织数据。

#### Radar

Controller：`ZentideRadarController`

| 方法 | 路径 | 登录 | 功能 |
|---|---|---:|---|
| POST | `/zentide/v1/radars/list` | 是 | 查询当前用户的 Radar 列表 |
| POST | `/zentide/v1/radars` | 是 | 创建 Radar 监测计划 |
| POST | `/zentide/v1/radars/{radarId}/plan` | 是 | 修改 Radar 的监测计划 |
| POST | `/zentide/v1/radars/{radarId}/topics/{topicId}` | 是 | 将话题绑定到 Radar |

#### Change、Evidence、Research

Controller：`ZentideFactController`、`ZentideChangeWatchController`、`ZentideChangeStanceController`、`ZentideCommunityEvidenceController`

| 方法 | 路径 | 登录 | 功能 |
|---|---|---:|---|
| POST | `/zentide/v1/changes/list` | 否 | 分页查询变化事件 |
| POST | `/zentide/v1/changes/{changeId}` | 否 | 查看变化事件详情 |
| POST | `/zentide/v1/changes/{changeId}/evidence` | 否 | 查看某条变化的正式证据 |
| POST | `/zentide/v1/changes/watching/list` | 是 | 查询当前用户关注的变化 |
| POST | `/zentide/v1/changes/{changeId}/watch` | 是 | 关注一条变化 |
| POST | `/zentide/v1/changes/{changeId}/unwatch` | 是 | 取消关注一条变化 |
| POST | `/zentide/v1/changes/{changeId}/stances/summary` | 否 | 查看该变化下的立场统计摘要 |
| POST | `/zentide/v1/changes/{changeId}/stances/mine` | 是 | 查看当前用户对该变化的立场 |
| POST | `/zentide/v1/changes/{changeId}/stances` | 是 | 选择或修改当前用户立场 |
| POST | `/zentide/v1/changes/{changeId}/evidence-submissions` | 是 | 提交一条待审核的社区证据 |

### 4. Change 讨论

Controller：`ZentideDiscussionController`

| 方法 | 路径 | 登录 | 功能 |
|---|---|---:|---|
| POST | `/zentide/v1/changes/{changeId}/discussions/list` | 否 | 查询某条变化下已发布的讨论 |
| POST | `/zentide/v1/changes/{changeId}/discussions` | 是 | 发布讨论，可关联父讨论和证据 |
| POST | `/zentide/v1/changes/{changeId}/discussions/{discussionId}/remove` | 是 | 删除当前用户有权限删除的讨论 |

### 5. 话题、信号、观察和情报

Controller：`ZentideTopicController`、`ZentideSignalController`、`ZentideObservationController`、`ZentideInsightController`

| 方法 | 路径 | 登录 | 功能 |
|---|---|---:|---|
| POST | `/zentide/v1/topics/discover` | 否 | 发现和搜索热门话题 |
| POST | `/zentide/v1/topics/{topicId}/unfollow` | 是 | 取消关注话题 |
| POST | `/zentide/v1/topics/{topicId}/sources/list` | 否 | 查询话题关联的数据来源 |
| POST | `/zentide/v1/topics/{topicId}/source-applications` | 是 | 申请为话题添加数据来源 |
| POST | `/zentide/v1/topics/{topicId}/feed` | 否 | 查询话题相关动态流 |
| POST | `/zentide/v1/observations/activities/list` | 是 | 查询观察活动记录 |
| POST | `/zentide/v1/insights/list` | 是 | 查询个性化洞察 |
| POST | `/zentide/v1/insights/feedback` | 是 | 提交洞察反馈 |
| POST | `/zentide/v1/briefings/daily` | 是 | 获取每日情报简报 |

### 6. 兴趣现场（Community Hub）

Controller：`ZentideCommunityController`

#### 搜索、发现和基础配置

| 方法 | 路径 | 登录 | 功能 |
|---|---|---:|---|
| POST | `/zentide/v1/community/search` | 否 | 搜索帖子或兴趣现场，优先使用 Elasticsearch |
| POST | `/zentide/v1/community/search/history` | 是 | 查询当前用户搜索历史 |
| POST | `/zentide/v1/community/search/history/clear` | 是 | 清空搜索历史 |
| POST | `/zentide/v1/community/hubs` | 否 | 查询当前用户可见的兴趣现场 |
| POST | `/zentide/v1/community/hub-directions` | 否 | 查询兴趣现场方向 |
| POST | `/zentide/v1/community/hubs/discover` | 否 | 按关键词或分类发现兴趣现场 |
| POST | `/zentide/v1/community/hubs/create` | 是 | 创建兴趣现场 |
| POST | `/zentide/v1/community/hubs/{hubId}/delete` | 是 | 删除或停用兴趣现场 |
| POST | `/zentide/v1/community/hub-categories` | 是 | 查询当前用户创建的分类 |
| POST | `/zentide/v1/community/hub-categories/create` | 是 | 创建兴趣现场分类 |
| POST | `/zentide/v1/community/hub-categories/{categoryId}/delete` | 是 | 删除兴趣现场分类 |
| POST | `/zentide/v1/community/hubs/{hubId}/category` | 是 | 给兴趣现场分配分类 |
| POST | `/zentide/v1/community/entities` | 否 | 查询兴趣对象（实体）列表 |
| POST | `/zentide/v1/community/hub-directions` | 否 | 查询可用的兴趣方向 |

#### 成员和邀请

| 方法 | 路径 | 登录 | 功能 |
|---|---|---:|---|
| POST | `/zentide/v1/community/hubs/{hubId}/membership` | 是 | 加入或退出兴趣现场；私密现场可提交申请答案 |
| POST | `/zentide/v1/community/hubs/{hubId}/members/pending` | 是 | 查询待审核成员申请 |
| POST | `/zentide/v1/community/hubs/{hubId}/members` | 是 | 查询兴趣现场成员 |
| POST | `/zentide/v1/community/hubs/{hubId}/members/{memberId}/admin` | 是 | 修改成员角色和权限 |
| POST | `/zentide/v1/community/hubs/{hubId}/members/{memberId}/remove` | 是 | 移除成员 |
| POST | `/zentide/v1/community/hubs/{hubId}/members/{memberId}/review` | 是 | 审核成员申请 |
| POST | `/zentide/v1/community/hubs/{hubId}/invitations` | 是 | 创建兴趣现场邀请 |
| POST | `/zentide/v1/community/invitations/{token}` | 否 | 查看邀请信息 |
| POST | `/zentide/v1/community/invitations/{token}/redeem` | 是 | 使用邀请加入兴趣现场 |

#### 话题、帖子类型和关联对象

| 方法 | 路径 | 登录 | 功能 |
|---|---|---:|---|
| POST | `/zentide/v1/community/topics` | 否 | 查询兴趣现场话题 |
| POST | `/zentide/v1/community/hubs/{hubId}/topics/create` | 是 | 创建兴趣现场话题 |
| POST | `/zentide/v1/community/post-types` | 否 | 查询系统帖子类型 |
| POST | `/zentide/v1/community/hubs/{hubId}/post-types` | 是 | 查询兴趣现场自定义帖子类型 |
| POST | `/zentide/v1/community/hubs/{hubId}/post-types/create` | 是 | 创建自定义帖子类型 |
| POST | `/zentide/v1/community/hubs/{hubId}/post-types/{postTypeId}/delete` | 是 | 删除自定义帖子类型 |
| POST | `/zentide/v1/community/hubs/{hubId}/post-types/{postTypeId}/update` | 是 | 修改自定义帖子类型 |
| POST | `/zentide/v1/community/changes/{changeId}/context` | 否 | 查询变化事件的社区上下文 |
| POST | `/zentide/v1/community/entities/{entityId}/follow` | 是 | 关注或取消关注兴趣对象 |
| POST | `/zentide/v1/community/entities/{entityId}/action` | 是 | 设置兴趣对象状态，如 WANT、WATCHING |

#### Feed、活动和统计

| 方法 | 路径 | 登录 | 功能 |
|---|---|---:|---|
| POST | `/zentide/v1/community/feed` | 否 | 查询兴趣现场帖子动态流 |
| POST | `/zentide/v1/community/events` | 否 | 查询活动列表或动态 |
| POST | `/zentide/v1/community/highlights/today` | 否 | 查询今日社区高光数据 |
| POST | `/zentide/v1/community/hubs/{hubId}/events/managed` | 是 | 查询当前用户管理的活动 |
| POST | `/zentide/v1/community/hubs/{hubId}/events` | 是 | 创建活动 |
| POST | `/zentide/v1/community/events/{eventId}/update` | 是 | 修改活动 |
| POST | `/zentide/v1/community/events/{eventId}/delete` | 是 | 删除活动 |
| POST | `/zentide/v1/community/events/{eventId}/stats` | 否 | 查看活动统计 |
| POST | `/zentide/v1/community/events/{eventId}/attendance` | 是 | 设置参加活动的状态 |

#### 帖子、评论、互动和用户主页

| 方法 | 路径 | 登录 | 功能 |
|---|---|---:|---|
| POST | `/zentide/v1/community/posts/{postId}` | 否 | 查看帖子详情并记录浏览量 |
| POST | `/zentide/v1/community/posts` | 是 | 发布帖子；经过治理和智能发帖检查 |
| POST | `/zentide/v1/community/posts/{postId}/update` | 是 | 修改自己发布的帖子 |
| POST | `/zentide/v1/community/posts/{postId}/delete` | 是 | 删除自己发布的帖子 |
| POST | `/zentide/v1/community/posts/{postId}/comments` | 是 | 发布评论或回复评论 |
| POST | `/zentide/v1/community/posts/{postId}/comments/list` | 否 | 查询帖子评论 |
| POST | `/zentide/v1/community/comments/{commentId}/like` | 是 | 点赞或取消点赞评论 |
| POST | `/zentide/v1/community/posts/{postId}/like` | 是 | 点赞或取消点赞帖子 |
| POST | `/zentide/v1/community/posts/{postId}/bookmark` | 是 | 收藏或取消收藏帖子 |
| POST | `/zentide/v1/community/posts/{postId}/action` | 是 | 设置帖子兴趣状态 |
| POST | `/zentide/v1/community/users/{userId}` | 否 | 查看用户公开主页 |
| POST | `/zentide/v1/community/users/{userId}/posts` | 否 | 查询用户发布的帖子 |
| POST | `/zentide/v1/community/users/{userId}/comments` | 否 | 查询用户发布的评论 |
| POST | `/zentide/v1/community/users/me/bookmarks` | 是 | 查询当前用户收藏的帖子 |
| POST | `/zentide/v1/community/users/me/profile` | 是 | 修改个人社区主页资料 |
| POST | `/zentide/v1/community/users/{userId}/follow` | 是 | 关注或取消关注用户 |

### 7. 私信

Controller：`ZentideDirectMessageController`

| 方法 | 路径 | 登录 | 功能 |
|---|---|---:|---|
| POST | `/zentide/v1/messages/conversations` | 是 | 查询会话列表 |
| POST | `/zentide/v1/messages/with/{peerId}` | 是 | 查询与指定用户的消息 |
| POST | `/zentide/v1/messages/with/{peerId}/send` | 是 | 向指定用户发送私信 |
| POST | `/zentide/v1/messages/unread-count` | 是 | 查询未读消息数 |
| POST | `/zentide/v1/messages/users/search` | 是 | 搜索可发送私信的用户 |

### 8. AI Agent、发帖检查和兴趣情报

Controller：`ZentideAgentController`、`ZentidePostReviewController`、`ZentideGovernanceController`、`ZentideInterestInsightController`

| 方法 | 路径 | 登录 | 功能 |
|---|---|---:|---|
| POST | `/zentide/v1/agent/chat/stream` | 是 | 以流式方式接收 Chat Agent 回答 |
| POST | `/posts/review` | 是 | 调用 SmartPosting 检查帖子内容 |
| POST | `/posts/review/events` | 是 | 记录发帖检查过程中的用户行为 |
| POST | `/zentide/v1/governance/review` | 是 | 请求治理 Agent 审核单条内容 |
| POST | `/zentide/v1/governance/scan` | 是 | 批量扫描已发布公开帖子 |
| GET | `/zentide/v1/governance/rules` | 是 | 查询治理规则 |
| POST | `/zentide/v1/governance/rules` | 是 | 创建治理规则 |
| POST | `/zentide/v1/governance/feedback` | 是 | 提交治理审核反馈 |
| POST | `/zentide/v1/interest-insights/generate` | 是 | 根据用户兴趣生成情报推荐 |
| GET | `/zentide/v1/interest-insights/keywords` | 是 | 查询兴趣关键词 |
| POST | `/zentide/v1/interest-insights/keywords` | 是 | 新增兴趣关键词 |
| PATCH | `/zentide/v1/interest-insights/keywords/{id}` | 是 | 启用或停用兴趣关键词 |
| DELETE | `/zentide/v1/interest-insights/keywords/{id}` | 是 | 删除兴趣关键词 |
| POST | `/zentide/v1/interest-insights/feedback` | 是 | 提交推荐结果反馈 |
| GET | `/zentide/v1/interest-insights/saved` | 是 | 查询已保存的情报 |
| DELETE | `/zentide/v1/interest-insights/saved/{itemId}` | 是 | 删除已保存的情报 |

## 二、管理端 API（zentide-admin）

管理端除了账号、文件和平台公开接口外，其余接口由管理端拦截器校验 `adminToken`，用于后台运营和审核。

### 1. 管理员账号、文件和平台

Controller：`AccountController`、`FileController`、`PlatformController`

| 方法 | 路径 | 功能 |
|---|---|---|
| POST | `/account/checkCode` | 生成管理员登录验证码 |
| POST | `/account/login` | 管理员登录并创建 Redis 管理会话 |
| POST | `/account/logout` | 删除管理员会话 |
| POST | `/file/uploadImage` | 上传后台使用的图片 |
| 请求 | `/file/getResource` | 读取静态资源 |
| GET | `/v1/platform/info` | 返回管理端平台信息 |

### 2. 用户管理

Controller：`UserController`

| 方法 | 路径 | 功能 |
|---|---|---|
| 请求 | `/user/loadUser` | 分页查询用户列表 |
| 请求 | `/user/changeStatus` | 启用或禁用用户；禁用时强制清理该用户登录会话 |

### 3. 社区管理

Controller：`ZentideAdminCommunityController`

| 方法 | 路径 | 功能 |
|---|---|---|
| POST | `/zentide/v1/admin/community/hubs/list` | 分页查询兴趣现场 |
| POST | `/zentide/v1/admin/community/hubs/{hubId}/status` | 修改兴趣现场状态 |
| POST | `/zentide/v1/admin/community/posts/list` | 分页查询帖子并进行后台筛选 |
| POST | `/zentide/v1/admin/community/posts/{postId}/status` | 修改帖子审核/展示状态 |
| POST | `/zentide/v1/admin/community/comments/list` | 分页查询评论 |
| POST | `/zentide/v1/admin/community/comments/{commentId}/status` | 修改评论状态 |
| POST | `/zentide/v1/admin/community/topics/list` | 分页查询话题 |
| POST | `/zentide/v1/admin/community/topics/{topicId}/status` | 修改话题状态 |
| POST | `/zentide/v1/admin/community/overview` | 查询社区运营概览统计 |
| POST | `/zentide/v1/admin/community/members/pending` | 查询待审核成员申请 |
| POST | `/zentide/v1/admin/community/hubs/{hubId}/members/{userId}/status` | 修改成员状态 |
| POST | `/zentide/v1/admin/community/directions/list` | 查询兴趣方向 |
| POST | `/zentide/v1/admin/community/directions/create` | 创建兴趣方向 |
| POST | `/zentide/v1/admin/community/directions/{directionId}/update` | 修改兴趣方向 |
| POST | `/zentide/v1/admin/community/directions/{directionId}/delete` | 删除兴趣方向 |

### 4. 管理员治理工作台

Controller：`ZentideAdminGovernanceController`

| 方法 | 路径 | 功能 |
|---|---|---|
| POST | `/zentide/v1/admin/governance/feedback` | 记录管理员对治理结果的最终判断 |
| POST | `/zentide/v1/admin/governance/scan` | 批量扫描公开帖子并返回风险项 |
| POST | `/zentide/v1/admin/governance/results/pending` | 查询待处理治理结果 |
| POST | `/zentide/v1/admin/governance/rules/list` | 查询指定场景治理规则 |
| POST | `/zentide/v1/admin/governance/rules/all` | 查询全部治理规则 |
| POST | `/zentide/v1/admin/governance/rules` | 创建治理规则 |
| POST | `/zentide/v1/admin/governance/rules/{ruleId}` | 修改治理规则 |
| POST | `/zentide/v1/admin/governance/rules/{ruleId}/delete` | 删除治理规则 |
| POST | `/zentide/v1/admin/governance/internal/rules` | Agent 控制面读取规则 |
| POST | `/zentide/v1/admin/governance/internal/result` | Agent 控制面写入治理结果 |

### 5. 系统帖子类型管理

Controller：`ZentidePostTypeAdminController`

| 方法 | 路径 | 功能 |
|---|---|---|
| POST | `/zentide/v1/community/post-types/list` | 查询系统帖子类型 |
| POST | `/zentide/v1/community/post-types/create` | 创建系统帖子类型 |
| POST | `/zentide/v1/community/post-types/{postTypeId}/update` | 修改系统帖子类型 |
| POST | `/zentide/v1/community/post-types/{postTypeId}/delete` | 删除或停用系统帖子类型 |

## 三、阅读接口代码的统一方法

每个 Controller 接口都可以按下面的顺序理解：

1. 类上的 `@RequestMapping`：确定模块公共路径。
2. 方法上的 `@GetMapping` / `@PostMapping`：确定具体 HTTP 方法和子路径。
3. `@PathVariable`：读取 URL 路径中的资源 ID。
4. `@RequestParam`、`@ModelAttribute`、`@RequestBody`：读取请求参数。
5. `@Valid`、`@Positive`、`@Min`、`@Max`：执行基础参数校验。
6. `@GlobalInterceptor(checkLogin = true)`：执行用户登录校验。
7. `getTokenUserInfo().getUserId()`：从服务端 Session 获取当前用户身份，不能相信客户端传入的 authorId。
8. `service.xxx(...)`：进入业务层完成状态、权限、关联关系和事务处理。
9. `getSuccessResponseVO(...)`：包装统一响应结构。

Controller 主要负责 HTTP 适配；真正的业务规则应该继续追到 Service 和 Mapper XML 中。
