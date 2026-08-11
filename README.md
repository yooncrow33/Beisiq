# Beisiq Engine (PRE 0.0.3)

A lightweight Java/AWT-based 2D game engine. It supports high-performance memory management, dynamic texture atlas packing, an in-game developer console, and a completed Hangul (Korean) input module.

---

## Key Features

* **Advanced Graphics Pipeline**
  * Double-buffering rendering support based on `VolatileImage` and `BufferStrategy`
  * **Dynamic Texture Atlas Generation** based on real-time Alpha Cropping and non-spherical MaxRects algorithms
  * Automatic calculation of display scaling (Fractional/Integer Physical Scaling)

* **High-Performance Sound Engine (BeisiqTinySound)**
  * Off-heap direct memory audio allocation utilizing `sun.misc.Unsafe` (minimizes GC overhead)
  * Supports BGM streaming, SFX overlapping playback, real-time panning, and volume control

* **Developer Experience & Tooling**
  * In-game developer console (supports `~` key, system control, GC memory cleanup, auto-completion)
  * Built-in `KoreanModule` providing full support for Korean composition input in an AWT environment
  * AES encryption-based data save/load (`IoObject`) and dynamic asset manager (`AssetManager`)

---

## Requirements

* **JDK:** OpenJDK 21.0.7 (GraalVM 21+ recommended)
* **OS:** Windows / macOS / Linux(May be)

## Getting Started

### 1. Engine Initialization

Configure settings via `Core.setConfig`, then inherit and use the `Base` class.

```java
public class MyGame extends Base {

    static {
        Core.setConfig(new Config.Builder("MyGameFolder") // Creates a hidden folder.
                .setWindowWidth(1280) // Virtual resolution is fixed to Full HD; sets the actual window size.
                .setWindowHeight(720)
                .setUseKoreanModule(true) // Enables Korean input module when true.
                .setUseEncryption(false) // Applies encryption to all files when true.
                .build());
    }

    public MyGame() {
        super(new Builder()
                .setUseConsole(true)
                .setRenderingOption(RenderingOption.DEFAULT) // Rendering option
        );
    }

    @Override
    public void init(BaseInit init) {
        // Allocate and initialize object pools
        assetManager.mallocTexturePool(1000);
        assetManager.mallocLazyLoadPool(200);
    }

    @Override
    public void update(double dt) {
        // Update game logic
    }

    @Override
    public void render(Graphics2D g) {
        // Rendering code
        g.setColor(Color.WHITE);
        g.drawString("Hello, Beisiq Engine!", 50, 50);
    }

    public static void main(String[] args) {
        new MyGame();
    }
}
```

## Life Cycle
The `Base` class is the core context responsible for engine execution, thread and rendering loop management, scene lifecycle, and safe shutdown handling.

---

### 1. Engine Booting & Async Loading Sequence

A sequential initialization phase that takes place on a background thread after the engine is instantiated.

1. **Bootstrapping (Main Thread)**
   * `Core` configuration validation and `JFrame`/`Canvas` window creation
   * `BufferStrategy(2)` and `ViewMetrics` setup
   * Starts logic and render threads by calling `launch()`
2. **Async Initialization (`InitLoadState`)**
   * **`sys`**: System modules configuration (console, mouse interface, etc.)
   * **`assetInit`**: Initial essential image/texture loading
   * **`io`**: Save and configuration data loading
   * **`sceneInit`**: Invokes initial `Scene.init()` and completes (`initLoadEnd = true`)

---

### 2. Multi-Threaded Main Loop

Logic and rendering run on independent threads. (Target: 60 FPS)

#### Logic Thread (`logicLoop`)
* Calculates Delta Time (`dt`) and executes `update(dt)`
* Detects scene change requests (`isChangeScene`) and schedules async transition threads
* Controls precision sleep timing using `Thread.sleep()` and `Thread.yield()`

#### Render Thread (`renderLoop`)
* Waits for `RESIZE_SETTLE_NANOS` during window resizing to prevent flickering
* Screen updates based on configured `RenderingOption`:
  * **`DEFAULT`**: Double Buffering (`BufferStrategy`) rendering
  * **`LEGACY`**: VRAM memory buffer (`VolatileImage`) based rendering
  * **`EXPERIMENTAL`**: Batched rendering after call buffer caching
    (Very slow, inefficient, and incompatible)

