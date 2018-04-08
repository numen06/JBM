angular.module('app')
    .directive('imageSelect', function () {
            return {
                restrict: 'ECMA',
                templateUrl: 'views/imageSelectTemp.html',
                scope: {
                    isEdit: '=',
                    imageList: '=',
                    tempList:'=',
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
                    scope.myFile = {imageList:[], fileList: [{name: "currentFile0"}]};
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
                            if(item.isClear)
                            {
                                scope.myFile.imageList=[];
                            }
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
                            scope.myFile.imageList=[];
                        }
                    });

                    scope.deleteImg = function (id, num) {
                        angular.forEach(scope.myFile.imageList, function (item, index) {
                            if (item.id == id) {
                                scope.myFile.imageList.splice(index, 1);
                            }
                        })
                        // scope.myFile.imageList.splice(index,1);
                        scope.myFile.fileList.splice(num, 1);
                    }
                    scope.uploadFile = function () {
                        var file = $("#currentFile" + (scope.myFile.fileList.length - 1));
                        var fileType = file.val();
                        fileType = fileType.substr(fileType.lastIndexOf(".") + 1, fileType.length);
                        if (fileType != "png" && fileType != "jpg" && fileType != "jpeg" && fileType != "bmp" && fileType != "tiff") {
                            file.val("");
                            toaster.pop("error", "文件类型", fileType + "类型不支持");
                            return false;
                        }
                        var reader = new FileReader();

                        reader.onloadend = function () {
                            scope.myFile.imageList.push({
                                id: scope.myFile.fileList.length,
                                name: file.val().substr(file.val().lastIndexOf("\\") + 1, file.val().length),
                                url: reader.result
                            });
                            scope.$apply();
                        }
                        if (file) {
                            reader.readAsDataURL(file[0].files[0]);
                        }


                        scope.myFile.fileList.push({name: "currentFile" + scope.myFile.fileList.length});

                    }

                }
            };
        }
    );
