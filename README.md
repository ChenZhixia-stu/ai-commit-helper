# AI Commit Helper

AI Commit Helper 是一个 IntelliJ IDEA 插件，可以根据 Commit 窗口中当前勾选的代码改动生成中文 commit message，并自动回填到提交信息输入框。

## 功能

- 在 Commit 窗口的提交信息区域附近增加 `Generate AI Commit Message` 操作。
- 只读取当前准备提交、已勾选的变更文件。
- 使用 IntelliJ Platform VCS API 汇总代码改动，不依赖命令行 `git diff`。
- 调用 DeepSeek OpenAI-compatible Chat Completions 接口生成提交信息。
- API Key 存储在 IDEA PasswordSafe 中，URL、模型等普通配置保存为全局设置。
- 生成格式如下：

```text
优化私信功能

- 调整私信列表展示逻辑
- 修复会话未读状态更新问题
- 补充消息接口异常处理
```

## 本地开发

本项目按需求使用 Maven + Java 开发。JetBrains 官方更推荐 Gradle 开发 IntelliJ Platform 插件，因此 Maven 方案依赖本机已安装的 IDEA SDK。

默认 IDEA 安装目录：

```text
F:/software/IntelliJ IDEA 2026.1
```

编译：

```bash
mvn -q -DskipTests compile
```

如果使用 IDEA 2026.1 的 SDK 编译，需要用 JDK 21 或更高版本的 `javac`，因为 IDEA 2026.1 的 SDK jar 使用 Java 21 字节码。当前 `pom.xml` 已默认 fork 使用：

```text
F:/jdk22/bin/javac
```

所以即使当前命令行的 `mvn` 运行在 Java 8 上，也可以直接执行：

```bash
mvn -q package
```

如果你的高版本 JDK 不在 `F:\jdk22`，可以这样覆盖：

```bash
mvn -q package -Dbuild.jdk.home="D:/Java/jdk-22"
```

如果 IDEA 安装目录不同，可以通过 `-Didea.home` 覆盖：

```bash
mvn -q -DskipTests compile -Didea.home="D:/Apps/IntelliJ IDEA"
```

运行测试：

```bash
mvn -q test
```

## 打包成可安装插件

先打出插件 jar：

```bash
mvn -q package
```

产物位置：

```text
target/ai-commit-helper-1.0.0-SNAPSHOT.jar
```

IDEA 支持从磁盘安装插件 zip。zip 不能只是简单包含一个 jar，推荐使用如下标准结构：

```text
AI Commit Helper/
  lib/
    ai-commit-helper-1.0.0-SNAPSHOT.jar
```

可以用下面的 PowerShell 命令生成可分发 zip：

```powershell
$version = '1.0.0-SNAPSHOT'
$staging = 'target\dist\AI Commit Helper'
$dist = "target/ai-commit-helper-$version.zip"

if (Test-Path 'target\dist') { Remove-Item -LiteralPath 'target\dist' -Recurse -Force }
New-Item -ItemType Directory -Force -Path "$staging\lib" | Out-Null
Copy-Item -LiteralPath "target/ai-commit-helper-$version.jar" -Destination "$staging/lib/ai-commit-helper-$version.jar" -Force
Compress-Archive -Path 'target\dist\AI Commit Helper' -DestinationPath $dist -Force
```

也可以直接运行项目内置脚本，它会自动执行 Maven 打包并生成标准插件 zip：

```powershell
.\package-plugin.ps1
```

如果你的高版本 JDK 不在 `F:\jdk22`，运行脚本前设置：

```powershell
$env:AI_COMMIT_HELPER_JDK_HOME='D:\Java\jdk-22'
.\package-plugin.ps1
```

生成的 zip：

```text
target/ai-commit-helper-1.0.0-SNAPSHOT.zip
```

别人安装时：

1. 打开 IDEA。
2. 进入 `Settings | Plugins`。
3. 点击齿轮按钮。
4. 选择 `Install Plugin from Disk...`。
5. 选择 `ai-commit-helper-1.0.0-SNAPSHOT.zip`。
6. 重启 IDEA。
7. 进入 `Settings | Tools | AI Commit Helper` 配置 DeepSeek URL、模型和 API Key。

## 兼容性

- `plugin.xml` 中配置 `since-build="221"`，目标为 IDEA 2022.1 及以后版本。
- 源码和目标字节码为 Java 8。
- 正式发布前建议使用 IDEA Plugin DevKit 或 JetBrains Plugin Verifier 分别验证 IDEA 2022.1、2022.3、2023.3、2024.x 和最新版 IDEA。

## 配置项

配置入口：

```text
Settings | Tools | AI Commit Helper
```

可配置内容：

- Base URL：默认 `https://api.deepseek.com`
- Model：默认 `deepseek-v4-flash`
- API Key：存储在 IntelliJ PasswordSafe
- Timeout Seconds：请求超时时间
- Max Diff Characters：发送给模型的最大 diff 字符数
- Language：生成语言，默认中文

插件只会把 Commit 窗口中当前勾选的代码改动发送到配置的 API URL。
