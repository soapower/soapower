$(document).ready(function() {
    createTable();
});

function createTable() {
  $('#datas').dataTable( {
    "bPaginate": false,
    "bFilter": true,
    "bSort": true,
    "bInfo": true,
    "bAutoWidth": false,
    "bLengthChange": true,
    "oLanguage": {"sProcessing": "<span class='label label-important'>Loading...</span>"},
    "bProcessing": true,
    "bServerSide": false,
    "bDeferRender": true,
    "sAjaxSource": "groups/listDatatable",
    "bStateSave": true,
    "fnStateSave": function (oSettings, oData) {
      localStorage.setItem( 'DataTables_'+window.location.pathname, JSON.stringify(oData) );
    },
    "fnStateLoad": function (oSettings) {
      return JSON.parse( localStorage.getItem('DataTables_'+window.location.pathname) );
    }
  } );
};
