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

    $.getJSON('load/', function(datas) {

       var data = [];
       var categories = [];
       $.each(datas, function(key, value) {
            var tuple = [value.date, value.time]
            categories.push(value.date)
            data.push(tuple)
        });


        chart1 = new Highcharts.Chart({
            chart: {
                renderTo: 'container',
                type: 'areaspline'	// 'areaspline'
            },
            title: {
                text: "Average Response Time in ms"
            },
            xAxis: {
                categories: categories,
                title: {
                    text: 'Requests'
                }
            },
            yAxis: {
                title: {
                    text: 'Average Response Time in ms'
                }
            },
            series: [{
                name : 'Requests',
                data:  data
            }],
            tooltip: {
                formatter: function() {
                    return this.x +': '+ this.y + 'ms';
                }
            }
        });
    });
}