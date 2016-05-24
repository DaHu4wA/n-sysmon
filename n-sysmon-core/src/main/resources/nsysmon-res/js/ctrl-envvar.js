
angular.module('NSysMonApp').controller('CtrlEnvVar', function($scope, $log, Rest, escapeHtml) {
    $('.btn').tooltip({
        container: 'body',
        html: true
    });

    $('title').text("NSysmon - Environment");
    var rootLevel = 0;
    var nodesByFqn = {};

    function initFromResponse(data) {
        reorder(data.envTree);
        $scope.envTree = data.envTree;
        nodesByFqn = {};
        initTreeNodes($scope.envTree, 0, '');

        $('#theTree').html(htmlForAllTrees());
        $('.data-row.with-children')
            .click(function() {
                var fqn = $(this).children('.fqn-holder').text();
                var childrenDiv = $(this).next();
                var nodeIconDiv = $(this).children('.node-icon');
                var isExpanded = nodeIconDiv.hasClass('node-icon-expanded');

                childrenDiv.slideToggle(50, function() {
                    if(isExpanded) {
                        nodeIconDiv.addClass('node-icon-collapsed').removeClass('node-icon-expanded');
                    }
                    else {
                        nodeIconDiv.removeClass('node-icon-collapsed').addClass('node-icon-expanded');
                    }
                });
            });
        expandFirstNode();
    }
    function expandFirstNode() {
        var dataRow = $('.data-row-0').first();
        var childrenDiv = dataRow.next();
        var nodeIconDiv = dataRow.children('.node-icon');

        childrenDiv.show();
        nodeIconDiv.addClass('node-icon-expanded').removeClass('node-icon-collapsed');
    }

    function reorder(rootNodes) {
        for(var i=0; i<rootNodes.length; i++) {
            var node = rootNodes[i];
            if(node.name === 'overview') {
                rootNodes.splice(i, 1);
                rootNodes.unshift(node);
            }
        }
    }
    function initTreeNodes(nodes, level, prefix) {
        if(nodes) {
            for(var i=0; i<nodes.length; i++) {
                if(level === 0) {
                    nodes[i].name = resolveWellKnownTopLevelNames(nodes[i].name);
                }

                nodes[i].level = level;
                var fqn = prefix + '\n' + (nodes[i].id || nodes[i].name);
                nodes[i].fqn = fqn;
                nodesByFqn[fqn] = nodes[i];
                initTreeNodes(nodes[i].children, level+1, fqn);
            }
        }
    }

    function resolveWellKnownTopLevelNames(name) {
        if(name === 'envvar') {
            return 'Environment Variables';
        }
        if(name === 'sysprop') {
            return 'System Properties';
        }
        if(name === 'hw') {
            return 'Hardware';
        }
        if(name === 'overview') {
            return 'Overview';
        }
        if(name === 'jar-version') {
            return 'JAR Files';
        }
        return name;
    }


    Rest.call('getData', initFromResponse);

    function htmlForAllTrees() {
        var result = '';
        angular.forEach($scope.envTree, function(rootNode) {
            result +=
                '<div>' +
                    htmlForTreeNode(rootNode) +
                '</div>';
        });

        return result;
    }

    function htmlForTreeNode(curNode) {
        var withChildrenClass = (curNode.children && curNode.children.length) ? ' with-children' : '';

        var result =
            '<div class="data-row data-row-' + (curNode.level - rootLevel) + withChildrenClass + '">' +
                '<div class="fqn-holder">' + curNode.fqn + '</div>' +
                '<div class="node-icon ' + nodeIconClass(curNode.fqn) + '">&nbsp;</div>' +
                '<div class="env-value">' + escapeHtml(curNode.value) + '</div>' +
                '<div class="node-text env-name">' + escapeHtml(curNode.name) + '</div>' +
                '</div>';

        if(curNode.children && curNode.children.length) {
            result += '<div class="children" style="display: none;">';
            angular.forEach(curNode.children, function(child) {
                result += htmlForTreeNode(child);
            });
            result += '</div>';
        }
        return result;
    }

    function nodeIconClass(node) {
        if(! node.fqn) {
            //TODO remove this heuristic
            node = nodesByFqn[node];
        }

        if(node.children && node.children.length) {
            return 'node-icon-collapsed';
//            return expansionModel[node.fqn] ? 'node-icon-expanded' : 'node-icon-collapsed';
        }
        return 'node-icon-empty';
    }
});

