/* global require */

'use strict';

var angularDrag = angular.module('angular-drag', []);

angularDrag.directive('drag', function() {
    return {
        restrict: 'A',
        scope:
        {
            dropList:"="
        },
        link: function(scope, elem) {
            var dragNode = elem[0];
            var dragHandle = dragNode.querySelector('[drag-handle]');

            if (!dragHandle) {
                new Drag(dragNode,null,null,scope.dropList);
            }
        }
    };
});

angularDrag.directive('dragHandle', function() {
    return {
        restrict: 'A',
        link: function(scope, elem) {
            var dragNode = elem[0];

            while (!dragNode.hasAttribute('drag')) {
                dragNode = dragNode.parentNode;
            }

            elem.on(Drag.START, function(event) {
                new Drag(dragNode, event);
                event.preventDefault();
            });
        }
    };
});