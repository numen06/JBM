SELECT
    *
FROM
    "gatewayLogs"
WHERE
    #if(${beginTime})
    time >= '$DateUtil.formatDateTime(${beginTime})'
    #end
    #if(${endTime})
    AND time <= '$DateUtil.formatDateTime(${endTime})'
    #end
    #if(${gatewayLogs.path})
    AND path =~/${gatewayLogs.path}/
    #end
    #if(${gatewayLogs.apiName})
	AND apiName =~/${gatewayLogs.apiName}/
    #end
    #if(${gatewayLogs.ip})
	AND ip =~/${gatewayLogs.ip}/
    #end
    #if(${gatewayLogs.serviceId})
	AND serviceId =~/${gatewayLogs.serviceId}/
    #end
    #if(${gatewayLogs.apiId})
    AND apiId =${gatewayLogs.apiId}
    #end
    #if(${gatewayLogs.method})
	AND method ='${gatewayLogs.method}'
    #end
    #if(${gatewayLogs.httpStatus})
	AND httpStatus =${gatewayLogs.httpStatus}
    #end
    #if(${gatewayLogs.requestRealName})
	AND requestRealName =~/${gatewayLogs.requestRealName}/
    #end
    #if(${gatewayLogs.requestUserId})
	AND requestUserId ='${gatewayLogs.requestUserId}'
    #end
ORDER BY
    time DESC
