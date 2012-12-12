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
        $('.popSoapAction').tooltip()
	    prepareRequestsReplays();
    }
  } );
};

function prepareRequestsReplays() {
	$(".replay").click(function() {
		var requestId = $(this).attr("data-request-id");
		$.get("/replay/" + requestId, 
			function() {
				location.reload(false);
				//document.location.href=window.location
			}
		);
	});
};
