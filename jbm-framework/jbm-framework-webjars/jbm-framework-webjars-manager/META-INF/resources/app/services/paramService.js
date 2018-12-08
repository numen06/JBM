'use strict';
app.service('ParamService', function() {
    var paramData = {}
    function setParam(data) {
        paramData = data;
    }
    function getParam() {
        return paramData;
    }

    return {
        set: setParam,
        get: getParam
    }

});