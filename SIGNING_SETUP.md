# 签名配置说明

## 概述

该项目现在支持生成签名的Release版APK。签名密钥已生成并添加到`.gitignore`中以确保安全。

## 签名密钥信息

- **密钥库文件**: `siyuan-release.keystore`
- **密钥库密码**: `siyuan123456`  
- **密钥别名**: `siyuan`
- **密钥密码**: `siyuan123456`
- **有效期**: 10000天 (约27年)

## GitHub Actions配置

### 方式1：使用临时签名 (当前默认)
- 工作流会自动生成临时签名密钥
- 适合开发和测试环境
- 每次构建使用不同的签名

### 方式2：使用生产签名 (推荐用于发布)
1. 将`siyuan-release.keystore`文件进行base64编码：
   ```bash
   base64 -i siyuan-release.keystore
   ```

2. 在GitHub仓库设置中添加Secret：
   - 名称: `KEYSTORE_BASE64`
   - 值: 上一步得到的base64字符串

3. 工作流将自动使用生产签名密钥

## 构建产物

GitHub Actions现在会生成：

### Debug版本 (开发测试)
- `siyuan-*-debug-*.apk` (cn渠道)
- `siyuan-*-debug-*.apk` (official渠道)

### Release版本 (发布版本)
- `siyuan-*-release.apk` (cn渠道) - **签名版本**
- `siyuan-*-release.apk` (official渠道) - **签名版本**

## 本地构建

如果需要本地构建release版本：

1. 确保`siyuan-release.keystore`文件在项目根目录
2. 确保`signings.gradle`文件存在
3. 运行构建命令：
   ```bash
   ./gradlew assembleCnRelease assembleOfficialRelease
   ```

## 安全注意事项

- ✅ 签名密钥文件已添加到`.gitignore`
- ✅ 不会提交到版本控制系统
- ✅ 仅通过GitHub Secrets安全传递
- ⚠️  本地开发时请妥善保管密钥文件

## 发布流程

1. **开发测试**: 使用debug版本进行开发和内部测试
2. **预发布**: 使用临时签名的release版本进行预发布测试
3. **正式发布**: 配置生产签名后构建最终release版本

---

**重要**: 生产环境的签名密钥应该妥善保管，不要泄露给未授权人员。