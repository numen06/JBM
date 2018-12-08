angular.module('editor', []).directive('editor', function($modal,$timeout) {

    return {
        restrict: 'ECMA',
        template: '<div id="editor"></div><input type="file" id="filePic" style="display: none" name="file" onchange="uploadImg()" accept="image/gif,image/png,image/jpg,image/jpeg" >' ,
        scope: {
            editorModel:"=",
            isreadonly:"="
        },
        link: function (scope, el, attr, toaster) {
            // scope.$watch('$viewContentLoaded', function() {
            isLoad=false;
            CKEDITOR.config.height = attr['height']?attr['height']:150;
            CKEDITOR.config.width = 'auto';
            var myeditor=CKEDITOR.replace( 'editor');
            var editorElement = CKEDITOR.document.getById('editor');

            // if(attr['isreadonly'])
            // {
            //     if(attr['isreadonly']=="true")
            //     {
            //         CKEDITOR.config.readOnly=true;
            //     }
            // }

            scope.$watch('isreadonly', function (value) {
                if(value)
                {
                    CKEDITOR.config.readOnly=true;
                }
                else
                {
                    CKEDITOR.config.readOnly=false;
                }
            });

            CKEDITOR.instances['editor'].on('instanceReady', function (e) {
                if(scope.editorModel!=="")
                {
                    setEditorData(scope.editorModel);
                }

                var parentDom=$(".cke_button__image").parent();
                $(".cke_button__image").remove();

                $(parentDom).prepend('<a id="cke_75" class="cke_button cke_button__image cke_button_off"  title="图像" tabindex="-1" hidefocus="true" role="button" aria-labelledby="cke_75_label" aria-describedby="cke_75_description" aria-haspopup="false" ' +
                    'onclick="getFile()">' +
                    '<span class="cke_button_icon cke_button__image_icon" ' +
                    'style="background-image:url(&quot;https://localhost:9999/assets/plugins/editor/plugins/icons.png?t=H0CG&quot;);' +
                    'background-position:0 -960px;background-size:auto;">&nbsp;</span>' +
                    '<span id="cke_75_label" class="cke_button_label cke_button__image_label" aria-hidden="false">图像</span><span id="cke_75_description" class="cke_button_label" aria-hidden="false"></span></a>');

            });

            myeditor.on( 'change', function( event ) {
                var me=this;
                $timeout(function () {
                    var data = me.getData();//内容
                    scope.editorModel=data;
                },500)

            });


        }
    };
});


function setEditorData(data) {
    CKEDITOR.instances.editor.setData(data);
}
function getEditorData() {
    return CKEDITOR.instances.editor.getData();

}

function uploadImg() {

    var file = $("#filePic");
    var fileType = file.val();
    fileType = fileType.substr(fileType.lastIndexOf(".") + 1, fileType.length);
    if (fileType != "png" && fileType != "jpg" && fileType != "jpeg" && fileType != "bmp" && fileType != "tiff") {
        file.val("");
        alert(fileType+ "类型不支持");
        return false;
    }

    var data= new FormData();
    data.append('file', $("#filePic")[0].files[0]);
    $.ajax({
        url : "https://doc.51jbm.com/api/upload",
        data : data,
        myType:"assembly",
        type : "POST",
        dataType : "json",
        processData: false,
        contentType: false,
        success : function(resp) {
            if(resp.status==1){
                var editorElement = CKEDITOR.document.getById( 'editor' );
                var htmlData=CKEDITOR.instances.editor.getData();
                var appEndData="<img src='https://doc.51jbm.com/img/"+resp.result[0].id+".png'></img>";
                var theData=htmlData+appEndData;
                CKEDITOR.instances.editor.setData(theData);
            }else{
                alert("error","上传失败", resp.message);
            }
        }
    });
}
function getFile() {
    $("#filePic").click();
}
