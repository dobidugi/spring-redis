package com.playground.springredis.lock;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.mockito.Mockito.when;

// Mockito 기능을 사용하기 위한 애노테이션
@ExtendWith(MockitoExtension.class)
class StockServiceUnitTest {

    // @InjectMocks: @Mock으로 생성된 가짜 객체들을 이 객체에 주입합니다.
    @InjectMocks
    private StockService stockService;

    // @Mock: 가짜(Mock) Repository 객체를 생성합니다.
    @Mock
    private MockStockRepository mockStockRepository;

    // @Mock: StockService가 의존하는 다른 Bean도 Mock으로 만들어줍니다.
    @Mock
    private RedisLockRepository redisLockRepository;

    private Stock stock;

    @BeforeEach
    void setUp() {
        // 테스트 시작 전, 재고 100을 가진 Stock 객체 생성
        stock = new Stock(100L);

        // Mock 객체의 행동 정의:
        // mockStockRepository.findById(1L)가 호출되면, 위에서 만든 stock 객체를 반환하도록 설정
        when(mockStockRepository.findById(1L)).thenReturn(stock);
    }

    @Test
    @DisplayName("단위 테스트에서 100개의 요청이 동시에 재고를 감소시키면 Race Condition이 발생한다")
    void decreaseStock_UnitTest_RaceCondition() throws InterruptedException {
        // given
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    stockService.decreaseStockWithNoneLock();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        long finalQuantity = stock.count();
        System.out.println("최종 재고 수량 (단위 테스트) = " + finalQuantity);

        // 최종 재고가 0이 아닌 것을 확인
        assertThat(finalQuantity).isNotEqualTo(0L);
    }

    @Test
    @DisplayName("Redis로 락을 구현하여 Race Condition 해결")
    void decreaseStock_UnitTest_With_RedisLock() {
        // given
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    stockService.decreaseStockWithRedisLock();
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        executorService.shutdown();

        // then
        long finalQuantity = stock.count();
        System.out.println("최종 재고 수량 (Redis Lock 단위 테스트) = " + finalQuantity);

        // 최종 재고가 0임을 확인
        assertThat(finalQuantity).isEqualTo(0L);
    }
}