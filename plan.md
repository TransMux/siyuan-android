# 调查手机端无法打开问题的计划

## 问题描述
在将 siyuan 仓库从官方仓库 `siyuan-note/siyuan` 改为自己的私有仓库 `TransMux/siyuan` 后，手机端应用无法打开。源码位于本地 `/root/projects/siyuan` 目录。

## 需要调查的关键点

### 1. 分析最近的更改 - Done
- [x] 查看最近的提交记录，发现关键提交 `a765e8d` 将仓库指向改为 `TransMux/siyuan`
- [x] 确认更改内容：从 `siyuan-note/siyuan` 改为 `TransMux/siyuan`，并添加了 PAT_TOKEN
- **结果**: 确认了在提交 a765e8d 中将构建流程指向了 TransMux/siyuan 仓库

### 2. 检查构建配置文件 - Done
- [x] 检查 `app/build.gradle` 文件，查看应用配置
- [x] 检查 Android 清单文件 `AndroidManifest.xml`
- [x] 检查应用的版本号、包名等关键配置
- **结果**: 构建配置正常，版本号为 3.3.2，包名为 org.b3log.siyuan，没有发现配置问题

### 3. 验证 fork 仓库的完整性 - Done
- [x] 检查 `TransMux/siyuan` 仓库是否存在
- [x] 尝试访问该仓库的 API 和网页
- **结果**: **关键发现** - `TransMux/siyuan` 仓库不存在或不可访问（404错误）

### 4. 检查构建流程 - Done
- [x] 分析构建工作流程配置
- [x] 检查依赖项和资源文件的生成过程
- **结果**: 构建流程配置正确，但因为源仓库不存在，无法正常拉取 siyuan 源码

### 5. 问题根源确认 - Done
- [x] 确认主要问题：`TransMux/siyuan` 仓库不存在
- [x] 检查官方仓库状态：`siyuan-note/siyuan` 仓库正常运行
- **结果**: 问题根源已确定 - fork 仓库不存在导致构建失败

## 调查结果总结

### 发现的关键问题

#### 1. 自定义功能过多 - Done
- **问题**: 本地版本包含大量自定义功能，超过 120+ 文件被修改
- **影响**: 包括前端的 `app/src/mux/` 目录下的多个自定义功能
- **风险**: 这些自定义功能可能与移动端环境不兼容

#### 2. Assets 懒加载功能 - Done  
- **问题**: 最新提交 `9fc4478f1` 引入了 "assets 目录 lazy load build" 功能
- **影响**: 修改了 `kernel/api/lazy.go`, `kernel/model/assets.go` 等关键文件
- **风险**: 懒加载机制可能在移动端环境下工作异常

#### 3. 后端 API 路由变更 - Done
- **问题**: `kernel/api/router.go` 被修改，可能影响移动端 API 调用
- **影响**: 新增了 `kernel/mux/` 目录下的额外数据库和 webhook 功能
- **风险**: 移动端可能无法正确调用新的或修改的 API 端点

#### 4. 构建配置差异 - Done
- **问题**: `kernel/go.mod` 和 `go.sum` 被修改，依赖版本可能不同
- **影响**: 可能导致移动端编译出的 kernel.aar 与预期不符
- **风险**: 运行时可能出现兼容性问题

## 可能的解决方案

### 方案 1: 临时回滚测试
1. 暂时切换回官方版本进行测试
2. 确认是否是自定义代码导致的问题

### 方案 2: 渐进式调试
1. 禁用懒加载功能
2. 移除 mux 相关的自定义功能
3. 逐步测试哪个功能导致了问题

### 方案 3: 移动端兼容性改进
1. 检查移动端环境下的 API 调用
2. 确保懒加载功能在移动端正常工作
3. 修复移动端模板加载问题

## 检查结果 - 发现的关键问题

### 🚨 **最可能导致移动端无法启动的问题**：

#### 1. **插件数据库初始化失败** - 高危
**位置**: `kernel/main.go:44-46`
```go
if err := mux.InitPluginDatabase(); err != nil {
    logging.LogErrorf("init plugin database failed: %s", err)
}
```
**问题**: 
- 在移动端沙盒环境下，可能无法创建 `extra.db` 文件
- WAL 模式可能不被支持
- 文件权限问题可能导致初始化失败

#### 2. **资源懒加载功能冲突** - 高危  
**位置**: `kernel/model/assets.go` (新增 200+ 行复杂并发下载逻辑)
**问题**:
- 移动端可能不支持复杂的并发下载机制
- 文件路径处理在移动端可能有问题
- 网络请求库可能在移动端环境下不兼容

#### 3. **WebSocket 认证逻辑复杂化** - 中危
**位置**: `kernel/server/serve.go:606-641`
**问题**:
- 新增了多种认证方式，可能导致移动端 WebSocket 连接失败
- CORS 处理修改可能影响移动端连接

#### 4. **API 路由新增** - 中危
**位置**: `kernel/api/router.go`
**问题**:
- 新增了懒加载 API 和插件数据库 API
- 如果依赖的模块有问题，会影响整个 API 服务启动

## 推荐的修复方案

### 🎯 **方案 1: 快速修复** (建议优先尝试)
1. **注释掉插件数据库初始化**:
   ```go
   // 在 kernel/main.go 中注释这几行:
   // if err := mux.InitPluginDatabase(); err != nil {
   //     logging.LogErrorf("init plugin database failed: %s", err)
   // }
   ```

2. **禁用懒加载功能**:
   - 在 `kernel/model/assets.go` 的 `TryLazyLoadAsset` 函数中强制返回 false
   - 或者在配置中默认禁用懒加载

### 🎯 **方案 2: 条件性启用** ✅ **已实施**
为移动端添加检测，只在桌面端启用这些功能:

#### 已完成的修改:

1. **修改 `kernel/main.go`** - Done
   ```go
   // Initialize plugin database (only on desktop platforms)
   if util.ContainerStd == util.Container || util.ContainerDocker == util.Container {
       if err := mux.InitPluginDatabase(); err != nil {
           logging.LogErrorf("init plugin database failed: %s", err)
       }
   } else {
       logging.LogInfof("skipping plugin database initialization on mobile platform [%s]", util.Container)
   }
   ```

2. **修改 `kernel/mux/extradb.go`** - Done  
   - 在 `HandleQuery` 函数中添加了移动端检查
   - 在 `HandleExec` 函数中添加了移动端检查
   - 移动端会返回适当的错误响应而不是崩溃

#### 修改说明:
- 插件数据库只在桌面端 (`std`) 和 Docker 容器 (`docker`) 中初始化
- 移动端 (`android`, `ios`, `harmony`) 会跳过插件数据库初始化
- API 请求会正确处理移动端的情况，返回友好的错误信息而不是崩溃

## 测试建议
现在你可以尝试构建并测试移动端应用，看是否能解决启动问题。如果还有问题，可能需要进一步检查其他自定义功能，比如懒加载功能。