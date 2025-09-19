package com.playground.springredis.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Service
public class StockService {

    private final RedisLockRepository redisLockRepository;
    private final MockStockRepository mockStockRepository;


    /**
     * 재고 감소 (락 사용 X)
     * @return
     */
    public void decreaseStockWithNoneLock() {
        Stock stock = this.mockStockRepository.findById(1L);
        stock.decrease(1L);
    }

    /**
     * 재고 감소 (Redis 락 사용 O)
     * @return
     */
    public void decreaseStockWithRedisLock() {
        final String lockKey = "lock:stock:1";
        final String clientId = UUID.randomUUID().toString(); // 락 소유자 식별자

        int attempts = 0;
        boolean isLockAcquired = false;

        try {
            // 락 획득을 여러 번 시도
            while (attempts < 100 && !isLockAcquired) {
                isLockAcquired = redisLockRepository.lock(lockKey, clientId, Duration.ofSeconds(30).toMillis());
                if (!isLockAcquired) {
                    attempts++;
                    log.info("[{}] 락 획득 실패. {}번째 재시도 대기...", clientId, attempts);
                    TimeUnit.MILLISECONDS.sleep(100); // 잠시 대기 후 재시도
                }
            }

            if (!isLockAcquired) {
                log.warn("[{}] 락 획득 최종 실패. 재고 감소 작업을 수행할 수 없습니다.", clientId);
                return;
            }

            // 락 획득 성공 시 재고 감소 로직 수행 (임계 영역)
            log.info("[{}] 락 획득 성공! 재고 감소 로직을 시작합니다.", clientId);
            Stock stock = this.mockStockRepository.findById(1L); // 이 안에서 DB 조회 및 업데이트 로직이 있다면
            stock.decrease(1L);                                 // 모두 락으로 보호되어야 합니다.
            log.info("[{}] 재고 감소 완료. 현재 재고: {}", clientId, stock.count());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 인터럽트 상태 복원
            log.error("[{}] 락 획득 또는 비즈니스 로직 중 스레드 인터럽트 발생: {}", clientId, e.getMessage());
            throw new RuntimeException("작업 중단", e); // 예외 전파 또는 적절한 처리
        } catch (Exception e) {
            log.error("[{}] 재고 감소 중 예상치 못한 오류 발생: {}", clientId, e.getMessage());
            throw e; // 예외 전파
        } finally {
            if(isLockAcquired) log.info("락 해제 시도...");
            // 락 해제 (획득에 성공했을 경우만 해제 시도)
            if (isLockAcquired) {
                boolean isReleased = redisLockRepository.unlock(lockKey, clientId);
                if (isReleased) {
                    log.info("[{}] 락 해제 성공.", clientId);
                } else {
                    // 락 해제 실패: 락이 이미 만료되었거나 다른 스레드에 의해 해제되었을 수 있음
                    log.warn("[{}] 락 해제 실패. (락이 이미 만료되었거나 소유자가 아님)", clientId);
                }
            }
        }

    }
}
