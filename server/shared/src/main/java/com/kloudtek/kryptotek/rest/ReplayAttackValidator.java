/*
 * Copyright (c) 2016 Kloudtek Ltd
 */

package com.kloudtek.kryptotek.rest;

/**
 * Created by yannick on 10/03/2015.
 */
public interface ReplayAttackValidator {
    boolean checkNonceReplay(String nonce);
}
