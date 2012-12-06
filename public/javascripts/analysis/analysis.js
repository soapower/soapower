$(document).ready(function() {
    loadGraph()
    $('#environmentSelect').change(function() {
        document.location.href="/analysis/" + $('#environmentSelect').val() + "/"+ $('#soapActionSelect').val() +"/"
    })
    $('#soapActionSelect').change(function() {
        document.location.href="/analysis/" + $('#environmentSelect').val() + "/"+ $('#soapActionSelect').val() +"/"
    })
});


function loadGraph() {

    $(function() {

        $.getJSON('load/', function(datas) {

            // Create a timer
            var start = + new Date();

            var seriesOptions = [];


            var idSeries = {};
            var nseries = 0;
            $.each(datas, function(key, value) {
                var name = value.env + " " + value.act;
                if (idSeries[name] == null) {
                    idSeries[name] = nseries;

                    console.log("create serie:" + name + " val:" + nseries + " val2:" + idSeries[name]);

                    seriesOptions[nseries] = {
                        name: name,
                        data: [],
                        pointInterval: 3600 * 1000,
                        dataGrouping: {
                            enabled: true
                        },
                        marker : {
                            enabled : true,
                            radius : 3
                        },
                        tooltip: {
                            valueDecimals: 2,
                            valueSuffix: 'ms'
                        }
                    };

                    nseries++
                }

                // value.env, value.act, value.date, value.time

                var tuple = [value.date, value.time];
                seriesOptions[idSeries[name]].data.push(tuple)

            });

            // Create the chart
            window.chart = new Highcharts.StockChart({
                chart: {
                    renderTo: 'container',
                    events: {
                        load: function(chart) {
                            this.setTitle(null, {
                                text: 'Built chart at '+ (new Date() - start) +'ms'
                            });
                        }
                    },
                    zoomType: 'x'
                },

                rangeSelector: {
                    buttons: [{
                        type: 'day',
                        count: 1,
                        text: '1d'
                    },{
                        type: 'day',
                        count: 3,
                        text: '3d'
                    }, {
                        type: 'week',
                        count: 1,
                        text: '1w'
                    }, {
                        type: 'month',
                        count: 1,
                        text: '1m'
                    }, {
                        type: 'month',
                        count: 6,
                        text: '6m'
                    }, {
                        type: 'year',
                        count: 1,
                        text: '1y'
                    }, {
                        type: 'all',
                        text: 'All'
                    }],
                    selected: 3
                },

                title: {
                    text: 'Response Time'
                },

                subtitle: {
                    text: 'Built chart at...' // dummy text to reserve space for dynamic subtitle
                },

                yAxis: {
                    title: {
                        text: 'Response Time in ms'
                    }
                },

                tooltip: {
                    pointFormat: '<span style="color:{series.color}">{series.name}</span>: <b>{point.y}</b><br/>',
                    valueDecimals: 0
                },

                xAxis : {
                    minRange: 60 * 1000 // 3600 * 1000 : one hour. 3600 * 1000 => 1 min
                },

                series : seriesOptions
            });
        });

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