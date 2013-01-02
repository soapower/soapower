
// Global var
var socket = null;
var chartCPU = null;
var chartMemory = null;

$(document).ready(function() {
    graph();
    startWS();
    btnActions();
});

var receiveEvent = function(event) {
    var data = event.data.split(":");
    var type = data[1];
    var value = parseFloat(data[0]);
    //console.log("event:" + " " + type + ":"+ value)

    if (type == "cpu") {
        chartCPU.series[0].points[0].update(value)
    } else if (type == "memory") {
        chartMemory.series[0].points[0].update(value)
    } else if (type == "totalMemory") {
        chartMemory.series[0].points[0].update(value)
        chartMemory.yAxis[0].setExtremes(0,value)
    }
}


function btnActions() {
    $('#btnStop').click(function() {
        stopWS();
    });
    $('#btnStart').click(function() {
        startWS();
    });
}

function stopWS() {
    socket.close();
    console.log("Websocket closed")
    $('.liveOnAir').hide();
    $('.liveOff').show();
}

function startWS() {
    var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
    socket = new WS($('#urlWS').val())
    console.log("Websocket started")
    socket.onmessage = receiveEvent
    $('.liveOnAir').show();
    $('.liveOff').hide();
}

function graph() {
    chartCPU = makeGraph('cpu', 'CPU Usage in %', 100, 60, 80, '%');
    chartMemory = makeGraph('memory', 'Memory in MB', 400, 60, 80, 'MB');
}

function makeGraph(container, title, maxValue, range1, range2, valueSuffix) {
       var ret = new Highcharts.Chart({

        chart: {
            renderTo: container,
            type: 'gauge',
            plotBackgroundColor: null,
            plotBackgroundImage: null,
            plotBorderWidth: 0,
            plotShadow: false
        },

        title: {
            text: ''
        },

        pane: {
            startAngle: -150,
            endAngle: 150,
            background: [{
                backgroundColor: {
                    linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1 },
                    stops: [
                        [0, '#FFF'],
                        [1, '#333']
                    ]
                },
                borderWidth: 0,
                outerRadius: '109%'
            }, {
                backgroundColor: {
                    linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1 },
                    stops: [
                        [0, '#333'],
                        [1, '#FFF']
                    ]
                },
                borderWidth: 1,
                outerRadius: '107%'
            }, {
                // default background
            }, {
                backgroundColor: '#DDD',
                borderWidth: 0,
                outerRadius: '105%',
                innerRadius: '103%'
            }]
        },

        // the value axis
        yAxis: {
            min: 0,
            max: maxValue,

            minorTickInterval: 'auto',
            minorTickWidth: 1,
            minorTickLength: 10,
            minorTickPosition: 'inside',
            minorTickColor: '#666',

            tickPixelInterval: 30,
            tickWidth: 2,
            tickPosition: 'inside',
            tickLength: 10,
            tickColor: '#666',
            labels: {
                step: 2,
                rotation: 'auto'
            },
            title: {
                text: title
            },
            plotBands: [{
                from: 0,
                to: range1,
                color: '#55BF3B' // green
            }, {
                from: range1,
                to: range2,
                color: '#DDDF0D' // yellow
            }, {
                from: range2,
                to: maxValue + 500,
                color: '#DF5353' // red
            }]
        },

        series: [{
            name: 'Serie1',
            data: [0],
            tooltip: {
                valueSuffix: valueSuffix
            }
        }]

        }
    );
    return ret;
}