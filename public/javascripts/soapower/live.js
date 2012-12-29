
// Global var
var socket = null;

$(document).ready(function() {
    startWS();
    createTable();
    initCriterias("search");
    btnActions();
});

var receiveEvent = function(event) {
    var data = JSON.parse(event.data)

    // Handle errors
    if(data.error) {
        socket.close()
        $("#onError span").text(data.error)
        $("#onError").show()
        return
    }

    if (data.kind == "talkRequestData") {
        $('#datas').dataTable().fnAddData( [ data.message["0"] ] );
    }
    $('#nbConnected').html($(data.members).size() - 1) // substract Robot

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

function btnActions() {
    $('#btnStop').click(function() {
        stopWS();
    });
    $('#btnStart').click(function() {
        startWS();
    });
}

function createTable() {
    $('#datas').dataTable( {
        "bPaginate": true,
        "bFilter": true,
        "bSort": true,
        "bInfo": true,
        "aaSorting": [[ 4, "desc" ]],
        "bAutoWidth": false,
        "bLengthChange": true,
        "iDisplayLength": 100,
        "oLanguage": {"sSearch": "<span class='label'>Search in all columns</span>"},
        "bProcessing": true,
        "bServerSide": false,
        "bDeferRender": true,
        "bStateSave": true,
        "fnStateSave": function (oSettings, oData) {
            localStorage.setItem( 'DataTables_'+window.location.pathname, JSON.stringify(oData) );
        },
        "fnStateLoad": function (oSettings) {
            return JSON.parse( localStorage.getItem('DataTables_'+window.location.pathname) );
        },
        "fnDrawCallback": function( oSettings ) {
            $('#datas td:nth-child(7), #datas td:nth-child(8), #datas td:nth-child(9)').addClass('narrow')
            $('.popSoapAction').tooltip()
            prepareRequestsReplays();
        }
    } );

    $('#datas').dataTable().fnFilter($('#search').val());
};

function prepareRequestsReplays() {
    var callback = function() {
        console.log("replay Callback")
    }

    $(".replay").click(function() {
        var requestId = $(this).attr("data-request-id");
        $.ajax({
            url: "/replay/" + requestId,
            error: callback
        }).done(callback);
    });
};
