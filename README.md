<div align="center">

<img src="assets/logo_2.png" alt="FinalBenchmark 2 Logo" width="200"/>

# FinalBenchmark 2

<a href="https://f-droid.org/packages/com.ivarna.finalbenchmark2">
  <img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="80"/>
</a>

---

[![Discord](https://img.shields.io/badge/Discord-Join_Server-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/khzKmGzfRf)
[![Downloads](https://img.shields.io/github/downloads/abhay-byte/finalbenchmark-platform/total?style=for-the-badge&logo=github&logoColor=white&labelColor=24292e&color=success)](https://github.com/abhay-byte/finalbenchmark-platform/releases)
[![Stars](https://img.shields.io/github/stars/abhay-byte/finalbenchmark-platform?style=for-the-badge&logo=github&logoColor=white&labelColor=24292e&color=yellow)](https://github.com/abhay-byte/finalbenchmark-platform/stargazers)
<br>
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=for-the-badge&logo=apache&logoColor=white)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-Compose-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Latest Release](https://img.shields.io/github/v/release/abhay-byte/finalbenchmark-platform?style=for-the-badge&logo=github&logoColor=white&labelColor=24292e&color=blue)](https://github.com/abhay-byte/finalbenchmark-platform/releases/latest)

A comprehensive Android CPU, GPU, RAM, Storage, AI/ML, and Productivity benchmarking application built with Jetpack Compose and C++/Native JNI libraries.

<br/>
<img src="docs/storelisting/store_listing_slideshow_lowq.gif" alt="FinalBenchmark 2 Slideshow" width="800" style="border-radius: 12px;"/>

</div>

---

## 📱 Screenshots

<div align="center">
  <table>
    <tr>
      <td align="center">
        <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="180"/>
        <br/>
        <sub><b>Home Screen</b></sub>
      </td>
      <td align="center">
        <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="180"/>
        <br/>
        <sub><b>Device Info</b></sub>
      </td>
      <td align="center">
        <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width="180"/>
        <br/>
        <sub><b>Results</b></sub>
      </td>
      <td align="center">
        <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" width="180"/>
        <br/>
        <sub><b>Running Tests</b></sub>
      </td>
    </tr>
    <tr>
      <td align="center">
        <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/5.png" width="180"/>
        <br/>
        <sub><b>History</b></sub>
      </td>
      <td align="center">
        <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/6.png" width="180"/>
        <br/>
        <sub><b>Rankings</b></sub>
      </td>
      <td align="center">
        <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/7.png" width="180"/>
        <br/>
        <sub><b>Settings</b></sub>
      </td>
      <td align="center">
        <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/8.png" width="180"/>
        <br/>
        <sub><b>CPU Comparison</b></sub>
      </td>
    </tr>
  </table>
</div>

---

## ⚡ Functionality Overview

| Feature Category | Implementation Status | Description |
| :--- | :--- | :--- |
| **CPU Benchmarks** | **Fully Implemented** | 10 workload algorithms executed in Single-Core and Multi-Core configurations. |
| **AI/ML Benchmarks** | **Implemented (Compute Kernels)** | 5 native high-performance compute kernels (GEMM, Conv2D, Activation, Quantized GEMM, Attention). |
| **GPU Native Benchmarks** | **Fully Implemented** | OpenGLES and Vulkan native rendering/compute shaders. |
| **GPU External Engine** | *Docs / Architecture* | Architectural specs for deep-linked external engine benchmarks. |
| **RAM Benchmarks** | **Fully Implemented** | 5 native memory bandwidth, latency, and copy operations. |
| **Storage Benchmarks** | **Fully Implemented** | 6 I/O benchmarks including sequential, random, small file, and SQLite operations. |
| **Productivity Benchmarks** | **Fully Implemented** | 10 real-world UI, media, document, and multitasking workloads. |
| **Full Benchmark Suite** | **Fully Implemented** | Sequential execution of all benchmark categories generating comprehensive scores. |
| **Throttle & Efficiency Modes** | *Docs / Architecture* | Detailed specifications in [/docs/modes](docs/modes/full.md). |
| **History & Database** | **Fully Implemented** | Local SQLite Room database with score history and interactive performance graphs. |
| **Rankings & Comparisons** | **Fully Implemented** | Local baseline & device comparison UI with server sync capabilities. |
| **Thermal & Power Monitoring** | **Fully Implemented** | Real-time monitoring of CPU/battery temperatures, power draw, and frequencies. |
| **UI & Customization** | **Fully Implemented** | Jetpack Compose Material 3 design with dark/light theme switching and customization. |
| **Export & Share** | **Fully Implemented** | Result sharing cards and raw data export (JSON, CSV, text). |

---

## 📋 Implemented Benchmark Tests

### 💻 CPU Benchmarks (10 Tests)
*Executed in both **Single-Core** and **Multi-Core** configurations:*
- **Prime Generation**: Pollard's Rho prime verification testing integer compute.
- **Fibonacci Iterative**: Tests tight iterative loop performance.
- **Matrix Multiplication**: Floating-point throughput and memory alignment performance.
- **Hash Computing**: Cryptographic hashing (SHA-256 and MD5).
- **String Sorting**: Array manipulation and Quicksort sorting algorithms.
- **Ray Tracing**: Perlin Noise spatial compute and floating-point math.
- **Compression (LZMA)**: Data compression/decompression throughput.
- **Monte Carlo**: Mandelbrot set randomized mathematical approximations.
- **JSON Parsing**: Text processing, string manipulation, and tree instantiation.
- **N-Queens**: Recursion, backtracking, and CPU branch prediction.

### 🧠 AI/ML Compute Kernels (5 Tests)
- **Matrix Multiplication**: Heavy floating-point dense matrix computation.
- **Convolution 2D**: 2D convolutional filter operations for neural network layers.
- **Activation Functions**: Non-linear activations including ReLU, GELU, and Sigmoid.
- **Quantized GEMM**: Low-precision integer matrix operations.
- **Attention Engine**: Scaled dot-product self-attention mechanism calculations.

### 🎮 Native GPU Benchmarks (5 Tests)
- **Triangle Rendering**: OpenGLES/Vulkan pipeline stress rendering.
- **Compute Shader Matrix**: GPU hardware compute shader matrix operations.
- **Particle System**: Simulation of interactive physical particles.
- **Texture Sampling**: Dynamic high-resolution texture fillrate testing.
- **Tessellation**: Geometry subdivision and dynamic LOD rendering.

### 💾 RAM Benchmarks (5 Tests)
- **Sequential Read/Write**: Sequential memory transfer speed.
- **Random Access Latency**: Memory latency and pointer-chasing page hopping.
- **Memory Copy**: Byte array memory copy throughput.
- **Multi-threaded Bandwidth**: Concurrent multi-core memory bandwidth.
- **Cache Hierarchy**: L1, L2, and L3 cache access transitions and latencies.

### 📦 Storage Benchmarks (6 Tests)
- **Sequential Read & Write**: High-throughput file I/O operations.
- **Random Read/Write**: 4KB block IOPS read/write testing.
- **Small File Operations**: Metadata indexing speed across thousands of small files.
- **Database Performance**: Local SQLite database query and transaction throughput.
- **Mixed Workload**: Concurrent read and write file operations.

### ⚡ Productivity Benchmarks (10 Tests)
- **UI & Dynamic Rendering**: RecyclerView scrolling, Canvas operations, dynamic layout, and typography rendering.
- **Media Workloads**: Heavy image filter processing, batch resize, and H.264/H.265 video processing.
- **Document Workloads**: High-page PDF generation and rendering.
- **Multi-tasking**: Concurrent background processing with dynamic UI animation tasks.

---

## 📂 Documentation

Detailed technical documentation and architecture specs are available in the [/docs](docs/) directory:
- [Features Overview](docs/features.md)
- [Benchmark Index](docs/benchmark/index.md)
  - [CPU Details](docs/benchmark/cpu.md)
  - [AI/ML Details](docs/benchmark/aiml.md)
  - [GPU Native Details](docs/benchmark/gpu_native.md)
  - [RAM Details](docs/benchmark/ram.md)
  - [Storage Details](docs/benchmark/storage.md)
  - [Productivity Details](docs/benchmark/productivity.md)
- [Database Schema & Architecture](docs/database/database_schema.md)
- [Full Benchmark Mode](docs/modes/full.md)

---

## 🔧 Building

```bash
# Clone repository
git clone https://github.com/abhay-byte/finalbenchmark-platform.git
cd finalbenchmark-platform

# Build Debug APK
./gradlew assembleDebug

# Run Unit Tests
./gradlew testDebugUnitTest
```

---

## 📄 License

Licensed under the Apache 2.0 License. See [LICENSE](LICENSE) for details.

---

<div align="center">

Made with ❤️ by [Abhay](https://github.com/abhay-byte)

</div>
