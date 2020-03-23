package com.jbm.framework.masterdata.usage.form;

import com.jbm.framework.usage.form.BaseRequsetBody;
import lombok.Data;

import java.io.Serializable;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-03-23 23:47
 **/
@Data
public class MasterDataRequsetBody<Entity extends Serializable> extends BaseRequsetBody<Entity> {

}
