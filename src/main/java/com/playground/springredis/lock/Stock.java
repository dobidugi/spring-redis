package com.playground.springredis.lock;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@AllArgsConstructor
public class Stock {

    public Long count;

    public void decrease(Long quantity) {
        if(count > 0) {
            count -= quantity;
        }
    }

    public Long count() {
        return count;
    }
}
