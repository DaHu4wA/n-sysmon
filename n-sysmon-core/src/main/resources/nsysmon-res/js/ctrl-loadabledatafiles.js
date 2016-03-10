angular.module('NSysMonApp').controller('CtrlLoadableServerDataFiles', function($scope, $log, Rest, escapeHtml, $timeout) {
    $scope.refresh = function() {
        sendCommand('getFiles');
    };
    //getData on load
    $scope.refresh();

    function sendCommand(cmd) {
        Rest.call(cmd, initFromResponse);
    }

    function initFromResponse(data) {
        $scope.pages = data.pages;
        //console.log(data);
    }

});
