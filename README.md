# Android camera

Android CameraX image analysis demo with OpenCV4 and TesorFlow Lite

```
[Image sensor]-->[CameraX]-->[OpenCV4]--+-----------------------+
                     |                  |                       |
                     +------------------+--->[TensorFlow Lite]--+---> Final output
```

## Background and motivation

I think 4G/5G smart phones (or 4G/5G with Android-based cameras) with 4K/8K image sensors will replace the traditional PTZ monitoring cameras.

```
Traditional PTZ monitoring camera

                             Edge AI
  [Camera]---ONVIF/PoE---[Edge computer]---Ethernet---[Video recorder]---[Video management system]
  

Android-based monitoring cameras are cheaper and more flexible than the traditional ones.

    Edge AI
  [Smartphone]---4G/5G network---[Cloud storage]---[Video management app on cloud]
        |
 Direct communication over 4G/5G network
        |
  [Smartphone]
```

This project is just a skeleton of such an AI camera.


## Image processing filters with OpenCV4

- Color filter
- Optical flow
- Difference extraction

## TensorFlow Lite

- Object detection

## CameraX beta getting started

- https://codelabs.developers.google.com/codelabs/camerax-getting-started/

## opencv-4.4.0-android-sdk.zip

- https://sourceforge.net/projects/opencvlibrary/files/4.4.0/
