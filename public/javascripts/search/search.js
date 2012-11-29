
$(document).ready(function() {
  createTable();
  $('#environmentSelect').change(function() {
    document.location.href="/search/" + $('#environmentSelect').val() + "/"+ $('#soapActionSelect').val() +"/"
  })
  $('#soapActionSelect').change(function() {
    document.location.href="/search/" + $('#environmentSelect').val() + "/"+ $('#soapActionSelect').val() +"/"
  })
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
    }
  } );


}

