
$(document).ready(function() {
   createTable();
});


function createTable() {
  
  console.log("Create table")
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
    "sAjaxSource": "listDatatable"
  } );
}

