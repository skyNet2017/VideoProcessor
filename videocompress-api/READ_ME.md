



[VideoProcessor](https://github.com/yellowcath/VideoProcessor)使用Android原生的MediaCodec实现视频压缩、剪辑、混音、快慢放及倒流的功能（快慢放及倒流支持音频同步变化），在支持MediaCodec的手机上优于使用FFmpeg的方案

- **体积小** ：编译后的aar只有262K，ffmpeg一个so就7、8M，精简之后也差不多还有一半大小
- **速度快** ：在huaweiP9上压缩(1080P 20s 20000k -> 720p 2000k)

| lib               | 耗时  |
| ----------------- | ----- |
| VideoProcessor    | 13.3s |
| ffmpeg            | 172s  |
| ffmpeg(ultrafast) | 74s   |

但是在有的手机上,mediacodec压缩出来的视频文件头有问题,只有自己本机能播放,其他设备,webview无法播放,此时,需要切换到ffmpeg.

此时有问题的视频表现为: 

文件大小>0, 视频长宽和时长均获取不到,为0.

更大原因不是mediacodec兼容,而是回调不准确,判断结束的条件不准确:

![image-20241023104135784](https://cdn.jsdelivr.net/gh/shuiniuhss/myimages@main/imagemac3/image-20241023104135784.png)

通过日志可以看到,progress=1.0只是压缩进度,压缩完成后,还在写mp4文件头,音频等信息

![image-20241023104252097](https://cdn.jsdelivr.net/gh/shuiniuhss/myimages@main/imagemac3/image-20241023104252097.png)



## 于是兼容策略如下:

默认使用mediacodec压缩,压缩后校验其压缩后视频长宽和时长,

如果都为0, 则本次回调失败,并在本地记录: 本机无法使用mediacodec压缩

后续所有的压缩都使用ffmpeg

