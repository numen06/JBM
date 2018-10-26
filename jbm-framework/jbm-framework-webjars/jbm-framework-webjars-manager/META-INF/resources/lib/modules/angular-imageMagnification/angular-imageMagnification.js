angular.module('imageMagnification', []).directive('imageMagnification', function($modal) {

    return function(scope, element, attrs) {
       $(element).unbind("click").bind("click",function () {

           if(!scope.carouselImgList||scope.carouselImgList.length<1)
           {
               if (scope.toaster != null)
               {
                   scope.toaster.pop('error', '轮播',"轮播的图片不能为空");
               }
               return;
           }
           scope.modalInstance = $modal.open({
               windowClass: "",
               template: '<div class="example"><ul >' +
               '<li on-finish ng-repeat="item in carouselImgList"><img src="{{item.url}}"></li></ul>' +
               '<ol><li ng-repeat="item in carouselImgList"></li>' +
               '</ol></div>',
               controller: carouselModelCtrl,
               size: null,

               resolve: {
                   param: function () {
                       return {carouselImgList:scope.carouselImgList,width:attrs["imgwidth"]?attrs["imgwidth"]:800,height:attrs["imgheight"]?attrs["imgheight"]:300,interval:attrs["interval"]?attrs["interval"]:5000,deriction:attrs["deriction"]?attrs["deriction"]:"left"};
                   }

               }
           });
           scope.modalInstance.result.then(function(result) {

           })





       })

    };
});
var carouselModelCtrl = function ($scope, $modalInstance,param ) {
    $scope.carouselImgList=param.carouselImgList;

    $scope.$on('ngViewFinished', function (ngRepeatFinishedEvent) {
        $(".example").luara({width:param.width,height:param.height,interval:param.interval,selected:"seleted",deriction:param.deriction});
    });

    // $scope.$watch('$viewContentLoaded', function() {
    //
    // });

}