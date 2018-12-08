angular.module('app')
    .directive('imageBaseSelect', function () {
            return {
                restrict: 'ECMA',
                templateUrl: 'views/imageSelectBaseTemp.html',
                scope: {
                    isEdit: '=',
                    baseImageList: '=',
                    imageList: '=',
                    tempList:'='
                },
                link: function (scope, el, attr, toaster) {
                    var names=[];
                    var urls=[];
                    scope.existFormName=attr["existname"];
                    scope.existFormUrl=attr["existurl"];
                    scope.$on('ngViewFinished', function (ngRepeatFinishedEvent) {
                        $( ".lb_flowers" ).rlightbox();
                    });
                    scope.uploadFileName = attr["filename"] ? attr["filename"] : "attachs";
                    scope.getFile = function (num) {
                        $("#currentFile" + (scope.myFile.fileList.length - 1)).click();
                    }
                    scope.deleteExistImg=function (url) {
                        angular.forEach(urls,function (item,index) {
                            if(item==url)
                            {
                                scope.imageList.splice(index,1);
                                urls.splice(index,1);
                                names.splice(index,1);
                            }
                        });
                        scope.existName=names.join(",");
                        scope.existUrl=urls.join(",");
                    }
                    scope.$watch('imageList',function(nv,ov){
                        names=[];
                        urls=[];
                        angular.forEach(nv,function (item,index) {
                            names.push(item.name);
                            urls.push(item.url);
                        });
                        scope.existName=names.join(",");
                        scope.existUrl=urls.join(",");


                    },true);

                    scope.$watch('tempList',function(nv,ov){
                        if(nv&&nv.length==0)
                        {
                            scope.imageList=[];
                            scope.baseImageList=[];
                        }
                    });

                    scope.deleteImg = function (num) {
                        scope.baseImageList.splice(num, 1);
                    }
                }
            };
        }
    );
