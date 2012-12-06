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

            var data = [];
            $.each(datas, function(key, value) {
                var tuple = [value.date, value.time]
                data.push(tuple)
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
                    text: 'Reponse Time'
                },

                subtitle: {
                    text: 'Built chart at...' // dummy text to reserve space for dynamic subtitle
                },

                yAxis: {
                    title: {
                        text: 'Response Time in ms'
                    }
                },

                xAxis : {
                    minRange: 60 * 1000 // 3600 * 1000 : one hour. 3600 * 1000 => 1 min
                },

                series : [{
                    name : 'Requests',
                    data : data,
                    pointInterval: 3600 * 1000,
                    dataGrouping: {
                        enabled: true
                    },
                    tooltip: {
                        valueDecimals: 1,
                        valueSuffix: 'ms'
                    }
                }]
            });
        });

    });
}