package com.playground.springredis.lock;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;


@Testcontainers
@SpringBootTest
public class StockServiceIntegrationTest {


    // GenericContainer를 사용해 Docker Hub의 redis:latest 이미지를 기반으로 컨테이너 생성
    @Container
    private static final GenericContainer<?> REDIS_CONTAINER =
            new GenericContainer<>(DockerImageName.parse("redis:latest"))
                    .withExposedPorts(6379);

    // 3. 실행된 컨테이너의 실제 host와 port를 스프링 설정에 동적으로 주입
    @DynamicPropertySource
    private static void setRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379).toString());
    }

    @Autowired
    private StockService stockService;

    @Autowired
    private MockStockRepository mockStockRepository; // 실제 DB 대신 쓸 Mock Repository

    @BeforeEach
    void setUp() {
        // 매 테스트 시작 전, 재고를 100으로 초기화
        Stock stock = new Stock(100L);
        mockStockRepository.save(stock); // Mock이지만 상태를 가지도록 설정
    }

    @Test
    @DisplayName("100개의 요청이 동시에 재고를 감소시킬 때, 분산 락이 데이터 정합성을 보장한다")
    void decreaseStockWithRedisLock_ConcurrencyTest() throws InterruptedException {
        // given
        int threadCount = 100; // 100개의 동시 요청을 시뮬레이션
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    // 각 스레드는 재고 감소 메서드를 호출합니다.
                    stockService.decreaseStockWithRedisLock();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        Stock finalStock = mockStockRepository.findById(1L);
        System.out.println("최종 재고 수량 (통합 테스트) = " + finalStock.count());

        assertThat(finalStock.count()).isEqualTo(0L);
    }
}