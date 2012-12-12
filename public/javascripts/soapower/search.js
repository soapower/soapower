$(document).ready(function() {
    createTable();
    initCriterias("search");
});

function createTable() {
  $('#datas').dataTable( {
    "bPaginate": true,
    "bFilter": true,
    "bSort": false,
    "bInfo": true,
    "bAutoWidth": false,
    "bLengthChange": true,
    "iDisplayLength": 10,
    "oLanguage": {"sSearch": "<span class='label'>Search in soapAction</span>"},
    "bProcessing": true,
    "bServerSide": true,
    "bDeferRender": true,
    "sAjaxSource": "listDatatable",
    "fnDrawCallback": function( oSettings ) {
    	$('#datas td:nth-child(7), #datas td:nth-child(8), #datas td:nth-child(9)').addClass('narrow')
        $('.popSoapAction').tooltip()
	    prepareRequestsReplays();
    }
  } );
};

function prepareRequestsReplays() {
	var callback = function() {
		location.reload();
	}
	
	$(".replay").click(function() {
		var requestId = $(this).attr("data-request-id");
		$.ajax({
			url: "/replay/" + requestId,
			error: callback
		}).done(callback);
	});
};
