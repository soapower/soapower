
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
    "bLengthChange": false,
    "iDisplayLength": 10,
    "bProcessing": true,
    "bServerSide": true,
    "bDeferRender": true,
    "sAjaxSource": "/search/listDatatable"
  } );
}

