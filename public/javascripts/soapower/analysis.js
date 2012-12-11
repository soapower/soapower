$(document).ready(function() {
    loadGraph();
    $('#environmentSelect').change(function() {
        document.location.href=makeUrl();
    });
    $('#soapActionSelect').change(function() {
        document.location.href=makeUrl();
    });
    $('#statusSelect').change(function() {
        document.location.href=makeUrl();
    });
    $('#from').change(function() {
        document.location.href=makeUrl();
    });
    $('#to').change(function() {
        document.location.href=makeUrl();
    });

    $("#from").datepicker({
        dateFormat: "yy-mm-dd",
        changeMonth: true,
        numberOfMonths: 3,
        onClose: function (selectedDate) {
            $("#to").datepicker("option", "minDate", selectedDate);
        }
    });
    $("#to").datepicker({
        dateFormat: "yy-mm-dd",
        changeMonth: true,
        numberOfMonths: 3,
        onClose: function (selectedDate) {
            $("#from").datepicker("option", "maxDate", selectedDate);
        }
    });
});

function makeUrl() {
    var minDate = $('#from').val();
    var maxDate = $('#to').val();
    if (minDate == "") minDate = "all";
    if (maxDate == "") maxDate = "all";

    return "/analysis/" + $('#environmentSelect').val()
        + "/"+ $('#soapActionSelect').val()
        +"/"+ minDate
        +"/"+ maxDate
        +"/" + $('#statusSelect').val() + "/";
}

function loadGraph() {

    $(function() {

        var data = [];

        $.getJSON('load/', function(datas) {

            // Create a timer
            var start = + new Date();

            var data = processData(datas);

            // Create the chart
            window.chart = new Highcharts.Chart({
                chart: {
                    renderTo: 'container',
                    type: 'scatter',
                    //type: 'line',
                    events: {
                        load: function(chart) {
                            this.setTitle(null, {
                                text: 'Built chart at '+ (new Date() - start) +'ms'
                            });
                        }
                    },
                    zoomType: 'x'
                },

                title: {
                    text: 'Response Time'
                },

                subtitle: {
                    text: 'Built chart at...' // dummy text to reserve space for dynamic subtitle
                },

                yAxis: {
                    min:0,
                    labels: {
                        formatter: function() {
                            return this.value +' ms';
                        }
                    },
                    valueDecimals: 0,
                    title: {
                        text: 'Response Time in ms'
                    }
                },

                tooltip: {
                    formatter: function() {
                        return ''+
                        this.series.name +' : '+ this.y +' ms';
                    },
                    valueDecimals: 0
                },

                xAxis : {
                    type: 'datetime',
                    dateTimeLabelFormats: { // don't display the dummy year
                        month: '%e. %b',
                        year: '%b'
                    }
                },
                series : data
            });
            if (data.length == 0) chart.showLoading('No Result');
        });
    });
}

function processData(datas) {
    var data = [];
    var idSeries = {};
    var nseries = 0;
    $.each(datas, function(key, value) {
        // value.e : environment
        // value.a : soapAction
        // value.d : startTime Date : timestamp
        // value.t : time in ms
        var name = value.e + " " + value.a;
        if (idSeries[name] == null) {
            idSeries[name] = nseries;

            data[nseries] = {
                name: name,
                data: [],
                pointInterval: 3600 * 1000,
                dataGrouping: {
                    smoothed: true
                },
                marker : {
                    enabled : true,
                    radius : 3
                },
                tooltip: {
                    valueDecimals: 0,
                    valueSuffix: 'ms'
                }
            };

            nseries++
        }
        // value.env, value.act, value.date, value.time
        var tuple = [value.d, value.t];
        data[idSeries[name]].data.push(tuple);

    });
    return data;
}


/**
 * Load new data depending on the selected min and max
 */
function afterSetExtremes(e) {

    var url,
        currentExtremes = this.getExtremes(),
        range = e.max - e.min;

    chart.showLoading('Loading data from server...');

    $.getJSON('load/?dateMin='+ Math.round(e.min) + '&dateMax='+ Math.round(e.max), function(datas) {

        chart.series[0].setData(processData(datas));
        chart.hideLoading();
    });
}

Highcharts.theme = {
    colors: ['#058DC7', '#50B432', '#ED561B', '#DDDF00', '#24CBE5', '#64E572', '#FF9655', '#FFF263', '#6AF9C4'],
    chart: {
        /*backgroundColor: {
            linearGradient: [0, 0, 500, 500],
            stops: [
                [0, 'rgb(255, 255, 255)'],
                [1, 'rgb(240, 240, 255)']
            ]
        },*/
        borderWidth: 2,
        plotBackgroundColor: 'rgba(255, 255, 255, .9)',
        plotShadow: true,
        plotBorderWidth: 1
    },
    title: {
        style: {
            color: '#000',
            font: 'bold 16px "Trebuchet MS", Verdana, sans-serif'
        }
    },
    subtitle: {
        style: {
            color: '#666666',
            font: 'bold 12px "Trebuchet MS", Verdana, sans-serif'
        }
    },
    xAxis: {
        gridLineWidth: 1,
        lineColor: '#000',
        tickColor: '#000',
        labels: {
            style: {
                color: '#000',
                font: '11px Trebuchet MS, Verdana, sans-serif'
            }
        },
        title: {
            style: {
                color: '#333',
                fontWeight: 'bold',
                fontSize: '12px',
                fontFamily: 'Trebuchet MS, Verdana, sans-serif'

            }
        }
    },
    yAxis: {
        minorTickInterval: 'auto',
        lineColor: '#000',
        lineWidth: 1,
        tickWidth: 1,
        tickColor: '#000',
        labels: {
            style: {
                color: '#000',
                font: '11px Trebuchet MS, Verdana, sans-serif'
            }
        },
        title: {
            style: {
                color: '#333',
                fontWeight: 'bold',
                fontSize: '12px',
                fontFamily: 'Trebuchet MS, Verdana, sans-serif'
            }
        }
    },
    legend: {
        itemStyle: {
            font: '9pt Trebuchet MS, Verdana, sans-serif',
            color: 'black'

        },
        itemHoverStyle: {
            color: '#039'
        },
        itemHiddenStyle: {
            color: 'gray'
        }
    },
    labels: {
        style: {
            color: '#99b'
        }
    }
};

// Apply the theme
var highchartsOptions = Highcharts.setOptions(Highcharts.theme);