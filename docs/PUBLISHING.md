# 上传 GitHub

建议仓库名：`sony-keyfix`。建议简介：

> No-root Bluetooth button repair for Sony X9500H and RMF-TX910C. Runs locally on the TV.

建议 topics：`android-tv`、`sony-bravia`、`bluetooth-remote`、`adb`、`key-remapping`。

## 仓库内容

上传本源码目录或解压 source.zip 得到的 `sony-keyfix` 目录内容。README 应位于仓库根目录。保留 `.github`、`.gitignore` 等隐藏文件。

不要上传整个开发工作目录。APK 放在 Release 附件，不放入源码 Git 历史；也不要上传 build/、dist/、SDK、原始设备日志、签名密钥或 ADB 密钥。

新建空 GitHub 仓库后，从本源码根目录运行以下命令。先把远程地址替换为你创建的真实仓库地址；仓库已初始化时无需重复 `git init`。

```sh
git init -b main
git add .
git diff --cached --stat
git commit -m "Release Sony Keyfix 1.0.0"
git remote add origin YOUR_GITHUB_REPOSITORY_URL
git push -u origin main
```

若 Git 尚未设置作者身份，请使用你自己的姓名与 GitHub 隐私邮箱配置，不要采用其他人的身份。推送前检查暂存列表。

## 发布 1.0.0

1. 等待首次 Actions 构建成功；它的 APK 是未签名产物，不能当安装包分发。
2. 创建 `v1.0.0` 标签，对应本次源码提交。
3. 手动创建 GitHub Release，标题 **Sony Keyfix 1.0.0**。
4. 附上已签名 `Sony-Keyfix-1.0.0.apk`、`Sony-Keyfix-1.0.0-source.zip` 和 `SHA256SUMS.txt`。
5. 发布说明可采用 CHANGELOG 的 1.0.0 段落，并链接兼容文档。明确支持 X9500H / Android 9 / RMF-TX910C，保留尚未验证的范围。

仓库没有自动上传、自动签名或自动公开发布步骤。可以在 GitHub 的安全设置中启用 private vulnerability reporting，为安全报告提供私下渠道。
