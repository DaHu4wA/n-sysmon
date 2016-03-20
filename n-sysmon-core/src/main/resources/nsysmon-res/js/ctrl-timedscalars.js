angular.module('NSysMonApp').controller('CtrlTimedScalars', function($scope, $timeout, $log, Rest, $location) {
    $scope.options =  {
      "chart": {
            "type": "lineWithFocusChart",
            "height": 450,
            "margin": {
              "top": 20,
              "right": 20,
              "bottom": 60,
              "left": 60
            },
            color: d3.scale.category10().range(),
            useInteractiveGuideline: true,
            clipVoronoi: true,
            objectEquality : false,
            "transitionDuration": 1000,
            "xAxis": {
               tickFormat: function(d) {
                    return d3.time.format('%m.%d %H:%M:%S')(new Date(d))
                },
                showMaxMin: false,
                axisLabel: "Time"
            },
            "x2Axis": {
               tickFormat: function(d) {
                    return d3.time.format('%m.%d %H:%M:%S')(new Date(d))
                },
                showMaxMin: false
            },
            "yAxis": {
              "axisLabel": "Value",
              "rotateYLabel": true,
            },
            "y2Axis": {
              "rotateYLabel": true,
            }
        }
    };

    $scope.config = {
        visible: true, // default: true
        extended: true, // default: false
        disabled: false, // default: false
        autorefresh: true, // default: true
        refreshDataOnly: true // default: false
    };

    $scope.graphData = []; //can leave empty

    $scope.autoRefresh = false;
    $scope.autoRefreshSeconds = 120;
    $scope.entriesToLoadDataFor = [];
    // to invalidate auto-refresh if there was a manual refresh in between
    var autoRefreshCounter = 0;

    function initGraphDataFromResponse(data) {
        $scope.loadedGraphData = data;
        if ($location.search().loadfile) {
            $scope.rc.api.updateWithData([]);
            $scope.timedScalars = {};
            selectedEntries = "";

            var variableKey;
            for (var myKey in $scope.loadedGraphData) {
                variableKey = data[myKey].key;
                $scope.timedScalars[variableKey] = {key: data[myKey].key, selected: false};
            }
        }else{
            $scope.rc.api.updateWithData(data);
            triggerAutoRefresh();
        }
    }

    $scope.$watch('autoRefresh', triggerAutoRefresh);
    $scope.$watch('autoRefreshSeconds', triggerAutoRefresh);

    function triggerAutoRefresh() {
        if(! $scope.autoRefresh) {
            return;
        }
        if ($location.search().loadfile) {
            return;
        }

        var oldCounter = autoRefreshCounter;
        setTimeout(function() {
            if(autoRefreshCounter !== oldCounter+1) {
                return;
            }
            $scope.refresh();
        }, $scope.autoRefreshSeconds * 1000);
        autoRefreshCounter += 1;
    }

    $scope.refresh = function() {
        if ($location.search().loadfile) {
            return;
        }

        var selectedEntriesForServer = "";
        if (typeof $scope.timedScalars == 'undefined'){
            return;
        }

        for (var keySelected in $scope.entriesToLoadDataFor) {
            for (var keyData in $scope.timedScalars) {
                if ($scope.timedScalars[keyData].key == $scope.entriesToLoadDataFor[keySelected]){
                    selectedEntriesForServer = selectedEntriesForServer.concat($scope.entriesToLoadDataFor[keySelected]);
                    selectedEntriesForServer = selectedEntriesForServer.concat(",");
                }
            }
        }
        if (selectedEntriesForServer.length > 1) {
            Rest.call('getGraphData/' + selectedEntriesForServer, initGraphDataFromResponse);
        }else {
            //remove old graph-data
            $scope.rc.api.updateWithData([]);
        }

    };

    function initFromResponse(data) {
        $scope.timedScalars = data.timedScalars;
        triggerAutoRefresh();
    }

    $scope.toggleGraphData = function(key) {
        var found = false;
        for (var myKey in $scope.entriesToLoadDataFor) {
            if ($scope.entriesToLoadDataFor[myKey] == key){
                //$scope.entriesToLoadDataFor.remove(Number(myKey));
                $scope.entriesToLoadDataFor.splice(myKey ,1);
                found = true;
            }
        }
        if (!found) {
            $scope.entriesToLoadDataFor.push(key);
        }

        if ($location.search().loadfile) {
            //TODO FOX088S transform this to entriesToLoadDataFor
            var newGaphData = [];
            for (var myKey in $scope.loadedGraphData) {
                var keyName = $scope.loadedGraphData[myKey].key;
                if ($scope.timedScalars[keyName].selected) {
                    newGaphData.push({
                        key: $scope.loadedGraphData[myKey].key,
                        values: $scope.loadedGraphData[myKey].values
                    });
                    // newGraphData.push($scope.graphData[myKey].key, $scope.graphData[myKey].values);
                }
                $scope.graphData = newGaphData;
                $scope.rc.api.updateWithData(newGaphData);
            }
        } else {
            //only, if not loaded from a file
            $scope.refresh();
        }
    };

    // check if data from other sources should be loaded
    function loadExternalData() {
        var loadfileParam = $location.search().loadfile;
        if (loadfileParam) {
            selectedEntries = "";
            Rest.callOther("loadableServerDataFiles", "loadFromFile" + "/" + loadfileParam, initGraphDataFromResponse);
        }else{
            Rest.call('getData', initFromResponse);
            $scope.refresh();
        }
    }
    loadExternalData();

});
