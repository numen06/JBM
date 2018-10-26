angular.module('app')
    .config([
        '$ocLazyLoadProvider', function ($ocLazyLoadProvider) {
            $ocLazyLoadProvider.config({
                debug: true,
                events: true,
                modules: [
                    {
                        name: 'toaster',
                        files: [
                            'lib/modules/angularjs-toaster/toaster.css',
                            'lib/modules/angularjs-toaster/toaster.js'
                        ]
                    },
                    {
                        name: 'ngDraggable',
                        files: [
                            'assets/plugins/drag/ngDraggable.js'

                        ]
                    },
                    {
                        name: 'angular-drag',
                        files: [
                            'assets/plugins/drag/dragModule.js',
                            'assets/plugins/drag/drag.js',
                        ]
                    },
                    {
                        name: 'ui.select',
                        files: [
                            'lib/modules/angular-ui-select/select.css',
                            'lib/modules/angular-ui-select/select.js'
                        ]
                    },
                    {
                        name: 'ngTagsInput',
                        files: [
                            'lib/modules/ng-tags-input/ng-tags-input.js'
                        ]
                    },
                    {
                        name: 'daterangepicker',
                        serie: true,
                        files: [
                            'lib/modules/angular-daterangepicker/moment.js',
                            'lib/modules/angular-daterangepicker/daterangepicker.js',
                            'lib/modules/angular-daterangepicker/angular-daterangepicker.js'
                        ]
                    },
                    {
                        name: 'vr.directives.slider',
                        files: [
                            'lib/modules/angular-slider/angular-slider.min.js'
                        ]
                    },
                    {
                        name: 'minicolors',
                        files: [
                            'lib/modules/angular-minicolors/jquery.minicolors.js',
                            'lib/modules/angular-minicolors/angular-minicolors.js'
                        ]
                    },
                    {
                        name: 'textAngular',
                        files: [
                            'lib/modules/text-angular/textAngular-sanitize.min.js',
                            'lib/modules/text-angular/textAngular-rangy.min.js',
                            'lib/modules/text-angular/textAngular.min.js'
                        ]
                    },
                    {
                        name: 'ng-nestable',
                        files: [
                            'lib/modules/angular-nestable/jquery.nestable.js',
                            'lib/modules/angular-nestable/angular-nestable.js'
                        ]
                    },
                    {
                        name: 'angularBootstrapNavTree',
                        files: [
                            'lib/modules/angular-bootstrap-nav-tree/abn_tree_directive.js'
                        ]
                    },
                    {
                        name: 'ui.calendar',
                        files: [
                            'lib/jquery/fullcalendar/jquery-ui.custom.min.js',
                            'lib/jquery/fullcalendar/moment.min.js',
                            'lib/jquery/fullcalendar/fullcalendar.js',
                            'lib/modules/angular-ui-calendar/calendar.js'
                        ]
                    },
                    {
                        name: 'ngGrid',
                        files: [
                            'lib/modules/ng-grid/ng-grid.min.js',
                            'lib/modules/ng-grid/ng-grid.css'
                        ]
                    },
                    {
                        name: 'dropzone',
                        files: [
                            'lib/modules/angular-dropzone/dropzone.js',
                            'lib/modules/angular-dropzone/angular-dropzone.js'
                        ]
                    },
                    {
                        name: 'carousel',
                        files: [
                            'app/directives/viewFinish.js',
                            'lib/modules/angular-carousel/angular-carousel.js',
                            'lib/modules/angular-carousel/jquery.luara.min.js',
                            'lib/modules/angular-carousel/luara.css',
                            'lib/modules/angular-carousel/luara.left.css',
                            'lib/modules/angular-carousel/luara.top.css'
                            // 'lib/modules/angular-carousel/.css"
                        ]
                    },
                    {
                        name: 'imageMagnification',
                        files: [
                            'lib/modules/angular-imageMagnification/jquery.ui.core.js',
                            'lib/modules/angular-imageMagnification/jquery.ui.widget.js',
                            'lib/modules/angular-imageMagnification/jquery.ui.rlightbox.js',
                            'lib/modules/angular-imageMagnification/angular-imageMagnification.js',
                            'lib/modules/angular-imageMagnification/lightbox.css',
                            'lib/modules/angular-imageMagnification/ui-lightness/jquery-ui-1.8.16.custom.css'
                        ]
                    },
                    {
                        name: 'editor',
                        files: [
                            'lib/modules/angular-editor/angular-editor.js',
                            'lib/modules/angular-editor/ckeditor.js'
                        ]
                    },
                    {
                        name: 'pagination',
                        files: [
							'assets/common/js/pagination-with-styles.js',
							'assets/common/js/juicer.js'

                        ]
                    }
                ]
            });
            angular.module('pagination', []);
        }
    ]);