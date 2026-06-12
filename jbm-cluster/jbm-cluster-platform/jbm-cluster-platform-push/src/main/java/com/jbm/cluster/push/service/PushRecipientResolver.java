package com.jbm.cluster.push.service;

import com.jbm.cluster.api.model.push.PushMsg;

import java.util.Set;

public interface PushRecipientResolver {

    Set<Long> resolve(PushMsg pushMsg);
}
