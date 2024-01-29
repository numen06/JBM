select * from sql_initialize
         where
         file_name in (
             $StrUtil.join(",",$list)
             )