---

### 3. Scene Lifecycle

Memory release and transition workflow executed upon calling `changeScene(newScene)`.

* changeScene(newScene)

  * └─► Set pendingScene and isChangeScene = true

  * └─► Invoke previous Scene's dispose() (Reflection)

  * └─► AssetManager garbage collection & explicit GC execution

  * └─► Execute new Scene.init() and complete transition

---

### 4. Safe Shutdown Phase

Safely releases resources and terminates the process upon calling `exit()`.

1. **Save Data**: Immediately synchronizes save data by invoking `io.save.save()`
2. **Hook Execution**: Process shutdown tasks tied to `operatorManager.exitOperatorPack`
3. **Thread Join**: Await safe termination of `logicThread` and `renderThread` with timeout
4. **Dispose**: Release `BufferStrategy` and fully dispose of `JFrame` resources

---

## KNOWN ISSUE!

* **Unstable Sound Engine**
  * `.ogg` extension unavailable
  * Non-streaming `Music` usage unavailable
  * Mono channel audio gets corrupted when loaded into memory without streaming

* **Unstable API**
  * Incomplete encapsulation
  * Unstable API design

## Tip

```
This project is a hobby project developed by a single developer. As such, stabilization may take time, and documentation progress will be slower.

Acknowledging these limitations, a PDF containing the entire full source code will be attached to major version releases. Feel free to utilize it for training AI models or other reference purposes.
```

# Beisiq Engine (PRE 0.0.3)

Java/AWT 기반의 경량 2D 게임 엔진입니다. 고성능 메모리 관리, 동적 텍스처 아틀라스 패킹, 인게임 개발자 콘솔 및 완성형 한글 입력 모듈을 지원합니다.

---

## Key Features

* **Advanced Graphics Pipeline**
  * `VolatileImage` 및 `BufferStrategy` 기반의 이중 버퍼링 렌더링 지원
  * 실시간 알파 크롭(Alpha Cropping) 및 비구의형 맥스렉츠(MaxRects) 알고리즘 기반 **동적 텍스처 아틀라스(Atlas) 생성**
  * 디스플레이 스케일링(Fractional/Integer Physical Scaling) 자동 계산

* **High-Performance Sound Engine (BeisiqTinySound)**
  * `sun.misc.Unsafe`를 활용한 Off-Heap 다이렉트 메모리 오디오 할당 (GC 부하 최소화)
  * BGM 스트리밍 및 SFX 오버랩 재생, 실시간 패닝(Pan) 및 볼륨 제어 지원

* **Developer Experience & Tooling**
  * 인게임 개발자 콘솔 (`~` 키 지원, 시스템 제어, GC 메모리 정리, 명령어 자동 완성 제공)
  * AWT 환경의 한글 조합 입력을 완벽 지원하는 `KoreanModule` 내장
  * AES 암호화 기반의 데이터 저장/로드(`IoObject`) 및 동적 에셋 관리자(`AssetManager`)

---

## Requirements

* **JDK:** OpenJDK 21.0.7 (GraalVM 21+ 권장)
* **OS:** Windows / macOS / Linux(아마도)

## Getting Started

### 1. Engine Initialization

`Core.setConfig`를 수행한 후 `Base` 클래스를 상속받아 사용합니다.

```java
public class MyGame extends Base {

    static {
        Core.setConfig(new Config.Builder("MyGameFolder") //숨김폴더가 생성됩니다.
                .setWindowWidth(1280) //가상해상도는 Full HD고정이며 실제 크기를 설정합니다.
                .setWindowHeight(720)
                .setUseKoreanModule(true) //활성화시에 한글이 활성화 됩니다.
                .setUseEncryption(false) //활성화시 모든 파일에 암호화가 적용됩니다.
                .build());
    }

    public MyGame() {
        super(new Builder()
                .setUseConsole(true)
                .setRenderingOption(RenderingOption.DEFAULT) //렌더링옵션
        );
    }

    @Override
    public void init(BaseInit init) {
        // 오브젝트 풀 할당 및 초기화
        assetManager.mallocTexturePool(1000);
        assetManager.mallocLazyLoadPool(200);
    }

    @Override
    public void update(double dt) {
        // 게임 로직 업데이트
    }

    @Override
    public void render(Graphics2D g) {
        // 렌더링 코드
        g.setColor(Color.WHITE);
        g.drawString("Hello, Beisiq Engine!", 50, 50);
    }

    public static void main(String[] args) {
        new MyGame();
    }
}
```

