package com.jbm.framework.masterdata.usage.form;

import com.jbm.framework.usage.form.BaseRequsetBody;
import lombok.Data;

/**
 * @program: JBM7
 * @author: wesley.zhang
 * @create: 2020-03-23 23:47
 * @deprecated 自 7.3.0 起废弃。Controller 请使用显式 DTO / Form，勿再从 {@link com.jbm.framework.usage.form.BaseRequsetBody} 还原实体。
 **/
@Deprecated
@Data
public class MasterDataRequsetBody extends BaseRequsetBody {

}
