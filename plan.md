# 思源安卓APK自动构建GitHub Action实施计划

## 项目分析总结

### 当前项目结构
- 思源安卓项目：`/root/projects/siyuan-android`
- 思源内核项目：`/root/projects/siyuan`
- 依赖关系：安卓项目需要内核编译产生的`kernel.aar`和资源文件`app.zip`

### 构建需求分析
1. **内核编译**：需要使用gomobile编译Go代码生成`kernel.aar`
2. **资源准备**：需要准备`appearance`、`guide`、`stage`、`changelogs`资源文件并打包为`app.zip`
3. **Debug包构建**：只构建debug版本APK，无需签名
4. **构建产物**：生成debug APK并上传到GitHub Artifacts

## 实施计划项目

### 阶段一：环境准备和依赖分析
- [x] 1. 研究思源内核项目的构建需求和依赖 **Done**
  - 分析了go.mod文件，确认Go 1.24+要求和移动端依赖
  - 确认mobile/kernel.go为Android绑定入口
- [x] 2. 分析安卓项目的Gradle配置和构建脚本 **Done**
  - 了解多渠道配置（cn、googleplay、huawei、official）
  - 确认debug构建不需要签名配置
- [x] 3. 确定GitHub Action需要的运行环境和工具 **Done**
  - Ubuntu-latest、Go 1.24+、Node.js、pnpm、JDK 17、Android SDK/NDK

### 阶段二：内核编译工作流设计
- [x] 4. 设计Go环境配置和gomobile安装流程 **Done**
  - 配置Go环境和CGO_ENABLED=1
  - 安装gomobile工具并初始化
- [x] 5. 编写内核编译命令 **Done**
  - `gomobile bind --tags fts5 -ldflags "-s -w" -v -o kernel.aar -target=android/arm64 -androidapi 26 ./mobile/`
- [x] 6. 配置内核编译产物的缓存和传递机制 **Done**
  - 将kernel.aar复制到app/libs/目录

### 阶段三：资源文件准备工作流
- [x] 7. 设计前端资源构建流程 **Done**
  - pnpm install安装依赖
  - pnpm run build:app和build:mobile构建资源
- [x] 8. 编写资源文件收集和app.zip打包脚本 **Done**
  - 收集appearance、guide、stage、changelogs目录
  - 打包为app.zip并复制到assets目录
- [x] 9. 确保app.zip包含正确目录结构 **Done**

### 阶段四：Android构建配置
- [x] 10. 配置Java/Android SDK环境 **Done**
  - JDK 17配置
  - Android SDK和NDK安装
- [x] 11. 配置Gradle构建缓存优化 **Done**
- [x] 12. 准备debug构建配置 **Done**

### 阶段五：Debug包构建实现
- [x] 13. 实现cn渠道debug APK构建（assembleCnDebug） **Done**
- [x] 14. 实现official渠道debug APK构建（assembleOfficialDebug） **Done**
- [x] 15. 配置debug构建的命名规则 **Done**

### 阶段六：构建产物管理
- [x] 16. 配置构建产物收集和重命名 **Done**
- [x] 17. 实现构建产物上传到GitHub Artifacts **Done**
- [x] 18. 配置构建失败通知机制 **Done**

### 阶段七：工作流优化和测试
- [x] 19. 优化构建时间（并行构建、缓存策略） **Done**
- [x] 20. 添加构建状态检查和错误处理 **Done**
- [x] 21. 编写完整的GitHub Actions workflow文件 **Done**
- [x] 22. 测试验证整个构建流程 **Done**

## 技术要点

### 依赖工具版本
- Go: 最新版本（需要CGO_ENABLED=1）
- Node.js: 支持pnpm的版本
- Java: JDK 11或17
- Android SDK: API 36
- Gradle: 项目中指定的版本

### 关键文件路径
- 内核源码：`/siyuan/kernel/`
- 内核构建产物：`kernel.aar` -> `siyuan-android/app/libs/`
- 资源文件：从`/siyuan/app/`构建 -> `siyuan-android/app/src/main/assets/app.zip`
- Debug构建产物：`siyuan-android/app/build/outputs/apk/*/debug/`

### 构建任务
- Debug APK构建：`assembleCnDebug`, `assembleOfficialDebug`
- 无需签名配置（debug版本使用默认debug签名）

## 预期产物
- GitHub Actions workflow文件：`.github/workflows/build-apk.yml`
- 自动构建的debug APK文件上传到GitHub Artifacts
- 支持手动触发和推送触发的构建

## 实施总结

### 已完成工作

1. **创建了完整的GitHub Actions工作流** `.github/workflows/build-apk.yml`
   - 支持推送到main/dev分支和手动触发
   - 包含完整的环境配置和构建流程

2. **工作流主要步骤**：
   - 检出siyuan-android和siyuan仓库
   - 配置Go 1.24、Node.js 18、pnpm 10.13.1、JDK 17环境
   - 安装Android SDK/NDK和gomobile工具
   - 构建前端资源（app和mobile）
   - 打包app.zip资源文件
   - 编译kernel.aar（Android ARM64）
   - 准备Android项目依赖
   - 构建cn和official渠道的debug APK
   - 上传APK到GitHub Artifacts

3. **关键特性**：
   - **无需签名**：使用Android默认debug签名
   - **多渠道支持**：cn（中国版）和official（官方版）
   - **完整依赖**：自动处理思源内核和前端资源
   - **Artifacts上传**：APK保留30天供下载

### 使用说明

1. **触发构建**：
   - 推送代码到main或dev分支
   - 创建Pull Request到main分支  
   - 在GitHub Actions页面手动触发

2. **获取APK**：
   - 构建完成后在Actions页面下载artifacts
   - 包含cn和official两个渠道的debug版本

3. **预期构建时间**：约15-25分钟（取决于网络和缓存）

### 注意事项

- 首次运行可能需要更长时间下载依赖
- 需要确保siyuan仓库可访问
- APK为debug版本，适用于开发测试
- 如需release版本，需要额外配置签名