
$(document).ready(function() {
    var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
    var socket = new WS($('#urlWS').val())

    var receiveEvent = function(event) {
        var data = JSON.parse(event.data)

        // Handle errors
        if(data.error) {
            socket.close()
            $("#onError span").text(data.error)
            $("#onError").show()
            return
        } else {
            $("#onChat").show()
        }

        console.log("Valeur : " + data.message["0"]);

        if (data.kind == "talkRequestData") {
            $('#datas').dataTable().fnAddData( [ data.message["0"] ] );
        }
        $('#nbConnected').html($(data.members).size() - 1) // substract Robot

    }

    socket.onmessage = receiveEvent

    createTable();
    initCriterias("search");

});

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
        "fnDrawCallback": function( oSettings ) {
            $('#datas td:nth-child(7), #datas td:nth-child(8), #datas td:nth-child(9)').addClass('narrow')
            $('.popSoapAction').tooltip()
            prepareRequestsReplays();
        }
    } );
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
