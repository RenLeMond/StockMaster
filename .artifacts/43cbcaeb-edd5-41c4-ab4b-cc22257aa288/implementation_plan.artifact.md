# 将 StockMaster Web 项目配置为 Android App (Capacitor)

本计划将使用 Capacitor 框架将现有的 React + Vite 项目包装成一个安卓应用。

## User Review Required

> [!IMPORTANT]
> 1. 本操作将安装新的 npm 依赖项（@capacitor/core, @capacitor/cli, @capacitor/android）。
> 2. 将在项目根目录下创建一个 `android` 文件夹，其中包含原生的安卓工程代码。
> 3. 您需要在本地安装有 Android SDK 和 Android Studio 以便进行后续的真机/模拟器调试。

## Proposed Changes

### [Dependencies]

#### [MODIFY] [package.json](file:///C:/Users/huayu/Desktop/github/StockMaster/package.json)
添加 Capacitor 相关依赖和脚本。

### [Capacitor Configuration]

#### [NEW] [capacitor.config.ts](file:///C:/Users/huayu/Desktop/github/StockMaster/capacitor.config.ts)
创建 Capacitor 配置文件，设置 App ID、名称及 Web 资源目录。

### [Android Platform]

#### [NEW] [android/](file:///C:/Users/huayu/Desktop/github/StockMaster/android/)
运行 `npx cap add android` 生成原生安卓模块。

## Verification Plan

### Automated Tests
- 运行 `npx cap sync` 检查同步是否正常。
- 检查 `android` 目录结构是否完整。

### Manual Verification
- 指导用户在 Android Studio 中打开 `android` 目录。
- 运行 `npm run build` 后，通过 `npx cap sync` 将代码同步到安卓工程。
- 在模拟器中启动应用。
