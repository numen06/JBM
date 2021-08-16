package com.jbm.cluster.api.model.message;

import lombok.Data;

/**
 * @author LIQIU
 * @date 2018-3-27
 **/
@Data
public class EmailNotification extends Notification{

    private String receiver;

    private String title;

    private String content;

}