## Life Cycle
`Base` 클래스는 엔진의 실행, 스레드 및 렌더링 루프 관리, 씬 라이프사이클, 안전한 종료 처리를 담당하는 핵심 콘텍스트입니다.

---

### 1. Engine Booting & Async Loading Sequence

엔진 생성 후 백그라운드 스레드에서 진행되는 순차적 초기화 단계입니다.

1. **Bootstrapping (Main Thread)**
   * `Core` 설정 검증 및 `JFrame`/`Canvas` 창 생성
   * `BufferStrategy(2)` 및 `ViewMetrics` 구성
   * `launch()` 호출을 통한 로직/렌더 스레드 구동
2. **Async Initialization (`InitLoadState`)**
   * **`sys`**: 콘솔, 마우스 인터페이스 등 시스템 모듈 구성
   * **`assetInit`**: 초기 구동 필수 이미지/텍스처 로딩
   * **`io`**: 세이브 및 설정 데이터 로드
   * **`sceneInit`**: 최초 지정된 `Scene.init()` 호출 후 완료(`initLoadEnd = true`)

---

### 2. Multi-Threaded Main Loop

로직과 렌더링이 상호 독립된 스레드에서 구동됩니다. (Target: 60 FPS)

#### Logic Thread (`logicLoop`)
* Delta Time(`dt`)을 계산하여 `update(dt)` 실행
* 씬 변경 상태(`isChangeScene`) 감지 및 비동기 교체 스레드 스케줄링
* `Thread.sleep()` 및 `Thread.yield()`를 활용한 Precision Sleep 타이밍 제어

#### Render Thread (`renderLoop`)
* 윈도우 크기 변경 시 `RESIZE_SETTLE_NANOS` 동안 렌더 연산 대기 (깜빡임 방지)
* 설정된 `RenderingOption` 모드에 따른 화면 갱신:
  * **`DEFAULT`**: Double Buffering (`BufferStrategy`) 렌더링
  * **`LEGACY`**: VRAM 메모리 버퍼 (`VolatileImage`) 기반 렌더링
  * **`EXPERIMENTAL`**: Call 버퍼 캐싱 연산 후 일괄 렌더링
    (매우 느리고, 비효율적이며, 호환이 되지 않습니다.)

---

### 3. Scene Lifecycle

`changeScene(newScene)` 호출 시 수행되는 메모리 해제 및 전환 흐름입니다.

* changeScene(newScene)

  * └─► pendingScene 설정 및 isChangeScene = true

  * └─► 이전 Scene의 dispose() 호출 (Reflection)

  * └─► AssetManager 리소스 가비지 수거 및 명시적 GC 수행

  * └─► 신규 Scene.init() 실행 및 완료 처리

---

### 4. Safe Shutdown Phase

`exit()` 호출 시 리소스를 안전하게 정리하고 프로세스를 종료합니다.

1. **Save Data**: `io.save.save()` 호출로 세이브 데이터 즉시 동기화
2. **Hook Execution**: `operatorManager.exitOperatorPack` 연동 종료 작업 처리
3. **Thread Join**: `logicThread`, `renderThread` 타임아웃 대기 후 안전한 종료
4. **Dispose**: `BufferStrategy` 해제 및 `JFrame` 리소스 완전 반납

---

## KNOWN ISSUE!

* **Unstale Sound Engine**
  * `ogg` 확장자 사용불능
  * 스트림옵션이 아닌 `Music` 사용불능
  * 모노채널 사운드가 메모리에 스트림이 아닌 메모리에 올라갈떄 깨짐.

* **Unstable API**
  * 완벽하지 캡슐화
  * 불안정한 API

## Tip

```
이 프로젝트는 개발자 1명이 취미로 하는 프로젝트입니다. 이로써 빨리 안정되지 않을 수 있으며 문서화는 더 더딜것 입니다. 

이 한계도 저는 알기 때문에 크게 바뀌는 엔진 버전의 릴리스에는 모든 코드의 전문이 담긴 PDF를 첨부합니다. AI에게 학습시키는 등의 방법으로 활용하시길 바랍니다.
```
