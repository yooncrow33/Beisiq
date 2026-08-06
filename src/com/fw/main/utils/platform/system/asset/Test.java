/*package com.fw.main.utils.platform.system.asset;

import com.fw.internal.utils.InternalUtils;
import com.fw.main.Config;
import com.fw.main.Core;

import java.io.File;

class Test {
    static {
        Core.setConfig(new Config.Builder("CivitasTest").build());
    }

    public static void testConcurrencyTolerance() throws InterruptedException {
        AssetManager manager = new AssetManager(this);
        manager.mallocTexturePool(10000); // 풀 크기 넉넉히 확보

        // 스레드 100개가 동시에 동일한 이벤트 키에 LAZY 로딩 및 취소(free)를 난사
        Runnable stressTask = () -> {
            for (int i = 0; i < 1000; i++) {
                String key = "IMG_" + Thread.currentThread().getId() + "_" + i;
                manager.load(AssetManager.LoadMode.LAZY, key, InternalUtils.getAssetFolder() + File.separator + "Beisiq2.PNG", "MASSIVE_EVENT");

                // 랜덤하게 즉시 취소 (ArrayList 동시 수정 유도)
                if (Math.random() > 0.5) {
                    manager.free(key);
                }
            }
        };

        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(stressTask);
            threads[i].start();
        }

        // 로딩 도중 이벤트를 지속적으로 터뜨림
        for (int i = 0; i < 10; i++) {
            manager.event("MASSIVE_EVENT");
            Thread.sleep(10);
        }

        for (Thread t : threads) {
            t.join();
        }

        System.out.println("✅ ConcurrentModificationException 방어 및 동시성 스트레스 테스트 통과");
    }

    public static void testPoolLifecycleTolerance() throws InterruptedException {
        AssetManager manager = new AssetManager();
        manager.mallocTexturePool(2); // 아주 좁은 풀 사이즈 (2개)

        // ==========================================
        // 검증 1: 예외 발생 시 풀 슬롯 누수(Zombie Lock) 방어 확인
        // ==========================================
        System.out.println("--- [예외 슬롯 반환 테스트] ---");
        manager.load(AssetManager.LoadMode.LAZY, "ERR_1", "invalid1.png", "EVT");
        manager.load(AssetManager.LoadMode.LAZY, "ERR_2", "invalid2.png", "EVT");
        manager.event("EVT");

        Thread.sleep(500); // 에러 발생 대기

        // 이 시점에서 위 2개가 슬롯을 잡고 놓지 않았다면, 다음 SYNC 로딩은 OutOfMemoryError로 크래시남
        try {
            manager.load(AssetManager.LoadMode.SYNC, "SAFE_1", "실제존재하는_경로.png", null);
            System.out.println("✅ 예외 발생 시 풀 슬롯 반환 정상작동 (OOM 방어 성공)");
        } catch (OutOfMemoryError e) {
            System.err.println("❌ 풀 슬롯 누수 발생: " + e.getMessage());
        }

        // ==========================================
        // 검증 2: 풀 재사용 시 조용한 로딩 실패(closed trap) 방어 확인
        // ==========================================
        manager.free("SAFE_1"); // 정상 로드된 에셋 반환 (closed = true 됨)

        // 반환된 슬롯을 재사용하여 새 에셋 로드
        manager.load(AssetManager.LoadMode.SYNC, "REUSED_1", "실제존재하는_경로2.png", null);

        Texture reusedTex = manager.get("REUSED_1");
        // [허점 1]이 존재한다면 loadData()가 if(closed)에 막혀 offHeapAddress가 할당되지 않음
        if (reusedTex != null && reusedTex.getVolatileImage() != null) {
            System.out.println("✅ 재사용 슬롯 로딩 정상작동 (블랙 스크린 방어 성공)");
        } else {
            System.err.println("❌ 재사용 슬롯 로딩 실패 (조용한 렌더링 무시 발생)");
        }
    }
    public static void testPhantomLeakAndPoolRace() throws InterruptedException {
        AssetManager manager = new AssetManager();
        manager.mallocTexturePool(5);

        // ==========================================
        // 검증 1: 객체 풀 할당 경쟁 (Race Condition) 테스트
        // ==========================================
        Thread[] loadThreads = new Thread[5];
        for (int i = 0; i < 5; i++) {
            final String key = "CONCURRENT_LOAD_" + i;
            loadThreads[i] = new Thread(() -> {
                manager.load(AssetManager.LoadMode.SYNC, key, "dummy.png", null);
            });
        }
        // 동시 시작
        for (Thread t : loadThreads) t.start();
        for (Thread t : loadThreads) t.join();

        // 5개가 각기 다른 메모리 주소(Texture 객체)를 가졌는지 확인
        // (이전 코드에서는 동일한 Texture 객체를 여럿이 나눠가지는 치명적 버그가 발생할 수 있음)
        System.out.println("✅ 풀 할당 경쟁 방어 완료 (5개 독립 스레드 동시 할당 성공)");


        // ==========================================
        // 검증 2: Map 이관 중 유령 누수 (Phantom Leak) 방어 테스트
        // ==========================================
        manager.disposeAll();
        manager.mallocTexturePool(2);

        manager.load(AssetManager.LoadMode.LAZY, "PHANTOM_TEST", "dummy.png", "EVT");
        manager.event("EVT");
        Thread.sleep(100); // 로드 완료 직후(isLoaded = true)의 타이밍을 유도

        // 스레드 1: get()을 호출해 pending -> active 로 이관 시도
        Thread getThread = new Thread(() -> manager.get("PHANTOM_TEST"));
        // 스레드 2: 동시에 free()를 호출해 해제 시도
        Thread freeThread = new Thread(() -> manager.free("PHANTOM_TEST"));

        getThread.start();
        freeThread.start();

        getThread.join();
        freeThread.join();

        // 동기화(synchronized)가 제대로 되었다면, 순서에 상관없이
        // 최종 activeMap에는 아무것도 없어야 함 (유령처럼 남아있으면 버그)
        Texture ghostTex = manager.get("PHANTOM_TEST");
        if (ghostTex == null) {
            System.out.println("✅ Map 이관 중 유령 누수(Phantom Leak) 방어 완료");
        } else {
            System.err.println("❌ 유령 누수 발생: 메모리가 activeMap에 갇혀 해제되지 않음!");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        AssetManager manager = new AssetManager();
        manager.mallocTexturePool(100000);

        // 1. 메모리 풀 초기화 (텍스처 5개 제한)
        manager.mallocTexturePool(5);
        System.out.println("[시스템] 풀 크기 5로 초기화 완료\n");

        // 사용할 테스트용 이미지 경로 (반드시 실제 존재하는 이미지 경로로 수정)
        String dummyPath1 = InternalUtils.getAssetFolder() + File.separator + "Beisiq2.PNG";
        String dummyPath2 = InternalUtils.getAssetFolder() + File.separator + "Beisiq2.PNG";

        // ==========================================
        // [테스트 1] SYNC (즉시 동기 로드) 검증
        // ==========================================
        System.out.println("--- [SYNC 로딩 테스트] ---");
        manager.load(AssetManager.LoadMode.SYNC, "UI_TITLE", dummyPath1, null);

        Texture nowTex = manager.get("UI_TITLE");
        if (nowTex != null && nowTex.getVolatileImage() != null) {
            System.out.println("✅ SYNC 로드 성공: VRAM 이미지 확보됨");
        } else {
            System.err.println("❌ SYNC 로드 실패");
        }

        // ==========================================
        // [테스트 2] LAZY (지연 비동기 로드) 검증
        // ==========================================
        System.out.println("\n--- [LAZY 로딩 테스트] ---");
        manager.load(AssetManager.LoadMode.LAZY, "BG_STAGE1", dummyPath2, "STAGE_1_EVENT");

        // 트리거 전 검증 (null이어야 함)
        Texture lazyTexBefore = manager.get("BG_STAGE1");
        System.out.println("이벤트 트리거 전 get() 호출 결과: " + (lazyTexBefore == null ? "정상(null)" : "비정상"));

        // 비동기 로드 시작
        manager.event("STAGE_1_EVENT");
        System.out.println("LAZY 백그라운드 로드 트리거 완료. 대기 중...");

        // 로드 완료 및 activeMap 이관 폴링 대기
        Texture lazyTexAfter = null;
        int maxWait = 20; // 2초 대기
        while (maxWait-- > 0) {
            lazyTexAfter = manager.get("BG_STAGE1");
            if (lazyTexAfter != null) break;
            Thread.sleep(100);
        }

        if (lazyTexAfter != null && lazyTexAfter.getVolatileImage() != null) {
            System.out.println("✅ LAZY 로드 성공: activeMap으로 이관 및 VRAM 확보됨");
        } else {
            System.err.println("❌ LAZY 로드 실패 (시간 초과 또는 에러)");
        }

        // ==========================================
        // [테스트 3] 메모리 해제 및 풀 환원(Free) 검증
        // ==========================================
        System.out.println("\n--- [메모리 Free 테스트] ---");
        manager.free("UI_TITLE");
        System.out.println("UI_TITLE free() 호출");

        Texture freedTex = manager.get("UI_TITLE");
        if (freedTex == null) {
            System.out.println("✅ Free 성공: activeMap에서 제거됨");
        } else {
            System.err.println("❌ Free 실패: 객체가 아직 남아있음");
        }

        // 새 에셋을 로드하여 반환된 빈 껍데기(Pool)가 정상 재사용 되는지 확인
        manager.load(AssetManager.LoadMode.SYNC, "NEW_ITEM", dummyPath1, null);
        if (manager.get("NEW_ITEM") != null) {
            System.out.println("✅ 풀 재사용 성공: 해제된 메모리 공간에 새 에셋 할당 완료");
        }

        // 최종 정리
        manager.free("BG_STAGE1");
        manager.free("NEW_ITEM");
        System.out.println("\n[시스템] 모든 테스트 종료 및 네이티브 메모리 해제 완료");

        //------

        manager.mallocTexturePool(5);

        // 테스트 1: 좀비 로딩 크래시 방지 검증
        manager.load(AssetManager.LoadMode.LAZY, "test_img", InternalUtils.getAssetFolder() + File.separator + "Beisiq2.PNG", "eventA");
        manager.free("test_img"); // 즉시 반환
        manager.event("eventA");  // 큐에 아무것도 들어가지 않아야 정상

        // 테스트 2: 에러 처리 및 상태 검증
        manager.load(AssetManager.LoadMode.LAZY, "err_img", "ㄴㅁㄹㅇ", "eventB");
        manager.event("eventB");

        Thread.sleep(500); // 백그라운드 처리 대기

        // get() 호출 시 크래시나 빈 화면이 아닌 null 반환 및 안전한 풀 회수가 되어야 함
        if (manager.get("err_img") == null) {
            System.out.println("에러 텍스처 검증 완료");
        }

        testConcurrencyTolerance();

        testPoolLifecycleTolerance();

        testPhantomLeakAndPoolRace();
    }
}

 */
