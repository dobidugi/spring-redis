package com.playground.springredis.lock;

import org.springframework.stereotype.Repository;

@Repository
public class MockStockRepository {

    private Stock stock;

    public Stock findById(Long itemId) {
        return stock;
    }

    public void save(Stock stock) {
       this.stock = stock;
    }
}
