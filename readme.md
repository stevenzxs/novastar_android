**demo工程环境**：\
android studio：现代版本（支持 AGP 8.x）\
jdk 17 \
gradle：8.7（AGP 8.5.2）\
compileSdk 34，minSdk 21，targetSdk 34 \
abi：armeabi-v7a,arm64-v8a

**demo发布节目操作步骤**：
1. 导入项目到android studio 后，连接手机和电脑，并且连接手机和T卡于同一wifi下。
2. 修改代码中的 sn 号和 密码。
2. 运行 app，授予文件读写权限，然后使用 adb 命令将测试图片 4.png 放到 /storage/emulated/0/Android/data/com.qht.jnatest/files/demotest 目录下：

```
adb push 4.png /storage/emulated/0/Android/data/com.qht.jnatest/files/demotest
```
此目录对应代码中：getDiskCacheDir() 目录。

4. 点击开始按钮，直到发布节目结束。
