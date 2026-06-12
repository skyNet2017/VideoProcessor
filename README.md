# VideoProcessor

[VideoProcessor](https://github.com/yellowcath/VideoProcessor)使用Android原生的MediaCodec实现视频压缩、剪辑、混音、快慢放及倒流的功能（快慢放及倒流支持音频同步变化），在支持MediaCodec的手机上优于使用FFmpeg的方案

- **体积小** ：编译后的aar只有262K，ffmpeg一个so就7、8M，精简之后也差不多还有一半大小
- **速度快** ：在huaweiP9上压缩(1080P 20s 20000k -> 720p 2000k)

lib | 耗时
------ | ------
VideoProcessor| 13.3s
ffmpeg | 172s
ffmpeg(ultrafast) | 74s

### Gradle
在根目录下的build.gradle里添加maven仓库
``` groovy
allprojects {
		repositories {
			...
			maven { url 'https://www.jitpack.io' }
		}
	}
```
添加依赖
要求minSdkVersion 21
``` groovy

	dependencies {
	        implementation 'com.github.yellowcath:VideoProcessor:2.4.2'
	}
```

## Changelog

2.4.2
* 解决arm64的机器上crash的问题
* 解决某些情况下进度计算错误的问题

2.4.0
* 兼容Android Q
* 去掉对support lib的依赖

## 使用
基本用法如下
``` java
    VideoProcessor.processor(context)
            .input(inputVideoPath)
            .output(outputVideoPath)
            .outWidth(outWidth)
            .outHeight(outHeight)
            .process();
```
VideoProcessor支持多种参数同时处理，完整参数如下
``` java
VideoProcessor.processor(context)
       .input(inputVideoPath) // .input(inputVideoUri)
       .output(outputVideoPath)
       //以下参数全部为可选
       .outWidth(width)
       .outHeight(height)
       .startTimeMs(startTimeMs)//用于剪辑视频
       .endTimeMs(endTimeMs)    //用于剪辑视频
       .speed(speed)            //改变视频速率，用于快慢放
       .changeAudioSpeed(changeAudioSpeed) //改变视频速率时，音频是否同步变化
       .bitrate(bitrate)       //输出视频比特率
       .frameRate(frameRate)   //帧率
       .iFrameInterval(iFrameInterval)  //关键帧距，为0时可输出全关键帧视频（部分机器上需为-1）
       .progressListener(listener)      //可输出视频处理进度
       .process();
```
用户使用时可自行根据需要调用，例如
``` java
    //视频两倍速
    VideoProcessor.processor(context)
            .input(inputVideoPath)
            .output(outputVideoPath)
            .speed(2.0f)
            .process();
```
此外，其它功能
``` java
//视频逆序
VideoProcessor.reverseVideo(context, inputVideoPath, outputVideoPath,reverseAudio,listener);
//混音,支持渐入渐出
VideoProcessor.mixAudioTrack(context, inputVideoPath, aacAudioPath, outputVideoPath, startMs, endMs, videoVolume, audioVolume,fadeInSec, fadeOutSec);
```

## Demo
![enter image description here](https://github.com/yellowcath/VideoProcessor/raw/master/readme/VideoProcessorDemo.png)

---

## VideoCompress API（videocompress-api / videocompress-api-ffmpeg）

在 `VideoProcessor` 底层能力之上，新增两层封装，提供**开箱即用的视频压缩 API**：统一入口、串行队列、比特率策略、进度回调，以及 MediaCodec / FFmpeg 自动降级。

### 模块说明

| 模块 | 职责 | 依赖 |
|------|------|------|
| `videocompress-api` | 压缩入口 `VideoCompressUtil`、任务队列、比特率计算、进度对话框、默认 `MediaCodecCompressImpl` | `videoprocessor`、`utilcodex` |
| `videocompress-api-ffmpeg` | FFmpeg 压缩实现 `FFmpegCompressImpl`（基于 RxFFmpeg），供 MediaCodec 不兼容时降级 | `videocompress-api`、`RxFFmpeg` |

- 只需 MediaCodec 压缩：依赖 `videocompress-api` 即可。
- 需要 FFmpeg 降级：额外依赖 `videocompress-api-ffmpeg`（通过反射加载，未引入时降级不可用）。

### 设计思路

```
VideoCompressUtil（统一入口）
    │
    ├─ CompressTaskQueue        串行队列，同时只跑一个任务
    ├─ CompressHepler           解析原视频信息，计算目标分辨率/码率
    ├─ IBitrateConfig           可插拔的码率策略（默认 LowThanBiliBitrateConfig）
    │
    └─ ICompressor（可替换）
           ├─ MediaCodecCompressImpl（默认）→ 内部调用 VideoProcessor
           └─ FFmpegCompressImpl（可选模块）→ RxFFmpeg 命令行压缩
```

**核心设计原则：**

1. **默认快、按需稳**：优先走 MediaCodec（体积小、速度快）；仅在输出校验失败或本机历史记录不可用时，才切换到 FFmpeg。
2. **接口隔离**：`ICompressor` 抽象具体压缩引擎，`videocompress-api` 不硬依赖 FFmpeg，避免强耦合和大体积 so。
3. **串行队列**：多视频同时提交时 FIFO 排队，避免并发压缩占满 CPU/内存；支持取消尚未开始的任务。
4. **智能跳过**：若原视频分辨率/码率已低于目标，直接 `onFinish` 返回原路径，不做无意义重编码。
5. **输出校验**：MediaCodec 在部分机型上 `progress=1.0` 只代表编码结束，MP4 文件头可能尚未写完；因此在 `onFinish` 后延迟 500ms 再读取宽高/时长，三者均为空则判定失败并触发 FFmpeg 重试。
6. **进度对话框（仅 Debug）**：`showCompressProgressDialog` 在 debug 包下可展示任务列表；用户手动关闭后，内部 `onProgress` / 队列清理不再自动弹出，仅在下一次 `compressAsync` 或主动 `showCompressTaskListDialog` 时恢复。

### Gradle 依赖

本地模块：

```groovy
dependencies {
    implementation project(':videocompress-api')
    // 需要 FFmpeg 降级时再加：
    implementation project(':videocompress-api-ffmpeg')
}
```

JitPack 发布时，模块名与版本号以实际 tag 为准。

### 初始化

```java
// Application 或首个使用页面
VideoCompressUtil.init(context, showCompressProgressDialog, showCompareAfterCompress);
// showCompressProgressDialog：仅 debug 包生效，控制是否弹出任务列表对话框
// showCompareAfterCompress：已废弃，保留兼容

// 可选：压缩完成后点击任务项的预览行为
VideoCompressUtil.setiPreviewVideo((context, path) -> {
  // 例如用系统播放器打开
});

// 可选：全局码率策略
VideoCompressUtil.setGlobalBitRateConfig(new BilibiliBitrateConfig());

// 可选：强制使用 FFmpeg（一般不需要，失败时会自动降级）
// VideoCompressUtil.setCompressor(new FFmpegCompressImpl());
```

### 基本用法

```java
VideoCompressUtil.doCompressAsync(
    inputPath,                      // 输入视频绝对路径
    null,                           // 输出目录，null 则写到 cache/videoCompress/
    CompressType.TYPE_SDR_720P,     // 目标档位，见下表
    new ICompressListener() {
        @Override
        public void onStart(String input, String outPath) { }

        @Override
        public void onProgress(int progress, long progressTime) { }

        @Override
        public void onFinish(String outputFilePath) { }

        @Override
        public void onError(String message) { }

        @Override
        public void onCancel() { }
    }
);
```

同步压缩（阻塞当前线程，队列内仍串行）：

```java
VideoCompressUtil.doCompress(false, inputPath, outDir, CompressType.TYPE_SDR_1080P, listener);
```

### 压缩档位（CompressType）

| 常量 | 目标短边分辨率 |
|------|----------------|
| `TYPE_SDR_360P` | 360 |
| `TYPE_SDR_480P` | 480 |
| `TYPE_SDR_720P` | 720 |
| `TYPE_SDR_1080P` | 1080 |
| `TYPE_HDR_720P` | 720 |
| `TYPE_HDR_1080P` | 1080 |
| `TYPE_HDR_2K` | 1440 |
| `TYPE_HDR_4K` | 2160 |
| `TYPE_FOR_STORE` | 2160 |

实际输出宽高由 `CompressHepler.getRealTargetWHBitrate()` 结合原视频比例与 `IBitrateConfig` 计算。

### 队列与取消

```java
// 当前是否有任务在执行
VideoCompressUtil.isCompressing();

// 执行中 + 排队中的任务总数
VideoCompressUtil.getPendingTaskCount();

// 取消尚未开始、且输入路径匹配的任务
VideoCompressUtil.cancel(filePath);
```

多选视频时，对每个路径分别调用 `doCompressAsync` 即可，队列自动串行处理。Demo 见 `VideoCompressDemoActivity`。

### 进度对话框（Debug）

```java
// 在 compress 时 init(..., true, ...) 会自动注册任务并弹窗
VideoCompressUtil.init(activity, true, false);
VideoCompressUtil.doCompressAsync(...);

// 或手动打开任务列表（仅 debug 包）
VideoCompressUtil.showCompressTaskListDialog(activity);
```

用户 dismiss / 点外部关闭后，后续进度更新不会再自动弹出；下一次 `doCompressAsync` 或主动调用 `showCompressTaskListDialog` 会重新显示。

### MediaCodec / FFmpeg 兼容策略

部分机型 MediaCodec 压缩产物存在**文件头异常**：文件大小 > 0，但宽高、时长读出来均为 0，仅本机可播，WebView / 其他设备无法播放。根因往往是 `progress=1.0` 时 MP4 元数据尚未写完，而非单纯的编解码器兼容问题。

**策略：**

1. 默认 `MediaCodecCompressImpl`（内部 `VideoProcessor`）。
2. `onFinish` 后延迟 500ms，用 `MediaMetadataRetriever` 校验输出文件的宽高、时长。
3. 校验失败 → 本机写入 `video_compress_mediacodec_compact=not_compact`，并反射加载 `FFmpegCompressImpl` 重试当前任务；之后本机所有压缩走 FFmpeg。
4. 未引入 `videocompress-api-ffmpeg` 时，降级路径不可用，校验失败会 `onError`。

手动指定引擎：

```java
VideoCompressUtil.setCompressor(new FFmpegCompressImpl()); // 需依赖 ffmpeg 模块
```

### 扩展点

| 接口 | 用途 |
|------|------|
| `ICompressor` | 自定义压缩实现（替换 MediaCodec / FFmpeg） |
| `IBitrateConfig` | 自定义目标码率计算（如 `BilibiliBitrateConfig`、`YoutubeBitrateConfig`） |
| `IPreviewVideo` | 任务列表点击完成项时的预览行为 |
| `ICompressListener` | 压缩生命周期回调 |

`videocompress-api-ffmpeg` 内还提供按场景预设的 `CompressorConfig` 实现（`Config720pUpload`、`Config1080Upload`、`BilibiliUpload`、`ConfigLocalStore` 等），供 `FFmpegCompressImpl` 组装 FFmpeg 命令行参数。


